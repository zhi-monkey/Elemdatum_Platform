//package org.dlut.adv.mineai.model.service;
//
//import org.dlut.adv.mineai.core.entity.Model;
//import org.dlut.adv.mineai.core.entity.Monitor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class ModelMonitorServiceTest {
//    @Autowired
//    private ModelMonitorService modelMonitorService;
//    Model model = new Model();
//    Monitor monitor = new Monitor();
//    Monitor monitor1 = new Monitor();
//
//    @BeforeEach
//    void setUp() {
//        model.setId(20);
//        model.setModelName("算法20");
//        model.setModelEnglishName("model20");
//        model.setIsDelete(0);
//        monitor.setId(30);
//        monitor.setName("摄像头30");
//        monitor.setMonitorName("monitor30");
//
//        monitor1.setId(300);
//    }
//
//    @Test
//    void save() {
//        assertTrue(modelMonitorService.save(monitor));
//        assertFalse(modelMonitorService.save(monitor1));
//    }
//
//    @Test
//    void getMonitorById() {
//        modelMonitorService.save(monitor);
//        assertEquals("摄像头30", modelMonitorService.getMonitorById(30L).getName());
//        assertNull(modelMonitorService.getMonitorById(300L));
//    }
//}