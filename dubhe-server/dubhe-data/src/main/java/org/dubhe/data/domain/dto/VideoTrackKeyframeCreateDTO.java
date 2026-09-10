package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 新增视频 Track 关键帧。
 */
@Data
public class VideoTrackKeyframeCreateDTO {
    /** 从 URL path 传入，@Valid 校验时不参与（Controller 内 setTrackId） */
    private Long trackId;
    @NotNull
    private Integer frameIndex;
    @NotNull
    private Double x;
    @NotNull
    private Double y;
    @NotNull
    private Double width;
    @NotNull
    private Double height;
    private Boolean outside;
    private Boolean occluded;
}
