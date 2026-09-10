//package org.dlut.adv.mineai.model.service;
//
//import org.dlut.adv.mineai.core.entity.Model;
//import org.dlut.adv.mineai.core.entity.ModelAlert;
//import org.dlut.adv.mineai.core.entity.Monitor;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//@SpringBootTest
//class ModelAlertServiceTest {
//    @Autowired
//    private ModelAlertService modelAlertService;
//    ModelAlert modelAlert = new ModelAlert();
//
//    @BeforeEach
//    void setUp() {
//        modelAlert.setId(52);
//        modelAlert.setDescription("cdbqviwidu");
//        modelAlert.setVideo(true);
//        modelAlert.setImage(false);
//        modelAlert.setAudio(false);
//        modelAlert.setIsDelete(0);
//        modelAlert.setImagePath("a-b-c-d");
//        modelAlert.setAudioPath("a-b-c-e");
//        modelAlert.setVideoPath("a-b-c-f");
//        modelAlert.setStatus(1);
//    }
//
//    @Test
//    void saveModelAlert() {
//        assertDoesNotThrow(() -> {
//            modelAlertService.saveModelAlert(modelAlert);
//        });
//    }
//
//    //软删除，仅仅将modelAlert对象的isDelete属性置为1
//    @Test
//    void deleteModelAlert() {
//        modelAlertService.deleteModelAlert(modelAlert);
//        assertEquals(1, modelAlert.getIsDelete());
//    }
//
//    @Test
//    void getAllModelAlert() {
//        //获取所有未删除的警告
//        List<ModelAlert> list1 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        //获取所有删除的警告
//        List<ModelAlert> list2 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(1);
//        if (list1.size() == 0) {
//            System.out.println("所有警告均已被删除");
//        } else {
//            int count = 0;
//            for (int i = 0; i < list1.size(); i++) {
//                if (list1.get(i).getIsDelete() == 0) {
//                    count++;
//                }
//            }
//            assertEquals(count, list1.size());
//            System.out.println("未删除的警告数量为" + count);
//        }
//        if (list2.size() == 0) {
//            System.out.println("所有警告均未被删除");
//        } else {
//            int count = 0;
//            for (int i = 0; i < list2.size(); i++) {
//                if (list2.get(i).getIsDelete() == 1) {
//                    count++;
//                }
//            }
//            assertEquals(count, list2.size());
//            System.out.println("已删除的警告数量为" + count);
//        }
//
//    }
//
//    @Test
//    void findModelAlertListByDataNumAndDesc() {
//        List<ModelAlert> list1 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        List<ModelAlert> list2 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(1);
//        //查询最近5条数据
//        if (list1.size() < 5) {
//            System.out.println("最近的未删除警告数量不足5条");
//        } else {
//            //查询最近五条未删除的警告
//            List<ModelAlert> list3 = modelAlertService.findModelAlertListByDataNumAndDesc(0, 5);
//            int count = 0;
//            for (int i = 0; i < list3.size(); i++) {
//                if (list3.get(i).getIsDelete() == 0) {
//                    count++;
//                }
//            }
//            assertEquals(count, list3.size());
//        }
//        if (list2.size() < 5) {
//            System.out.println("最近的已删除警告数量不足5条");
//        } else {
//            //查询最近五条已删除的警告
//            List<ModelAlert> list4 = modelAlertService.findModelAlertListByDataNumAndDesc(1, 5);
//            int sum = 0;
//            for (int j = 0; j < list4.size(); j++) {
//                if (list4.get(j).getIsDelete() == 1) {
//                    sum++;
//                }
//            }
//            assertEquals(sum, list4.size());
//        }
//    }
//
//    @Test
//    void getImageModelAlertNum() {
//        List<ModelAlert> list1 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        List<ModelAlert> list2 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(1);
//        if (list1.size() != 0 || list2.size() != 0) {
//            int count = 0;
//            int sum = 0;
//            if (list1.size() != 0) {
//                for (int i = 0; i < list1.size(); i++) {
//                    if (list1.get(i).getImage().equals(true)) {
//                        count++;
//                    }
//                }
//            }
//            if (list2.size() != 0) {
//                for (int j = 0; j < list2.size(); j++) {
//                    if (list2.get(j).getImage().equals(true)) {
//                        sum++;
//                    }
//                }
//            }
//            assertEquals(count + sum, modelAlertService.getImageModelAlertNum());
//            System.out.println("图像警告的数量为" + modelAlertService.getImageModelAlertNum());
//        }
//    }
//
//    @Test
//    void getVideoModelAlertNum() {
//        List<ModelAlert> list1 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        List<ModelAlert> list2 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(1);
//        if (list1.size() != 0 || list2.size() != 0) {
//            int count = 0;
//            int sum = 0;
//            if (list1.size() != 0) {
//                for (int i = 0; i < list1.size(); i++) {
//                    if (list1.get(i).getVideo().equals(true)) {
//                        count++;
//                    }
//                }
//            }
//            if (list2.size() != 0) {
//                for (int j = 0; j < list2.size(); j++) {
//                    if (list2.get(j).getVideo().equals(true)) {
//                        sum++;
//                    }
//                }
//            }
//            assertEquals(count + sum, modelAlertService.getVideoModelAlertNum());
//            System.out.println("视频警告的数量为" + modelAlertService.getVideoModelAlertNum());
//        }
//    }
//
//    @Test
//    void getAudioModelAlertNum() {
//        List<ModelAlert> list1 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        List<ModelAlert> list2 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(1);
//        if (list1.size() != 0 || list2.size() != 0) {
//            int count = 0;
//            int sum = 0;
//            if (list1.size() != 0) {
//                for (int i = 0; i < list1.size(); i++) {
//                    if (list1.get(i).getAudio().equals(true)) {
//                        count++;
//                    }
//                }
//            }
//            if (list2.size() != 0) {
//                for (int j = 0; j < list2.size(); j++) {
//                    if (list2.get(j).getAudio().equals(true)) {
//                        sum++;
//                    }
//                }
//            }
//            assertEquals(count + sum, modelAlertService.getAudioModelAlertNum());
//            System.out.println("音频警告的数量为" + modelAlertService.getAudioModelAlertNum());
//        }
//    }
//
//    @Test
//    void getModelAlertNum() {
//        List<ModelAlert> list1 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        List<ModelAlert> list2 = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(1);
//        assertEquals(list1.size() + list2.size(), modelAlertService.getModelAlertNum());
//        System.out.println("警告数量为" + modelAlertService.getModelAlertNum());
//    }
//
//    @Test
//    void getModelAlertListByDate() {
//        //返回最近两天的报警信息
//        List<ModelAlert> list1 = modelAlertService.getModelAlertListByDate(0, 2);
//        List<ModelAlert> list2 = modelAlertService.getModelAlertListByDate(1, 2);
//        if (list1.size() == 0) {
//            System.out.println("最近两天未删除的报警信息数量为0");
//        } else {
//            System.out.println("最近两天未删除的报警信息数量为" + list1.size());
//        }
//        if (list2.size() == 0) {
//            System.out.println("最近两天删除的报警信息数量为0");
//        } else {
//            System.out.println("最近两天删除的报警信息数量为" + list2.size());
//        }
//    }
//
//    @Test
//    void findModelWithAlert() {
//        List<ModelAlert> list = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        if (list.size() == 0) {
//            System.out.println("所有警告信息均被删除");
//        } else {
//            Set<Model> set = new HashSet<>();
//            for (int i = 0; i < list.size(); i++) {
//                set.add(list.get(i).getModel());
//            }
//            assertEquals(set.size(), modelAlertService.findModelWithAlert().size());
//            System.out.println("model报警信息的数量为" + set.size());
//        }
//
//    }
//
//    @Test
//    void findMonitorsWithAlert() {
//        List<ModelAlert> list = modelAlertService.modelAlertRepo.findModelAlertsByIsDelete(0);
//        if (list.size() == 0) {
//            System.out.println("所有警告信息均被删除");
//        } else {
//            Set<Monitor> set = new HashSet<>();
//            for (int i = 0; i < list.size(); i++) {
//                set.add(list.get(i).getMonitor());
//            }
//            assertEquals(set.size(), modelAlertService.findMonitorsWithAlert().size());
//            System.out.println("monitor报警信息的数量为" + set.size());
//        }
//    }
//}