package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Controller implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //设备索引
    private long id;

    //设备ip
    @Column(unique = true)
    private String ip;

    //设备名称
    @Column(unique = true)
    private String name;
    //设备端口号
    private int port;



    private String cpuInfo;
    private int cpuCores;
    private float cpuFrequency;


    /**
     * 设备内存
     * */
    private long ramSize;
    /**
     * 硬盘大小
     * */
    private long diskSize;

    //控制器（0为启用 1为删除）默认为启用0
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int isDelete;
    public static final int UNDELETED = 0;
    public static final int DELETED = 1;



    /**
     * status 是否还在使用 on/off
     */
    private int statusUsing;
    public static final int STATUS_OFF = 2;
    public static final int STATUS_ON = 1;

    private int status;
    public static final int STATUS_ERROR = -1;
    public static final int STATUS_WORKING = 1;

    /**
     * 服务列表
     */
    @OneToMany
    private List<MineService> mineService;
    /**
     *   上次更新时间
     */
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastModifiedTime;

    /**
     * 控制器节点标签（唯一）
     * */
    @Column(unique = true)
    private String label;

    /**
     * 控制器节点描述
     * */
    private String description;

    /**
     * 架构(amd64 就是x86_64)
     */
    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'amd64'")
    private String architecture;
    public static final String AMD64 = "amd64";
    public static final String ARM64 = "arm64";
}
