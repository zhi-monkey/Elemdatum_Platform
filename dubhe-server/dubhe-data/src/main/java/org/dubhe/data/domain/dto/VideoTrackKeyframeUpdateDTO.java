package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 修改视频 Track 关键帧。
 */
@Data
public class VideoTrackKeyframeUpdateDTO {
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
