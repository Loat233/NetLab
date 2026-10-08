package org.lostwind.netlab.enums;

import lombok.Getter;

@Getter
public enum LaboratoryStatus {
    ENABLED("启用"),
    DISABLE("未启用");

    private final String label;

    LaboratoryStatus(String label) {
        this.label = label;
    }
}
