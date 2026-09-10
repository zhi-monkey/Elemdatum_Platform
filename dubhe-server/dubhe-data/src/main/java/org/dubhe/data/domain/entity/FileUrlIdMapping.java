package org.dubhe.data.domain.entity;

import lombok.Data;

/**
 * 文件URL和ID的映射
 */
@Data
public class FileUrlIdMapping {
    private String url;
    private Long id;
}