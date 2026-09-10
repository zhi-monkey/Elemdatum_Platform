package org.dubhe.data.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @description 视频信息VO
 * @date 2025-12-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "VideoInfoVO", description = "视频信息")
public class VideoInfoVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty("视频宽度")
    private Integer width;
    
    @ApiModelProperty("视频高度")
    private Integer height;
    
    @ApiModelProperty("视频帧率")
    private Double frameRate;
    
    @ApiModelProperty("视频时长（秒）")
    private Integer duration;
}
