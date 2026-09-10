package org.dubhe.data.domain.vo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 视频数据集详情 VO（video 命名空间，数字 id 与图片数据集独立，避免跨类型冲突）
 */
@Data
public class VideoDatasetDetailVO {
    private Long id;
    private String name;
    private Long fileCount;
    private Integer status;
    private String uploadStatus;
    private String uploadError;
    private String remark;
    private Timestamp createTime;
    private Timestamp updateTime;
    /** 已抽帧视频数量（video_dataset_file.extract_status = EXTRACTED） */
    private Long extractedVideos;
    /** 未抽帧视频数量 */
    private Long unextractedVideos;
}
