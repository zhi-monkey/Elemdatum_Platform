package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.dubhe.biz.db.entity.BaseEntity;
import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.entity
 * @Project：mineai
 * @name：Team
 * @Date：2024/3/11 15:37
 * @Filename：Team
 * @Desc：标注团队实体类
 */

@Data
//@EqualsAndHashCode(callSuper = false)
//@Accessors(chain = true)
@TableName("data_team")
@ApiModel(value = "标注团队实体类", description = "多人标注")
@Builder
//@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DataTeam extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "团队名称")
    private String name;

    @ApiModelProperty(value = "团队描述")
    private String remark;

    @ApiModelProperty(value = "团队类型")
    private Integer type;

    @ApiModelProperty(value = "团队成员数")
    @TableField(value = "member_num")
    private Integer memberNum;

    @TableField(exist = false)
    @ApiModelProperty(value = "团队成员id")
    private List<Long> userIds;

    @TableField(exist = false) // 非数据库字段
    private Boolean canModify;

    @TableField(value = "deleted",fill = FieldFill.INSERT)
    private Boolean deleted = false;
}
