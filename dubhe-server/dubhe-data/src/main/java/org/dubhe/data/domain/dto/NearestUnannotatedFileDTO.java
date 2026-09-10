package org.dubhe.data.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 最近未标注文件信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "NearestUnannotatedFileDTO", description = "最近未标注文件信息")
public class NearestUnannotatedFileDTO {
    
    @ApiModelProperty("目标文件ID")
    private Long fileId;
    
    @ApiModelProperty("距离当前文件的记录数")
    private Integer distance;
}
