package org.lostwind.netlab.service;

import org.lostwind.netlab.entity.DashboardStatistics;
import org.lostwind.netlab.mapper.DashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    @Autowired
    DashboardMapper mapper;

    public DashboardStatistics getAdminDashboard() {
        DashboardStatistics statistics = mapper.selectAdminDashboardStats();
        statistics.setAvailablePreview(mapper.selectAvailablePreview());
        return statistics;
    }

    public DashboardStatistics getUserDashboard(Integer id) {
        DashboardStatistics statistics = mapper.selectUserDashboardStats(id);
        statistics.setAvailablePreview(mapper.selectAvailablePreview());
        return statistics;
    }
}

