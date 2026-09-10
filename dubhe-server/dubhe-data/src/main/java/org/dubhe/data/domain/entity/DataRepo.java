package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.dubhe.biz.base.dto.TeamDTO;
import org.dubhe.biz.base.dto.UserDTO;
import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.data.domain.dto.DatasetCreateDTO;
import org.dubhe.data.domain.dto.DatasetCustomCreateDTO;
import org.dubhe.data.machine.constant.DataStateCodeConstant;

import java.sql.Timestamp;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("data_repo")
@ApiModel(value = "DataRepo对象", description = "数据集管理")
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class DataRepo {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String remark;

    @ApiModelProperty(value = "数据集存储位置")
    private String uri;

    @TableField(value = "create_user_id", fill = FieldFill.INSERT)
    private Long createUserId;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Timestamp createTime;

    @TableField("deleted")
    private Boolean deleted;

    private Long originUserId;


    // @ApiModelProperty(value = "资源拥有人id")
    // private Long originUserId;
}
