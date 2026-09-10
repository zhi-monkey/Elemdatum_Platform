package org.dubhe.data.domain.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 点云数据集详情 VO
 * <p>
 * 用于点云数据集「详情」页面的数据展示，包含两部分：
 * 1. 数据集基础信息（名称、状态、文件数、描述等）
 * 2. 文件统计信息（总点数、总文件大小），供前端信息总览卡片展示
 * </p>
 */
@Data
public class PcDatasetDetailVO {
    /** 数据集ID */
    private Long id;
    /** 数据集名称 */
    private String name;
    /** 标签组ID */
    private Long labelGroupId;
    /** 文件数量 */
    private Long fileCount;
    /** 数据集状态 */
    private Integer status;
    /** 数据集描述 */
    private String remark;
    /** 上传状态 */
    private String uploadStatus;
    /** 上传失败原因 */
    private String uploadError;
    /** 创建时间 */
    private Timestamp createTime;
    /** 更新时间 */
    private Timestamp updateTime;

    /** 文件统计：总点数（所有 PCD 文件点数之和） */
    private Long totalPointCount;
    /** 文件统计：总大小（所有 PCD 文件字节数之和） */
    private Long totalFileSize;
}
