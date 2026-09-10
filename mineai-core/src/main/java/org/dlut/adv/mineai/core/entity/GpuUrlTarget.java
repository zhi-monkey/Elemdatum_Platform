package org.dlut.adv.mineai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Table(name = "gpu_url_target")
@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class GpuUrlTarget implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "ip", nullable = false, length = 64)
    private String ip;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "gpu_url", nullable = false, length = 1024)
    private String gpuUrl;

    @Column(name = "platform_ip", nullable = false, length = 64)
    private String platformIp;

    @Column(name = "platform_port", nullable = false)
    private Integer platformPort;

    @Column(name = "auth_code", length = 256)
    private String authCode;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "is_delete", nullable = false)
    private Integer isDelete = 0;

    @CreatedDate
    @Column(name = "create_time")
    private Date createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private Date updateTime;

    @Column(name = "create_user_id")
    private Long createUserId;
}
