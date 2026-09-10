package org.dlut.adv.mineai.model.controller;

import com.google.common.base.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.bcel.generic.INSTANCEOF;
import org.dlut.adv.mineai.core.entity.ModelAlert;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.CoalFlowService;
import org.dlut.adv.mineai.model.service.ModelAlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @package: org.dlut.adv.mineai.model.controller
 * @author: chystart
 * @create: 2023-12-07 13:40
 * @description: 透明化煤流 Controller
 **/
@Slf4j
@RestController
@RequestMapping(value = "/coalflow")
public class CoalFlowController {

    public static final String SUBSYSTEMPREFIXNAME = "guangtianxia";
    public static final String INSTANTANEOUS_COAL_FLOW = "gpshy_momentflow";
    public static final String HOURLY_COAL_FLOW = "gpshy_totalflow";
    public static final String BELT_TEARING = "gplongtear_alarm";
    public static final String GEAR_CONTROL = "gpshy_speedAdj";

    @Resource
    private CoalFlowService coalFlowService;

    /**
     * 瞬时煤流算法最新数据展示
     *
     * @return
     */
    @GetMapping(value = "/getInstantaneousCoalFlow")
    public Msg<List<ModelAlert>> getInstantaneousCoalFlow() {
        List<ModelAlert> modelAlerts = coalFlowService.getModelAlert(INSTANTANEOUS_COAL_FLOW, SUBSYSTEMPREFIXNAME);
        log.info("最新的瞬时煤流数据信息：" + modelAlerts.toString());
        return new Msg<>(MsgCode.SUCCEED, modelAlerts);
    }

    /**
     * 小时煤流算法最新数据展示
     *
     * @return
     */
    @GetMapping(value = "/getHourlyCoalFlow")
    public Msg<List<ModelAlert>> getHourlyCoalFlow() {
        List<ModelAlert> modelAlerts = coalFlowService.getModelAlert(HOURLY_COAL_FLOW, SUBSYSTEMPREFIXNAME);
        log.info("最新的小时煤流数据信息：" + modelAlerts.toString());
        return new Msg<>(MsgCode.SUCCEED, modelAlerts);
    }

    /**
     * 皮带纵撕算法最新数据展示
     *
     * @return
     */
    @GetMapping(value = "/getBeltTearing")
    public Msg<List<ModelAlert>> getBeltTearing() {
        List<ModelAlert> modelAlerts = coalFlowService.getModelAlert(BELT_TEARING, SUBSYSTEMPREFIXNAME);
        log.info("最新的皮带纵撕数据信息：" + modelAlerts.toString());
        return new Msg<>(MsgCode.SUCCEED, modelAlerts);
    }

    /**
     * 档位调速算法最新数据展示
     *
     * @return
     */
    @GetMapping(value = "/getGearControl")
    public Msg<List<ModelAlert>> getGearControl() {
        List<ModelAlert> modelAlerts = coalFlowService.getModelAlert(GEAR_CONTROL, SUBSYSTEMPREFIXNAME);
        log.info("最新的档位调速数据信息：" + modelAlerts.toString());
        return new Msg<>(MsgCode.SUCCEED, modelAlerts);
    }


    /**
     * 针对 小时煤流 的数据量返回近 7 个小时的数据
     *
     * @return
     */
    @GetMapping(value = "/getHourlyCoalFlowOvertime")
    public Msg<List<ModelAlert>> getHourlyCoalFlowOvertime() {
        List<ModelAlert> result = coalFlowService.getHourlyCoalFlowOvertime(HOURLY_COAL_FLOW);
        return new Msg(MsgCode.SUCCEED, result);
    }



}
