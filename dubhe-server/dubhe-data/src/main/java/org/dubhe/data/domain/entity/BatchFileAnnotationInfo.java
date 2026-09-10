package org.dubhe.data.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量文件标注信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchFileAnnotationInfo {
    private Long datasetId;
    private Long versionFileId;
    private List<Long> labelIds;
    private String fileName;
}
