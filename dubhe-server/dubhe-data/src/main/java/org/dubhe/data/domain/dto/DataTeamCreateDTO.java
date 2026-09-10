package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.dto
 * @Project：mineai
 * @name：DataTeamQueryDTO
 * @Date：2024/3/11 16:21
 * @Filename：DataTeamQueryDTO
 * @Desc：
 */

@Data
public class DataTeamCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String name;

    private String remark;

    @NotNull
    private Integer type;

    private Integer memberNum;

    @NotEmpty
    private List<Long> userIds;

    private Boolean deleted;


}
