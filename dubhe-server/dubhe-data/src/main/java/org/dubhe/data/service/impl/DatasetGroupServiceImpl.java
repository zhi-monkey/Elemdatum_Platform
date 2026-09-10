package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.dto.GroupInfoDTO;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.base.vo.DatasetVO;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.DatasetModuleEnum;
import org.dubhe.data.dao.DatasetDatasetGroupMapper;
import org.dubhe.data.dao.DatasetGroupMapper;
import org.dubhe.data.dao.DatasetMapper;
import org.dubhe.data.dao.DatasetVersionFileMapper;
import org.dubhe.data.dao.DatasetVersionMapper;
import org.dubhe.data.dao.PcDatasetDatasetGroupMapper;
import org.dubhe.data.dao.PcDatasetMapper;
import org.dubhe.data.dao.MultiDatasetDatasetGroupMapper;
import org.dubhe.data.dao.MultiDatasetMapper;
import org.dubhe.data.dao.VideoDatasetDatasetGroupMapper;
import org.dubhe.data.dao.VideoDatasetMapper;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.DatasetDatasetGroup;
import org.dubhe.data.domain.entity.DatasetGroup;
import org.dubhe.data.domain.entity.DatasetVersion;
import org.dubhe.data.domain.entity.PcDataset;
import org.dubhe.data.domain.entity.PcDatasetDatasetGroup;
import org.dubhe.data.domain.entity.MultiDataset;
import org.dubhe.data.domain.entity.MultiDatasetDatasetGroup;
import org.dubhe.data.domain.entity.VideoDataset;
import org.dubhe.data.domain.entity.VideoDatasetDatasetGroup;
import org.dubhe.data.domain.vo.DatasetGroupVO;
import org.dubhe.data.machine.constant.DataStateCodeConstant;
import org.dubhe.data.service.DatasetGroupService;
import org.dubhe.data.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mingming
 * @date 2025/09/05
 */
@Service
public class DatasetGroupServiceImpl extends ServiceImpl<DatasetGroupMapper, DatasetGroup> implements DatasetGroupService {
    private static final Logger log = LoggerFactory.getLogger(DatasetGroupServiceImpl.class);

    private final DatasetGroupMapper datasetGroupMapper;
    private final DatasetDatasetGroupMapper datasetDatasetGroupMapper;
    private final DatasetServiceImpl datasetServiceImpl;
    private final UserContextService userContextService;
    private final DatasetDatasetGroupServiceImpl datasetDatasetGroupServiceImpl;
    private final DatasetVersionServiceImpl datasetVersionServiceImpl;
    private final DatasetVersionFileServiceImpl datasetVersionFileServiceImpl;
    private final DatasetVersionMapper datasetVersionMapper;
    private final DatasetMapper datasetMapper;
    private final DatasetVersionFileMapper datasetVersionFileMapper;
    private final PcDatasetMapper pcDatasetMapper;
    private final PcDatasetDatasetGroupMapper pcDatasetDatasetGroupMapper;
    private final VideoDatasetMapper videoDatasetMapper;
    private final VideoDatasetDatasetGroupMapper videoDatasetDatasetGroupMapper;
    private final MultiDatasetMapper multiDatasetMapper;
    private final MultiDatasetDatasetGroupMapper multiDatasetDatasetGroupMapper;

    public DatasetGroupServiceImpl(DatasetGroupMapper datasetGroupMapper, DatasetDatasetGroupMapper datasetDatasetGroupMapper, @Lazy DatasetServiceImpl datasetServiceImpl, UserContextService userContextService, DatasetDatasetGroupServiceImpl datasetDatasetGroupServiceImpl, @Lazy DatasetVersionServiceImpl datasetVersionServiceImpl, DatasetVersionFileServiceImpl datasetVersionFileServiceImpl, DatasetVersionMapper datasetVersionMapper, DatasetMapper datasetMapper, DatasetVersionFileMapper datasetVersionFileMapper, PcDatasetMapper pcDatasetMapper, PcDatasetDatasetGroupMapper pcDatasetDatasetGroupMapper, VideoDatasetMapper videoDatasetMapper, VideoDatasetDatasetGroupMapper videoDatasetDatasetGroupMapper, MultiDatasetMapper multiDatasetMapper, MultiDatasetDatasetGroupMapper multiDatasetDatasetGroupMapper) {
        this.datasetGroupMapper = datasetGroupMapper;
        this.datasetDatasetGroupMapper = datasetDatasetGroupMapper;
        this.datasetServiceImpl = datasetServiceImpl;
        this.userContextService = userContextService;
        this.datasetDatasetGroupServiceImpl = datasetDatasetGroupServiceImpl;
        this.datasetVersionServiceImpl = datasetVersionServiceImpl;
        this.datasetVersionFileServiceImpl = datasetVersionFileServiceImpl;
        this.datasetVersionMapper = datasetVersionMapper;
        this.datasetMapper = datasetMapper;
        this.datasetVersionFileMapper = datasetVersionFileMapper;
        this.pcDatasetMapper = pcDatasetMapper;
        this.pcDatasetDatasetGroupMapper = pcDatasetDatasetGroupMapper;
        this.videoDatasetMapper = videoDatasetMapper;
        this.videoDatasetDatasetGroupMapper = videoDatasetDatasetGroupMapper;
        this.multiDatasetMapper = multiDatasetMapper;
        this.multiDatasetDatasetGroupMapper = multiDatasetDatasetGroupMapper;
    }

    @Override
    public List<DatasetGroup> getAllDatasetGroup() {
        Long userId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();

        QueryWrapper<DatasetGroup> wrapper = new QueryWrapper<>();

        // 如果不是管理员，添加用户ID过滤条件
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            wrapper.eq("create_user_id", userId);
        }

        return datasetGroupMapper.selectList(wrapper);
    }

    @Override
    public IPage<DatasetVO> getDatasetsPageByDatasetGroupId(Long datasetGroupId, Page<DatasetDatasetGroup> page) {
        Long userId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();
        boolean isAdmin = "管理员".equals(roleName) || "管理人员".equals(roleName);

        QueryWrapper<DatasetDatasetGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", datasetGroupId);
        // 添加按 dataset_id 降序排序，确保分页顺序正确
        wrapper.orderByDesc("dataset_id");

        List<DatasetDatasetGroup> relations = datasetDatasetGroupMapper.selectList(wrapper);
        List<Long> allDatasetIds = relations.stream()
                .map(DatasetDatasetGroup::getDatasetId)
                .collect(Collectors.toList());
        Map<Long, DatasetVO> allDatasetVOMap = allDatasetIds.isEmpty()
                ? new HashMap<>() : datasetServiceImpl.batchGet(allDatasetIds);
        if (allDatasetVOMap == null) {
            allDatasetVOMap = new HashMap<>();
        }
        if (!isAdmin) {
            allDatasetVOMap.entrySet().removeIf(entry -> !Objects.equals(userId, entry.getValue().getCreateUserId()));
        }

        List<PcDatasetDatasetGroup> pcRelations = pcDatasetDatasetGroupMapper.selectList(
                new QueryWrapper<PcDatasetDatasetGroup>().eq("dataset_group_id", datasetGroupId));
        List<Long> pcDatasetIds = pcRelations.stream()
                .map(PcDatasetDatasetGroup::getPcDatasetId)
                .collect(Collectors.toList());
        List<PcDataset> pcDatasets = pcDatasetIds.isEmpty()
                ? Collections.emptyList() : pcDatasetMapper.selectBatchIds(pcDatasetIds);

        List<VideoDatasetDatasetGroup> videoRelations = videoDatasetDatasetGroupMapper.selectList(
                new QueryWrapper<VideoDatasetDatasetGroup>().eq("dataset_group_id", datasetGroupId));
        List<Long> videoDatasetIds = videoRelations.stream()
                .map(VideoDatasetDatasetGroup::getVideoDatasetId)
                .collect(Collectors.toList());
        List<VideoDataset> videoDatasets = videoDatasetIds.isEmpty()
                ? Collections.emptyList() : videoDatasetMapper.selectBatchIds(videoDatasetIds);

        List<MultiDatasetDatasetGroup> multiRelations = multiDatasetDatasetGroupMapper.selectList(
                new QueryWrapper<MultiDatasetDatasetGroup>().eq("dataset_group_id", datasetGroupId));
        List<Long> multiDatasetIds = multiRelations.stream()
                .map(MultiDatasetDatasetGroup::getMultiDatasetId)
                .collect(Collectors.toList());
        List<MultiDataset> multiDatasets = multiDatasetIds.isEmpty()
                ? Collections.emptyList() : multiDatasetMapper.selectBatchIds(multiDatasetIds);

        List<DatasetVO> allRecords = new ArrayList<>(allDatasetVOMap.values());
        for (PcDataset pcDataset : pcDatasets) {
            if (isAdmin || Objects.equals(userId, pcDataset.getCreateUserId())) {
                allRecords.add(toDatasetVO(pcDataset));
            }
        }
        for (VideoDataset videoDataset : videoDatasets) {
            if (isAdmin || Objects.equals(userId, videoDataset.getCreateUserId())) {
                allRecords.add(toDatasetVO(videoDataset));
            }
        }
        for (MultiDataset multiDataset : multiDatasets) {
            if (isAdmin || Objects.equals(userId, multiDataset.getCreateUserId())) {
                allRecords.add(toDatasetVO(multiDataset));
            }
        }
        allRecords.sort(Comparator.comparing(DatasetVO::getCreateTime,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        long validTotal = allRecords.size();

        int pageSize = (int) page.getSize();
        int current = (int) page.getCurrent();
        int fromIndex = Math.max(0, (current - 1) * pageSize);
        int toIndex = Math.min(allRecords.size(), fromIndex + pageSize);
        List<DatasetVO> recordsOnPage = fromIndex < toIndex
                ? new ArrayList<>(allRecords.subList(fromIndex, toIndex)) : Collections.emptyList();
        
        List<DatasetVO> commonRecords = recordsOnPage.stream()
                .filter(datasetVO -> !Integer.valueOf(5).equals(datasetVO.getDataType())
                        && !Integer.valueOf(6).equals(datasetVO.getDataType())
                        && !Integer.valueOf(7).equals(datasetVO.getDataType()))
                .collect(Collectors.toList());
        Map<Long, List<GroupInfoDTO>> datasetGroupsMap = getDatasetGroupsMap(commonRecords);

        // 批量准备数据集信息用于查询 changed 文件计数
        List<DatasetVersionFileMapper.DatasetChangedInfo> datasetInfos = new ArrayList<>();
        for (DatasetVO datasetVO : commonRecords) {
            if (datasetVO.getCurrentVersionName() != null) {
                datasetInfos.add(new DatasetVersionFileMapper.DatasetChangedInfo(
                        datasetVO.getId(),
                        datasetVO.getCurrentVersionName()
                ));
            }
        }
        Map<Long, Integer> changedFilesCountMap = datasetVersionFileServiceImpl.batchCountChangedFiles(datasetInfos);

        // 设置 VO 信息
        for (DatasetVO datasetVO : commonRecords) {
            datasetVO.setGroups(datasetGroupsMap.getOrDefault(datasetVO.getId(), Collections.emptyList()));
            if (datasetVO.getCurrentVersionName() != null) {
                DatasetVersion datasetVersion = datasetVersionServiceImpl
                        .getVersionByDatasetIdAndVersionName(datasetVO.getId(), datasetVO.getCurrentVersionName());
                if (datasetVersion != null) {
                    datasetVO.setDataConversion(datasetVersion.getDataConversion());
                }
            }

            // 计算 hasChanges 标志
            boolean hasChanges = false;

            // 情况1: currentVersionName 为 null 且状态是已标注
            if (datasetVO.getCurrentVersionName() == null && datasetVO.getStatus() != null
                    && datasetVO.getStatus().equals(DataStateCodeConstant.ANNOTATION_COMPLETE_STATE)) {
                hasChanges = true;
            }
            
            // 情况2: 当前版本有文件的 changed = 1（使用批量查询结果）
            if (!hasChanges && datasetVO.getCurrentVersionName() != null) {
                // 批量查询所有有版本的数据集的 changed 文件计数
                Integer count = changedFilesCountMap.get(datasetVO.getId());
                hasChanges = (count != null && count > 0);
            }

            datasetVO.setHasChanges(hasChanges);
        }

        IPage<DatasetVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), validTotal);
        resultPage.setRecords(recordsOnPage);

        return resultPage;
    }

    private DatasetVO toDatasetVO(PcDataset pcDataset) {
        DatasetVO datasetVO = new DatasetVO();
        datasetVO.setId(pcDataset.getId());
        datasetVO.setDatasetCode(FileUtil.getDatasetCode(pcDataset.getId(), 5));
        datasetVO.setName(pcDataset.getName());
        datasetVO.setRemark(pcDataset.getRemark());
        datasetVO.setUri(pcDataset.getStoragePrefix());
        datasetVO.setDataType(5);
        datasetVO.setStatus(pcDataset.getStatus());
        datasetVO.setLabelGroupId(pcDataset.getLabelGroupId());
        datasetVO.setFileCount(pcDataset.getFileCount() == null ? 0 : Math.toIntExact(pcDataset.getFileCount()));
        datasetVO.setCreateTime(pcDataset.getCreateTime());
        datasetVO.setUpdateTime(pcDataset.getUpdateTime());
        datasetVO.setCreateUserId(pcDataset.getCreateUserId());
        datasetVO.setType(0);
        datasetVO.setIsGuided(false);
        return datasetVO;
    }

    private DatasetVO toDatasetVO(VideoDataset videoDataset) {
        DatasetVO datasetVO = new DatasetVO();
        datasetVO.setId(videoDataset.getId());
        datasetVO.setDatasetCode(FileUtil.getDatasetCode(videoDataset.getId(), 6));
        datasetVO.setName(videoDataset.getName());
        datasetVO.setRemark(videoDataset.getRemark());
        datasetVO.setUri(videoDataset.getStoragePrefix());
        datasetVO.setDataType(6);
        datasetVO.setStatus(videoDataset.getStatus());
        datasetVO.setLabelGroupId(videoDataset.getLabelGroupId());
        datasetVO.setFileCount(videoDataset.getFileCount() == null ? 0 : Math.toIntExact(videoDataset.getFileCount()));
        datasetVO.setCreateTime(videoDataset.getCreateTime());
        datasetVO.setUpdateTime(videoDataset.getUpdateTime());
        datasetVO.setCreateUserId(videoDataset.getCreateUserId());
        datasetVO.setType(0);
        datasetVO.setIsGuided(false);
        return datasetVO;
    }

    private DatasetVO toDatasetVO(MultiDataset multiDataset) {
        DatasetVO datasetVO = new DatasetVO();
        datasetVO.setId(multiDataset.getId());
        datasetVO.setDatasetCode(FileUtil.getDatasetCode(multiDataset.getId(), 7));
        datasetVO.setName(multiDataset.getName());
        datasetVO.setRemark(multiDataset.getRemark());
        datasetVO.setUri(multiDataset.getStoragePrefix());
        datasetVO.setDataType(7);
        datasetVO.setStatus(multiDataset.getStatus());
        datasetVO.setLabelGroupId(multiDataset.getLabelGroupId());
        datasetVO.setFileCount(0);
        datasetVO.setCreateTime(multiDataset.getCreateTime());
        datasetVO.setUpdateTime(multiDataset.getUpdateTime());
        datasetVO.setCreateUserId(multiDataset.getCreateUserId());
        datasetVO.setType(0);
        datasetVO.setIsGuided(false);
        return datasetVO;
    }

    /**
     * 批量获取并构建 DatasetId -> List<GroupInfoDTO> 的映射
     * @param datasets 当前页的数据集列表
     * @return Map<数据集ID, List<数据集组DTO>>
     */
    private Map<Long, List<GroupInfoDTO>> getDatasetGroupsMap(List<DatasetVO> datasets) {
        // 提取当前页所有数据集的ID
        List<Long> datasetIds = datasets.stream().map(DatasetVO::getId).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(datasetIds)) {
            return Collections.emptyMap();
        }

        // 1. 一次性查询出所有相关的关联关系
        List<DatasetDatasetGroup> relations = datasetDatasetGroupServiceImpl.list(
                new LambdaQueryWrapper<DatasetDatasetGroup>().in(DatasetDatasetGroup::getDatasetId, datasetIds)
        );

        if (CollectionUtils.isEmpty(relations)) {
            return Collections.emptyMap();
        }

        // 2. 提取所有需要的 Group ID，并去重
        List<Long> groupIds = relations.stream()
                .map(DatasetDatasetGroup::getDatasetGroupId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 一次性查询出所有 Group 的详细信息
        Map<Long, DatasetGroup> groupDetailsMap = listByIds(groupIds).stream()
                .collect(Collectors.toMap(DatasetGroup::getId, group -> group));

        // 4. 在内存中组装最终的 Map<DatasetId, List<GroupInfoDTO>>
        Map<Long, List<GroupInfoDTO>> resultMap = new HashMap<>();
        for (DatasetDatasetGroup relation : relations) {
            DatasetGroup group = groupDetailsMap.get(relation.getDatasetGroupId());
            if (group != null) {
                GroupInfoDTO dto = new GroupInfoDTO();
                dto.setId(group.getId());
                dto.setName(group.getName());
                dto.setIsPublic(group.getIsPublic());

                // a. computeIfAbsent 确保 key 存在，b. get(key)然后添加 DTO
                resultMap.computeIfAbsent(relation.getDatasetId(), k -> new ArrayList<>()).add(dto);
            }
        }
        return resultMap;
    }

    @Override
    public Map<String, Object> getAllDatasetGroupPage(Page<DatasetGroup> page, String name) {
        return getAllDatasetGroupPage(page, name, null);
    }

    public Map<String, Object> getAllDatasetGroupPage(Page<DatasetGroup> page, String name, Boolean isGuided) {
        Long userId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();
        Long userIdParam = null;
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            userIdParam = userId;
        }

        // 如果指定了 isGuided 筛选，使用 SQL 层面的筛选方法
        IPage<DatasetGroup> datasetGroupPage;
        if (isGuided != null) {
            datasetGroupPage = datasetGroupMapper.getAllDatasetGroupPageWithFilter(page, userIdParam, name, isGuided);
        } else {
            // 没有筛选时，使用原来的 QueryWrapper 方式
            QueryWrapper<DatasetGroup> datasetQueryWrapper = new QueryWrapper<>();
            if (userIdParam != null) {
                datasetQueryWrapper.eq("create_user_id", userIdParam);
            }
            datasetQueryWrapper.like(StringUtils.isNotBlank(name), "name", name);
            datasetQueryWrapper.orderByDesc("creation_time");
            datasetGroupPage = datasetGroupMapper.selectPage(page, datasetQueryWrapper);
        }

        List<DatasetGroup> records = datasetGroupPage.getRecords();

        final Long userIdFinal = userIdParam;
        List<DatasetGroupVO> datasetGroupVOs = records.stream().map(datasetGroup -> {
            LocalDateTime creationTimeWithOffset = datasetGroup.getCreationTime() != null
                    ? datasetGroup.getCreationTime().plusHours(8)
                    : null;
            LocalDateTime updateTimeWithOffset = datasetGroup.getUpdateTime() != null
                    ? datasetGroup.getUpdateTime().plusHours(8)
                    : null;

            // 拿到数据集组内所有数据集ids
            List<Long> datasetIds = null;
            if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                datasetIds = datasetDatasetGroupMapper.getGroupDatasetIdsByUserId(datasetGroup.getId(), userId);
            } else {
                datasetIds = datasetDatasetGroupMapper.getDatasetsByDatasetGroupId(datasetGroup.getId());
            }
            Integer versionCount = 0;
            Integer labelCount = 0;
            Boolean groupIsGuided = false;
            Map<String, Date> latestDatasetNamesMap = Collections.emptyMap();
            // 只有当数据集组中确实有数据集时才进行
            if (datasetIds != null || !datasetIds.isEmpty()) {
                try {
                    Map<String, Object> statsMap = datasetDatasetGroupMapper.getDatasetGroupStat(datasetGroup.getId(), userIdFinal);

                    // 获取版本数量，标签数量值
                    versionCount = ((Number) statsMap.get("version_count")).intValue();
                    labelCount = ((Number) statsMap.get("label_count")).intValue();
                    // 计算是否是引导式：组内只要有至少一个引导式数据集就是引导式
                    groupIsGuided = Boolean.TRUE.equals(datasetServiceImpl.existsGuidedDataset(datasetIds));
                    // 通过Feign收集Map<数据集名称, 创建时间>
                    latestDatasetNamesMap = datasetServiceImpl.getDatasetNameTimeMap(
                            Collections.singletonList(datasetIds.get(0))
                    );

                } catch (Exception e) {
                    log.warn("获取数据集组{}统计信息时出错: {}", datasetGroup.getId(), e.getMessage());
                    // 出错时保持默认值0
                }
            }

            return DatasetGroupVO.builder()
                    .id(datasetGroup.getId())
                    .name(datasetGroup.getName())
                    .description(datasetGroup.getDescription())
                    .isPublic(datasetGroup.getIsPublic())
                    .creationTime(creationTimeWithOffset)
                    .updateTime(updateTimeWithOffset)
                    .datasetCount(datasetIds.size())
                    .versionCount(versionCount)
                    .labelCount(labelCount)
                    .latestDatasets(latestDatasetNamesMap)
                    .totalFiles(calculateTotalFilesForGroup(datasetGroup.getId())) // 修复后的方法
                    .isGuided(groupIsGuided)
                    .build();
        }).collect(Collectors.toList());
        
        return PageUtil.toPage(datasetGroupPage, datasetGroupVOs);
    }

    public Integer countGroupDatasetCount(DatasetGroup datasetGroup){
        return datasetDatasetGroupMapper.countGroupDatasetCount(datasetGroup.getId());
    }

    public List<Long> getGroupLatestDatasetIds(Long datasetGroupId) {
        return datasetDatasetGroupMapper.getGroupLatestDatasetIds(datasetGroupId);
    }

    /**
     * 分页查询数据集分组
     * @param page 分页参数
     * @return 分页结果
     */
    public IPage<DatasetGroup> getDatasetGroupByPage(Page<DatasetGroup> page) {
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();

        if (("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            // 管理员查看所有数据集分组
            return datasetGroupMapper.getAllDatasetGroupByPage(page);
        } else {
            // 普通用户只查看自己创建的数据集分组
            Long userId = userContextService.getCurUser().getId();
            return datasetGroupMapper.getPersonalDatasetGroupByPage(page, userId);
        }
    }

    /**
     * 分页查询数据集分组
     * @param page 分页参数
     * @return 分页结果
     */
    public IPage<DatasetGroup> getPrivateAndPublicDatasetGroupByPage(Page<DatasetGroup> page) {
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();

        if (("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            // 管理员查看所有数据集分组
            return datasetGroupMapper.getAllDatasetGroupByPage(page);
        } else {
            // 普通用户只查看自己创建的数据集分组
            Long userId = userContextService.getCurUser().getId();
            return datasetGroupMapper.getPrivateAndPublicDatasetGroupByPage(page, userId);
        }
    }

    /**
     * 按名称分页搜索数据集分组
     * @param page 分页参数
     * @param name 搜索关键词
     * @return 分页结果
     */
    public IPage<DatasetGroup> searchDatasetGroupByName(Page<DatasetGroup> page, String name) {
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();

        if (("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            // 管理员搜索所有数据集分组
            return datasetGroupMapper.searchAllDatasetGroupByName(page, name);
        } else {
            // 普通用户只搜索自己创建的数据集分组
            Long userId = userContextService.getCurUser().getId();
            return datasetGroupMapper.searchPersonalDatasetGroupByName(page, userId, name);
        }
    }


    /**
     * 按名称分页搜索数据集分组
     * @param page 分页参数
     * @param name 搜索关键词
     * @return 分页结果
     */
    public IPage<DatasetGroup> searchPrivateAndPublicDatasetGroupByName(Page<DatasetGroup> page, String name) {
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();

        if (("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            // 管理员搜索所有数据集分组
            return datasetGroupMapper.searchAllDatasetGroupByName(page, name);
        } else {
            // 普通用户只搜索自己创建的数据集分组
            Long userId = userContextService.getCurUser().getId();
            return datasetGroupMapper.searchPrivateAndPublicDatasetGroupByName(page, userId, name);
        }
    }



    @Override
    public DatasetGroup create(DatasetGroup datasetGroup) {
        String datasetGroupName = datasetGroup.getName().trim();
        if (datasetGroupMapper.existCountByName(datasetGroupName) > 0) {
            throw new BusinessException("数据集组名称已存在");
        }
        datasetGroup.setCreateUserId(userContextService.getCurUser().getId());
        datasetGroup.setName(datasetGroupName);
        datasetGroup.setDescription(datasetGroup.getDescription() == null ? "" : datasetGroup.getDescription().trim());
        datasetGroupMapper.insert(datasetGroup);
        return datasetGroup;
    }

    @Override
    public List<DatasetVO> getDatasetsByDatasetGroupId(Long datasetGroupId) {
        Long currentUserId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();
        
        List<Long> datasetIds;
        datasetIds = datasetDatasetGroupMapper.getDatasetsByDatasetGroupId(datasetGroupId);
        Collection<DatasetVO> values = datasetServiceImpl.batchGet(datasetIds).values();
        
        // 如果是管理员或管理人员，不需要过滤，直接返回所有数据集
        if ("管理员".equals(roleName) || "管理人员".equals(roleName)) {
            return new ArrayList<>(values);
        }
        
        // 过滤数据集：只保留公开的数据集或自己创建的私有数据集
        List<DatasetVO> filteredDatasets = values.stream()
                .filter(datasetVO -> {
                    // 公开数据集：直接返回
                    if (datasetVO.getIsPublic() == 1) {
                        return true;
                    }
                    // 私有数据集：只有自己是创建者才返回
                    return datasetVO.getIsPublic() == 0 
                            && datasetVO.getCreateUserId() != null 
                            && datasetVO.getCreateUserId().equals(currentUserId);
                })
                .collect(Collectors.toList());
        
        return filteredDatasets;
    }

    @Override
    public List<DatasetVO> getDatasetsByDatasetGroupIdAndAnnotateType(Long datasetGroupId, Integer annotateType) {
        // 根据annotateType过滤
        List<Long> datasetIds = datasetDatasetGroupMapper.getDatasetsByDatasetGroupIdAndAnnotateType(datasetGroupId, annotateType);
        if (datasetIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量获取数据集
        Collection<DatasetVO> values = datasetServiceImpl.batchGet(datasetIds).values();
        return new ArrayList<>(values);
    }

    @Override
    public List<DatasetVO> getPublicDatasetsByDatasetGroupIdAndAnnotateType(Long datasetGroupId, Integer annotateType) {
        // 根据annotateType和isPublic过滤
        List<Long> datasetIds = datasetDatasetGroupMapper.getPublicDatasetsByDatasetGroupIdAndAnnotateType(datasetGroupId, annotateType);
        if (datasetIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量获取数据集
        Collection<DatasetVO> values = datasetServiceImpl.batchGet(datasetIds).values();
        return new ArrayList<>(values);
    }

    @Override
    public IPage<DatasetGroup> searchPublicDatasetGroupByName(Page<DatasetGroup> page, String name) {
        QueryWrapper<DatasetGroup> datasetQueryWrapper =  new QueryWrapper<>();
        datasetQueryWrapper.eq("is_public", true);
        datasetQueryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        datasetQueryWrapper.orderByDesc("creation_time");
        return datasetGroupMapper.selectPage(page, datasetQueryWrapper);
    }

    @Override
    public IPage<DatasetGroup> getPublicDatasetGroupByPage(Page<DatasetGroup> page) {
        QueryWrapper<DatasetGroup> datasetQueryWrapper =  new QueryWrapper<>();
        datasetQueryWrapper.eq("is_public", true);
        datasetQueryWrapper.orderByDesc("creation_time");
        return datasetGroupMapper.selectPage(page, datasetQueryWrapper);
    }

    @Override
    public Map<String, Object> getPublicDatasetGroupByPageTYX(Page<DatasetGroup> page, String name, Boolean isGuided) {
        // 如果指定了 isGuided 筛选，使用 SQL 层面的筛选方法
        IPage<DatasetGroup> datasetGroupPage;
        if (isGuided != null) {
            // 使用自定义 SQL 查询，支持 isGuided 筛选
            datasetGroupPage = datasetGroupMapper.getPublicDatasetGroupPageWithFilter(page, name, isGuided);
        } else {
            // 没有筛选时，使用原来的 QueryWrapper 方式
            QueryWrapper<DatasetGroup> datasetQueryWrapper = new QueryWrapper<>();
            datasetQueryWrapper.eq("is_public", 1);
            datasetQueryWrapper.like(StringUtils.isNotBlank(name), "name", name);
            datasetQueryWrapper.orderByDesc("creation_time");
            datasetGroupPage = datasetGroupMapper.selectPage(page, datasetQueryWrapper);
        }
        
        List<DatasetGroup> records = datasetGroupPage.getRecords();

        List<DatasetGroupVO> datasetGroupVOs = new ArrayList<>();

        for (DatasetGroup datasetGroup : records) {
            LocalDateTime creationTimeWithOffset = datasetGroup.getCreationTime() != null
                    ? datasetGroup.getCreationTime().plusHours(8)
                    : null;
            LocalDateTime updateTimeWithOffset = datasetGroup.getUpdateTime() != null
                    ? datasetGroup.getUpdateTime().plusHours(8)
                    : null;

            // 获取该组下所有数据集IDs
            List<Long> allDatasetIds = datasetDatasetGroupMapper.getGroupAllDatasetIds(datasetGroup.getId());

            // 关键修改：更严格的过滤逻辑 - 检查数据集是否真的有公开版本
            Map<Long, Map<String, Object>> result = datasetVersionMapper.getPublicVersionCountsByDatasetIds(allDatasetIds);
            List<Long> publicDatasetIds = new ArrayList<>(result.keySet());
            Integer publicDatasetCount = publicDatasetIds.size();
            Integer publicVersionCount = result.values().stream()
                    .mapToInt(map -> ((Long) map.get("public_count")).intValue())
                    .sum();
            Long publicTotalFiles = datasetVersionServiceImpl.calculatePublicDatasetTotalFiles(publicDatasetIds);

            // 计算公开数据集的标签种类数
            Integer publicLabelCount = 0;
            try {
                publicLabelCount = datasetDatasetGroupMapper.countLabelsByDatasetIds(publicDatasetIds);
            } catch (Exception e) {
                // 忽略异常，使用默认值0
            }

            // 获取最新的公开数据集
            List<Long> latestPublicDatasetIds = publicDatasetIds.isEmpty() ?
                    Collections.emptyList() :
                    Collections.singletonList(Collections.max(publicDatasetIds));
            Map<String, Date> latestPublicDatasetNamesMap = datasetServiceImpl.getDatasetNameTimeMap(latestPublicDatasetIds);

            DatasetGroupVO vo = DatasetGroupVO.builder()
                    .id(datasetGroup.getId())
                    .name(datasetGroup.getName())
                    .description(datasetGroup.getDescription())
                    .isPublic(datasetGroup.getIsPublic())
                    .creationTime(creationTimeWithOffset)
                    .updateTime(updateTimeWithOffset)
                    .datasetCount(publicDatasetCount)
                    .versionCount(publicVersionCount)
                    .labelCount(publicLabelCount)
                    .latestDatasets(latestPublicDatasetNamesMap)
                    .totalFiles(publicTotalFiles)
                    .build();

            datasetGroupVOs.add(vo);
        }

        return PageUtil.toPage(datasetGroupPage, datasetGroupVOs);
    }

    @Override
    public Map<DatasetGroup, List<Dataset>> getDatasetGroupsMapByDatasetIds(List<Long> datasetIds) {
        if (datasetIds == null || datasetIds.isEmpty()) {
            return Collections.emptyMap();
        }

        QueryWrapper<DatasetDatasetGroup> linkQuery = new QueryWrapper<>();
        linkQuery.in("dataset_id", datasetIds);
        List<DatasetDatasetGroup> links = datasetDatasetGroupMapper.selectList(linkQuery);

        if (links.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> datasetGroupIds = links.stream()
                .map(DatasetDatasetGroup::getDatasetGroupId)
                .distinct()
                .collect(Collectors.toList());
        List<DatasetGroup> datasetGroups = datasetGroupMapper.selectBatchIds(datasetGroupIds);

        List<Dataset> datasets = datasetServiceImpl.listByIds(datasetIds);

        Map<Long, DatasetGroup> groupLookupMap = datasetGroups.stream()
                .collect(Collectors.toMap(DatasetGroup::getId, group -> group));

        Map<Long, Dataset> datasetLookupMap = datasets.stream()
                .collect(Collectors.toMap(Dataset::getId, dataset -> dataset));

        Map<DatasetGroup, List<Dataset>> result = new HashMap<>();
        for (DatasetDatasetGroup link : links) {
            DatasetGroup group = groupLookupMap.get(link.getDatasetGroupId());
            Dataset dataset = datasetLookupMap.get(link.getDatasetId());

            if (group != null && dataset != null) {
                result.computeIfAbsent(group, k -> new ArrayList<>()).add(dataset);
            }
        }

        return result;
    }

    @Override
    public List<DatasetVO> getPublicDatasetsByDatasetGroupId(Long datasetGroupId) {
        QueryWrapper<DatasetDatasetGroup> datasetQueryWrapper =  new QueryWrapper<>();
        datasetQueryWrapper.eq("dataset_group_id", datasetGroupId);
        List<DatasetDatasetGroup> datasetDatasetGroups = datasetDatasetGroupMapper.selectList(datasetQueryWrapper);
        List<Long> datasetIds = datasetDatasetGroups.stream().map(DatasetDatasetGroup::getDatasetId).collect(Collectors.toList());
        return datasetServiceImpl.batchGet(datasetIds).values().stream().filter(datasetVO -> datasetVO.getIsPublic() == 1).collect(Collectors.toList());
    }

    @Override
    public String getDatasetGroupNameByDatasetId(Long datasetId) {
        QueryWrapper<DatasetDatasetGroup> datasetQueryWrapper =  new QueryWrapper<>();
        datasetQueryWrapper.eq("dataset_id", datasetId);
        DatasetDatasetGroup datasetDatasetGroup = datasetDatasetGroupMapper.selectOne(datasetQueryWrapper);
        if (datasetDatasetGroup == null) {
            return "";
        }
        return datasetGroupMapper.selectById(datasetDatasetGroup.getDatasetGroupId()).getName();
    }

    @Override
    public boolean datasetGroupExists(String datasetGroupName) {
        return datasetGroupMapper.selectCount(new QueryWrapper<DatasetGroup>().eq("name", datasetGroupName)) > 0;
    }

    @Override
    public DatasetGroup getDatasetGroupByName(String datasetGroupName) {
        return datasetGroupMapper.selectOne(new QueryWrapper<DatasetGroup>().eq("name", datasetGroupName));
    }

    @Override
    public boolean deleteDatasetGroup(Long datasetGroupId) {
        if (datasetDatasetGroupServiceImpl.count(new QueryWrapper<DatasetDatasetGroup>().eq("dataset_group_id", datasetGroupId)) > 0
                || pcDatasetDatasetGroupMapper.selectCount(new QueryWrapper<PcDatasetDatasetGroup>()
                .eq("dataset_group_id", datasetGroupId)) > 0
                || videoDatasetDatasetGroupMapper.selectCount(new QueryWrapper<VideoDatasetDatasetGroup>()
                .eq("dataset_group_id", datasetGroupId)) > 0
                || multiDatasetDatasetGroupMapper.selectCount(new QueryWrapper<MultiDatasetDatasetGroup>()
                .eq("dataset_group_id", datasetGroupId)) > 0) {
            // 说明存在关联
            return false;
        } else {
            datasetGroupMapper.deleteById(datasetGroupId);
            return true;
        }
    }

/**
 * 计算数据集组的总图片数 - 使用已有方法
 * @param datasetGroupId 数据集组ID
 * @return 总图片数
 */
private Long calculateTotalFilesForGroup(Long datasetGroupId) {
    try {
        // 1. 获取数据集组下的所有数据集ID
        Long userId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();
        List<Long> datasetIds = null;
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            datasetIds = datasetDatasetGroupMapper.getGroupDatasetIdsByUserId(datasetGroupId, userId);
        } else {
            datasetIds = datasetDatasetGroupMapper.getDatasetsByDatasetGroupId(datasetGroupId);
        }
        if (datasetIds == null || datasetIds.isEmpty()) {
            return 0L;
        }

        long totalFiles = 0L;

        // 2. 遍历每个数据集，调用已有的计算方法
        for (Long datasetId : datasetIds) {
            try {
                // 调用DatasetVersionServiceImpl中已有的方法
                Integer datasetFiles = datasetVersionServiceImpl.calculateDatasetTotalFiles(datasetId);
                if (datasetFiles != null) {
                    totalFiles += datasetFiles;
                }
            } catch (Exception e) {
                log.warn("计算数据集{}的图片数时出错: {}", datasetId, e.getMessage());
                // 继续处理其他数据集，不因为一个数据集的错误而中断整个计算
            }
        }

        return totalFiles;
    } catch (Exception e) {
        log.error("计算数据集组{}的总图片数时出错: {}", datasetGroupId, e.getMessage(), e);
        return 0L;
    }
}

    /**
     * 分页获取公开数据集组中的公开数据集
     * @param datasetGroupId 数据集组ID
     * @param page 分页参数
     * @return 分页结果
     */
    public IPage<DatasetVO> getPublicDatasetsPageByDatasetGroupId(Long datasetGroupId, Page<DatasetDatasetGroup> page) {
        // 查询数据集组与数据集的关联关系
        QueryWrapper<DatasetDatasetGroup> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_group_id", datasetGroupId);

        List<DatasetDatasetGroup> relations = datasetDatasetGroupMapper.selectList(wrapper);

        // 如果当前页没有数据，直接返回一个空的 Page 对象
        if (relations.isEmpty()) {
            return new Page<DatasetVO>().setRecords(Collections.emptyList());
        }

        // 获取当前页的 datasetIds
        List<Long> datasetIds = relations.stream()
                .map(DatasetDatasetGroup::getDatasetId)
                .collect(Collectors.toList());

        // 批量获取 DatasetVO，并过滤出有公开版本的数据集
        Collection<DatasetVO> allDatasets = datasetServiceImpl.batchGet(datasetIds).values();
        List<DatasetVO> publicDatasets = new ArrayList<>();

        // 提取所有数据集ID
        List<Long> allDatasetIds = allDatasets.stream()
                .map(DatasetVO::getId)
                .collect(Collectors.toList());
        Map<Long, Map<String, Object>> publicCountMap = datasetVersionMapper.getPublicVersionCountsByDatasetIds(allDatasetIds);

        for (DatasetVO dataset : allDatasets) {
            // 检查该数据集是否真的有公开版本
            Map<String, Object> countInfo = publicCountMap.get(dataset.getId());
            if (countInfo != null) {
                // 获取public_count的值
                Long publicCount = (Long) countInfo.get("public_count");
                if (publicCount != null && publicCount > 0) {
                    publicDatasets.add(dataset);
                }
            }
        }

        // 关键修复：如果没有公开数据集，直接返回空结果，避免调用 getDatasetGroupsMap
        if (publicDatasets.isEmpty()) {
            IPage<DatasetVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), 0);
            resultPage.setRecords(Collections.emptyList());
            return resultPage;
        }
        // 批量准备数据集信息用于查询 changed 文件计数
        List<DatasetVersionFileMapper.DatasetChangedInfo> datasetInfos = new ArrayList<>();

        // 对公开数据集进行逻辑处理（与原方法相同）
        Map<Long, List<GroupInfoDTO>> datasetGroupsMap = getDatasetGroupsMap(publicDatasets);
        for (DatasetVO datasetVO : publicDatasets) {
            datasetVO.setGroups(datasetGroupsMap.getOrDefault(datasetVO.getId(), Collections.emptyList()));
            if (datasetVO.getCurrentVersionName() != null) {
                DatasetVersion datasetVersion = datasetVersionServiceImpl
                        .getVersionByDatasetIdAndVersionName(datasetVO.getId(), datasetVO.getCurrentVersionName());
                if (datasetVersion != null) {
                    datasetVO.setDataConversion(datasetVersion.getDataConversion());
                }
            }

            // 计算 hasChanges 标志
            boolean hasChanges = false;

            // 情况1: currentVersionName 为 null 且状态是已标注
            if (datasetVO.getCurrentVersionName() == null && datasetVO.getStatus() != null
                    && datasetVO.getStatus().equals(DataStateCodeConstant.ANNOTATION_COMPLETE_STATE)) {
                hasChanges = true;
            }

            // 情况2: 当前版本有文件的 changed = 1（使用批量查询结果）
            if (!hasChanges && datasetVO.getCurrentVersionName() != null) {
                // 批量查询所有有版本的数据集的 changed 文件计数
                Map<Long, Integer> changedFilesCountMap = datasetVersionFileServiceImpl.batchCountChangedFiles(datasetInfos);
                Integer count = changedFilesCountMap.get(datasetVO.getId());
                hasChanges = (count != null && count > 0);
            }

            datasetVO.setHasChanges(hasChanges);
        }

        publicDatasets.sort(Comparator.comparing(DatasetVO::getId).reversed());

        IPage<DatasetVO> resultPage = new Page<>(page.getCurrent(), page.getSize(), publicDatasets.size());
        resultPage.setRecords(publicDatasets);

        return resultPage;
    }

    @Override
    public boolean datasetGroupExistsById(Long datasetGroupId) {
        return datasetGroupMapper.selectCount(new QueryWrapper<DatasetGroup>().eq("id", datasetGroupId)) > 0;
    }

    /**
     * 按名称分页搜索私有数据集分组
     * @param page 分页参数
     * @param name 搜索关键词
     * @return 分页结果
     */
    public IPage<DatasetGroup> searchPrivateDatasetGroupByName(Page<DatasetGroup> page, String name) {
        QueryWrapper<DatasetGroup> datasetQueryWrapper = new QueryWrapper<>();
        Long userId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();

        // 如果不是管理员，添加用户ID过滤条件
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            datasetQueryWrapper.eq("create_user_id", userId);
        }else {
            //如果是管理员的话，查一下所有非公开的
            datasetQueryWrapper.eq("is_public", false);
        }
        datasetQueryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        datasetQueryWrapper.orderByDesc("creation_time");
        return datasetGroupMapper.selectPage(page, datasetQueryWrapper);
    }

    /**
     * 分页查询私有数据集分组
     * @param page 分页参数
     * @return 分页结果
     */
    public IPage<DatasetGroup> getPrivateDatasetGroupByPage(Page<DatasetGroup> page) {
        QueryWrapper<DatasetGroup> datasetQueryWrapper = new QueryWrapper<>();
        Long userId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();

        // 如果不是管理员，添加用户ID过滤条件
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            datasetQueryWrapper.eq("create_user_id", userId);
        }else {
            //如果是管理员的话，查一下所有非公开的
            datasetQueryWrapper.eq("is_public", false);
        }

        // 只查询自己的私有数据集组
        //datasetQueryWrapper.eq("is_public", false);
        datasetQueryWrapper.orderByDesc("creation_time");
        return datasetGroupMapper.selectPage(page, datasetQueryWrapper);
    }

    /**
     * 获取包含未发布数据集的数据集组列表（用于多人标注任务创建）
     * @return 数据集组列表
     */
    @Override
    public List<DatasetGroup> getDatasetGroupsWithUnpublishedDatasets() {
        UserContext curUser = userContextService.getCurUser();
        String roleName = curUser.getRoles().get(0).getName();
        long creatorId = curUser.getId();

        // 查询所有未发布的数据集ID
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<Dataset>()
                .select("id")
                .eq("deleted", 0)
                .isNull("current_version_name")
                .eq("module", DatasetModuleEnum.NO_TEAM.getStatus());

        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            datasetQueryWrapper.eq("create_user_id", creatorId);
        }

        List<Long> unpublishedDatasetIds = datasetMapper.selectList(datasetQueryWrapper)
                .stream()
                .filter(dataset -> datasetVersionFileMapper.getDatasetVersionAllFileCount(dataset.getId(), null) > 0)
                .map(Dataset::getId)
                .collect(Collectors.toList());

        if (unpublishedDatasetIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询包含这些数据集的数据集组ID
        List<Long> datasetGroupIds = datasetDatasetGroupMapper.selectList(
                new QueryWrapper<DatasetDatasetGroup>()
                        .select("DISTINCT dataset_group_id")
                        .in("dataset_id", unpublishedDatasetIds)
        ).stream().map(DatasetDatasetGroup::getDatasetGroupId).distinct().collect(Collectors.toList());

        if (datasetGroupIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询数据集组信息
        QueryWrapper<DatasetGroup> groupQueryWrapper = new QueryWrapper<DatasetGroup>()
                .in("id", datasetGroupIds)
                .orderByDesc("creation_time");

        return datasetGroupMapper.selectList(groupQueryWrapper);
    }


}
