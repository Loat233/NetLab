package org.lostwind.netlab.enums;

import lombok.Getter;

@Getter
public enum FaultStatus {
    PENDING("待处理"),
    PROCESSING("维修中"),
    RESOLVED("已解决");

    private final String label;

    FaultStatus(String label) {
        this.label = label;
    }
}
