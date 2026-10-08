package org.lostwind.netlab.enums;

import lombok.Getter;

@Getter
public enum ReservationStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
    CANCELLED("已取消"),
    BORROWED("已借出"),
    RETURN_PENDING("待确认归还"),
    COMPLETED("已完成");

    final String label;

    private ReservationStatus(String label) {
        this.label = label;
    }
}
