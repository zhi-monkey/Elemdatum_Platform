package org.dlut.adv.mineai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author mingming
 * @date 2025/09/23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ModelGenerationBindInfo {
    private Long modelGenerationId;
    private String modelGenerationName;

    private Long datasetId;
    private String datasetName;

    private Boolean isGuided;
}
