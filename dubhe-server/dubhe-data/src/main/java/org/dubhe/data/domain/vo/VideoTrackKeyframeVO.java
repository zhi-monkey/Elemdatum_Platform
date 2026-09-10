package org.dubhe.data.domain.vo;

import lombok.Data;

/**
 * 视频 Track 关键帧视图。
 */
@Data
public class VideoTrackKeyframeVO {
    private Long id;
    private Integer frameIndex;
    private Double x;
    private Double y;
    private Double width;
    private Double height;
    private Boolean outside;
    private Boolean occluded;
}
