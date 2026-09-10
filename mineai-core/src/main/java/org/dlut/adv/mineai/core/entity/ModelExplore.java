package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table
@Getter
@Setter
@ToString
public class ModelExplore implements Serializable {

    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 算法名称不是唯一的
     */
    private String modelName;

    /**
     * 算法别名
     */
    private String modelNickName;

    /**
     * 最新版模型本地保存的主配置
     */
    String mainConfig;

    /**
     * 摄像头类型=》1:可见光，2:红外，3:三维
     */
    @Column(columnDefinition = "int default 1")
    private int monitorType;
    public static final int VISIBLE_LIGHT = 1;
    public static final int INFRARED = 2;
    public static final int THREE_DIMENSIONAL = 3;

    /**
     * 模型描述
     */
    private String description;


    /**
     * 在平台用户进行区域标注时，算法给用户的提示信息
     */
    private String areaLabelingTip;

    /**
     * 是否删除 （0为未删除，1为删除） 默认：0
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    private int isDelete;
    public static final int NOT_DELETE = 0;
    public static final int DELETE = 1;

    /**
     *  算法发布状态（1为未发布，2为待发布）
     * */
    @Column(columnDefinition = "int default 1")
    private int modelStatus;
    public static final int UNRELEASED = 1;
    public static final int RELEASED = 2;

    /**
     *  算法与生产任务的绑定状态（1为未绑定，2为已绑定）
     * */
    @Column(columnDefinition = "int default 1")
    private int isBoundWithGeneration;
    public static final int NOT_BOUND = 1;
    public static final int BOUND = 2;

    /**
     * 绑定CustomAttrs
     */
    @OneToMany
    private List<ModelConfig> modelConfigList;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModelExplore model = (ModelExplore) o;
        return id == model.id && isDelete == model.isDelete && Objects.equals(modelName, model.modelName) && Objects.equals(mainConfig, model.mainConfig) && Objects.equals(description, model.description) && Objects.equals(areaLabelingTip, model.areaLabelingTip) && Objects.equals(modelConfigList, model.modelConfigList);
    }
    /**
     * 所属子系统，现在对接系统有：视屏，音频，后续可能有其他子系统
     */
    @Column(columnDefinition = "varchar(255) default 'CENTRAL_PLATFORM' NOT NULL")
    private String subsystem;
    public static final String CENTRAL_PLATFORM = "CENTRAL_PLATFORM";

    @Override
    public int hashCode() {
        return Objects.hash(id, modelName, mainConfig, description, areaLabelingTip, isDelete, modelConfigList);
    }

    /**
     * 创造时间
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /**
     * 申请发布时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date applyTime;

    /**
     * 算法创建人Id
     */
    private long creatorId;

    /**
     * 算法发布申请人Id
     */
    private long applicantId;

    /**
     * 算法发布状态（0=未进入被审核状态的 即 未被生产任务发布的 1=待审核、2=审核通过、3=审核未通过）
     */
    private Integer releaseStatus;
    public static Integer NOT_UNDER_EXAMINE = 0;
    public static Integer REVIEWING = 1;
    public static Integer APPROVED = 2;
    public static Integer DENIED = 3;

    /**
     * 监控文件类型
     */
    private Integer dataType;
    public static Integer IMAGE = 0;
    public static Integer VIDEO = 1;
    public static Integer TEXT = 2;

    /**
     * 训练模型
     */
    @OneToOne
    @JoinColumn(name = "train_model_version_id")
    private ModelVersion trainModelVersion;

    /**
     * 推理模型
     */
    @OneToOne
    @JoinColumn(name = "deploy_model_version_id")
    private ModelVersion deployModelVersion;

    /**
     * 转换模型
     */
    @OneToOne
    @JoinColumn(name = "convert_model_version_id")
    private ModelVersion convertModelVersion;

    /**
     * 训练模型权重路径
     */
    private String bestWeightPath;

    /**
     *  算法来源（1为上传，2为标准化发布，3为引导式发布）
     * */
    @Column(columnDefinition = "int default 1")
    public int source;
    public static final int UPLOAD = 1;
    public static final int GENERATION = 2;
    public static final int GUIDE_GENERATION = 3;

    /**
     * 拒绝发布的原因
     */
    private String rejectReason;

    /**
     * 自动标注模型
     */
    @OneToOne
    @JoinColumn(name = "autoLabel_model_version_id")
    private ModelVersion autoLabelModelVersion;

    @OneToOne
    private ModelJob releaseJob;
}
