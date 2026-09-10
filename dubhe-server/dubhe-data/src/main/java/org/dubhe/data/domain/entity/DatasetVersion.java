

package org.dubhe.data.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.constant.UserConstant;
import org.dubhe.biz.db.entity.BaseEntity;
import org.dubhe.data.domain.dto.DatasetVersionCreateDTO;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * @description 数据集版本管理
 * @date 2020-05-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("data_dataset_version")
@ApiModel(value = "Dataset版本对象", description = "数据集版本管理")
@Builder
@AllArgsConstructor
public class DatasetVersion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属数据集ID")
    private Long datasetId;

    @ApiModelProperty(value = "团队ID")
    private Long teamId;


    @ApiModelProperty(value = "版本号")
    private String versionName;

    @ApiModelProperty(value = "版本说明")
    private String versionNote;

    @ApiModelProperty(value = "来源版本号")
    private String versionSource;

    @ApiModelProperty(value = "版本信息存储url")
    private String versionUrl;

    @ApiModelProperty(value = "版本信息转换")
    private Integer dataConversion;

    @TableField(value = "deleted",fill = FieldFill.INSERT)
    private Boolean deleted = false;

    @ApiModelProperty(value = "资源拥有人id")
    private Long originUserId;

    @ApiModelProperty(value = "是否生成ofRecord文件")
    private Integer ofRecord;

    @ApiModelProperty(value = "格式")
    private String format;

    @ApiModelProperty(value = "是否是切分的数据集")
    private Boolean isSplit;

    @ApiModelProperty(value = "切分比例")
    private Double splitSize;

    @ApiModelProperty(value = "是否是上半部分")
    private Boolean isUpperHalf;

    @ApiModelProperty(value = "创建人id")
    private Long createUserId;

    @ApiModelProperty(value = "创建者名称")
    @TableField(exist = false)
    private String createUserName;

    @ApiModelProperty(value = "是否公开")
    private short isPublic;

    @TableField(exist = false)
    private Map<Long, Long> labelMappings;

    public DatasetVersion() {
    }

    public DatasetVersion(String versionSource, String versionUrl, DatasetVersionCreateDTO datasetVersionCreateDTO) {
        this.datasetId = datasetVersionCreateDTO.getDatasetId();
        this.versionName = datasetVersionCreateDTO.getVersionName();
        this.versionNote = datasetVersionCreateDTO.getVersionNote();
        this.versionSource = versionSource;
        this.versionUrl = versionUrl;
        this.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    public DatasetVersion(Long datasetId, String versionName, String versionNote) {
        this.datasetId = datasetId;
        this.versionName = versionName;
        this.setCreateUserId(UserConstant.DEFAULT_CREATE_USER_ID);
        this.setCreateTime(new Timestamp(System.currentTimeMillis()));
        this.versionUrl = "dataset/"+datasetId +"/versionFile/"+versionName;
        this.dataConversion = NumberConstant.NUMBER_2;
        this.originUserId = UserConstant.DEFAULT_ORIGIN_USER_ID;
        this.versionNote = versionNote;
    }

    public DatasetVersion(Long datasetId, String datasetUri, String versionName, String versionNote) {
        this.datasetId = datasetId;
        this.versionName = versionName;
        this.setCreateUserId(UserConstant.DEFAULT_CREATE_USER_ID);
        this.setCreateTime(new Timestamp(System.currentTimeMillis()));
        // Use the dataset's actual storage URI instead of hardcoding dataset/{id}
        this.versionUrl = datasetUri + "/versionFile/" + versionName;
        this.dataConversion = NumberConstant.NUMBER_2;
        this.originUserId = UserConstant.DEFAULT_ORIGIN_USER_ID;
        this.versionNote = versionNote;
    }


}
