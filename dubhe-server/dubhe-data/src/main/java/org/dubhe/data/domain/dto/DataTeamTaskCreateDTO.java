package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
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
public class DataTeamTaskCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank
    private String name;

    private Integer status;

    private String strategy;

    @NotNull
    private Long datasetId;

    private String datasetName;

    private String datasetType;

    @NotNull
    private Long teamId;

    private String teamName;

    private Boolean deleted;

    @Valid
    private List<UserProportionDTO> proportions;

    /**
     * 任务起始偏移量（从第几张图片开始分配，0表示从第一张开始）
     */
    private Integer startOffset;
}
