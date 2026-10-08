package org.lostwind.netlab.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.lostwind.netlab.enums.DeviceStatus;

import java.time.LocalDateTime;

@Data
public class Device {
    int id;
    String assetCode;
    String deviceName;
    Integer categoryId;
    Integer laboratoryId;
    String model;
    DeviceStatus status = DeviceStatus.AVAILABLE;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
