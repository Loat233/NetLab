package org.lostwind.netlab.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.lostwind.netlab.entity.DashboardStatistics;
import org.lostwind.netlab.entity.Device;

import java.util.List;

@Mapper
public interface DashboardMapper {
    @Select("""
        select 
            (select count(*) from reservation) as total_reservations,
            (select count(*) from device where status = 'AVAILABLE') as available_devices,
            (select count(*) from reservation where status in ('BORROWED', 'RETURN_PENDING')) as borrowed_reservations,
            (select count(*) from reservation where status in ('PENDING', 'RETURN_PENDING')) as unsolved_reservations
    """)
    DashboardStatistics selectAdminDashboardStats();

    @Select("""
        select 
            (select count(*) from reservation where applicant_id = #{id}) as total_reservations,
            (select count(*) from device where status = 'AVAILABLE') as available_devices,
            (select count(*) from reservation where status in ('BORROWED', 'RETURN_PENDING') and applicant_id = #{id}) as borrowed_reservations,
            (select count(*) from reservation where status in ('PENDING', 'RETURN_PENDING') and applicant_id = #{id}) as unsolved_reservations
    """)
    DashboardStatistics selectUserDashboardStats(@Param("id") Integer id);

    @Select("select * from device where status = 'AVAILABLE' order by id desc limit 5")
    List<Device> selectAvailablePreview();
}
