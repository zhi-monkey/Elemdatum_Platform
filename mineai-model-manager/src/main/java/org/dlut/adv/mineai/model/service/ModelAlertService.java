package org.dlut.adv.mineai.model.service;

import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.ModelAlert;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.repository.ModelAlertRepo;
import org.dlut.adv.mineai.model.repository.ModelRepo;
import org.dlut.adv.mineai.model.repository.MonitorRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelAlertService {

    @Resource
    ModelAlertRepo modelAlertRepo;

    @Resource
    private ModelRepo modelRepo;

    @Resource
    private MonitorRepo monitorRepo;


    public void saveModelAlert(ModelAlert modelAlert) {
        modelAlertRepo.save(modelAlert);
    }

    public Page<ModelAlert> findAll(Pageable pageable) {
        return modelAlertRepo.findAll(pageable);
    }

//    public Page<ModelAlert> findModelAlertsByMonitorName(Pageable pageable, String monitorName) {
//        return modelAlertRepo.findModelAlertsByMonitorName(pageable, monitorName);
//    }
//
//    public Page<ModelAlert> findModelAlertsByModelName(Pageable pageable, String modelName) {
//        return modelAlertRepo.findModelAlertsByModelName(pageable, modelName);
//    }

    public Page<ModelAlert> findModelAlertsByMonitorNameAndModelName(Pageable pageable, String monitorName, String modelName) {
        return modelAlertRepo.findModelAlertsByMonitorNameAndModelName(pageable, monitorName, modelName);
    }

    public List<ModelAlert> findModelAlertsByMonitorNamesAndModelNames(Pageable pageable, List<String> monitorNames, List<String> modelNames) {
        List<ModelAlert> subsystemModelAlertList = modelAlertRepo.findSubsystemModelAlertsByMonitorNamesAndModelNames(monitorNames, modelNames);
        List<ModelAlert> modelAlertList = modelAlertRepo.findModelAlertsByMonitorNamesAndModelNames(monitorNames, modelNames);
        modelAlertList.addAll(subsystemModelAlertList);
        Collections.sort(modelAlertList,
                Comparator.comparingLong(
                        ModelAlert::getId).reversed());
        return modelAlertList;
    }

//    public Page<ModelAlert> tempFindModelAlerts(Pageable pageable, String monitorName, String modelName, String startTime, String endTime, int status) {
//        if (StringUtils.isNotBlank(monitorName)) {
//            return modelAlertRepo.findModelAlertsByMonitorName(pageable, monitorName);
//        } else if (StringUtils.isNotBlank(modelName)) {
//            return modelAlertRepo.findModelAlertsByMonitorName(pageable, modelName);
//        } else if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
//            return modelAlertRepo.findModelAlertsByCreateTimeBetweenAndIsDelete(startTime, endTime, ModelAlert.UNDELETED, pageable);
//        } else if (status != 0) {
//            return modelAlertRepo.findModelAlertsByStatusAndIsDelete(status, ModelAlert.UNDELETED, pageable);
//        } else {
//            return modelAlertRepo.findAll(pageable);
//        }
//
//    }

    /**
     * 删除警报信息(假删除)
     *
     * @param modelAlert
     * @return
     */

    public boolean deleteModelAlert(ModelAlert modelAlert) {
        modelAlert.setIsDelete(ModelAlert.DELETED);
        return modelAlertRepo.save(modelAlert) != null;
    }

    public List<ModelAlert> getAllModelAlert(int isDelete) {
        return modelAlertRepo.findModelAlertsByIsDelete(isDelete);
    }

    /**
     * 警报信息上报界面的时间柱状图
     */
    public Map<String, List<String>> getRecentBarNumOfSource(int dateNum) {
        Map<String, List<String>> mapResult = new HashMap<>(4);
        List<String> alertNumList = new ArrayList<>();
        List<String> dateList = new ArrayList<>();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd");

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();

        for (int i = 0; i < dateNum; i++) {
            int boundAlertNum = modelAlertRepo.countBarAlertByDate(today);

            alertNumList.add(String.valueOf(boundAlertNum));
            dateList.add(simpleDateFormat.format(today));

            calendar.add(Calendar.DATE, -1);
            today = calendar.getTime();
        }

        Collections.reverse(dateList);
        Collections.reverse(alertNumList);

        mapResult.put("dateList", dateList);
        mapResult.put("alertNumList", alertNumList);


        return mapResult;

    }

    /**
     * 查询最近num条数据
     *
     * @param num 查询条数
     * @return List<ModelAlert> 查询结果
     */
    public List<ModelAlert> findModelAlertListByDataNumAndDesc(int isDelete, int num) {
        return modelAlertRepo.findModelAlertListByDataNumAndDesc(isDelete, num).stream().filter(modelAlert -> modelAlert.getCharacteristic() == ModelAlert.IS_ALERT).collect(Collectors.toList());
    }

    /**
     * 查询所有有报警信息记录的monitorName
     */
    public List<String> getMonitorNameWithAlert() {
        List<String> monitorNames = new ArrayList<>();
        List<Long> centerMonitorIds = modelAlertRepo.findDistinctMonitorIdWithModelAlert(ModelAlert.UNDELETED, ModelAlert.IS_ALERT);
        List<String> centerMonitorNames = centerMonitorIds.stream()
                .map(id -> Optional.ofNullable(monitorRepo.findMonitorById(id))
                        .map(Monitor::getMonitorName)
                        .orElse(" "))
                .filter(name -> !Objects.equals(name, " "))
                .collect(Collectors.toList());
        List<String> subMonitorNames = modelAlertRepo.findDistinctMonitorNameWithModelAlert(ModelAlert.UNDELETED, ModelAlert.IS_ALERT);
        monitorNames.addAll(centerMonitorNames);
        monitorNames.addAll(subMonitorNames);
        return monitorNames;
    }

    /**
     * 查询所有有报警信息记录的modelName
     */
    public List<String> getModelNameWithAlert() {
        List<String> modelNames = new ArrayList<>();
        List<Long> centerModelIds = modelAlertRepo.findDistinctModelIdWithModelAlert(ModelAlert.UNDELETED, ModelAlert.IS_ALERT);
        List<String> centerModelNames = centerModelIds.stream()
                .map(id -> Optional.ofNullable(modelRepo.findModelById(id))
                        .map(Model::getModelName)
                        .orElse(" "))
                .filter(name -> !Objects.equals(name, " "))
                .collect(Collectors.toList());
        List<String> subModelNames = modelAlertRepo.findDistinctModelNameWithModelAlert(ModelAlert.UNDELETED, ModelAlert.IS_ALERT);
        modelNames.addAll(centerModelNames);
        modelNames.addAll(subModelNames);
        return modelNames;
    }

    /**
     * 获得报警信息各类型数量
     */
    public int getImageModelAlertNum() {
        return modelAlertRepo.findModelAlertNumsByImage();
    }

    public int getVideoModelAlertNum() {
        return modelAlertRepo.findModelAlertNumsByVideo();
    }

    public int getAudioModelAlertNum() {
        return modelAlertRepo.findModelAlertNumsByAudio();
    }

    public int getModelAlertNum() {
        return modelAlertRepo.findModelAlertNums();
    }


    /**
     * 查询所有有报警信息记录的model
     */
    public List<Model> findModelWithAlert() {
        List<ModelAlert> modelAlertList = modelAlertRepo.findModelAlertsByIsDelete(ModelAlert.UNDELETED).stream().filter(modelAlert -> modelAlert.getCharacteristic() == ModelAlert.IS_ALERT).collect(Collectors.toList());
        Set<Model> modelSet = new HashSet<>();
        for (ModelAlert modelAlert : modelAlertList) {
            Model model = modelAlert.getModel();
            if (model == null) {
                continue;
            }
            modelSet.add(model);
        }
        return new ArrayList<>(modelSet);
    }

    /**
     * 查询所有有报警信息记录的monitor
     */
    public List<Monitor> findMonitorsWithAlert() {
        List<ModelAlert> modelAlertList = modelAlertRepo.findModelAlertsByIsDelete(ModelAlert.UNDELETED).stream().filter(modelAlert -> modelAlert.getCharacteristic() == ModelAlert.IS_ALERT).collect(Collectors.toList());
        Set<Monitor> monitorSet = new HashSet<>();
        for (ModelAlert modelAlert : modelAlertList) {
            Monitor monitor = modelAlert.getMonitor();
            if (monitor == null) {
                continue;
            }
            monitorSet.add(monitor);
        }
        return new ArrayList<>(monitorSet);
    }

    /**
     * 返回近n天的报警信息
     */
    public List<ModelAlert> getModelAlertListByDate(int isDelete, int dateNum) {
        return modelAlertRepo.findModelAlertListByDateNumAndDesc(isDelete, dateNum).stream().filter(modelAlert -> modelAlert.getCharacteristic() == ModelAlert.IS_ALERT).collect(Collectors.toList());
    }

    /**
     * 返回所有未删除的异常报警信息
     */
    public List<ModelAlert> getModelAlertList(int isDelete) {
        return modelAlertRepo.findAllModelAlerts(isDelete, ModelAlert.IS_ALERT);
    }

    /**
     * 查询
     *
     * @param pageable
     * @return
     */

    public Page<ModelAlert> getModelAlertPage(Pageable pageable, String monitorName, String modelName, String startTime, String endTime, int status) {

        return modelAlertRepo.findAll(new Specification<ModelAlert>() {
                                          @Override
                                          public Predicate toPredicate(Root<ModelAlert> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                                              List<Predicate> predicates = new ArrayList<>();

                                              predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelAlert.UNDELETED));
                                              predicates.add(cb.equal(root.<Integer>get("characteristic"), ModelAlert.IS_ALERT));

                                              if (StringUtils.isNotBlank(monitorName)) {
                                                  Monitor monitor = monitorRepo.findMonitorByMonitorName(monitorName);
                                                  predicates.add(cb.or(cb.and(cb.equal(root.get("subsystem"), ModelAlert.CENTRAL_PLATFORM), cb.equal(root.get("monitor").as(Monitor.class), monitor)), cb.and(cb.notEqual(root.get("subsystem"), ModelAlert.CENTRAL_PLATFORM), cb.equal(root.get("monitorName"), monitorName))));
                                              }
                                              if (StringUtils.isNotBlank(modelName)) {
                                                  Model model = modelRepo.findModelByModelName(modelName);
                                                  predicates.add(cb.or(cb.and(cb.equal(root.get("subsystem"), ModelAlert.CENTRAL_PLATFORM), cb.equal(root.get("model").as(Model.class), model)), cb.and(cb.notEqual(root.get("subsystem"), ModelAlert.CENTRAL_PLATFORM), cb.equal(root.get("modelName"), modelName))));
                                              }
                                              if (status != 0) {
                                                  predicates.add(cb.equal(root.get("status"), status));
                                              }
                                              if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
                                                  SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                                  try {
                                                      Date st = dateFormat.parse(startTime);
                                                      Date et = dateFormat.parse(endTime);
                                                      predicates.add(cb.between(root.get("createTime").as(Date.class), st, et));
                                                  } catch (ParseException e) {
                                                      throw new RuntimeException(e);
                                                  }
                                              }

                                              cq.where(predicates.toArray(new javax.persistence.criteria.Predicate[predicates.size()]));
                                              return null;
                                          }
                                      },
                pageable);
    }


    public Msg<String> updateModelAlert(ModelAlert modelAlert) {
        try {
            ModelAlert newModelAlert = modelAlertRepo.findModelAlertById(modelAlert.getId());
            newModelAlert.setStatus(modelAlert.getStatus());
            modelAlertRepo.save(newModelAlert);
        } catch (Exception e) {
            return new Msg<>(MsgCode.UPDATE_MODEL_ALERT_FAILED);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    /**
     * 查询所有和传入的monitor相关的报警信息里面的model
     */
    public List<String> findModelByMonitorWithAlert(long monitorId) {
        List<ModelAlert> modelAlertList = modelAlertRepo.findModelAlertsByMonitor(monitorId);
        Set<String> modelList = new TreeSet<>();
        for (ModelAlert modelAlert : modelAlertList) {
            Model model = modelAlert.getModel();
            if (model != null) {
                modelList.add(model.getModelName());
            }
        }
        return new ArrayList<>(modelList);
    }

    public ModelAlert findModelAlertById(Long id) {
        return modelAlertRepo.findModelAlertById(id);
    }

    public boolean deleteModelAlertById(Long id) {
        ModelAlert modelAlert = modelAlertRepo.findModelAlertById(id);
        modelAlert.setIsDelete(ModelAlert.DELETED);
        return modelAlertRepo.save(modelAlert) != null;
    }

    /**
     * 未存入数据库的监控设备，根据monitorName字段查询相关的报警信息和model信息
     */
    public List<String> findModelNameByMonitorString(String monitorName) {
        List<ModelAlert> modelAlertList = modelAlertRepo.findModelAlertsByMonitorName(monitorName);
        System.err.println(modelAlertList);
        Set<String> modelSet = new TreeSet<>();
        for (ModelAlert modelAlert : modelAlertList) {
            String modelName = modelAlert.getModelName();
            if (modelName != null) {
                modelSet.add(modelName);
                System.err.println(modelName);
            }
        }
        return new ArrayList<>(modelSet);
    }
}
