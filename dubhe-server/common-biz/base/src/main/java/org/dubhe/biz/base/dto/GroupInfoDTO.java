package org.dubhe.biz.base.dto;

import lombok.Data;

/**
 * 数据集组织信息
 * @author mingming
 * @date 2025/09/05
 */
@Data
public class GroupInfoDTO {
    private Long id;
    private String name;
    private Boolean isPublic;
}
