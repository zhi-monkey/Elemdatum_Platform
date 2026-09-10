package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.data.dao.RtspCaptureExecutionMapper;
import org.dubhe.data.domain.dto.DatasetDeleteDTO;
import org.dubhe.data.domain.entity.RtspCaptureExecution;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.RtspCaptureExecutionService;
import org.dubhe.data.service.RtspCaptureTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(rollbackFor = Exception.class)
public class RtspCaptureExecutionServiceImpl extends ServiceImpl<RtspCaptureExecutionMapper, RtspCaptureExecution> implements RtspCaptureExecutionService {

    private static final Logger log = LoggerFactory.getLogger(RtspCaptureExecutionServiceImpl.class);

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private RtspCaptureTaskService rtspCaptureTaskService;

    @Autowired
    private UserContextService userContextService;

    @Override
    public Page<RtspCaptureExecution> page(Page<RtspCaptureExecution> page, RtspCaptureExecution query) {
        if (query == null) {
            query = new RtspCaptureExecution();
        }
        LambdaQueryWrapper<RtspCaptureExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtspCaptureExecution::getIsDeleted, NOT_DELETED)
                .eq(query.getTaskId() != null, RtspCaptureExecution::getTaskId, query.getTaskId())
                .eq(query.getStatus() != null, RtspCaptureExecution::getStatus, query.getStatus())
                .orderByDesc(RtspCaptureExecution::getCreateTime)
                .orderByDesc(RtspCaptureExecution::getId);
        appendTaskScope(wrapper);
        return page(page, wrapper);
    }

    @Override
    public RtspCaptureExecution detail(Long id) {
        RtspCaptureExecution execution = lambdaQuery()
                .eq(RtspCaptureExecution::getId, id)
                .eq(RtspCaptureExecution::getIsDeleted, NOT_DELETED)
                .one();
        if (execution == null || rtspCaptureTaskService.detail(execution.getTaskId()) == null) {
            return null;
        }
        return execution;
    }

    @Override
    public boolean create(RtspCaptureExecution execution) {
        execution.setId(null);
        execution.setIsDeleted(NOT_DELETED);
        execution.setCreateTime(LocalDateTime.now());
        if (execution.getTaskId() != null && rtspCaptureTaskService.detail(execution.getTaskId()) == null) {
            return false;
        }
        return save(execution);
    }

    @Override
    public boolean update(Long id, RtspCaptureExecution execution) {
        RtspCaptureExecution current = detail(id);
        if (current == null) {
            return false;
        }
        execution.setId(id);
        execution.setIsDeleted(null);
        execution.setCreateTime(null);
        return updateById(execution);
    }

    @Override
    public boolean delete(Long id) {
        RtspCaptureExecution execution = detail(id);
        if (execution == null) {
            return false;
        }
        return lambdaUpdate()
                .eq(RtspCaptureExecution::getId, id)
                .eq(RtspCaptureExecution::getIsDeleted, NOT_DELETED)
                .set(RtspCaptureExecution::getIsDeleted, DELETED)
                .update();
    }

    @Override
    public boolean deleteWithDataset(Long id) {
        RtspCaptureExecution execution = detail(id);
        if (execution == null) {
            return false;
        }
        // 先软删除执行记录
        boolean deleted = delete(id);
        // 再删除关联数据集
        if (deleted && execution.getDatasetId() != null) {
            try {
                DatasetDeleteDTO dto = new DatasetDeleteDTO();
                dto.setIds(new Long[]{execution.getDatasetId()});
                datasetService.delete(dto);
                log.info("执行记录 [{}] 删除，同步删除关联数据集 [{}]", id, execution.getDatasetId());
            } catch (Exception e) {
                log.error("删除关联数据集失败，执行记录ID={}，数据集ID={}，执行记录已删除", id, execution.getDatasetId(), e);
            }
        }
        return deleted;
    }

    @Override
    public boolean cancel(Long id) {
        RtspCaptureExecution execution = detail(id);
        if (execution == null) {
            return false;
        }
        Integer status = execution.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            return false;
        }
        RtspCaptureExecution update = new RtspCaptureExecution();
        update.setId(id);
        update.setStatus(4);
        update.setEndTime(LocalDateTime.now());
        update.setMessage("手动停止采集");
        return updateById(update);
    }

    private void appendTaskScope(LambdaQueryWrapper<RtspCaptureExecution> wrapper) {
        UserContext curUser = currentUser();
        if (isManagerRole(curUser)) {
            return;
        }
        Long userId = curUser == null ? null : curUser.getId();
        if (userId == null) {
            wrapper.eq(RtspCaptureExecution::getTaskId, -1L);
            return;
        }
        wrapper.inSql(RtspCaptureExecution::getTaskId,
                "SELECT id FROM rtsp_capture_task WHERE is_deleted = 0 AND create_user_id = " + userId);
    }

    private UserContext currentUser() {
        try {
            return userContextService.getCurUser();
        } catch (Exception e) {
            log.warn("获取当前登录用户失败，数据回流执行记录按无可见数据处理：{}", e.getMessage());
            return null;
        }
    }

    private boolean isManagerRole(UserContext curUser) {
        if (curUser == null || curUser.getRoles() == null || curUser.getRoles().isEmpty()) {
            return false;
        }
        return curUser.getRoles().stream()
                .anyMatch(role -> "管理员".equals(role.getName()) || "管理人员".equals(role.getName()));
    }
}
