package org.dlut.adv.mineai.core.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Model implements Serializable {

    /**
     * 内部索引
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * 一个 Model -> 多个 PublishedHyperParams
     *    mysql: on update casecade on delete casecade
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PublishedHyperParams> publishedHyperParamsList;


    /**
     * 算法名：唯一约束
     */
    @Column(unique = true)
    private String modelName;

    /**
     * 算法英文
     */
    private String modelEnglishName;

    /**
     * 最新版模型本地保存的主配置
     */
    String mainConfig;

    /**
     * 摄像头类型：
     *      1：可见光
     *      2：红外
     *      3：三维
     */
    @Column(columnDefinition = "int default 1")
    public int monitorType;
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
     * 是否删除
     *      0：未删除(默认)
     *      1：已删除
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int isDelete;
    public static final int NOT_DELETE = 0;
    public static final int DELETE = 1;

    /**
     * 绑定 CustomAttrs
     */
    @OneToMany
    private List<ModelConfig> modelConfigList;

    /**
     * 所属子系统，现在对接系统有：视屏，音频，后续可能有其他子系统
     */
    @Column(columnDefinition = "varchar(255) default 'CENTRAL_PLATFORM' NOT NULL")
    private String subsystem;
    public static final String CENTRAL_PLATFORM = "CENTRAL_PLATFORM";

    /**
     * 算法来源
     *      1：算法上传
     *      2：标准化训练
     *      3：引导式发布
     */
    @Column(columnDefinition = "int default 1")
    private int source;
    public static final int UPLOAD = 1;
    public static final int GENERATION = 2;
    public static final int GUIDE_GENERATION = 3;

    /**
     *  发布算法的生产任务ID
     */
    private long generationId;

    /**
     * 算法发布时间
     */
    @Temporal(TemporalType.TIMESTAMP)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date releaseTime;

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
     * 自动标注模型
     */
    @OneToOne
    @JoinColumn(name = "autoLabel_model_version_id")
    private ModelVersion autoLabelModelVersion;

    /**
     * 训练模型 job
     */
    @OneToOne
    private ModelJob trainModelJob;

    /**
     * 算法 对应 分类
     */
    @ManyToMany
    private List<ModelClassification> modelClassification;

    /**
     * 一个 model -> 对应 一个 chip
     */
    @OneToOne
    private Chip chip;

    private Long createUserId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Model model = (Model) o;
        return id == model.id && isDelete == model.isDelete && Objects.equals(modelName, model.modelName) && Objects.equals(modelEnglishName, model.modelEnglishName) && Objects.equals(mainConfig, model.mainConfig) && Objects.equals(description, model.description) && Objects.equals(areaLabelingTip, model.areaLabelingTip) && Objects.equals(modelConfigList, model.modelConfigList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, modelName, modelEnglishName, mainConfig, description, areaLabelingTip, isDelete, modelConfigList);
    }
}