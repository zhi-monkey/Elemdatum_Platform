package org.dlut.adv.mineai.model.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PrometheusServiceTest {
    @Autowired
    private PrometheusService prometheusService;
    @Test
    public void getCpuUsageCount() {
        System.out.println("prometheusService.getCpuCount() = " + prometheusService.getCpuCount());
    }
}
