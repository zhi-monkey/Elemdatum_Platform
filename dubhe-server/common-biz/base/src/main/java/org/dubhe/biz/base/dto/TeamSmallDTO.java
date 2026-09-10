

package org.dubhe.biz.base.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @description 团队DTO
 * @date 2020-06-01
 */
@Data
public class TeamSmallDTO implements Serializable {

    private static final long serialVersionUID = 7811877555052402740L;

    private Long id;

    private String name;
}
