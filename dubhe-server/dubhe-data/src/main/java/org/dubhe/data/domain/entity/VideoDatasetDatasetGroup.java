package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Video dataset to dataset-group relation. */
@Data
@NoArgsConstructor
@TableName("video_dataset_dataset_group")
public class VideoDatasetDatasetGroup implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("dataset_group_id")
    private Long datasetGroupId;
    @TableField("video_dataset_id")
    private Long videoDatasetId;

    public VideoDatasetDatasetGroup(Long datasetGroupId, Long videoDatasetId) {
        this.datasetGroupId = datasetGroupId;
        this.videoDatasetId = videoDatasetId;
    }
}
