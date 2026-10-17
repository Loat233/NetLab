package org.lostwind.netlab.component;

import lombok.extern.slf4j.Slf4j;
import org.lostwind.netlab.mapper.BorrowMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OverdueTask {
    @Autowired
    BorrowMapper borrowMapper;

    @Scheduled(fixedDelay = 60000)
    public void markOverdueRecords() {
        int rows = borrowMapper.markOverdueRecords();
        if (rows > 0) {
            log.info("逾期扫描完成, 本次新增逾期记录数={}", rows);
        } else {
            log.debug("逾期扫描完成, 没有新增逾期记录");
        }
    }
}
