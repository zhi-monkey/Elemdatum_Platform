package org.dlut.adv.mineai.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.dlut.adv.mineai.core.entity.*;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 完全 保留ModelApplication 的 VO 对象
 */
@Data
public class ModelApplicationVO implements Serializable {
    private Long id;                        // 主键（与实体类一致）
    private String applicationTaskName;     // 算法应用名称
    private String description;             // 应用描述
    private ApplicationName applicationName; // 关联应用名称（完整对象）
    private List<Scene> applicableScene;     // 适用场景列表
    private Integer isDelete = 0;           // 删除标志（默认值 0）
    private Device device;                  // 关联设备（完整对象）
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private Date releaseTime;               // 发布时间
    private String publisherId;             // 发布者
    private String isReleased = "未发布";    // 发布状态（默认值）
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;                // 更新时间
    private String appZipPath;              // 应用包路径
    private Long appZipSize;                // 应用包大小
    private Model model;                    // 关联模型（完整对象）
    private Integer source = 1;             // 算法来源（默认值 1）
    private Integer creatorId;              // 创建者ID
    private Boolean canBeDeleted = true;                // 是否可以被当前用户删除

    /**
     * 从 ModelApplication 转换为 ModelApplicationVO
     */
    public static ModelApplicationVO fromModelApplication(ModelApplication model) {
        if (model == null) {
            return null;
        }

        ModelApplicationVO vo = new ModelApplicationVO();
        vo.setId(model.getId());
        vo.setApplicationTaskName(model.getApplicationTaskName());
        vo.setDescription(model.getDescription());
        vo.setApplicationName(model.getApplicationName());
        vo.setApplicableScene(model.getApplicableScene());
        vo.setIsDelete(model.getIsDelete());
        vo.setDevice(model.getDevice());
        vo.setReleaseTime(model.getReleaseTime());
        vo.setPublisherId(model.getPublisherId());
        vo.setIsReleased(model.getIsReleased());
        vo.setUpdateTime(model.getUpdateTime());
        vo.setAppZipPath(model.getAppZipPath());
        vo.setAppZipSize(model.getAppZipSize());
        vo.setModel(model.getModel());
        vo.setSource(model.getSource());
        vo.setCreatorId(model.getCreatorId());

        return vo;
    }
}
