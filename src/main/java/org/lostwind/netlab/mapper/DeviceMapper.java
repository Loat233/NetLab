package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.*;
import org.lostwind.netlab.entity.Device;
import org.lostwind.netlab.enums.DeviceStatus;

import java.util.List;

@Mapper
public interface DeviceMapper {
    @Select("select * from device order by id desc")
    List<Device> selectAll();

    @Select("select * from device where id = #{id}")
    Device selectById(int id);

    @Select("select * from device where asset_code = #{assetCode}")
    Device selectByAssetCode(String assetCode);

    // 组合条件查询
    @Select("""
        <script>
        select * from device 
        <where>
             <if test="keyword != null and keyword != ''">
                and (
                    device_name like concat('%', #{keyword}, '%')
                    or asset_code like concat('%', #{keyword}, '%')
                    or model like concat('%', #{keyword}, '%')
                )
             </if>
             <if test="categoryId != null">
                and category_id = #{categoryId}
             </if>
             <if test="laboratoryId != null">
                and laboratory_id = #{laboratoryId}
             </if>
             <if test="status != null">
                and status = #{status}
             </if> 
        </where>
            order by id desc
        </script>
    """)
    List<Device> selectByCondition(@Param("keyword") String keyword,
                                   @Param("categoryId") Integer categoryId,
                                   @Param("laboratoryId") Integer laboratoryId,
                                   @Param("status") DeviceStatus status);


    @Insert("insert into device (asset_code, device_name, category_id, laboratory_id, model, status, description, created_at, updated_at) values (#{assetCode},#{deviceName},#{categoryId},#{laboratoryId},#{model},#{status},#{description},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(Device device);

    // 根据id更新设备的信息
    @Update("update device set asset_code = #{assetCode}, device_name = #{deviceName}, category_id = #{categoryId},laboratory_id = #{laboratoryId}, model = #{model}, description = #{description}, updated_at = NOW() where id = #{id}")
    int update(Device device);

    // 单独修改设备的状态
    @Update("update device set status = #{status}, updated_at = NOW() where id = #{id}")
    int updateStatus(@Param("id") int id, @Param("status") DeviceStatus status);

}
