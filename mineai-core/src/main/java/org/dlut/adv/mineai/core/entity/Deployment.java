package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @author 高子轩
 */
@NoArgsConstructor
@AllArgsConstructor
@Table
@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Deployment implements Serializable {
    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 引用ModelVersion类
     */
    @OneToOne
    private ModelVersion modelVersion;

    /**
     * 引用Monitor类
     */
    @OneToOne
    private Monitor monitor;

    /**
     * 引用Controller类
     */
    @ManyToOne
    private Controller controller;

    private String namespace;
    private String weightPath;
    private String weightRootPath;
    private String label;
    private String gpuNum;
    private String cpuNum;
    private  String memoryNum;
    // 部署描述
    private  String description;

    @Column(unique = true)
    private String deploymentName;

    private String image;

    /**
     * 部署时间
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 部署成功 =1  部署失败 -1 部署中 2 未部署-2
     */
    @Column(columnDefinition = "int default 2 NOT NULL")
    private int status;
    public static final int SUCCEEDED = 1;
    public static final int FAILED = -1;
    public static final int EXECUTING = 2;
    public static final int NODEPLOY = -2;

    String streamUrl;


}
