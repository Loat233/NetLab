package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.*;
import org.lostwind.netlab.entity.BorrowRecord;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BorrowMapper {
    @Select("select * from borrow_record")
    List<BorrowRecord> selectAll();

    @Select("select * from borrow_record where id = #{id}")
    BorrowRecord selectById(Integer id);

    @Select("select * from borrow_record where id = #{id} for update")
    BorrowRecord selectByIdForUpdate(Integer id);

    @Select("select * from borrow_record where reservation_id = #{reservationId}")
    BorrowRecord selectByReservationId(Integer reservationId);

    @Select("select * from borrow_record where reservation_id = #{reservationId} for update")
    BorrowRecord selectByReservationIdForUpdate(Integer reservationId);

    @Select("select * from borrow_record where overdue = 1 order by due_at asc")
    List<BorrowRecord> selectOverdueRecords();

    @Insert("""
        insert into borrow_record (borrow_code,reservation_id,borrow_operator_id,return_operator_id,borrowed_at,due_at,requested_at,return_at,status,remark)
            values (#{borrowCode},#{reservationId},#{borrowOperatorId},#{returnOperatorId},#{borrowedAt},#{dueAt},#{requestedAt},#{returnAt},#{status},#{remark})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(BorrowRecord record);

    @Update("""
        update borrow_record
        set requested_at = #{requestedAt},
            overdue = #{overdue},
            status = 'RETURN_PENDING'
        where reservation_id = #{reservationId}
          and status = 'BORROWED'
    """)
    int requestReturn(@Param("reservationId") int reservationId,
                      @Param("requestedAt") LocalDateTime requestedAt,
                      @Param("overdue") Boolean overdue);

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

    // 扫描所有的逾期记录
    @Update("""
        update borrow_record
        set overdue = 1
        where status = 'BORROWED'
            and overdue = 0
            and due_at < now()
    """)
    int markOverdueRecords();
}
