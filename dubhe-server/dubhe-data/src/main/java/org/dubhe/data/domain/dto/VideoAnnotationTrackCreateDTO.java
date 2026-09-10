package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 创建视频标注 Track（首次画框 = 建 Track + 首个 Keyframe）。
 */
@Data
public class VideoAnnotationTrackCreateDTO {
    @NotNull
    private Long taskId;
    @NotNull
    private Integer trackNo;
    @NotNull
    private Long labelId;
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
}
