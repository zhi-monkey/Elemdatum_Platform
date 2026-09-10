
package org.dubhe.biz.base.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @description 查询镜像路径
 * @date 2020-12-14
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode
public class PtImageQueryUrlDTO {

    private Integer imageResource;

    private String imageName;

    private String imageTag;

    private Boolean isDefault;

    private List<Integer> imageTypes;

}
