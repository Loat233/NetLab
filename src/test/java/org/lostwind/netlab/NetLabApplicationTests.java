package org.lostwind.netlab;

import org.junit.jupiter.api.Test;
import org.lostwind.netlab.mapper.DeviceMapper;
import org.lostwind.netlab.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NetLabApplicationTests {
    @Autowired
    DeviceMapper mapper;

    @Test
    void contextLoads() {
    }
}
