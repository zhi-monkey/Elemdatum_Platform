package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.biz.permission.base.BaseService;
import org.dubhe.biz.permission.util.SqlUtil;
import org.dubhe.data.dao.VideoDatasetMapper;
import org.dubhe.data.dao.VideoFileSearchMapper;
import org.dubhe.data.domain.dto.VideoFileSearchDTO;
import org.dubhe.data.domain.entity.VideoDataset;
import org.dubhe.data.domain.vo.VideoFileSearchResultVO;
import org.dubhe.data.service.VideoFileSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description 视频文件检索服务实现
 * @date 2026-08-27
 */
@Service
public class VideoFileSearchServiceImpl implements VideoFileSearchService {

    /**
     * 排序字段白名单（前端字段 -> SQL 列），杜绝 SQL 注入
     */
    private static final Map<String, String> SORT_FIELD_MAP = new HashMap<>();

    static {
        SORT_FIELD_MAP.put("id", "vf.id");
        SORT_FIELD_MAP.put("name", "vf.name");
        SORT_FIELD_MAP.put("updateTime", "vf.update_time");
        SORT_FIELD_MAP.put("fileSize", "vf.file_size");
    }

    @Autowired
    private VideoFileSearchMapper videoFileSearchMapper;

    @Autowired
    private VideoDatasetMapper videoDatasetMapper;

    @Resource
    private UserContextService userContextService;

    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public IPage<VideoFileSearchResultVO> search(VideoFileSearchDTO query) {
        long current = (query.getCurrent() == null || query.getCurrent() < 1) ? 1 : query.getCurrent();
        long size = (query.getSize() == null || query.getSize() < 1) ? 20 : query.getSize();
        Page<VideoFileSearchResultVO> page = new Page<>(current, size);

        // 数据权限：非管理员仅可见 create_user_id 属于自己或公共(0) 的数据
        Set<Long> resourceUserIds = null;
        UserContext curUser = userContextService.getCurUser();
        if (curUser != null && !BaseService.isAdmin(curUser)) {
            resourceUserIds = new HashSet<>(SqlUtil.getResourceIds(curUser));
        }

        String orderBy = buildOrderBy(query);
        return videoFileSearchMapper.search(page, query, resourceUserIds, orderBy);
    }

    @Override
    public List<VideoDataset> listDatasets() {
        return videoDatasetMapper.selectList(new QueryWrapper<VideoDataset>()
                .select("id", "name")
                .eq("deleted", 0)
                .orderByAsc("id"));
    }

    private String buildOrderBy(VideoFileSearchDTO query) {
        String column = SORT_FIELD_MAP.getOrDefault(query.getSortField(), "vf.id");
        String order = "desc".equalsIgnoreCase(query.getSortOrder()) ? "DESC" : "ASC";
        return column + " " + order;
    }
}
