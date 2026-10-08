package org.lostwind.netlab.enums;

import lombok.Getter;

@Getter
public enum DeviceStatus {
    AVAILABLE("可用"),
    BORROWED("已借出"),
    DAMAGED("损坏"),
    MAINTENANCE("维修中"),
    OFFLINE("下架");

    private final String label;

    DeviceStatus(String label) {
        this.label = label;
    }
}
