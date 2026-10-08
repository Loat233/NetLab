package org.lostwind.netlab.service;

import org.lostwind.netlab.mapper.FaultRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FaultRecordService {
    @Autowired
    FaultRecordMapper faultRecordMapper;

    // 用户创建并向数据库提交设备损坏单
    @Transactional
    public void createFaultRecord(Integer borrowRecordId, Integer reporterId, String description) {
        // 当用户申请归还、管理员确认归还时再修改设备状态为损坏

    }
}
