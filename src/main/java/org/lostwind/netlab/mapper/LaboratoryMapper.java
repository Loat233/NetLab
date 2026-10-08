package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lostwind.netlab.entity.Laboratory;

import java.util.List;

@Mapper
public interface LaboratoryMapper {
    @Select("select * from laboratory")
    List<Laboratory> selectAll();
}
