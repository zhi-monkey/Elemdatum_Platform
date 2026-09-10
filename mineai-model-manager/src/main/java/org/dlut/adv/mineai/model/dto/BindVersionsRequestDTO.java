package org.dlut.adv.mineai.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 用于引导式绑定版本
 * @author mingming
 * @date 2025/09/15
 */
@Data
public class BindVersionsRequestDTO {
    private List<Long> datasetVersionIds;
    private Long modelGenerationId;
}
