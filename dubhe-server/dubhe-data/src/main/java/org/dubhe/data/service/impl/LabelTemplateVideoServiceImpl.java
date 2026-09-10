package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.data.dao.LabelTemplateVideoMapper;
import org.dubhe.data.domain.dto.LabelTemplateDeleteDTO;
import org.dubhe.data.domain.entity.LabelTemplateVideo;
import org.dubhe.data.service.LabelTemplateVideoService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 视频标签库服务实现（独立表 label_template_video）
 */
@Service
public class LabelTemplateVideoServiceImpl extends ServiceImpl<LabelTemplateVideoMapper, LabelTemplateVideo> implements LabelTemplateVideoService {

    @Resource
    private LabelTemplateVideoMapper labelTemplateVideoMapper;

    @Override
    public List<LabelTemplateVideo> getAll() {
        return labelTemplateVideoMapper.selectList(null);
    }

    @Override
    public Map<String, Object> listVO(Page<LabelTemplateVideo> page, LabelTemplateVideo labelTemplateVideo) {
        QueryWrapper<LabelTemplateVideo> queryWrapper = WrapperHelp.getWrapper(labelTemplateVideo);
        queryWrapper.orderByDesc("id");
        // 名称过滤（支持中文名或标注名模糊匹配）
        if (!StringUtils.isEmpty(labelTemplateVideo.getName())) {
            queryWrapper.and(w -> w.like("name", labelTemplateVideo.getName())
                    .or().like("display_name", labelTemplateVideo.getName()));
        }
        Page<LabelTemplateVideo> resultPage = baseMapper.selectPage(page, queryWrapper);
        return PageUtil.toPage(page, resultPage.getRecords());
    }

    @Override
    public boolean save(LabelTemplateVideo labelTemplateVideo) {
        // 标注名称（annotationName）必填，且 name 与 annotationName 保持一致
        if (StringUtils.isEmpty(labelTemplateVideo.getAnnotationName())) {
            throw new BusinessException("视频标签的标注名称不能为空");
        }
        if (StringUtils.isEmpty(labelTemplateVideo.getName())) {
            labelTemplateVideo.setName(labelTemplateVideo.getAnnotationName());
        }
        // 判重：按标注名称（annotationName）
        QueryWrapper<LabelTemplateVideo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("annotation_name", labelTemplateVideo.getAnnotationName());
        if (labelTemplateVideo.getId() != null) {
            queryWrapper.ne("id", labelTemplateVideo.getId());
        }
        int count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("标签名称已存在");
        }
        return super.save(labelTemplateVideo);
    }

    @Override
    public boolean updateById(LabelTemplateVideo labelTemplateVideo) {
        if (StringUtils.isEmpty(labelTemplateVideo.getAnnotationName())) {
            throw new BusinessException("视频标签的标注名称不能为空");
        }
        if (StringUtils.isEmpty(labelTemplateVideo.getName())) {
            labelTemplateVideo.setName(labelTemplateVideo.getAnnotationName());
        }
        // 判重：按标注名称（annotationName）
        QueryWrapper<LabelTemplateVideo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("annotation_name", labelTemplateVideo.getAnnotationName());
        if (labelTemplateVideo.getId() != null) {
            queryWrapper.ne("id", labelTemplateVideo.getId());
        }
        int count = baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("标签名称已存在");
        }
        return super.updateById(labelTemplateVideo);
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
