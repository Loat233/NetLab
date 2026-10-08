package org.lostwind.netlab.enums;

import lombok.Getter;

@Getter
public enum BorrowStatus {
    BORROWED("借出中"),
    RETURN_PENDING("待确认归还"),
    RETURN("已归还");

    private final String label;

    BorrowStatus(String label) {
        this.label = label;
    }
}
