package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;

/** A video annotation task, one per video file. */
@Data
@Accessors(chain = true)
@TableName(value = "video_annotation_task", autoResultMap = true)
public class VideoAnnotationTask extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("video_file_id")
    private Long videoFileId;
    @TableField("dataset_id")
    private Long datasetId;
    @TableField("annotator_id")
    private Long annotatorId;
    @TableField("status")
    private String status;
    @TableField("last_frame_index")
    private Integer lastFrameIndex;
    @TableField("annotation_version")
    private Integer annotationVersion;
}
