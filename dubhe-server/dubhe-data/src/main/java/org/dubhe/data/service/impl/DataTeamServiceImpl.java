package org.dubhe.data.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.dto.UserDTO;
import org.dubhe.biz.base.enums.SwitchEnum;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.biz.statemachine.dto.StateChangeDTO;
import org.dubhe.cloud.authconfig.service.AdminClient;
import org.dubhe.data.client.UserClient;
import org.dubhe.data.constant.DataTeamTaskStatusEnum;
import org.dubhe.data.constant.DatasetModuleEnum;
import org.dubhe.data.constant.FileTypeEnum;
import org.dubhe.data.constant.NotificationOperationTypeEnum;
import org.dubhe.data.dao.*;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.*;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.machine.constant.DataTeamSubtaskStateMachineConstant;
import org.dubhe.data.machine.constant.DataTeamTaskStateMachineConstant;
import org.dubhe.data.machine.utils.StateMachineUtil;
import org.dubhe.data.service.AnnotationService;
import org.dubhe.data.service.DataTeamService;
import org.dubhe.data.service.DatasetVersionFileService;
import org.dubhe.data.service.NotificationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.dubhe.data.constant.Constant.SORT_ASC;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.service.impl
 * @Project：mineai
 * @name：DataTeamServiceImpl
 * @Date：2024/3/11 16:08
 * @Filename：DataTeamServiceImpl
 * @Desc：
 */

@Slf4j
@Service
public class DataTeamServiceImpl extends ServiceImpl<DataTeamMapper, DataTeam> implements DataTeamService {

    @Autowired
    private DataTeamMapper dataTeamMapper;

    @Autowired
    private DataTeamTaskMapper dataTeamTaskMapper;

    @Autowired
    private DataTeamSubtaskMapper dataTeamSubtaskMapper;

    @Autowired
    private DatasetVersionFileMapper datasetVersionFileMapper;

    @Autowired
    private DatasetMapper datasetMapper;

    @Autowired
    private DatasetVersionFileService datasetVersionFileService;

    @Autowired
    @Lazy
    private AnnotationService annotationService;

    @Autowired
    private DataTeamConvert dataTeamConvert;

    @Autowired
    private DataTeamTaskConvert dataTeamTaskConvert;

    @Autowired
    private DataTeamSubtaskConvert dataTeamSubtaskConvert;

    @Autowired
    private UserContextService userContextService;

    @Resource
    private DataTeamUserMapper dataTeamUserMapper;
    @Autowired
    private UserClient userClient;

    @Autowired
    private AdminClient adminClient;

    @Autowired
    private NotificationService notificationService;


    @Override
    public Object queryAll(DataTeamQueryDTO criteria, Page page) {
        QueryWrapper wrapper = WrapperHelp.getWrapper(criteria);
        // 设置排序规则
        if (StringUtils.isNotEmpty(criteria.getSort()) && StringUtils.isNotEmpty(criteria.getOrder())) {
            wrapper.orderBy(
                    true,
                    SORT_ASC.equals(criteria.getOrder().toLowerCase()),
                    StringUtils.humpToLine(criteria.getSort())
            );
        } else {
            wrapper.orderByDesc("id");
        }
        Page<DataTeam> teams = getBaseMapper().listPage(page, wrapper);
        teams.getRecords().stream().forEach(dataTeam -> {
            LambdaQueryWrapper<DataTeamUser> lqw = new LambdaQueryWrapper<>();
            lqw.eq(!Objects.isNull(dataTeam.getId()), DataTeamUser::getDataTeamId, dataTeam.getId());
            List<Long> userIds = dataTeamUserMapper.selectList(lqw).stream()
                    .map(DataTeamUser::getUserId)
                    .collect(Collectors.toList());
            dataTeam.setUserIds(userIds);
        });
        return PageUtil.toPage(teams, dataTeamConvert::toDto);
    }

    @Override
    public List<DataTeamVO> queryAllTeams() {
        List<DataTeam> dataTeams = dataTeamMapper.queryAllTeams();
        Collections.reverse(dataTeams);
        return dataTeamConvert.toDto(dataTeams);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataTeamVO create(DataTeamCreateDTO resources) {
        if (!Objects.isNull(dataTeamMapper.findByDataTeamName(resources.getName()))) {
            throw new BusinessException("团队名已存在");
        }
        DataTeam dataTeam = DataTeam.builder().build();
        BeanUtils.copyProperties(resources, dataTeam);
        dataTeamMapper.insert(dataTeam);
        for (Long userId : resources.getUserIds()) {
            dataTeamMapper.tiedDataTeamUser(dataTeam.getId(), userId);
        }
        return dataTeamConvert.toDto(dataTeam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataTeamVO update(DataTeamUpdateDTO resources) {

        dataTeamMapper.untiedDataTeamUserByDataTeamId(resources.getId());
        for (Long userId : resources.getUserIds()) {
            dataTeamMapper.tiedDataTeamUser(resources.getId(), userId);
        }
        DataTeam dataTeam = DataTeam.builder().build();
        BeanUtils.copyProperties(resources, dataTeam);
        dataTeamMapper.updateById(dataTeam);
        return dataTeamConvert.toDto(dataTeam);

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Set<Long> ids) {
        if (!CollectionUtil.isEmpty(ids)) {
            // 获取当前用户ID
            Long curUserId = userContextService.getCurUserId();
            // 检查是否是管理员
            boolean isAdmin = org.dubhe.biz.permission.base.BaseService.isAdmin();
            
            ids.forEach(id -> {
                // 获取团队信息
                DataTeam dataTeam = dataTeamMapper.selectById(id);
                if (dataTeam == null) {
                    throw new BusinessException("团队不存在");
                }
                
                // 检查权限：只有管理员或团队创建者可以删除团队
                if (!isAdmin && !dataTeam.getCreateUserId().equals(curUserId)) {
                    throw new BusinessException("您没有权限删除该团队，只有管理员或团队创建者可以删除团队");
                }
                
                dataTeamMapper.updateById(DataTeam.builder()
                        .id(id)
                        .deleted(SwitchEnum.getBooleanValue(SwitchEnum.ON.getValue()))
                        .build());
            });
        }
    }

    @Override
    public Object queryAllTasks(Page page, DataTeamTaskQueryDTO criteria) {
        QueryWrapper wrapper = WrapperHelp.getWrapper(criteria);

        // 设置排序规则
        if (StringUtils.isNotEmpty(criteria.getSort()) && StringUtils.isNotEmpty(criteria.getOrder())) {
            wrapper.orderBy(
                    true,
                    SORT_ASC.equals(criteria.getOrder().toLowerCase()),
                    StringUtils.humpToLine(criteria.getSort())
            );
        } else {
            wrapper.orderByDesc("id");
        }

        Page tasks = dataTeamTaskMapper.listPage(page, wrapper);
        return PageUtil.toPage(tasks, dataTeamTaskConvert::toDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object createTask(DataTeamTaskCreateDTO dto) {
        // 校验任务名重复
        if (dataTeamTaskMapper.findByDataTeamTaskName(dto.getName()) != null) {
            throw new BusinessException("任务名已存在");
        }

        // 创建主任务
        DataTeamTask task = new DataTeamTask();
        BeanUtils.copyProperties(dto, task);
        task.setStatus(DataTeamTaskStatusEnum.INIT.getValue());
        task.setStrategy(0);

        // 获取数据集和团队信息
        Dataset dataset = datasetMapper.selectById(dto.getDatasetId());
        DataTeam dataTeam = dataTeamMapper.selectById(dto.getTeamId());
        task.setDatasetName(dataset.getName());
        task.setDatasetType(dataset.getAnnotateType());
        task.setTeamName(dataTeam.getName());
        dataTeamTaskMapper.insert(task);

        // 更新数据集多人标注状态
        updateModuleByDatasetId(dto.getDatasetId(), DatasetModuleEnum.TEAM.getStatus());

        // 获取团队成员列表（带用户ID）
        List<Long> userIds = dataTeamMapper.getUserListByDataTeamId(dataTeam.getId())
                .stream()
                .sorted()
                .collect(Collectors.toList());
        if (userIds.isEmpty() || dataTeam.getMemberNum() <= 0) {
            throw new BusinessException("团队人数不足");
        }

        // 获取数据集总图片数
        Integer datasetVersionFileCount = datasetVersionFileMapper.getDatasetVersionAllFileCount(dataset.getId(), null);
        if (datasetVersionFileCount == null || datasetVersionFileCount <= 0) {
            throw new BusinessException("数据集无可用图片");
        }

        // 获取起始偏移量（默认从0开始）
        Integer startOffset = dto.getStartOffset() != null ? dto.getStartOffset() : 0;
        if (startOffset < 0 || startOffset >= datasetVersionFileCount) {
            startOffset = 0;
        }

        // 计算可分配的图片数量（从startOffset开始到结束）
        int availableImages = datasetVersionFileCount - startOffset;
        if (availableImages <= 0) {
            throw new BusinessException("起始偏移量超出数据集范围，无可分配图片");
        }

        // 计算用户分配数量 Map<>(用户ID, 分配数量)
        Map<Long, Integer> userAllocations;
        if (dto.getProportions() != null && !dto.getProportions().isEmpty()) {
            // 传入的比例不为空，则使用传入的比例进行分配
            validateProportions(dto.getProportions(), userIds, dataTeam.getMemberNum());
            userAllocations = calculateProportionalAllocation(
                    dto.getProportions(),
                    availableImages,
                    userIds
            );
        } else {
            // 默认均分模式
            userAllocations = calculateEqualAllocation(availableImages, userIds);
        }

        // 生成子任务（从startOffset开始）
        int accumulatedOffset = startOffset;
        boolean hasAnnotating = false; // 是否有子任务处于标注中状态
        for (Long userId : userIds) {
            Integer allocCount = userAllocations.get(userId);

            DataTeamSubtask subtask = new DataTeamSubtask();
            subtask.setName(dto.getName());
            subtask.setUserId(userId);
            subtask.setDatasetId(dto.getDatasetId());
            subtask.setTaskId(task.getId());
            subtask.setStartOffset((long) accumulatedOffset);

            // 如果分配数为0（起止偏移量异常：endOffset < startOffset），标记为已完成
            if (allocCount == 0) {
                subtask.setEndOffset((long) (accumulatedOffset - 1));  // endOffset < startOffset 表示无任务
                subtask.setStatus(DataTeamTaskStatusEnum.FINISHED.getValue());
                subtask.setCurrentOffset(0L);
                // 注意：accumulatedOffset 不增加，下一个用户从相同位置开始
            } else {
                subtask.setEndOffset((long) (accumulatedOffset + allocCount - 1));
                
                // 根据已标注图片数量设置 currentOffset
                Long currentOffset = datasetVersionFileMapper.countAnnotatedFilesByOffset(
                        dto.getDatasetId(), 
                        null, 
                        subtask.getStartOffset(), 
                        subtask.getEndOffset() - subtask.getStartOffset() + 1
                ).longValue();
                subtask.setCurrentOffset(currentOffset);
                
                // 根据 currentOffset 设置状态
                long totalAllocCount = subtask.getEndOffset() - subtask.getStartOffset() + 1;
                if (currentOffset == 0) {
                    subtask.setStatus(DataTeamTaskStatusEnum.INIT.getValue());
                } else if (currentOffset >= totalAllocCount) {
                    // Finished直接提交掉了
                    subtask.setStatus(DataTeamTaskStatusEnum.ANNOTATING.getValue());
                } else {
                    subtask.setStatus(DataTeamTaskStatusEnum.ANNOTATING.getValue());
                }
                
                // 只要有子任务有已标注图片，主任务就应该是标注中
                if (currentOffset > 0) {
                    hasAnnotating = true;
                }
                
                // 只有在分配数大于0时才累加偏移量
                accumulatedOffset += allocCount;
            }

            dataTeamSubtaskMapper.insert(subtask);
            dataTeamTaskMapper.tiedDataTaskSubtask(task.getId(), subtask.getId());

            // 只有在分配数大于0时才发送通知
            if (allocCount > 0) {
                // 构建通知 payload
                JSONObject payload = new JSONObject();
                payload.put("taskName", dto.getName());
                payload.put("datasetName", dataset.getName());
                payload.put("allocCount", allocCount);
                payload.put("teamName", dataTeam.getName());

                // 创建通知对象并异步保存
                Notification notification = new Notification();
                notification.setToUserId(userId);
                notification.setNotificationType(Notification.NotificationType.INFO);
                notification.setOperationType(NotificationOperationTypeEnum.ANNOTATION_TASK_ASSIGNED.getCode());
                notification.setPayload(payload.toJSONString());
                notification.setReadStatus(0);
                Date now = new Date();
                notification.setCreateTime(now);
                notification.setUpdateTime(now);
                notification.setDeleted(false);

                notificationService.safeInsertAsync(notification);
            }
        }
        
        // 根据子任务状态更新主任务状态
        if (hasAnnotating) {
            task.setStatus(DataTeamTaskStatusEnum.ANNOTATING.getValue());
            dataTeamTaskMapper.updateById(task);
        }
        
        // 把图片置为未标注
        // datasetVersionFileMapper.updateFileStatus2UnAnnotated(dataset.getId(), null);
        return dataTeamTaskConvert.toDto(task);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public Object adjustTaskProportions(Long taskId, Map<Long, Integer> userAllocations) {
        // 获取主任务
        DataTeamTask task = dataTeamTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        // 获取所有子任务（按userId排序保证顺序一致）
        List<DataTeamSubtask> subtasks = dataTeamSubtaskMapper.selectByTaskId(taskId);
        if (subtasks.isEmpty()) {
            throw new BusinessException("子任务不存在");
        }

        // 获取用户ID列表
        List<Long> userIds = subtasks.stream()
                .map(DataTeamSubtask::getUserId)
                .collect(Collectors.toList());

        // 获取数据集总图片数（与创建时保持一致）
        Integer datasetVersionFileCount = datasetVersionFileMapper.getDatasetVersionAllFileCount(
                task.getDatasetId(), null);
        if (datasetVersionFileCount == null || datasetVersionFileCount <= 0) {
            throw new BusinessException("数据集无可用图片");
        }
        if (userAllocations.size() != subtasks.size()) {
            throw new BusinessException("分配比例数量与团队成员数不匹配");
        }
        // 校验用户ID合法性
        Set<Long> allocationUserIds = userAllocations.keySet();
        if (!new HashSet<>(userIds).equals(allocationUserIds)) {
            throw new BusinessException("分配包含非法用户ID");
        }
        // 校验：分配总数不能超过数据集图片总数
        int totalAllocated = userAllocations.values().stream().mapToInt(Integer::intValue).sum();
        if (totalAllocated > datasetVersionFileCount) {
            throw new BusinessException(String.format(
                    "分配总数 (%d) 超过数据集图片总数 (%d)",
                    totalAllocated,
                    datasetVersionFileCount
            ));
        }

        // 计算起始偏移量（未分配的图片数量）
        int startOffset = datasetVersionFileCount - totalAllocated;

        // 重新分配偏移量（从startOffset开始）
        int accumulatedOffset = startOffset;
        boolean hasAnnotating = false; // 是否有子任务有已标注图片
        for (DataTeamSubtask subtask : subtasks) {
            Long userId = subtask.getUserId();
            Integer allocCount = userAllocations.get(userId);

            // 保存原有的偏移量和状态
            Long oldStartOffset = subtask.getStartOffset();
            Long oldEndOffset = subtask.getEndOffset();
            Integer oldStatus = subtask.getStatus();

            // 计算新的偏移量
            long newStart = (long) accumulatedOffset;
            long newEnd;

            // 如果分配数为0（起止偏移量异常：endOffset < startOffset），标记为已完成
            if (allocCount == 0) {
                newEnd = (long) (accumulatedOffset - 1);  // endOffset < startOffset 表示无任务
                subtask.setStartOffset(newStart);
                subtask.setEndOffset(newEnd);
                subtask.setStatus(DataTeamTaskStatusEnum.FINISHED.getValue());
                subtask.setCurrentOffset(0L);
                // 注意：accumulatedOffset 不增加，下一个用户从相同位置开始
            } else {
                newEnd = (long) (accumulatedOffset + allocCount - 1);

                // 判断偏移量是否发生改变
                boolean offsetChanged = !oldStartOffset.equals(newStart) || !oldEndOffset.equals(newEnd);

                Integer newStatus;
                Long newCurrentOffset;

                if (!offsetChanged) {
                    // 偏移量未改变，状态保持不变
                    newStatus = oldStatus;
                    newCurrentOffset = subtask.getCurrentOffset();
                } else {
                    // 偏移量发生改变，查询新区域内已标注图片数量
                    Long annotatedCount = datasetVersionFileMapper.countAnnotatedFilesByOffset(
                        task.getDatasetId(), null, newStart, newEnd - newStart + 1).longValue();
                    newCurrentOffset = annotatedCount;

                    if (oldStatus.equals(DataTeamTaskStatusEnum.FINISHED.getValue())) {
                        // 之前是已提交状态
                        long totalInNewRange = newEnd - newStart + 1;
                        if (annotatedCount.equals(totalInNewRange)) {
                            // 新区域全部已标注，保持已提交状态
                            newStatus = DataTeamTaskStatusEnum.FINISHED.getValue();
                        } else {
                            // 新区域存在未标注图片，置为未提交状态
                            newStatus = annotatedCount > 0 ?
                                DataTeamTaskStatusEnum.ANNOTATING.getValue() :
                                DataTeamTaskStatusEnum.INIT.getValue();
                        }
                    } else {
                        // 之前是未提交状态（INIT或ANNOTATING），根据新区域标注情况决定
                        if (annotatedCount == 0) {
                            newStatus = DataTeamTaskStatusEnum.INIT.getValue();
                        } else {
                            newStatus = DataTeamTaskStatusEnum.ANNOTATING.getValue();
                        }
                    }
                }

                subtask.setStartOffset(newStart);
                subtask.setEndOffset(newEnd);
                subtask.setCurrentOffset(newCurrentOffset);
                subtask.setStatus(newStatus);

                // 只要有子任务有已标注图片，主任务就应该是标注中
                if (newCurrentOffset > 0) {
                    hasAnnotating = true;
                }

                // 只有在分配数大于0时才累加偏移量
                accumulatedOffset += allocCount;
            }

            dataTeamSubtaskMapper.updateById(subtask);
        }

        // 根据子任务状态更新主任务状态
        if (hasAnnotating) {
            task.setStatus(DataTeamTaskStatusEnum.ANNOTATING.getValue());
        } else {
            task.setStatus(DataTeamTaskStatusEnum.INIT.getValue());
        }
        dataTeamTaskMapper.updateById(task);

        return dataTeamTaskConvert.toDto(task);
    }

    private void validateProportions(List<UserProportionDTO> proportions, List<Long> userIds, int memberNum) {
        // 校验数量一致性
        if (proportions.size() != memberNum) {
            throw new BusinessException("分配比例数量与团队成员数不匹配");
        }

        // 校验总和100%（支持小数，允许0.01的误差）
        BigDecimal sum = proportions.stream()
                .map(UserProportionDTO::getProportion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal hundred = new BigDecimal("100");
        if (sum.compareTo(hundred) != 0 && sum.subtract(hundred).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new BusinessException("分配比例总和必须为100%");
        }

        // 校验用户ID合法性
        Set<Long> propUserIds = proportions.stream()
                .map(UserProportionDTO::getUserId)
                .collect(Collectors.toSet());
        if (!new HashSet<>(userIds).equals(propUserIds)) {
            throw new BusinessException("分配比例包含非法用户ID");
        }
    }

    private Map<Long, Integer> calculateProportionalAllocation(
            List<UserProportionDTO> proportions,
            int totalImages,
            List<Long> userIds
    ) {
        Map<Long, Integer> allocation = new LinkedHashMap<>();
        int memberCount = userIds.size();

        int remaining = totalImages - memberCount;
        if (remaining < 0) {
            throw new BusinessException("图片数不足以分配给所有成员");
        }

        // 创建userId到proportion的映射，方便后续查找
        Map<Long, BigDecimal> proportionMap = new HashMap<>();
        for (UserProportionDTO prop : proportions) {
            proportionMap.put(prop.getUserId(), prop.getProportion());
        }

        List<Pair<Long, Double>> exactAllocations = new ArrayList<>();
        int accumulated = 0;

        for (UserProportionDTO prop : proportions) {
            // 使用BigDecimal进行精确计算
            BigDecimal proportionDecimal = prop.getProportion();
            double exact = totalImages * (proportionDecimal.doubleValue() / 100.0);
            int base = (int) Math.floor(exact);
            allocation.put(prop.getUserId(), base);
            accumulated += base;
            exactAllocations.add(ImmutablePair.of(prop.getUserId(), exact - base));
        }

        int remainingAfterFloor = totalImages - accumulated;
        // 排序：优先按小数部分降序，小数相同则按原始比例降序
        exactAllocations.sort((a, b) -> {
            int decimalCompare = Double.compare(b.getRight(), a.getRight());
            if (decimalCompare != 0) {
                return decimalCompare;
            }
            // 小数相同时，按原始比例降序
            BigDecimal propA = proportionMap.get(a.getLeft());
            BigDecimal propB = proportionMap.get(b.getLeft());
            return propB.compareTo(propA);
        });

        for (int i = 0; i < remainingAfterFloor; i++) {
            Long userId = exactAllocations.get(i).getLeft();
            allocation.put(userId, allocation.get(userId) + 1);
        }

        return allocation;
    }

    private Map<Long, Integer> calculateEqualAllocation(int totalImages, List<Long> userIds) {
        int base = totalImages / userIds.size();
        int remainder = totalImages % userIds.size();

        Map<Long, Integer> allocation = new LinkedHashMap<>();
        for (int i = 0; i < userIds.size(); i++) {
            // 给前remainder个用户加1
            allocation.put(userIds.get(i), base + (i < remainder ? 1 : 0));
        }
        return allocation;
    }

    private BigDecimal findProportion(List<UserProportionDTO> proportions, Long userId) {
        return proportions.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .map(UserProportionDTO::getProportion)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Set<Long> ids) {
        if (!CollectionUtil.isEmpty(ids)) {
            ids.forEach(id -> {
                dataTeamTaskMapper.updateById(DataTeamTask.builder()
                        .id(id)
                        .deleted(SwitchEnum.getBooleanValue(SwitchEnum.ON.getValue()))
                        .build());

                // 更新数据集多人标注状态
                DataTeamTask dataTeamTask = dataTeamTaskMapper.selectById(id);
                updateModuleByDatasetId(dataTeamTask.getDatasetId(), DatasetModuleEnum.NO_TEAM.getStatus());

                // subtaskIds
                List<Long> subtaskIds = dataTeamTaskMapper.getDataTeamSubtaskListByDataTeamTaskId(id);
                subtaskIds.forEach(subtaskId -> {
                    dataTeamSubtaskMapper.updateById(DataTeamSubtask.builder()
                            .id(subtaskId)
                            .deleted(SwitchEnum.getBooleanValue(SwitchEnum.ON.getValue()))
                            .build());
                });

            });
        }
    }

    @Override
    public Object queryAllSubtasks(Page page, DataTeamSubtaskQueryDTO criteria) {
        Long curUserId = userContextService.getCurUserId();
        QueryWrapper<DataTeamSubtask> queryWrapper = WrapperHelp.getWrapper(criteria);
        queryWrapper.eq("user_id", curUserId);
        String userName = userClient.getNameById(curUserId);

        // 设置排序规则
        if (StringUtils.isNotEmpty(criteria.getSort()) && StringUtils.isNotEmpty(criteria.getOrder())) {
            queryWrapper.orderBy(
                    true,
                    SORT_ASC.equals(criteria.getOrder().toLowerCase()),
                    StringUtils.humpToLine(criteria.getSort())
            );
        } else {
            queryWrapper.orderByDesc("id");
        }

        Page subtasks = dataTeamSubtaskMapper.listPage(page, queryWrapper);
        // 获取记录列表并设置 userName
        List<DataTeamSubtask> records = subtasks.getRecords();
        records.forEach(record -> record.setUserId(curUserId));
        subtasks.setRecords(records);
        List<DataTeamSubtaskVO> subtaskVOList = dataTeamSubtaskConvert.toDto(records);
        subtaskVOList.forEach(vo -> {
            vo.setUserName(userName);
            if (datasetMapper.selectById(vo.getDatasetId()) != null) {
                vo.setDatasetName(datasetMapper.selectById(vo.getDatasetId()).getName());
            }
            if (vo.getCreateUserId() != null) {
                vo.setCreateUserName(userClient.getNameById(vo.getCreateUserId()));
            }
        });
        return PageUtil.toPage(subtasks, subtaskVOList);
    }

    @Override
    public Integer queryTaskStatus(Long id) {
        return dataTeamTaskMapper.selectById(id).getStatus();
    }

    @Override
    public Integer querySubtaskStatus(Long id) {
        return dataTeamSubtaskMapper.selectById(id).getStatus();
    }

    @Override
    public List<Integer> getFileStatusListBySubtaskId(Long subtaskId) {
        DataTeamSubtask dataTeamSubtask = dataTeamSubtaskMapper.selectById(subtaskId);
        Long startOffset = dataTeamSubtask.getStartOffset();
        Long endOffset = dataTeamSubtask.getEndOffset();
        Long fileNum = endOffset - startOffset + 1;
        Long datasetId = dataTeamSubtask.getDatasetId();
        return datasetVersionFileMapper.findFileStatusListByDatasetAndOffset(datasetId, startOffset, fileNum);
    }

    @Override
    public List<Integer> getSubtaskStatusListByTaskId(Long taskId) {
        return dataTeamMapper.getSubtaskStatusListByTaskId(taskId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveAnnotation(Long taskId, Long subtaskId, Long fileId, Long datasetId, Long expectedStartOffset,
                               Long expectedEndOffset, AnnotationInfoCreateDTO annotationInfoCreateDTO) {

        DataTeamTask dataTeamTask = dataTeamTaskMapper.selectById(taskId);

        DataTeamSubtask dataTeamSubtask = dataTeamSubtaskMapper.selectById(subtaskId);

        if (expectedStartOffset != null || expectedEndOffset != null) {
            boolean rangeChanged = !Objects.equals(expectedStartOffset, dataTeamSubtask.getStartOffset())
                    || !Objects.equals(expectedEndOffset, dataTeamSubtask.getEndOffset());
            if (rangeChanged) {
                throw new BusinessException("任务区间已变更，请刷新页面后重试");
            }
        }

        // 判断当前fieldId的图片是否已标注过
        List<DatasetVersionFile> datasetVersionFiles = datasetVersionFileMapper.selectByDatasetIdAndVersionNameAndFileIds(datasetId, null, Arrays.asList(fileId));
        // 图片状态
        Integer fileStatus = datasetVersionFiles.get(MagicNumConstant.ZERO).getAnnotationStatus();
        // 标注文件保存流程
        annotationService.finishManual(fileId, datasetId, annotationInfoCreateDTO);

        // 当前图片处于未标注状态，再进行更新子任务，任务状态
        if (fileStatus == FileTypeEnum.UNFINISHED.getValue()) {

            // 更新子任务的已标注文件数量
            dataTeamMapper.updateAnnotationCountBySubtaskId(dataTeamSubtask.getId(), MagicNumConstant.ONE_LONG);

            // 子任务执行标注图片事件
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataTeamSubtask});
                setStateMachineType(DataTeamSubtaskStateMachineConstant.DATA_TEAM_SUBTASK_STATE_MACHINE);
                setEventMethodName(DataTeamSubtaskStateMachineConstant.DATA_TEAM_SUBTASK_ANNOTATION_FINISH_EVENT);
            }});

            // 任务执行标注图片事件
            StateMachineUtil.stateChange(new StateChangeDTO() {
                {
                    setObjectParam(new Object[]{dataTeamTask});
                    setStateMachineType(DataTeamTaskStateMachineConstant.DATA_TEAM_TASK_STATE_MACHINE);
                    setEventMethodName(DataTeamTaskStateMachineConstant.DATA_TEAM_TASK_ANNOTATION_FINISH_EVENT);
                }
            });
        }


    }

    @Override
    public void submitSubtask(Long taskId, Long subtaskId) {

        DataTeamSubtask dataTeamSubtask = dataTeamSubtaskMapper.selectById(subtaskId);

        DataTeamTask dataTeamTask = dataTeamTaskMapper.selectById(taskId);

        // 判断是否已完成标注
        if (dataTeamSubtask.getCurrentOffset() - MagicNumConstant.ONE_LONG != dataTeamSubtask.getEndOffset() - dataTeamSubtask.getStartOffset()) {
            return;
        }

        // 子任务执行标注完成事件
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataTeamSubtask});
            setStateMachineType(DataTeamSubtaskStateMachineConstant.DATA_TEAM_SUBTASK_STATE_MACHINE);
            setEventMethodName(DataTeamSubtaskStateMachineConstant.DATA_TEAM_SUBTASK_ANNOTATION_COMPLETE_EVENT);
        }});

        // 任务执行标注完成事件
        StateMachineUtil.stateChange(new StateChangeDTO() {
            {
                setObjectParam(new Object[]{dataTeamTask});
                setStateMachineType(DataTeamTaskStateMachineConstant.DATA_TEAM_TASK_STATE_MACHINE);
                setEventMethodName(DataTeamTaskStateMachineConstant.DATA_TEAM_TASK_ANNOTATION_COMPLETE_EVENT);
            }
        });

    }

    @Override
    public void terminateTask(Long taskId) {

        DataTeamTask dataTeamTask = dataTeamTaskMapper.selectById(taskId);

        List<Long> subTaskIdList = dataTeamTaskMapper.getDataTeamSubtaskListByDataTeamTaskId(taskId);

        // 子任务执行终止事件
        for (Long subtaskId : subTaskIdList) {

            DataTeamSubtask dataTeamSubtask = dataTeamSubtaskMapper.selectById(subtaskId);

            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataTeamSubtask});
                setStateMachineType(DataTeamSubtaskStateMachineConstant.DATA_TEAM_SUBTASK_STATE_MACHINE);
                setEventMethodName(DataTeamSubtaskStateMachineConstant.DATA_TEAM_SUBTASK_ANNOTATION_TERMINATE_EVENT);
            }});

        }

        // 任务执行终止事件
        StateMachineUtil.stateChange(new StateChangeDTO() {
            {
                setObjectParam(new Object[]{dataTeamTask});
                setStateMachineType(DataTeamTaskStateMachineConstant.DATA_TEAM_TASK_STATE_MACHINE);
                setEventMethodName(DataTeamTaskStateMachineConstant.DATA_TEAM_TASK_ANNOTATION_TERMINATE_EVENT);
            }
        });

        // 更新数据集多人标注状态
        updateModuleByDatasetId(dataTeamTask.getDatasetId(), DatasetModuleEnum.NO_TEAM.getStatus());


    }

    @Override
    public void updateModuleByDatasetId(Long datasetId, Integer moduleStatus) {
        datasetMapper.updateModuleById(datasetId, moduleStatus);
    }

    @Override
    public Long queryDatasetIdByTaskId(Long id) {
        return dataTeamTaskMapper.selectById(id).getDatasetId();
    }

    @Override
    public DataTeamTaskSubtaskInfoVO querySubtaskInfo(Long taskId) {
        // task
        DataTeamTask dataTeamTask = dataTeamTaskMapper.selectById(taskId);
        // dataset
        Dataset dataset = datasetMapper.selectById(dataTeamTask.getDatasetId());
        if (dataset == null) {
            return null;
        }
        // team
        DataTeam dataTeam = dataTeamMapper.selectById(dataTeamTask.getTeamId());
        // image count 如果要带版本这里要改
        Integer datasetFileCount = datasetVersionFileMapper.getDatasetVersionAllFileCount(dataset.getId(), null);
        // subtaskList
        List<DataTeamSubtask> subtaskList = dataTeamMapper.getSubtaskListByTaskId(taskId);

        // finished count
        Integer finishedCount = subtaskList.stream().reduce(0, (num, subtask) -> subtask.getCurrentOffset().intValue() + num, (num1, num2) -> num1 + num2);
        // build info
        DataTeamTaskSubtaskInfoVO taskSubtaskInfoVO = DataTeamTaskSubtaskInfoVO.builder().build();
        taskSubtaskInfoVO.setId(dataTeamTask.getId());
        taskSubtaskInfoVO.setName(dataTeamTask.getName());
        taskSubtaskInfoVO.setStatus(dataTeamTask.getStatus());
        taskSubtaskInfoVO.setDatasetId(dataTeamTask.getDatasetId());
        taskSubtaskInfoVO.setDatasetName(dataset.getName());
        taskSubtaskInfoVO.setTeamName(dataTeam.getName());
        taskSubtaskInfoVO.setTeamId(dataTeam.getId());
        taskSubtaskInfoVO.setPersonCount(dataTeam.getMemberNum() != 0 ? datasetFileCount / dataTeam.getMemberNum() : 0);
        taskSubtaskInfoVO.setImageCount(datasetFileCount);
        taskSubtaskInfoVO.setFinishedCount(finishedCount);
        taskSubtaskInfoVO.setMemberNum(dataTeam.getMemberNum());
        // 计算实际任务图片数量（最后一个人endOffset - 第一个人startOffset + 1）
        int taskImageCount = 0;
        if (!subtaskList.isEmpty()) {
            DataTeamSubtask firstSubtask = subtaskList.get(0);
            DataTeamSubtask lastSubtask = subtaskList.get(subtaskList.size() - 1);
            taskImageCount = lastSubtask.getEndOffset().intValue() - firstSubtask.getStartOffset().intValue() + 1;
        }
        taskSubtaskInfoVO.setTaskImageCount(taskImageCount);
        // progress基于实际任务数量计算
        if (taskImageCount == 0) {
            taskSubtaskInfoVO.setProgress(0);
        } else {
            taskSubtaskInfoVO.setProgress((int) Math.floor((double) finishedCount / taskImageCount * 100));
        }
        List<DataTeamSubtaskVO> subtaskVOList = dataTeamSubtaskConvert.toDto(subtaskList);
        for (DataTeamSubtaskVO subtaskVO : subtaskVOList) {
            subtaskVO.setUserName(userClient.getNameById(subtaskVO.getUserId()));
        }
        taskSubtaskInfoVO.setSubtaskVOList(subtaskVOList);
        return taskSubtaskInfoVO;
    }

    @Override
    public DataTeamTaskSubtaskAnnotationStatusVO querySubtaskAnnotationStatus(Long subtaskId) {
        DataTeamSubtask dataTeamSubtask = dataTeamSubtaskMapper.selectById(subtaskId);
        return DataTeamTaskSubtaskAnnotationStatusVO.builder()
                .finishedCount(Math.toIntExact(dataTeamSubtask.getCurrentOffset()))
                .totalCount(Math.toIntExact(dataTeamSubtask.getEndOffset()) - Math.toIntExact(dataTeamSubtask.getStartOffset()) + 1)
                .startOffset(dataTeamSubtask.getStartOffset())
                .endOffset(dataTeamSubtask.getEndOffset())
                .updateTime(dataTeamSubtask.getUpdateTime() == null ? null : dataTeamSubtask.getUpdateTime().getTime())
                .build();
    }

    @Override
    public Boolean validateDatasetPicNum(Long datasetId, Long teamId) {
        if (datasetId == null || teamId == null) {
            log.warn("参数校验失败 - datasetId: {}, teamId: {}", datasetId, teamId);
            return false;
        }

        Integer fileCount = datasetVersionFileMapper.getDatasetVersionAllFileCount(datasetId, null);
        if (fileCount == null) {
            log.warn("文件数量查询异常 - datasetId: {}", datasetId);
            return false;
        }

        DataTeam team = dataTeamMapper.selectById(teamId);
        if (team == null) {
            log.error("团队不存在 - teamId: {}", teamId);
            return false;
        }

        Integer memberNum = team.getMemberNum();
        if (memberNum == null) {
            log.error("团队成员数量为空 - teamId: {}", teamId);
            return false;
        }

        boolean isValid = fileCount > memberNum;
        log.debug("验证结果: {} > {} = {}", fileCount, memberNum, isValid);
        return isValid;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteSubTask(Long subTaskId) {
        // 获取要删除的子任务
        DataTeamSubtask subTask = dataTeamSubtaskMapper.selectById(subTaskId);

        // 标记删除当前子任务
        dataTeamSubtaskMapper.updateById(DataTeamSubtask.builder()
                .id(subTaskId)
                .deleted(true)
                .build());

        // 解除任务与子任务的关联
        dataTeamTaskMapper.untieDataTaskSubtask(subTask.getTaskId(), subTaskId);

        // 获取任务信息
        DataTeamTask task = dataTeamTaskMapper.selectById(subTask.getTaskId());

        // 获取剩余有效子任务列表
        List<DataTeamSubtask> validSubtasks = dataTeamSubtaskMapper.selectList(
                new QueryWrapper<DataTeamSubtask>()
                        .eq("task_id", task.getId())
                        .eq("deleted", false));

        // 重新分配图片逻辑
        Integer memberNum = validSubtasks.size();
        if (memberNum == 0) {
            //无剩余子任务，终止任务
            terminateTask(task.getId());
            return;
        };

        Integer totalFiles = datasetVersionFileMapper.getDatasetVersionAllFileCount(task.getDatasetId(), null);
        int filesPerMember = totalFiles / memberNum;

        // 按顺序更新剩余子任务的offset
        for (int i = 0; i < memberNum; i++) {
            DataTeamSubtask subtask = validSubtasks.get(i);

            // 保存原有的偏移量和状态
            Long oldStartOffset = subtask.getStartOffset();
            Long oldEndOffset = subtask.getEndOffset();
            Integer oldStatus = subtask.getStatus();

            // 计算新的偏移量
            long newStart = (long) i * filesPerMember;
            long newEnd = (i == memberNum - 1) ? totalFiles - 1 : newStart + filesPerMember - 1;

            // 判断偏移量是否发生改变
            boolean offsetChanged = !oldStartOffset.equals(newStart) || !oldEndOffset.equals(newEnd);

            Integer newStatus;
            Long newCurrentOffset;

            if (!offsetChanged) {
                // 偏移量未改变，状态保持不变
                newStatus = oldStatus;
                newCurrentOffset = subtask.getCurrentOffset();
            } else {
                // 偏移量发生改变，查询新区域内已标注图片数量
                Long annotatedCount = datasetVersionFileMapper.countAnnotatedFilesByOffset(
                    task.getDatasetId(), null, newStart, newEnd - newStart + 1).longValue();
                newCurrentOffset = annotatedCount;

                if (oldStatus.equals(DataTeamTaskStatusEnum.FINISHED.getValue())) {
                    // 之前是已提交状态
                    long totalInNewRange = newEnd - newStart + 1;
                    if (annotatedCount.equals(totalInNewRange)) {
                        // 新区域全部已标注，保持已提交状态
                        newStatus = DataTeamTaskStatusEnum.FINISHED.getValue();
                    } else {
                        // 新区域存在未标注图片，置为未提交状态
                        newStatus = annotatedCount > 0 ?
                            DataTeamTaskStatusEnum.ANNOTATING.getValue() :
                            DataTeamTaskStatusEnum.INIT.getValue();
                    }
                } else {
                    // 之前是未提交状态（INIT或ANNOTATING），根据新区域标注情况决定
                    if (annotatedCount == 0) {
                        newStatus = DataTeamTaskStatusEnum.INIT.getValue();
                    } else {
                        newStatus = DataTeamTaskStatusEnum.ANNOTATING.getValue();
                    }
                }
            }

            dataTeamSubtaskMapper.updateById(DataTeamSubtask.builder()
                    .id(subtask.getId())
                    .startOffset(newStart)
                    .currentOffset(newCurrentOffset)
                    .endOffset(newEnd)
                    .status(newStatus)
                    .build());
        }
    }



    @Transactional(rollbackFor = Exception.class)
    @Override
    public void removeUserFromAllTeams(Long userId) {

        List<Long> teamIds = dataTeamMapper.selectTeamIdsByUserId(userId);
        if (teamIds.isEmpty()) return;

        //把被删的user移出标注团队
        dataTeamMapper.deleteUserFromAllTeams(userId);

        // 3. 批量更新团队成员数量
        teamIds.forEach(teamId -> {
            DataTeam team = dataTeamMapper.selectById(teamId);
            if (team != null) {
                int newCount = Math.max(team.getMemberNum() - 1, 0);
                dataTeamMapper.updateById(DataTeam.builder()
                        .id(teamId)
                        .memberNum(newCount)
                        .build());
            }
        });
    }


    //用户删除后的后处理
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void handleUserRemoval(Long userId) {
        //移除用户所有标注团队关联
        removeUserFromAllTeams(userId);

        //查询需要处理的子任务,主任务没结束的
        List<DataTeamSubtask> targetSubtasks = dataTeamSubtaskMapper.selectActiveSubtasksByUser(
                userId,
                Arrays.asList(
                        DataTeamTaskStatusEnum.FINISHED.getValue(),
                        DataTeamTaskStatusEnum.TERMINATED.getValue()
                )
        );

        // 3. 处理每个子任务
        targetSubtasks.forEach(subtask -> {
            // 删除子任务（会触发重新分配）
            deleteSubTask(subtask.getId());
        });

    }

    @Override
    public DataTeamVO getTeamInfoById(Long id) {
        if (id == null) {
            return null;
        }
        DataTeam dataTeam = dataTeamMapper.selectById(id);
        if (dataTeam == null) {
            return null;
        }
        // 获取团队成员
        List<Long> userIds = dataTeamMapper.getUserListByDataTeamId(id);
        List<UserDTO> users = adminClient.getUserList(userIds).getData();
        DataTeamVO teamVO = dataTeamConvert.toDto(dataTeam);
        teamVO.setUsers(users);
        teamVO.setUserIds(userIds);
        return teamVO;
    }

    /**
     * 检查子任务是否所有图片都已标注
     * @param subtaskId 子任务ID
     * @return 包含检查结果的Map
     */
    @Override
    public Map<String, Object> checkAllAnnotated(Long subtaskId) {
        Map<String, Object> result = new HashMap<>();
        
        // 获取子任务信息
        DataTeamSubtask subtask = dataTeamSubtaskMapper.selectById(subtaskId);
        if (subtask == null) {
            throw new BusinessException("子任务不存在");
        }
        
        // 获取任务信息
        DataTeamTask task = dataTeamTaskMapper.selectById(subtask.getTaskId());
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        
        Long datasetId = task.getDatasetId();
        Long startOffset = subtask.getStartOffset();
        Long endOffset = subtask.getEndOffset();
        
        // 计算总图片数
        long totalCount = endOffset - startOffset + 1;
        
        // 获取数据集信息
        Dataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new BusinessException("数据集不存在");
        }
        
        // 查询该范围内的所有文件（使用offset分页方式）
        // 注意：这里的offset是SQL的LIMIT offset，表示跳过前面多少条记录
        List<DatasetVersionFileDTO> allFiles = datasetVersionFileService
                .getListByDatasetIdAndAnnotationStatus(
                        datasetId,
                        // 理论上来说应该为null，但是sql支持
                        dataset.getCurrentVersionName(),
                        new Integer[]{101, 104},
                        startOffset,
                        (int) totalCount,
                        "id",
                        "asc",
                        null
                );
        
        // 统计未标注的图片数量（annotation_status = 101）
        long unannotatedCount = allFiles.stream()
                .filter(file -> file.getAnnotationStatus() != null && file.getAnnotationStatus() == 101)
                .count();
        
        result.put("allAnnotated", unannotatedCount == 0);
        result.put("totalCount", totalCount);
        result.put("unannotatedCount", unannotatedCount);
        result.put("annotatedCount", totalCount - unannotatedCount);
        
        return result;
    }

    /**
     * 一键确认（批量提交子任务）
     * 将currentOffset设置为endOffset，表示所有图片都已确认
     * @param taskId 任务ID
     * @param subtaskId 子任务ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchConfirm(Long taskId, Long subtaskId) {
        // 获取子任务信息
        DataTeamSubtask subtask = dataTeamSubtaskMapper.selectById(subtaskId);
        if (subtask == null) {
            throw new BusinessException("子任务不存在");
        }
        
        // 验证任务ID是否匹配
        if (!subtask.getTaskId().equals(taskId)) {
            throw new BusinessException("任务ID与子任务不匹配");
        }
        
        // 检查是否所有图片都已标注
        Map<String, Object> checkResult = checkAllAnnotated(subtaskId);
        Boolean allAnnotated = (Boolean) checkResult.get("allAnnotated");
        
        if (!allAnnotated) {
            Long unannotatedCount = (Long) checkResult.get("unannotatedCount");
            throw new BusinessException("还有 " + unannotatedCount + " 张图片未标注，无法进行一键确认");
        }
        
        // 将currentOffset设置为endOffset
        subtask.setCurrentOffset(subtask.getEndOffset() - subtask.getStartOffset() + 1);
        dataTeamSubtaskMapper.updateById(subtask);
        
        log.info("一键确认成功，子任务ID: {}, currentOffset已设置为: {}", subtaskId, subtask.getEndOffset());
    }


}

