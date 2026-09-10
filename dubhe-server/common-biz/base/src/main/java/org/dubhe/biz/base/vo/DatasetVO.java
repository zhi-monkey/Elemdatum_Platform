

package org.dubhe.biz.base.vo;

import lombok.Data;
import org.dubhe.biz.base.dto.GroupInfoDTO;
import org.dubhe.biz.base.dto.TeamSmallDTO;
import org.dubhe.biz.base.dto.UserSmallDTO;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * @description 数据集VO
 * @date 2020-04-10
 */
@Data
public class DatasetVO implements Serializable {

    /**
     * 数据集ID
     */
    private Long id;

    /**
     * 带数据类型前缀的业务编号，例如 image_12、video_12、pc_12。
     */
    private String datasetCode;

    /**
     * 数据集名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 数据集文件主目录
     */
    private String uri;

    /**
     * 数据类型
     */
    private Integer dataType;

    /**
     * 标注类型
     */
    private Integer annotateType;

    /**
     * 数据集状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 更新时间
     */
    private Timestamp updateTime;

    /**
     * 团队信息
     */
    private TeamSmallDTO team;

    /**
     * 创建人
     */
    private UserSmallDTO createUser;

    /**
     * 创建人名称
     */
    private String creatorName;

    /**
     * 更新人
     */
    private UserSmallDTO updateUser;

    /**
     * 进度
     */
    private ProgressVO progress;

    /**
     * 当前版本
     */
    private String currentVersionName;

    /**
     * 是否导入
     */
    private boolean isImport;

    /**
     * 解压状态
     */
    private Integer decompressState;

    /**
     * 是否置顶
     */
    private boolean isTop;

    /**
     * 标签组ID
     */
    private Long labelGroupId;

    /**
     * 标签组名称
     */
    private String labelGroupName;

    /**
     * 标签组类型
     */
    private Integer labelGroupType;

    /**
     * 是否自动标注
     */
    private boolean autoAnnotation;

    /**
     * 数据转换状态
     */
    private Integer dataConversion;

    /**
     * 源ID
     */
    private Long sourceId;

    /**
     * 文件数量
     */
    private Integer fileCount;

    /**
     * 模板
     */
    private Integer templateType;

    /**
     * 所属模块
     */
    private Integer module;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * [0:私有,1:公开]
     */
    private short isPublic;

    /**
     * [0:否,1:是]
     */
    private Boolean isGuided;

    /**
     * 是否处于发布中
     */
    private Boolean isPublishing;

    private Boolean deleted;

    private List<GroupInfoDTO> groups;

    /**
     * 标识数据集与上个版本相比是否有更改
     */
    private Boolean hasChanges;

}
