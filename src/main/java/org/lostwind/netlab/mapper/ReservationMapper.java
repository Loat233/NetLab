package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.*;
import org.lostwind.netlab.entity.Reservation;
import org.lostwind.netlab.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper {
    @Select("select * from reservation order by created_at desc")
    List<Reservation> selectAll();

    @Select("select * from reservation where id = #{id}")
    Reservation selectById(Integer id);

    @Select("select * from reservation where applicant_id = #{applicantId} order by created_at desc")
    List<Reservation> selectByApplicantId(Integer applicantId);

    // 用于判断是否存在时间冲突的预约
    @Select("""
        select COUNT(*)
        from reservation
        where device_id = #{deviceId}
          and status in (
              'APPROVED',
              'BORROWED',
              'RETURN_PENDING'
          )
          and start_time < #{endTime}
          and end_time > #{startTime}
    """)
    int countConflict(@Param("deviceId") Integer deviceId,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    @Select("select * from reservation where status = 'PENDING' order by created_at asc, id asc")
    List<Reservation> selectPending();

    @Select("select * from reservation where status = 'APPROVED' and start_time < now() and end_time > now() order by created_at asc, id asc")
    List<Reservation> selectApproved();

    @Select("select * from reservation where status = 'RETURN_PENDING'")
    List<Reservation> selectReturnPending();

    @Select("""
        <script>
        select * from reservation
        where status in ('PENDING', 'APPROVED', 'RETURN_PENDING')
        <if test='status != null'>
        and status = #{status}
        </if>
        order by created_at asc, id asc
        </script>
    """)
    List<Reservation> selectAdminList(ReservationStatus status);

    @Insert("insert into reservation (reservation_code, applicant_id, device_id, start_time, end_time, purpose, status, reviewer_id, review_time, reject_reason, cancel_reason, created_at, updated_at) values (#{reservationCode}, #{applicantId}, #{deviceId}, #{startTime}, #{endTime}, #{purpose}, #{status}, #{reviewerId}, #{reviewTime}, #{rejectReason}, #{cancelReason}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(Reservation reservation);

    // 管理员同意预约
    @Update("""
        update reservation
        set status = 'APPROVED',
            reviewer_id = #{reviewerId},
            review_time = NOW(),
            updated_at = NOW()
        where id = #{id}
            and status = 'PENDING'
    """)
    int approve(@Param("id") Integer id,
               @Param("reviewerId") Integer reviewerId);

    // 管理员拒绝预约
    @Update("""
        <script>
            update reservation
            set status = 'REJECTED',
                reviewer_id = #{reviewerId},
                review_time = NOW(),
                <if test="reason != null and reason != ''">
                    reject_reason = #{reason},
                </if>
                updated_at = NOW()
            where id = #{id}
                and status = 'PENDING'
        </script>
    """)
    int reject(@Param("id") Integer id,
               @Param("reviewerId") Integer reviewerId,
               @Param("reason") String reason);

    // 用户取消预约
    @Update("""
        update reservation
        set status = 'CANCELLED',
            updated_at = NOW(),
            cancel_reason = #{reason}
        where id = #{id}
            and applicant_id = #{applicantId}
            and status in ('PENDING', 'APPROVED')
    """)
    int cancel(@Param("id") Integer id,
               @Param("applicantId") Integer applicantId,
               @Param("reason") String reason);

    @Update("update reservation set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") ReservationStatus status);
}
