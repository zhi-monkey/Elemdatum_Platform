package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.context.DataContext;
import org.dubhe.biz.base.dto.CommonPermissionDataDTO;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.data.constant.ErrorEnum;
import org.dubhe.data.dao.LabelMapper;
import org.dubhe.data.dao.LabelTemplateMapper;
import org.dubhe.data.domain.dto.LabelGroupDeleteDTO;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.Label;
import org.dubhe.data.domain.entity.LabelGroup;
import org.dubhe.data.domain.entity.LabelTemplate;
import org.dubhe.data.domain.vo.LabelGroupQueryVO;
import org.dubhe.data.service.LabelTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static org.dubhe.data.constant.Constant.SORT_ASC;

/**
 * @author mingming
 * @date 2025/04/16
 */
@Service
public class LabelTemplateServiceImpl extends ServiceImpl<LabelTemplateMapper, LabelTemplate> implements LabelTemplateService {
    @Resource
    private LabelTemplateMapper labelTemplateMapper;
    @Override
    public List<LabelTemplate> getAllLabelTemplate() {
        return labelTemplateMapper.selectList(null);
    }

    /**
     * 按类型查询标签库列表（type 为空则返回全部）
     *
     * @param type 标签类型：0=图片/视频，1=点云；null=全部
     * @return 标签列表
     */
    public List<LabelTemplate> getAllLabelTemplate(Integer type) {
        if (type == null) {
            return getAllLabelTemplate();
        }
        QueryWrapper<LabelTemplate> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", type);
        return labelTemplateMapper.selectList(queryWrapper);
    }

    /**
     * 标签库分页列表
     *
     * @param page              分页信息
     * @param labelTemplate 查询条件
     * @return Map<String, Object> 查询出对应的标签
     */
    @Override
    @DataPermissionMethod
    public Map<String, Object> listVO(Page<LabelTemplate> page, LabelTemplate labelTemplate) {
        return queryLabelTemplate(page, labelTemplate, null);
    }
    public Map<String, Object> queryLabelTemplate(Page<LabelTemplate> page, LabelTemplate labelTemplate, Long labelGroupId) {

        QueryWrapper<LabelTemplate> queryWrapper = WrapperHelp.getWrapper(labelTemplate);
        // 添加按照 id 降序排序
        queryWrapper.orderByDesc("id");
        // 如果name为空，直接查询
        if(!StringUtils.isEmpty(labelTemplate.getName())){
            queryWrapper.eq("name",labelTemplate.getName());
        }
        // 按类型过滤：0=图片/视频，1=点云
        if (labelTemplate.getType() != null) {
            queryWrapper.eq("type", labelTemplate.getType());
        }
        Page<LabelTemplate> labelTemplatePage = baseMapper.selectPage(page, queryWrapper);
        List<LabelTemplate> labelTemplates = labelTemplatePage.getRecords();
        Map<String, Object> stringObjectMap = PageUtil.toPage(page,labelTemplates);
        return stringObjectMap;
    }

    /**
     * 删除标签
     *
     * @param labelTemplateDeleteDTO 删除标签DTO
     */
    @Override
    public void delete(LabelTemplateDeleteDTO labelTemplateDeleteDTO) {
        if (CollectionUtils.isEmpty(Collections.singleton(labelTemplateDeleteDTO.getIds()))) {
            return;
        }
        for (Long id : labelTemplateDeleteDTO.getIds()) {
            baseMapper.deleteById(id);
        }
    }

    @Override
    public boolean save(LabelTemplate labelTemplate) {
        // 默认值：type=0（图片/视频）、shape=RECT（封闭矩形）
        if (labelTemplate.getType() == null) {
            labelTemplate.setType(0);
        }
        if (StringUtils.isEmpty(labelTemplate.getShape())) {
            labelTemplate.setShape("RECT");
        }
        // 点云标签：用标注名称（annotationName）判重；图片标签：用名称（name）判重
        QueryWrapper<LabelTemplate> queryWrapper = new QueryWrapper<>();
        if (labelTemplate.getType() != null && labelTemplate.getType() == 1) {
            if (!StringUtils.isEmpty(labelTemplate.getAnnotationName())) {
                queryWrapper.eq("annotation_name", labelTemplate.getAnnotationName());
            } else {
                throw new BusinessException("点云标签的标注名称不能为空");
            }
        } else {
            queryWrapper.eq("name", labelTemplate.getName());
        }

        // 如果是更新操作，需要排除自身
        if (labelTemplate.getId() != null) {
            queryWrapper.ne("id", labelTemplate.getId());
        }

        int count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("标签名称已存在");
        }

        // 调用父类保存方法
        return super.save(labelTemplate);
    }

    @Override
    public boolean updateById(LabelTemplate labelTemplate) {
        // 默认值：type=0（图片/视频）、shape=RECT（封闭矩形）
        if (labelTemplate.getType() == null) {
            labelTemplate.setType(0);
        }
        if (StringUtils.isEmpty(labelTemplate.getShape())) {
            labelTemplate.setShape("RECT");
        }
        // 点云标签：用标注名称（annotationName）判重；图片标签：用名称（name）判重
        QueryWrapper<LabelTemplate> queryWrapper = new QueryWrapper<>();
        if (labelTemplate.getType() != null && labelTemplate.getType() == 1) {
            if (!StringUtils.isEmpty(labelTemplate.getAnnotationName())) {
                queryWrapper.eq("annotation_name", labelTemplate.getAnnotationName());
            } else {
                throw new BusinessException("点云标签的标注名称不能为空");
            }
        } else {
            queryWrapper.eq("name", labelTemplate.getName());
        }

        // 如果是更新操作，需要排除自身
        if (labelTemplate.getId() != null) {
            queryWrapper.ne("id", labelTemplate.getId());
        }

        int count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("标签名称已存在");
        }

        return super.updateById(labelTemplate);
    }
    
    /**
     * 根据标签名称查找标签库中的标签，如果不存在则创建
     *
     * @param name 标签名称
     * @return LabelTemplate 标签库中的标签实体
     */
    public LabelTemplate findOrCreateByName(String name) {
        // 不区分大小写查找标签
        List<LabelTemplate> allTemplates = baseMapper.selectList(null);
        LabelTemplate labelTemplate = null;
        for (LabelTemplate template : allTemplates) {
            if (template.getName().equalsIgnoreCase(name)) {
                labelTemplate = template;
                break;
            }
        }
        
        if (labelTemplate == null) {
            labelTemplate = new LabelTemplate();
            labelTemplate.setName(name);
            save(labelTemplate);
        }
        
        return labelTemplate;
    }
}