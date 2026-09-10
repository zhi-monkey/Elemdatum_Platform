package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.LabelTemplateVideo;

import java.util.List;
import java.util.Map;

/**
 * 视频标签库服务（独立表 label_template_video）
 */
public interface LabelTemplateVideoService extends IService<LabelTemplateVideo> {

    /**
     * 查询全部视频标签
     * @return 视频标签列表
     */
    List<LabelTemplateVideo> getAll();

    /**
     * 视频标签分页列表
     * @param page 分页信息
     * @param labelTemplateVideo 查询条件
     * @return 分页结果
     */
    Map<String, Object> listVO(Page<LabelTemplateVideo> page, LabelTemplateVideo labelTemplateVideo);

    /**
     * 保存视频标签
     * @param labelTemplateVideo 视频标签实体
     * @return 是否保存成功
     */
    boolean save(LabelTemplateVideo labelTemplateVideo);

    /**
     * 根据ID更新视频标签
     * @param labelTemplateVideo 视频标签实体
     * @return 是否更新成功
     */
    boolean updateById(LabelTemplateVideo labelTemplateVideo);

    /**
     * 删除视频标签
     * @param labelTemplateDeleteDTO 删除DTO
     */
    void delete(LabelTemplateDeleteDTO labelTemplateDeleteDTO);
}
