package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.dlut.adv.mineai.core.constant.ModelJobStateCodeConstant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Table
@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class ModelJob implements Serializable {
    //一次作业
    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * job名称，创建的时候设置
     */
    @Column(unique = true)
    private String name;
    /**
     * 使用的算法版本，真正执行的时候设置，或者可以删除改选项
     */
    @ManyToOne
    private ModelVersion modelVersion;

    /**
     * 作业类型
     */
    @Column(columnDefinition = "int default 1 NOT NULL")
    private int jobType;

    /**
     * 引用Controller类
     */
    @ManyToOne
    private Controller controller;

    /**
     * 训练作业 = 1
     * 质检作业 = 2
     * 转换作业 = 3
     */
    public static final int TRAIN = 1;
    public static final int INSPECT = 2;
    public static final int CONVERT = 3;

    /**
     * 正在进行作业 = 2 作业执行成功 = 1 作业未激活 = 0 作业执行失败 = -1  取消状态 = -2
     */
//    @Column(columnDefinition = "int default 0 NOT NULL")
//    private int status;
//    public static final int EXECUTING = 2;
//    public static final int SUCCEEDED = 1;
//    public static final int NOT_ACTIVE = 0;
//    public static final int FAILED = -1;
//    public static final int CANCELLED = -2;
//    public static final int TIMEOUT = -3;

    /**
     * 所属generation
     */
    private Long modelGenerationId;

    /**
     * 作业信息描述
     */
    private String description;

    /**
     * 参数存于哈希表 创建的时候设置
     */
    @ElementCollection
    @Column(columnDefinition = "LONGTEXT")
    private Map<String, String> params;

    /**
     * 权重文件路径  真正执行的时候设置
     */
    private String weightPath;

    /**
     * 使用的资源:内存大小（单位为MB）；使用的处理器数量；GPU显存大小  创建的时候设置
     */
    private String memory;

    private String cpus;

    private String gpus;

    private String gpuMemory;

    /**
     * 输出的日志文件的路径   执行的时候设置
     */
    private String logFilePath;

    /**
     * 开始时间，结束时间
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 最后更新时间/结束时间
     */
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date lastJobTime;
    /**
     * 准确率
     */
    private Double accuracy;

    private Long trainDatasetVersionId;

    @Column(columnDefinition = "int default 0 NOT NULL")
    private Integer status;



    /**
     * 训练数据集版本ids（无版本）
     */
    @ElementCollection
    private List<Long> trainDatasetVersions;

    @ElementCollection
    private Map<Integer,String> selectedLabels;


    private String gpuMode;

    // 多卡模式下的GPU数量
    private Integer gpuCount;
    // GPU使用模式常量
    public static final String GPU_MODE_SINGLE_EXCLUSIVE = "single-exclusive";   // 单卡独占
    public static final String GPU_MODE_SINGLE_SHARED = "single-shared";         // 单卡共享
    public static final String GPU_MODE_MULTI_EXCLUSIVE = "multi-exclusive";     // 多卡独占
    public static final String GPU_MODE_MULTI_SHARED = "multi-shared";           // 多卡共享



//    public static final Integer CREATE_SUCCESS = 0;
//    public static final Integer TRAINING = 1;
//    public static final Integer PAUSED = -1;
//    public static final Integer CANCELED = -10;

//    public static final Integer TRAIN_FAILED = -2;
//    public static final Integer TRAIN_SUCCEEDED = 2;

    public static final Integer INSPECTING = 3;
    public static final Integer INSPECT_FAILED = -3;
    public static final Integer INSPECT_SUCCEEDED = 4;

//    public static final Integer CONVERTING = 5;
//    public static final Integer CONVERT_FAILED = -5;
//    public static final Integer CONVERT_SUCCEEDED = 6;

//    public static final Integer PUBLISHED = 7;

    public Boolean isExecuting() {
        if (status != null && (status.equals(ModelJobStateCodeConstant.TRAINING) || status.equals(ModelJobStateCodeConstant.CONVERTING) || status.equals(INSPECTING) || status.equals(ModelJobStateCodeConstant.SPLITTING) || status.equals(ModelJobStateCodeConstant.QUEUING))) {
            return true;
        } else {
            return false;
        }
    }
}
