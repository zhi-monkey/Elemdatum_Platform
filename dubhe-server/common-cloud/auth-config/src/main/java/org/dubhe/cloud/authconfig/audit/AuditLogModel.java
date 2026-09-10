package org.dubhe.cloud.authconfig.audit;

import lombok.Data;

import java.util.Date;

@Data
public class AuditLogModel {
    private Integer id;

    private Integer requestStatus;

    private Integer operationType;

    private Integer uid;

    private String uname;

    private String ip;

    private String description;

    private Date createDate;

    private String method;

    private String params;

    private long executionTime;

    private String exceptionDesc;

    private String requestUri;

    //遇到中文问题需要修改一下表
    //ALTER TABLE audit_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
}
