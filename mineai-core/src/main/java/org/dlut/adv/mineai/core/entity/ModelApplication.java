package org.dlut.adv.mineai.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author mingming
 * @date 2024/11/06
 */
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table
@Getter
@Setter
@Data
public class ModelApplication implements Serializable {

    /**
     * 内部索引，主键，自动生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 算法应用名称
     */
    @Column(nullable = false)
    private String applicationTaskName;

    /**
     * 应用描述信息
     */
    private String description;

    /**
     * 应用
     */
    @ManyToOne
    @JoinColumn(name = "application_name_id")
    @TableField(exist = false)
    private ApplicationName applicationName;

    @ManyToMany
    @JoinTable(
            name = "model_application_scene",
            joinColumns = @JoinColumn(name = "model_application_id"),
            inverseJoinColumns = @JoinColumn(name = "scene_id")
    )
    private List<Scene> applicableScene;

    /**
     * 内部索引，主键，自动生成
     */
    @Column(nullable = false)
    private Integer isDelete = 0;

    /**
     * 设备
     */
    @ManyToOne
    @JoinColumn(name = "device_id", nullable = true) // 外键列名为 device_id
    private Device device;

    /**
     * 算法应用发布时间
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date releaseTime;

    /**
     * 发布者名称
     */
    private String publisherId;

    /**
     * 发布状态
     */
    private String isReleased = "未发布";

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    /**
     * 算法应用包路径
     */
    private String appZipPath;

    /**
     * 算法应用包大小
     */
    private Long appZipSize;

    @ManyToOne
    @JoinColumn(name = "model_id")
    private Model model;

    /**
     * 算法来源，默认值为1
     */
    @Column(nullable = false)
    private Integer source = 1;

    @Column(nullable = false)
    private Integer creatorId;


    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;

}
