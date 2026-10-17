package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.*;
import org.lostwind.netlab.entity.OperationLog;
import org.lostwind.netlab.enums.BusinessType;

import java.util.List;

@Mapper
public interface OperationLogMapper {
    @Insert("""
        insert into operation_log (
            business_type,
            business_id,
            operator_id,
            operation,
            previous_status,
            next_status,
            created_at
        ) values (
            #{businessType},
            #{businessId},
            #{operatorId},
            #{operation},
            #{previousStatus},
            #{nextStatus},
            now()
        )
    """)
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(OperationLog log);

    @Select("""
        select * from operation_log
        where business_type = #{businessType}
            and business_id = #{businessId}
        order by created_at, id asc
    """)
    List<OperationLog> selectByBusiness(@Param("businessType") BusinessType type,  @Param("businessId") Integer id);
}
