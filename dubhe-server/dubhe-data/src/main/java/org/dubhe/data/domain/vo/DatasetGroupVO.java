package org.dubhe.data.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * @author mingming
 * @date 2025/09/08
 */
@Data
@Builder
public class DatasetGroupVO {
    private Long id;
    private String name;
    private String description;
    private Boolean isPublic;
    private LocalDateTime creationTime;
    private LocalDateTime updateTime;
    private Integer datasetCount;
    private Integer versionCount;
    private Integer labelCount;
    private Long totalFiles;
    /**
     * 最新数据集名称
     * [{"数据集名称": "创建时间"}...]
     */
    private Map<String, Date> latestDatasets;
    /**
     * 是否是引导式：组内只要有至少一个引导式数据集就是引导式，否则是标准化
     */
    private Boolean isGuided;
}
