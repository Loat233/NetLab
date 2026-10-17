package org.lostwind.netlab.service;

import lombok.extern.slf4j.Slf4j;
import org.lostwind.netlab.entity.Device;
import org.lostwind.netlab.enums.BusinessType;
import org.lostwind.netlab.enums.DeviceStatus;
import org.lostwind.netlab.enums.OperationType;
import org.lostwind.netlab.mapper.DeviceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class DeviceService {
    @Autowired
    DeviceMapper mapper;
    @Autowired
    OperationLogService operationLogService;

    // 条件查询设备
    public List<Device> getDeviceList(String keyword, Integer categoryId, Integer laboratoryId, DeviceStatus status) {
        return mapper.selectByCondition(keyword, categoryId, laboratoryId, status);
    }

    // 查询设备详情
    public Device getDeviceById(int id){
        Device device = mapper.selectById(id);
        if (device == null) {
            throw new IllegalArgumentException("设备不存在");
        }
        return device;
    }

    // 新增设备
    @Transactional
    public void addDevice(Device device, Integer operatorId) {
        Device existing = mapper.selectByAssetCode(device.getAssetCode());

        if (existing != null) {
            throw new IllegalArgumentException("设备的资产编号已存在");
        }
        device.setStatus(DeviceStatus.AVAILABLE);

        int row = mapper.insert(device);
        if (row != 1) {
            throw new IllegalStateException("新增设备失败");
        }

        operationLogService.record(
                BusinessType.DEVICE,
                device.getId(),
                operatorId,
                OperationType.ADD_DEVICE,
                null,
                DeviceStatus.AVAILABLE
        );

        log.info(
                "添加新设备, deviceId={}, operatorId={}",
                device.getId(),
                operatorId
        );
    }

    // 修改设备状态
    @Transactional
    public void updateDevice(Device device, Integer operatorId) {
        int id = device.getId();

        // 判断是否存在目标设备
        Device old = getDeviceById(id);

        // 判断更新后的资产编号是否存在
        Device existing = mapper.selectByAssetCode(device.getAssetCode());
        if (existing != null && existing.getId() != id) {
            throw new IllegalArgumentException("设备的资产编号已存在");
        }

        int row = mapper.update(device);
        if (row != 1) {
            throw new IllegalStateException("修改设备失败");
        }

        operationLogService.record(
                BusinessType.DEVICE,
                id,
                operatorId,
                OperationType.UPDATE_DEVICE,
                old.getStatus(),
                old.getStatus()
        );

        log.info(
                "修改设备信息, deviceId={}, operatorId={}",
                device.getId(),
                operatorId
        );
    }

    // 修改设备状态
    public void updateStatus(int id, DeviceStatus status) {
        Device device = getDeviceById(id);

        if (device.getStatus() == DeviceStatus.BORROWED
                && status == DeviceStatus.OFFLINE) {
            throw new IllegalArgumentException(
                    "借出中的设备不能直接修改状态"
            );
        }
        int rows = mapper.updateStatus(id, status);

        if (rows != 1) {
            throw new IllegalStateException("设备状态修改失败");
        }
    }
}
