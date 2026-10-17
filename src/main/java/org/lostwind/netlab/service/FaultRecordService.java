package org.lostwind.netlab.service;

import lombok.extern.slf4j.Slf4j;
import org.lostwind.netlab.entity.*;
import org.lostwind.netlab.enums.*;
import org.lostwind.netlab.mapper.BorrowMapper;
import org.lostwind.netlab.mapper.DeviceMapper;
import org.lostwind.netlab.mapper.FaultRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class FaultRecordService {
    @Autowired
    private AccountService accountService;
    @Autowired
    private BorrowService borrowService;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    private OperationLogService operationLogService;

    @Autowired
    private FaultRecordMapper faultRecordMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private BorrowMapper borrowMapper;


    // 得到所有的故障单
    public List<FaultRecord> getAllList() {
        return faultRecordMapper.selectALl();
    }

    // 根据故障单id得到故障单
    public FaultRecord getFaultRecordById(Integer id) {
        FaultRecord record = faultRecordMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("未查询到故障单");
        }
        return record;
    }

    // 根据用户id得到该用户提交的故障单
    public List<FaultRecord> getUserFaultRecordByUserId(Integer accountId) {
        List<FaultRecord> records = faultRecordMapper.selectByReporterId(accountId);
        if (records == null) {
            throw new IllegalArgumentException("未查询到当前用户提交的故障单");
        }
        return records;
    }

    // 用户创建并向数据库提交设备故障单
    @Transactional
    public void createFaultRecordByUser(Integer borrowRecordId, Integer reporterId, String description) {
        accountService.getAccountById(reporterId);

        BorrowRecord recordPreview = borrowService.getRecordById(borrowRecordId);
        // 检测用户id是否与借出单对应的预约单的申请人id相同
        Reservation reservationPreview = reservationService.getUserReservationById(reporterId, recordPreview.getReservationId());

        // device,record加锁
        Device device = deviceMapper.selectByIdForUpdate(reservationPreview.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        BorrowRecord record = borrowMapper.selectByIdForUpdate(borrowRecordId);
        if (record == null) {
            throw new IllegalArgumentException("借用记录不存在");
        }

        String content = description == null ? null : description.trim();
        if (content == null || content.isEmpty() || content.length() > 500) {
            throw new IllegalArgumentException("故障描述不能为空或超过500字");
        }

        if (device.getStatus() != DeviceStatus.BORROWED) {
            throw new IllegalArgumentException("当前借出的设备为非借出状态，不可创建故障单");
        }
        if (record.getReturnAt() != null) {
            throw new IllegalArgumentException("该借用记录已经归还，不能上报故障");
        }
        if (record.getStatus() != BorrowStatus.BORROWED && record.getStatus() != BorrowStatus.RETURN_PENDING) {
            throw new IllegalArgumentException("当前借用记录不能上报故障");
        }
        if (faultRecordMapper.countUnresolvedByBorrowRecordId(borrowRecordId) > 0) {
            throw new IllegalArgumentException("该借用记录已有未处理的故障单，请勿重复提交");
        }

        FaultRecord faultRecord = new FaultRecord();
        faultRecord.setDeviceId(device.getId());
        faultRecord.setReporterId(reporterId);
        faultRecord.setBorrowRecordId(borrowRecordId);
        faultRecord.setStatus(FaultStatus.PENDING);
        faultRecord.setDescription(content);
        faultRecord.setReportedAt(LocalDateTime.now());
        // 用户申请归还然后管理员确认归还时,再修改设备状态为损坏
        int rows = faultRecordMapper.insert(faultRecord);
        if (rows != 1) {
            throw new IllegalStateException("故障单创建失败，请稍后重试");
        }

        operationLogService.record(
                BusinessType.FAULT,
                faultRecord.getId(),
                reporterId,
                OperationType.REPORT_FAULT,
                null,
                FaultStatus.PENDING
        );

        log.info(
                "用户创建故障单:faultRecordId={}, deviceId={}, reporterId={}",
                faultRecord.getId(),
                faultRecord.getDeviceId(),
                reporterId
        );
    }


    // 管理员创建并向数据库提交设备故障单
    @Transactional
    public void createFaultRecordByAdmin(Integer deviceId, Integer reporterId, String description) {
        Account account = accountService.getAccountById(reporterId);
        if (!"ADMIN".equals(account.getRole())) {
            throw new IllegalArgumentException("当前用户无权登记设备故障");
        }

        String content = description == null ? null : description.trim();
        if (content == null || content.isEmpty() || content.length() > 500) {
            throw new IllegalArgumentException("故障描述不能为空或超过500字");
        }

        Device device = deviceMapper.selectByIdForUpdate(deviceId);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        if (device.getStatus() != DeviceStatus.AVAILABLE) {
            throw new IllegalArgumentException("只有可用/未借出状态的设备才能登记故障");
        }

        if(faultRecordMapper.countUnresolvedByDeviceId(deviceId) > 0) {
            throw new IllegalArgumentException("该设备已有未处理的故障单");
        }

        FaultRecord faultRecord = new FaultRecord();
        faultRecord.setDeviceId(deviceId);
        faultRecord.setReporterId(reporterId);
        // 管理员发现的故障不属于某次借用
        faultRecord.setBorrowRecordId(null);
        faultRecord.setStatus(FaultStatus.PENDING);
        faultRecord.setDescription(content);
        faultRecord.setReportedAt(LocalDateTime.now());

        if (faultRecordMapper.insert(faultRecord) != 1) {
            throw new IllegalStateException("故障单创建失败");
        }

        if (deviceMapper.markDamaged(deviceId) != 1) {
            throw new IllegalStateException("设备状态已经发生改变");
        }

        operationLogService.record(
                BusinessType.FAULT,
                faultRecord.getId(),
                reporterId,
                OperationType.REPORT_FAULT,
                null,
                FaultStatus.PENDING
        );

        log.info(
                "管理员创建故障单:faultRecordId={}, deviceId={}, reporterId={}",
                faultRecord.getId(),
                faultRecord.getDeviceId(),
                reporterId
        );
    }

    // 管理员开始维修、处理故障单
    @Transactional
    public void startMaintenance(String accountName, Integer recordId) {
        Account account = accountService.getAccountByUsername(accountName);
        if (!"ADMIN".equals(account.getRole())) {
            throw new IllegalArgumentException("当前用户无权限处理故障单");
        }

        // 第一次只读取device_id
        FaultRecord preview = getFaultRecordById(recordId);
        if (preview == null) {
            throw new IllegalArgumentException("故障单不存在");
        }

        // 得到device并上锁
        Device device = deviceMapper.selectByIdForUpdate(preview.getDeviceId());
        // 得到record并上锁
        FaultRecord record = faultRecordMapper.selectByIdForUpdate(recordId);
        if (record == null) {
            throw new IllegalArgumentException("故障单不存在");
        }

        if (record.getStatus() != FaultStatus.PENDING) {
            throw new IllegalArgumentException("该故障单的状态不可维修");
        }

        if (device.getStatus() != DeviceStatus.DAMAGED) {
            throw new IllegalArgumentException("设备未归还或不处于损坏状态，不能维修");
        }

        int deviceRows = deviceMapper.startMaintenance(device.getId());
        if (deviceRows != 1) {
            throw new IllegalStateException("设备状态已经发生改变");
        }

        int recordRows = faultRecordMapper.startProcessing(recordId);
        if (recordRows != 1) {
            throw new IllegalArgumentException("故障单状态已发生改变");
        }

        operationLogService.record(
                BusinessType.FAULT,
                recordId,
                account.getId(),
                OperationType.START_MAINTENANCE,
                FaultStatus.PENDING,
                FaultStatus.PROCESSING
        );

        log.info(
                "设备开始维修:faultRecordId={}, deviceId={}, operatorId={}",
                recordId,
                device.getId(),
                account.getId()
        );
    }

    // 管理员选择故障单的维修结果
    @Transactional
    public void setRecordResult(String accountName, Integer recordId, boolean result) {
        Account account = accountService.getAccountByUsername(accountName);
        if (!"ADMIN".equals(account.getRole())) {
            throw new IllegalArgumentException("当前用户无权限处理故障单");
        }

        FaultRecord preview = getFaultRecordById(recordId);

        // device,record加锁
        Device device = deviceMapper.selectByIdForUpdate(preview.getDeviceId());
        FaultRecord record = faultRecordMapper.selectByIdForUpdate(recordId);

        if (record.getStatus() != FaultStatus.PROCESSING) {
            throw new IllegalArgumentException("该故障单不处于维修状态");
        }

        if (device.getStatus() != DeviceStatus.MAINTENANCE) {
            throw new IllegalArgumentException("设备不处于维修状态，不能设置维修结果");
        }

        DeviceStatus status = result ? DeviceStatus.AVAILABLE : DeviceStatus.OFFLINE;
        deviceService.updateStatus(record.getDeviceId(), status);
        int rows = faultRecordMapper.resolve(recordId);
        if (rows != 1) {
            throw new IllegalArgumentException("故障单状态更新失败");
        }

        operationLogService.record(
                BusinessType.FAULT,
                recordId,
                account.getId(),
                OperationType.COMPLETE_MAINTENANCE,
                FaultStatus.PROCESSING,
                FaultStatus.RESOLVED
        );

        log.info(
                "维修处理完成:faultRecordId={}, deviceId={}, resultStatus={}, operatorId={}",
                recordId,
                record.getDeviceId(),
                status,
                account.getId()
        );
    }
}
