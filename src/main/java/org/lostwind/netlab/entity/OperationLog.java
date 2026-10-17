package org.lostwind.netlab.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.lostwind.netlab.enums.BusinessType;
import org.lostwind.netlab.enums.OperationType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {
    Integer id;
    BusinessType businessType;
    Integer businessId;
    Integer operatorId;
    OperationType operation;
    String previousStatus;
    String nextStatus;
    LocalDateTime createdAt;
}
