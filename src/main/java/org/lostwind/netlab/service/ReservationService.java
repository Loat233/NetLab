package org.lostwind.netlab.service;

import org.lostwind.netlab.dto.ReservationRequest;
import org.lostwind.netlab.entity.Account;
import org.lostwind.netlab.entity.Device;
import org.lostwind.netlab.entity.Reservation;
import org.lostwind.netlab.enums.DeviceStatus;
import org.lostwind.netlab.enums.ReservationStatus;
import org.lostwind.netlab.mapper.ReservationMapper;
import org.lostwind.netlab.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {
    @Autowired
    ReservationMapper reservationMapper;
    @Autowired
    DeviceService deviceService;
    @Autowired
    AccountService accountService;

    // 根据预约单id得到预约单(管理员方法，无访问限制)
    public Reservation getReservationById(Integer reservationId) {
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("预约单不存在");
        }
        return reservation;
    }


    // 根据预约单id和账户id，得到预约单(用户方法，只访问属于当前账户的预约单)
    public Reservation getUserReservationById(Integer accountId, Integer reservationId) {
        Reservation reservation = getReservationById(reservationId);
        if (!reservation.getApplicantId().equals(accountId)) {
            throw new IllegalArgumentException("该预约单不属于当前用户，无权访问");
        }
        return reservation;
    }

    // 根据预约单id和账户id，得到预约单(用户方法，只访问属于当前账户的pending预约单和approved预约单)
    public Reservation getCancellableUserReservationById(Integer accountId, Integer reservationId) {
        Reservation reservation = getUserReservationById(accountId, reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new IllegalStateException("当前状态不能取消");
        }
        return reservation;
    }

    // 根据预约单id和账户id，得到预约单(用户方法，只访问属于当前账户的borrowed预约单)
    public Reservation getReturnableUserReservationById(Integer accountId, Integer reservationId) {
        Reservation reservation = getUserReservationById(accountId, reservationId);
        if (reservation.getStatus() != ReservationStatus.BORROWED) {
            throw new IllegalStateException("当前状态不能申请归还");
        }
        return reservation;
    }

    // 得到当前用户申请的全部预约单
    public List<Reservation> getReservationsByApplicantId(Integer applicantId) {
        return reservationMapper.selectByApplicantId(applicantId);
    }

    // 得到全部的待审核预约单
    public List<Reservation> getPendingList() {
        return reservationMapper.selectPending();
    }

    // 得到全部的批准(待确认借出)预约单
    public List<Reservation> getApprovedList() {return reservationMapper.selectApproved();}

    // 得到全部的申请归还(待确认归还)预约单
    public List<Reservation> getReturnPendingList() {return reservationMapper.selectReturnPending();}

    // 用户创建并向数据库提交预约单
    @Transactional
    public void createReservation(Integer applicantId, ReservationRequest request) {
        // 检查属性是否为空
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new IllegalArgumentException("预约时间不能为空");
        }
        // 判断是否符合 结束时间 > 开始时间 > 现在
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("开始时间不能早于当前时间");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("结束时间必须晚于开始时间");
        }

        // 获取设备并判断设备是否为空
        Device device = deviceService.getDeviceById(request.getDeviceId());

        // 判断设备状态是否为可借
        DeviceStatus status = device.getStatus();
        if (status == DeviceStatus.OFFLINE || status == DeviceStatus.DAMAGED || status == DeviceStatus.MAINTENANCE) {
            throw new IllegalArgumentException("当前设备不可预约");
        }

        // 检查是否存在时间冲突的预约
        int conflicts = reservationMapper.countConflict(request.getDeviceId(), request.getStartTime(), request.getEndTime());
        if (conflicts > 0) {
           throw new IllegalArgumentException("该时段已经被预约");
        }

        // 完善预约单
        Reservation reservation = new Reservation();
        reservation.setReservationCode(CodeGenerator.reservationCode());
        reservation.setApplicantId(applicantId);
        reservation.setDeviceId(request.getDeviceId());
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPurpose(request.getPurpose());
        reservation.setStatus(ReservationStatus.PENDING);

        reservationMapper.insert(reservation);
    }



    // 用户取消预约单
    @Transactional
    public void cancelReservation(Integer id, Integer applicantId, String reason) {
        Reservation reservation = getReservationById(id);
        if (!reservation.getApplicantId().equals(applicantId)) {
            throw new IllegalArgumentException("该预约单不属于当前用户，无权取消");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new IllegalArgumentException("当前状态不能取消");
        }

        if (reason != null && reason.length() > 500) {
            throw new IllegalArgumentException("取消原因不能超过500字");
        }

        int rows = reservationMapper.cancel(id, applicantId, reason);
        // 防止预约状态在查询后被其他请求修改
        if (rows != 1) {
            throw new IllegalStateException("预约状态已经发生改变，请重试");
        }
    }



    // 管理员批准预约
    @Transactional
    public void approveReservation(Integer reservationId, Integer reviewerId) {
        // 防止普通用户批准预约
        Account account = accountService.getAccountById(reviewerId);
        if (!account.getRole().equals("ADMIN") && !account.getRole().equals("LAD_ADMIN")) {
            throw new IllegalArgumentException("当前用户无权修改预约状态");
        }
        // 判断预约是否存在
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("该预约不存在");
        }
        // 判断预约状态
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("只有待审核预约才能批准");
        }
        // 当预约过了开始时间时，不能批准
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("预约开始时间已过，请重新申请");
        }
        // 当设备不可用时，不能批准
        Device device = deviceService.getDeviceById(reservation.getDeviceId());
        if (device.getStatus() != DeviceStatus.AVAILABLE && device.getStatus() != DeviceStatus.BORROWED) {
            throw new IllegalArgumentException("当前设备不可预约");
        }
        // 预约创建后，可能设备在其他有冲突时间的预约中被批准，因此需要重新检查冲突
        int conflicts = reservationMapper.countConflict(reservation.getDeviceId(), reservation.getStartTime(), reservation.getEndTime());
        if (conflicts > 0) {
            throw new IllegalArgumentException("该时段与其他已批准预约冲突，无法批准");
        }

        int rows = reservationMapper.approve(reservationId, reviewerId);
        // 防止预约状态在查询后被其他请求修改
        if (rows != 1) {
            throw new IllegalStateException("预约状态已经发生改变，请重试");
        }
    }

    // 管理员拒绝预约
    @Transactional
    public void rejectReservation(Integer reservationId, Integer reviewerId, String reason) {
        // 判断拒绝原因字数是否超上限
        if (reason != null && reason.length() > 500) {
            throw new IllegalArgumentException("拒绝原因不能超过500字");
        }
        // 防止普通用户批准预约
        Account account = accountService.getAccountById(reviewerId);
        if (!account.getRole().equals("ADMIN") && !account.getRole().equals("LAD_ADMIN")) {
            throw new IllegalArgumentException("当前用户无权修改预约状态");
        }
        // 判断预约是否存在
        Reservation reservation = reservationMapper.selectById(reservationId);
        if (reservation == null) {
            throw new IllegalArgumentException("该预约不存在");
        }
        // 判断预约状态
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("只有待审核预约才能拒绝");
        }

        int rows = reservationMapper.reject(reservationId, reviewerId, reason);
        // 防止预约状态在查询后被其他请求修改
        if (rows != 1) {
            throw new IllegalStateException("预约状态已经发生改变，请重试");
        }
    }
}
