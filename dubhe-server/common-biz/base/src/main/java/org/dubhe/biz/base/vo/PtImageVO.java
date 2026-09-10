
package org.dubhe.biz.base.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class PtImageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 镜像id
     */
    private Long id;

    /**
     * 镜像名称
     */
    private String name;

    /**
     * 镜像地址
     */
    private String imageUrl;

    /**
     * 镜像版本
     */
    private String tag;

}
