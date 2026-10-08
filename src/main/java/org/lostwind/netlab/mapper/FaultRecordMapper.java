package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lostwind.netlab.entity.FaultRecord;

@Mapper
public interface FaultRecordMapper {
    @Select("select count(*) from device_fault_record where borrow_record_id = #{id} and status in ('PENDING', 'PROCESSING')")
    int countUnsolvedByBorrowRecordId(Integer id);

    @Insert("""
        insert into device_fault_record (device_id, reporter_id, borrow_record_id, status, description, reported_at, resolved_at) 
        values (#{deviceId}, #{reporterId}, #{borrowRecordId}, #{status}, #{description}, #{reportedAt}, #{resolvedAt})
    """)
    int insert(FaultRecord record);
}
