package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Table
@Entity
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
@ToString
@Data
public class ModelVersion implements Serializable {
    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 版本号
     * 例子  微信的版本号信息：微信3.7.6.44(电脑) Version 8.0.32(手机)
     */

    private String name;

    /**
     * 镜像版本号
     */
    private String level;

    private int versionLevel;

    public static int BASIC_VERSION = 1;

    public static int BEST_VERSION = 2;
    /**
     * 展示的版本号名字，不唯一
     */
    private String showName;

    /**
     * 版本信息描述
     */
    private String description;

    /**
     * 模型版本大小（以MB为基本单位）
     */
    private long size;

    /**
     * 算力芯片
     */
    // 多个ModelVersion关联一个Chip
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "convert_image_chip_id", nullable = true)  // 外键名为convert_image_chip_id
    private Chip chip;  // 每个ModelVersion都指向一个Chip

    /**
     * 超参
     */
    @OneToMany
    private List<ModelConfig> modelConfigList;

    /**
     * 架构(amd64 就是x86_64)
     */
    private String architecture;
    public static final String AMD64 = "amd64";
    public static final String ARM64 = "arm64";

    /**
     * 权重文件路径
     */
    private String weightPath;

    /**
     * 是否可训练
     */
    private boolean isTrainable;

    /**
     * 是否可质检
     */
    private boolean isInspectable;

    /**
     * 是否可推理
     */
    private boolean isInferable;

    /**
     * 是否用于自动标注
     */
    private boolean isAutoLabel;

    /**
     * 是否需要使用GPU
     */
    private boolean isUseGpu;
    /**
     * 是否是复用的算法
     */
    private boolean isReuse;
    /**
     *复用modelVersion的Id
     */
    private long reuseId;
    private String url;

    /**
     * 上传时间
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 是否删除 （0为未删除，1为删除） 默认为0
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int isDelete;
    public static final int NOT_DELETE = 0;
    public static final int DELETE = 1;

    /**
     * 算法版本训练情况数量统计
     */
    private String trainNum;

    /**
     * 算法版本质检情况数量统计
     */
    private String inspectNum;

    /**
     * 在平台用户进行区域标注时，算法给用户的提示信息
     */
    private String areaLabelingTip;

    /**
     * 是否删除 （0为上传中，1为已上传） 默认为0
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int status;
    public static final int uploading = 0;
    public static final int uploaded = 1;


    /**
     * 转换平台
     */
    private String conversionPlatform;
    public static final String NPU = "NPU";
    public static final String NX = "NX";
    public static final String RV1126 = "RV1126";
    public static final String RK3588 = "RK3588";
    public static final String BM1684 = "BM1684";

    private long roleId;

    //是否公开
    private boolean isPublic;

    /*
    所属的用户id
    */
    private long userId;

    /**
     * 标注类型, 默认为目标检测Detection, 平台还支持语义分割Segmentation
     */
    private String annotationType = "Detection";
    /**
     * 标注格式, 默认为YOLO, 平台还支持COCO(吗)
     */
    private String annotationFormat = "YOLO";
}
