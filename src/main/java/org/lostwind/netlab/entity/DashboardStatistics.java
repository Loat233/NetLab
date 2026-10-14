package org.lostwind.netlab.entity;

import lombok.Data;

import java.util.List;

@Data
public class DashboardStatistics {
    Integer totalReservations;
    Integer availableDevices;
    Integer borrowedReservations; // 用户处于借出状态的预约单数量
    Integer unsolvedReservations;  // 管理员未处理的预约单数量
    List<Device> availablePreview; // 可用设备预览列表
}
