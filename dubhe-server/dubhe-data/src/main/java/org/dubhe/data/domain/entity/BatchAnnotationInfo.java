package org.dubhe.data.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量标注信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAnnotationInfo {
    private Long fileId;
    private String url;
    private String annotation;
    private boolean isModified;
}
