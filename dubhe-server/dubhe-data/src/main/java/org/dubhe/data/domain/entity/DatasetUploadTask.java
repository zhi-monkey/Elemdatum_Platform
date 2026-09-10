package org.dubhe.data.domain.entity;

import lombok.*;

import java.util.Date;

/**
 * @description 数据集上传任务实体
 * @date 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DatasetUploadTask {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务状态常量
     */
    public static class Status {
        public static final String PROCESSING = "processing";
        public static final String SUCCESS = "success";
        public static final String FAILED = "failed";
    }
    
    /**
     * 任务ID（UUID）
     */
    private String id;
    
    /**
     * 数据集ID
     */
    private Long datasetId;
    
    /**
     * 任务状态：processing-处理中, success-成功, failed-失败
     */
    private String status;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 文件数量
     */
    private Integer fileCount;
    
    /**
     * 创建时间
     */
    private Date createdTime;
    
    /**
     * 更新时间
     */
    private Date updatedTime;
}
