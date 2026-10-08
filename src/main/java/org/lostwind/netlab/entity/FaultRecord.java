package org.lostwind.netlab.entity;

import lombok.Data;
import org.lostwind.netlab.enums.FaultStatus;

import java.time.LocalDateTime;

@Data
public class FaultRecord {
    Integer id;
    Integer deviceId;
    Integer reporterId;
    Integer borrowRecordId;
    FaultStatus status;
    String description;
    LocalDateTime reportedAt;
    LocalDateTime resolvedAt;
}
