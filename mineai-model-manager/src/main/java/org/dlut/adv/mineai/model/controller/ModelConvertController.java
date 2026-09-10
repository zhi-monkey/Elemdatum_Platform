package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.Controller;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.kubernetes.controller.JobController;
import org.dlut.adv.mineai.model.service.ModelControllerService;
import org.dlut.adv.mineai.model.service.ModelJobService;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;

@Slf4j
@RestController
@RequestMapping("/modelConvert")
public class ModelConvertController {

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private JobController jobController;

    @Resource
    private ModelControllerService modelControllerService;

    /**
     * 新增算法转换
     */
    @RequestMapping("/addModelConvert")
    public Msg<String> addModelConvert(@RequestBody JSONObject modelConvertJson) {
        Controller controller = modelControllerService.findControllerById(modelConvertJson.getLong("controllerId"));
        ModelJob convertJob = new ModelJob();
        String namespace = modelConvertJson.getString("namespace");
        String jobName = modelConvertJson.getJSONObject("job").getString("job");
        String description = modelConvertJson.getJSONObject("job").getString("description");
        String weightPath = modelConvertJson.getString("weightPath");
//        convertJob.setWeightPath(weightPath);
        ModelVersion mv = JSON.toJavaObject(modelConvertJson.getJSONObject("modelVersion"), ModelVersion.class);
        String image = modelConvertJson.getString("image");
        String weightRootPath = modelConvertJson.getString("weightRootPath");
        String gpuNum = modelConvertJson.getString("gpuNum");
        convertJob.setGpus(StringUtils.isBlank(gpuNum) ? "无限制" : gpuNum);
        String cpuNum = modelConvertJson.getString("cpuNum");
        convertJob.setCpus(StringUtils.isBlank(cpuNum) ? "无限制" : cpuNum);
        String memoryNum = modelConvertJson.getString("memoryNum");
        String convertType = modelConvertJson.getString("convertType");
        convertJob.setMemory(StringUtils.isBlank(memoryNum) ? "无限制" : memoryNum);
        convertJob.setJobType(ModelJob.CONVERT);
        modelJobService.saveModelJob(convertJob);
        convertJob.setName("job" + convertJob.getId());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
        String createTime = simpleDateFormat.format(convertJob.getCreateTime());
        String defaultDescription = createTime + " 模型转换" + convertJob.getId() + "_算法" + mv.getName();
        convertJob.setDescription(StringUtils.isBlank(description) ? defaultDescription : description);
        convertJob.setModelVersion(mv);
        //convertJob.setStatus(ModelJob.EXECUTING);
        convertJob.setStatus(ModelJobStateCodeConstant.CONVERTING);
        modelJobService.saveModelJob(convertJob);
        String rknn = "rknn";
        String engine = "engine";
        if (convertType.equals(rknn)) {
            System.err.println("rknn");
            jobController.createConvertJob(namespace, convertJob.getName(), weightPath, image, weightRootPath, gpuNum, cpuNum, memoryNum);
        } else {
            jobController.createConvertEngineJob(namespace, convertJob.getName(), weightPath, image, weightRootPath, gpuNum, cpuNum, memoryNum);
        }
        return new Msg<>(MsgCode.SUCCEED);
    }

    // 用于板端守护进程，单独存一个job，返回job id
    @RequestMapping("/addModelConvertJob")
    public Msg<Long> addModelConvertJob(@RequestBody JSONObject modelConvertJson) {
        ModelJob convertJob = new ModelJob();
        String description = modelConvertJson.getJSONObject("job").getString("description");
        ModelVersion mv = JSON.toJavaObject(modelConvertJson.getJSONObject("modelVersion"), ModelVersion.class);
        String gpuNum = modelConvertJson.getString("gpuNum");
        convertJob.setGpus(StringUtils.isBlank(gpuNum) ? "无限制" : gpuNum);
        String cpuNum = modelConvertJson.getString("cpuNum");
        convertJob.setCpus(StringUtils.isBlank(cpuNum) ? "无限制" : cpuNum);
        String memoryNum = modelConvertJson.getString("memoryNum");
        convertJob.setMemory(StringUtils.isBlank(memoryNum) ? "无限制" : memoryNum);
        convertJob.setJobType(ModelJob.CONVERT);
        modelJobService.saveModelJob(convertJob);
        convertJob.setName("job" + convertJob.getId());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
        String createTime = simpleDateFormat.format(convertJob.getCreateTime());
        String defaultDescription = createTime + " 模型转换" + convertJob.getId() + "_算法" + mv.getName();
        convertJob.setDescription(StringUtils.isBlank(description) ? defaultDescription : description);
        convertJob.setModelVersion(mv);
        convertJob.setStatus(ModelJobStateCodeConstant.CONVERTING);
        modelJobService.saveModelJob(convertJob);
        return new Msg<>(MsgCode.SUCCEED, convertJob.getId());
    }

    @ApiOperation(value = "获取到数据集中对应的图片数量")
    @GetMapping("/getDataRepoPicCount/{jobId}")
    public Msg<Long> getDataRepoPicCount(@ApiParam("任务id") @PathVariable Long jobId) {
        log.info("jobId: {}", jobId);
        return new Msg<>(MsgCode.SUCCEED, modelJobService.getTrainSetCount(jobId));
    }


    @ApiOperation(value = "获取到数据集中对应的图片数量")
    @GetMapping("/getTrainSetCountByDatasetVersionId/{datasetVersionId}")
    public Msg<Long> getTrainSetCountByDatasetVersionId(@ApiParam("数据集id") @PathVariable Long datasetVersionId) {
        return new Msg<>(MsgCode.SUCCEED, modelJobService.getTrainSetCountByDatasetVersionId(datasetVersionId));
    }
}