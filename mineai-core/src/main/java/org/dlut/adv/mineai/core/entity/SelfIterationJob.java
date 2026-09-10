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

/**
 * 自迭代训练子任务（一轮迭代）
 * <p>
 * 每个子任务代表一轮完整的自迭代训练流程，依次经历四个阶段：
 * 数据采集（DATA_COLLECT）→ 数据标注（DATA_ANNOTATE）→ 模型训练（MODEL_TRAIN）→ 模型下发（MODEL_DEPLOY，预留）
 * </p>
 * <p>
 * 各阶段的底层执行分别引用：
 * - 数据采集：{@code RtspCaptureExecution}（通过 captureExecutionIds）
 * - 模型训练/转换：{@code ModelJob}（通过 trainJobId / convertJobId）
 * - 打包：{@code mineai-package-manager} Task（通过 packageTaskId）
 * </p>
 */
@Table
@Entity
@Getter
@Setter
@ToString(exclude = "selfIterationTask")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SelfIterationJob {

    // ===================== 阶段常量 =====================

    /** 数据采集阶段 */
    public static final int PHASE_DATA_COLLECT = 1;
    /** 数据标注阶段（自动标注 + 可选人工审核）*/
    public static final int PHASE_DATA_ANNOTATE = 2;
    /** 模型训练阶段（训练 + 转换）*/
    public static final int PHASE_MODEL_TRAIN = 3;
    /** 模型下发阶段（预留）*/
    public static final int PHASE_MODEL_DEPLOY = 4;

    // ===================== 状态常量 =====================

    /** 已创建，待启动 */
    public static final int STATUS_CREATED = 0;
    /** 数据采集中 */
    public static final int STATUS_COLLECTING = 1;
    /** 数据采集失败 */
    public static final int STATUS_COLLECT_FAILED = 11;
    /** 自动标注中 */
    public static final int STATUS_AUTO_LABELING = 2;
    /** 等待人工审核（父任务同步进入 PAUSED）*/
    public static final int STATUS_WAITING_REVIEW = 21;
    /** 标注失败 */
    public static final int STATUS_ANNOTATE_FAILED = 22;
    /** 训练中 */
    public static final int STATUS_TRAINING = 3;
    /** 转换中 */
    public static final int STATUS_CONVERTING = 31;
    /** 训练/转换失败 */
    public static final int STATUS_TRAIN_FAILED = 32;
    /** 打包中 */
    public static final int STATUS_PACKAGING = 4;
    /** 打包失败 */
    public static final int STATUS_PACKAGE_FAILED = 41;
    /** 待下发（预留，打包完成后进入）*/
    public static final int STATUS_PENDING_DEPLOY = 5;
    /** 本轮完成 */
    public static final int STATUS_COMPLETED = 6;
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
     * 轮次编号（从 1 开始）
     */
    @Column(nullable = false)
    private int round;

    /**
     * 所属父任务
     */
    @ManyToOne
    @JoinColumn(name = "self_iteration_task_id", nullable = false)
    @JsonIgnore
    private SelfIterationTask selfIterationTask;

    // ===================== 各阶段底层任务引用 =====================

    /**
     * 本轮数据采集触发的 RtspCaptureExecution ID 列表（对应多个数据回流任务各自的执行记录）
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_job_capture_executions",
            joinColumns = @JoinColumn(name = "self_iteration_job_id"))
    @Column(name = "capture_execution_id")
    private List<Long> captureExecutionIds;

    /**
     * 本轮训练的 ModelJob ID
     */
    private Long trainJobId;

    /**
     * 本轮转换的 ModelJob ID
     */
    private Long convertJobId;

    /**
     * 本轮打包的 Task ID（来自 mineai-package-manager）
     */
    private String packageTaskId;

    // ===================== 数据集 =====================

    /**
     * 本轮实际提交自动标注的 dataset 数量（可能少于 captureExecutionIds.size()，因部分 execution 无 datasetId）。
     * onAutoLabelComplete 用此字段判断是否全部标注完成。
     */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int annotateJobCount = 0;

    /**
     * 本轮各 Dataset 发布后的版本 ID 列表。
     * 每个回流任务执行产生一个 Dataset，自动标注完成后为每个 Dataset 发布版本快照，
     * ID 依次追加到此列表，训练时与父任务历史版本合并传入 DatasetMergeRequest.versions。
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_job_dataset_versions",
            joinColumns = @JoinColumn(name = "self_iteration_job_id"))
    @Column(name = "dataset_version_id")
    private List<Long> datasetVersionIds;

    /**
     * 本轮已完成自动标注的 Dataset ID 列表（用于进度判断）。
     * 自动标注完成回调时追加，审核完成后统一发布版本。
     */
    @ElementCollection
    @CollectionTable(name = "self_iteration_job_annotated_datasets",
            joinColumns = @JoinColumn(name = "self_iteration_job_id"))
    @Column(name = "dataset_id")
    private List<Long> annotatedDatasetIds;

    // ===================== 指标记录（用于轮次间对比） =====================

    /**
     * 本轮训练准确率（来自 ModelJobLogData.accuracy）
     */
    @Column(precision = 9, scale = 4)
    private BigDecimal trainAccuracy;

    /**
     * 本轮训练召回率（来自 ModelJobLogData.recall）
     */
    @Column(precision = 9, scale = 4)
    private BigDecimal trainRecall;

    /**
     * epoch 详情 JSON（来自 ModelJobLogData.epochDetail，用于轮次间指标曲线对比）
     * 格式示例：{"train loss":[...], "test accuracy":[...], "epoch":[...]}
     */
    @Column(columnDefinition = "LONGTEXT")
    private String epochDetail;

    // ===================== 阶段与状态 =====================

    /**
     * 当前所在阶段
     *
     * @see #PHASE_DATA_COLLECT
     * @see #PHASE_DATA_ANNOTATE
     * @see #PHASE_MODEL_TRAIN
     * @see #PHASE_MODEL_DEPLOY
     */
    @Column(nullable = false, columnDefinition = "int default 1")
    private int phase = PHASE_DATA_COLLECT;

    /**
     * 当前状态
     *
     * @see #STATUS_CREATED
     * @see #STATUS_COLLECTING
     * @see #STATUS_AUTO_LABELING
     * @see #STATUS_WAITING_REVIEW
     * @see #STATUS_TRAINING
     * @see #STATUS_CONVERTING
     * @see #STATUS_PACKAGING
     * @see #STATUS_PENDING_DEPLOY
     * @see #STATUS_COMPLETED
     * @see #STATUS_CANCELLED
     */
    @Column(nullable = false, columnDefinition = "int default 0")
    private int status = STATUS_CREATED;

    /**
     * 打包重试次数（用于打包失败自动重试）
     */
    @Column(columnDefinition = "int default 0")
    private int packageRetryCount = 0;

    // ===================== 时间戳 =====================

    @CreatedDate
    private Date createTime;

    @LastModifiedDate
    private Date updateTime;
}
