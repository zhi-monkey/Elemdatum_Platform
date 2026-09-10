package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.dlut.adv.mineai.core.constant.ModelJobStateCodeConstant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author oyjp
 * @create 2023/10/22 14:25
 */
@Table
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ModelGeneration {
    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 生产任务的名称
     * 约束： 唯一性
     */
    @Column(unique = true)
    private String name;

    /**
     * 训练模型
     */
    @OneToOne
    @JoinColumn(name = "train_model_version_id")
    private ModelVersion trainModelVersion;

    /**
     * 质检模型
     */
    @OneToOne
    @JoinColumn(name = "test_model_version_id")
    private ModelVersion testModelVersion;

    /**
     * 部署模型
     */
    @OneToOne
    @JoinColumn(name = "deploy_model_version_id")
    private ModelVersion deployModelVersion;

    @OneToMany
    private List<ModelJob> modelJobList;

    /**
     * 该模型生产任务从属于哪个model
     */
    @ManyToOne
    private ModelExplore modelExplore;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean isDelete = false;

    /**
     * 是否已发布
     */
    private Boolean isRelease;

    /**
     * 是否重用
     */
    private Boolean isReuse;

    /**
     * 是否是引导类任务
     */
    private Boolean isGuided;

    /**
     * 生产任务训练时使用的数据集版本id
     */
    private Long trainDataset;
    /**
     * 生产任务质检时使用的数据集版本id
     */
    private Long testDataset;

    /**
     * 开始训练时数据集id（无版本）
     */
    private Long trainDatasetStart;
    /**
     * 训练数据集版本ids（无版本）
     */
    @ElementCollection
    private List<Long> trainDatasetVersions;

    @ElementCollection
    private Map<Integer,String> selectedLabels;


    /**
     * 生产任务质检时使用的验证数据集版本id
     */
    private Long valDataset;

    @Column(columnDefinition = "int default 0 NOT NULL")
    private int datasetSource;
    public static final int NOT_FROM_TRAIN_DATASET = 0;
    public static final int FROM_TRAIN_DATASET = 1;

    /**
     * 数据集分割比例，设定训练集占比，如0.8
     */
    private double trainSize;

    /**
     * 新的数据集切分比例：训练集-测试集-验证集
     */
    private String splitSize;

    /**
     * 任务创建时间
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 按照用户权限展示生产任务信息
     */
    private Long userId;

    /**
     * 生产任务状态字段
     */
//    @Column(columnDefinition = "int default 0 NOT NULL")
//    private int status;
//    public static final int CREATE_SUCCESS = 0;
//    public static final int TRAINING = 1;
//    public static final int TESTING = 2;
//    public static final int STOP = 3;
//    public static final int TRAIN_FAIL = 4;
//    public static final int TEST_FAIL = 5;
//    public static final int TRAIN_SUCCESS = 6;
//    public static final int TEST_SUCCESS = 7;
//    public static final int COMPLETED = 8;
//    public static final int CANCELED = 9;
//    public static final int PUBLISHING = 10;
//    public static final int REJECTED = 11;
//    public static final int TRAIN_WAIT = 12;
//    public static final int TRAIN_TIME_OUT = 13;
//    public static final int PUBLISHED = 14;
//    public static final int CONVERT_SUCCESS = 15;
//    public static final int CONVERT_FAIL = 16;
//    public static final int CONVERTING = 17;

    private String description;

    @ManyToOne
    private HardwareParams hardwareParams;

    //延迟训练时间
    private int delayTrainTime;

    //最大训练时长
    private int maxTrainTime;

    /**
     * 保存的超参数
     */
    @ElementCollection
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, String> hyperParams;

    private long latestJobId;
    @OneToOne
    @JoinColumn(name = "model_id")
    private Model model;

    /**
     * 最新质检任务，用以获取当前质检模型的任务信息
     */
    @OneToOne
    private ModelJob testJob;

    /**
     * 两个质检任务同时进行，用来存储另一个质检任务
     */
    @OneToOne
    private ModelJob anotherTestJob;

    /**
     * 如果是引导式训练，需要这个来找到对应的appZipPath
     */
    @OneToOne
    private ModelApplication modelApplication;

    /**
     * 模型转换信息
     */
    @Embedded
    private ModelConversion modelConvert;

    /**
     * 量化参数
     */
    @ElementCollection
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, String> quantizationParams;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ModelConversion {

        private String jobName;
        private String image;
        private String datasetPath;
        private String weightPath;
        private String outputWeightPath;
        private String gpuNum;
        private String cpuNum;
        private String memoryNum;
        @ElementCollection
        @Column(columnDefinition = "LONGTEXT")
        private Map<String, String> params;
    }

    private String gpuMode;

    // 多卡模式下的GPU数量
    private Integer gpuCount;
    // GPU使用模式常量
    public static final String GPU_MODE_SINGLE_EXCLUSIVE = "single-exclusive";   // 单卡独占
    public static final String GPU_MODE_SINGLE_SHARED = "single-shared";         // 单卡共享
    public static final String GPU_MODE_MULTI_EXCLUSIVE = "multi-exclusive";     // 多卡独占
    public static final String GPU_MODE_MULTI_SHARED = "multi-shared";           // 多卡共享

    /**
     * 标注类型, 默认为目标检测Detection, 平台还支持语义分割Segmentation
     */
    private String annotationType = "Detection";
    
    // 标注类型常量
    public static final String ANNOTATION_TYPE_DETECTION = "Detection";          // 目标检测
    public static final String ANNOTATION_TYPE_SEGMENTATION = "Segmentation";    // 语义分割

    /**
     * 标注格式, 默认为YOLO, 平台还支持COCO
     */
    private String annotationFormat = "YOLO";
    
    // 标注格式常量
    public static final String ANNOTATION_FORMAT_YOLO = "YOLO";                 // YOLO格式
    public static final String ANNOTATION_FORMAT_COCO = "COCO";                 // COCO格式


//    @Column(columnDefinition = "int default 0 NOT NULL")
//    private Integer status_new;

    public static final int EXECUTING = 1;

    public static final int PAUSED = 2;

    public static final int NOT_ACTIVE = 3;

    //下面几个是给引导训练用的
    public static final int NOT_ACTIVE_GUIDED = 0;
    public static final int TRAINING_GUIDED = 1;
    public static final int TRAIN_SUCCESS_GUIDED = 2;
    public static final int TRAIN_FAILED_GUIDED = -2;
    public static final int CONVERTING_GUIDED = 3;
    public static final int TRAIN_CONVERT_SUCCESS_GUIDED = 4;
    public static final int CONVERT_FAILED_GUIDED = -4;
    public static final int CANCEllED_GUIDED = -10;
    public static final int SPLITTING_GUIDED = 12;


    public Integer getStatusNew() {
        if (modelJobList == null || modelJobList.isEmpty()) {
            return NOT_ACTIVE;
        }

        boolean going = false;
        boolean not_active = true;

        for (ModelJob job : modelJobList) {

            Integer jobStatus = job.getStatus();
            if (jobStatus != null) {
                if (job.isExecuting()) {
                    going = true;
                    not_active = false;
                    break;
                }
//                if (jobStatus.equals(ModelJob.TRAIN_FAILED) || jobStatus.equals(ModelJob.TRAIN_SUCCEEDED) ||
//                        jobStatus.equals(ModelJob.CONVERT_SUCCEEDED) || jobStatus.equals(ModelJob.CONVERT_FAILED) || jobStatus.equals(ModelJob.INSPECT_SUCCEEDED) || jobStatus.equals(ModelJob.INSPECT_FAILED) || jobStatus.equals(ModelJob.CREATE_SUCCESS)) {
//                    not_active = true;
//                    break;
//                }

            }
        }

        if (going) {
            return ModelGeneration.EXECUTING;
        } else if (not_active) {
            return ModelGeneration.NOT_ACTIVE;
        } else {
            return ModelGeneration.PAUSED;
        }


    }


    // 0-未运行，1训练中，-2训练失败，2训练完成，3转换中，4训练-转换完成，-4转换失败
    public Integer getStatusForGuided() {
        if (modelJobList == null || modelJobList.isEmpty()) {
            return NOT_ACTIVE_GUIDED;
        }
        ModelJob trainJob = null;
        ModelJob convertJob = null;
        for (ModelJob job : modelJobList) {
            Integer status = job.getStatus();
            if (Objects.equals(status, ModelJobStateCodeConstant.CANCELED)) {
                return CANCEllED_GUIDED;
            }
            if (Objects.equals(status, ModelJobStateCodeConstant.SPLITTING)) {
                return NOT_ACTIVE_GUIDED;
            }
            if (job.getJobType() == ModelJob.CONVERT)
                convertJob = job;
            else if (job.getJobType() == ModelJob.TRAIN) {
                trainJob = job;
            }
        }
        if (convertJob != null) {
            Integer jobStatus = convertJob.getStatus();
            if (Objects.equals(jobStatus, ModelJobStateCodeConstant.CONVERT_SUCCEEDED)) {
                return TRAIN_CONVERT_SUCCESS_GUIDED;
            } else if (Objects.equals(jobStatus, ModelJobStateCodeConstant.CONVERT_FAILED)) {
                return CONVERT_FAILED_GUIDED;
            } else if (Objects.equals(jobStatus, ModelJobStateCodeConstant.TRAINING)) {
                return CONVERTING_GUIDED;
            }
        } else if (trainJob != null) {
            Integer jobStatus = trainJob.getStatus();
            if (Objects.equals(jobStatus, ModelJobStateCodeConstant.TRAIN_SUCCEEDED)) {
                return TRAIN_SUCCESS_GUIDED;
            } else if (Objects.equals(jobStatus, ModelJobStateCodeConstant.TRAIN_FAILED)) {
                return TRAIN_FAILED_GUIDED;
            } else if (Objects.equals(jobStatus, ModelJobStateCodeConstant.TRAINING)) {
                return TRAINING_GUIDED;
            }
        }
        return -1;
    }
}
