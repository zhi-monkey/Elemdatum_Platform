package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.dubhe.biz.db.entity.BaseEntity;
import org.dubhe.data.domain.dto.LabelMappingDTO;

import java.io.Serializable;
import java.security.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description 标签映射关系
 * @date 2020-04-17
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Builder
@TableName("label_mapping")
@ApiModel(value = "LabelMapping对象", description = "标签映射关系")
public class LabelMapping extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("数据集版本ID")
    private Long datasetVersionId;

    @ApiModelProperty("源标签ID")
    private Long sourceLabelId;

    @ApiModelProperty("目标标签ID")
    private Long targetLabelId;

    @ApiModelProperty("删除标识")
    @TableField(value = "deleted", fill = FieldFill.INSERT)
    private Boolean deleted = false;


    /**
     * 将 List<LabelMapping> 转换为 Map<Long, Long>，key 是 sourceLabelId，value 是 targetLabelId
     *
     * @param labelMappingList List<LabelMapping> 实体类列表
     * @return Map<Long, Long> 返回源标签ID和目标标签ID的映射
     */
    public static Map<Long, Long> toMap(List<LabelMapping> labelMappingList) {
        return labelMappingList.stream()
                .filter(labelMapping -> labelMapping.getTargetLabelId() != null)  // 过滤掉 targetLabelId 为 null 的条目
                .collect(Collectors.toMap(LabelMapping::getSourceLabelId, LabelMapping::getTargetLabelId));
    }


    // /**
    //  * 将 Map<Long, Long> 转换为 List<LabelMapping>
    //  *
    //  * @param labelMappingMap Map<Long, Long> 源标签ID到目标标签ID的映射
    //  * @return List<LabelMapping> 返回转换后的实体类列表
    //  */
    // public static List<LabelMapping> fromMap(Map<Long, Long> labelMappingMap, Long datasetVersionId) {
    //     return labelMappingMap.entrySet().stream()
    //             .map(entry -> new LabelMapping(datasetVersionId, entry.getKey(), entry.getValue(), false))
    //             .collect(Collectors.toList());
    // }

    /**
     * 将 LabelMappingDTO 列表转换为 LabelMapping 实体列表
     * @param labelMappingDTOs LabelMappingDTO 列表
     * @param datasetVersionId 数据集版本ID
     * @return LabelMapping 实体列表
     */
    public static List<LabelMapping> fromDtoList(List<LabelMappingDTO> labelMappingDTOs, Long datasetVersionId) {
        if (labelMappingDTOs == null) {
            return Collections.emptyList();
        }


        return labelMappingDTOs.stream()
                .map(dto -> new LabelMapping(datasetVersionId, dto.getSourceLabelId(), dto.getTargetLabelId(), false))
                .collect(Collectors.toList());
    }
}
//
//CREATE TABLE `label_mapping` (
//        `id` BIGINT NOT NULL AUTO_INCREMENT,
//                                 `dataset_version_id` BIGINT NOT NULL,           -- 与 DatasetVersion 表关联
//                                 `source_label_id` BIGINT NOT NULL,              -- 源标签ID
//                                 `target_label_id` BIGINT DEFAULT NULL,          -- 目标标签ID，允许为NULL
//                                 `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
//                                 `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, -- 更新时间
//                                 `deleted` BOOLEAN DEFAULT FALSE,                -- 删除标记
//PRIMARY KEY (`id`),
//CONSTRAINT `FK_dataset_version_id` FOREIGN KEY (`dataset_version_id`) REFERENCES `data_dataset_version`(`id`) ON DELETE CASCADE
//) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
