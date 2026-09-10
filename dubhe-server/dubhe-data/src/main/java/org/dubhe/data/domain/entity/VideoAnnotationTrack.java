package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/** One tracked object in a video annotation task. */
@Data
@Accessors(chain = true)
@TableName(value = "video_annotation_track", autoResultMap = true)
public class VideoAnnotationTrack extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("task_id")
    private Long taskId;
    @TableField("track_no")
    private Integer trackNo;
    @TableField("label_id")
    private Long labelId;
    @TableField("start_frame")
    private Integer startFrame;
    @TableField("end_frame")
    private Integer endFrame;
    @TableField("status")
    private String status;
    @TableField("source")
    private String source;
}
