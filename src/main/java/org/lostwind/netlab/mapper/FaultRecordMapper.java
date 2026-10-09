package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.*;
import org.lostwind.netlab.entity.FaultRecord;
import org.lostwind.netlab.enums.FaultStatus;

import java.util.List;

@Mapper
public interface FaultRecordMapper {
    @Select("select * from device_fault_record order by reported_at desc")
    List<FaultRecord> selectALl();

    @Select("select * from device_fault_record where id = #{id}")
    FaultRecord selectById(Integer id);

    @Select("select * from device_fault_record where reporter_id = #{id} order by reported_at desc")
    List<FaultRecord> selectByReporterId(Integer id);

    @Select("select * from device_fault_record where borrow_record_id = #{id} order by reported_at desc")
    FaultRecord selectByBorrowRecordId(Integer id);

    @Select("select count(*) from device_fault_record where borrow_record_id = #{id} and status in ('PENDING', 'PROCESSING')")
    int countUnresolvedByBorrowRecordId(Integer id);

    @Select("select count(*) from device_fault_record where device_id = #{id} and status in ('PENDING', 'PROCESSING')")
    int countUnresolvedByDeviceId(Integer id);

    @Insert("""
        insert into device_fault_record (device_id, reporter_id, borrow_record_id, status, description, reported_at, resolved_at)
        values (#{deviceId}, #{reporterId}, #{borrowRecordId}, #{status}, #{description}, #{reportedAt}, #{resolvedAt})
    """)
    int insert(FaultRecord record);

    @Update("update device_fault_record set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Integer id , @Param("status") FaultStatus status);

    @Update("update device_fault_record set status = 'RESOLVED', resolved_at = now() where id = #{id} and status = 'PROCESSING'")
    int resolve(@Param("id") Integer id);

}
