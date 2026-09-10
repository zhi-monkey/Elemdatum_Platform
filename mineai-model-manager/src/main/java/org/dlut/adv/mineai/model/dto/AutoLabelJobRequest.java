package org.dlut.adv.mineai.model.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * @author JRC
 */
@Data
public class AutoLabelJobRequest {
    private String jobName;
    private Long modelGenerationId;
    private String standardJobName;
    private String datasetPath;
    private Boolean useDefault;
    private String imageUrl;
    private Map<String, String> params;
    private List<String> labelNames;
    private Boolean clearExistingLabels;
    private Boolean requireManualConfirmation;
    /**
     * 直接指定权重路径（自迭代训练第 N 轮使用上一轮产出的权重，跳过 modelGenerationId 查询）。
     * 非 null 时：useDefault=false 路径直接用此值作为 weightPath，imageUrl 字段需同时传入正确镜像。
     * null 时：走原有 modelGenerationId → ModelGeneration.trainModelVersion 路径。
     */
    private String directWeightPath;
}
