package org.lostwind.netlab.service;

import lombok.extern.slf4j.Slf4j;
import org.lostwind.netlab.entity.BorrowRecord;
import org.lostwind.netlab.entity.Device;
import org.lostwind.netlab.entity.Reservation;
import org.lostwind.netlab.enums.*;
import org.lostwind.netlab.mapper.BorrowMapper;
import org.lostwind.netlab.mapper.DeviceMapper;
import org.lostwind.netlab.mapper.FaultRecordMapper;
import org.lostwind.netlab.mapper.ReservationMapper;
import org.lostwind.netlab.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BorrowService {
    @Autowired
    ReservationService reservationService;
    @Autowired
    DeviceService deviceService;
    @Autowired
    OperationLogService operationLogService;

    @Autowired
    private BorrowMapper borrowMapper;
    @Autowired
    private ReservationMapper reservationMapper;
    @Autowired
    private DeviceMapper deviceMapper;
    @Autowired
    private FaultRecordMapper faultRecordMapper;


    public BorrowRecord getRecordById(Integer id) {
        BorrowRecord record = borrowMapper.selectById(id);
        if (record == null) {
            throw new IllegalArgumentException("借出单不存在");
        }
        return record;
    }

    public BorrowRecord getRecordByReservationId(Integer id) {
        BorrowRecord record = borrowMapper.selectByReservationId(id);
        if (record == null) {
            throw new IllegalArgumentException("借出单不存在");
        }
        return record;
    }

    // 管理员确认借出
    @Transactional
    public void confirmBorrow(Integer reservationId, Integer operatorId) {
        if (borrowMapper.selectByReservationId(reservationId) != null) {
            throw new IllegalArgumentException("该预约已被其他管理员受理");
        }

        // 第一普通查询，只获得device_id
        Reservation preview = reservationMapper.selectById(reservationId);
        if (preview == null) {
            throw new IllegalArgumentException("预约不存在");
        }

        // 对设备加锁
        Device device = deviceMapper.selectByIdForUpdate(preview.getDeviceId());
        // 对预约记录加锁
        Reservation reservation = reservationMapper.selectByIdForUpdate(reservationId);

        if (reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new IllegalArgumentException("只有审核通过的预约才能借出");
        }

        if (device.getStatus() != DeviceStatus.AVAILABLE) {
            throw new IllegalArgumentException("设备当前状态不可借出");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(reservation.getStartTime())) {
            throw new IllegalArgumentException("还未到预约开始时间");
        }
        if (!now.isBefore(reservation.getEndTime())) {
            throw new IllegalArgumentException("该预约已经过期");
        }

        BorrowRecord record = new BorrowRecord();
        record.setBorrowCode(CodeGenerator.borrowCode());
        record.setReservationId(reservationId);
        record.setBorrowOperatorId(operatorId);
        // 设置时间相关信息
        record.setBorrowedAt(LocalDateTime.now().withNano(0));
        record.setDueAt(reservation.getEndTime());  // 应归还时间 = 预约单填入的结束时间

        record.setStatus(BorrowStatus.BORROWED);

        borrowMapper.insert(record);
        if (reservationMapper.updateStatus(reservationId, ReservationStatus.BORROWED) != 1) {
            throw new IllegalStateException("预约更新失败，请稍后重试");
        }
        if (deviceMapper.markBorrowed(device.getId()) != 1) {
            throw new IllegalStateException("设备更新失败");
        }

        operationLogService.record(
                BusinessType.RESERVATION,
                reservationId,
                operatorId,
                OperationType.CONFIRM_BORROW,
                ReservationStatus.APPROVED,
                ReservationStatus.BORROWED
        );

        log.info(
                "设备确认借出, reservationId={}, borrowRecordId={}, operatorId={}, deviceId={}",
                reservationId,
                record.getId(),
                operatorId,
                device.getId()
        );
    }

    // 管理员确认归还
    @Transactional
    public void confirmReturn(Integer id, Integer operatorId, String remark) {
        // 第一次查询只用于判断预约是否存在,并取得device_id
        Reservation preview = reservationService.getReservationById(id);
        if (preview == null) {
            throw new IllegalArgumentException("该预约不存在");
        }

        // 获取设备并上锁
        Device device = deviceMapper.selectByIdForUpdate(preview.getDeviceId());
        if (device == null) {
            throw new IllegalArgumentException("预约对应的设备不存在");
        }
        // 重新获取预约单并上锁
        Reservation reservation = reservationMapper.selectByIdForUpdate(id);
        // 获取借出记录并上锁
        BorrowRecord borrowRecord = borrowMapper.selectByReservationIdForUpdate(id);

        if (remark != null && remark.length() > 500) {
            throw new IllegalArgumentException("借用记录的备注不能超过500字");
        }
        if (reservation.getStatus() != ReservationStatus.RETURN_PENDING) {
            throw new IllegalArgumentException("当前预约单状态不可归还");
        }
        if (borrowRecord.getStatus() != BorrowStatus.RETURN_PENDING) {
            throw new IllegalArgumentException("当前预约单对应的借出单状态不可归还");
        }
        if (LocalDateTime.now().isBefore(borrowRecord.getBorrowedAt())) {
            throw new IllegalArgumentException("确认归还时间不应早于确认借出时间");
        }
        if (device.getStatus() != DeviceStatus.BORROWED) {
            throw new IllegalArgumentException("当前预约单对应的设备状态为未借出");
        }

        int borrowRows = borrowMapper.confirmReturnByReservationId(id, operatorId, remark);
        if (borrowRows != 1) {
            throw new IllegalStateException("该借用记录确认归还失败，请稍后重试");
        }
        int reservationRows = reservationMapper.updateStatus(id, ReservationStatus.COMPLETED);
        if (reservationRows != 1) {
            throw new IllegalStateException("该借用记录所对应的预约状态更新失败，请稍后重试");
        }

        int deviceRows;
        // 查看用户有没有提交该借出单对应的设备损坏单
        int faultRecord = faultRecordMapper.countUnresolvedByBorrowRecordId(borrowRecord.getId());
        if (faultRecord > 0) {  // 如果有，设备状态变为不可用
            deviceRows = deviceMapper.updateStatus(device.getId(), DeviceStatus.DAMAGED);
        } else { // 如果没有，设备状态变为可用
            deviceRows = deviceMapper.updateStatus(device.getId(), DeviceStatus.AVAILABLE);
        }

        if (deviceRows != 1) {
            throw new IllegalStateException("该借用记录对应的设备状态更新失败，请稍后再试");
        }

        operationLogService.record(
                BusinessType.RESERVATION,
                id,
                operatorId,
                OperationType.CONFIRM_RETURN,
                ReservationStatus.RETURN_PENDING,
                ReservationStatus.COMPLETED
        );

        log.info(
                "设备确认归还, reservationId={}, operatorId={}, deviceId={}, nextDeviceStatus={}",
                id,
                operatorId,
                device.getId(),
                faultRecord > 0 ? DeviceStatus.DAMAGED : DeviceStatus.AVAILABLE
        );
    }

    // 用户申请归还
    @Transactional
    public void returnPending(Integer id, Integer accountId) {
        Reservation reservation = reservationService.getReturnableUserReservationById(accountId, id);
        Device device = deviceService.getDeviceById(reservation.getDeviceId());
        BorrowRecord record = borrowMapper.selectByReservationId(id);

        if (device.getStatus() != DeviceStatus.BORROWED) {
            throw new IllegalArgumentException("当前设备状态不可归还");
        }

        LocalDateTime requestedAt = LocalDateTime.now();
        Boolean overdue = requestedAt.isAfter(record.getDueAt());

        int borrowRows = borrowMapper.requestReturn(id, requestedAt, overdue);
        if (borrowRows != 1) {
            throw new IllegalStateException("该借用记录申请归还失败，请稍后重试");
        }

        int reservationRows = reservationMapper.updateStatus(id, ReservationStatus.RETURN_PENDING);
        if (reservationRows != 1) {
            throw new IllegalStateException("该借用记录所对应的预约状态更新失败，请稍后重试");
        }

        operationLogService.record(
                BusinessType.RESERVATION,
                id,
                accountId,
                OperationType.REQUEST_RETURN,
                ReservationStatus.BORROWED,
                ReservationStatus.RETURN_PENDING
        );

        log.info(
                "学生申请归还, reservationId={}, accountId={}, overdue={}",
                id,
                accountId,
                overdue
        );
    }
}
