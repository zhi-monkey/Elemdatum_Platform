package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.ExternalModelConverter;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.vo.DataResponseBody;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.ExternalModelConverterService;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/externalModelConverter")
public class ExternalModelConverterController {

    @Resource
    private ExternalModelConverterService externalModelConverterService;

    // 查询所有设备
    @GetMapping("/findAll")
    public List<ExternalModelConverter> getAllDevices() {
        return externalModelConverterService.findAllDevices();
    }

    @GetMapping("/dynamicFindExternalModelConverterPage")
    public Msg<Page<JSONObject>> dynamicFindExternalModelConverterPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(defaultValue = "15") int pageSize,
            @RequestParam(required = false) String vagueInfo,
            ExternalModelConverter externalModelConverter) {

        // 处理排序参数，去除 "end" 后缀
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf(end));
        }
        // 创建分页请求对象，按 id 排序
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        // 查询结果集
        Page<ExternalModelConverter> queryPage;
        if (vagueInfo != null) {
            // 使用模糊查询
            queryPage = externalModelConverterService.getVagueExternalModelConverterPage(pageable, vagueInfo);
        } else {
            // 使用动态条件查询
            queryPage = externalModelConverterService.dynamicFindExternalModelConverterPage(pageable, externalModelConverter);
        }
        // 将查询结果转换为 JSONObject 列表
        List<JSONObject> externalModelConverterJsonList = queryPage.getContent().stream().map(externalModelConverter1 -> {
            JSONObject externalModelConverterJson = JSON.parseObject(JSONObject.toJSONStringWithDateFormat(externalModelConverter1, "yyyy-MM-dd HH:mm:ss"));
            return externalModelConverterJson.fluentPut("canDelete", true);
           // return externalModelConverterJson.fluentPut("canDelete", externalModelConverterService.isSceneDeletable(externalModelConverter1));
        }).collect(Collectors.toList());
        // 返回包含分页信息的自定义 Msg 对象
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(externalModelConverterJsonList, pageable, queryPage.getTotalElements()));
    }

    // 根据设备ID查询设备
    @GetMapping("/findById")
    public Msg<ExternalModelConverter> getDeviceById(@RequestParam Long id) {
        ExternalModelConverter externalModelConverter =externalModelConverterService.findDeviceById(id);
        return new Msg<>(MsgCode.SUCCEED, externalModelConverter);
    }

    // 根据设备IP查询设备
    @GetMapping("/findByIp")
    public Msg<ExternalModelConverter> getDeviceByIp(@RequestParam String ip) {
        ExternalModelConverter externalModelConverter = externalModelConverterService.findDeviceByIp(ip);
        return new Msg<>(MsgCode.SUCCEED, externalModelConverter);
    }

    // 根据描述信息模糊查询设备
    @GetMapping("/findByDescription")
    public Msg<List<ExternalModelConverter>> getDevicesByDescription(@RequestParam String description) {
        List<ExternalModelConverter> externalModelConverter = externalModelConverterService.findDevicesByDescription(description);
        return new Msg<>(MsgCode.SUCCEED, externalModelConverter);
    }

    // 创建或更新设备
    @PostMapping("/saveOrUpdate")
    public Msg<ExternalModelConverter> createOrUpdateDevice(@RequestBody ExternalModelConverter device) {
        ExternalModelConverter externalModelConverter = externalModelConverterService.saveDevice(device);
        return new Msg<>(MsgCode.SUCCEED,externalModelConverter);
    }

    @GetMapping("/isipexist/{ip}")
    public DataResponseBody isIpExists(@PathVariable String ip) {
        boolean exists = externalModelConverterService.isIpExist(ip);
        return new DataResponseBody(exists);
    }

    // 删除设备
    @DeleteMapping("/delete/{id}")
    public Msg<Void> deleteDevice(@PathVariable Long id) {
        externalModelConverterService.deleteDevice(id);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @Scheduled(fixedRate = 10000)
    public void pingDevice() {
        List<ExternalModelConverter> allDevices = externalModelConverterService.findAllDevices();
        allDevices.forEach((item) -> {
            boolean isOnline = externalModelConverterService.pingDevice(item.getIp());
            // log.info("设备 {} 状态为：{}", item.getIp(), isOnline ? "在线" : "离线");
            if (isOnline != item.getOnlineStatus()) {
                item.setOnlineStatus(isOnline);
                externalModelConverterService.saveDevice(item);
            }
        });
    }

}
