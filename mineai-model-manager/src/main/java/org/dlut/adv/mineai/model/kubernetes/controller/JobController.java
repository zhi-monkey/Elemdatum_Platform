package org.dlut.adv.mineai.model.kubernetes.controller;

import com.alibaba.cloud.commons.lang.StringUtils;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.*;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.constant.ModelTypeConstant;
import org.dlut.adv.mineai.core.constant.RedisConstants;
import org.dlut.adv.mineai.core.entity.ModelGeneration;
import org.dlut.adv.mineai.core.entity.ModelJob;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.utils.RedisUtil;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.ModelConvertDTO;
import org.dlut.adv.mineai.model.domain.dto.ModelValidationDTO;
import org.dlut.adv.mineai.model.domain.vo.ModelValidationVO;
import org.dlut.adv.mineai.model.dto.AutoLabelJobRequest;
import org.dlut.adv.mineai.model.kubernetes.service.JobService;
import org.dlut.adv.mineai.model.repository.ModelGenerationRepo;
import org.dlut.adv.mineai.model.repository.ModelJobRepo;
import org.dlut.adv.mineai.model.service.GpuManagementService;
import org.dlut.adv.mineai.model.statusMachine.statemachine.ModelJobStateMachine;
import org.dlut.adv.mineai.model.statusMachine.statemachine.StateMachineFactory;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.dlut.adv.mineai.model.utils.MinioUtils;
import org.dlut.adv.mineai.model.utils.SpringContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author oyjp
 */
@Slf4j
@RestController
@RequestMapping("/job")
@Api(tags = "工作负载 - job 相关操作")
public class JobController {
    @Autowired
    private StateMachineFactory stateMachineFactory;

    /**
     * harbor IP地址
     */
    @Value("${harbor-server.url}")
    String harborUrl;
    /**
     * 项目名称
     */
    @Value("${harbor-server.project}")
    String harborProject;
    /**
     * inputPVC 是输入的文件（训练用的数据集）挂载的 pvc
     */
    @Value("${kubernetes.pvc.inputPVC}")
    String inputPVC;
    /**
     * outputPVC 是容器运行时输出的文件挂载的 pvc
     */
    @Value("${kubernetes.pvc.outputPVC}")
    String outputPVC;
    /**
     * 该 pvc 中存储的是训练过后的权重文件夹的路径
     */
    @Value("${kubernetes.rootPath.weightRootPath}")
    String weightRootPath;

    /**
     * pvc中的数据集文件夹路径
     * minio 中桶路径
     */
    @Value("${kubernetes.rootPath.datasetRootPath}")
    String datasetRootPath;

    @Value("${kubernetes.nodeSelector.key}")
    String selectorKey;
    @Value("${kubernetes.nodeSelector.value}")
    String selectorValue;
    /**
     * 容器中权重文件路径
     */
    @Value("${container.weightPath}")
    String containerWeight;
    /**
     * 容器中数据集路径
     */
    @Value("${container.datasetPath}")
    String containerDataset;
    /**
     * Job服务
     */
    @Resource
    JobService jobService;
    /**
     * GPU管理服务（动态管理禁用的GPU UUID列表）
     */
    @Autowired
    private GpuManagementService gpuManagementService;

    /**
     * 远程调用dubhe-data服务
     */
    @Resource
    DubheDataFeign dubheDataFeign;
    @Resource
    DubheUtils dubheUtils;
    @Resource
    RedisUtil redisUtil;
    @Value("${kubernetes.namespace}")
    private String namespace;
    @Autowired
    private ModelGenerationRepo modelGenerationRepo;
    @Autowired
    private ModelJobRepo modelJobRepo;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Autowired
    private MinioUtils minioUtil;

    @Value("${storage.file-store-root-path}")
    private String nfs;

    /**
     * @param namespace   命名空间
     * @param jobName     job名称
     * @param weightPath  与weightRootPath共同构成存储空间中的具体的权重文件路径
     * @param image       镜像名称
     * @param datasetPath 存储空间中具体的数据集路径
     * @param gpuNum      预计使用的gpu数量 Gi
     * @param cpuNum      预计使用的cpu数量
     * @param memoryNum   预计使用的内存数量
     * @param label       节点标记
     * @param params      容器环境变量参数 即 传递给模型的参数
     * @return
     */
    @RequestMapping("/createJob")
    public V1Job createJob(String namespace, String jobName, String weightPath, String image, String datasetPath, String gpuNum, String cpuNum, String memoryNum, String label, @RequestBody Map<String, String> params) {
        System.out.println("namespace:" + namespace);
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);
        //挂载卷声明  使用pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName(outputPVC);

        V1PersistentVolumeClaimVolumeSource nfsDataPVC = new V1PersistentVolumeClaimVolumeSource();
        nfsDataPVC.claimName(inputPVC);

        List<V1Volume> volumes = new ArrayList<>();
        V1Volume fileDataVolume = new V1Volume();
        fileDataVolume.setName(outputPVC);
        fileDataVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(fileDataVolume);

        V1Volume nfsDataVolume = new V1Volume();
        nfsDataVolume.setName(inputPVC);
        nfsDataVolume.setPersistentVolumeClaim(nfsDataPVC);
        volumes.add(nfsDataVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        V1VolumeMount weightMount = new V1VolumeMount();
        weightMount.setName(outputPVC);
        weightMount.setMountPath(containerWeight);
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        weightMount.setSubPath(weightRootPath + "/" + weightPath);
        volumeMounts.add(weightMount);

        V1VolumeMount datasetMount = new V1VolumeMount();
        datasetMount.setName(inputPVC);
        datasetMount.setMountPath(containerDataset);
        //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
        datasetMount.setSubPath(datasetRootPath + "/" + datasetPath);
        volumeMounts.add(datasetMount);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : params.keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(params.get(key));
            envVars.add(v1EnvVar);
        }
//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        V1Affinity v1Affinity = new V1Affinity();
        V1NodeAffinity v1NodeAffinity = new V1NodeAffinity();
        V1NodeSelector v1NodeSelector = new V1NodeSelector();

        List<V1NodeSelectorTerm> v1NodeSelectorTerms = new ArrayList<>();
        V1NodeSelectorTerm v1NodeSelectorTerm = new V1NodeSelectorTerm();


        List<V1NodeSelectorRequirement> matchExpressions = new ArrayList<>();
        V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
        nodeSelectorRequirement.setKey(selectorKey);
        nodeSelectorRequirement.setOperator("In");
        nodeSelectorRequirement.setValues(Collections.singletonList(selectorValue));
        matchExpressions.add(nodeSelectorRequirement);
        v1NodeSelectorTerm.setMatchExpressions(matchExpressions);

        v1NodeSelectorTerms.add(v1NodeSelectorTerm);
        v1NodeSelector.setNodeSelectorTerms(v1NodeSelectorTerms);
        v1NodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(v1NodeSelector);
        v1Affinity.setNodeAffinity(v1NodeAffinity);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(cpuNum);
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);

        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta())
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        .affinity(v1Affinity)
                                        .containers(Arrays.asList(new V1Container()
                                                .image(fullImageName)
                                                .imagePullPolicy("IfNotPresent")
                                                .name(jobName)
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        return jobService.createJob(namespace, job);
    }

    /**
     * @param namespace  命名空间
     * @param jobName    job名称
     * @param weightPath 与 weightRootPath 共同构成存储空间中的具体的权重文件路径
     * @param image      镜像名称
     * @param trainPath  存储空间中具体的 训练数据集 路径，
     * @param testPath   存储空间中具体的 测试数据集 路径
     * @param valPath    存储空间中具体的 验证数据集 路径
     * @param yoloPath   如果是yolo格式的数据集，还需要挂载一个data.yaml文件
     * @param gpuNum     预计使用的gpu数量 Gi
     * @param cpuNum     预计使用的cpu数量
     * @param memoryNum  预计使用的内存数量
     * @param params     容器环境变量参数 即 传递给模型的参数
     * @return
     */
    @RequestMapping("/createTrainJobUseCutDataset")
    public V1Job createTrainJobUseCutDataset(String namespace, String jobName, String weightPath, String image, String trainImagesPath, String testImagesPath, String valImagesPath,
                                             String trainPath, String testPath, String valPath, String yoloPath, Integer gpuCount,
                                             String cpuNum, String memoryNum, String gpuMemory, String vGpuCores, @RequestBody Map<String, String> params, String preWeightPath, String modelType) {
        log.info("参数: namespace: {}", namespace);
        log.info("参数: jobName: {}", jobName);
        log.info("参数: weightPath: {}", weightPath);
        log.info("参数: image: {}", image);
        //log.info("参数: imagesPath: {}", imagesPath);
        log.info("参数: trainPath: {}", trainPath);
        log.info("参数: testPath: {}", testPath);
        log.info("参数: valPath: {}", valPath);
        log.info("参数: yoloPath: {}", yoloPath);
        log.info("参数: preWeightPath: {}", preWeightPath);
        if (image == null || image.trim().isEmpty()) {
            throw new IllegalArgumentException("训练镜像不能为空");
        }
        // 将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        if (imageName.length < 2) {
            throw new IllegalArgumentException("训练镜像格式非法: " + image);
        }
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);

        // 挂载卷声明  使用 pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName(outputPVC);
        V1PersistentVolumeClaimVolumeSource nfsDataPVC = new V1PersistentVolumeClaimVolumeSource();
        nfsDataPVC.claimName(inputPVC);

        List<V1Volume> volumes = new ArrayList<>();

        V1Volume fileDataVolume = new V1Volume();
        fileDataVolume.setName(outputPVC);
        fileDataVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(fileDataVolume);

        V1Volume nfsDataVolume = new V1Volume();
        nfsDataVolume.setName(inputPVC);
        nfsDataVolume.setPersistentVolumeClaim(nfsDataPVC);
        volumes.add(nfsDataVolume);

        // 这个也是解决共享内存的, 别忘了copy, 否则会报错
        V1Volume shmDataVolume = new V1Volume();
        shmDataVolume.setName("dshm");
        V1EmptyDirVolumeSource v1EmptyDirVolumeSource = new V1EmptyDirVolumeSource();
        v1EmptyDirVolumeSource.setMedium("Memory");
        shmDataVolume.setEmptyDir(v1EmptyDirVolumeSource);
        volumes.add(shmDataVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();


        //解决共享内存不足问题 ERROR: Unexpected bus error encountered in worker. This might be caused by insufficient shared memor...
        //https://www.jianshu.com/p/091fca7d9647
        V1VolumeMount dshmMount = new V1VolumeMount();

        dshmMount.setName("dshm");
        dshmMount.setMountPath("/dev/shm");
        volumeMounts.add(dshmMount);

        V1VolumeMount weightMount = new V1VolumeMount();
        weightMount.setName(outputPVC);
        weightMount.setMountPath(containerWeight + "/output");
        // 通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        weightMount.setSubPath(weightRootPath + "/" + weightPath);
        volumeMounts.add(weightMount);

        // 如果是预训练模型，才需要加载预训练权重文件
        if (modelType.equals(ModelTypeConstant.PRETRAINED)) {
            // 加载预训练权重文件
            V1VolumeMount preWeightMount = new V1VolumeMount();
            preWeightMount.setName(outputPVC);
            preWeightMount.setMountPath(containerWeight + "/input");
            preWeightMount.setSubPath(weightRootPath + "/" + preWeightPath);
            volumeMounts.add(preWeightMount);
        }

        V1VolumeMount datasetMount1 = new V1VolumeMount();
        // inputPVC   minio
        // outputPVC  模型生成的文件存放的 pv
        datasetMount1.setName(inputPVC);
        datasetMount1.setMountPath(containerDataset + "/train/annotations"); // /dataset/train 挂在到容器中的路径
        // 通过 subpath 设置挂载到存储空间中具体数据集文件夹, datasetPath即为数据集文件夹
        datasetMount1.setSubPath(datasetRootPath + "/" + trainPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
        volumeMounts.add(datasetMount1);

        V1VolumeMount datasetMount2 = new V1VolumeMount();
        // inputPVC   minio
        // outputPVC  模型生成的文件存放的 pv
        datasetMount2.setName(inputPVC);
        datasetMount2.setMountPath(containerDataset + "/test/annotations"); // /dataset/test 挂在到容器中的路径
        // 通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
        datasetMount2.setSubPath(datasetRootPath + "/" + testPath);
        volumeMounts.add(datasetMount2);

        V1VolumeMount datasetMount3 = new V1VolumeMount();
        // inputPVC   minio
        // outputPVC  模型生成的文件存放的pv
        datasetMount3.setName(inputPVC);
        datasetMount3.setMountPath(containerDataset + "/validation/annotations"); // /dataset/validation 挂在到容器中的路径
        //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
        datasetMount3.setSubPath(datasetRootPath + "/" + valPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
        volumeMounts.add(datasetMount3);

        // 训练数据集
        V1VolumeMount datasetMount5 = new V1VolumeMount();
        datasetMount5.setName(inputPVC);
        datasetMount5.setMountPath(containerDataset + "/train/images");
        datasetMount5.setSubPath(datasetRootPath + "/" + trainImagesPath);
        volumeMounts.add(datasetMount5);

        // 测试数据集
        V1VolumeMount datasetMount6 = new V1VolumeMount();
        datasetMount6.setName(inputPVC);
        datasetMount6.setMountPath(containerDataset + "/test/images");
        datasetMount6.setSubPath(datasetRootPath + "/" + testImagesPath);
        volumeMounts.add(datasetMount6);

        // 验证数据集
        V1VolumeMount datasetMount7 = new V1VolumeMount();
        datasetMount7.setName(inputPVC);
        datasetMount7.setMountPath(containerDataset + "/validation/images");
        datasetMount7.setSubPath(datasetRootPath + "/" + valImagesPath);
        volumeMounts.add(datasetMount7);

        if (yoloPath != null) {
            V1VolumeMount datasetMount4 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的pv
            datasetMount4.setName(inputPVC);
            datasetMount4.setMountPath(containerDataset + "/data.yaml"); // /dataset/validation 挂在到容器中的路径
            //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
            datasetMount4.setSubPath(datasetRootPath + "/" + yoloPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
            volumeMounts.add(datasetMount4);
        }

        if (yoloPath != null) {
            V1VolumeMount datasetMount8 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的pv
            datasetMount8.setName(inputPVC);
            datasetMount8.setMountPath(containerDataset + "/train" + "/data.yaml");
            //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
            datasetMount8.setSubPath(datasetRootPath + "/" + yoloPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
            volumeMounts.add(datasetMount8);
        }

        if (yoloPath != null) {
            V1VolumeMount datasetMount9 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的pv
            datasetMount9.setName(inputPVC);
            datasetMount9.setMountPath(containerDataset + "/validation" + "/data.yaml");
            //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
            datasetMount9.setSubPath(datasetRootPath + "/" + yoloPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
            volumeMounts.add(datasetMount9);
        }

        if (yoloPath != null) {
            V1VolumeMount datasetMount10 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的pv
            datasetMount10.setName(inputPVC);
            datasetMount10.setMountPath(containerDataset + "/test" + "/data.yaml");
            //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
            datasetMount10.setSubPath(datasetRootPath + "/" + yoloPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
            volumeMounts.add(datasetMount10);
        }

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : params.keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(params.get(key));
            envVars.add(v1EnvVar);
        }

        // 添加模型类型的环境变量
        V1EnvVar modelTypeVar = new V1EnvVar();
        modelTypeVar.setName("MODEL_TYPE");
        modelTypeVar.setValue(modelType);
        envVars.add(modelTypeVar);
//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        // 创建 workingModeVar
        V1EnvVar workingModeVar = new V1EnvVar();
        workingModeVar.setName("MODE_WORKING_MODE");
        workingModeVar.setValue("1");
        envVars.add(workingModeVar);

        // 禁用P2P通信
        V1EnvVar ncclP2PDisableVar = new V1EnvVar();
        ncclP2PDisableVar.setName("NCCL_P2P_DISABLE");
        ncclP2PDisableVar.setValue("1");
        envVars.add(ncclP2PDisableVar);

        V1Affinity v1Affinity = new V1Affinity();
        V1NodeAffinity v1NodeAffinity = new V1NodeAffinity();
        V1NodeSelector v1NodeSelector = new V1NodeSelector();

        List<V1NodeSelectorTerm> v1NodeSelectorTerms = new ArrayList<>();
        V1NodeSelectorTerm v1NodeSelectorTerm = new V1NodeSelectorTerm();


        List<V1NodeSelectorRequirement> matchExpressions = new ArrayList<>();
        V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
        nodeSelectorRequirement.setKey(selectorKey);
        nodeSelectorRequirement.setOperator("In");
        nodeSelectorRequirement.setValues(Collections.singletonList(selectorValue));
        matchExpressions.add(nodeSelectorRequirement);
        v1NodeSelectorTerm.setMatchExpressions(matchExpressions);

        v1NodeSelectorTerms.add(v1NodeSelectorTerm);
        v1NodeSelector.setNodeSelectorTerms(v1NodeSelectorTerms);
        v1NodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(v1NodeSelector);
        v1Affinity.setNodeAffinity(v1NodeAffinity);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(cpuNum);
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }
        if (gpuCount == null || gpuCount <= 0) {
            throw new IllegalArgumentException("GPU数量不能为空或小于等于0");
        }
        limits.put("nvidia.com/gpu", new Quantity(String.valueOf(gpuCount)));
        if (StringUtils.isNotBlank(gpuMemory)) {
            Quantity gpuMemoryQuantity = new Quantity(String.valueOf(Integer.parseInt(gpuMemory.split("G")[0]) * 1000));
            limits.put("nvidia.com/gpumem", gpuMemoryQuantity);
        }
        if (StringUtils.isNotBlank(vGpuCores)) {
            Quantity vGpuCoresQuantity = new Quantity(vGpuCores);
            limits.put("nvidia.com/gpucores", vGpuCoresQuantity);
        }

        v1ResourceRequirements.setLimits(limits);

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        // 从GpuManagementService获取禁用的GPU UUID列表
        String disabledGpuUuids = gpuManagementService.getDisabledGpuUuidsString();
        if (StringUtils.isNotBlank(disabledGpuUuids)) {
            v1ObjectMeta.putAnnotationsItem("nvidia.com/nouse-gpuuuid", disabledGpuUuids);
            log.info("训练任务将禁用以下GPU: {}", disabledGpuUuids);
        }

        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .backoffLimit(0)
                        .template(new V1PodTemplateSpec()
                                .metadata(v1ObjectMeta)
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        .affinity(v1Affinity)
                                        .containers(Arrays.asList(new V1Container()
                                                .image(fullImageName)
                                                .imagePullPolicy("IfNotPresent")
                                                .name(jobName)
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        // 状态转换
        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobStateMachine.modelJobQueueEvent(Integer.parseInt(String.valueOf(job.getMetadata().getName().split("-")[3])));
        System.out.println("切分任务完成：" + job.getMetadata().getName().split("-")[3]);
        return jobService.createJob(namespace, job);
    }


    /**
     * @param namespace  命名空间
     * @param jobName    job名称
     * @param weightPath 与 weightRootPath 共同构成存储空间中的具体的权重文件路径
     * @param image      镜像名称
     * @param trainPath  存储空间中具体的 训练数据集 路径，
     * @param testPath   存储空间中具体的 测试数据集 路径
     * @param valPath    存储空间中具体的 验证数据集 路径
     * @param yoloPath   如果是yolo格式的数据集，还需要挂载一个data.yaml文件
     * @param gpuNum     预计使用的gpu数量 Gi
     * @param cpuNum     预计使用的cpu数量
     * @param memoryNum  预计使用的内存数量
     * @param params     容器环境变量参数 即 传递给模型的参数
     * @return
     */
    @RequestMapping("/createTrainJob")
    public V1Job createTrainJob(String namespace, String jobName, String weightPath, String image, String trainPath, String testPath, String valPath, String yoloPath, String gpuNum, String cpuNum, String memoryNum, String gpuMemory, String vGpuCores, @RequestBody Map<String, String> params, String preWeightPath, String modelType) {
        // 将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];

        // 挂载卷声明  使用 pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName(outputPVC);
        V1PersistentVolumeClaimVolumeSource nfsDataPVC = new V1PersistentVolumeClaimVolumeSource();
        nfsDataPVC.claimName(inputPVC);

        List<V1Volume> volumes = new ArrayList<>();

        V1Volume fileDataVolume = new V1Volume();
        fileDataVolume.setName(outputPVC);
        fileDataVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(fileDataVolume);

        V1Volume nfsDataVolume = new V1Volume();
        nfsDataVolume.setName(inputPVC);
        nfsDataVolume.setPersistentVolumeClaim(nfsDataPVC);
        volumes.add(nfsDataVolume);

        V1Volume shmDataVolume = new V1Volume();
        shmDataVolume.setName("dshm");
        V1EmptyDirVolumeSource v1EmptyDirVolumeSource = new V1EmptyDirVolumeSource();
        v1EmptyDirVolumeSource.setMedium("Memory");
        shmDataVolume.setEmptyDir(v1EmptyDirVolumeSource);
        volumes.add(shmDataVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();
        V1VolumeMount weightMount = new V1VolumeMount();
        weightMount.setName(outputPVC);
        weightMount.setMountPath(containerWeight + "/output");
        // 通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        weightMount.setSubPath(weightRootPath + "/" + weightPath);
        volumeMounts.add(weightMount);

        // 如果是预训练模型，才需要加载预训练权重文件
        if (modelType.equals(ModelTypeConstant.PRETRAINED)) {
            // 加载预训练权重文件
            V1VolumeMount preWeightMount = new V1VolumeMount();
            preWeightMount.setName(outputPVC);
            preWeightMount.setMountPath(containerWeight + "/input");
            preWeightMount.setSubPath(weightRootPath + "/" + preWeightPath);
            volumeMounts.add(preWeightMount);
        }

        if (trainPath != null) {
            V1VolumeMount datasetMount1 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的 pv
            datasetMount1.setName(inputPVC);
            datasetMount1.setMountPath(containerDataset + "/train"); // /dataset/train 挂在到容器中的路径
            // 通过 subpath 设置挂载到存储空间中具体数据集文件夹, datasetPath即为数据集文件夹
            datasetMount1.setSubPath(datasetRootPath + "/" + trainPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
            volumeMounts.add(datasetMount1);
        }

        if (testPath != null) {
            V1VolumeMount datasetMount2 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的 pv
            datasetMount2.setName(inputPVC);
            datasetMount2.setMountPath(containerDataset + "/test"); // /dataset/test 挂在到容器中的路径
            // 通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
            datasetMount2.setSubPath(datasetRootPath + "/" + testPath);
            volumeMounts.add(datasetMount2);
        }
        if (valPath != null) {
            V1VolumeMount datasetMount3 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的pv
            datasetMount3.setName(inputPVC);
            datasetMount3.setMountPath(containerDataset + "/validation"); // /dataset/validation 挂在到容器中的路径
            //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
            datasetMount3.setSubPath(datasetRootPath + "/" + valPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
            volumeMounts.add(datasetMount3);
        }


        if (yoloPath != null) {
            V1VolumeMount datasetMount4 = new V1VolumeMount();
            // inputPVC   minio
            // outputPVC  模型生成的文件存放的pv
            datasetMount4.setName(inputPVC);
            datasetMount4.setMountPath(containerDataset + "/data.yaml"); // /dataset/validation 挂在到容器中的路径
            //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
            datasetMount4.setSubPath(datasetRootPath + "/" + yoloPath); // dubhe-dev/dataset/102/versionFile/V0001/CreateML
            volumeMounts.add(datasetMount4);
        }

        //解决共享内存不足问题 ERROR: Unexpected bus error encountered in worker. This might be caused by insufficient shared memor...
        //https://www.jianshu.com/p/091fca7d9647
        V1VolumeMount dshmMount = new V1VolumeMount();

        dshmMount.setName("dshm");
        dshmMount.setMountPath("/dev/shm");
        volumeMounts.add(dshmMount);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : params.keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(params.get(key));
            envVars.add(v1EnvVar);
        }

        // 添加模型类型的环境变量
        V1EnvVar modelTypeVar = new V1EnvVar();
        modelTypeVar.setName("MODEL_TYPE");
        modelTypeVar.setValue(modelType);
        envVars.add(modelTypeVar);

        // 创建 workingModeVar
        V1EnvVar workingModeVar = new V1EnvVar();
        workingModeVar.setName("MODE_WORKING_MODE");
        workingModeVar.setValue("1");

        // 检查是否已经存在具有相同名称的变量
        boolean exists = envVars.stream()
                .anyMatch(envVar -> envVar.getName().equals(workingModeVar.getName()));

        // 如果不存在，则添加
        if (!exists) {
            envVars.add(workingModeVar);
        }

        // 禁用P2P通信
        V1EnvVar ncclP2PDisableVar = new V1EnvVar();
        ncclP2PDisableVar.setName("NCCL_P2P_DISABLE");
        ncclP2PDisableVar.setValue("1");
        envVars.add(ncclP2PDisableVar);
//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        V1Affinity v1Affinity = new V1Affinity();
        V1NodeAffinity v1NodeAffinity = new V1NodeAffinity();
        V1NodeSelector v1NodeSelector = new V1NodeSelector();

        List<V1NodeSelectorTerm> v1NodeSelectorTerms = new ArrayList<>();
        V1NodeSelectorTerm v1NodeSelectorTerm = new V1NodeSelectorTerm();


        List<V1NodeSelectorRequirement> matchExpressions = new ArrayList<>();
        V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
        nodeSelectorRequirement.setKey(selectorKey);
        nodeSelectorRequirement.setOperator("In");
        nodeSelectorRequirement.setValues(Collections.singletonList(selectorValue));
        matchExpressions.add(nodeSelectorRequirement);
        v1NodeSelectorTerm.setMatchExpressions(matchExpressions);

        v1NodeSelectorTerms.add(v1NodeSelectorTerm);
        v1NodeSelector.setNodeSelectorTerms(v1NodeSelectorTerms);
        v1NodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(v1NodeSelector);
        v1Affinity.setNodeAffinity(v1NodeAffinity);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();
        // if (StringUtils.isNotBlank(gpuNum)) {
        //     Quantity gpuQuantity = new Quantity(gpuNum);
        //     limits.put("aliyun.com/gpu-mem", gpuQuantity);
        // }

        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(cpuNum);
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }

        limits.put("nvidia.com/gpu", new Quantity(String.valueOf(1)));
        if (StringUtils.isNotBlank(gpuMemory)) {
            // 4Gi -> 4000
            Quantity gpuMemoryQuantity = new Quantity(String.valueOf(Integer.parseInt(gpuMemory.split("G")[0]) * 1000));
            limits.put("nvidia.com/gpumem", gpuMemoryQuantity);
        }
        if (StringUtils.isNotBlank(vGpuCores)) {
            Quantity vGpuCoresQuantity = new Quantity(vGpuCores);
            limits.put("nvidia.com/gpucores", vGpuCoresQuantity);
        }

        v1ResourceRequirements.setLimits(limits);

        V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
        // 从GpuManagementService获取禁用的GPU UUID列表
        String disabledGpuUuids = gpuManagementService.getDisabledGpuUuidsString();
        if (StringUtils.isNotBlank(disabledGpuUuids)) {
            v1ObjectMeta.putAnnotationsItem("nvidia.com/nouse-gpuuuid", disabledGpuUuids);
            log.info("推理任务将禁用以下GPU: {}", disabledGpuUuids);
        }

        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .backoffLimit(0)
                        .template(new V1PodTemplateSpec()
                                .metadata(v1ObjectMeta)
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        .affinity(v1Affinity)
                                        .containers(Collections.singletonList(new V1Container()
                                                .image(fullImageName)
                                                .imagePullPolicy("IfNotPresent")
                                                .name(jobName)
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        // 状态转换
        ModelJobStateMachine modelJobStateMachine = SpringContextHolder.getBean("modelJobStateMachine");
        modelJobStateMachine.modelJobDirectQueueEvent(Integer.parseInt(String.valueOf(job.getMetadata().getName().split("-")[3])));
        return jobService.createJob(namespace, job);
    }

    /**
     * 删除指定工作负载 ——  job
     *
     * @param namespace 名称空间
     * @param jobName   job名称
     * @return
     */
    @RequestMapping("/deleteJob")
    public V1Status deleteJob(String namespace, String jobName) {
        return jobService.deleteJob(namespace, jobName);
    }

    @RequestMapping("/getV1Job")
    public V1Job getJob(String namespace, String jobName) {
        return jobService.getJob(namespace, jobName);
    }

    @RequestMapping("/getV1JobByDefaultNamespace")
    public Msg<V1Job> getJobByDefaultNamespace(String jobName) {
        return new Msg<>(MsgCode.SUCCEED, jobService.getJob(namespace, jobName));
    }

    @RequestMapping("/createConvertJob")
    public V1Job createConvertJob(String namespace, String jobName, String weightPath, String image, String weightRootPath, String gpuNum, String cpuNum, String memoryNum) {
//        if(jobService.getJob(jobService.getApiClient(k8sConfig),namespace, jobName)!=null){
//            return null;
//        }
        System.out.println("namespace:" + namespace);
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);
        //挂载卷声明  使用pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName("file-data");

        List<V1Volume> volumes = new ArrayList<>();
        V1Volume pvcVolume = new V1Volume();
        pvcVolume.setName("file-data");
        pvcVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(pvcVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        V1VolumeMount modelMount = new V1VolumeMount();
        modelMount.setName("file-data");
        modelMount.setMountPath("/rknn-toolkit2/examples/pytorch/model");
        //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
        modelMount.setSubPath(weightRootPath + "/" + weightPath);
        volumeMounts.add(modelMount);

        V1VolumeMount resultMount = new V1VolumeMount();
        resultMount.setName("file-data");
        resultMount.setMountPath("/rknn-toolkit2/examples/pytorch/result");
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        resultMount.setSubPathExpr(weightRootPath + "/" + jobName);
        volumeMounts.add(resultMount);

        List<V1EnvVar> envVars = new ArrayList<>();
//        for (String key : params.keySet()) {
//            V1EnvVar v1EnvVar = new V1EnvVar();
//            v1EnvVar.setName(key);
//            v1EnvVar.setValue(params.get(key));
//            envVars.add(v1EnvVar);
//        }
        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();


        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(String.valueOf(1));
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);


        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .backoffLimit(0)
                        .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta())
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        .containers(Arrays.asList(new V1Container()
                                                .image("dockerhub.kubekey.local/image_registry_of_user/modelconvert:1.0")
                                                .imagePullPolicy("IfNotPresent")
                                                .name(jobName)
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        return jobService.createJob(namespace, job);
    }

    /**
     * .pt转.engine
     *
     * @param namespace
     * @param jobName
     * @param weightPath
     * @param image
     * @param weightRootPath
     * @param gpuNum
     * @param cpuNum
     * @param memoryNum
     * @return
     */
    @RequestMapping("/createConvertEngineJob")
    public V1Job createConvertEngineJob(String namespace, String jobName, String weightPath, String image, String weightRootPath, String gpuNum, String cpuNum, String memoryNum) {
//        if(jobService.getJob(jobService.getApiClient(k8sConfig),namespace, jobName)!=null){
//            return null;
//        }
        System.out.println("namespace:" + namespace);
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);
        //挂载卷声明  使用pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName("file-data");

        List<V1Volume> volumes = new ArrayList<>();
        V1Volume pvcVolume = new V1Volume();
        pvcVolume.setName("file-data");
        pvcVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(pvcVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        V1VolumeMount modelMount = new V1VolumeMount();
        modelMount.setName("file-data");
        modelMount.setMountPath("/root/yolov5-7.0/weights/model");
        //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
        modelMount.setSubPath(weightRootPath + "/" + weightPath);
        volumeMounts.add(modelMount);

        V1VolumeMount resultMount = new V1VolumeMount();
        resultMount.setName("file-data");
        resultMount.setMountPath("/root/DeepStream-Yolo/result");
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        resultMount.setSubPathExpr(weightRootPath + "/" + jobName);
        volumeMounts.add(resultMount);

        List<V1EnvVar> envVars = new ArrayList<>();
//        for (String key : params.keySet()) {
//            V1EnvVar v1EnvVar = new V1EnvVar();
//            v1EnvVar.setName(key);
//            v1EnvVar.setValue(params.get(key));
//            envVars.add(v1EnvVar);
//        }
//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(cpuNum);
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);


        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta())
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        .containers(Arrays.asList(new V1Container()
                                                .image("dockerhub.kubekey.local/image_registry_of_user/modelconvert:2.0")
                                                .imagePullPolicy("IfNotPresent")
                                                .name(jobName)
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        return jobService.createJob(namespace, job);
    }

    @RequestMapping("/createPreprocessJob")
    public V1Job createPreprocessJob(String namespace, String jobName, String image, String datasetSrcPath, String datasetTrgPath, String cpuNum, String memoryNum, String label, @RequestBody Map<String, Object> params) {
//        if(jobService.getJob(jobService.getApiClient(k8sConfig),namespace, jobName)!=null){
//            return null;
//        }
        System.out.println("namespace:" + namespace);
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);
        //挂载卷声明  使用pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName("file-data");

        List<V1Volume> volumes = new ArrayList<>();
        V1Volume pvcVolume = new V1Volume();
        pvcVolume.setName("file-data");
        pvcVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(pvcVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        V1VolumeMount srcDatasetMount = new V1VolumeMount();
        srcDatasetMount.setName("file-data");
        srcDatasetMount.setMountPath("/srcDataset");
        srcDatasetMount.setReadOnly(true);
        //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
        srcDatasetMount.setSubPath(datasetSrcPath);
        volumeMounts.add(srcDatasetMount);

        V1VolumeMount trgDatasetMount = new V1VolumeMount();
        trgDatasetMount.setName("file-data");
        trgDatasetMount.setMountPath("/trgDataset");
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        trgDatasetMount.setSubPathExpr(datasetTrgPath);
        volumeMounts.add(trgDatasetMount);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : params.keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(params.get(key).toString());
            envVars.add(v1EnvVar);
        }

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        if (StringUtils.isNotBlank(cpuNum)) {
            Quantity cpuQuantity = new Quantity(cpuNum);
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(memoryNum)) {
            Quantity memoryQuantity = new Quantity(memoryNum);
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);


        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta())
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
//                                        .affinity(v1Affinity)
                                        .containers(Arrays.asList(new V1Container()
                                                .image(fullImageName)
                                                .imagePullPolicy("IfNotPresent")
                                                .name(jobName)
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        return jobService.createJob(namespace, job);
    }

    /**
     * 创建自动标注作业
     * image处理： 镜像 yolov8-det-train_v1  ---》 dockerhub.kubekey.local/image_registry_of_user/yolov8-det-train:v1
     *
     * @return
     */
    @RequestMapping("/createAutoLabelJob")
    public Msg<String> createAutoLabelJob(@RequestBody AutoLabelJobRequest request) {

        //String jobName, Long modelGenerationId, , ,,  @RequestBody, @RequestBody
        String jobName = request.getJobName();
        Long modelGenerationId = request.getModelGenerationId();
        String standardJobName = request.getStandardJobName();
        String datasetPath = request.getDatasetPath();
        Boolean useDefault = request.getUseDefault();
        String imageUrl = request.getImageUrl();
        Map<String, String> params = request.getParams();
        List<String> labelNames = request.getLabelNames();
        Boolean clearExistingLabels = request.getClearExistingLabels();
        Boolean requireManualConfirmation = request.getRequireManualConfirmation();

        System.out.println("namespace:" + namespace);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : params.keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(params.get(key));
            envVars.add(v1EnvVar);
        }
        List<V1Volume> volumes = new ArrayList<>();
        List<V1VolumeMount> volumeMounts = new ArrayList<>();
        Long datasetId = Long.parseLong(datasetPath.split("/")[1]);
        V1PersistentVolumeClaimVolumeSource nfsDataPVC = new V1PersistentVolumeClaimVolumeSource();
        nfsDataPVC.claimName(inputPVC);
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName(outputPVC);

        V1Volume nfsDataVolume = new V1Volume();
        nfsDataVolume.setName(inputPVC);
        nfsDataVolume.setPersistentVolumeClaim(nfsDataPVC);
        volumes.add(nfsDataVolume);
        V1Volume fileDataVolume = new V1Volume();
        fileDataVolume.setName(outputPVC);
        fileDataVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(fileDataVolume);

        String image;
        if(!imageUrl.isEmpty() && useDefault) {
            // 这意味着用户选择了使用默认的模型，让他自己选的镜像
            image = imageUrl;
            V1EnvVar useDefaultEnvVar = new V1EnvVar();
            useDefaultEnvVar.setName("USE_DEFAULT");
            useDefaultEnvVar.setValue(String.valueOf(true));
            envVars.add(useDefaultEnvVar);
        } else if (request.getDirectWeightPath() != null) {
            // 自迭代训练第 N 轮（N>1）：直接使用上一轮训练产出的权重，跳过 modelGenerationId 查询
            image = imageUrl;
            String weightPath = request.getDirectWeightPath();

            V1VolumeMount weightMount = new V1VolumeMount();
            weightMount.setName(outputPVC);
            weightMount.setMountPath(containerWeight + "/input");
            weightMount.setSubPath(weightRootPath + "/" + weightPath);
            volumeMounts.add(weightMount);

            V1EnvVar useDefaultEnvVar = new V1EnvVar();
            useDefaultEnvVar.setName("USE_DEFAULT");
            useDefaultEnvVar.setValue(String.valueOf(false));
            envVars.add(useDefaultEnvVar);

            // data.yaml 由调用方通过 labelNames 生成，此处根据 labelNames 列表构建
            if (labelNames != null && !labelNames.isEmpty()) {
                Map<Integer, String> labelMap = new java.util.LinkedHashMap<>();
                for (int i = 0; i < labelNames.size(); i++) {
                    labelMap.put(i, labelNames.get(i));
                }
                String yamlContent = generateYoloDataYamlFromMap(labelMap);
                String dataYamlPath = "auto-temp/" + jobName + "/data.yaml";
                try {
                    minioUtil.writeString(bucketName, dataYamlPath, yamlContent);
                    log.info("Successfully wrote data.yaml for self-iteration job '{}' to MinIO path: {}/{}", jobName, bucketName, dataYamlPath);
                } catch (Exception e) {
                    log.error("Failed to write data.yaml to MinIO for self-iteration job {}", jobName, e);
                    throw new RuntimeException("Failed to prepare data.yaml for self-iteration auto-label job.", e);
                }
                V1VolumeMount datasetVersionFileMount = new V1VolumeMount();
                datasetVersionFileMount.setName(inputPVC);
                datasetVersionFileMount.setMountPath("/infer_dep");
                datasetVersionFileMount.setSubPath(bucketName + "/" + org.apache.commons.lang.StringUtils.substringBeforeLast(dataYamlPath, "/"));
                volumeMounts.add(datasetVersionFileMount);
            }
        } else {
            // 修改之后传递引导式实体 -> 拿到的也是以前传过来的image(url)
            ModelGeneration modelGeneration = modelGenerationRepo.findById(modelGenerationId).orElse(null);
            if (modelGeneration == null) {
                return new Msg<>(MsgCode.FAILED, "未找到该自动标注任务");
            }
            if (!modelGeneration.getModelExplore().getTrainModelVersion().isAutoLabel()) {
                return new Msg<>(MsgCode.FAILED, "该训练镜像不支持自动标注");
            }
            // image: train-with-autolabel_image1744097827952
            image = modelGeneration.getModelExplore().getTrainModelVersion().getUrl();

            // 这里的trainDataset实际上是一个trainDatasetVersion
            // 这个weightPath就是选中的任务名 job-local-cz-2222
            String weightPath;
            if (modelGeneration.getIsGuided()) {
                if (modelGeneration.getLatestJobId() <= 0) {
                    return new Msg<>(MsgCode.FAILED, "引导式自动标注训练任务缺少最新训练任务ID");
                }
                ModelJob latestModelJob = modelJobRepo.findModelJobById(modelGeneration.getLatestJobId());
                if (latestModelJob == null || latestModelJob.getWeightPath() == null || latestModelJob.getWeightPath().trim().isEmpty()) {
                    return new Msg<>(MsgCode.FAILED, "引导式自动标注训练任务缺少权重路径");
                }
                weightPath = latestModelJob.getWeightPath();
            } else {
                weightPath = standardJobName;
            }

            ModelJob modelJob = modelJobRepo.findModelJobByNameWithTrainDatasetVersions(weightPath);
            if (modelJob == null) {
                return new Msg<>(MsgCode.FAILED, "未找到自动标注训练任务：" + weightPath);
            }
            List<Long> trainDatasetVersionIds = modelJob.getTrainDatasetVersions();
            if (trainDatasetVersionIds == null || trainDatasetVersionIds.isEmpty()) {
                return new Msg<>(MsgCode.FAILED, "自动标注训练任务缺少训练数据版本：" + weightPath);
            }

            V1VolumeMount weightMount = new V1VolumeMount();
            weightMount.setName(outputPVC);
            weightMount.setMountPath(containerWeight + "/input");
            // 通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径 modelGeneration.getModelConvert().getWeightPath()只有引导式这么拿
            weightMount.setSubPath(weightRootPath + "/" + weightPath);
            volumeMounts.add(weightMount);

            V1EnvVar useDefaultEnvVar = new V1EnvVar();
            useDefaultEnvVar.setName("USE_DEFAULT");
            useDefaultEnvVar.setValue(String.valueOf(false));
            envVars.add(useDefaultEnvVar);
            // 获取datasetVersion仅为了拿标注格式
            DatasetVersionVO datasetVersion = dubheDataFeign.selectDatasetVersionById(dubheUtils.getAuthorization(), trainDatasetVersionIds.get(0)).getData();
            String format = datasetVersion.getFormat();
            // YOLO和COCO区分处理
            if ("YOLO".equals(format) || "Segment-YOLO".equals(format)) {
                // 把引导式对应的模型传进来, 如果选择使用默认模型实际上不需要这个挂载， 因为data.yaml应该跟着模型走
                V1VolumeMount datasetVersionFileMount = new V1VolumeMount();
                datasetVersionFileMount.setName(inputPVC);
                datasetVersionFileMount.setMountPath("/infer_dep");
                // nfs\cz-dev\auto-temp\autoLabelxxx\data.yaml
                ModelJob labelModelJob = modelJobRepo.findModelJobByNameWithSelectedLabels(weightPath);
                Map<Integer, String> selectedLabels = labelModelJob != null
                        ? labelModelJob.getSelectedLabels()
                        : null;
                if (selectedLabels == null || selectedLabels.isEmpty()) {
                    return new Msg<>(MsgCode.FAILED, "自动标注训练任务缺少训练标签映射：" + weightPath);
                }
                String yamlContent = generateYoloDataYamlFromMap(selectedLabels);
                String dataYamlPath = "auto-temp/" + jobName + "/data.yaml";
                try {
                    minioUtil.writeString(bucketName, dataYamlPath, yamlContent);
                    log.info("Successfully wrote data.yaml for job '{}' to MinIO path: {}/{}", jobName, bucketName, dataYamlPath);
                } catch (Exception e) {
                    log.error("Failed to write data.yaml to MinIO for job {}", jobName, e);
                    throw new RuntimeException("Failed to prepare data.yaml for YOLO job.", e);
                }
                datasetVersionFileMount.setSubPath(bucketName + "/" + org.apache.commons.lang.StringUtils.substringBeforeLast(dataYamlPath, "/"));
                volumeMounts.add(datasetVersionFileMount);
            }
            // else if ("COCO".equals(format)) {
            //     List<Long> list = new ArrayList<>();
            //     list.add(trainDatasetVersionId);
            //     V1VolumeMount datasetVersionFileMount = new V1VolumeMount();
            //     datasetVersionFileMount.setName(inputPVC);
            //     datasetVersionFileMount.setMountPath(containerDataset + "/autolabel/labelInfo");
            //     // nfs\cz-dev\dataset\472\versionFile\V0001\CreateML\annotations\coco_info.json
            //     datasetVersionFileMount.setSubPath(datasetRootPath + "/dataset/" + datasetVersion.getDatasetId() + "/versionFile/" + dubheDataFeign.findDatasetVersionsByIds(dubheUtils.getCurrentAuthorization(), list).getData().get(0).getVersionName() + "createML/annotations/coco_info.json");
            //     volumeMounts.add(datasetVersionFileMount);
            //
            //     V1EnvVar datasetVersionEnvVar = new V1EnvVar();
            //     datasetVersionEnvVar.setName("FILE_NAME");
            //     datasetVersionEnvVar.setValue("coco_info.json");
            //     envVars.add(datasetVersionEnvVar);
            // } else if ("Segment".equals(format)) {
            //
            // }
        }
        V1VolumeMount datasetMount = new V1VolumeMount();
        datasetMount.setName(inputPVC);
        datasetMount.setMountPath(containerDataset + "/autolabel");
        //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹  //cz-dev/dataset/102
        // 这里的datasetPath不应该用当前的id，而是引导式用的datasetId
        datasetMount.setSubPath(datasetRootPath + "/dataset/" + datasetId);
        volumeMounts.add(datasetMount);

        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = image.split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);

//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        V1Affinity v1Affinity = new V1Affinity();
        V1NodeAffinity v1NodeAffinity = new V1NodeAffinity();
        V1NodeSelector v1NodeSelector = new V1NodeSelector();

        List<V1NodeSelectorTerm> v1NodeSelectorTerms = new ArrayList<>();
        V1NodeSelectorTerm v1NodeSelectorTerm = new V1NodeSelectorTerm();


        List<V1NodeSelectorRequirement> matchExpressions = new ArrayList<>();
        V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
        nodeSelectorRequirement.setKey(selectorKey);
        nodeSelectorRequirement.setOperator("In");
        nodeSelectorRequirement.setValues(Collections.singletonList(selectorValue));
        matchExpressions.add(nodeSelectorRequirement);
        v1NodeSelectorTerm.setMatchExpressions(matchExpressions);

        v1NodeSelectorTerms.add(v1NodeSelectorTerm);
        v1NodeSelector.setNodeSelectorTerms(v1NodeSelectorTerms);
        v1NodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(v1NodeSelector);
        v1Affinity.setNodeAffinity(v1NodeAffinity);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();

        v1ResourceRequirements.setLimits(limits);

        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(jobName))
                .spec(new V1JobSpec()
                        .backoffLimit(1)
                        .template(new V1PodTemplateSpec()
                                .metadata(new V1ObjectMeta())
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        .affinity(v1Affinity)
                                        .containers(Collections.singletonList(new V1Container()
                                                .image(fullImageName)
                                                .imagePullPolicy("IfNotPresent")
                                                .name(jobName)
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        try {
            jobService.createJob(namespace, job);
        } catch (RuntimeException e) {
            log.error("Failed to start auto label job: {}", e.getMessage());
            return new Msg<>(MsgCode.FAILED_TO_START_AUTO_LABEL);
        }
        if (labelNames == null || labelNames.isEmpty()) {
            // 标签为空或列表为空的处理逻辑
            dubheDataFeign.autoLabelStart(dubheUtils.getAuthorization(), datasetId);
        } else {
            // 使用新的方法，传递clearExistingLabels和requireManualConfirmation参数
            Boolean clearLabels = (clearExistingLabels != null && clearExistingLabels) ? true : false;
            Boolean manualConfirm = (requireManualConfirmation != null) ? requireManualConfirmation : true; // 默认需要人工确认
            dubheDataFeign.autoLabelStartWithNamesAndClearOption(dubheUtils.getAuthorization(), datasetId, clearLabels, manualConfirm, jobName, labelNames);
        }


        // Submit a task to watch job status
        jobService.watchJobAsync(namespace, jobName, datasetId);

        return new Msg<>(MsgCode.SUCCEED);
    }

    /**
     * 从标签Map生成YOLO data.yaml格式的字符串内容。
     * 此版本生成一个 `names` 字段，其值为一个从索引到名称的映射。
     * Example Output:
     * names:
     *   0: helmet
     *   1: person
     *
     * @param selectedLabels Map, 键是类别索引，值是类别名称. e.g., {0: "helmet", 1: "person"}
     * @return YAML格式的字符串
     */
    public String generateYoloDataYamlFromMap(Map<Integer, String> selectedLabels) {
        if (selectedLabels == null || selectedLabels.isEmpty()) {
            return "names: {}\n";
        }
        StringBuilder yamlBuilder = new StringBuilder("names:\n");

        // 按key（索引）排序以确保文件内容是确定的
        selectedLabels.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    // 拼接 "  key: value\n" 格式的字符串
                    yamlBuilder.append("  ") // 两个空格的缩进
                            .append(entry.getKey())
                            .append(": ")
                            .append(entry.getValue())
                            .append("\n");
                });
        return yamlBuilder.toString();
    }


        // 发起模型验证任务，并将其放入缓存中
    @PostMapping(value = "/validation/create")
    public Msg<String> modelValidationStart(@RequestBody ModelValidationDTO modelValidationDTO) {

        // 将当前任务放入缓存 完成时间为-1代表未完成
        ModelValidationVO modelValidationVO = new ModelValidationVO();
        modelValidationVO.setFinishTime(-1L);
        modelValidationVO.setName(modelValidationDTO.getJobName());
        modelValidationVO.setPath(modelValidationDTO.getPath());
        String zKey = RedisConstants.MODEL_VALIDATE_PREFIX + modelValidationDTO.getId().toString();
        redisUtil.zSet(zKey, modelValidationVO, -1L, RedisConstants.CACHE_TTL, TimeUnit.SECONDS);

        System.out.println("namespace:" + namespace);
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = modelValidationDTO.getImage().split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        System.out.println("fullImageName:" + fullImageName);

        // 挂载卷声明  使用 pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName(outputPVC);
        V1PersistentVolumeClaimVolumeSource nfsDataPVC = new V1PersistentVolumeClaimVolumeSource();
        nfsDataPVC.claimName(inputPVC);

        List<V1Volume> volumes = new ArrayList<>();

        V1Volume fileDataVolume = new V1Volume();
        fileDataVolume.setName(outputPVC);
        fileDataVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(fileDataVolume);

        V1Volume nfsDataVolume = new V1Volume();
        nfsDataVolume.setName(inputPVC);
        nfsDataVolume.setPersistentVolumeClaim(nfsDataPVC);
        volumes.add(nfsDataVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        V1VolumeMount datasetMount = new V1VolumeMount();
        datasetMount.setName(inputPVC);
        datasetMount.setMountPath(containerDataset + "/user_validation"); //  /dataset/user_validation     其中的images文件夹存放输入待验证的图片  output文件夹放验证完的图片
        //通过subpath设置挂载到存储空间中具体数据集文件夹,datasetPath即为数据集文件夹
        datasetMount.setSubPath(datasetRootPath + "/" + modelValidationDTO.getPath()); //dubhe-dev/dataset/102   其中的images文件夹存放输入待验证的图片  output文件夹放验证完的图片
        volumeMounts.add(datasetMount);

        V1VolumeMount weightMount = new V1VolumeMount();
        weightMount.setName(outputPVC);
        weightMount.setMountPath(containerWeight + "/input");
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        weightMount.setSubPath(weightRootPath + "/" + modelValidationDTO.getWeightPath());
        volumeMounts.add(weightMount);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : modelValidationDTO.getParams().keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(modelValidationDTO.getParams().get(key));
            envVars.add(v1EnvVar);
        }
//        //ali-gpushare需要设置环境变量
//        V1EnvVar gpuEnvVar = new V1EnvVar();
//        gpuEnvVar.setName("NVIDIA_VISIBLE_DEVICES");
//        gpuEnvVar.setValue("all");
//        envVars.add(gpuEnvVar);

        V1Affinity v1Affinity = new V1Affinity();
        V1NodeAffinity v1NodeAffinity = new V1NodeAffinity();
        V1NodeSelector v1NodeSelector = new V1NodeSelector();

        List<V1NodeSelectorTerm> v1NodeSelectorTerms = new ArrayList<>();
        V1NodeSelectorTerm v1NodeSelectorTerm = new V1NodeSelectorTerm();


        List<V1NodeSelectorRequirement> matchExpressions = new ArrayList<>();
        V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
        nodeSelectorRequirement.setKey(selectorKey);
        nodeSelectorRequirement.setOperator("In");
        nodeSelectorRequirement.setValues(Collections.singletonList(selectorValue));
        matchExpressions.add(nodeSelectorRequirement);
        v1NodeSelectorTerm.setMatchExpressions(matchExpressions);

        v1NodeSelectorTerms.add(v1NodeSelectorTerm);
        v1NodeSelector.setNodeSelectorTerms(v1NodeSelectorTerms);
        v1NodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(v1NodeSelector);
        v1Affinity.setNodeAffinity(v1NodeAffinity);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();
        if (StringUtils.isNotBlank(modelValidationDTO.getGpuNum())) {
            Quantity gpuQuantity = new Quantity(modelValidationDTO.getGpuNum());
            limits.put("aliyun.com/gpu-mem", gpuQuantity);
        }

        if (StringUtils.isNotBlank(modelValidationDTO.getCpuNum())) {
            Quantity cpuQuantity = new Quantity(modelValidationDTO.getCpuNum());
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(modelValidationDTO.getMemoryNum())) {
            Quantity memoryQuantity = new Quantity(modelValidationDTO.getMemoryNum());
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);

        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(modelValidationDTO.getJobName()))
                .spec(new V1JobSpec()
                        .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta())
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        .affinity(v1Affinity)
                                        .containers(Arrays.asList(new V1Container()
                                                .image(fullImageName)
                                                .imagePullPolicy("IfNotPresent")
                                                .name(modelValidationDTO.getJobName())
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
        jobService.createJob(namespace, job);
        // 异步查询job状态
        jobService.watchJobAsync(namespace, modelValidationDTO.getJobName(), zKey, modelValidationVO);
        return new Msg<>(MsgCode.SUCCEED);
    }

    // 发起模型转换任务，并将其放入缓存中

    @PostMapping(value = "/convert/create")
    public Msg<V1Job> modelConvertStart(@RequestBody ModelConvertDTO modelConvertDTO, @RequestParam(required = false) String targetObjName) {
        //将传入的算法版本名称拼接成  仓库名称/项目名称/image:tag
        String[] imageName = modelConvertDTO.getImage().split("_");
        String fullImageName = harborUrl + "/" + harborProject + "/" + imageName[0] + ':' + imageName[1];
        // 挂载卷声明  使用 pvc
        V1PersistentVolumeClaimVolumeSource fileDataPVC = new V1PersistentVolumeClaimVolumeSource();
        fileDataPVC.claimName(outputPVC);
        V1PersistentVolumeClaimVolumeSource nfsDataPVC = new V1PersistentVolumeClaimVolumeSource();
        nfsDataPVC.claimName(inputPVC);

        List<V1Volume> volumes = new ArrayList<>();

        V1Volume fileDataVolume = new V1Volume();
        fileDataVolume.setName(outputPVC);
        fileDataVolume.setPersistentVolumeClaim(fileDataPVC);
        volumes.add(fileDataVolume);

        V1Volume nfsDataVolume = new V1Volume();
        nfsDataVolume.setName(inputPVC);
        nfsDataVolume.setPersistentVolumeClaim(nfsDataPVC);
        volumes.add(nfsDataVolume);

        List<V1VolumeMount> volumeMounts = new ArrayList<>();

        if (targetObjName != null) {
            V1VolumeMount convertDatasetMount = new V1VolumeMount();
            convertDatasetMount.setName(inputPVC);
            convertDatasetMount.setMountPath(containerDataset + "/convert/images");
            convertDatasetMount.setSubPath(datasetRootPath + "/" + targetObjName);
            volumeMounts.add(convertDatasetMount);
        }

        V1VolumeMount weightMount = new V1VolumeMount();
        weightMount.setName(outputPVC);
        weightMount.setMountPath(containerWeight + "/input"); // /weight/output
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        weightMount.setSubPath(weightRootPath + "/" + modelConvertDTO.getWeightPath()); // weight/job-local-merge-440
        volumeMounts.add(weightMount);

        V1VolumeMount outputWeightMount = new V1VolumeMount();
        outputWeightMount.setName(outputPVC);
        outputWeightMount.setMountPath(containerWeight + "/output_convert"); // /weight/output_convert
        //通过setSubPathExpr设置挂载挂载到存储空间中具体的权重文件路径
        outputWeightMount.setSubPath(weightRootPath + "/" + modelConvertDTO.getWeightPath() + "/" + modelConvertDTO.getOutputWeightPath()); // weight/job-local-merge-440/job-local-merge-440-convert-05-09-11
        volumeMounts.add(outputWeightMount);

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String key : modelConvertDTO.getParams().keySet()) {
            V1EnvVar v1EnvVar = new V1EnvVar();
            v1EnvVar.setName(key);
            v1EnvVar.setValue(modelConvertDTO.getParams().get(key));
            envVars.add(v1EnvVar);
        }

        V1Affinity v1Affinity = new V1Affinity();
        V1NodeAffinity v1NodeAffinity = new V1NodeAffinity();
        V1NodeSelector v1NodeSelector = new V1NodeSelector();

        List<V1NodeSelectorTerm> v1NodeSelectorTerms = new ArrayList<>();
        V1NodeSelectorTerm v1NodeSelectorTerm = new V1NodeSelectorTerm();


        List<V1NodeSelectorRequirement> matchExpressions = new ArrayList<>();
        V1NodeSelectorRequirement nodeSelectorRequirement = new V1NodeSelectorRequirement();
        nodeSelectorRequirement.setKey(selectorKey);
        nodeSelectorRequirement.setOperator("In");
        nodeSelectorRequirement.setValues(Collections.singletonList(selectorValue));
        matchExpressions.add(nodeSelectorRequirement);
        v1NodeSelectorTerm.setMatchExpressions(matchExpressions);

        v1NodeSelectorTerms.add(v1NodeSelectorTerm);
        v1NodeSelector.setNodeSelectorTerms(v1NodeSelectorTerms);
        v1NodeAffinity.setRequiredDuringSchedulingIgnoredDuringExecution(v1NodeSelector);
        v1Affinity.setNodeAffinity(v1NodeAffinity);

        //资源
        V1ResourceRequirements v1ResourceRequirements = new V1ResourceRequirements();
        Map<String, Quantity> limits = new HashMap<>();
        if (StringUtils.isNotBlank(modelConvertDTO.getGpuNum())) {
            Quantity gpuQuantity = new Quantity(modelConvertDTO.getGpuNum());
            limits.put("aliyun.com/gpu-mem", gpuQuantity);
        }

        if (StringUtils.isNotBlank(modelConvertDTO.getCpuNum())) {
            Quantity cpuQuantity = new Quantity(String.valueOf(1));
            limits.put("cpu", cpuQuantity);
        }
        if (StringUtils.isNotBlank(modelConvertDTO.getMemoryNum())) {
            Quantity memoryQuantity = new Quantity(modelConvertDTO.getMemoryNum());
            limits.put("memory", memoryQuantity);
        }

        v1ResourceRequirements.setLimits(limits);

        V1Job job = new V1Job()
                .apiVersion("batch/v1")
                .kind("Job")
                .metadata(new V1ObjectMeta().name(modelConvertDTO.getJobName()))
                .spec(new V1JobSpec()
                        .backoffLimit(0)
                        .template(new V1PodTemplateSpec().metadata(new V1ObjectMeta())
                                .spec(new V1PodSpec()
                                        .restartPolicy("OnFailure")
                                        //.runtimeClassName("runc")
                                        .affinity(v1Affinity)
                                        .containers(Arrays.asList(new V1Container()
                                                .image(fullImageName)
                                                .imagePullPolicy("IfNotPresent")
                                                .name(modelConvertDTO.getJobName())
                                                .volumeMounts(volumeMounts)
                                                .env(envVars).resources(v1ResourceRequirements)))
                                        .volumes(volumes))));
//        // 异步查询job状态
//        jobService.watchJobAsync(namespace, modelConvertDTO.getJobName(), zKey, modelValidationVO);
        return new Msg<>(MsgCode.SUCCEED, jobService.createJob(namespace, job));
    }


    // 返回队列中的任务数量
    @GetMapping("/getJobCount")
    public Msg<Integer> getTaskCount() {
        int count = jobService.getQueuedTaskCount();
        return new Msg<>(MsgCode.SUCCEED, count);
    }

}
