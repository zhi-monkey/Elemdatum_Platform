package org.dubhe.data.domain.vo;

import lombok.*;
import org.dubhe.data.constant.TaskStatus;

// 任务状态VO
@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusVO {
    private String taskId;
    private TaskStatus status;
    private String errorMessage;
    private Object result;
    private Long createTime;
    private Long updateTime;
    private Integer progress; // 0-100


    public TaskStatusVO(String taskId, TaskStatus status) {
        this.taskId = taskId;
        this.status = status;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

}