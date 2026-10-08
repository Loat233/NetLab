package org.lostwind.netlab.entity;

import lombok.Data;
import org.lostwind.netlab.enums.LaboratoryStatus;

@Data
public class Laboratory {
    int id;
    String labCode;
    String labName;
    String location;
    LaboratoryStatus status;
}
