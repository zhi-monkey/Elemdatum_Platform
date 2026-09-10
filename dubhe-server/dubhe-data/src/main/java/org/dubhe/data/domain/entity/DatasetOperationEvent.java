package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;
import org.dubhe.biz.db.entity.BaseEntity;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.Date;


@TableName("dataset_operation_event")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "DatasetOperationEvent对象", description = "数据集操作事件记录")
public class DatasetOperationEvent {


    public static class EventType {
        public static final Integer INFO = 0;
        public static final Integer ERROR = 1;
        public static final Integer Warning = 2;
    }

    public static class OperationType {
        public static final Integer DATA_AUGMENTATION = 0;
        public static final Integer VIDEO_FRAME_EXTRACTION = 1;
        public static final Integer AUTO_LABELING = 2;
        public static final Integer DATA_EXPORT = 3;
        public static final Integer DATA_IMPORT = 4;
        public static final Integer DATA_PUBLISH = 5;
        public static final Integer DATA_SAVE_VERSION = 6;
        public static final Integer DATA_UPLOAD = 7;
    }

    @ApiModelProperty("主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("数据集ID")
    private Long datasetId;

    @ApiModelProperty("事件提交时间")
    private Date eventSubmittedTime;

    @ApiModelProperty("事件详情")
    private String eventDetailInfo;


    @ApiModelProperty("事件类型（使用DatasetOperationEvent.EventType常量）")
    @TableField("event_type")
    private Integer eventType;

    @ApiModelProperty("操作类型（使用DatasetOperationEvent.OperationType常量）")
    @TableField("operation_type")
    private Integer operationType;

}

