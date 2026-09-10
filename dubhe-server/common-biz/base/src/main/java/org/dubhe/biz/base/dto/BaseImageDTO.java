
package org.dubhe.biz.base.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @description 镜像基础类DTO
 * @date 2020-07-14
 */
@Data
@Accessors(chain = true)
public class BaseImageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 镜像版本
     */
    @NotBlank(message = "镜像版本不能为空")
    private String imageTag;

    /**
     * 镜像名称
     */
    @NotBlank(message = "镜像名称不能为空")
    private String imageName;

}