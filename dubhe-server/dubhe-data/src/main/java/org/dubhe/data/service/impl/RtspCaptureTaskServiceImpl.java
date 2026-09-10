package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.data.constant.AnnotateTypeEnum;
import org.dubhe.data.capture.CaptureExecutionStatus;
import org.dubhe.data.dao.RtspCaptureExecutionMapper;
import org.dubhe.data.dao.RtspCaptureTaskMapper;
import org.dubhe.data.domain.dto.DatasetCreateDTO;
import org.dubhe.data.domain.entity.DatasetGroup;
import org.dubhe.data.domain.entity.HttpCamera;
import org.dubhe.data.domain.entity.RtspCaptureExecution;
import org.dubhe.data.domain.entity.RtspCaptureTask;
import org.dubhe.data.service.DatasetGroupService;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.HttpCameraService;
import org.dubhe.data.service.LabelService;
import org.dubhe.data.service.RtspCaptureExecutionRunnerService;
import org.dubhe.data.service.RtspCaptureTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Transactional(rollbackFor = Exception.class)
public class RtspCaptureTaskServiceImpl extends ServiceImpl<RtspCaptureTaskMapper, RtspCaptureTask> implements RtspCaptureTaskService {

    private static final Logger log = LoggerFactory.getLogger(RtspCaptureTaskServiceImpl.class);

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Autowired
    private RtspCaptureExecutionMapper rtspCaptureExecutionMapper;

    @Autowired
    private DatasetGroupService datasetGroupService;

    @Autowired
    private DatasetService datasetService;

    @Autowired
    private HttpCameraService httpCameraService;

    @Autowired
    private RtspCaptureExecutionRunnerService rtspCaptureExecutionRunnerService;

    @Autowired
    private LabelService labelService;

    @Autowired
    private UserContextService userContextService;

    /** 注入自身代理，使 REQUIRES_NEW 事务方法通过代理调用生效 */
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Page<RtspCaptureTask> page(Page<RtspCaptureTask> page, RtspCaptureTask query) {
        if (query == null) {
            query = new RtspCaptureTask();
        }
        LambdaQueryWrapper<RtspCaptureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtspCaptureTask::getIsDeleted, NOT_DELETED)
                .like(query.getTaskName() != null && !query.getTaskName().isEmpty(),
                        RtspCaptureTask::getTaskName, query.getTaskName())
                .eq(query.getSourceType() != null && !query.getSourceType().isEmpty(),
                        RtspCaptureTask::getSourceType, query.getSourceType())
                .eq(query.getRtspSourceId() != null,
                        RtspCaptureTask::getRtspSourceId, query.getRtspSourceId())
                .eq(query.getHttpCameraServerId() != null,
                        RtspCaptureTask::getHttpCameraServerId, query.getHttpCameraServerId())
                .eq(query.getHttpCameraId() != null,
                        RtspCaptureTask::getHttpCameraId, query.getHttpCameraId())
                .eq(query.getDatasetGroupId() != null,
                        RtspCaptureTask::getDatasetGroupId, query.getDatasetGroupId())
                .orderByDesc(RtspCaptureTask::getCreateTime)
                .orderByDesc(RtspCaptureTask::getId);
        appendUserScope(wrapper);
        return page(page, wrapper);
    }

    @Override
    public RtspCaptureTask detail(Long id) {
        LambdaQueryWrapper<RtspCaptureTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtspCaptureTask::getId, id)
                .eq(RtspCaptureTask::getIsDeleted, NOT_DELETED);
        appendUserScope(wrapper);
        return getOne(wrapper);
    }

    @Override
    public boolean create(RtspCaptureTask task, String datasetGroupName) {
        task.setId(null);
        task.setIsDeleted(NOT_DELETED);
        Long userId = currentUserId();
        if (userId != null) {
            task.setCreateUserId(userId);
        }

        normalizeAndValidateSource(task);

        // 数据集组处理：若未选择已有组，则按名称创建新组
        if (task.getDatasetGroupId() == null) {
            if (datasetGroupName == null || datasetGroupName.trim().isEmpty()) {
                throw new IllegalArgumentException("必须选择已有数据集组或填写新数据集组名称");
            }
            DatasetGroup group = datasetGroupService.getDatasetGroupByName(datasetGroupName.trim());
            if (group != null) {
                // 同名组已存在，直接关联
                task.setDatasetGroupId(group.getId());
            } else {
                // 创建新数据集组
                DatasetGroup newGroup = DatasetGroup.builder()
                        .name(datasetGroupName.trim())
                        .description("由数据回流任务「" + task.getTaskName() + "」自动创建")
                        .isPublic(false)
                        .build();
                datasetGroupService.create(newGroup);
                task.setDatasetGroupId(newGroup.getId());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return save(task);
    }

    @Override
    public boolean update(Long id, RtspCaptureTask task) {
        RtspCaptureTask current = detail(id);
        if (current == null) {
            return false;
        }
        task.setId(id);
        task.setIsDeleted(null);
        task.setCreateTime(null);
        task.setCreateUserId(current.getCreateUserId());
        task.setUpdateTime(LocalDateTime.now());
        normalizeAndValidateSource(task);
        return updateById(task);
    }

    @Override
    public boolean delete(Long id) {
        RtspCaptureTask current = detail(id);
        if (current == null) {
            return false;
        }
        return lambdaUpdate()
                .eq(RtspCaptureTask::getId, id)
                .eq(RtspCaptureTask::getIsDeleted, NOT_DELETED)
                .set(RtspCaptureTask::getIsDeleted, DELETED)
                .set(RtspCaptureTask::getUpdateTime, LocalDateTime.now())
                .update();
    }

    @Override
    public boolean startCapture(Long id) {
        return startCapture(id, false);
    }

    @Override
    public boolean startCapture(Long id, boolean delRaw) {
        RtspCaptureTask task = detail(id);
        if (task == null) {
            return false;
        }

        // 计算本次是第几次采集，用于数据集命名
        Integer executionCount = rtspCaptureExecutionMapper.selectCount(
                new LambdaQueryWrapper<RtspCaptureExecution>()
                        .eq(RtspCaptureExecution::getTaskId, task.getId())
        );
        int seq = (executionCount == null ? 0 : executionCount) + 1;
        String datasetName = task.getTaskName() + "-" + seq;

        // 在独立事务中创建数据集，失败不回滚外层事务
        Long datasetId = null;
        if (task.getDatasetGroupId() != null) {
            // 必须通过 Spring 代理调用，REQUIRES_NEW 才能生效
            RtspCaptureTaskServiceImpl self = applicationContext.getBean(RtspCaptureTaskServiceImpl.class);
            datasetId = self.createDatasetInNewTransaction(task, seq, datasetName);
        }

        // 创建执行记录
        RtspCaptureExecution execution = new RtspCaptureExecution();
        execution.setTaskId(task.getId());
        execution.setDatasetId(datasetId);
        execution.setStatus(CaptureExecutionStatus.QUEUED.getCode());
        execution.setProgress(BigDecimal.ZERO);
        execution.setCapturedCount(0);
        execution.setCaptureInterval(task.getCaptureInterval());
        execution.setImageQuantity(task.getImageQuantity());
        execution.setMessage("手动触发采集" + (datasetId != null ? "，数据集：" + datasetName : "，数据集创建失败"));
        execution.setCreateTime(LocalDateTime.now());
        execution.setIsDeleted(NOT_DELETED);
        boolean inserted = rtspCaptureExecutionMapper.insert(execution) > 0;
        if (inserted && execution.getId() != null) {
            rtspCaptureExecutionRunnerService.triggerAsync(execution.getId(), delRaw);
        }
        return inserted;
    }

    /**
     * 在独立事务（REQUIRES_NEW）中创建数据集，保证失败时不污染外层 startCapture 事务。
     * 注意：Spring AOP 代理要求此方法必须通过 Bean 本身调用才能生效，
     * 这里通过 ApplicationContext 自注入绕过内部调用限制。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Long createDatasetInNewTransaction(RtspCaptureTask task, int seq, String datasetName) {
        try {
            // annotateType：优先用任务配置值，为空则默认 102（目标检测）
            int annotateType = (task.getAnnotateType() != null)
                    ? task.getAnnotateType()
                    : AnnotateTypeEnum.OBJECT_DETECTION.getValue();
            DatasetCreateDTO dto = DatasetCreateDTO.builder()
                    .name(datasetName)
                    .remark("由数据回流任务「" + task.getTaskName() + "」第 " + seq + " 次采集自动创建")
                    .type(0)            // 私有数据集
                    .dataType(0)        // 图片类型
                    .annotateType(annotateType)
                    .datasetGroupId(task.getDatasetGroupId())
                    .isImport(false)
                    .isGuided(false)    // 必须显式设置，避免 Boolean->boolean 拆箱 NPE
                    .isPublic((short) 0)
                    .build();
            Long datasetId = datasetService.create(dto);
            log.info("数据回流任务 [{}] 第 {} 次采集，自动创建数据集 [{}]，ID={}", task.getTaskName(), seq, datasetName, datasetId);

            // 绑定标签（等价于 bandLabels）：label 在全局 label 表已存在，
            // labelService.save 对有 id 的 Label 只建 DatasetLabel 关联记录，不会重复创建标签
            if (task.getLabelIds() != null && !task.getLabelIds().isEmpty()) {
                try {
                    java.util.List<Long> labelIdsLong = task.getLabelIds().stream()
                            .map(Integer::longValue)
                            .collect(java.util.stream.Collectors.toList());
                    java.util.List<org.dubhe.data.domain.entity.Label> labels =
                            labelService.findLabelByIds(labelIdsLong);
                    if (labels != null && !labels.isEmpty()) {
                        labelService.save(labels, datasetId);
                        log.info("自动绑定标签：datasetId={}，labelCount={}", datasetId, labels.size());
                    }
                } catch (Exception ex) {
                    log.warn("绑定标签失败（不影响采集流程）：datasetId={}，原因：{}", datasetId, ex.getMessage());
                }
            }

            return datasetId;
        } catch (Exception e) {
            log.error("自动创建数据集失败，任务ID={}，数据集名称={}，将继续创建执行记录", task.getId(), datasetName, e);
            return null;
        }
    }

    private void normalizeAndValidateSource(RtspCaptureTask task) {
        if (task.getSourceType() == null) {
            task.setSourceType("RTSP");
        }
        if ("RTSP".equalsIgnoreCase(task.getSourceType())) {
            if (task.getRtspSourceId() == null) {
                throw new IllegalArgumentException("RTSP类型任务必须指定 rtspSourceId");
            }
            task.setHttpCameraId(null);
            task.setHttpCameraServerId(null);
            return;
        }
        if ("HTTP".equalsIgnoreCase(task.getSourceType())) {
            if (task.getHttpCameraId() == null) {
                throw new IllegalArgumentException("HTTP类型任务必须指定 httpCameraId");
            }
            HttpCamera camera = httpCameraService.findAvailableById(task.getHttpCameraId());
            if (camera == null) {
                throw new IllegalArgumentException("HTTP摄像机不存在或已删除");
            }
            task.setHttpCameraServerId(camera.getHttpCameraServerId());
            task.setRtspSourceId(null);
            return;
        }
        throw new IllegalArgumentException("不支持的数据源类型: " + task.getSourceType());
    }

    private void appendUserScope(LambdaQueryWrapper<RtspCaptureTask> wrapper) {
        UserContext curUser = currentUser();
        if (isManagerRole(curUser)) {
            return;
        }
        Long userId = curUser == null ? null : curUser.getId();
        if (userId == null) {
            wrapper.eq(RtspCaptureTask::getCreateUserId, -1L);
            return;
        }
        wrapper.eq(RtspCaptureTask::getCreateUserId, userId);
    }

    private Long currentUserId() {
        UserContext curUser = currentUser();
        return curUser == null ? null : curUser.getId();
    }

    private UserContext currentUser() {
        try {
            return userContextService.getCurUser();
        } catch (Exception e) {
            log.warn("获取当前登录用户失败，数据回流任务按无可见数据处理：{}", e.getMessage());
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
