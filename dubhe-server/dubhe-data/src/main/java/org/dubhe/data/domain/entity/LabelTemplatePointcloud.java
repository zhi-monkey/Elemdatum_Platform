package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 点云标签库实体（独立表，点云标签专用）
 * @author mingming
 * @date 2025/04/14
 */
@Builder
@Data
@TableName("label_template_pointcloud")
@ApiModel(value = "点云标签库实体", description = "点云数据集标签库实体")
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplatePointcloud implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 标签库中标签id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 标注名称（英文，标注时写入）
     */
    private String name;
    /**
     * 中文名称（展示用）
     */
    private String displayName;
    /**
     * 标注名称（英文，与 name 一致）
     */
    private String annotationName;
    /**
     * 标注形状：默认 RECT（封闭矩形）
     */
    private String shape;
    /**
     * 类别颜色（#RRGGBB，可空；空则由前端按标签名哈希生成）
     */
    private String color;
}
