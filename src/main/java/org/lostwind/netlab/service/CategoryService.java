package org.lostwind.netlab.service;

import org.lostwind.netlab.entity.DeviceCategory;
import org.lostwind.netlab.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    @Autowired
    CategoryMapper mapper;

    public List<DeviceCategory> getAllCategories() {
        return mapper.selectAll();
    }
}
