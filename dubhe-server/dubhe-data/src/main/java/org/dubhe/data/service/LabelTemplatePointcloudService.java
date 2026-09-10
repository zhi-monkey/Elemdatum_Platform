package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.LabelTemplatePointcloud;

import java.util.List;
import java.util.Map;

/**
 * 点云标签库服务（独立表 label_template_pointcloud）
 * @author mingming
 * @date 2025/04/16
 */
public interface LabelTemplatePointcloudService extends IService<LabelTemplatePointcloud> {

    /**
     * 查询全部点云标签
     * @return 点云标签列表
     */
    List<LabelTemplatePointcloud> getAll();

    /**
     * 点云标签分页列表
     * @param page 分页信息
     * @param labelTemplatePointcloud 查询条件
     * @return 分页结果
     */
    Map<String, Object> listVO(Page<LabelTemplatePointcloud> page, LabelTemplatePointcloud labelTemplatePointcloud);

    /**
     * 保存点云标签
     * @param labelTemplatePointcloud 点云标签实体
     * @return 是否保存成功
     */
    boolean save(LabelTemplatePointcloud labelTemplatePointcloud);

    /**
     * 根据ID更新点云标签
     * @param labelTemplatePointcloud 点云标签实体
     * @return 是否更新成功
     */
    boolean updateById(LabelTemplatePointcloud labelTemplatePointcloud);

    /**
     * 删除点云标签
     * @param labelTemplateDeleteDTO 删除DTO
     */
    void delete(LabelTemplateDeleteDTO labelTemplateDeleteDTO);
}