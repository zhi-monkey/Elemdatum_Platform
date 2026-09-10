package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dubhe.biz.base.dto.UserDTO;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.vo
 * @Project：mineai
 * @name：DataTeamVO
 * @Date：2024/3/11 16:56
 * @Filename：DataTeamVO
 * @Desc：团队信息VO
 */

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "DataTeam vo", description = "团队信息")
public class DataTeamVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "团队ID")
    private Long id;

    @ApiModelProperty(value = "团队名称")
    private String name;

    @ApiModelProperty(value = "团队描述")
    private String remark;

    @ApiModelProperty(value = "团队类型")
    private Integer type;

    @ApiModelProperty(value = "团队成员数")
    private Integer memberNum;

    @ApiModelProperty("创建时间")
    private Date createTime;

    @ApiModelProperty(value = "团队成员id")
    private List<Long> userIds;

    private List<UserDTO> users;

    private Boolean canModify;
}
