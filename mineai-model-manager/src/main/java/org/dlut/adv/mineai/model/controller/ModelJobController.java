package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.constant.ModelTypeConstant;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.domain.dto.ModelConvertDTO;
import org.dlut.adv.mineai.model.domain.vo.ModelValidationVO;
import org.dlut.adv.mineai.model.dto.ModelJobDTO;
import org.dlut.adv.mineai.model.dto.TrainingDashboardDTO;
import org.dlut.adv.mineai.model.kubernetes.controller.JobController;
import org.dlut.adv.mineai.model.kubernetes.service.JobService;
import org.dlut.adv.mineai.model.kubernetes.service.LogService;
import org.dlut.adv.mineai.model.openfeign.ModelUploadFeign;
import org.dlut.adv.mineai.model.repository.ModelJobLogDataRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.service.*;
import org.dlut.adv.mineai.model.statusMachine.constant.ModelJobStateCodeConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/modelJob")
public class ModelJobController {

    @Resource
    private ModelJobService modelJobService;

    @Resource
    private ModelVersionService modelVersionService;

    @Resource
    private ModelUploadFeign modelUploadFeign;

    @Resource
    private ModelDatasetService modelDatasetService;

    @Resource
    private JobService jobService;

    @Resource
    private JobController jobController;

    @Resource
    private LogService logService;

    @Resource
    @Lazy
    private ModelGenerationService modelGenerationService;

    @Value("${kubernetes.namespace}")
    private String namespace;
    /**
     * 权重根路径
     */
    @Value("${container.weightPath}")
    private String weightRootPath;

    @Value("${kubernetes.nameSuffix}")
    private String nameSuffix;

    @Resource
    private ModelService modelService;
    @Autowired
    private ModelJobRepo modelJobRepo;
    @Autowired
    private ModelExploreService modelExploreService;

    @Resource
    ModelJobLogDataRepo modelJobLogDataRepo;


    /**
     * 获取所有作业
     *
     * @return
     */
    @RequestMapping("/getJob")
    public Msg<List<ModelJob>> getJob() {
        List<ModelJob> jobs = modelJobService.getModelJob();
        return new Msg<>(MsgCode.SUCCEED, jobs);
    }

    /**
     * 获取所有作业
     *
     * @return
     */
    @RequestMapping("/getJobWithNoDeleted")
    public Msg<List<ModelJob>> getJobWithNoDeleted() {
        List<ModelJob> jobs = modelJobService.getModelJobWithNoDeleted();
        return new Msg<>(MsgCode.SUCCEED, jobs);
    }

    /**
     * 获取未删除作业数量
     * 仅返回 count，避免一次性传输全部列表
     */
    @RequestMapping("/getJobCountWithNoDeleted")
    public Msg<Long> getJobCountWithNoDeleted() {
        long count = modelJobService.getModelJobCountWithNoDeleted();
        return new Msg<>(MsgCode.SUCCEED, count);
    }
    /**
     * 获取所有作业和创建人
     *
     * @return
     */
    @RequestMapping("/getJobWithUser")
    public Msg<List<ModelJobDTO>> getJobWithUser() {
        List<ModelJobDTO> jobs = modelJobService.getModelJobWithUser();
        return new Msg<>(MsgCode.SUCCEED, jobs);
    }

    @ApiOperation(value = "大屏训练任务统计", notes = "只统计训练任务(jobType=1)，状态为排队中 / 训练中的数量和列表")
    @GetMapping("/overviewTrainingStats")
    public Msg<Map<String, Object>> getOverviewTrainingStats() {
        Map<String, Object> stats = modelJobService.getOverviewTrainingStats();
        return new Msg<>(MsgCode.SUCCEED, stats);
    }

    /**
     * 作业分页查找
     *
     * @param page
     * @param order
     * @param pageSize
     * @param modelJob
     * @return
     */
    @GetMapping("/jobList")
    public Msg<Page<ModelJob>> jobList(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "desc") String order,
                                       @RequestParam(defaultValue = "10") int pageSize, ModelJob modelJob, ModelVersion modelVersion) {
        //前端排序规则带有end后缀需要去除
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf("end"));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        if (modelJob.getJobType() != 1 && modelJob.getJobType() != 2) {
            modelJob.setJobType(0);
        }
        if (modelJob.getStatus() != 2 && modelJob.getStatus() != 1 && modelJob.getStatus() != -1 && modelJob.getStatus() != -2) {
            modelJob.setStatus(0);
        }
        modelJob.setModelVersion(modelVersion);
        Page<ModelJob> jobPage = modelJobService.getJobPage(pageable, modelJob);
        return new Msg<>(MsgCode.SUCCEED, jobPage);
    }

    /**
     * 取消作业
     *
     * @param modelJob
     * @return
     */
    @RequestMapping("/abortJob")
    public Msg<String> abortJob(@RequestBody ModelJob modelJob) {
        ModelVersion modelVersion = modelJobService.findModelVersionByModelJob(modelJob);
        if (modelJob.getStatus().equals(ModelJobStateCodeConstant.TRAIN_SUCCEEDED) || modelJob.getStatus().equals(ModelJobStateCodeConstant.CONVERT_SUCCEEDED) || modelJob.getStatus().equals(ModelJob.INSPECT_SUCCEEDED)) {
            return new Msg<>(MsgCode.CANCEL_EXECUTED_MODEL_JOB_FAILED);
        } else if (modelJob.getStatus().equals(ModelJobStateCodeConstant.TRAIN_FAILED) || modelJob.getStatus().equals(ModelJobStateCodeConstant.CONVERT_FAILED) || modelJob.getStatus().equals(ModelJob.INSPECT_FAILED)) {
            return new Msg<>(MsgCode.CANCEL_FAILED_MODEL_JOB_FAILED);
        } else if (modelJob.getStatus().equals(ModelJobStateCodeConstant.CANCELED)) {
            return new Msg<>(MsgCode.CANCEL_FAILED_MODEL_JOB_CANCELED);
        } else {
            jobController.deleteJob(namespace, modelJob.getName());
            modelJob.setStatus(ModelJobStateCodeConstant.CANCELED);
            modelJobService.saveModelJob(modelJob);
            modelVersionService.updateModelVersionByModelJob(modelVersion, modelJob, 2);
            return new Msg<>(MsgCode.SUCCEED);
        }
    }


    /**
     * 创建作业
     *
     * @param modelGenerationId
     * @return
     */
    @RequestMapping("/createJob")
    public Msg<String> createJob(
            @RequestParam Long modelGenerationId,
            @RequestParam(required = false) Long modelJobId,
            @RequestParam Integer modelWorkingMode,
            @RequestParam String modelType
    ) {

        return modelJobService.createJob(modelGenerationId, modelJobId, modelWorkingMode, modelType);
    }

    /**
     * 终止作业
     *
     * @param modelGenerationId
     * @return
     */
    @RequestMapping("/stopJob")
    public Msg<String> stopJob(@RequestParam Long modelGenerationId) {
        return modelJobService.stopJob(modelGenerationId);
    }

    /**
     * 更新作业描述
     *
     * @param modelJob
     * @return
     */
    @RequestMapping("/updateJobDescription")
    public Msg<String> updateJobDescription(@RequestBody ModelJob modelJob) {
        if (!modelJobService.isModelJobExist(modelJob)) {
            return new Msg<>(MsgCode.JOB_NOT_EXIST);
        }
        boolean status = modelJobService.updateJobDesc(modelJob);
        if (status) {
            return new Msg<>(MsgCode.SUCCEED);
        } else {
            return new Msg<>(MsgCode.UPDATE_JOB_FAILED);
        }
    }

    /**
     * 根据id查询job
     */
    @RequestMapping("/getJobByJobId")
    public Msg<ModelJob> getJobByJobId(@RequestParam long id) {
        ModelJob modelJob = modelJobService.getModelJobById(id);
        if (modelJob == null) {
            return new Msg<>(MsgCode.FAILED);
        } else {
            return new Msg<>(MsgCode.SUCCEED, modelJob);
        }
    }


    @RequestMapping("/getParamByJobId")
    public Msg<Map<String, String>> getParamByJobId(@RequestParam long id) {
        return new Msg<>(MsgCode.SUCCEED, modelJobService.getModelJobById(id).getParams());
    }

    @ApiOperation(value = "创建转换作业")
    @RequestMapping("/createConvertJob")
    public Msg<String> createConvertJob(
            @RequestParam Long modelGenerationId,
            @RequestBody ModelConvertDTO modelConvertDTO,
            @RequestBody(required = false) Map<String, String> quantizationParams) {

        log.info("ModelGenerationId: {}, ModelConvertDTO: {}, QuantizationParams: {}",
                modelGenerationId, modelConvertDTO.toString(), quantizationParams);

        return modelJobService.createConvertJob(modelGenerationId, modelConvertDTO, quantizationParams);
    }

    //    /**
    //     * 新建作业
    //     */
    //    @RequestMapping("addModelJob")
    //    public Msg<Long> addModelJob(@RequestBody JSONObject modelJobJson) {
    //        ModelVersion modelVersion = JSON.toJavaObject(modelJobJson.getJSONObject("modelValues").getJSONObject("modelVersion"), ModelVersion.class);
    //        //保存modelJob
    //        ModelJob modelJob = new ModelJob();
    //        //添加权重文件
    //        String weightPath = modelJobJson.getJSONObject("modelValues").getString("weightPath");
    //        //添加modelVersion
    //        if (modelVersion.isReuse()) {
    //            modelJob.setModelVersion(modelVersionService.findModelVersionById(modelVersion.getReuseId()));
    //        } else {
    //            modelJob.setModelVersion(modelVersion);
    //        }
    //        Long modelGenerationId = Long.valueOf(modelJobJson.getJSONObject("modelValues").getString("modelGenerationId"));
    //        ModelGeneration mg = modelGenerationService.getModelGenerationById(modelGenerationId);
    //        //添加modelVersionName
    //        modelJob.setModelVersionName(modelVersion.getShowName());
    //        //添加作业类型
    //        int jobType = modelJobJson.getJSONObject("modelValues").getIntValue("jobType");
    //        modelJob.setJobType(jobType);
    //        //添加数据集
    //        DatasetVersionVO dataset = modelDatasetService.selectDatasetVersionById(modelJobJson.getJSONObject("modelValues").getJSONObject("dataset").getLong("id"));
    //        //添加memory
    //        String memory = modelJobJson.getJSONObject("modelValues").getString("memory");
    //        modelJob.setMemory(StringUtils.isBlank(memory) ? "无限制" : memory);
    //        //添加cpus
    //        String cpus = modelJobJson.getJSONObject("modelValues").getString("cpus");
    //        modelJob.setCpus(StringUtils.isBlank(cpus) ? "无限制" : cpus);
    //        //添加gpus
    //        String gpus = modelJobJson.getJSONObject("modelValues").getString("gpus");
    //        modelJob.setGpus(StringUtils.isBlank(gpus) ? "无限制" : gpus);
    //        //添加描述
    //        String description = modelJobJson.getJSONObject("modelValues").getString("description");
    //        //添加超参
    //        JSONObject paramJson = modelJobJson.getJSONObject("modelValues").getJSONObject("params");
    //
    //        //存入job类型字段
    //        paramJson.put("MODEL_WORKING_MODE", String.valueOf(jobType));
    //        //存超参
    //        String paramsString = JSON.toJSONString(paramJson);
    //        Map<String, String> paramsMap = JSON.parseObject(paramsString, new TypeReference<HashMap<String, String>>() {
    //        });
    //        for (String key : paramsMap.keySet()) {
    //            System.out.println(key + ":" + paramsMap.get(key));
    //        }
    //        modelJob.setParams(paramsMap);
    //
    //        //暂时新增job后默认状态为  正在运行   日志路径为data/jobLogFile/{当前时间}
    //        modelJob.setStatus(ModelJob.EXECUTING);
    //
    //        modelJobService.saveModelJob(modelJob);
    //        modelJob.setName("job-" + nameSuffix + '-' + modelJob.getId());
    //        modelJob.setLogFilePath(modelJob.getName() + "---" + System.currentTimeMillis() + ".log");
    //        //创建作业日期修改
    //        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh:mm:ss");
    //        String createTime = simpleDateFormat.format(modelJob.getCreateTime());
    //        String defaultDescription = null;
    //        if (jobType == ModelJob.TRAIN) {
    //            defaultDescription = createTime + " 训练作业" + modelJob.getId() + "_算法" + modelVersion.getShowName() + "_数据集" + dataset.getName();
    //        } else if (jobType == ModelJob.INSPECT) {
    //            defaultDescription = createTime + " 质检作业" + modelJob.getId() + "_算法" + modelVersion.getShowName() + "_数据集" + dataset.getName();
    //        }
    //        modelJob.setDescription(StringUtils.isBlank(description) ? defaultDescription : description);
    //        modelJobService.saveModelJob(modelJob);
    //        modelVersionService.updateModelVersionByModelJob(modelJob.getModelVersion(), modelJob, 1);
    //        //训练，不需要权重文件；质检需要权重文件
    //        if (modelJob.getJobType() == ModelJob.TRAIN) {
    //            jobController.createJob(namespace, modelJob.getName(), modelJob.getName(), modelJob.getModelVersion().getUrl(), dataset.getVersionUrl(), gpus, cpus, memory, "m1", modelJob.getParams());
    //            //mg.setTrainJob(modelJob);
    //            modelVersion.setWeightPath(modelJob.getName());
    //            mg.setStatus(ModelGeneration.TRAINING);
    //            modelVersionService.saveModelVersion(modelVersion);
    //            modelGenerationService.saveModelGeneration(mg);
    //        } else {
    //            //查找该modelGeneration的model是否有其他的testModelVersion,如果没有，则不允许单独训练，如果有，则同时进行训练。
    //            Model model = mg.getModel();
    //            //获取model的A，B modelVersion
    //            ModelVersion modelVersionA = modelVersionService.findBasicTestModelVersionByModel(model);
    //            ModelVersion modelVersionB = modelVersionService.findBestTestModelVersionByModel(model);
    //            //如果A,B都不存在，则无需比较，直接开始质检
    //            //如果A存在，B不存在，则与A同时质检
    //            if (modelVersionA != null && modelVersionB == null) {
    //                ModelJob modelJobOriginTest = new ModelJob();
    //                modelJobOriginTest.setJobType(jobType);
    //                modelJobOriginTest.setModelVersion(modelVersionA);
    //                modelJobOriginTest.setModelVersionName(modelVersionA.getShowName());
    //                modelJobOriginTest.setCpus(StringUtils.isBlank(cpus) ? "无限制" : cpus);
    //                modelJobOriginTest.setGpus(StringUtils.isBlank(gpus) ? "无限制" : gpus);
    //                modelJobOriginTest.setMemory(StringUtils.isBlank(memory) ? "无限制" : memory);
    //                modelJobOriginTest.setStatus(ModelJob.EXECUTING);
    //                modelJobOriginTest.setParams(paramsMap);
    //                modelJobService.saveModelJob(modelJobOriginTest);
    //                modelJobOriginTest.setName("job-" + nameSuffix + '-' + modelJobOriginTest.getId());
    //                modelJobOriginTest.setLogFilePath(modelJob.getName() + "---" + System.currentTimeMillis() + ".log");
    //                String testDescription = createTime + " 质检作业" + modelJobOriginTest.getId() + "_算法" + modelVersionA.getShowName() + "_数据集" + dataset.getName();
    //                modelJobOriginTest.setDescription(testDescription);
    //                modelJobService.saveModelJob(modelJobOriginTest);
    //                modelVersionService.updateModelVersionByModelJob(modelJobOriginTest.getModelVersion(), modelJobOriginTest, 1);
    //                jobController.createJob(namespace, modelVersionA.getName(), modelVersionA.getWeightPath(),
    //                        modelVersionA.getUrl(), dataset.getVersionUrl(), gpus, cpus, memory,
    //                        "m1", modelJobOriginTest.getParams());
    //                //mg.setAnotherTestJob(modelJobOriginTest);
    //                mg.setStatus(ModelGeneration.TESTING);
    //                //最后一种情况，无论只存在B，或者是AB都存在，则都与B同时质检
    //            } else if (modelVersionB != null) {
    //                ModelJob modelJobOriginTest = new ModelJob();
    //                modelJobOriginTest.setJobType(jobType);
    //                modelJobOriginTest.setModelVersion(modelVersionB);
    //                modelJobOriginTest.setModelVersionName(modelVersionB.getShowName());
    //                modelJobOriginTest.setCpus(StringUtils.isBlank(cpus) ? "无限制" : cpus);
    //                modelJobOriginTest.setGpus(StringUtils.isBlank(gpus) ? "无限制" : gpus);
    //                modelJobOriginTest.setMemory(StringUtils.isBlank(memory) ? "无限制" : memory);
    //                modelJobOriginTest.setStatus(ModelJob.EXECUTING);
    //                modelJobOriginTest.setParams(paramsMap);
    //                modelJobService.saveModelJob(modelJobOriginTest);
    //                modelJobOriginTest.setName("job-" + nameSuffix + '-' + modelJobOriginTest.getId());
    //                modelJobOriginTest.setLogFilePath(modelJob.getName() + "---" + System.currentTimeMillis() + ".log");
    //                String testDescription = createTime + " 质检作业" + modelJobOriginTest.getId() + "_算法" + modelVersionB.getShowName() + "_数据集" + dataset.getName();
    //                modelJobOriginTest.setDescription(testDescription);
    //                modelJobService.saveModelJob(modelJobOriginTest);
    //                modelVersionService.updateModelVersionByModelJob(modelJobOriginTest.getModelVersion(), modelJobOriginTest, 1);
    //                jobController.createJob(namespace, modelJobOriginTest.getName(), modelVersionB.getWeightPath(),
    //                        modelVersionB.getUrl(), dataset.getVersionUrl(), gpus, cpus, memory,
    //                        "m1", modelJobOriginTest.getParams());
    //                //mg.setAnotherTestJob(modelJobOriginTest);
    //                mg.setStatus(ModelGeneration.TESTING);
    //            }
    //            jobController.createJob(namespace, modelJob.getName(), weightPath, modelJob.getModelVersion().getUrl(), dataset.getVersionUrl(), gpus, cpus, memory, "m1", modelJob.getParams());
    //            //mg.setTestJob(modelJob);
    //            mg.setStatus(ModelGeneration.TESTING);
    //            modelGenerationService.saveModelGeneration(mg);
    //        }
    //        System.err.println(modelJob.getId());
    //        return new Msg<>(MsgCode.SUCCEED, modelJob.getId());
    //    }

    //    @RequestMapping("/trainJob")
    //    public Msg<String> trainJob(long modelGenerationId) {
    //        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
    //        //用editTrainJob接口，使得训练过的generation即使重新进行训练也能成功，因为editTrainJob会创建个与原trainjob相同配置的job，但是jobname不同
    //        //如果直接ModelJob trainJob = modelGeneration.getTrainJob()，则训练过的jobname由于在集群中存在过，则实际上不会进行训练
    //        ModelJob trainJob = modelGenerationService.editTrainJob(modelGeneration.getId(), modelGeneration.getTrainJob());
    //
    //        trainJob.setGpus("2");
    //        if (modelGeneration.getHardwareParams() != null) {
    //            trainJob.setCpus(modelGeneration.getHardwareParams().getCpus());
    //            trainJob.setMemory(modelGeneration.getHardwareParams().getMemory());
    //        }
    //        trainJob.setWeightPath(trainJob.getName());
    //        trainJob.setModelVersion(modelGeneration.getTrainModelVersion());
    //        trainJob.setStatus(ModelJob.EXECUTING);
    //        ModelVersion trainModelVersion = modelGeneration.getTrainModelVersion();
    //        trainModelVersion.setWeightPath(trainJob.getName());
    //        modelJobService.saveModelJob(trainJob);
    //        modelVersionService.saveModelVersion(trainModelVersion);
    //        if (modelGeneration.getDelayTrainTime() != 0) {
    //            modelGeneration.setStatus(ModelGeneration.TRAIN_WAIT);
    //            modelGenerationService.saveModelGeneration(modelGeneration);
    //            Timer timer = new Timer();
    //            TimerTask timerTask = new TimerTask() {
    //                @Override
    //                public void run() {
    //                    System.out.println("-------------------延时执行---------------------");
    //                    jobController.createJob(namespace, trainJob.getName(), trainModelVersion.getWeightPath(), trainModelVersion.getUrl(), modelDatasetService.selectDatasetVersionById(modelGeneration.getTrainDataset()).getVersionUrl(), trainJob.getGpus(), trainJob.getCpus(), trainJob.getMemory(), "m1", trainJob.getParams());
    //                    modelGeneration.setStatus(ModelGeneration.TRAINING);
    //                    modelGenerationService.saveModelGeneration(modelGeneration);
    //                }
    //            };
    //            timer.schedule(timerTask, (long) modelGeneration.getDelayTrainTime() * 1000 * 60);
    //        } else {
    //            jobController.createJob(namespace, trainJob.getName(), trainModelVersion.getWeightPath(), trainModelVersion.getUrl(), modelDatasetService.selectDatasetVersionById(modelGeneration.getTrainDataset()).getVersionUrl(), trainJob.getGpus(), trainJob.getCpus(), trainJob.getMemory(), "m1", trainJob.getParams());
    //            modelGeneration.setStatus(ModelGeneration.TRAINING);
    //            modelGenerationService.saveModelGeneration(modelGeneration);
    //        }
    //        return new Msg<>(MsgCode.SUCCEED);
    //    }


    //    @RequestMapping("/trainGuideJob")
    //    public Msg<String> trainGuideJob(long modelGenerationId){
    //        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
    //        //用editTrainJob接口，使得训练过的generation即使重新进行训练也能成功，因为editTrainJob会创建个与原trainjob相同配置的job，但是jobname不同
    //        //如果直接ModelJob trainJob = modelGeneration.getTrainJob()，则训练过的jobname由于在集群中存在过，则实际上不会进行训练
    //        ModelJob trainJob = modelGenerationService.editTrainJob(modelGeneration.getId(), modelGeneration.getTrainJob());
    //
    //        trainJob.setGpus("2");
    //        if(modelGeneration.getHardwareParams() != null){
    //            trainJob.setCpus(modelGeneration.getHardwareParams().getCpus());
    //            trainJob.setMemory(modelGeneration.getHardwareParams().getMemory());
    //        }
    //        trainJob.setWeightPath(trainJob.getName());
    //        trainJob.setModelVersion(modelGeneration.getTrainModelVersion());
    //        trainJob.setStatus(ModelJob.EXECUTING);
    //        ModelVersion trainModelVersion = modelGeneration.getTrainModelVersion();
    //        trainModelVersion.setWeightPath(trainJob.getName());
    //        modelJobService.saveModelJob(trainJob);
    //        modelVersionService.saveModelVersion(trainModelVersion);
    //        try {
    //            jobController.createJob(namespace, trainJob.getName(), trainModelVersion.getWeightPath(), trainModelVersion.getUrl(), modelDatasetService.selectDatasetVersionById(modelGeneration.getTrainDataset()).getVersionUrl(), trainJob.getGpus(), trainJob.getCpus(), trainJob.getMemory(), "m1", trainJob.getParams());
    //        }catch (Exception e){
    //            e.printStackTrace();
    //            return new Msg<>(MsgCode.FAILED);
    //        }
    //        modelGeneration.setStatus(ModelGeneration.TRAINING);
    //        modelGenerationService.saveModelGeneration(modelGeneration);
    //        return new Msg<>(MsgCode.SUCCEED);
    //    }
    //    /**
    //     * 如果超时，停止训练
    //     *
    //     * @param modelGenerationId
    //     * @return
    //     */
    //    @RequestMapping("/stopTrainJob")
    //    public Msg<String> stopTrainJob(long modelGenerationId) {
    //        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
    //        if (modelGeneration.getMaxTrainTime() == 0){
    //            return new Msg<>(MsgCode.SUCCEED);
    //        }else{
    //            ModelJob trainJob = modelGeneration.getTrainJob();
    //            Timer timer = new Timer();
    //            TimerTask timerTask = new TimerTask() {
    //                @Override
    //                public void run() {
    //                    if (trainJob.getStatus() != ModelJob.SUCCEEDED) {
    //                        jobService.deleteJob(namespace, trainJob.getName());
    //                        trainJob.setStatus(ModelJob.TIMEOUT);
    //                        modelJobService.saveModelJob(trainJob);
    //                        modelGeneration.setStatus(ModelGeneration.TRAIN_TIME_OUT);
    //                        modelGenerationService.saveModelGeneration(modelGeneration);
    //                    }
    //                }
    //            };
    //            timer.schedule(timerTask, (long) (modelGeneration.getDelayTrainTime() + modelGeneration.getMaxTrainTime()) * 1000 * 60);
    //        }
    //        return new Msg<>(MsgCode.SUCCEED);
    //    }

//    /**
//     * 创建质检作业
//     *
//     * @param modelGenerationId
//     * @return
//     */
//    @RequestMapping("/createTestJob")
//    public Msg<String> createTestJob(@RequestParam Long modelGenerationId, @RequestParam Integer modelWorkingMode) {
//        ModelGeneration modelGenerationById = modelGenerationService.findModelGenerationById(modelGenerationId);
//        ModelJob latestTrainJob = modelJobService.getModelJobById(modelGenerationById.getLatestJobId());
//        HardwareParams hardwareParams = modelGenerationById.getHardwareParams();
//        //生成新的Job
//        ModelJob modelJob = new ModelJob();
//        modelJob.setJobType(2);
//        modelJob.setGpus(hardwareParams == null ? null : hardwareParams.getGpus());
//        modelJob.setCpus(hardwareParams == null ? null : hardwareParams.getCpus());
//        modelJob.setMemory(hardwareParams == null ? null : hardwareParams.getMemory());
//        modelJob.setGpuMemory(hardwareParams == null ? null : hardwareParams.getGpuMemory());
//        Map<String, String> generationParams = new HashMap<>();
//        generationParams.put("MODE_WORKING_MODE", "5");
//        modelJob.setParams(generationParams);
//        modelJob.setStatus(ModelJob.INSPECTING);
//        //生成anotherJob
//        ModelJob modelJob1 = new ModelJob();
//        modelJob1.setJobType(2);
//        modelJob1.setGpus(hardwareParams == null ? null : hardwareParams.getGpus());
//        modelJob1.setCpus(hardwareParams == null ? null : hardwareParams.getCpus());
//        modelJob1.setMemory(hardwareParams == null ? null : hardwareParams.getMemory());
//        modelJob1.setParams(generationParams);
//        modelJob.setStatus(ModelJob.INSPECTING);
//        //生成两个job，一个用于质检，另一个用于算法商城模型的对比
//        ModelJob modelJobSaved = modelJobService.saveModelJob(modelJob);
//        ModelJob anotherJobSaved = modelJobService.saveModelJob(modelJob1);
//        modelJobSaved.setName("job-" + nameSuffix + '-' + modelJobSaved.getId());
//        anotherJobSaved.setName("job-" + nameSuffix + '-' + anotherJobSaved.getId());
//        modelJobSaved.setWeightPath(latestTrainJob.getWeightPath());
//        anotherJobSaved.setWeightPath(modelGenerationById.getModel().getBestWeightPath());
//        modelJobSaved.setModelGenerationId(modelGenerationId);
//        anotherJobSaved.setModelGenerationId(modelGenerationId);
//        modelJobService.saveModelJob(modelJobSaved);
//        modelJobService.saveModelJob(anotherJobSaved);
//        String url = modelGenerationById.getModelExplore() != null ? modelGenerationById.getModelExplore().getTrainModelVersion().getUrl() : modelGenerationById.getModel().getTrainModelVersion().getUrl();
//        DatasetVersionVO trainDataset = modelDatasetService.selectDatasetVersionById(modelGenerationById.getTrainDataset());
//        if (modelGenerationById.getSplitSize() != null && !modelGenerationById.getSplitSize().isEmpty()) {
//            modelDatasetService.datasetCut(modelGenerationById.getSplitSize(), modelGenerationId,modelGenerationById.getLatestJobId());
//            DatasetVersionVO datasetVersionVO = modelDatasetService.selectDatasetVersionById(modelGenerationById.getTestDataset());
//            jobController.createTrainJobUseCutDataset(namespace, modelJobSaved.getName(), modelJobSaved.getWeightPath(), url,
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/train/images",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/test/images",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/validation/images",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/train/annotations",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/test/annotations",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/validation/annotations",
//                    "YOLO".equals(trainDataset.getFormat()) ? (trainDataset.getVersionUrl() + "/data.yaml") : null,
//                    hardwareParams == null ? null : hardwareParams.getGpus(),
//                    hardwareParams == null ? null : hardwareParams.getCpus(),
//                    hardwareParams == null ? null : hardwareParams.getMemory(),
//                    hardwareParams == null ? null : hardwareParams.getGpuMemory(),
//                    hardwareParams == null ? null : hardwareParams.getVGpuCores(),
//                    generationParams, null, ModelTypeConstant.OFFICIAL);
//            jobController.createTrainJobUseCutDataset(namespace, anotherJobSaved.getName(), anotherJobSaved.getWeightPath(), url,
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/train/images",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/test/images",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/validation/images",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/train/annotations",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/test/annotations",
//                    datasetVersionVO.getVersionUrl() + "/" + modelGenerationById.getSplitSize() + "/validation/annotations",
//                    "YOLO".equals(trainDataset.getFormat()) ? (trainDataset.getVersionUrl() + "/data.yaml") : null,
//                    hardwareParams == null ? null : hardwareParams.getGpus(),
//                    hardwareParams == null ? null : hardwareParams.getCpus(),
//                    hardwareParams == null ? null : hardwareParams.getMemory(),
//                    hardwareParams == null ? null : hardwareParams.getGpuMemory(),
//                    hardwareParams == null ? null : hardwareParams.getVGpuCores(),
//                    generationParams, null, ModelTypeConstant.OFFICIAL);
//        } else {
//            // 正常的切分不同的数据集
//            jobController.createTrainJob(namespace, modelJobSaved.getName(), modelJobSaved.getWeightPath(), url,
//                    trainDataset.getVersionUrl(),
//                    modelDatasetService.selectDatasetVersionById(modelGenerationById.getTestDataset()).getVersionUrl(),
//                    modelDatasetService.selectDatasetVersionById(modelGenerationById.getValDataset()).getVersionUrl(),
//                    "YOLO".equals(trainDataset.getFormat()) ? (trainDataset.getVersionUrl() + "/data.yaml") : null,
//                    hardwareParams == null ? null : hardwareParams.getGpus(),
//                    hardwareParams == null ? null : hardwareParams.getCpus(),
//                    hardwareParams == null ? null : hardwareParams.getMemory(),
//                    hardwareParams == null ? null : hardwareParams.getGpuMemory(),
//                    hardwareParams == null ? null : hardwareParams.getVGpuCores(),
//                    generationParams, null, ModelTypeConstant.OFFICIAL);
//            jobController.createTrainJob(namespace, anotherJobSaved.getName(), anotherJobSaved.getWeightPath(), url,
//                    trainDataset.getVersionUrl(),
//                    modelDatasetService.selectDatasetVersionById(modelGenerationById.getTestDataset()).getVersionUrl(),
//                    modelDatasetService.selectDatasetVersionById(modelGenerationById.getValDataset()).getVersionUrl(),
//                    "YOLO".equals(trainDataset.getFormat()) ? (trainDataset.getVersionUrl() + "/data.yaml") : null,
//                    hardwareParams == null ? null : hardwareParams.getGpus(),
//                    hardwareParams == null ? null : hardwareParams.getCpus(),
//                    hardwareParams == null ? null : hardwareParams.getMemory(),
//                    hardwareParams == null ? null : hardwareParams.getGpuMemory(),
//                    hardwareParams == null ? null : hardwareParams.getVGpuCores(),
//                    generationParams, null, ModelTypeConstant.OFFICIAL);
//        }
//        List<ModelJob> modelJobList = modelGenerationById.getModelJobList();
//        if (modelJobList == null) {
//            ArrayList<ModelJob> modelJobs = new ArrayList<>();
//            modelJobs.add(modelJobSaved);
//            modelJobs.add(anotherJobSaved);
//            modelGenerationById.setModelJobList(modelJobs);
//        } else {
//            modelJobList.add(modelJobSaved);
//            modelJobList.add(anotherJobSaved);
//            modelGenerationById.setModelJobList(modelJobList);
//        }
//        //这里把generation的Test设置为当前训练出的模型创建的TestJob
//        modelGenerationById.setTestJob(modelJobSaved);
//        modelGenerationById.setAnotherTestJob(anotherJobSaved);
//        modelGenerationService.saveModelGeneration(modelGenerationById);
//        return new Msg<>(MsgCode.SUCCEED);
//    }

//    @RequestMapping("/testJob")
//    public Msg<String> testJob(long modelGenerationId) {
//        ModelGeneration modelGeneration = modelGenerationService.findModelGenerationById(modelGenerationId);
//        //用editTestJob接口，使得训练过的generation即使重新进行质检也能成功，因为editTestJob会创建个与原testjob相同配置的job，但是jobname不同
//        //如果直接ModelJob test = modelGeneration.getTestJob()，则训练过的jobname由于在集群中存在过，则实际上不会进行质检
//        //
//        //
//        // modelGenerationService.editTestJob(modelGeneration.getId(), modelGeneration.getTestJob());
//        //modelGeneration处于持久化状态，所以可以不用重新获取，editTestJob更新了该对象的testJob
//        ModelJob testJob = new ModelJob();
//        testJob.setGpus("2");
//        if (modelGeneration.getHardwareParams() != null) {
//            testJob.setCpus(modelGeneration.getHardwareParams().getCpus());
//            testJob.setMemory(modelGeneration.getHardwareParams().getMemory());
//        }
//        testJob.setModelVersion(modelGeneration.getTestModelVersion());
//        testJob.setStatus(ModelJob.EXECUTING);
//        modelJobService.saveModelJob(testJob);
//        try {
//            jobController.createTrainJob(namespace, testJob.getName(), modelGeneration.getTrainJob().getWeightPath(), modelGeneration.getTestModelVersion().getUrl(), modelDatasetService.selectDatasetVersionById(modelGeneration.getTestDataset()).getVersionUrl(), modelGeneration.getHardwareParams().getGpus(), modelGeneration.getHardwareParams().getCpus(), modelGeneration.getHardwareParams().getMemory(), "m1", testJob.getParams());
//            ModelJob anotherTestJob = modelGeneration.getAnotherTestJob();
//            if (anotherTestJob != null && modelGeneration.getModel() != null) {
//
//                anotherTestJob.setGpus("2");
//                if (modelGeneration.getHardwareParams() != null) {
//                    anotherTestJob.setCpus(modelGeneration.getHardwareParams().getCpus());
//                    anotherTestJob.setMemory(modelGeneration.getHardwareParams().getMemory());
//                }
//                anotherTestJob.setWeightPath(anotherTestJob.getName());
//                anotherTestJob.setStatus(ModelJob.EXECUTING);
//                ModelJob savedAnother = modelJobService.saveModelJob(anotherTestJob);
//                Model model = modelGeneration.getModel();
//                //有best的选best的，否则选basic
//                ModelVersion trainModelVersion = modelVersionService.findBestOrBasicTrainMVByModel(model);
//                ModelVersion testModelVersion = modelVersionService.findBestOrBasicTestMVByModel(model);
//                jobController.createJob(namespace, savedAnother.getName(), trainModelVersion.getWeightPath(), testModelVersion.getUrl(), modelDatasetService.selectDatasetVersionById(modelGeneration.getTestDataset()).getVersionUrl(), savedAnother.getGpus(), savedAnother.getCpus(), savedAnother.getMemory(), "m1", savedAnother.getParams());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new Msg<>(MsgCode.FAILED);
//        }
//        modelGeneration.setStatus(ModelGeneration.TESTING);
//        modelGenerationService.saveModelGeneration(modelGeneration);
//        return new Msg<>(MsgCode.SUCCEED);
//    }

    // 守护进程调用，修改作业状态
    @RequestMapping("/setJobStatusByJobId")
    public Msg<ModelJob> setJobStatusByJobId(@RequestParam long id, @RequestParam int status) {
        ModelJob modelJob = modelJobService.getModelJobById(id);
        if (modelJob == null) {
            return new Msg<>(MsgCode.FAILED);
        } else {
            modelJob.setStatus(status);
            modelJobService.saveModelJob(modelJob);
            return new Msg<>(MsgCode.SUCCEED, modelJob);
        }
    }

    @RequestMapping("/createPreprocessModelJob")
    public Msg<String> createPreprocessModelJob(@RequestBody JSONObject preProcessJob) {
        String jobName = preProcessJob.getString("jobName");
        String image = preProcessJob.getString("image");
        String datasetSrcPath = preProcessJob.getString("datasetSrcPath");
        String datasetTrgPath = preProcessJob.getString("datasetTrgPath");
        //String formatList = preProcessJob.getJSONArray("formatList").toJavaList(String.class).toString();
        Map<String, Object> params = preProcessJob.getJSONObject("config").getInnerMap();
        jobController.createPreprocessJob("ai-platform-s2", jobName, image, datasetSrcPath, datasetTrgPath, null, null, null, params);
        return new Msg<>(MsgCode.SUCCEED);
    }

    @RequestMapping("/getTrainJobProgress")
    public Msg<Double> getTrainJobProgress(@RequestParam long jobId) {
        try {
            ModelJob modelJob = modelJobService.getModelJobById(jobId);
            List<String> epochList = logService.getEpochDetail(namespace, modelJob.getName(), 99999L, 1641280408415000000L, null).get("epoch");
            String allEpoch = modelJob.getParams().get("HP_EPOCHES");
            //获取最新epoch
            double newEpoch = epochList.stream().mapToInt(Integer::parseInt).max().orElse(0);
            //获取百分比进度
            double progress = (newEpoch) / Double.parseDouble(allEpoch) * 100;
            return new Msg<>(MsgCode.SUCCEED, progress);
        } catch (Exception e) {
            return new Msg<>(MsgCode.SUCCEED);
        }
    }

    // 只有训练，latestjob， currentBetterJob
    @RequestMapping("/getTestJobProgressAndAcc")
    @ApiOperation(value = "获取训练之后的信息", notes = "比较精度")
    public Msg<Map<String, Double>> getTestJobProgressAndAcc(@RequestParam long testJobId, @RequestParam long anotherTestJobId) {
        try {
            if (anotherTestJobId != -1) {
                ModelJob nowTestJob = modelJobService.getModelJobById(testJobId);
                ModelJob originTestJob = modelJobService.getModelJobById(anotherTestJobId);
                double totalImage = Double.parseDouble(logService.getTotal(namespace, nowTestJob.getName(), 5000L, 1641280408415000000L, null));
                List<String> nowTestedList = logService.getTestedDetail(namespace, nowTestJob.getName(), 5000L, 1641280408415000000L, null).get("tested");
                List<String> originTestedList = logService.getTestedDetail(namespace, originTestJob.getName(), 5000L, 1641280408415000000L, null).get("tested");
                double newNowTested = nowTestedList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
                double newOriginTested = originTestedList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
                // 获得当前最高准确率
                List<String> nowAccList = logService.getTestedDetail(namespace, nowTestJob.getName(), 5000L, 1641280408415000000L, null).get("accuracy");
                List<String> originAccList = logService.getTestedDetail(namespace, originTestJob.getName(), 5000L, 1641280408415000000L, null).get("accuracy");
                double newNowAcc = nowAccList.stream().mapToDouble(Double::parseDouble).max().orElse(0) * 100;
                double newOriginAcc = originAccList.stream().mapToDouble(Double::parseDouble).max().orElse(0) * 100;
                double progress = (newNowTested + newOriginTested) * 100 / (2 * totalImage);
                Map<String, Double> map = new HashMap<>(3);
                map.put("progress", progress);
                map.put("newNowAcc", newNowAcc);
                map.put("newOriginAcc", newOriginAcc);
                return new Msg<>(MsgCode.SUCCEED, map);
            } else {
                ModelJob nowTestJob = modelJobService.getModelJobById(testJobId);
                double totalImage = Double.parseDouble(logService.getTotal(namespace, nowTestJob.getName(), 5000L, 1641280408415000000L, null));
                List<String> nowTestedList = logService.getTestedDetail(namespace, nowTestJob.getName(), 5000L, 1641280408415000000L, null).get("tested");
                double newNowTested = nowTestedList.stream().mapToDouble(Double::parseDouble).max().orElse(0);
                List<String> nowAccList = logService.getTestedDetail(namespace, nowTestJob.getName(), 5000L, 1641280408415000000L, null).get("accuracy");
                double newNowAcc = nowAccList.stream().mapToDouble(Double::parseDouble).max().orElse(0) * 100;
                double progress = newNowTested * 100 / (totalImage);
                Map<String, Double> map = new HashMap<>(3);
                map.put("progress", progress);
                map.put("newNowAcc", newNowAcc);
                map.put("newOriginAcc", 0d);
                return new Msg<>(MsgCode.SUCCEED, map);
            }
        } catch (Exception e) {
            return new Msg<>(MsgCode.SUCCEED);
        }
    }

    @RequestMapping("/getTrainAccAndRecall")
    @ApiOperation(value = "获取训练之后的信息", notes = "比较精度")
    public Msg<Map<String, Double>> getTrainAccAndRecall(@RequestParam long trainJobId) {
        try {
            ModelJob nowTestJob = modelJobService.getModelJobById(trainJobId);
            return new Msg<>(MsgCode.SUCCEED, modelJobService.getTrainAccAndRecall(nowTestJob));
        } catch (Exception e) {
            // 可以选择日志记录异常信息
            return new Msg<>(MsgCode.SUCCEED, null);
        }
    }


    @RequestMapping("/getTotalImage")
    public Msg<String> getTotalImage() {
        String totalImage = logService.getTotal(namespace, "job-cloud-453", 5000L, 1641280408415000000L, null);
        return new Msg<>(MsgCode.SUCCEED, totalImage);
    }

//    @RequestMapping("/findModelJobsByModelGenerationId")
//    public Msg<List<ModelJob>> findModelJobsByModelGenerationId(@RequestParam long modelGenerationId) {
//        ModelGeneration mg = modelGenerationService.findModelGenerationById(modelGenerationId);
//        List<ModelJob> mjs = mg.getModelJobList();
//        List<ModelJob> successMJ = mjs.stream().collect(Collectors.toList());
//        Collections.reverse(successMJ);
//        return new Msg<>(MsgCode.SUCCEED, successMJ);
//    }

    @RequestMapping("/findModelJobsByModelGenerationId")
    public Msg<List<ModelJob>> findModelJobsByModelGenerationId(@RequestParam long modelGenerationId) {
        // 获取 ModelGeneration 实体
        ModelGeneration mg = modelGenerationService.findModelGenerationById(modelGenerationId);
        // 获取 ModelJob 列表
        List<ModelJob> modelJobs = mg.getModelJobList();
        // 获取 ModelExplore 的 description 字段
        String modelExploreDescription = modelJobService.getModelExploreDescriptionByGenerationId(modelGenerationId);
        // 遍历 modelJobs 列表，检查每个 job 的 description，如果为空，则设置为 modelExploreDescription
        for (ModelJob job : modelJobs) {
            if (job.getDescription() == null) {
                job.setDescription(modelExploreDescription);
            }
        }
        // 复制成功的 ModelJob 列表并进行逆序
        List<ModelJob> successMJ = new ArrayList<>(modelJobs);
        Collections.reverse(successMJ);
        // 返回 Msg 封装后的 ModelJob 列表
        return new Msg<>(MsgCode.SUCCEED, successMJ);
    }


    @GetMapping("/guidePublishModel")
    @ApiOperation(value = "自动发布算法到算法商城", notes = "通过比较精确度，判断是否发布算法到算法商城")
    public Msg<String> getLatestJobAndAnotherJobProgressAndAcc(@RequestParam long latestJobId, @RequestParam long modelId, @RequestParam long modelGenerationId) {
        log.info("参数：latestJobId:{}, modelId:{}, modelGenerationId{}", latestJobId, modelId, modelGenerationId); // 377  59  96
        Model model = modelService.getModelById(modelId);
        // 说明是新增算法到算法商城，需要填充完整信息
        if (model.getGenerationId() == 0) {
            // 更新算法商城中的算法
            model.setSource(Model.GUIDE_GENERATION);
            model.setBestWeightPath(modelJobService.getModelJobById(latestJobId).getWeightPath());
            model.setGenerationId(modelGenerationId);
            model.setIsDelete(Model.NOT_DELETE);
            modelService.saveModel(model);
        } else {
            // 说明为手动发布的，或者引导发布的
            // 进行精度的比较
            ModelJob modelJob = modelJobService.getModelJobById(latestJobId);
            logService.getJobAcc(namespace, modelJob.getName(), 5000L, 1641280408415000000L, null);
        }
        return new Msg<>(MsgCode.SUCCEED, null);
    }

    // 根据job id查询模型验证缓存
    @GetMapping(value = "/{jobId}/cache/query")
    public Msg<List<ModelValidationVO>> queryModelValidationCache(@PathVariable(name = "jobId") Long jobId) {

        List<ModelValidationVO> modelValidationVOS = modelJobService.queryModelValidationCache(jobId);
        return new Msg<>(MsgCode.SUCCEED, modelValidationVOS);
    }

    /**
     * 根据jobId，获取Job
     */
    @RequestMapping("/getModelJobByJobId")
    public Msg<ModelJob> getModelJobByJobId(@RequestParam long modelJobId) {
        ModelJob modelJob = modelJobService.getModelJobById(modelJobId);
        return new Msg<>(MsgCode.SUCCEED, modelJob);
    }

    @GetMapping("/{jobId}/configs")
    public Msg<List<ModelConfig>> getModelConfigs(@PathVariable long jobId) {
        List<ModelConfig> configs = modelJobService.getConvertModelConfigsByJobId(jobId);
        return new Msg<>(MsgCode.SUCCEED, configs);
    }

    @GetMapping("getHyperParams/{modelId}")
    public Msg<Map<String, List<PublishedHyperParams>>> getHyperParams(@PathVariable("modelId") Long modelId) {
        return new Msg<>(MsgCode.SUCCEED, modelJobService.getHyperParams(modelId));
    }

    @GetMapping("getConvertJobsByTrainJobID")
    public Msg<List<ModelJob>> getConvertJobs(@RequestParam long trainJobID) {
        List<ModelJob> convertJobs = modelJobService.getConvertJobsByTrainJobID(trainJobID);
        return new Msg<>(MsgCode.SUCCEED, convertJobs);
    }

    @GetMapping("getQueuePositionByModelJobName")
    public Msg<Integer> getQueuePositionByModelJobName(@RequestParam String modelJobName) {
        int position = modelJobService.getQueuePositionByModelJobName(modelJobName);
        return new Msg<>(MsgCode.SUCCEED, position);
    }

    /**
     * 获取训练仪表盘所需的全部聚合信息。
     * 此API旨在取代前端对 getTrainJobProgress, getEpochDetail, getJobLog, getTrainAccAndRecall 的多次分散调用。
     *
     * @param jobId 训练作业的ID
     * @return 包含进度、图表数据、最终指标和原始日志的聚合DTO
     */
    @ApiOperation(value = "获取训练仪表盘聚合信息", notes = "从数据库获取训练进度、指标等聚合信息")
    @GetMapping("/getTrainingDashboard")
    public Msg<TrainingDashboardDTO> getTrainingDashboard(@RequestParam long jobId,
                                                          @RequestParam(required = false) Long maxLines,
                                                          @RequestParam(required = false) String lastTimestamp) {
        try {
            // 1. 获取作业基础信息
            ModelJob modelJob = modelJobService.getModelJobById(jobId);
            if (modelJob == null) {
                log.warn("Request for Training Dashboard failed: Job with ID {} not found.", jobId);
                return new Msg<>(MsgCode.FAILED, null);
            }

            // 2. 从数据库获取日志解析后的数据
            ModelJobLogData logData = modelJobLogDataRepo.findByModelJob(modelJob);
            TrainingDashboardDTO dto = new TrainingDashboardDTO();

            if (logData != null) {
                // 3. 设置基础数据
                dto.setProgress(logData.getProgress() != null ? logData.getProgress().doubleValue() : 0.0);

                // 4. 设置epoch详情（JSON格式）
                if (StringUtils.isNotBlank(logData.getEpochDetail())) {
                    try {
                        dto.setEpochDetail(JSON.parseObject(logData.getEpochDetail(), new TypeReference<Map<String, List<String>>>() {}));
                    } catch (Exception e) {
                        log.warn("Failed to parse epochDetail for job {}: {}", jobId, e.getMessage());
                        dto.setEpochDetail(new HashMap<>());
                    }
                } else {
                    dto.setEpochDetail(new HashMap<>());
                }

                // 5. 如果是已完成任务，设置最终指标
                if (!modelJob.isExecuting() || dto.getProgress() >= 100.0) {
                    dto.setProgress(100.0); // 确保完成状态进度为100%

                    Map<String, Double> finalMetrics = new HashMap<>();
                    if (logData.getAccuracy() != null) {
                        finalMetrics.put("newNowAcc", logData.getAccuracy().doubleValue() * 100);
                    }
                    if (logData.getRecall() != null) {
                        finalMetrics.put("newNowRecall", logData.getRecall().doubleValue() * 100);
                    }
                    dto.setFinalMetrics(finalMetrics);
                }

                // 6. 设置空的原始日志（保持接口兼容）
                dto.setRawLogs(new JSONArray());
            } else {
                // 无数据时返回默认值
                dto.setProgress(0.0);
                dto.setEpochDetail(new HashMap<>());
                dto.setRawLogs(new JSONArray());
            }

            return new Msg<>(MsgCode.SUCCEED, dto);
        } catch (Exception e) {
            log.error("Error getting Training Dashboard for job ID {}: {}", jobId, e.getMessage(), e);
            return new Msg<>(MsgCode.FAILED, null);
        }
    }

    /**
     * 获取训练作业的原始日志内容
     *
     * @param jobId 训练作业的ID
     * @param maxLines 最大日志行数（可选）
     * @param lastTimestamp 上次获取的时间戳（可选）
     * @return 包含原始日志的DTO
     */
    @ApiOperation(value = "获取训练原始日志", notes = "获取训练作业的原始日志内容")
    @GetMapping("/getTrainingRawLogs")
    public Msg<TrainingDashboardDTO> getTrainingRawLogs(@RequestParam long jobId,
                                                        @RequestParam(required = false) Long maxLines,
                                                        @RequestParam(required = false) String lastTimestamp) {
        try {
            // 1. 获取作业基础信息
            ModelJob modelJob = modelJobService.getModelJobById(jobId);
            if (modelJob == null) {
                log.warn("Request for Training Raw Logs failed: Job with ID {} not found.", jobId);
                return new Msg<>(MsgCode.FAILED, null);
            }

            // 2. 获取原始日志
            Map<String, String> allLogs = logService.getLogsFromFile(modelJob.getName(), maxLines, lastTimestamp);

            TrainingDashboardDTO dto = new TrainingDashboardDTO();

            if (allLogs != null && !allLogs.isEmpty()) {
                // 3. 填充原始日志 (用于终端显示)
                JSONArray resultArray = new JSONArray();
                // 注意：由于Loki返回的map是无序的，我们需要按时间戳（key）排序以保证前端显示正确
                List<Map.Entry<String, String>> sortedLogs = new ArrayList<>(allLogs.entrySet());
                // 按照时间排序 顺序
                sortedLogs.sort(Map.Entry.comparingByKey());

                for (Map.Entry<String, String> entry : sortedLogs) {
                    Map<String, String> logEntry = new HashMap<>();
                    logEntry.put(entry.getKey(), entry.getValue());
                    resultArray.add(logEntry);
                }
                dto.setRawLogs(resultArray);
            } else {
                dto.setRawLogs(new JSONArray());
            }

            return new Msg<>(MsgCode.SUCCEED, dto);
        } catch (Exception e) {
            log.error("Error getting raw logs for job ID {}: {}", jobId, e.getMessage(), e);
            return new Msg<>(MsgCode.FAILED, null);
        }
    }
}
