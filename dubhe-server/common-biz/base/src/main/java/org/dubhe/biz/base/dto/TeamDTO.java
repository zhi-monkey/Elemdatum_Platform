

package org.dubhe.biz.base.dto;

import lombok.Data;
import org.dubhe.biz.base.dto.UserSmallDTO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * @description 团队转换DTO
 * @date 2020-06-01
 */
@Data
public class TeamDTO implements Serializable {

    private static final long serialVersionUID = -7049447715255649751L;
    private Long id;

    private String name;

    private Boolean enabled;

    private Long pid;

    /**
     * 团队成员
     */
    private List<UserSmallDTO> teamUserList;

    private Timestamp createTime;

    public String getLabel() {
        return name;
    }
}
