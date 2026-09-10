package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "audit_log")
@Data
public class AuditLogModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "request_status", nullable = false)
    private Integer requestStatus;

    @Column(name = "operation_type", nullable = false)
    private Integer operationType;

    @Column(nullable = false)
    private Integer uid;

    @Column(name = "uname", nullable = false)
    private String uname;

    @Column()
    private String ip;

    @Column()
    private String description;

    @Column(name = "create_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createDate;

    @Column()
    private String method;

    @Lob
    private String params;

    @Column(name = "execution_time", nullable = false)
    private long executionTime;

    @Lob
    @Column(name = "exception_desc")
    private String exceptionDesc;

    @Column(name = "request_uri")
    private String requestUri;

    // 预初始化日期格式（非持久化字段）
    @Transient
    @JsonIgnore  // 告诉Jackson不要序列化这个字段
    private DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    //遇到中文问题需要修改一下表
    //ALTER TABLE audit_log CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
}

