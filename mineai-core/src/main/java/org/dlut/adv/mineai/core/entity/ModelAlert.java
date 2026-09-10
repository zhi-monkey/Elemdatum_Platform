package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @author haoxiaoyang
 */
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Getter
@Setter
public class ModelAlert implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true)
    private long id;
    //id是唯一的

    /**
     * 警报描述
     */
    @Column(columnDefinition = "varchar(255) NOT NULL")
    private String description;

    /**
     * 警报发生时间
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 所属子系统，现在对接系统有：视屏，音频，后续可能有其他子系统
     */
    @Column(columnDefinition = "varchar(255) default 'CENTRAL_PLATFORM' NOT NULL")
    private String subsystem;
    public static final String CENTRAL_PLATFORM = "CENTRAL_PLATFORM";

    /*
     * 子系统模型名称、摄像头名称、位置、场景
     */
    private String modelName;
    private String monitorName;
    private String location;
    private String scene;

    /**
     * 所属算法
     */
    @ManyToOne
    private Model model;

    /**
     * 所属监控设备
     */
    @ManyToOne
    private Monitor monitor;

    /**
     * 是否删除   （0为未删除 ，1为删除）
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    private int isDelete;
    public static final int UNDELETED = 0;
    public static final int DELETED = 1;

    /**
     * 是否是图像警报（0为不是图像 1为是图像）默认为0
     */
    @Column(columnDefinition = "boolean default false NOT NULL")
    private Boolean image;

    /**
     * imagePath
     */
    private String imagePath;

    /**
     * 是否是视频警报（0为不是视频 1为是视频）默认为启用0
     */
    @Column(columnDefinition = "boolean default false NOT NULL")
    private Boolean video;

    /**
     * videoPath
     */
    private String videoPath;

    /**
     * 是否是音频警报（0为不是音频 1为是音频）默认为启用0
     */
    @Column(columnDefinition = "boolean default false NOT NULL")
    private Boolean audio;

    /**
     * audioPath
     */
    private String audioPath;

    /**
     * 标注框等模型所需传输的自定义数据
     */
    @ElementCollection
    private Map<String, String> data = new HashMap<>();

    /**
     * 报警信息状态
     */
    @Column(columnDefinition = "int default 1")
    private int status;
    public static final int STATUS_UNHANDLED = 1;
    public static final int STATUS_IGNORED = 2;
    public static final int STATUS_HANDLED = 3;

    /**
     * 是否为正常上报
     */
    @Column(columnDefinition = "int default 0")
    private int characteristic;
    public static final int IS_ALERT = 0;
    public static final int IS_INFO = 1;
}
