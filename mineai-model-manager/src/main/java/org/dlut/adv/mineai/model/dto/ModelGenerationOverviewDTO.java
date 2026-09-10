package org.dlut.adv.mineai.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;

import java.util.List;

/**
 * 用于引导式数据处理处的总览信息
 * @author mingming
 * @date 2025/09/19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelGenerationOverviewDTO {
    /**
     * 算法包中标签, id即为顺序
     */
    private List<LabelDTO> labels;
    /**
     * 绑定的模型版本数量
     */
    private Integer boundVersionsCount;
}
