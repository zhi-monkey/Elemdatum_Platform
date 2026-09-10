package org.dubhe.data.domain.vo;

import com.amazonaws.services.dynamodbv2.xspec.L;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.dubhe.biz.base.dto.UserSmallDTO;
import org.dubhe.biz.base.vo.ProgressVO;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.DatasetVersion;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @description 数据集版本Vo
 * @date 2020-05-21
 */
@Data
public class DatasetVersionVO implements Serializable {

    @ApiModelProperty("数据集版本ID")
    private Long id;
    @ApiModelProperty("数据集ID")
    private Long datasetId;
    @ApiModelProperty(value = "数据类型:0图片，1视频")
    private Integer dataType;
    @ApiModelProperty(value = "标注类型：2分类,1目标检测,5目标跟踪")
    private Integer annotateType;
    @ApiModelProperty("数据集名称")
    private String name;
    @ApiModelProperty("数据集版本名称")
    private String versionName;
    @ApiModelProperty(value = "数据集源版本")
    private String versionSource;
    @ApiModelProperty("版本创建时间")
    private Date createTime;
    @ApiModelProperty("版本说明")
    private String versionNote;
    @ApiModelProperty("是否当前版本")
    private Boolean isCurrent;
    @ApiModelProperty("0:未标注，1:手动标注中，2:自动标注中，3:自动标注完成，4:标注完成")
    private Integer status;
    @ApiModelProperty("文件数量")
    private Integer fileCount;
    @ApiModelProperty("标注进度")
    private ProgressVO progressVO;
    @ApiModelProperty("版本信息存储url")
    private String versionUrl;
    @ApiModelProperty("二进制转换后文件url")
    private String versionOfRecordUrl;
    @ApiModelProperty("创建人")
    private UserSmallDTO createUser;
    @ApiModelProperty("创建者名称")
    private String createUserName;
    @ApiModelProperty("更新人")
    private UserSmallDTO updateUser;
    @ApiModelProperty("转换状态")
    private Integer dataConversion;
    @ApiModelProperty("图片数量")
    private Integer imageCounts;
    @ApiModelProperty("文件总大小(字节)")
    private Long totalFileSize;
    @ApiModelProperty("转预置标识")
    private Boolean presetFlag;
    @ApiModelProperty("是否生成ofRecord")
    private Integer isOfRecord;
    @ApiModelProperty(value = "数据集版本格式")
    private String format;
    @ApiModelProperty("是否公开")
    private short isPublic;
    @ApiModelProperty("创建人ID")
    private Long createUserId;

    // 新增字段：标签计数映射
    @ApiModelProperty("标签计数映射 (标签名 -> 图片数量)")
    private Map<String, Integer> labelCountMap;

    /**
     * 数据集版本VO转换方法
     *
     * @param datasetVersion 数据集版本实体
     * @param dataset        数据集实体
     * @param progress       数据集状态
     * @param fileCount      文件数量
     * @param createUser     创建用户
     * @param updateUser     编辑用户
     * @param presetFlag     预置标识
     * @param isPublic       是否公开
     * @return 数据集版本VO
     */
    public static DatasetVersionVO from(DatasetVersion datasetVersion, Dataset dataset, ProgressVO progress,
                                        Integer fileCount, UserSmallDTO createUser, UserSmallDTO updateUser, Boolean presetFlag, short isPublic) {
        if (dataset == null || datasetVersion == null) {
            return null;
        }
        return new DatasetVersionVO(){{
            setId(datasetVersion.getId());
            setDatasetId(dataset.getId());
            setDataType(dataset.getDataType());
            setAnnotateType(dataset.getAnnotateType());
            setName(dataset.getName());
            setVersionName(datasetVersion.getVersionName());
            setVersionSource(datasetVersion.getVersionSource());
            setCreateTime(datasetVersion.getCreateTime());
            setVersionNote(datasetVersion.getVersionNote());
            setVersionUrl(datasetVersion.getVersionUrl());
            setIsOfRecord(datasetVersion.getOfRecord());
            setIsCurrent(dataset.getCurrentVersionName() == null || dataset.getCurrentVersionName().equals(datasetVersion.getVersionName()));
            setStatus(dataset.getStatus());
            setFileCount(fileCount);
            setProgressVO(progress);
            setVersionUrl(datasetVersion.getVersionUrl());
            setCreateUser(createUser);
            setCreateUserName(getCreateUser().getUsername()); // 从 datasetVersion 中获取 createUserName
            setUpdateUser(updateUser);
            setDataConversion(datasetVersion.getDataConversion());
            setImageCounts(fileCount);
            setPresetFlag(presetFlag);
            setFormat(datasetVersion.getFormat());
            setIsPublic(isPublic);
            setCreateUserId(datasetVersion.getCreateUserId());
            // 初始化标签计数映射
            setLabelCountMap(new HashMap<>());
        }};
    }

    public DatasetVersionVO() {
        // 在默认构造函数中初始化 labelCountMap
        this.labelCountMap = new HashMap<>();
    }

    public DatasetVersionVO(DatasetVersion datasetVersion, Dataset dataset) {
        this.id = datasetVersion.getId();
        this.name = dataset.getName();
        this.versionName = datasetVersion.getVersionName();
        this.createTime = datasetVersion.getCreateTime();
        this.versionNote = datasetVersion.getVersionNote();
        this.isCurrent = datasetVersion.getVersionName().equals(dataset.getCurrentVersionName());
        this.status = datasetVersion.getDeleted() ? 1 : 0;
        this.versionUrl = datasetVersion.getVersionUrl();
        this.format = datasetVersion.getFormat();
        this.datasetId = dataset.getId();
        this.annotateType = dataset.getAnnotateType();
        this.dataType = dataset.getDataType();
        this.versionSource = datasetVersion.getVersionSource();
        this.dataConversion = datasetVersion.getDataConversion();
        this.createUserName = datasetVersion.getCreateUserName();
        this.isPublic = datasetVersion.getIsPublic();

        // 初始化标签计数映射
        this.labelCountMap = new HashMap<>();
    }

    // 显式添加 labelCountMap 的 getter 和 setter（虽然 @Data 会生成，但为了确保）
    public Map<String, Integer> getLabelCountMap() {
        return labelCountMap != null ? labelCountMap : new HashMap<>();
    }

    public void setLabelCountMap(Map<String, Integer> labelCountMap) {
        this.labelCountMap = labelCountMap != null ? labelCountMap : new HashMap<>();
    }
}