
package org.dubhe.biz.base.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode
public class PtImageIdDTO {

    /**
     * 镜像ID
     */
    @NotNull(message = "镜像ID不能为空")
    private Long id;
}
