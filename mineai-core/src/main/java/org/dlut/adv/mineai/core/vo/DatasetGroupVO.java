package org.dlut.adv.mineai.core.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据集组VO
 * @author mingming
 * @date 2025/09/15
 */
@Data
public class DatasetGroupVO {
    private Long id;
    private String name;
    private String description;
    private Long createUserId;
    private Boolean isPublic;
    private LocalDateTime creationTime;
    private LocalDateTime updateTime;
}
