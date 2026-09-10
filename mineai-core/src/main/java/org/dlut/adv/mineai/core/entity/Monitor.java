package org.dlut.adv.mineai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * @author haoxiaoyang
 */
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
@Getter
@Setter
public class Monitor implements Serializable {

    public Monitor(long id, String name,int statusUsing, int isDelete, String monitorName, String subsystem) {
        this.id = id;
        this.name = name;
        this.statusUsing = statusUsing;
        this.isDelete = isDelete;
        this.monitorName = monitorName;
        this.subsystem = subsystem;
    }

    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true)
    private long id;

    /**
     * 设备序列号，设备序列号是唯一的
     */
    @Column(unique = true)
    private String name;

    /**
     * 设备名称，设备名是唯一的
     */
    @Column(unique = true)
    private String monitorName;

    /**
     * 通讯协议元数据
     */
    private String protocolMetaData;

    /**
     * 前端原始流是否显示自定义信息
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    private int useCustomInfo;
    public static final int NOT_USE = 0;
    public static final int USE = 1;

    /**
     * status 是否还在使用 on/off
     */
    @Column(columnDefinition = "int default 1 NOT NULL")
    private int statusUsing;
    public static final int OFF = 2;
    public static final int ON = 1;

    /**
     * 是否删除（0为启用 1为删除）默认为启用0
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int isDelete;
    public static final int NOT_DELETE = 0;
    public static final int DELETE = 1;

    /**
     * 是否推流
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    private int isPushStream;
    public static final int NOT_PUSH_STREAM = 0;
    public static final int PUSH_STREAM = 1;


    /**
     * 设备版本
     */
    private String version;

    /**
     * 生产厂商
     */
    private String manufacturer;

    /**
     * 设备场景 多对一  1个设备只有1个设备场景，一个设备场景可能被多个设备所有
     */
    @ManyToOne
    private Scene scene;

    /**
     * 描述信息 稍后修改为description
     */
    private String instruction;

    /**
     * 监控文件类型
     */
    public String dataType;
    public static final String VIDEO = "视频";
    public static final String AUDIO = "音频";
    public static final String IMAGE = "图像";
    public static final String POINT_CLOUD = "点云";

    /**
     * 摄像头类型=》1:可见光，2:红外，3:三维
     */
    @Column(columnDefinition = "int default 1")
    public int monitorType;
    public static final int VISIBLE_LIGHT = 1;
    public static final int INFRARED = 2;
    public static final int THREE_DIMENSIONAL = 3;

    /**
     * 绑定数据集
     */
    private Long datasetId;

    /**
     * 未绑定算法的视频名称
     */
    String originalStreamName;

    /**
     * 是否要进行推流
     */
    /**
     * 是否推流
     */
    @Column(columnDefinition = "int default -1 NOT NULL")
    private int isRecord;
    public static final int NOT_RECORD = -1;
    public static final int RECORD = 1;

    /**
     * 所属子系统，现在对接系统有：视屏，音频，后续可能有其他子系统
     */
    @Column(columnDefinition = "varchar(255) default 'CENTRAL_PLATFORM' NOT NULL")
    private String subsystem;
    public static final String CENTRAL_PLATFORM = "CENTRAL_PLATFORM";

    /**
     * 最大切片时长
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    private int maxSecond;
}
