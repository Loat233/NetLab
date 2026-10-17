package org.lostwind.netlab.service;

import org.lostwind.netlab.entity.OperationLog;
import org.lostwind.netlab.enums.BusinessType;
import org.lostwind.netlab.enums.OperationType;
import org.lostwind.netlab.mapper.OperationLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OperationLogService {
    @Autowired
    OperationLogMapper mapper;

    public void record(BusinessType businessType,
                       Integer businessId,
                       Integer operatorId,
                       OperationType operation,
                       Enum<?> previousStatus,
                       Enum<?> nextStatus) {
        OperationLog log = OperationLog
                .builder()
                .businessType(businessType)
                .businessId(businessId)
                .operatorId(operatorId)
                .operation(operation)
                .previousStatus(previousStatus == null ? null : previousStatus.name())
                .nextStatus(nextStatus.name())
                .build();

        if (mapper.insert(log) != 1) {
            throw new IllegalStateException("操作日志保存失败");
        }
    }
}
