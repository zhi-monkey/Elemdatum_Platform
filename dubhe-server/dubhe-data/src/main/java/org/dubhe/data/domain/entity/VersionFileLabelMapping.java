package org.dubhe.data.domain.entity;

import lombok.Data;

/**
 * 版本文件和标签的映射
 */
@Data
public class VersionFileLabelMapping {
    private Long versionFileId;
    private Long labelId;
}
