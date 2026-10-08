package org.lostwind.netlab.service;

import org.lostwind.netlab.entity.Laboratory;
import org.lostwind.netlab.mapper.LaboratoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LaboratoryService {
    @Autowired
    LaboratoryMapper mapper;

    public List<Laboratory> getAllLaboratories() {
        return mapper.selectAll();
    }
}
