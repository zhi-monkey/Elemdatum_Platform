package org.dlut.adv.mineai.model.controller;

import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.ModelAlert;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.ModelAlertService;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/modelAlert")
public class ModelAlertController {

    @Resource
    private ModelAlertService modelAlertService;

//    /**
//     * 分页查询
//     *
//     * @param page
//     * @param pageSize
//     * @return
//     */
//    @GetMapping("/dynamicFindModelAlert")
//    public Msg<Page<ModelAlert>> getAlertList(@RequestParam(defaultValue = "0") int page,
//                                              @RequestParam(defaultValue = "desc") String order,
//                                              @RequestParam(defaultValue = "15") int pageSize, String modelName, String monitorName, String startTime, String endTime, @RequestParam(defaultValue = "0") int status) {
//        String end = "end";
//        String originMonitorName = "-1";
//        String originModelName = "-1";
//        if (order.contains(end)) {
//            order = order.substring(0, order.indexOf("end"));
//        }
//        if (monitorName.equals(originMonitorName) && modelName.equals(originModelName)) {
//            Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
//            Page<ModelAlert> modelAlertPage = modelAlertService.findAll(pageable);
//            return new Msg<>(MsgCode.SUCCEED, modelAlertPage);
//        } else if (!monitorName.equals(originMonitorName) && modelName.equals(originModelName)) {
//            Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
//            Page<ModelAlert> modelAlertPage = modelAlertService.findModelAlertsByMonitorName(pageable, monitorName);
//            return new Msg<>(MsgCode.SUCCEED, modelAlertPage);
//        } else if (!modelName.equals(originModelName) && monitorName.equals(originMonitorName)) {
//            Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
//            Page<ModelAlert> modelAlertPage = modelAlertService.findModelAlertsByModelName(pageable, modelName);
//            return new Msg<>(MsgCode.SUCCEED, modelAlertPage);
//        } else {
//            Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
//            Page<ModelAlert> modelAlertPage = modelAlertService.findModelAlertsByMonitorNameAndModelName(pageable, monitorName, modelName);
//            return new Msg<>(MsgCode.SUCCEED, modelAlertPage);
//        }
//    }

    /**
     * 获得所有未删除的警告信息列表
     */
    @RequestMapping("/getAllModelAlert")
    public Msg<List<ModelAlert>> getAllModelAlert() {
        List<ModelAlert> modelAlerts = modelAlertService.getAllModelAlert(ModelAlert.UNDELETED);
        Collections.reverse(modelAlerts);
        return new Msg<>(MsgCode.SUCCEED, modelAlerts);
    }

    // 最多显示模型报警信息的条数
    private static final int MAX_MODEL_ALERT_NUM = 50;

    /**
     * 总览页面：获得最新的MAX_MODEL_ALERT_NUM条警告信息
     */
    @RequestMapping("/getModelAlertListByDataNum")
    public Msg<List<ModelAlert>> getModelAlertByDataNum() {
        List<ModelAlert> modelAlerts = modelAlertService.findModelAlertListByDataNumAndDesc(Model.NOT_DELETE, MAX_MODEL_ALERT_NUM);
        Collections.reverse(modelAlerts);
        return new Msg<>(MsgCode.SUCCEED, modelAlerts);

    }

    @RequestMapping("getDataBar")
    public Msg<Map<String, List<String>>> getDataBar(@RequestParam int dateNum) {
        return new Msg<>(MsgCode.SUCCEED, modelAlertService.getRecentBarNumOfSource(dateNum));
    }

    /**
     * description: 删除报警信息
     *
     * @author: dingxinpeng
     * @date: 2022/11/17 0:45
     * @Param null:
     * @return: null
     */
    @RequestMapping("/deleteModelAlert")
    public Msg<String> deleteModelAlert(@RequestBody ModelAlert modelAlert) {
        boolean status = modelAlertService.deleteModelAlert(modelAlert);
        if (status) {
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.DELETE_MODEL_ALERT_FAILED);
        }
    }

    /**
     * 批量删除modelAlert
     */
    @RequestMapping("/deleteModelAlertList")
    public Msg<String> deleteModelAlert(@RequestBody JSONObject modelAlertLists) {
        String str = (String) modelAlertLists.get("modelAlertLists");
        String[] modelAlertArray = str.split(" ");
        for (String strId : modelAlertArray) {
            Long id = Long.valueOf(strId);
            if (!modelAlertService.deleteModelAlertById(id)) {
                return new Msg<>(MsgCode.FAILED);
            }
        }

        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/updateModelAlertList")
    public Msg<String> updateModelAlertList(@RequestBody JSONObject modelAlertLists) {
        String str = (String) modelAlertLists.getJSONObject("modelAlertLists").get("ids");
        int status = (int) modelAlertLists.getJSONObject("handle").get("status");
        String[] modelAlertArray = str.split(" ");
        for (String strId : modelAlertArray) {
            Long id = Long.valueOf(strId);
            ModelAlert modelAlert = modelAlertService.findModelAlertById(id);
            modelAlert.setStatus(status);
            modelAlertService.saveModelAlert(modelAlert);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/updateAllModelAlert")
    public Msg<String> updateAllModelAlert(@RequestBody JSONObject modelAlertLists) {
        int status = (int) modelAlertLists.getJSONObject("handle").get("status");
        List<ModelAlert> modelAlerts = modelAlertService.getAllModelAlert(ModelAlert.UNDELETED);
        for(ModelAlert modelAlert : modelAlerts){
            modelAlert.setStatus(status);
            modelAlertService.saveModelAlert(modelAlert);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }
    /**
     * 各个报警信息类型数量
     */
    @RequestMapping("/getModelAlertDataType")
    public Msg<Map<String, Integer>> getModelAlertDataType() {
        Map<String, Integer> map = new HashMap<>(4);
        Integer imageNum = modelAlertService.getImageModelAlertNum();
        Integer audioNum = modelAlertService.getAudioModelAlertNum();
        Integer videoNum = modelAlertService.getVideoModelAlertNum();
        Integer modelAlertNum = modelAlertService.getModelAlertNum();
        map.put("image", imageNum);
        map.put("audio", audioNum);
        map.put("video", videoNum);
        map.put("modelAlertNum", modelAlertNum);
        return new Msg<>(MsgCode.SUCCEED, map);
    }

    @GetMapping("/dynamicModelAlert")
    public Msg<Page<ModelAlert>> getModelAlertList(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "desc") String order,
                                                   @RequestParam(defaultValue = "15") int pageSize, @RequestParam(name = "modelNames", defaultValue = "[]") List<String> modelNames, @RequestParam(name = "monitorNames", defaultValue = "[]") List<String> monitorNames) {
        String end = "end";
        String originMonitorName = "-1";
        String originModelName = "-1";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        List<ModelAlert> modelAlertList = modelAlertService.findModelAlertsByMonitorNamesAndModelNames(pageable, monitorNames, modelNames);
        int totalElements = modelAlertList.size();
        int fromIndex = pageable.getPageSize() * pageable.getPageNumber();
        int toIndex = pageable.getPageSize() * (pageable.getPageNumber() + 1);
        if (toIndex > totalElements) {
            toIndex = totalElements;
        }
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(modelAlertList.subList(fromIndex, toIndex), pageable, totalElements));

    }

    /**
     * 判断是否被删除
     *
     * @param modelName
     * @return
     */
    @RequestMapping("/isModelExisted")
    public Msg<Integer> isModelExisted(@RequestParam String modelName) {
        List<Model> models = modelAlertService.findModelWithAlert();
        for (Model m : models) {
            if (Objects.equals(m.getModelName(), modelName)) {
                return new Msg<>(MsgCode.SUCCEED, 1);
            }
        }
        return new Msg<>(MsgCode.SUCCEED, 0);
    }

    /**
     * 查询所有有报警信息记录的modelName
     */
    @RequestMapping("/getModelNameWithAlert")
    public Msg<List<String>> getModelNameWithAlert() {
        return new Msg<>(MsgCode.SUCCEED, modelAlertService.getModelNameWithAlert());
    }

    /**
     * 查询所有有报警信息记录的monitorName
     */
    @RequestMapping("/getMonitorNameWithAlert")
    public Msg<List<String>> getMonitorNameWithAlert() {
        return new Msg<>(MsgCode.SUCCEED, modelAlertService.getMonitorNameWithAlert());
    }

    /**
     * 查询所有有子系统没有外键的报警信息记录的modelName
     */
    @RequestMapping("/getModelNameByMonitorString")
    public Msg<List<String>> getModelNameByMonitorString(@RequestParam String monitorName) {
        return new Msg<>(MsgCode.SUCCEED, modelAlertService.findModelNameByMonitorString(monitorName));
    }

    @RequestMapping("/isMonitorExisted")
    public Msg<Integer> isMonitorExisted(@RequestParam String monitorName) {
        List<Monitor> monitors = modelAlertService.findMonitorsWithAlert();
        for (Monitor m : monitors) {
            if (Objects.equals(m.getMonitorName(), monitorName)) {
                return new Msg<>(MsgCode.SUCCEED, 1);
            }
        }
        return new Msg<>(MsgCode.SUCCEED, 0);
    }

    /**
     * Desc: 获取最近n条报警信息的图片的url
     *
     * @param dataNum 报警数
     * @author wgj
     * @date 2023/4/14 19:45
     */
    @GetMapping("/getMonitorAlertPicUrl")
    public Msg<List<Map<String, String>>> getMonitorAlertPicUrl(@RequestParam int dataNum) {
        List<ModelAlert> modelAlerts = modelAlertService.findModelAlertListByDataNumAndDesc(Model.NOT_DELETE, dataNum);
        List<Map<String, String>> list = new ArrayList<>();
        Collections.reverse(modelAlerts);
        for (ModelAlert modelAlert : modelAlerts) {
            if (modelAlert.getImage() && modelAlert.getImagePath() != null) {
                Map<String, String> map = new HashMap<>();
                Monitor monitor = modelAlert.getMonitor();
                if (monitor != null) {
                    map.put("picName", monitor.getName() + ":" + modelAlert.getDescription());
                } else {
                    map.put("picName", modelAlert.getMonitorName() + ":" + modelAlert.getDescription());
                }
                StringBuilder sb = new StringBuilder();
                try {
                    sb.append("raw/")
                            .append(URLEncoder.encode(modelAlert.getImagePath(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    return new Msg<>(MsgCode.FAILED, null);
                }
                map.put("picUrl", sb.toString());
                list.add(map);
            }
        }
        return new Msg<>(MsgCode.SUCCEED, list);
    }

    /**
     * Desc:获取各个状态的报警数量
     *
     * @return 各个状态的报警数量
     * @author wgj
     * @date 2023/4/14 15:39
     */
    @GetMapping("/getModelAlertStatusNum")
    public Msg<Map<String, Integer>> getModelAlertStatusNum() {
        List<ModelAlert> modelAlerts = modelAlertService.getModelAlertList(Model.NOT_DELETE);
        int sum = modelAlerts.size();
        int ignoredNum = 0;
        int handledNum = 0;
        int unHandledNum = 0;
        for (ModelAlert modelAlert : modelAlerts) {
            switch (modelAlert.getStatus()) {
                case ModelAlert.STATUS_HANDLED:
                    handledNum++;
                    break;
                case ModelAlert.STATUS_IGNORED:
                    ignoredNum++;
                    break;
                case ModelAlert.STATUS_UNHANDLED:
                    unHandledNum++;
                    break;
                default:
                    break;
            }
        }
        Map<String, Integer> map = new HashMap<>();
        map.put("sumNum", sum);
        map.put("ignoredNum", ignoredNum);
        map.put("handledNum", handledNum);
        map.put("unHandledNum", unHandledNum);
        return new Msg<>(MsgCode.SUCCEED, map);
    }

    /**
     * 返回近几天的报警信息
     * @param day 近day天
     * @return
     */
    @GetMapping("getModelAlertStatusNumByDays")
    public Msg<Map<String, Integer>> getModelAlertStatusNum(@RequestParam("day") int day) {
        // log.info("getModelAlertStatusNumday接口参数：" + day);
        List<ModelAlert> alertList = modelAlertService.getModelAlertListByDate(Model.NOT_DELETE, day);
        HashMap<String, Integer> map = new HashMap<>();
        // init：如果list未null则HashMap值存在sumNum key。
        map.put("handledNum", 0);
        map.put("unHandledNum", 0);
        map.put("ignoredNum", 0);
        alertList.forEach(modelAlert -> {
            switch (modelAlert.getStatus()) {
                case ModelAlert.STATUS_HANDLED:
                    map.put("handledNum", map.get("handledNum") + 1);
                    break;
                case ModelAlert.STATUS_UNHANDLED:
                    map.put("unHandledNum", map.get("unHandledNum") + 1);
                    break;
                case ModelAlert.STATUS_IGNORED:
                    map.put("ignoredNum", map.get("ignoredNum") + 1);
                    break;
                default:
                    break;
            }
        });
        map.put("sumNum", alertList.size());
        return new Msg<>(MsgCode.SUCCEED, map);
    };

    /**
     * 新总览页面，获取前n天的报警数目前十的算法名称和报警数目
     */
    @GetMapping("/getMostModelAlertModelListByDateNum")
    public Msg<Map<String, List<String>>> getMostModelAlertModelListByDateNum(@RequestParam int dateNum, @RequestParam(defaultValue = "10") int modelNum) {
        List<ModelAlert> modelAlerts = modelAlertService.getModelAlertListByDate(Model.NOT_DELETE, dateNum);
        Map<String, Integer> modelMap = new HashMap<>();
        for (ModelAlert modelAlert : modelAlerts) {
            String name;
            if (modelAlert.getModel() != null) {
                name = modelAlert.getModel().getModelName();
            } else {
                name = modelAlert.getModelName();
            }
            int count = modelMap.getOrDefault(name, 0);
            modelMap.put(name, count + 1);
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(modelMap.entrySet());
        List<String> nameList = new ArrayList<>();
        List<String> numList = new ArrayList<>();
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        int length = list.size() >= modelNum ? modelNum : list.size();
        for (int i = 0; i < length; i++) {
            nameList.add(list.get(i).getKey());
            numList.add(String.valueOf(list.get(i).getValue()));
        }
        Map<String, List<String>> map = new HashMap<>();
        map.put("nameList", nameList);
        map.put("numList", numList);
        return new Msg<>(MsgCode.SUCCEED, map);
    }

    @RequestMapping("/getModelAlertSceneByDateNum")
    public Msg<Map<String, Integer>> getModelAlertSceneByDateNum(@RequestParam int dateNum) {
        List<ModelAlert> modelAlerts = modelAlertService.getModelAlertListByDate(Model.NOT_DELETE, dateNum);
        Map<String, Integer> map = new HashMap<>();
        for (ModelAlert modelAlert : modelAlerts) {
            String sceneName;
            if (modelAlert.getMonitor() == null) {
                sceneName = modelAlert.getScene();
                int count = map.getOrDefault(sceneName, 0);
                map.put(sceneName, count + 1);
            } else {
                if (modelAlert.getMonitor().getScene() != null) {
                    sceneName = modelAlert.getMonitor().getScene().getName();
                    int count = map.getOrDefault(sceneName, 0);
                    map.put(sceneName, count + 1);
                }
            }
        }
        return new Msg<>(MsgCode.SUCCEED, map);
    }

    /**
     * 分页查询
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/dynamicFindModelAlert")
    public Msg<Page<ModelAlert>> getAlertList(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "desc") String order,
                                              @RequestParam(defaultValue = "15") int pageSize,
                                              String monitorName, String modelName, String startTime, String endTime,
                                              @RequestParam(defaultValue = "0") int status) {
        String end = "end";

        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");

        if (StringUtils.isBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            startTime = "1970-01-01 00:00:00";
        }
        if (StringUtils.isBlank(endTime) && StringUtils.isNotBlank(startTime)) {
            endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
//        List<ModelAlert> modelAlertList = modelAlertService.getAllModelAlert(ModelAlert.DELETED);
//
//        JSONArray modelAlertArray = JSONArray.parseArray(JSONArray.toJSONStringWithDateFormat(modelAlertList, "yyyy-MM-dd HH:mm:ss", SerializerFeature.WriteDateUseDateFormat));
//        JSONArray newModelAlertArray = modelAlertArray.stream().filter(item -> {
//            JSONObject currentModelAlert = JSONObject.parseObject(item.toString());
//            if (!StringUtils.isBlank(monitorName)) {
//                if (currentModelAlert.getString("subsystem").contains(ModelAlert.CENTRAL_PLATFORM) && !currentModelAlert.getJSONObject("monitor").getString("monitorName").contains(monitorName)) {
//                    return false;
//                }
//                if (!currentModelAlert.getString("subsystem").contains(ModelAlert.CENTRAL_PLATFORM) && !currentModelAlert.getString("monitorName").contains(monitorName)) {
//                    return false;
//                }
//            }
//            if (!StringUtils.isBlank(modelName)) {
//                if (currentModelAlert.getString("subsystem").contains(ModelAlert.CENTRAL_PLATFORM) && !currentModelAlert.getJSONObject("model").getString("modelName").contains(modelName)) {
//                    return false;
//                }
//                if (!currentModelAlert.getString("subsystem").contains(ModelAlert.CENTRAL_PLATFORM) && !currentModelAlert.getString("modelName").contains(modelName)) {
//                    return false;
//                }
//            }
//            if (!StringUtils.isBlank(startTime)) {
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                try {
//                    Date st = dateFormat.parse(startTime);
//                    Date ct = dateFormat.parse(currentModelAlert.getString("createTime"));
//
//                    if (ct.before(st)) {
//                        return false;
//                    }
//                } catch (ParseException e) {
//                    throw new RuntimeException(e);
//                }
//
//            }
//            if (!StringUtils.isBlank(endTime)) {
//                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                try {
//                    Date et = dateFormat.parse(endTime);
//                    Date ct = dateFormat.parse(currentModelAlert.getString("createTime"));
//
//                    if (ct.after(et)) {
//                        return false;
//                    }
//                } catch (ParseException e) {
//                    throw new RuntimeException(e);
//                }
//
//            }
//            if (currentModelAlert.getInteger("status") != status && status != 0) {
//                return false;
//            }
//            return true;
//        }).collect(Collectors.toCollection(JSONArray::new));
//        List<JSONObject> queryResult = newModelAlertArray.toJavaList(JSONObject.class);
//        int totalElements = queryResult.size();
//        int fromIndex = pageable.getPageSize() * pageable.getPageNumber();
//        int toIndex = pageable.getPageSize() * (pageable.getPageNumber() + 1);
//        if (toIndex > totalElements) {
//            toIndex = totalElements;
//        }
//        //根据字段进行排序
//        queryResult.sort(new Comparator<JSONObject>() {
//            @Override
//            public int compare(JSONObject o1, JSONObject o2) {
//                int compareResult = 0;
//                compareResult = o1.getLong("id").compareTo(o2.getLong("id"));
//                return compareResult;
//            }
//        });
//        //默认升序，如果是desc则进行颠倒
//        if (order.equals("desc")) {
//            Collections.reverse(queryResult);
//        }
//        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(queryResult.subList(fromIndex, toIndex), pageable, totalElements));
        return new Msg<>(MsgCode.SUCCEED, modelAlertService.getModelAlertPage(pageable, monitorName, modelName, startTime, endTime, status));
    }

    @RequestMapping("/updateModelAlert")
    public Msg<String> updateModelAlert(@RequestBody ModelAlert modelAlert) {
        return modelAlertService.updateModelAlert(modelAlert);
    }

    /**
     * 新总览页面，获取前n天的报警数目前5的监控设备名称和报警数目
     */
    @GetMapping("/getMostModelAlertMonitorListByDateNum")
    public Msg<Map<String, List<String>>> getMostModelAlertMonitorListByDateNum(@RequestParam int dateNum, @RequestParam(defaultValue = "5") int monitorNum) {
        List<ModelAlert> modelAlerts = modelAlertService.getModelAlertListByDate(Model.NOT_DELETE, dateNum);
        Map<String, Integer> monitorMap = new HashMap<>();
        for (ModelAlert modelAlert : modelAlerts) {
            String name;
            if (modelAlert.getMonitor() != null) {
                name = modelAlert.getMonitor().getMonitorName();
            } else {
                name = modelAlert.getMonitorName();
            }
            int count = monitorMap.getOrDefault(name, 0);
            monitorMap.put(name, count + 1);
        }
        List<String> nameList = new ArrayList<>();
        List<String> numList = new ArrayList<>();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(monitorMap.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        int length = list.size() >= monitorNum ? monitorNum : list.size();
        for (int i = 0; i < length; i++) {
            nameList.add(list.get(i).getKey());
            numList.add(String.valueOf(list.get(i).getValue()));
        }
        Map<String, List<String>> map = new HashMap<>();
        map.put("nameList", nameList);
        map.put("numList", numList);
        return new Msg<>(MsgCode.SUCCEED, map);
    }
    /**
     * 返回最近n天内，报警数目前五的监控设备名称和报警数目，表格版
     * */

    @GetMapping("/getMostModelAlertMonitorListByDateNumInTable")
    public Msg<Map<String, List<String>>> getMostModelAlertMonitorListByDateNumInTable(@RequestParam int dateNum, @RequestParam(defaultValue = "5") int monitorNum) {
        List<ModelAlert> modelAlerts = modelAlertService.getModelAlertListByDate(Model.NOT_DELETE, dateNum);
        Map<String, Integer> monitorMap = new HashMap<>();
        for (ModelAlert modelAlert : modelAlerts) {
            String name;
            if (modelAlert.getMonitor() != null) {
                name = modelAlert.getMonitor().getMonitorName();
            } else {
                name = modelAlert.getMonitorName();
            }
            int count = monitorMap.getOrDefault(name, 0);
            monitorMap.put(name, count + 1);
        }
        List<String> nameList = new ArrayList<>();
        List<String> numList = new ArrayList<>();
        List<Map.Entry<String, Integer>> list = new ArrayList<>(monitorMap.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        int length = list.size() >= monitorNum ? monitorNum : list.size();
        for (int i = 0; i < length; i++) {
            nameList.add(list.get(i).getKey());
            numList.add(String.valueOf(list.get(i).getValue()));
        }
        Map<String, List<String>> map = new HashMap<>();
        map.put("nameList", nameList);
        map.put("numList", numList);
        return new Msg<>(MsgCode.SUCCEED, map);
    }
    /**
     * 根据monitor的id，获取平台Monitor的所有相关报警信息的model
     */
    @RequestMapping("/getModelsByMonitorInModelAlert")
    public Msg<List<String>> getModelsByMonitorInModelAlert(long monitorId) {
        return new Msg<>(MsgCode.SUCCEED, modelAlertService.findModelByMonitorWithAlert(monitorId));
    }

}

