package org.dlut.adv.mineai.model.kubernetes.controller;

import com.alibaba.fastjson.JSONArray;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.kubernetes.service.LogService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/log")
public class LogController {
    @Resource
    private LogService logService;

    @Value("${kubernetes.namespace}")
    private String namespace;

    /**
     * 获取打印内容
     *
     * @param jobName
     * @param direction
     * @param limit
     * @param start
     * @param end
     * @return
     */
    @RequestMapping("/getJobLog")
    public Msg<JSONArray> getJobLog(String jobName, String direction, Long limit, Long start, Long end) {
        Map<String, String> map = null;
        try {
            map = logService.getLog(namespace, jobName, direction, limit, start, end);
        } catch (Exception e) {
            log.error("获取 JobLog 错误 错误原因: {}", e.getMessage());
        }
        JSONArray resultArray = new JSONArray();
        if (map != null) {
            resultArray.addAll(map.entrySet());
            return new Msg<>(MsgCode.SUCCEED, resultArray);
        }
        return new Msg<>(MsgCode.SUCCEED, null);
    }

    @RequestMapping("/getDeployLog")
    public Msg<JSONArray> getDeployLog(String deployName, String direction, Long limit, Long start, Long end) {
        Map<String, String> map = logService.getDeployLog(namespace, deployName, direction, limit, start, end);
        JSONArray resultArray = new JSONArray();
        if (map != null) {
            resultArray.addAll(map.entrySet());
            return new Msg<>(MsgCode.SUCCEED, resultArray);
        }
        return new Msg<>(MsgCode.FAILED);
    }

    /**
     * 获取打印内容，并将每个参数的值整理进list中
     * 如  epoch:[1,2,3] loss:[0.005,0.003,0.004]
     *
     * @return
     */
    @RequestMapping("/getEpochDetail")
    public Msg<Map<String, List<String>>> getEpochDetail(String jobName, Long limit, Long start, Long end) {
        try {
            Map<String, List<String>> epochDetail = logService.getEpochDetail(namespace, jobName, limit, start, end);
            return new Msg<>(MsgCode.SUCCEED, epochDetail);
        } catch (Exception e) {
            log.error("获取 EpochDetail 失败 原因: {}", e.getMessage());
            return new Msg<>(MsgCode.SUCCEED, null);
        }
    }


    /**
     * 获取打印内容，并将每个参数的值整理进list中
     * 如  epoch:[1,2,3] loss:[0.005,0.003,0.004]
     *
     * @return
     */
    @RequestMapping("/getTestedDetail")
    public Msg<Map<String, List<String>>> getTestedDetail(String jobName, Long limit, Long start, Long end) {
        return new Msg<>(MsgCode.SUCCEED, logService.getTestedDetail(namespace, jobName, limit, start, end));
    }
}
