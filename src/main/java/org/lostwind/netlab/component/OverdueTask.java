package org.lostwind.netlab.component;

import org.lostwind.netlab.mapper.BorrowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OverdueTask {
    @Autowired
    BorrowMapper borrowMapper;

    @Scheduled(fixedDelay = 60000)
    public void markOverdueRecords() {
        borrowMapper.markOverdueRecords();
    }
}
