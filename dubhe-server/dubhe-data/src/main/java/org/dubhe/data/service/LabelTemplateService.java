package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.LabelGroup;
import org.dubhe.data.domain.entity.LabelTemplate;
import org.dubhe.data.domain.vo.LabelGroupQueryVO;

import java.util.List;
import java.util.Map;

/**
 * @author mingming
 * @date 2025/04/16
 */
public interface LabelTemplateService {
    List<LabelTemplate> getAllLabelTemplate();
    Map<String, Object> listVO(Page<LabelTemplate> page, LabelTemplate labelTemplate);
    void delete(LabelTemplateDeleteDTO labelTemplateDeleteDTO);
    
    /**
     * 保存标签模板
     * @param labelTemplate 标签模板实体
     * @return 是否保存成功
     */
    boolean save(LabelTemplate labelTemplate);
    
    /**
     * 根据ID更新标签模板
     * @param labelTemplate 标签模板实体
     * @return 是否更新成功
     */
    boolean updateById(LabelTemplate labelTemplate);
    
    /**
     * 根据标签名称查找标签库中的标签，如果不存在则创建
     *
     * @param name 标签名称
     * @return LabelTemplate 标签库中的标签实体
     */
    LabelTemplate findOrCreateByName(String name);
}