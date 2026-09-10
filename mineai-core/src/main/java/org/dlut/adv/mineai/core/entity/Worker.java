package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.util.Date;

/**
 * @author dingyadong
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table
public class Worker {
    /**
     * 服务器静态信息
     */
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * ip和address均唯一
     */
    @Column(unique = true)
    private String address;

    @Column(unique = true)
    private String ip;
    private int port;

    /**
     * status 表示服务器是否还在使用 on/off
     */
    private int status;
    public static final int STATUS_OFF = 0;
    public static final int STATUS_ON = 1;

    /**
     * lastModifiedTime 服务器信息修改时间
     */
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastModifiedTime;

    /**
     * 一些服务器固有信息 创建后几乎不会修改
     */
    private String cpuInfo;
    private int cpuCores;
    private int cpuNum;
    private float cpuFrequency;
    private String gpuInfo;
    private int gpuNum;
    private long gpuMemorySize;
    private long ramSize;
    private long diskSize;
}