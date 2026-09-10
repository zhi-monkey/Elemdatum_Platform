package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.biz.permission.base.BaseService;
import org.dubhe.biz.permission.util.SqlUtil;
import org.dubhe.data.dao.FileSearchMapper;
import org.dubhe.data.domain.dto.FileSearchDTO;
import org.dubhe.data.domain.entity.LabelTemplate;
import org.dubhe.data.domain.vo.FileSearchResultVO;
import org.dubhe.data.service.FileSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @description 图片多条件检索服务实现
 * @date 2026-08-26
 */
@Service
public class FileSearchServiceImpl implements FileSearchService {

    /**
     * 排序字段白名单（前端字段 -> SQL 列），杜绝 SQL 注入
     */
    private static final Map<String, String> SORT_FIELD_MAP = new HashMap<>();

    static {
        SORT_FIELD_MAP.put("id", "f.id");
        SORT_FIELD_MAP.put("name", "f.name");
        SORT_FIELD_MAP.put("updateTime", "f.update_time");
        SORT_FIELD_MAP.put("captureTime", "m.capture_time");
    }

    @Autowired
    private FileSearchMapper fileSearchMapper;

    @Resource
    private UserContextService userContextService;

    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public IPage<FileSearchResultVO> search(FileSearchDTO query) {
        long current = (query.getCurrent() == null || query.getCurrent() < 1) ? 1 : query.getCurrent();
        long size = (query.getSize() == null || query.getSize() < 1) ? 20 : query.getSize();
        Page<FileSearchResultVO> page = new Page<>(current, size);

        // 数据权限：非管理员仅可见 create_user_id 属于自己或公共(0) 的数据，与 SqlUtil.buildTargetSql 语义一致
        Set<Long> resourceUserIds = null;
        UserContext curUser = userContextService.getCurUser();
        if (curUser != null && !BaseService.isAdmin(curUser)) {
            resourceUserIds = new HashSet<>(SqlUtil.getResourceIds(curUser));
        }

        String orderBy = buildOrderBy(query);
        return fileSearchMapper.search(page, query, resourceUserIds, orderBy);
    }

    @Override
    public List<LabelTemplate> listAllLabels() {
        return fileSearchMapper.listAllLabels();
    }

    private String buildOrderBy(FileSearchDTO query) {
        String column = SORT_FIELD_MAP.getOrDefault(query.getSortField(), "f.id");
        String order = "desc".equalsIgnoreCase(query.getSortOrder()) ? "DESC" : "ASC";
        return column + " " + order;
    }
}
