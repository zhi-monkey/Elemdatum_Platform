package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * HTTP 服务器下的摄像机实体
 */
@Data
@TableName("http_camera")
public class HttpCamera implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 http_camera_server.id */
    private Long httpCameraServerId;

    /** 甲方系统中的摄像机标识 */
    private String cameraId;

    /** 摄像机名称 */
    private String name;

    /** 视频流地址 */
    private String streamUrl;

    /** 摄像机描述 */
    private String description;

    /** 摄像机状态：ONLINE / OFFLINE / UNKNOWN */
    private String status;

    /** 最近一次在同步结果中出现的时间 */
    private LocalDateTime lastSeenTime;

    /** 是否删除：0-否，1-是 */
    private Integer isDelete;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
