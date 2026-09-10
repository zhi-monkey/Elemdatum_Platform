package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 自迭代训练父任务
 * <p>
 * 管理自迭代训练的整体配置和生命周期。
 * 一个父任务包含多轮子任务（SelfIterationJob），每轮依次执行：
 * 数据采集 → 数据标注 → 模型训练 → 模型下发（预留）
 * </p>
 */
@Table
@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SelfIterationTask {

    // ===================== 状态常量 =====================

    /** 空闲/初始化，未开始 */
    public static final int STATUS_IDLE = 0;
    /** 运行中（某轮子任务进行中）*/
    public static final int STATUS_RUNNING = 1;
    /** 暂停（等待人工审核）*/
    public static final int STATUS_PAUSED = -1;
    /** 已完成（达到最大轮次或手动停止）*/
    public static final int STATUS_COMPLETED = 2;
    /** 失败（子任务失败后父任务停止）*/
    public static final int STATUS_FAILED = -2;
    /** 已取消 */
    public static final int STATUS_CANCELLED = -10;

    // ===================== 基础信息 =====================

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 任务名称（唯一）
     */
    @Column(unique = true)
    private String name;

    /**
     * 任务描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    // ===================== 算法来源 =====================

    /**
     * 关联的应用（通过 ModelApplication → Model 确定基础算法库的一条记录）。
     * 同引导式训练逻辑：ApplicationName + Device 唯一确定一个 ModelApplication。
     */
    @ManyToOne
    @JoinColumn(name = "model_application_id")
    private ModelApplication modelApplication;

    /**
     * 自动标注镜像 URL。
     * 创建任务时从 modelApplication.model 中取 isAutoLabel=true 的 ModelVersion.url 存入，避免每轮重复查询。
     * - 第 1 轮：useDefault=true，镜像自带默认模型权重
     * - 第 N 轮（N>1）：useDefault=false，配合上一轮 trainJobId 的 weightPath 使用同一镜像
     */
    private String autoLabelImageUrl;

    /**
     * 首轮自动标注模型来源：BOUND_DEFAULT / DEFAULT_IMAGE / TRAINED_MODEL。
     * 为空时按旧数据兼容逻辑处理。
     */
    @Column(length = 32)
    private String initialAutoLabelSource;

    /**
     * 首轮训练模型来源：STANDARD / GUIDED。
     */
    @Column(length = 32)
    private String initialAutoLabelTrainSource;

    /**
     * 首轮训练模型对应的 ModelGeneration ID。
     */
    private Long initialAutoLabelModelGenerationId;

    /**
     * 标准化训练模型下选中的训练任务名称。
     */
    private String initialAutoLabelStandardJobName;

    // ===================== 数据集 =====================

    /**
     * 所有已发布的数据集版本 ID 累积列表（跨轮次）。
     * <p>
     * 每轮迭代完成自动标注后，将本轮各 Dataset 的发布版本 ID 追加至此列表。
     * 训练时将此列表全量传入 DatasetMergeRequest.versions，实现数据随轮次持续累积。
     * 各 RtspCaptureTask 仍写入自己绑定的 DatasetGroup，此列表只记录版本快照引用，
     * 无需修改回流任务的现有逻辑。
     * </p>
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_task_dataset_versions",
            joinColumns = @JoinColumn(name = "self_iteration_task_id"))
    @Column(name = "dataset_version_id")
    private List<Long> allDatasetVersionIds;

    // ===================== 数据回流配置 =====================

    /**
     * 关联的数据回流任务 ID 列表（引用现有 RtspCaptureTask）。
     * 每轮迭代触发这些回流任务拉取图片。
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_task_data_source",
            joinColumns = @JoinColumn(name = "self_iteration_task_id"))
    @Column(name = "rtsp_capture_task_id")
    private List<Long> dataSourceIds;

    /**
     * 采集后是否删除原始图片。
     */
    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean deleteRawAfterCollect = true;

    // ===================== 迭代控制配置 =====================

    /**
     * 是否需要人工审核（自动标注完成后）。
     * true → 子任务进入 WAITING_REVIEW，父任务暂停等待；
     * false → 自动标注完成后直接进入训练阶段。
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean requireManualReview = false;

    /**
     * 是否自动下发（预留字段，暂不实现下发逻辑）
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean autoDeployEnabled = false;

    /**
     * 最大迭代轮次（null 表示不限）
     */
    private Integer maxRounds;

    /**
     * 迭代开始图片数量：所有采集执行累计达到该数量后进入训练链路。
     */
    private Integer iterationStartImageQuantity;

    /** Target accuracy in 0-1 decimal form. Enables metric stop with targetRecall. */
    @Column(precision = 9, scale = 4)
    private BigDecimal targetAccuracy;

    /** Target recall in 0-1 decimal form. Enables metric stop with targetAccuracy. */
    @Column(precision = 9, scale = 4)
    private BigDecimal targetRecall;

    /** 自动标注置信度阈值 */
    @Column(columnDefinition = "double default 0.5")
    private Double confidenceThreshold = 0.5;

    /**
     * 当前已完成的轮次数
     */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int currentRound = 0;

    /**
     * 标注阶段模型复用策略：ALWAYS（始终复用）、NEVER（始终用默认）、METRIC_COMPARE（基于指标对比）
     */
    @Column(length = 20, columnDefinition = "varchar(20) default 'ALWAYS'")
    private String reuseAnnotationModel = "ALWAYS";

    /**
     * 训练阶段模型复用策略：ALWAYS（始终finetune）、NEVER（始终从头）、METRIC_COMPARE（基于指标对比）
     */
    @Column(length = 20, columnDefinition = "varchar(20) default 'ALWAYS'")
    private String reuseTrainModel = "ALWAYS";

    // ===================== 硬件配置 =====================

    /**
     * 硬件资源配置（同 ModelGeneration）
     */
    @ManyToOne
    @JoinColumn(name = "hardware_params_id")
    private HardwareParams hardwareParams;

    /**
     * GPU 使用模式（single-exclusive / single-shared / multi-exclusive / multi-shared）
     */
    private String gpuMode;

    /**
     * 多卡模式下的 GPU 数量
     */
    private Integer gpuCount;

    // ===================== 训练超参 =====================

    /**
     * 数据集切分比例（同 ModelGeneration.splitSize，如 "7-2-1" 表示训练/测试/验证 7:2:1）。
     * 为空或 null 时不做切分，直接使用全量数据训练。
     * 用于 DatasetMergeRequest.isSplit + splitRatio。
     */
    private String splitSize;

    /**
     * 训练模型类型（同 ModelJobService.createJob 的 modelType 参数）。
     * 取值：{@code ModelTypeConstant.PRETRAINED} / {@code OFFICIAL} / {@code SCRATCH}。
     * 注入 K8s 训练 Job 环境变量 MODEL_TYPE，决定预训练权重加载策略。
     */
    private String modelType;

    /**
     * 训练超参配置（HP_BATCH_SIZE、HP_EPOCHS、HP_LEARNING_RATE 等，每轮训练时读取）
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_task_hyper_params",
            joinColumns = @JoinColumn(name = "self_iteration_task_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value", columnDefinition = "TEXT")
    private Map<String, String> hyperParams;

    /**
     * 转换超参配置（CONVERT_TARGET_PLATFORM 等，每轮转换时读取）
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_task_convert_params",
            joinColumns = @JoinColumn(name = "self_iteration_task_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value", columnDefinition = "TEXT")
    private Map<String, String> convertParams;

    /**
     * 打包授权码（可选）。在自动打包阶段透传给 package-manager。
     */
    @Column(columnDefinition = "TEXT")
    private String authCode;

    /**
     * 绑定的标签（来自 ModelApplication，创建任务时写入，供前端展示和每轮标注使用）。
     * <p>
     * 与 ModelGeneration.selectedLabels 对齐：key = 0-based 顺序索引，value = 标签名。
     * 构建方式：getBoundLabelIds → findLabelByIds → index 建 Map。
     * </p>
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_task_selected_labels",
            joinColumns = @JoinColumn(name = "self_iteration_task_id"))
    @MapKeyColumn(name = "label_index")
    @Column(name = "label_name")
    private Map<Integer, String> selectedLabels;

    /**
     * 绑定的算力推理设备 ID 列表（对应 dubhe-data inference_device.id）。
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_task_inference_device_ids",
            joinColumns = @JoinColumn(name = "self_iteration_task_id"))
    @Column(name = "inference_device_id")
    private List<Long> inferenceDeviceIds;

    /**
     * 绑定的 GPU 模型下发地址 ID 列表（对应 gpu_url_target.id）。
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_task_gpu_url_target",
            joinColumns = @JoinColumn(name = "self_iteration_task_id"))
    @Column(name = "gpu_url_target_id")
    private List<Long> gpuUrlTargetIds;

    // ===================== 状态 =====================

    /**
     * 父任务整体状态
     *
     * @see #STATUS_IDLE
     * @see #STATUS_RUNNING
     * @see #STATUS_PAUSED
     * @see #STATUS_COMPLETED
     * @see #STATUS_FAILED
     * @see #STATUS_CANCELLED
     */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int status = STATUS_IDLE;

    /**
     * 创建用户 ID
     */
    private Long userId;

    // ===================== 子任务列表 =====================

    /**
     * 该父任务下的所有子任务（按轮次排列）
     */
    @OneToMany(mappedBy = "selfIterationTask", cascade = CascadeType.ALL)
    @OrderBy("round ASC")
    @JsonIgnore
    private List<SelfIterationJob> jobList;

    // ===================== 时间戳 =====================

    @CreatedDate
    private Date createTime;

    @LastModifiedDate
    private Date updateTime;
}
