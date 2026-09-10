//package org.dlut.adv.mineai.model.service;
//
//import org.dlut.adv.mineai.core.entity.Model;
//import org.dlut.adv.mineai.core.entity.MonitorModelConfig;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//class MonitorModelConfigServiceTest {
//    @Autowired
//    private MonitorModelConfigService monitorModelConfigService;
//    MonitorModelConfig monitorModelConfig = new MonitorModelConfig();
//
//    @BeforeEach
//    void setUp() {
//        Model model = new Model();
//        model.setId(61);
//        model.setModelName("算法61");
//        model.setModelEnglishName("model61");
//        ModelService modelService = new ModelService();
//        modelService.saveModel(model);
//        monitorModelConfig.setModel(model);
//        monitorModelConfig.setId(50);
//        monitorModelConfig.setVideoName("xcbwqidcu");
//        monitorModelConfig.setStreamUrl("rtsp://210.30.96.106:8554/model_61/monitor_60");
//    }
//
//    @Test
//    void saveMonitorModelConfig() {
////        assertDoesNotThrow(() -> monitorModelConfigService.saveMonitorModelConfig(monitorModelConfig));
//    }
//
//    @Test
//    void findMonitorModelConfigByMonitorIdAndModelId() {
//
//    }
//
//    @Test
//    void findMonitorModelConfigsByModelId() {
////        List<MonitorModelConfig> list = monitorModelConfigService.findMonitorModelConfigsByModelId(61L);
////        if (list.size() == 0) {
////            System.out.println("MonitorModelConfig中不存在id为61的算法");
////        } else {
////            assertEquals("rtsp://210.30.96.106:8554/model_61/monitor_60",list.get(0).getStreamUrl());
////        }
//    }
//
//    @Test
//    void deleteMonitorModelConfigByMonitorAndModel() {
//    }
//
//    @Test
//    void addMonitorModelConfig() {
//
//    }
//
//    @Test
//    void getMonitorModelConfigList() {
////        monitorModelConfigService.saveMonitorModelConfig(monitorModelConfig);
////        List<MonitorModelConfig> list = monitorModelConfigService.getMonitorModelConfigList();
////        if(list.size()==0){
////            System.out.println("MonitorModelConfig表为空");
////        }
////        else{
////            assertEquals(50,list.get(list.size()-1).getId());
////        }
//    }
//}