package org.dlut.adv.mineai.core.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class AuditLogQueryDTO {
    // 操作人
    private String uname;

    // IP地址
    private String ip;

    // 操作描述
    private String description;

    // 操作类型（单个，用于分页查询）
    private Integer operationType;

    // 操作类型（多个，用逗号分隔，用于导出）
    private String operationTypes;

    // 状态
    private Integer requestStatus;

    // 时间范围 - 开始时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    // 时间范围 - 结束时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
