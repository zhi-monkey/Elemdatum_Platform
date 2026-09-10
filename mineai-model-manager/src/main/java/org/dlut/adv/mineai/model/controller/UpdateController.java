package org.dlut.adv.mineai.model.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.dlut.adv.mineai.core.constant.ModelJobStateCodeConstant;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.UpdateService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@EnableScheduling
@RequestMapping("update")
public class UpdateController {

    @Resource
    UpdateService updateService;

    /**
     * 设置定时任务，更新 Job 的状态信息
     */
    @Scheduled(cron = "0/10 * * * * *")
    public void updateStatus() throws Exception {
        updateService.updateAllJobStatus();
    }

    @RequestMapping("/getModelJobStatus")
    @ApiOperation(value = "获得Job的状态信息", notes = "根据ModelJobId获得Job的状态信息")
    public Msg<Integer> getModelJobStatus(@ApiParam("任务的id") long modelJobId) {
        int status = updateService.getModelJobStatus(modelJobId);

        if (status == ModelJobStateCodeConstant.TRAIN_SUCCEEDED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.TRAIN_SUCCEEDED);
        } else if (status == ModelJobStateCodeConstant.TRAINING) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.TRAINING);
//        } else if (status == ModelJob.PAUSED) {
//            return new Msg<Integer>(MsgCode.SUCCEED, ModelJob.PAUSED);
        } else if (status == ModelJobStateCodeConstant.CANCELED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.CANCELED);
        } else if (status == ModelJobStateCodeConstant.TRAIN_FAILED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.TRAIN_FAILED);
        } else if (status == ModelJob.INSPECTING) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJob.INSPECTING);
        } else if (status == ModelJob.INSPECT_FAILED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJob.INSPECT_FAILED);
        } else if (status == ModelJob.INSPECT_SUCCEEDED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJob.INSPECT_SUCCEEDED);
        } else if (status == ModelJobStateCodeConstant.CONVERTING) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.CONVERTING);
        } else if (status == ModelJobStateCodeConstant.CONVERT_FAILED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.CONVERT_FAILED);
        } else if (status == ModelJobStateCodeConstant.CONVERT_SUCCEEDED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.CONVERT_SUCCEEDED);
        } else if (status == ModelJobStateCodeConstant.PUBLISHED) {
            return new Msg<Integer>(MsgCode.SUCCEED, ModelJobStateCodeConstant.PUBLISHED);
        } else {
            return new Msg<Integer>(MsgCode.FAILED, status);
        }
    }

}
