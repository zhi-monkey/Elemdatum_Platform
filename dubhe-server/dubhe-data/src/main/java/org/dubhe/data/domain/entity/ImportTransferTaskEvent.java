package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("transfer_import_task_event")
public class ImportTransferTaskEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String eventType;
    private String message;
    private String detail;
    private Long createUserId;
    private Date createTime;
}
