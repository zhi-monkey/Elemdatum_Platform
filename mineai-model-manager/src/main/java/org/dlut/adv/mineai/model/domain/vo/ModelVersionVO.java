package org.dlut.adv.mineai.model.domain.vo;

import lombok.Data;
import org.dlut.adv.mineai.core.dto.UserSmallDTO;
import org.dlut.adv.mineai.core.entity.ModelConfig;

import javax.persistence.Column;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * 算法版本VO
 */
@Data
public class ModelVersionVO implements Serializable {

    /**
     * 内部索引
     */
    private Long id;

    private String name;

    /**
     * 镜像版本号
     */
    private String level;

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

    private List<ModelConfig> modelConfigList;

    /**
     * 架构(amd64 就是x86_64)
     */
    private String architecture;

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
    private Timestamp createTime;

    /**
     * 是否删除 （0为未删除，1为删除） 默认为0
     */
    @Column(columnDefinition = "int default 0 NOT NULL")
    public int isDelete;

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
    public int status;

    /**
     * 转换平台
     */
    private String conversionPlatform;


    private String roleName;

    /**
     * 创建人
     */
    private UserSmallDTO createUser;

}
