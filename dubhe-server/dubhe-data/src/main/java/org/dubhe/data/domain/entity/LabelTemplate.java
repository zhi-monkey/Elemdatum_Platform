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
 * 标签库实体
 * @author mingming
 * @date 2025/04/14
 */
@Builder
@Data
@TableName("label_template")
@ApiModel(value = "标签库实体", description = "数据集标签库实体")
@NoArgsConstructor
@AllArgsConstructor
public class LabelTemplate implements Serializable {
    private static final long serialVersionUID = 4L;
    /**
     * 标签库中标签id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 标签名
     */
    private String name;
    /**
     * 标签类型：0=图片/视频标签，1=点云标签
     */
    private Integer type;
    /**
     * 中文名称（展示用）
     */
    private String displayName;
    /**
     * 标注名称（标注时写入的英文标识）
     */
    private String annotationName;
    /**
     * 标注形状：默认 RECT（封闭矩形）
     */
    private String shape;
}
