package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/** A keyframe bounding box on a track; intermediate frames are interpolated. */
@Data
@Accessors(chain = true)
@TableName(value = "video_track_keyframe", autoResultMap = true)
public class VideoTrackKeyframe extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("track_id")
    private Long trackId;
    @TableField("frame_index")
    private Integer frameIndex;
    @TableField("x")
    private Double x;
    @TableField("y")
    private Double y;
    @TableField("width")
    private Double width;
    @TableField("height")
    private Double height;
    @TableField("outside")
    private Boolean outside;
    @TableField("occluded")
    private Boolean occluded;
    @TableField("source")
    private String source;
}
