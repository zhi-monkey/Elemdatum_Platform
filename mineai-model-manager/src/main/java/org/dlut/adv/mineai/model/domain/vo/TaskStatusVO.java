package org.dlut.adv.mineai.model.domain.vo;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

// 任务状态VO
@Data
@Getter
@Setter
public class TaskStatusVO {
    private String taskId;
    private String status;
    private String errorMessage;
    private Object result;
    private Long createTime;
    private Long updateTime;
    private Integer progress; // 0-100

    // 构造函数
    public TaskStatusVO() {}

    public TaskStatusVO(String taskId, String status) {
        this.taskId = taskId;
        this.status = status;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }


    // 任务状态枚举
    public enum TaskStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败");

        private String description;

        TaskStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

}