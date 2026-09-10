package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.data.dao.LabelTemplatePointcloudMapper;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.LabelTemplatePointcloud;
import org.dubhe.data.service.LabelTemplatePointcloudService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 点云标签库服务实现（独立表 label_template_pointcloud）
 * @author mingming
 * @date 2025/04/16
 */
@Service
public class LabelTemplatePointcloudServiceImpl extends ServiceImpl<LabelTemplatePointcloudMapper, LabelTemplatePointcloud> implements LabelTemplatePointcloudService {

    @Resource
    private LabelTemplatePointcloudMapper labelTemplatePointcloudMapper;

    @Override
    public List<LabelTemplatePointcloud> getAll() {
        return labelTemplatePointcloudMapper.selectList(null);
    }

    @Override
    public Map<String, Object> listVO(Page<LabelTemplatePointcloud> page, LabelTemplatePointcloud labelTemplatePointcloud) {
        QueryWrapper<LabelTemplatePointcloud> queryWrapper = WrapperHelp.getWrapper(labelTemplatePointcloud);
        // 按照 id 降序排序
        queryWrapper.orderByDesc("id");
        // 名称过滤（支持中文名或标注名模糊匹配）
        if (!StringUtils.isEmpty(labelTemplatePointcloud.getName())) {
            queryWrapper.and(w -> w.like("name", labelTemplatePointcloud.getName())
                    .or().like("display_name", labelTemplatePointcloud.getName()));
        }
        Page<LabelTemplatePointcloud> resultPage = baseMapper.selectPage(page, queryWrapper);
        return PageUtil.toPage(page, resultPage.getRecords());
    }

    @Override
    public boolean save(LabelTemplatePointcloud labelTemplatePointcloud) {
        // 默认形状：RECT（封闭矩形）
        if (StringUtils.isEmpty(labelTemplatePointcloud.getShape())) {
            labelTemplatePointcloud.setShape("RECT");
        }
        // 标注名称（annotationName）必填，且 name 与 annotationName 保持一致
        if (StringUtils.isEmpty(labelTemplatePointcloud.getAnnotationName())) {
            throw new BusinessException("点云标签的标注名称不能为空");
        }
        if (StringUtils.isEmpty(labelTemplatePointcloud.getName())) {
            labelTemplatePointcloud.setName(labelTemplatePointcloud.getAnnotationName());
        }
        // 判重：按标注名称（annotationName）
        QueryWrapper<LabelTemplatePointcloud> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("annotation_name", labelTemplatePointcloud.getAnnotationName());
        if (labelTemplatePointcloud.getId() != null) {
            queryWrapper.ne("id", labelTemplatePointcloud.getId());
        }
        int count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("标签名称已存在");
        }
        return super.save(labelTemplatePointcloud);
    }

    @Override
    public boolean updateById(LabelTemplatePointcloud labelTemplatePointcloud) {
        // 默认形状：RECT（封闭矩形）
        if (StringUtils.isEmpty(labelTemplatePointcloud.getShape())) {
            labelTemplatePointcloud.setShape("RECT");
        }
        if (StringUtils.isEmpty(labelTemplatePointcloud.getAnnotationName())) {
            throw new BusinessException("点云标签的标注名称不能为空");
        }
        if (StringUtils.isEmpty(labelTemplatePointcloud.getName())) {
            labelTemplatePointcloud.setName(labelTemplatePointcloud.getAnnotationName());
        }
        // 判重：按标注名称（annotationName）
        QueryWrapper<LabelTemplatePointcloud> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("annotation_name", labelTemplatePointcloud.getAnnotationName());
        if (labelTemplatePointcloud.getId() != null) {
            queryWrapper.ne("id", labelTemplatePointcloud.getId());
        }
        int count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("标签名称已存在");
        }
        return super.updateById(labelTemplatePointcloud);
    }

    @Override
    public void delete(LabelTemplateDeleteDTO labelTemplateDeleteDTO) {
        if (CollectionUtils.isEmpty(Collections.singleton(labelTemplateDeleteDTO.getIds()))) {
            return;
        }
        for (Long id : labelTemplateDeleteDTO.getIds()) {
            baseMapper.deleteById(id);
        }
    }
}
