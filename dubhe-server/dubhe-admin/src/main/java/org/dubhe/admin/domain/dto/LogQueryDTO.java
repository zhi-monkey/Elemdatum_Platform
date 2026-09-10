

package org.dubhe.admin.domain.dto;

import lombok.Data;
import org.dubhe.biz.db.annotation.Query;

import java.sql.Timestamp;
import java.util.List;

/**
 * @description 日志查询类
 * @date 2020-06-01
 */
@Data
public class LogQueryDTO {

    @Query(blurry = "username,requestIp,method,params")
    private String blurry;

    @Query
    private String logType;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> createTime;
}
