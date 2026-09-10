package org.dlut.adv.mineai.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 自迭代训练父任务响应 VO
 */
@Data
public class SelfIterationTaskVO {

    private Long id;
    private String name;
    private String description;

    // ── 算法来源 ──
    private Long modelApplicationId;
    /** ModelApplication.applicationTaskName */
    private String modelApplicationName;
    private String applicationName;
    private String deviceFirmware;
    /** 自动标注镜像 URL */
    private String autoLabelImageUrl;

    /** 首轮自动标注来源：BOUND_DEFAULT / DEFAULT_IMAGE / TRAINED_MODEL */
    private String initialAutoLabelSource;

    /** 首轮训练模型来源：STANDARD / GUIDED */
    private String initialAutoLabelTrainSource;

    /** 首轮训练模型对应的 ModelGeneration ID */
    private Long initialAutoLabelModelGenerationId;

    /** 标准化训练模型下选中的训练任务名称 */
    private String initialAutoLabelStandardJobName;

    // ── 数据集 ──
    private List<Long> dataSourceIds;
    private List<DataSourceConfig> dataSources;
    private boolean deleteRawAfterCollect;
    private Long datasetGroupId;
    private String datasetGroupName;
    private List<Long> allDatasetVersionIds;

    // ── 迭代控制 ──
    private boolean requireManualReview;
    private boolean autoDeployEnabled;
    private Integer maxRounds;
    private Integer iterationStartImageQuantity;
    private BigDecimal targetAccuracy;
    private BigDecimal targetRecall;
    private Double confidenceThreshold;
    private int currentRound;
    private String reuseAnnotationModel;
    private String reuseTrainModel;

    // ── 硬件资源 ──
    private Long hardwareParamsId;
    /** HardwareParams.title */
    private String hardwareParamsTitle;
    private String gpuMode;
    private Integer gpuCount;

    // ── 训练配置 ──
    private String splitSize;
    private String modelType;
    private Map<String, String> hyperParams;
    private Map<String, String> convertParams;
    private String authCode;
    private List<Long> inferenceDeviceIds;
    private List<Long> gpuUrlTargetIds;

    // ── 标签 ──
    /** 绑定的标签（index→名称，与 ModelGeneration.selectedLabels 对齐，供前端展示与标注阶段使用） */
    private Map<Integer, String> selectedLabels;

    // ── 状态 ──
    private int status;
    /** 状态中文标签（前端展示用） */
    private String statusLabel;
    private Long userId;
    private Date createTime;
    private Date updateTime;

    /** 子任务列表（按轮次升序） */
    private List<SelfIterationJobVO> jobList;

    @Data
    public static class DataSourceConfig {
        private Long httpCameraId;
        private Integer captureInterval;
        private Integer imageQuantity;
    }
}
