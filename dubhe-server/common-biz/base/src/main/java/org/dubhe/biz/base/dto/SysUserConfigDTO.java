
package org.dubhe.biz.base.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import java.io.Serializable;

/**
 * @description 系统用户配置 DTO
 * @date 2021-7-5
 */
@Data
@Accessors(chain = true)
public class SysUserConfigDTO implements Serializable{

    private static final long serialVersionUID = 1L;

    /**
     * Notebook 延迟删除时间配置
     */
    private Integer notebookDelayDeleteTime;

    /**
     * CPU 资源限制配置
     */
    private Integer cpuLimit;

    /**
     * 内存资源限制配置
     */
    private Integer memoryLimit;

    /**
     * GPU 资源限制配置
     */
    private Integer gpuLimit;

}
