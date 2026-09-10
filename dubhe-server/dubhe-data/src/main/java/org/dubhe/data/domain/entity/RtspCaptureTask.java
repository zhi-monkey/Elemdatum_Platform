package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据回流任务配置（父任务）
 * <p>
 * 通过 source_type 区分数据源类型：
 * <ul>
 *   <li>RTSP：rtsp_source_id 有值，http_camera_id/http_camera_server_id 为 null</li>
 *   <li>HTTP：http_camera_id 有值，rtsp_source_id 为 null</li>
 * </ul>
 */
@Data
@TableName(value = "rtsp_capture_task", autoResultMap = true)
public class RtspCaptureTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务名称 */
    private String taskName;

    /**
     * RTSP 数据源 ID（source_type=RTSP 时有值）
     * 对应 rtsp_source.id
     */
    private Long rtspSourceId;

    /**
     * HTTP 摄像头服务器 ID（source_type=HTTP 时有值）
     * 对应 http_camera_server.id
     */
    private Long httpCameraServerId;

    /**
     * HTTP 摄像机 ID（source_type=HTTP 时有值）
     * 对应 http_camera.id
     */
    private Long httpCameraId;

    /**
     * 数据源类型：RTSP / HTTP
     */
    private String sourceType;

    /**
     * 关联的数据集组 ID（dataset_group.id）
     * 采集的图片将存入该数据集组下自动创建的数据集
     */
    private Long datasetGroupId;

    /** 捕获间隔（秒） */
    private Integer captureInterval;

    /** 计划捕获的图片数量 */
    private Integer imageQuantity;

    /**
     * 数据集标注类型（102=目标检测, 103=目标分割）。
     * 创建任务时由 mineai 侧根据 ModelApplication 的 annotationType 设置，
     * 用于自动创建数据集时指定正确的标注类型，为空则默认 102。
     */
    private Integer annotateType;

    /**
     * 绑定的标签 ID 列表（dubhe-data 全局 label 表的 id）。
     * 自动创建数据集后通过 LabelService.save() 将这些标签关联到数据集（等价于 bandLabels），
     * 保证标注阶段可使用正确的标签。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Integer> labelIds;

    /** 创建人 ID */
    private Long createUserId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除：0-未删除，1-已删除 */
    private Integer isDeleted;
}
