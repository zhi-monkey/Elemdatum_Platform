

package org.dubhe.admin.domain.dto;

import lombok.Data;
import org.dubhe.biz.db.annotation.Query;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

/**
 * @description 团队查询DTO
 * @date 2020-06-01
 */
@Data
public class TeamQueryDTO implements Serializable {

    private static final long serialVersionUID = 700075706114767404L;
    @Query(type = Query.Type.IN, propName = "id")
    private Set<Long> ids;

    @Query(type = Query.Type.LIKE)
    private String name;

    @Query
    private Boolean enabled;

    @Query
    private Long pid;

    @Query(propName = "create_time", type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}
