package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Multi-modal dataset to dataset-group relation. */
@Data
@NoArgsConstructor
@TableName("multi_dataset_dataset_group")
public class MultiDatasetDatasetGroup {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("dataset_group_id")
    private Long datasetGroupId;
    @TableField("multi_dataset_id")
    private Long multiDatasetId;

    public MultiDatasetDatasetGroup(Long datasetGroupId, Long multiDatasetId) {
        this.datasetGroupId = datasetGroupId;
        this.multiDatasetId = multiDatasetId;
    }
}
