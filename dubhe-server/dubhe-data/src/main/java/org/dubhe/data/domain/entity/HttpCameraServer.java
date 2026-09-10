package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * HTTP摄像头服务器实体
 * <p>
 * 代表甲方提供的 HTTP 摄像头管理服务器，通过调用其 HTTP API
 * 获取摄像头列表、截图等信息。
 */
@Data
@TableName("http_camera_server")
public class HttpCameraServer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 服务器名称 */
    private String name;

    /** HTTP 服务器基础地址，例如 http://192.168.1.100:8080 */
    private String serverUrl;

    /**
     * 访问令牌（AES-GCM 密文 Base64）
     * 用于访问该 HTTP 服务器的身份认证 Token
     */
    @JsonIgnore
    private String authToken;

    /** 描述信息 */
    private String description;

    /** 是否删除：0-否，1-是 */
    private Integer isDelete;
}
