package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * RTSP 视频流数据源
 */
@Data
@TableName("rtsp_source")
public class RtspSource implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 数据源名称 */
    private String name;

    /** RTSP 流地址 */
    private String rtspUrl;

    /** 认证用户名（可为空） */
    private String username;

    /** 认证密码（AES-GCM 密文 Base64，不对外返回） */
    @JsonIgnore
    private String password;

    /** 描述信息 */
    private String description;

    /** 是否删除：0-否，1-是 */
    private Integer isDelete;
}
