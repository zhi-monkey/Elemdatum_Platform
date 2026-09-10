package org.dubhe.data.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 视频标注 Track 视图（含关键帧）。
 */
@Data
public class VideoAnnotationTrackVO {
    private Long id;
    private Integer trackNo;
    private Long labelId;
    private Integer startFrame;
    private Integer endFrame;
    private String status;
    private String source;
    private List<VideoTrackKeyframeVO> keyframes;
}
