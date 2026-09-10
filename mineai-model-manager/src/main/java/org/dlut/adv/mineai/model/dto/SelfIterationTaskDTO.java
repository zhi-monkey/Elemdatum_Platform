package org.dlut.adv.mineai.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 创建/更新自迭代训练父任务的请求 DTO
 * <p>参考引导式训练流程，只保留用户必填字段，其他配置从 ModelApplication 自动获取</p>
 */
@Data
public class SelfIterationTaskDTO {

    /** 更新时传入（不传则视为创建） */
    private Long id;

    // ── 基础信息 ──

    /** 任务名称（创建时必填，全局唯一） */
    private String taskName;

    /** 任务描述 */
    private String description;

    // ── 算法来源 ──

    /** 应用名称（必填，用于查找 ModelApplication） */
    private String applicationName;

    /** 设备固件（必填，格式：设备名-固件版本） */
    private String deviceFirmware;

    // ── 数据回流配置 ──

    /** 数据集组 ID（可选，为空则使用 datasetGroupName 创建新数据集组） */
    private Long datasetGroupId;

    /** 数据集组名称（datasetGroupId 为空时使用） */
    private String datasetGroupName;

    /** 数据源配置列表（HTTP摄像机） */
    private List<DataSourceConfig> dataSources;

    /** 采集后是否删除原始图片，默认删除 */
    private boolean deleteRawAfterCollect = true;

    // ── 迭代控制 ──

    /** 是否需要人工审核（默认 false） */
    private boolean requireManualReview = false;

    /** 是否自动下发（预留，默认 false） */
    private boolean autoDeployEnabled = false;

    /** 最大迭代轮次（null 表示不限） */
    private Integer maxRounds;

    /** 迭代开始图片数量：所有摄像头累计采集达到该数量后进入训练链路 */
    private Integer iterationStartImageQuantity;

    /** Target accuracy in 0-1 decimal form. */
    private BigDecimal targetAccuracy;

    /** Target recall in 0-1 decimal form. */
    private BigDecimal targetRecall;

    /** 置信度阈值（自动标注用，默认 0.5） */
    private Double confidenceThreshold = 0.5;

    /** 标注阶段模型复用策略：ALWAYS / NEVER / METRIC_COMPARE */
    private String reuseAnnotationModel;

    /** 训练阶段模型复用策略：ALWAYS / NEVER / METRIC_COMPARE */
    private String reuseTrainModel;

    // ── 硬件资源 ──

    /** 硬件配置 ID（必填） */
    private Long hardwareParamsId;

    /** GPU 使用模式：single-exclusive / single-shared / multi-exclusive / multi-shared（必填） */
    private String gpuMode;

    /** 多卡模式下的 GPU 数量 */
    private Integer gpuCount;

    // ── 训练配置 ──

    /** 数据集切分比例（如 "80-10-10"；为空则不切分） */
    private String splitSize;

    /** 训练超参（HP_BATCH_SIZE、HP_EPOCHS、HP_LEARNING_RATE 等） */
    private Map<String, String> trainParams;

    /** 转换超参（CONVERT_TARGET_PLATFORM 等） */
    private Map<String, String> convertParams;

    /** 打包授权码（可选） */
    private String authCode;

    /** 历史字段名，实际业务语义为绑定的模型接收地址 ID 列表（可选） */
    private List<Long> inferenceDeviceIds;

    /** 绑定的 GPU 模型下发地址 ID 列表（可选） */
    private List<Long> gpuUrlTargetIds;

    /** 自动标注镜像 URL（可选）。不填时自动从 ModelApplication.model.autoLabelModelVersion.url 取；填写时以用户指定值覆盖（支持前端手动选择镜像）。 */
    private String autoLabelImageUrl;

    /** 首轮自动标注来源：BOUND_DEFAULT / DEFAULT_IMAGE / TRAINED_MODEL */
    private String initialAutoLabelSource;

    /** 首轮训练模型来源：STANDARD / GUIDED */
    private String initialAutoLabelTrainSource;

    /** 首轮训练模型对应的 ModelGeneration ID */
    private Long initialAutoLabelModelGenerationId;

    /** 标准化训练模型下选中的训练任务名称 */
    private String initialAutoLabelStandardJobName;

    /**
     * 数据源配置（HTTP摄像机）
     */
    @Data
    public static class DataSourceConfig {
        /** HTTP摄像机 ID */
        private Long httpCameraId;

        /** 采样间隔（秒） */
        private Integer captureInterval;

        /** 采集数量 */
        private Integer imageQuantity;
    }
}

