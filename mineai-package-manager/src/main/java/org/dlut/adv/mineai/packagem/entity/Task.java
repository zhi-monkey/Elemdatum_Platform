package org.dlut.adv.mineai.packagem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.dlut.adv.mineai.packagem.enums.TaskStatus;

import java.util.concurrent.ScheduledFuture;

/**
 * @author mingming
 * @date 2024/09/29
 */
@Data
public class Task {
    /**
     * 任务状态
     */
    private TaskStatus status;
    /**
     * 任务开始时间，时间超过5分钟则任务超时
     */
    private Long startTime;
    /**
     * 带zip扩展名的文件名
     */
    private String fullFileName;
    /**
     * 打包完成之后前端轮询到这，凭文件名去linux目录拿文件，这个是不带后缀名的文件名
     */
    private String fileName;
    /**
     * 用于存储调度任务
     */
    @JsonIgnore
    private ScheduledFuture<?> scheduledFuture;
    /**
     * 错误类型（如：AUTH_CODE_INVALID）
     */
    private String errorType;
    /**
     * 错误详细信息
     */
    private String errorMessage;

}
