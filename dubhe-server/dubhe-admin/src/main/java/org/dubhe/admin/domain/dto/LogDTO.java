

package org.dubhe.admin.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @description 日志转换DTO
 * @date 2020-06-01
 */
@Data
public class LogDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;

    private String username;

    private String description;

    private String method;

    private String params;

    private String browser;

    private String requestIp;

    private String address;

    private Timestamp createTime;
}
