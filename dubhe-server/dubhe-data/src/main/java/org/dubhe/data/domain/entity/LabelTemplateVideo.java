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
 * 视频标签库实体（独立表 label_template_video，视频 BBox 标注专用）
 */
@Builder
@Data
@TableName("label_template_video")
@ApiModel(value = "视频标签库实体", description = "视频数据集标签库实体")
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplateVideo implements Serializable {
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
     * 类别颜色（#RRGGBB，可空；空则由前端按标签名哈希生成）
     */
    private String color;
}
