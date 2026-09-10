package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Point-cloud dataset to dataset-group relation. */
@Data
@NoArgsConstructor
@TableName("pc_dataset_dataset_group")
public class PcDatasetDatasetGroup implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("dataset_group_id")
    private Long datasetGroupId;
    @TableField("pc_dataset_id")
    private Long pcDatasetId;

    public PcDatasetDatasetGroup(Long datasetGroupId, Long pcDatasetId) {
        this.datasetGroupId = datasetGroupId;
        this.pcDatasetId = pcDatasetId;
    }
}
