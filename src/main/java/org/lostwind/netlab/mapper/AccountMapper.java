package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lostwind.netlab.entity.Account;

@Mapper
public interface AccountMapper {
    @Select("select * from account where username = #{name}")
    Account selectByUsername(String name);

    @Select("select * from account where id = #{id}")
    Account selectById(Integer id);
}
