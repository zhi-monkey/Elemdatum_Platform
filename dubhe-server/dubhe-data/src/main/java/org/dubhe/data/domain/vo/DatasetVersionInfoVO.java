package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author mingming
 * @date 2025/09/23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatasetVersionInfoVO {
    private Long datasetId;
    private String datasetName;
    private List<Long> datasetVersionIds;
}
