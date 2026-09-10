package org.dlut.adv.mineai.model.domain.dto;

import lombok.Data;
import org.dlut.adv.mineai.model.dto.JobPrecisionInfoDTO;

import java.util.List;

/**
 * @author mingming
 * @date 2025/08/18
 */
@Data
public class GuidedTrainingInfoCardDTO {

    /**
     * 引导式训练的ID
     */
    private Long generationId;

    /**
     * 引导式训练的名称
     */
    private String generationName;

    /**
     * 模型性能信息 (精度, 损失, 图片数)
     */
    private JobPrecisionInfoDTO modelInfo;

    /**
     * 模型包含的标签列表
     */
    private List<String> labelNames;
}
