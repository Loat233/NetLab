package org.lostwind.netlab.entity;

import lombok.Data;
import org.lostwind.netlab.enums.BorrowStatus;

import java.time.LocalDateTime;

@Data
public class BorrowRecord {
    Integer id;
    String borrowCode;

    Integer reservationId;
    Integer borrowOperatorId;
    Integer returnOperatorId;

    LocalDateTime borrowedAt;   // 确认借出时间
    LocalDateTime dueAt;        // 应归还时间
    LocalDateTime requestedAt;  // 申请归还时间
    LocalDateTime returnAt;     // 管理员确认归还时间

    BorrowStatus status;

    String remark;  // 记录
}
