package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.*;
import org.lostwind.netlab.entity.Review;

import java.util.List;

@Mapper
public interface ReviewMapper {
    @Select("""
        select r.id, r.account_id, r.content, a.username
        from review r
        join account a on a.id = r.account_id
        order by r.id desc
        limit 20
    """)
    List<Review> selectAll();

    @Insert("insert into review (account_id, content) values (#{accountId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Review review);

    @Delete("delete from review where id = #{reviewId} and account_id = #{accountId}")
    int delete(@Param("reviewId") Integer reviewId, @Param("accountId") Integer accountId);
}
