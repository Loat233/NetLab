package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.*;
import org.lostwind.netlab.entity.BorrowRecord;

import java.util.List;

@Mapper
public interface BorrowMapper {
    @Select("select * from borrow_record")
    List<BorrowRecord> selectAll();

    @Select("select * from borrow_record where reservation_id = #{reservationId}")
    BorrowRecord selectByReservationId(Integer reservationId);

    @Insert("""
        insert into borrow_record (borrow_code,reservation_id,borrow_operator_id,return_operator_id,borrowed_at,due_at,requested_at,return_at,status,damaged,remark)
            values (#{borrowCode},#{reservationId},#{borrowOperatorId},#{returnOperatorId},#{borrowedAt},#{dueAt},#{requestedAt},#{returnAt},#{status},#{damaged},#{remark})
    """)
    int insert(BorrowRecord record);

    @Update("""
        update borrow_record
        set requested_at = now(),
            status = 'RETURN_PENDING'
        where reservation_id = #{reservationId}
          and status = 'BORROWED'
    """)
    int requestReturn(int reservationId);

    @Update("""
        <script>
            update borrow_record
            set return_operator_id = #{operatorId},
                return_at = now(),
                <if test="remark != null and remark != ''">
                    remark = #{remark},
                </if>
                status = 'RETURNED'
            where reservation_id = #{reservationId}
                and status = 'RETURN_PENDING'
            </script>
    """)
    int confirmReturnByReservationId(Integer reservationId, Integer operatorId, String remark);
}
