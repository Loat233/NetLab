package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.lostwind.netlab.entity.DeviceCategory;

import java.util.List;

@Mapper
public interface CategoryMapper {
    @Select("select * from device_category")
    List<DeviceCategory> selectAll();
}
