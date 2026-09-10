//package org.dlut.adv.mineai.model.service;
//
//import org.dlut.adv.mineai.core.entity.Model;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//@SpringBootTest
//class ModelServiceTest {
//    @Autowired
//    private ModelService modelService;
//    Model model = new Model();
//    Model model1 = new Model();
//    Model model2 = new Model();
//
//    @BeforeEach
//    void setUp() {
//        model.setModelName("算法100");
//        model.setModelEnglishName("model100");
//        model.setDescription("这是算法100");
//        model1.setModelEnglishName("model101");
//        model2.setModelName("算法101");
//    }
//
//    @Test
//    void getModel() {
//        List<Model> list = modelService.getModel();
//        if (list.size() == 0) {
//            System.out.println("所有算法均被删除");
//        } else {
//            int count = 0;
//            for (int i = 0; i < list.size(); i++) {
//                if (list.get(i).getIsDelete() == 0) {
//                    count++;
//                }
//            }
//            assertEquals(count, list.size());
//            System.out.println("所有未删除的算法数量为" + list.size());
//        }
//    }
//
//    @Test
//    void findModelListByDataNumAndDesc() {
//        List<Model> list = modelService.findModelListByDataNumAndDesc();
//        if (list.size() == 0) {
//            System.out.println("算法数量为0");
//        } else {
//            int count = 0;
//            for (int i = 0; i < list.size(); i++) {
//                if (list.get(i).getIsDelete() == 0) {
//                    count++;
//                }
//            }
//            assertEquals(count, list.size());
//            System.out.println("算法数量为" + list.size());
//        }
//    }
//
//    @Test
//    void saveModel() {
//        assertDoesNotThrow(() -> {
//            modelService.saveModel(model);
//        });
//    }
//
//    @Test
//    void isModelExist() {
//        modelService.saveModel(model);
//        assertTrue(modelService.isModelExist(model));
//        assertFalse(modelService.isModelExist(model1));
//    }
//
//    @Test
//    void isModelDescriptionExist() {
//        modelService.saveModel(model);
//        assertTrue(modelService.isModelDescriptionExist(model));
//        assertFalse(modelService.isModelDescriptionExist(model1));
//    }
//
//    @Test
//    void isModelEnglishNameExist() {
//        modelService.saveModel(model);
//        assertTrue(modelService.isModelEnglishNameExist(model));
//        assertFalse(modelService.isModelEnglishNameExist(model1));
//    }
//
//    @Test
//    void updateModel() {
//        modelService.saveModel(model);
//        assertEquals("成功", modelService.updateModel(model).getText());
//        modelService.saveModel(model1);
//        assertEquals("算法名称为空", modelService.updateModel(model1).getText());
//        modelService.saveModel(model2);
//        assertEquals("算法描述为空", modelService.updateModel(model2).getText());
//    }
//
//    @Test
//    void findModelById() {
//        modelService.saveModel(model);
//        assertEquals(modelService.findModelById(modelService.findModelByName("算法100").getId()).getModelEnglishName(), "model100");
//        assertNull(modelService.findModelById(101));
//    }
//
//    @Test
//    void findModelByName() {
//        model1.setModelName("测试1");
//        modelService.saveModel(model);
//        assertEquals(model.getModelEnglishName(), "model100");
//        assertNull(modelService.findModelByName("测试1"));
//    }
//
//    @Test
//    void findModelIdByMonitorId() {
//
//    }
//
//    @Test
//    void findModelsByMonitorId() {
//
//    }
//
//    @Test
//    void findModelMineServiceByModelId() {
//    }
//
//    @Test
//    void deleteModelMineServiceByModelId() {
//
//    }
//
//    //软删除，被删除的数据仍然存在
//    @Test
//    void deleteModel() {
//        modelService.saveModel(model);
//        boolean a = modelService.deleteModel(modelService.findModelByName("算法100").getId());
//        assertTrue(a);
//        System.out.println("算法名称为算法100的算法被软删除");
//        boolean b = modelService.deleteModel(100L);
//        assertFalse(b);
//        System.out.println("id为100的算法软删除失败");
//    }
//}