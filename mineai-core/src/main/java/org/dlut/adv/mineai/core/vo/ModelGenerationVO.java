package org.dlut.adv.mineai.core.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.dlut.adv.mineai.core.entity.*;

import java.util.Date;
import java.util.Map;

@Data
public class ModelGenerationVO {

    /**
     * 内部索引
     */
    private long id;

    /**
     * 模型生成任务的名称
     */
    private String name;

    /**
     * 训练模型
     */
    private ModelVersion trainModelVersion;

    /**
     * 质检模型
     */
    private ModelVersion testModelVersion;

    /**
     * 部署模型
     */
    private ModelVersion deployModelVersion;

    /**
     * 最新训练 ModelJob，用以获取当前训练模型的任务信息
     */
    private ModelJob trainJob;

    /**
     * 最新质检任务，用以获取当前质检模型的任务信息
     */
    private ModelJob testJob;

    /**
     * 两个质检任务同时进行，用来存储另一个质检任务
     */
    private ModelJob anotherTestJob;

    /**
     * 该模型生产任务从属于哪个 modelExplore
     */
    private ModelExplore model;

    /**
     * 是否已发布
     */
    private Boolean isRelease;

    /**
     * 表示是否重用
     */
    private Boolean isReuse;

    /**
     * 是否是引导类任务
     */
    private Boolean isGuided;

    /**
     * 任务创建的数据集id（无版本）
     */
    private Long trainDatasetStart;

    /**
     * 任务创建的数据集信息（无版本）
     */
    private Object trainDatasetStartAll;

    /**
     * 生产任务训练时使用的数据集版本id
     */
    private DatasetVersionVO trainDataset;
    /**
     * 生产任务质检时使用的数据集版本id
     */
    private DatasetVersionVO testDataset;
    /**
     * 生产任务验证时使用的数据集版本id
     */
    private DatasetVersionVO valDataset;
    /**
     * 是否已经部署
     */
    private Boolean hasDeployments;

    private int datasetSource;
    public static final int NOT_FROM_TRAIN_DATASET = 0;
    public static final int FROM_TRAIN_DATASET = 1;

    private String splitSize;


    /**
     * 任务创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 任务状态
     */
    private int status;
    public static final int CREATE_SUCCESS = 0;
    public static final int TRAINING = 1;
    public static final int TESTING = 2;
    public static final int STOP = 3;
    public static final int TRAIN_FAIL = 4;
    public static final int TEST_FAIL = 5;
    public static final int TRAIN_SUCCESS = 6;
    public static final int TEST_SUCCESS = 7;
    public static final int COMPLETED = 8;
    public static final int CANCELED = 9;
    public static final int PUBLISHING = 10;
    public static final int REJECTED = 11;

    private String description;

    private HardwareParams hardwareParams;

    /**
     * 延迟训练时间
     */
    private int delayTrainTime;

    /**
     * 最大训练时长
     */
    private int maxTrainTime;

    /**
     * ModelGeneration 的创建用户名称
     */
    private String userName;
    private long latestJobId;

    private Model modelStore;

    /**
     * 0-未运行，1训练中，-2训练失败，2训练完成，3转换中，4训练-转换完成，-4转换失败
     */
    private int statusForGuided;

    /**
     * 是否可发布
     */
    private Boolean canPublish;
    private Map<String, String> hyperParams;

    /**
     * 拿appZipPath
     */
    private ModelApplication modelApplication;

    // 添加GPU模式和GPU数量字段
    private String gpuMode;

    // 多卡模式下的GPU数量
    private Integer gpuCount;

    private Map<Integer, String> selectedLabels;

    /**
     * 标注类型, 默认为目标检测Detection, 平台还支持语义分割Segmentation
     */
    private String annotationType;

    /**
     * 标注格式, 默认为YOLO
     */
    private String annotationFormat;

    public ModelGenerationVO(ModelGeneration modelGeneration) {
        this.id = modelGeneration.getId();
        this.name = modelGeneration.getName();
        this.trainModelVersion = modelGeneration.getTrainModelVersion();
        this.testModelVersion = modelGeneration.getTestModelVersion();
        this.deployModelVersion = modelGeneration.getDeployModelVersion();
        this.model = modelGeneration.getModelExplore();
        this.isGuided = modelGeneration.getIsGuided();
        this.isRelease = modelGeneration.getIsRelease();
        this.isReuse = modelGeneration.getIsReuse();
        this.datasetSource = modelGeneration.getDatasetSource();
        this.splitSize = modelGeneration.getSplitSize();
        this.createTime = modelGeneration.getCreateTime();
        this.status = modelGeneration.getStatusNew();
        this.statusForGuided = Boolean.TRUE.equals(modelGeneration.getIsGuided())
                ? modelGeneration.getStatusForGuided()
                : ModelGeneration.NOT_ACTIVE_GUIDED;
        this.description = modelGeneration.getDescription();
        this.testJob = modelGeneration.getTestJob();
        this.anotherTestJob = modelGeneration.getAnotherTestJob();
        this.hardwareParams = modelGeneration.getHardwareParams();
        this.delayTrainTime = modelGeneration.getDelayTrainTime();
        this.maxTrainTime = modelGeneration.getMaxTrainTime();
        this.hyperParams = modelGeneration.getHyperParams();
        this.latestJobId = modelGeneration.getLatestJobId();
        this.modelStore = modelGeneration.getModel();
        this.trainDatasetStart = modelGeneration.getTrainDatasetStart();
        // 添加GPU模式和GPU数量的设置
        this.gpuMode = modelGeneration.getGpuMode();
        this.gpuCount = modelGeneration.getGpuCount();
        this.selectedLabels = modelGeneration.getSelectedLabels();
        // 添加标注类型和格式
        this.annotationType = modelGeneration.getAnnotationType();
        this.annotationFormat = modelGeneration.getAnnotationFormat();
    }
}
