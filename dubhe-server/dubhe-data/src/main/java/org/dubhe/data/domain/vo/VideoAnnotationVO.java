package org.dubhe.data.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 视频标注加载视图（视频元信息 + tracks + keyframes）。
 */
@Data
public class VideoAnnotationVO {
    private Long taskId;
    private Long fileId;
    private Long datasetId;
    private String taskStatus;
    private String fileName;
    private Integer width;
    private Integer height;
    private Double fps;
    private Integer frameCount;
    private Double duration;
    private String url;
    private String convertedUrl;
    private Integer lastFrameIndex;
    private List<VideoAnnotationTrackVO> tracks;
}
