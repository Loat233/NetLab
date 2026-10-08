package org.lostwind.netlab.entity;

import lombok.Data;
import org.lostwind.netlab.enums.ReservationStatus;

import java.time.LocalDateTime;

@Data
public class Reservation {
    private Integer id;
    private String reservationCode;

    private Integer applicantId;
    private Integer deviceId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;

    private ReservationStatus status;

    private Integer reviewerId;
    private LocalDateTime reviewTime;
    private String rejectReason;
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
