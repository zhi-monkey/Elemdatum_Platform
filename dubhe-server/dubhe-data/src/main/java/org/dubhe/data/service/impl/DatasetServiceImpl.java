/**
 * Copyright 2020 Tianshu AI Platform. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================
 */

package org.dubhe.data.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.constant.SymbolConstant;
import org.dubhe.biz.base.context.DataContext;
import org.dubhe.biz.base.context.UserContext;
import org.dubhe.biz.base.dto.CommonPermissionDataDTO;
import org.dubhe.biz.base.dto.GroupInfoDTO;
import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.biz.base.enums.OperationTypeEnum;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.base.vo.DatasetVO;
import org.dubhe.biz.base.vo.ProgressVO;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.biz.file.api.FileStoreApi;
import org.dubhe.biz.file.utils.LocalFileUtil;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.biz.permission.annotation.RolePermission;
import org.dubhe.biz.permission.base.BaseService;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.biz.statemachine.dto.StateChangeDTO;
import org.dubhe.cloud.authconfig.utils.JwtUtils;
import org.dubhe.data.client.TrainServerClient;
import org.dubhe.data.client.UserClient;
import org.dubhe.data.constant.*;
import org.dubhe.data.dao.*;
import org.dubhe.data.domain.bo.FileUploadBO;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.*;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.machine.constant.DataStateCodeConstant;
import org.dubhe.data.machine.constant.DataStateMachineConstant;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.machine.utils.StateIdentifyUtil;
import org.dubhe.data.machine.utils.StateMachineUtil;
import org.dubhe.data.pool.BasePool;
import org.dubhe.data.service.*;
import org.dubhe.data.service.task.DatasetRecycleFile;
import org.dubhe.data.util.ConversionUtil;
import org.dubhe.data.util.FileUtil;
import org.dubhe.data.util.GeneratorKeyUtil;
import org.dubhe.data.util.ZipUtil;
import org.dubhe.recycle.domain.dto.RecycleCreateDTO;
import org.dubhe.recycle.domain.dto.RecycleDetailCreateDTO;
import org.dubhe.recycle.enums.RecycleModuleEnum;
import org.dubhe.recycle.enums.RecycleResourceEnum;
import org.dubhe.recycle.enums.RecycleTypeEnum;
import org.dubhe.recycle.service.RecycleService;
import org.dubhe.recycle.utils.RecycleTool;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.dubhe.data.constant.Constant.*;
import static org.dubhe.data.constant.ErrorEnum.DATASET_PUBLIC_LIMIT_ERROR;


/**
 * @description 数据集服务实现类
 * @date 2020-04-10
 */
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@Slf4j
@Service
public class DatasetServiceImpl extends ServiceImpl<DatasetMapper, Dataset> implements DatasetService {

    /**
     * 需要同步的状态
     */
    private static final Set<Integer> NEED_SYNC_STATUS = new HashSet<Integer>() {{
        add(DataStateCodeConstant.NOT_ANNOTATION_STATE);
        add(DataStateCodeConstant.MANUAL_ANNOTATION_STATE);
        add(DataStateCodeConstant.AUTO_TAG_COMPLETE_STATE);
        add(DataStateCodeConstant.ANNOTATION_COMPLETE_STATE);
        add(DataStateCodeConstant.NOT_SAMPLED_STATE);
        add(DataStateCodeConstant.TARGET_COMPLETE_STATE);
    }};
    /**
     * 执行数据扩容数据集应该具备的状态
     */
    private static final Set<Integer> COMPLETE_STATUS = new HashSet<Integer>() {{
        add(DataStateCodeConstant.AUTO_TAG_COMPLETE_STATE);
        add(DataStateCodeConstant.ANNOTATION_COMPLETE_STATE);
        add(DataStateCodeConstant.TARGET_COMPLETE_STATE);
    }};
    /**
     * 文件操作
     */
    @Autowired
    public LocalFileUtil localFileUtil;
    /**
     * 文件信息服务
     */
    @Autowired
    public FileService fileService;
    @Lazy
    @Autowired
    public AnnotationService annotationService;
    @Autowired
    @Lazy
    private TaskService taskService;
    @Autowired
    private DatasetMapper datasetMapper;
    @Autowired
    private DataRepoFileMapper dataRepoFileMapper;
    @Resource(name = "hostFileStoreApiImpl")
    private FileStoreApi fileStoreApi;

    /**
     * bucket
     */
    @Value("${minio.bucketName}")
    private String bucket;
    /**
     * 路径名前缀
     */
    @Value("${storage.file-store-root-path:/nfs/}")
    private String prefixPath;
    /**
     * 数据集标签服务类
     */
    @Resource
    @Lazy
    private LabelService labelService;

    /**
     * 文件服务类
     */
    @Autowired
    private FileUtil fileUtil;

    /**
     * 数据集版本文件关系服务实现类
     */
    @Autowired
    @Lazy
    private DatasetVersionFileService datasetVersionFileService;


    /**
     * 数据集版本服务实现类
     */
    @Autowired
    @Lazy
    private DatasetVersionService datasetVersionService;

    @Autowired
    @Lazy
    private DatasetVersionServiceImpl datasetVersionServiceImpl;


    /**
     * 数据集实时状态获取工具
     */
    @Autowired
    private StateIdentifyUtil stateIdentify;

    /**
     * 任务mapper
     */
    @Autowired
    private TaskMapper taskMapper;

    @Resource
    private TrainServerClient trainServiceClient;

    /**
     * 数据集标签服务
     */
    @Autowired
    private DatasetLabelService datasetLabelService;


    @Autowired
    private DatasetGroupLabelService datasetGroupLabelService;

    /**
     * 数据回收服务
     */
    @Autowired
    private RecycleService recycleService;

    /**
     * 标签组服务
     */
    @Autowired
    private LabelGroupServiceImpl labelGroupService;

    /**
     * 用户内容服务
     */
    @Autowired
    private UserContextService userContextService;

    /**
     * 数据回收服务类
     */
    @Autowired
    private DatasetRecycleFile datasetRecycleFile;

    /**
     * 数据集与数据集组关联mapper
     */
    @Autowired
    private DatasetDatasetGroupMapper datasetDatasetGroupMapper;

    /**
     * 文件回收工具
     */
    @Autowired
    private RecycleTool recycleTool;

    /**
     * 文件标注服务
     */
    @Resource
    private DataFileAnnotationService dataFileAnnotationService;

    /**
     * minIo客户端工具
     */
    @Resource
    private MinioUtil minioUtil;

    @Resource
    private RedisUtils redisUtils;

    @Autowired
    private GeneratorKeyUtil generatorKeyUtil;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private org.dubhe.data.client.SelfIterationClient selfIterationClient;

    @Autowired
    private UserClient userClient;

    @Autowired
    private DataTeamTaskMapper dataTeamTaskMapper;
    /**
     * 线程池
     */
    @Autowired
    private BasePool pool;

    @Value("${storage.file-store}")
    private String nfsIp;

    @Value("${data.server.userName}")
    private String userName;
    @Autowired
    private DatasetVersionFileMapper datasetVersionFileMapper;

    private final ConcurrentHashMap<Long, AtomicInteger> taskCounters = new ConcurrentHashMap<>();

    @Autowired
    private LabelMappingServiceImpl labelMappingServiceImpl;


    @Autowired
    private DatasetOperationEventService datasetOperationEventService;

    @Autowired
    private ImportTransferTaskService importTransferTaskService;

    /**
     * 文件元信息服务
     */
    @Autowired
    private FileMetadataService fileMetadataService;

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Autowired
    private DataFileAnnotationMapper dataFileAnnotationMapper;

    @Autowired
    private NotificationService notificationService;

    // 线程安全Map，id -> List<进度和时间>
    private final Map<Long, List<Integer>> progressTimeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> failedZipTransferTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> zipTransferTaskIds = new ConcurrentHashMap<>();
    @Autowired
    private DatasetGroupServiceImpl datasetGroupServiceImpl;
    @Autowired
    private DatasetDatasetGroupServiceImpl datasetDatasetGroupServiceImpl;
    @Autowired
    private DatasetGroupMapper datasetGroupMapper;

    /**
     * 检测是否为公共数据集
     *
     * @param id 数据集id
     * @return Boolean 是否为公共数据集
     */
    @Override
    public Boolean checkPublic(Long id, OperationTypeEnum type) {
        Dataset dataset = baseMapper.selectById(id);
        return checkPublic(dataset, type);
    }

    /**
     * 检测是否为公共数据集
     *
     * @param dataset 数据集
     */
    @Override
    public Boolean checkPublic(Dataset dataset, OperationTypeEnum type) {
        if (Objects.isNull(dataset)) {
            return false;
        }
        if (DatasetTypeEnum.PUBLIC.getValue().equals(dataset.getType())) {
            // 操作类型校验公共数据集
            if (OperationTypeEnum.UPDATE.equals(type)) {
                BaseService.checkAdminPermission();
                // 操作类型校验公共数据集
            } else if (OperationTypeEnum.LIMIT.equals(type)) {
                throw new BusinessException(DATASET_PUBLIC_LIMIT_ERROR);
            } else {
                return true;
            }

        }
        return false;
    }

    /**
     * 自动标注检查
     *
     * @param file 文件
     */
    @Override
    public void autoAnnotatingCheck(File file) {
        autoAnnotatingCheck(file.getDatasetId());
    }

    /**
     * 自动标注检查
     *
     * @param datasetId 数据集id
     */
    public void autoAnnotatingCheck(Long datasetId) {
        LambdaQueryWrapper<Dataset> datasetQueryWrapper = new LambdaQueryWrapper<>();
        datasetQueryWrapper
                .eq(Dataset::getId, datasetId)
                .eq(Dataset::getStatus, DataStateCodeConstant.AUTOMATIC_LABELING_STATE);
        if (getBaseMapper().selectCount(datasetQueryWrapper) > MagicNumConstant.ZERO) {
            throw new BusinessException(ErrorEnum.AUTO_ERROR);
        }
    }

    /**
     * 数据集修改
     *
     * @param datasetCreateDTO 更新的数据集详情
     * @param datasetId        数据集id
     * @return boolean 更新是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean update(DatasetCreateDTO datasetCreateDTO, Long datasetId) {
        // Step 1: 检查数据集是否存在
        if (!exist(datasetId)) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT);
        }
        // Step 2: 检查是否有权限进行更新操作
        checkPublic(datasetId, OperationTypeEnum.UPDATE);
        // Step 3: 获取当前数据集对象
        Dataset dataset = getBaseMapper().selectById(datasetId);
        // Step 4: 判断是否已存在同名的数据集
        Dataset datasetByName = datasetMapper.findByName(datasetCreateDTO.getName());
        if (!Objects.isNull(datasetByName) && !datasetByName.getId().equals(datasetId)) {
            // 如果存在同名数据集，抛出 BusinessException
            throw new BusinessException("数据集名称已被占用");
        }
        int fileCount = fileService.getFileCountByDatasetId(datasetId);
        if (!dataset.getDataType().equals(datasetCreateDTO.getDataType())
                && fileCount > MagicNumConstant.ZERO && datasetCreateDTO.getDataType() != null) {
            throw new BusinessException(ErrorEnum.DATASET_TYPE_MODIFY_ERROR);
        }
        if (!dataset.getAnnotateType().equals(datasetCreateDTO.getAnnotateType())
                && dataset.getStatus() != MagicNumConstant.ZERO && datasetCreateDTO.getAnnotateType() != null) {
            throw new BusinessException(ErrorEnum.DATASET_ANNOTATION_MODIFY_ERROR);
        }
        Dataset newDataset = DatasetCreateDTO.update(datasetCreateDTO);
        newDataset.setId(datasetId);
        newDataset.setTop(dataset.isTop());
        newDataset.setImport(dataset.isImport());
        int count;
        try {
            count = getBaseMapper().updateById(newDataset);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorEnum.DATASET_NAME_DUPLICATED_ERROR, null, e);
        }
        if (count == MagicNumConstant.ZERO) {
            throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH);
        }
        // 修改数据集和标签的关系
        doDatasetLabelByUpdate(dataset, datasetCreateDTO, datasetId);
        return true;
    }


    /**
     * 更新数据集状态
     *
     * @param dataset 数据集
     * @param pre     转变前的状态
     * @return boolean 更新结果
     */
    @Override
    public boolean updateStatus(Dataset dataset, DataStateEnum pre) {
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<>();
        datasetQueryWrapper.lambda().eq(Dataset::getId, dataset.getId());
        if (pre != null) {
            datasetQueryWrapper.lambda().eq(Dataset::getStatus, pre);
        }
        getBaseMapper().update(dataset, datasetQueryWrapper);
        return true;
    }

    /**
     * 更新状态
     *
     * @param id 数据集id
     * @param to 转变后的状态
     * @return boolean 更新结果
     */
    @Override
    public boolean updateStatus(Long id, DataStateEnum to) {
        return updateStatus(id, null, to);
    }

    /**
     * 更新状态实现
     *
     * @param id  数据集id
     * @param pre 转变前的状态
     * @param to  转变后的状态
     * @return boolean 更新结果
     */
    public boolean updateStatus(Long id, DataStateEnum pre, DataStateEnum to) {
        Dataset dataset = datasetMapper.selectById(id);
        dataset.setStatus(to.getCode());
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<>();
        datasetQueryWrapper.lambda().eq(Dataset::getId, id);
        getBaseMapper().update(dataset, datasetQueryWrapper);
        return true;
    }

    /**
     * 更改数据集状态
     *
     * @param dataset 数据集
     * @param to      转变后的状态
     * @return boolean 更新结果
     */
    @Override
    public boolean transferStatus(Dataset dataset, DataStateEnum to) {
        return transferStatus(dataset, null, to);
    }

    /**
     * 更新数据集实现
     *
     * @param dataset 数据集
     * @param pre     转变前的状态
     * @param to      转变后的状态
     * @return boolean 更新结果
     */
    public boolean transferStatus(Dataset dataset, DataStateEnum pre, DataStateEnum to) {
        if (dataset == null || to == null) {
            return false;
        }
        dataset.setStatus(to.getCode());
        return updateStatus(dataset, pre);
    }

    /**
     * 保存标签
     *
     * @param label     标签
     * @param datasetId 数据集id
     * @return Long     标签id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveLabel(Label label, Long datasetId) {
        if (label.getId() == null && StringUtils.isEmpty(label.getName())) {
            throw new BusinessException(ErrorEnum.LABEL_ERROR);
        }
        if (!exist(datasetId)) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT);
        }
        Dataset dataset = baseMapper.selectById(datasetId);
        if (Objects.isNull(dataset)) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT);
        }
        checkPublic(dataset, OperationTypeEnum.UPDATE);

        // 校验是否是预置数据集
        DatatypeEnum enumValue = DatatypeEnum.getEnumValue(dataset.getDataType());
        List<Label> labelList = labelService.getPubLabels(enumValue.getValue());

        // 名称重复性校验
        if (labelService.checkoutLabelIsRepeat(datasetId, label.getName())) {
            throw new BusinessException(ErrorEnum.LABEL_NAME_REPEAT);
        }
        if (!CollectionUtils.isEmpty(labelList)) {
            Map<String, Long> labelNameMap = labelList.stream().collect(Collectors.toMap(Label::getName, Label::getId));
            if (!Objects.isNull(labelNameMap.get(label.getName()))) {
                datasetLabelService.insert(DatasetLabel.builder().datasetId(datasetId).labelId(labelNameMap.get(label.getName())).build());
                // datasetGroupLabelService.insert(DatasetGroupLabel.builder().labelGroupId(dataset.getLabelGroupId()).labelId(labelNameMap.get(label.getName())).build());
            } else {
                insertLabelData(label, datasetId);
            }
        } else {
            insertLabelData(label, datasetId);
        }

    }

    /**
     * 获取数据集详情
     *
     * @param datasetId 数据集id
     * @return DatasetVO 数据集详情
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public DatasetVO get(Long datasetId) {
        Dataset ds = baseMapper.selectById(datasetId);
        if (ds == null) {
            return null;
        }
        if (checkPublic(ds, OperationTypeEnum.SELECT)) {
            DataContext.set(CommonPermissionDataDTO.builder().id(datasetId).type(true).build());
        }
        Map<Long, ProgressVO> statistics = fileService.listStatistics(Arrays.asList(ds));

        if (ds.getLabelGroupId() != null) {
            LabelGroup labelGroup = labelGroupService.getBaseMapper().selectById(ds.getLabelGroupId());
            if (labelGroup != null) {
                DatasetVO datasetVO = buildDatasetVO(ds, labelGroup.getName(), labelGroup.getType());
                datasetVO.setProgress(statistics.get(datasetVO.getId()));
                setDatasetVOFileCount(datasetVO);
                return datasetVO;
            }
        }

        DatasetVO datasetVO = buildDatasetVO(ds, null, null);
        setDatasetVOFileCount(datasetVO);
        return datasetVO;
    }

    /**
     * 批量获取数据集详情
     *
     * @param datasetIds 数据集ids
     * @return DatasetVO 根据Id查询出对应的数据集
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Map<Long, DatasetVO> batchGet(List<Long> datasetIds) {
        if (datasetIds == null || datasetIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, DatasetVO> resultMap = new HashMap<>();
        for (Long datasetId : datasetIds) {
            // 直接调用原有的单个获取方法，复用逻辑
            DatasetVO datasetVO = get(datasetId);
            resultMap.put(datasetId, datasetVO);
        }
        return resultMap;
    }

    @Override
    public Boolean existsGuidedDataset(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return false;
        }
        return datasetMapper.existsGuidedDataset(datasetIds);
    }

    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public GuidedAndLabelsVO guidedAndLabels(Long datasetId) {
        GuidedAndLabelsVO guidedAndLabelsVO = new GuidedAndLabelsVO();
        guidedAndLabelsVO.setIsDatasetGuided(datasetMapper.isDatasetGuided(datasetId));
        List<Label> labels = labelService.findLabelByIds(datasetMapper.findLabelsByDatasetId(datasetId));
        List<LabelZipDTO> labelZipDTOList = labels.stream()
                .map(label -> {
                    LabelZipDTO labelZipDTO = new LabelZipDTO();
                    BeanUtils.copyProperties(label, labelZipDTO);
                    return labelZipDTO;
                })
                .collect(Collectors.toList());
        guidedAndLabelsVO.setLabels(labelZipDTOList);
        
        // 获取数据集的标注类型
        Dataset dataset = datasetMapper.selectById(datasetId);
        if (dataset != null) {
            guidedAndLabelsVO.setAnnotateType(dataset.getAnnotateType());
        }
        
        return guidedAndLabelsVO;
    }

    private DatasetVO buildDatasetVO(Dataset dataset, String labelGroupName, Integer labelGroupType) {
        DatasetVO datasetVO = new DatasetVO();
        if (dataset == null) {
            return null;
        }
        datasetVO.setId(dataset.getId());
        datasetVO.setDatasetCode(FileUtil.getDatasetCode(dataset.getId(), dataset.getDataType()));
        datasetVO.setName(dataset.getName());
        datasetVO.setRemark(dataset.getRemark());
        datasetVO.setUri(dataset.getUri());
        datasetVO.setCreateTime(dataset.getCreateTime());
        datasetVO.setUpdateTime(dataset.getUpdateTime());
        datasetVO.setType(dataset.getType());
        datasetVO.setDataType(dataset.getDataType());
        datasetVO.setAnnotateType(dataset.getAnnotateType());
        datasetVO.setStatus(dataset.getStatus());
        datasetVO.setDecompressState(dataset.getDecompressState());
        datasetVO.setImport(dataset.isImport());
        datasetVO.setTop(dataset.isTop());
        datasetVO.setLabelGroupId(dataset.getLabelGroupId());
        datasetVO.setLabelGroupName(labelGroupName);
        datasetVO.setLabelGroupType(labelGroupType);
        datasetVO.setSourceId(dataset.getSourceId());
        datasetVO.setCurrentVersionName(dataset.getCurrentVersionName());
        datasetVO.setTemplateType(dataset.getTemplateType());
        datasetVO.setModule(dataset.getModule());
        datasetVO.setIsPublic(dataset.getIsPublic());
        datasetVO.setIsGuided(dataset.isGuided());
        datasetVO.setIsPublishing(dataset.getIsPublishing());
        datasetVO.setDeleted(dataset.getDeleted());
        datasetVO.setCreateUserId(dataset.getCreateUserId());
        return datasetVO;
    }

    /**
     * 设置数据集FileCount信息
     *
     * @param datasetVO 数据集详情
     */
    public void setDatasetVOFileCount(DatasetVO datasetVO) {
        datasetVO.setFileCount(datasetVersionFileService.getFileCountByDatasetIdAndVersion(new LambdaQueryWrapper<DatasetVersionFile>() {{
            eq(DatasetVersionFile::getDatasetId, datasetVO.getId());
            if ((datasetVO.getCurrentVersionName() == null)) {
                isNull(DatasetVersionFile::getVersionName);
            } else {
                eq(DatasetVersionFile::getVersionName, datasetVO.getCurrentVersionName());
            }
            ne(DatasetVersionFile::getStatus, DataStatusEnum.DELETE.getValue());
        }}));
    }

    /**
     * 数据集下载
     *
     * @param datasetId           数据集id
     * @param httpServletResponse 响应对象
     */
    @Override
    public boolean download(Long datasetId, HttpServletResponse httpServletResponse) {
        Dataset ds = baseMapper.selectById(datasetId);
        if (ds == null) {
            return false;
        }
        String zipFile = ZipUtil.zip(ds.getUri());
        fileStoreApi.download(zipFile, httpServletResponse);
        return true;
    }

    /**
     * 创建数据集
     *
     * @param datasetCreateDTO 数据集信息
     * @return Long 数据集id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long create(DatasetCreateDTO datasetCreateDTO) {
        // Step 1: 检查数据库中是否已存在同名数据集
        if (!Objects.isNull(datasetMapper.findByName(datasetCreateDTO.getName()))) {
            // Step 2: 如果存在同名数据集，抛出 BusinessException
            throw new BusinessException("数据集名称已被占用");
        }

        Long finalDatasetGroupId = null;
        if (datasetCreateDTO.getDatasetGroupId() != null) {
            // 安全性检查：确保这个传入的ID是真实存在的。
            if (!datasetGroupServiceImpl.datasetGroupExistsById(datasetCreateDTO.getDatasetGroupId())) {
                throw new BusinessException("指定的数据集组ID: " + datasetCreateDTO.getDatasetGroupId() + " 不存在！");
            }
            finalDatasetGroupId = datasetCreateDTO.getDatasetGroupId();
        }
        else if (datasetCreateDTO.getDatasetGroupName() != null && !datasetCreateDTO.getDatasetGroupName().trim().isEmpty()) {

            boolean needsToCreateNewGroup = Boolean.TRUE.equals(datasetCreateDTO.getIsCreateDatasetGroup());
            if (needsToCreateNewGroup) {
                if (datasetGroupServiceImpl.datasetGroupExists(datasetCreateDTO.getDatasetGroupName())) {
                    throw new BusinessException("无法创建新的数据集组，因为名称 '" + datasetCreateDTO.getDatasetGroupName() + "' 已存在。");
                }

                DatasetGroup newGroup = datasetGroupServiceImpl.create(
                        DatasetGroup.builder().name(datasetCreateDTO.getDatasetGroupName()).build()
                );
                finalDatasetGroupId = newGroup.getId();
            } else {
                DatasetGroup existingGroup = datasetGroupServiceImpl.getDatasetGroupByName(datasetCreateDTO.getDatasetGroupName());
                if (existingGroup == null) {
                    throw new BusinessException("无法关联到数据集组，因为名称为 '" + datasetCreateDTO.getDatasetGroupName() + "' 的数据集组不存在。");
                }
                finalDatasetGroupId = existingGroup.getId();
            }
        }
        // Step 3: 如果不存在同名数据集，继续创建数据集
        Dataset dataset = DatasetCreateDTO.from(datasetCreateDTO);
        dataset.setOriginUserId(userContextService.getCurUserId());
        try {
            save(dataset);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorEnum.DATASET_NAME_DUPLICATED_ERROR);
        }
        // 绑定数据集组
        if (finalDatasetGroupId != null) {
            datasetDatasetGroupServiceImpl.bind(finalDatasetGroupId, dataset.getId());
        }
        // 新增数据标签关系
        List<Label> labels = labelService.listByGroupId(datasetCreateDTO.getLabelGroupId());
        if (!CollectionUtils.isEmpty(labels)) {
            List<DatasetLabel> datasetLabels = labels.stream().map(a -> {
                DatasetLabel datasetLabel = new DatasetLabel();
                datasetLabel.setDatasetId(dataset.getId());
                datasetLabel.setLabelId(a.getId());
                return datasetLabel;
            }).collect(Collectors.toList());
            datasetLabelService.saveList(datasetLabels);
        }
        // 预置标签处理
        if (datasetCreateDTO.getPresetLabelType() != null) {
            presetLabel(datasetCreateDTO.getPresetLabelType(), dataset.getId());
        }
        if (DatatypeEnum.VIDEO.getValue().equals(datasetCreateDTO.getDataType())) {
            dataset.setStatus(DataStateCodeConstant.NOT_SAMPLED_STATE);
        }
        String calculatedUri = fileUtil.getDatasetStoragePath(dataset.getId(), dataset.getDataType());
        dataset.setUri(calculatedUri);
        if (datasetCreateDTO.getDataType().equals(DatatypeEnum.AUTO_IMPORT.getValue())) {
            // 自定义数据集处理 1.生成版本数据并设计数据集当前版本 2.数据集状态修改为标注完成
            DatasetVersion dv = new DatasetVersion(dataset.getId(), dataset.getUri(), DEFAULT_VERSION,
                    DatatypeEnum.getEnumValue(datasetCreateDTO.getDataType()).getMsg());
            dv.setCreateUserId(userContextService.getCurUserId());
            datasetVersionService.insertOne(dv);
            dataset.setStatus(DataStateCodeConstant.ANNOTATION_COMPLETE_STATE);
            dataset.setCurrentVersionName(DEFAULT_VERSION);
        }
        updateById(dataset);
        return dataset.getId();
    }

    /**
     * 从检索结果新建数据集（复制 data_file + 版本关系 + 标注 + 元信息 + MinIO 图片）
     *
     * @param dto 新建数据集入参
     * @return Long 新数据集id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromSearch(CreateDatasetFromSearchDTO dto) {
        // 1. 查原文件
        List<File> originFiles = fileMapper.selectBatchIds(dto.getFileIds());
        if (CollectionUtils.isEmpty(originFiles)) {
            throw new BusinessException("未找到要复制的文件");
        }
        Long originDatasetId = originFiles.get(0).getDatasetId();
        Dataset originDataset = baseMapper.selectById(originDatasetId);
        if (originDataset == null) {
            throw new BusinessException("源数据集不存在");
        }

        // 2. 建新数据集（数据类型图像，标注类型/标签组继承源数据集）
        DatasetCreateDTO createDTO = new DatasetCreateDTO();
        createDTO.setName(dto.getName());
        createDTO.setDataType(DatatypeEnum.IMAGE.getValue());
        createDTO.setAnnotateType(originDataset.getAnnotateType());
        createDTO.setType(originDataset.getType());
        createDTO.setLabelGroupId(originDataset.getLabelGroupId());
        createDTO.setDatasetGroupId(dto.getDatasetGroupId());
        if (dto.getDatasetGroupName() != null && !dto.getDatasetGroupName().trim().isEmpty()) {
            createDTO.setDatasetGroupName(dto.getDatasetGroupName());
            createDTO.setIsCreateDatasetGroup(true);
        }
        Long newDatasetId = create(createDTO);
        Dataset newDataset = baseMapper.selectById(newDatasetId);

        // 3. 复制 data_file（显式 id 映射，避免跨数据集 fileName 重名）
        Map<Long, Long> fileIdMap = new HashMap<>();
        Queue<Long> newFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE, originFiles.size());
        List<File> newFiles = new ArrayList<>();
        for (File f : originFiles) {
            Long newId = newFileIds.poll();
            fileIdMap.put(f.getId(), newId);
            File nf = File.builder()
                    .id(newId)
                    .fileType(f.getFileType())
                    .datasetId(newDatasetId)
                    .enhanceType(f.getEnhanceType())
                    .frameInterval(f.getFrameInterval())
                    .height(f.getHeight())
                    .name(f.getName())
                    .status(f.getStatus())
                    .pid(f.getPid())
                    .originUserId(f.getOriginUserId())
                    .width(f.getWidth())
                    .url(f.getUrl().replace(originDatasetId + "/", newDatasetId + "/"))
                    .build();
            nf.setCreateUserId(userContextService.getCurUserId());
            nf.setUpdateUserId(nf.getCreateUserId());
            nf.setDeleted(false);
            newFiles.add(nf);
        }
        fileMapper.insertBatch(newFiles);

        // 4. 复制 DVF（新 id + fileId 重映射）
        List<DatasetVersionFile> originDvfs = datasetVersionFileMapper.selectList(
                new LambdaQueryWrapper<DatasetVersionFile>().in(DatasetVersionFile::getFileId, dto.getFileIds()));
        Map<Long, Long> dvfIdMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(originDvfs)) {
            Queue<Long> newDvfIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_VERSION_FILE, originDvfs.size());
            List<DatasetVersionFile> newDvfs = new ArrayList<>();
            for (DatasetVersionFile dvf : originDvfs) {
                Long newDvfId = newDvfIds.poll();
                dvfIdMap.put(dvf.getId(), newDvfId);
                DatasetVersionFile nd = new DatasetVersionFile();
                nd.setId(newDvfId);
                nd.setDatasetId(newDatasetId);
                nd.setFileId(fileIdMap.get(dvf.getFileId()));
                nd.setVersionName(newDataset.getCurrentVersionName());
                nd.setFileName(dvf.getFileName());
                nd.setStatus(dvf.getStatus() == null ? 2 : dvf.getStatus());
                nd.setAnnotationStatus(dvf.getAnnotationStatus());
                nd.setChanged(dvf.getChanged());
                newDvfs.add(nd);
            }
            datasetVersionFileMapper.insertBatch(newDvfs);
        }

        // 5. 复制标注（versionFileId 重映射，labelId 不变）
        if (!CollectionUtils.isEmpty(originDvfs)) {
            List<Long> originDvfIds = originDvfs.stream().map(DatasetVersionFile::getId).collect(Collectors.toList());
            List<DataFileAnnotation> originAnnotations = dataFileAnnotationMapper.selectList(
                    new LambdaQueryWrapper<DataFileAnnotation>().in(DataFileAnnotation::getVersionFileId, originDvfIds));
            if (!CollectionUtils.isEmpty(originAnnotations)) {
                Queue<Long> newAnnotationIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE_ANNOTATION, originAnnotations.size());
                List<DataFileAnnotation> newAnnotations = new ArrayList<>();
                for (DataFileAnnotation a : originAnnotations) {
                    Long newVersionFileId = dvfIdMap.get(a.getVersionFileId());
                    if (newVersionFileId == null) {
                        continue;
                    }
                    DataFileAnnotation na = DataFileAnnotation.builder()
                            .datasetId(newDatasetId)
                            .fileName(a.getFileName())
                            .labelId(a.getLabelId())
                            .prediction(a.getPrediction())
                            .versionFileId(newVersionFileId)
                            .build();
                    na.setId(newAnnotationIds.poll());
                    na.setStatus(a.getStatus() == null ? 2 : a.getStatus());
                    na.setInvariable(a.getInvariable() == null ? 0 : a.getInvariable());
                    na.setDeleted(false);
                    na.setCreateUserId(userContextService.getCurUserId());
                    na.setUpdateUserId(na.getCreateUserId());
                    newAnnotations.add(na);
                }
                if (!CollectionUtils.isEmpty(newAnnotations)) {
                    dataFileAnnotationService.insertDataFileBatch(newAnnotations);
                }
            }
        }

        // 6. 复制元信息（fileId 重映射）
        List<FileMetadata> originMetadatas = fileMetadataMapper.selectList(
                new LambdaQueryWrapper<FileMetadata>().in(FileMetadata::getFileId, dto.getFileIds()));
        if (!CollectionUtils.isEmpty(originMetadatas)) {
            for (FileMetadata m : originMetadatas) {
                Long newFileId = fileIdMap.get(m.getFileId());
                if (newFileId == null) {
                    continue;
                }
                FileMetadata nm = FileMetadata.builder()
                        .fileId(newFileId)
                        .datasetId(newDatasetId)
                        .sourceType(m.getSourceType())
                        .captureTime(m.getCaptureTime())
                        .device(m.getDevice())
                        .deviceSn(m.getDeviceSn())
                        .location(m.getLocation())
                        .scenario(m.getScenario())
                        .lighting(m.getLighting())
                        .quality(m.getQuality())
                        .build();
                fileMetadataMapper.insert(nm);
            }
        }

        // 7. 复制 MinIO 图片
        for (File f : originFiles) {
            String source = StringUtils.substringAfter(f.getUrl(), "/");
            String fileName = StringUtils.substringAfterLast(f.getUrl(), "/");
            String target = newDataset.getUri() + "/origin/" + fileName;
            minioUtil.copyObject(bucket, source, target);
        }

        return newDatasetId;
    }

    /**
     * 合并文件到已有数据集（复制 data_file + 版本关系 + 标注 + 元信息 + MinIO 图片）
     *
     * @param dto 合并入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeToDataset(MergeToDatasetDTO dto) {
        Long targetDatasetId = dto.getTargetDatasetId();
        Dataset targetDataset = baseMapper.selectById(targetDatasetId);
        if (targetDataset == null) {
            throw new BusinessException("目标数据集不存在");
        }

        // 1. 查原文件
        List<File> originFiles = fileMapper.selectBatchIds(dto.getFileIds());
        if (CollectionUtils.isEmpty(originFiles)) {
            throw new BusinessException("未找到要合并的文件");
        }
        Long originDatasetId = originFiles.get(0).getDatasetId();

        // 2. 复制 data_file（显式 id 映射）
        Map<Long, Long> fileIdMap = new HashMap<>();
        Queue<Long> newFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE, originFiles.size());
        List<File> newFiles = new ArrayList<>();
        for (File f : originFiles) {
            Long newId = newFileIds.poll();
            fileIdMap.put(f.getId(), newId);
            File nf = File.builder()
                    .id(newId)
                    .fileType(f.getFileType())
                    .datasetId(targetDatasetId)
                    .enhanceType(f.getEnhanceType())
                    .frameInterval(f.getFrameInterval())
                    .height(f.getHeight())
                    .name(f.getName())
                    .status(f.getStatus())
                    .pid(f.getPid())
                    .originUserId(f.getOriginUserId())
                    .width(f.getWidth())
                    .url(f.getUrl().replace(originDatasetId + "/", targetDatasetId + "/"))
                    .build();
            nf.setCreateUserId(userContextService.getCurUserId());
            nf.setUpdateUserId(nf.getCreateUserId());
            nf.setDeleted(false);
            newFiles.add(nf);
        }
        fileMapper.insertBatch(newFiles);

        // 3. 复制 DVF（新 id + fileId 重映射）
        List<DatasetVersionFile> originDvfs = datasetVersionFileMapper.selectList(
                new LambdaQueryWrapper<DatasetVersionFile>().in(DatasetVersionFile::getFileId, dto.getFileIds()));
        Map<Long, Long> dvfIdMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(originDvfs)) {
            Queue<Long> newDvfIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_VERSION_FILE, originDvfs.size());
            List<DatasetVersionFile> newDvfs = new ArrayList<>();
            for (DatasetVersionFile dvf : originDvfs) {
                Long newDvfId = newDvfIds.poll();
                dvfIdMap.put(dvf.getId(), newDvfId);
                DatasetVersionFile nd = new DatasetVersionFile();
                nd.setId(newDvfId);
                nd.setDatasetId(targetDatasetId);
                nd.setFileId(fileIdMap.get(dvf.getFileId()));
                nd.setVersionName(targetDataset.getCurrentVersionName());
                nd.setFileName(dvf.getFileName());
                nd.setStatus(dvf.getStatus() == null ? 2 : dvf.getStatus());
                nd.setAnnotationStatus(dvf.getAnnotationStatus());
                nd.setChanged(dvf.getChanged());
                newDvfs.add(nd);
            }
            datasetVersionFileMapper.insertBatch(newDvfs);
        }

        // 4. 复制标注（versionFileId 重映射，labelId 不变）
        if (!CollectionUtils.isEmpty(originDvfs)) {
            List<Long> originDvfIds = originDvfs.stream().map(DatasetVersionFile::getId).collect(Collectors.toList());
            List<DataFileAnnotation> originAnnotations = dataFileAnnotationMapper.selectList(
                    new LambdaQueryWrapper<DataFileAnnotation>().in(DataFileAnnotation::getVersionFileId, originDvfIds));
            if (!CollectionUtils.isEmpty(originAnnotations)) {
                Queue<Long> newAnnotationIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE_ANNOTATION, originAnnotations.size());
                List<DataFileAnnotation> newAnnotations = new ArrayList<>();
                for (DataFileAnnotation a : originAnnotations) {
                    Long newVersionFileId = dvfIdMap.get(a.getVersionFileId());
                    if (newVersionFileId == null) {
                        continue;
                    }
                    DataFileAnnotation na = DataFileAnnotation.builder()
                            .datasetId(targetDatasetId)
                            .fileName(a.getFileName())
                            .labelId(a.getLabelId())
                            .prediction(a.getPrediction())
                            .versionFileId(newVersionFileId)
                            .build();
                    na.setId(newAnnotationIds.poll());
                    na.setStatus(a.getStatus() == null ? 2 : a.getStatus());
                    na.setInvariable(a.getInvariable() == null ? 0 : a.getInvariable());
                    na.setDeleted(false);
                    na.setCreateUserId(userContextService.getCurUserId());
                    na.setUpdateUserId(na.getCreateUserId());
                    newAnnotations.add(na);
                }
                if (!CollectionUtils.isEmpty(newAnnotations)) {
                    dataFileAnnotationService.insertDataFileBatch(newAnnotations);
                }
            }
        }

        // 5. 复制元信息（fileId 重映射）
        List<FileMetadata> originMetadatas = fileMetadataMapper.selectList(
                new LambdaQueryWrapper<FileMetadata>().in(FileMetadata::getFileId, dto.getFileIds()));
        if (!CollectionUtils.isEmpty(originMetadatas)) {
            for (FileMetadata m : originMetadatas) {
                Long newFileId = fileIdMap.get(m.getFileId());
                if (newFileId == null) {
                    continue;
                }
                FileMetadata nm = FileMetadata.builder()
                        .fileId(newFileId)
                        .datasetId(targetDatasetId)
                        .sourceType(m.getSourceType())
                        .captureTime(m.getCaptureTime())
                        .device(m.getDevice())
                        .deviceSn(m.getDeviceSn())
                        .location(m.getLocation())
                        .scenario(m.getScenario())
                        .lighting(m.getLighting())
                        .quality(m.getQuality())
                        .build();
                fileMetadataMapper.insert(nm);
            }
        }

        // 6. 复制 MinIO 图片
        for (File f : originFiles) {
            String source = StringUtils.substringAfter(f.getUrl(), "/");
            String fileName = StringUtils.substringAfterLast(f.getUrl(), "/");
            String target = targetDataset.getUri() + "/origin/" + fileName;
            minioUtil.copyObject(bucket, source, target);
        }
    }

    /**
     * 预置标签处理
     *
     * @param presetLabelType 预置标签类型
     * @param datasetId       数据集id
     */
    @Override
    public void presetLabel(Integer presetLabelType, Long datasetId) {
        List<Label> labels = labelService.listByType(presetLabelType);
        if (CollectionUtil.isNotEmpty(labels)) {
            List<DatasetLabel> datasetLabels = new ArrayList<>();
            labels.stream().forEach(label -> {
                datasetLabels.add(
                        DatasetLabel.builder()
                                .datasetId(datasetId)
                                .labelId(label.getId())
                                .build());
            });
            if (CollectionUtil.isNotEmpty(datasetLabels)) {
                datasetLabelService.saveList(datasetLabels);
            }
        }
    }

    /**
     * 删除数据集
     *
     * @param datasetDeleteDTO 删除数据集条件
     */
    @Override
    public void delete(DatasetDeleteDTO datasetDeleteDTO) {
        if (datasetDeleteDTO.getIds() == null || datasetDeleteDTO.getIds().length == MagicNumConstant.ZERO) {
            return;
        }
        // 创建两个列表
        List<Long> notInDatasetIdList = new ArrayList<>();
        List<Long> inDatasetIdList = new ArrayList<>();
        // 先获取所有标注任务用到的数据集id
        List<Long> datasetIdList = dataTeamTaskMapper.getAllDatasetIdsByTaskId();
        // 将ID分组，分为有标注任务和没有的
        for (Long id : datasetDeleteDTO.getIds()) {
            if (datasetIdList.contains(id)) {
                inDatasetIdList.add(id);
            } else {
                notInDatasetIdList.add(id);
            }
        }
        // 没有标注任务的正常删除
        for (Long id : notInDatasetIdList) {
            delete(id);
        }

        // 清除与数据集组之间的绑定关系
        if (!notInDatasetIdList.isEmpty()) {
            datasetDatasetGroupServiceImpl.unbind(notInDatasetIdList);
         }
        // 判断是否有因为创建了标注任务不能删除的数据集
        if (!inDatasetIdList.isEmpty()) {
            throw new BusinessException(ErrorEnum.DATASET_OCCUPIED_BY_MULTI_ANNO.getMsg() + inDatasetIdList);
        }
    }

    @Override
    public int getCountByLabelGroupIdAndDeleted(Long labelGroupId, boolean deleted) {
        return datasetMapper.getCountByLabelGroupIdAndDeleted(labelGroupId, deleted);
    }

    /**
     * 删除数据集相关信息
     *
     * @param id 数据集id
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAll(Long id) {
        int count = baseMapper.updateStatusById(id, true);
        if (count <= MagicNumConstant.ZERO) {
            throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH);
        }
        // 根据数据集ID删除数据集标签关联数据
        labelService.updateStatusByDatasetId(id, true);

        // 删除版本数据 标注数据
        datasetVersionService.updateStatusByDatasetId(id, true);


    }

    /**
     * 数据集正在自动标注中的文件不允许删除,否则会导致进行中的任务完成的文件数达不到总文件数，无法完成
     *
     * @param fileDeleteDTO 删除文件参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(FileDeleteDTO fileDeleteDTO) {

        for (Long datasetId : fileDeleteDTO.getDatasetIds()) {
            Dataset dataset = getById(datasetId);
            checkPublic(dataset, OperationTypeEnum.UPDATE);
            // 删除文件时，需要对文件做标记
            datasetVersionFileService.deleteShip(
                    datasetId,
                    dataset.getCurrentVersionName(),
                    Arrays.asList(fileDeleteDTO.getFileIds())
            );

            if (dataset.getDataType().equals(DatatypeEnum.AUDIO.getValue())) {
                List<Long> versionFileIdsByFileIds = datasetVersionFileService
                        .getVersionFileIdsByFileIds(datasetId, Arrays.asList(fileDeleteDTO.getFileIds()));
                dataFileAnnotationService.deleteBatch(datasetId, versionFileIdsByFileIds);
            }

            // 改变数据集的状态
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataset});
                setEventMethodName(DataStateMachineConstant.DATA_DELETE_FILES_EVENT);
                setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            }});
//            if (dataset.getDataType().equals(MagicNumConstant.TWO) || dataset.getDataType().equals(MagicNumConstant.THREE)) {
//                fileService.deleteEsData(fileDeleteDTO.getFileIds());
//            }
        }
    }

    @Override
    public void deleteFiles(FileDeleteDTO fileDeleteDTO) {
        fileMapper.deleteFiles(fileDeleteDTO.getFileIds());
    }


    /**
     * 删除数据集
     *
     * @param id 数据集id
     */
    public void delete(Long id) {
        checkPublic(id, OperationTypeEnum.UPDATE);
        // 数据增强中不可以删除
        Dataset dataset = baseMapper.selectById(id);
        if (dataset == null) {
            // 如果数据集库中没有, 别报错了就，反正符合预期效果
            return;
        }
        if (dataset.getStatus().equals(DataStateCodeConstant.STRENGTHENING_STATE)) {
            throw new BusinessException(ErrorEnum.DATASET_ENHANCEMENT);
        }
        if (dataset.getStatus().equals(DataStateCodeConstant.SAMPLING_STATE)) {
            throw new BusinessException(ErrorEnum.DATASET_SAMPLING);
        }
        if (dataset.getStatus().equals(DataStateCodeConstant.ZIP_IMPORT_STATE)) {
            throw new BusinessException(ErrorEnum.DATASET_ZIP_IMPORTING);
        }
        if (dataset.getStatus().equals(DataStateCodeConstant.AUTOMATIC_LABELING_STATE)) {
            throw new BusinessException(ErrorEnum.DATASET_AUTO_LABELING);
        }
        // 内置预置数据集不许删除 如：COCO等
        if (DatasetTypeEnum.PUBLIC.getValue().compareTo(dataset.getType()) == 0 && Objects.isNull(dataset.getSourceId())) {
            throw new BusinessException(ErrorEnum.DATASET_NOT_OPERATIONS_BASE_DATASET);
        }

        // 取出当前数据集信息
        List<DatasetVersionVO> datasetVersionVos = datasetVersionService.versionList(id);
        List<String> datasetVersionUrls = new ArrayList<>();
        datasetVersionVos.forEach(url -> {
            datasetVersionUrls.add(url.getVersionUrl());
            datasetVersionUrls.add(url.getVersionUrl() + StrUtil.SLASH + "ofrecord" + StrUtil.SLASH + "train");
        });
//        if (CollectionUtil.isNotEmpty(datasetVersionUrls)) {
//            //训练中的url进行比较
//            PtTrainDataSourceStatusQueryDTO dto = new PtTrainDataSourceStatusQueryDTO();
//            DataResponseBody<Map<String, Boolean>> trainDataSourceStatusData = trainServiceClient.getTrainDataSourceStatus(dto.setDataSourcePath(datasetVersionUrls));
//            if (!trainDataSourceStatusData.succeed() || Objects.isNull(trainDataSourceStatusData.getData())) {
//                throw new BusinessException(ErrorEnum.DATASET_VERSION_PTJOB_STATUS);
//            }
//            if (!trainDataSourceStatusData.getData().values().contains(false)) {
//                ((DatasetServiceImpl) AopContext.currentProxy()).deleteAll(id);
//            }
//        } else {
//            ((DatasetServiceImpl) AopContext.currentProxy()).deleteAll(id);
//        }
        // 暂时直接删除
        ((DatasetServiceImpl) AopContext.currentProxy()).deleteAll(id);
        // 添加回收数据
        try {
            addRecycleDataByDeleteDataset(dataset);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO delete the dataset file error", e);
        }
//        if (dataset.getDataType().equals(DatatypeEnum.TEXT.getValue()) || dataset.getDataType().equals(DatatypeEnum.TABLE.getValue())) {
//            DeleteByQueryRequest deleteRequest = new DeleteByQueryRequest(esIndex);
//            deleteRequest.setQuery(new TermQueryBuilder("datasetId", dataset.getId().toString()));
//            try {
//                restHighLevelClient.deleteByQuery(deleteRequest, RequestOptions.DEFAULT);
//            } catch (IOException e) {
//                LogUtil.error(LogEnum.BIZ_DATASET, "delete es data error:{}", e);
//            }
//        }
    }


    /**
     * 添加回收数据
     *
     * @param dataset 数据集实体
     */
    private void addRecycleDataByDeleteDataset(Dataset dataset) {

        // 落地回收详情数据文件回收信息
        List<RecycleDetailCreateDTO> detailList = new ArrayList<>();
        detailList.add(RecycleDetailCreateDTO.builder()
                .recycleCondition(dataset.getId().toString())
                .recycleType(RecycleTypeEnum.TABLE_DATA.getCode())
                .recycleNote(RecycleTool.generateRecycleNote("落地 数据集DB 数据文件回收", dataset.getId()))
                .build());
        // 落地回收详情minio 数据文件回收信息
        if (!Objects.isNull(dataset.getUri())) {
            detailList.add(RecycleDetailCreateDTO.builder()
                    .recycleCondition(prefixPath + bucket + SymbolConstant.SLASH + dataset.getUri())
                    .recycleType(RecycleTypeEnum.FILE.getCode())
                    .recycleNote(RecycleTool.generateRecycleNote("落地 minio 数据文件回收", dataset.getId()))
                    .build());
        }
        // 落地回收信息
        RecycleCreateDTO recycleCreateDTO = RecycleCreateDTO.builder()
                .recycleModule(RecycleModuleEnum.BIZ_DATASET.getValue())
                .recycleCustom(RecycleResourceEnum.DATASET_RECYCLE_FILE.getClassName())
                .restoreCustom(RecycleResourceEnum.DATASET_RECYCLE_FILE.getClassName())
                .recycleDelayDate(NumberConstant.NUMBER_1)
                .recycleNote(RecycleTool.generateRecycleNote("删除数据集相关信息", dataset.getName(), dataset.getId()))
                .detailList(detailList)
                .build();
        recycleService.createRecycleTask(recycleCreateDTO);
    }


    /**
     * 数据集查询
     *
     * @param page            分页信息
     * @param datasetQueryDTO 查询条件
     * @return MapMap<String, Object> 查询出对应的数据集
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Map<String, Object> listVO(Page<Dataset> page, DatasetQueryDTO datasetQueryDTO) {

        String name = datasetQueryDTO.getName();
        if (StringUtils.isEmpty(name)) {
            if (datasetQueryDTO.getCreatorID() != null) {
                int creatorId = datasetQueryDTO.getCreatorID();
                String roleName = userContextService.getCurUser().getRoles().get(0).getName();
                if (("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    datasetQueryDTO.setCreatorID(null);
                }
            }
            return queryDatasets(page, datasetQueryDTO, null);
        }
        boolean nameFlag = PATTERN_NUM.matcher(name).matches();
        if (nameFlag) {

            DatasetQueryDTO queryCriteriaId = new DatasetQueryDTO();
            BeanUtils.copyProperties(datasetQueryDTO, queryCriteriaId);
            queryCriteriaId.setName(null);
            Set<Long> ids = new HashSet<>();
            ids.add(Long.parseLong(datasetQueryDTO.getName()));
            queryCriteriaId.setIds(ids);
            if (datasetQueryDTO.getCreatorID() != null) {
                int creatorId = datasetQueryDTO.getCreatorID();
                String roleName = userContextService.getCurUser().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    queryCriteriaId.setCreatorID(creatorId);
                }
            }
            Map<String, Object> map = queryDatasets(page, queryCriteriaId, null);
            if (((List) map.get(RESULT)).size() > 0) {
                queryCriteriaId.setName(name);
                queryCriteriaId.setIds(null);
                return queryDatasets(page, queryCriteriaId, Long.parseLong(datasetQueryDTO.getName()));
            }
        }
        datasetQueryDTO.timeConvert();
        return queryDatasets(page, datasetQueryDTO, null);
    }



    @Override
    public Map<String, Object> getAllPublicDatasets(Page<Dataset> page, DatasetQueryDTO datasetQueryDTO) {
        String name = datasetQueryDTO.getName();
        if (StringUtils.isEmpty(name)) {
            return queryPublicDatasets(page, datasetQueryDTO, null);
        }
        boolean nameFlag = PATTERN_NUM.matcher(name).matches();
        if (nameFlag) {
            DatasetQueryDTO queryCriteriaId = new DatasetQueryDTO();
            BeanUtils.copyProperties(datasetQueryDTO, queryCriteriaId);
            queryCriteriaId.setName(null);
            Set<Long> ids = new HashSet<>();
            ids.add(Long.parseLong(datasetQueryDTO.getName()));
            queryCriteriaId.setIds(ids);
            Map<String, Object> map = queryDatasets(page, queryCriteriaId, null);
            if (!((List<?>) map.get(RESULT)).isEmpty()) {
                queryCriteriaId.setName(name);
                queryCriteriaId.setIds(null);
                return queryPublicDatasets(page, queryCriteriaId, Long.parseLong(datasetQueryDTO.getName()));
            }
        }
        return queryPublicDatasets(page, datasetQueryDTO, null);
    }

    /**
     * 查询数据集列表
     *
     * @param page          分页信息
     * @param queryCriteria 查询条件
     * @param datasetId     数据集id
     * @return java.util.Map<java.lang.String, java.lang.Object> 数据集列表
     */
    public Map<String, Object> queryDatasets(Page<Dataset> page, DatasetQueryDTO queryCriteria, Long datasetId) {
        queryCriteria.timeConvert();
        QueryWrapper<Dataset> datasetQueryWrapper = WrapperHelp.getWrapper(queryCriteria);
        datasetQueryWrapper.eq("deleted", MagicNumConstant.ZERO);
        if (datasetId != null) {
            datasetQueryWrapper.or().eq("id", datasetId);
        }
        if (!CollectionUtils.isEmpty(queryCriteria.getUpdateTimeSearch())) {
            datasetQueryWrapper.between("update_time", queryCriteria.getUpdateTimeSearch().get(0), queryCriteria.getUpdateTimeSearch().get(1));
        }
        if (StringUtils.isNotEmpty(queryCriteria.getSort()) && StringUtils.isNotEmpty(queryCriteria.getOrder())) {
            datasetQueryWrapper.orderByDesc("is_top").orderBy(
                    true,
                    SORT_ASC.equals(queryCriteria.getOrder().toLowerCase()),
                    StringUtils.humpToLine(queryCriteria.getSort())
            );
        } else {
            datasetQueryWrapper.orderByDesc("is_top", "update_time");
        }
        if (!Objects.isNull(queryCriteria.getType()) && queryCriteria.getType().compareTo(DatasetTypeEnum.PUBLIC.getValue()) == 0) {
            DataContext.set(CommonPermissionDataDTO.builder().id(datasetId).type(true).build());
        }

        // 执行主查询，获取分页的 Dataset 列表
        page = getBaseMapper().listPageWithUser(page, datasetQueryWrapper);
        List<Dataset> records = page.getRecords();

        if (CollectionUtils.isEmpty(records)) {
            return PageUtil.toPage(page); // 如果没有数据，直接返回空分页
        }


        // 2.1 批量获取并关联 Dataset Groups 信息
        Map<Long, List<GroupInfoDTO>> datasetGroupsMap = getDatasetGroupsMap(records);

        // 2.2 批量获取 LabelGroup 逻辑
        Map<Long, LabelGroup> labelGroupMap = getLabelGroupMap(records);

        // 2.3 批量获取其他信息 (例如进度统计)
        Map<Long, ProgressVO> statistics = newProgressVO(records);

        List<DatasetVO> datasetVOS = new ArrayList<>();
        for (Dataset dataset : records) {
            DatasetVO datasetVO = buildDatasetVO(dataset, null, null);
            datasetVO.setCreatorName(dataset.getCreateUser().getUsername());

            // 3.1 从 Map 中设置数据集组信息
            datasetVO.setGroups(datasetGroupsMap.getOrDefault(dataset.getId(), Collections.emptyList()));

            // 3.2 从 Map 中设置 LabelGroup 信息
            if (dataset.getLabelGroupId() != null && labelGroupMap.containsKey(dataset.getLabelGroupId())) {
                LabelGroup labelGroup = labelGroupMap.get(dataset.getLabelGroupId());
                datasetVO.setLabelGroupName(labelGroup.getName());
                datasetVO.setLabelGroupType(labelGroup.getType());
                datasetVO.setAutoAnnotation(labelGroup.getType() == MagicNumConstant.ONE);
            }

            // 3.3 设置其他信息
            datasetVO.setProgress(statistics.get(dataset.getId()));
            if (dataset.getCurrentVersionName() != null) {
                // 注意: 这里仍然是 N+1 查询，如果性能要求高，也需要批量获取
                DatasetVersion datasetVersion = datasetVersionService
                        .getVersionByDatasetIdAndVersionName(dataset.getId(), dataset.getCurrentVersionName());
                if(datasetVersion != null) {
                    datasetVO.setDataConversion(datasetVersion.getDataConversion());
                }
            }

            // 注意: 下面的文件数量查询也是严重的 N+1 问题，建议也用批量方式优化
            datasetVO.setFileCount(datasetVersionFileService.getFileCountByDatasetIdAndVersion(new LambdaQueryWrapper<DatasetVersionFile>() {{
                eq(DatasetVersionFile::getDatasetId, datasetVO.getId());
                if ((datasetVO.getCurrentVersionName() == null)) {
                    isNull(DatasetVersionFile::getVersionName);
                } else {
                    eq(DatasetVersionFile::getVersionName, datasetVO.getCurrentVersionName());
                }
                ne(DatasetVersionFile::getStatus, DataStatusEnum.DELETE.getValue());
            }}));

            datasetVOS.add(datasetVO);
        }

        BaseService.removeContext();
        return PageUtil.toPage(page, datasetVOS);
    }


    /**
     * 批量获取并构建 DatasetId -> List<GroupInfoDTO> 的映射
     * @param datasets 当前页的数据集列表
     * @return Map<数据集ID, List<数据集组DTO>>
     */
    private Map<Long, List<GroupInfoDTO>> getDatasetGroupsMap(List<Dataset> datasets) {
        // 提取当前页所有数据集的ID
        List<Long> datasetIds = datasets.stream().map(Dataset::getId).collect(Collectors.toList());

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
        Map<Long, DatasetGroup> groupDetailsMap = datasetGroupServiceImpl.listByIds(groupIds).stream()
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

    /**
     * 批量获取并构建 LabelGroupId -> LabelGroup 的映射 (优化你原来的逻辑)
     * @param datasets 当前页的数据集列表
     * @return Map<标签组ID, 标签组对象>
     */
    private Map<Long, LabelGroup> getLabelGroupMap(List<Dataset> datasets) {
        Set<Long> groupIds = datasets.stream()
                .map(Dataset::getLabelGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (CollectionUtils.isEmpty(groupIds)) {
            return Collections.emptyMap();
        }

        return labelGroupService.listByIds(groupIds).stream()
                .collect(Collectors.toMap(LabelGroup::getId, g -> g));
    }

    /**
     * 查询数据集列表
     *
     * @param page          分页信息
     * @param queryCriteria 查询条件
     * @param datasetId     数据集id
     * @return java.util.Map<java.lang.String, java.lang.Object> 数据集列表
     */
    public Map<String, Object> queryPublicDatasets(Page<Dataset> page, DatasetQueryDTO queryCriteria, Long datasetId) {
        queryCriteria.timeConvert();
        QueryWrapper<Dataset> datasetQueryWrapper = WrapperHelp.getWrapper(queryCriteria);
        datasetQueryWrapper.eq("deleted", MagicNumConstant.ZERO);
        // 添加对于 是否公开 的判断
        datasetQueryWrapper.eq("is_public", Dataset.IS_PUBLIC);
        if (datasetId != null) {
            datasetQueryWrapper.or().eq("id", datasetId);
        }
        if (StringUtils.isNotEmpty(queryCriteria.getSort()) && StringUtils.isNotEmpty(queryCriteria.getOrder())) {
            datasetQueryWrapper.orderByDesc("is_top").orderBy(
                    true,
                    SORT_ASC.equals(queryCriteria.getOrder().toLowerCase()),
                    StringUtils.humpToLine(queryCriteria.getSort())
            );
        } else {
            datasetQueryWrapper.orderByDesc("is_top", "update_time");
        }

        // 预置数据集类型校验
        if (!Objects.isNull(queryCriteria.getType()) && queryCriteria.getType().compareTo(DatasetTypeEnum.PUBLIC.getValue()) == 0) {
            DataContext.set(CommonPermissionDataDTO.builder().id(datasetId).type(true).build());
        }
        page = getBaseMapper().listPage(page, datasetQueryWrapper);

        Map<Long, ProgressVO> statistics = newProgressVO(page.getRecords());

        List<DatasetVO> datasetVOS = new ArrayList<>();

        // 构建数据集列表
        if (!CollectionUtils.isEmpty(page.getRecords())) {
            List<Long> groupIds = page.getRecords().stream().map(a -> a.getLabelGroupId()).collect(Collectors.toList());
            Map<Long, List<LabelGroup>> groupListMap = new HashMap<>(groupIds.size());
            if (!CollectionUtils.isEmpty(groupIds)) {
                List<LabelGroup> labelGroups = labelGroupService.getBaseMapper().selectBatchIds(groupIds);
                if (!CollectionUtils.isEmpty(labelGroups)) {
                    groupListMap = labelGroups.stream().collect(Collectors.groupingBy(LabelGroup::getId));
                }
            }
            List<Dataset> records = page.getRecords();
            if (!CollectionUtils.isEmpty(records)) {
                for (Dataset dataset : records) {
                    DatasetVO datasetVO = buildDatasetVO(dataset, null, null);
                    if (dataset.getCurrentVersionName() != null) {
                        DatasetVersion datasetVersion = datasetVersionService
                                .getVersionByDatasetIdAndVersionName(dataset.getId(), dataset.getCurrentVersionName());
                        datasetVO.setDataConversion(datasetVersion.getDataConversion());
                    }
                    datasetVO.setProgress(statistics.get(datasetVO.getId()));
//                    String creatorName = userClient.getNameById(dataset.getCreateUserId());

                    if (!Objects.isNull(groupListMap) && !Objects.isNull(dataset.getLabelGroupId()) &&
                            !Objects.isNull(groupListMap.get(dataset.getLabelGroupId()))) {
                        LabelGroup labelGroup = groupListMap.get(dataset.getLabelGroupId()).get(0);
                        datasetVO.setLabelGroupName(labelGroup.getName());
                        datasetVO.setLabelGroupType(labelGroup.getType());
                        datasetVO.setUpdateTime(dataset.getUpdateTime());
                        datasetVO.setAutoAnnotation(labelGroup.getType() == MagicNumConstant.ONE);

                    }
                    // 把这一行拿到外面防止走LabelGroup的相关逻辑
                    datasetVO.setCreatorName(dataset.getCreateUser().getUsername());
                    datasetVOS.add(datasetVO);
                }
            }

        }

        // 处理数据集文件数量
        if (CollectionUtil.isNotEmpty(datasetVOS)) {
            for (DatasetVO datasetVo : datasetVOS) {
                datasetVo.setFileCount(datasetVersionFileService.getFileCountByDatasetIdAndVersion(new LambdaQueryWrapper<DatasetVersionFile>() {{
                    eq(DatasetVersionFile::getDatasetId, datasetVo.getId());
                    if ((datasetVo.getCurrentVersionName() == null)) {
                        isNull(DatasetVersionFile::getVersionName);
                    } else {
                        eq(DatasetVersionFile::getVersionName, datasetVo.getCurrentVersionName());
                    }
                    ne(DatasetVersionFile::getStatus, DataStatusEnum.DELETE.getValue());
                }}));
            }
        }
        BaseService.removeContext();
        return PageUtil.toPage(page, datasetVOS);
    }

    /**
     * 获取数据集标注进度接口
     *
     * @param datasetIds 数据集id列表
     * @return Map<Long, ProgressVO> 数据集文件进度
     */
    @Override
    public Map<Long, ProgressVO> progress(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return Collections.emptyMap();
        }
        List<Dataset> datasets = new ArrayList<>();
        datasetIds.forEach(datasetId -> {
            Dataset dataset = getBaseMapper().selectById(datasetId);
            datasets.add(dataset);
        });
        return fileService.listStatistics(datasets);
    }

    /**
     * 取消页面列表数据集文件数量
     *
     * @param datasets 数据集列表
     * @return Map<Long, ProgressVO> 数据集文件进度
     */
    public Map<Long, ProgressVO> newProgressVO(List<Dataset> datasets) {
        Map<Long, ProgressVO> res = new HashMap<>(datasets.size());
        datasets.forEach(dataset -> {
            ProgressVO progressVO = null;
            res.put(dataset.getId(), progressVO);
        });
        return res;
    }

    /**
     * 文件提交
     *
     * @param datasetId          数据集id
     * @param batchFileCreateDTO 保存的文件
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void uploadFiles(Long datasetId, BatchFileCreateDTO batchFileCreateDTO) {
        Dataset dataset = baseMapper.selectById(datasetId);
        List<Long> fileIds = saveDbForUploadFiles(datasetId, batchFileCreateDTO, batchFileCreateDTO.getIfImport());
        LogUtil.info(LogEnum.BIZ_DATASET, "uploadFiles saveDb finished: datasetId={}, savedFileCount={}",
                datasetId, fileIds == null ? 0 : fileIds.size());
        if (batchFileCreateDTO.getIfImport() != null && batchFileCreateDTO.getIfImport()) {
            importFileAnnotation(datasetId, fileIds);
        }
        // 改变数据集的状态
        LogUtil.info(LogEnum.BIZ_DATASET, "uploadFiles stateChange start: datasetId={}, datasetStatus={}, currentVersionName={}",
                datasetId,
                dataset == null ? null : dataset.getStatus(),
                dataset == null ? null : dataset.getCurrentVersionName());
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.DATA_UPLOAD_FILES_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
        LogUtil.info(LogEnum.BIZ_DATASET, "uploadFiles stateChange finished: datasetId={}", datasetId);
    }

    @Transactional(rollbackFor = Exception.class)
    //不带状态机的
    public void uploadFilesWithoutState(Long datasetId, BatchFileCreateDTO batchFileCreateDTO) {
        Dataset dataset = baseMapper.selectById(datasetId);
        List<Long> fileIds = saveDbForUploadFiles(datasetId, batchFileCreateDTO, batchFileCreateDTO.getIfImport());
        if (batchFileCreateDTO.getIfImport() != null && batchFileCreateDTO.getIfImport()) {
            importFileAnnotation(datasetId, fileIds);
        }
    }

    /**
     * 从数据集导入到标注任务（文件信息入库+状态转换）
     *
     * @param datasetId          数据集id
     * @param batchFileCreateDTO 保存的文件
     */
    public void uploadFilesFromDataRepo(Long datasetId, BatchFileCreateDTO batchFileCreateDTO) {
        Dataset dataset = baseMapper.selectById(datasetId);
        List<Long> fileIds = saveDbForUploadFiles(datasetId, batchFileCreateDTO, batchFileCreateDTO.getIfImport());
        if (batchFileCreateDTO.getIfImport() != null && batchFileCreateDTO.getIfImport()) {
            importFileAnnotation(datasetId, fileIds);
        }
        // 改变数据集的状态
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
    }

    void importFileAnnotation(Long datasetId, List<Long> fileIds) {
        List<Long> versionFileIds = datasetVersionFileService.getVersionFileIdsByFileIds(datasetId, fileIds);
        List<FileUploadBO> fileUploadContent = datasetVersionFileService.getFileUploadContent(datasetId, fileIds);
        List<DataFileAnnotation> dataFileAnnotations = new ArrayList<>();
        fileUploadContent.forEach(fileUploadBO -> {
            String annPath = StringUtils.substringBeforeLast(fileUploadBO.getFileUrl(), ".");
            annPath = annPath.replace("/origin/", "/annotation/").replace(bucket + "/", "");
            try {
                JSONArray annJsonArray = JSONObject.parseArray((minioUtil.readString(bucket, annPath)));
                for (Object object : annJsonArray) {
                    JSONObject jsonObject = (JSONObject) object;
                    Long categoryId = Long.parseLong(jsonObject.getString("category_id"));
                    Double score = jsonObject.getString("score") == null ? null : Double.parseDouble(jsonObject.getString("score"));
                    DataFileAnnotation dataFileAnnotation = DataFileAnnotation.builder().fileName(fileUploadBO.getFileName())
                            .versionFileId(fileUploadBO.getVersionFileId())
                            .datasetId(datasetId)
                            .labelId(categoryId)
                            .prediction(score).build();
                    dataFileAnnotations.add(dataFileAnnotation);
                }
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "导入数据集读取标注出错:{}", e);
            }
        });
        if (!CollectionUtils.isEmpty(dataFileAnnotations)) {
            Queue<Long> dataFileAnnotionIds = generatorKeyUtil.getSequenceByBusinessCode(DATA_FILE_ANNOTATION, dataFileAnnotations.size());
            for (DataFileAnnotation dataFileAnnotation : dataFileAnnotations) {
                dataFileAnnotation.setId(dataFileAnnotionIds.poll());
                dataFileAnnotation.setStatus(MagicNumConstant.ZERO);
                dataFileAnnotation.setInvariable(MagicNumConstant.ZERO);
            }
            dataFileAnnotationService.insertDataFileBatch(dataFileAnnotations);
        }
    }

    /**
     * 上传文件数据之数据库保存
     *
     * @param datasetId
     * @param batchFileCreateDTO
     */
//    @Transactional(rollbackFor = Exception.class)
    public List<Long> saveDbForUploadFiles(Long datasetId, BatchFileCreateDTO batchFileCreateDTO, Boolean ifImport) {
        Dataset dataset = getBaseMapper().selectById(datasetId);
        if (null == dataset) {
            throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH, "id:" + datasetId, null);
        }
        checkPublic(datasetId, OperationTypeEnum.UPDATE);
        autoAnnotatingCheck(datasetId);
        LogUtil.info(LogEnum.BIZ_DATASET, "saveDbForUploadFiles start: datasetId={}, currentVersionName={}, datasetStatus={}, fileCount={}",
                datasetId, dataset.getCurrentVersionName(), dataset.getStatus(),
                batchFileCreateDTO.getFiles() == null ? 0 : batchFileCreateDTO.getFiles().size());
        List<File> list = fileService.saveFiles(datasetId, batchFileCreateDTO.getFiles());
        List<Long> fileIds = new ArrayList<>();
        list.forEach(file -> fileIds.add(file.getId()));
        if (!CollectionUtils.isEmpty(list)) {
            List<DatasetVersionFile> datasetVersionFiles = new ArrayList<>();
            for (File file : list) {
                DatasetVersionFile datasetVersionFile = new DatasetVersionFile(datasetId, dataset.getCurrentVersionName(), file.getId(), file.getName());
                if (ifImport != null && ifImport) {
                    datasetVersionFile.setAnnotationStatus(FileTypeEnum.FINISHED.getValue());
                }
                datasetVersionFiles.add(datasetVersionFile);
            }
            datasetVersionFileService.insertList(datasetVersionFiles);
            LogUtil.info(LogEnum.BIZ_DATASET, "saveDbForUploadFiles versionFile insert finished: datasetId={}, versionFileCount={}",
                    datasetId, datasetVersionFiles.size());
        }
        // 保存文件元信息（可选，供数据筛选使用）
        if (batchFileCreateDTO.getMetadata() != null && !CollectionUtils.isEmpty(fileIds)) {
            fileMetadataService.saveBatch(fileIds, datasetId, batchFileCreateDTO.getMetadata());
        }
        if (DataStateCodeConstant.NOT_ANNOTATION_STATE.equals(dataset.getStatus())
                || DataStateCodeConstant.MANUAL_ANNOTATION_STATE.equals(dataset.getStatus())) {
            return fileIds;
        }
        return fileIds;
    }

//    /**
//     * 上传文本文件数据之保存数据到ES
//     *
//     * @param datasetId 数据集ID
//     */
//    public void transportTextToEsForUploadFiles(Long datasetId, List<Long> fileIds, Boolean ifImport) {
//        Dataset dataset = getBaseMapper().selectById(datasetId);
//        if (dataset.getDataType().equals(MagicNumConstant.TWO) || dataset.getDataType().equals(MagicNumConstant.THREE)) {
//            fileService.transportTextToEs(dataset, fileIds, ifImport);
//        }
//    }

    /**
     * 上传视频
     *
     * @param datasetId          数据集id
     * @param batchFileCreateDTO 文件参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void uploadVideo(Long datasetId, BatchFileCreateDTO batchFileCreateDTO) {
        batchFileCreateDTO.getFiles().forEach(fileCreateDTO -> {
            if (!exist(datasetId)) {
                throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH, "id:" + datasetId, null);
            }
            checkPublic(datasetId, OperationTypeEnum.UPDATE);
            autoAnnotatingCheck(datasetId);
//        fileService.isExistVideo(datasetId);
            List<FileCreateDTO> videoFile = new ArrayList<>();
            videoFile.add(fileCreateDTO);
            List<File> files = fileService.saveVideoFiles(datasetId, videoFile, DatatypeEnum.VIDEO.getValue(), PID_OF_VIDEO, null);
//            //将任务存放入redis队列中
//            Task task = Task.builder()
//                    .datasets(JSON.toJSONString(Collections.singletonList(datasetId)))
//                    .files(JSON.toJSONString(Collections.EMPTY_LIST))
//                    .labels(JSONArray.toJSONString(Collections.emptyList()))
//                    .annotateType(MagicNumConstant.SIX)
//                    .dataType(MagicNumConstant.ONE)
//                    .datasetId(datasetId)
//                    .type(MagicNumConstant.FIVE)
//                    .url(fileCreateDTO.getUrl())
//                    .targetId(files.get(0).getId())
//                    .frameInterval(fileCreateDTO.getFrameInterval()).build();
//            taskMapper.insert(task);
        });
//        //创建入参请求体
//        StateChangeDTO stateChangeDTO = new StateChangeDTO();
//        //创建需要执行事件的方法的传入参数
//        Object[] objects = new Object[1];
//        objects[0] = datasetId.intValue();
//        stateChangeDTO.setObjectParam(objects);
//        //添加需要执行的状态机类
//        stateChangeDTO.setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
//        //采样事件
//        stateChangeDTO.setEventMethodName(DataStateMachineConstant.DATA_SAMPLED_EVENT);
//        StateMachineUtil.stateChange(stateChangeDTO);
    }

    // 数据集导入视频
    public void uploadVideoFromDataRepo(Long datasetId, BatchFileCreateDTO batchFileCreateDTO) {
        batchFileCreateDTO.getFiles().forEach(fileCreateDTO -> {
            if (!exist(datasetId)) {
                throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH, "id:" + datasetId, null);
            }
            checkPublic(datasetId, OperationTypeEnum.UPDATE);
            autoAnnotatingCheck(datasetId);
            List<FileCreateDTO> videoFile = new ArrayList<>();
            videoFile.add(fileCreateDTO);
            List<File> files = fileService.saveVideoFiles(datasetId, videoFile, DatatypeEnum.VIDEO.getValue(), PID_OF_VIDEO, null);
        });

        Dataset dataset = baseMapper.selectById(datasetId);
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});

    }

    /**
     * 采样视频
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sampleVideo(Long datasetId, Long fileId, Integer frameInterval, Integer startTime, Integer endTime, Integer resolutionType, Integer customWidth, Integer customHeight) {
        //打印接收到的参数信息
        log.info("impl接收到的参数");
        log.info("datasetId:{},fileId:{},frameInterval:{},startTime:{},endTime:{},resolutionType:{},customWidth:{},customHeight:{}", datasetId, fileId, frameInterval, startTime, endTime, resolutionType, customWidth, customHeight);
        //将数据集的原始状态保存到Redis中，键为"datasetOriginStatus"加上数据集ID，值为数据集当前状态，过期时间为1小时（60*60秒）。这是为了在抽帧过程中如果出现问题，能够恢复到原来的状态。
        redisUtils.hset("datasetOriginStatus", String.valueOf(datasetId), datasetMapper.selectById(datasetId).getStatus(), 60 * 60);
        if (!exist(datasetId)) {
            throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH, "id:" + datasetId, null);
        }
        checkPublic(datasetId, OperationTypeEnum.UPDATE);
        autoAnnotatingCheck(datasetId);

        //通过文件服务根据文件ID和数据集ID获取对应的视频文件对象
        File file = fileService.selectById(fileId, datasetId);
        // 构建分辨率参数JSON
        JSONObject resolutionParams = new JSONObject();
        resolutionParams.put("resolutionType", Optional.ofNullable(resolutionType).orElse(1));
        if (customWidth != null) {
            resolutionParams.put("customWidth", customWidth);
        }
        if (customHeight != null) {
            resolutionParams.put("customHeight", customHeight);
        }
        
        // 将任务存放入redis队列中
        Task task = Task.builder()
                .datasets(JSON.toJSONString(Collections.singletonList(datasetId)))
                .files(JSON.toJSONString(Collections.EMPTY_LIST))
                .labels(JSONArray.toJSONString(Collections.emptyList()))
                .annotateType(MagicNumConstant.SIX)
                .dataType(MagicNumConstant.ONE)
                .datasetId(datasetId)
                .type(MagicNumConstant.FIVE)
                .url(file.getUrl())
                .targetId(file.getId())
                .status(MagicNumConstant.ZERO)
                .stop(false)
                .createTime(new Date())
                .startTime(startTime)
                .endTime(endTime)
                .frameInterval(frameInterval)
                .taskParams(resolutionParams.toJSONString())
                .resolutionType(resolutionType)
                .customWidth(customWidth)
                .customHeight(customHeight).build();
        log.info("task:{}", task);
        taskMapper.insert(task);
        log.info("任务插入成功，任务ID: {}", task.getId());
        // 数据集状态为采样中则不变更状态机
        Dataset dataset = datasetMapper.selectById(datasetId);
        if (!Objects.equals(dataset.getStatus(), DataStateCodeConstant.SAMPLING_STATE)) {
            // 创建入参请求体
            StateChangeDTO stateChangeDTO = new StateChangeDTO();
            // 创建需要执行事件的方法的传入参数
            Object[] objects = new Object[1];
            objects[0] = datasetId.intValue();
            stateChangeDTO.setObjectParam(objects);
            // 添加需要执行的状态机类
            stateChangeDTO.setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            // 采样事件
            stateChangeDTO.setEventMethodName(DataStateMachineConstant.DATA_SAMPLED_EVENT);
            StateMachineUtil.stateChange(stateChangeDTO);
        }
        datasetOperationEventService.createAndInsertEvent(datasetId,new Date(),"视频抽帧任务提交", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.VIDEO_FRAME_EXTRACTION);
    }

    /**
     * 批量采样视频
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void sampleVideoList(Long datasetId, List<Long> fileIds, Integer frameInterval) {
        fileIds.forEach(fileId -> {
            if (!exist(datasetId)) {
                throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH, "id:" + datasetId, null);
            }
            checkPublic(datasetId, OperationTypeEnum.UPDATE);
            autoAnnotatingCheck(datasetId);

            File file = fileService.selectById(fileId, datasetId);
            // 将任务存放入redis队列中
            Task task = Task.builder()
                    .datasets(JSON.toJSONString(Collections.singletonList(datasetId)))
                    .files(JSON.toJSONString(Collections.EMPTY_LIST))
                    .labels(JSONArray.toJSONString(Collections.emptyList()))
                    .annotateType(MagicNumConstant.SIX)
                    .dataType(MagicNumConstant.ONE)
                    .datasetId(datasetId)
                    .type(MagicNumConstant.FIVE)
                    .url(file.getUrl())
                    .targetId(file.getId())
                    .frameInterval(frameInterval).build();
            taskMapper.insert(task);
        });
        // 数据集状态为采样中则不变更状态机
        Dataset dataset = datasetMapper.selectById(datasetId);
        if (!Objects.equals(dataset.getStatus(), DataStateCodeConstant.SAMPLING_STATE)) {
            // 创建入参请求体
            StateChangeDTO stateChangeDTO = new StateChangeDTO();
            // 创建需要执行事件的方法的传入参数
            Object[] objects = new Object[1];
            objects[0] = datasetId.intValue();
            stateChangeDTO.setObjectParam(objects);
            // 添加需要执行的状态机类
            stateChangeDTO.setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            // 采样事件
            stateChangeDTO.setEventMethodName(DataStateMachineConstant.DATA_SAMPLED_EVENT);
            StateMachineUtil.stateChange(stateChangeDTO);
        }
    }

    /**
     * 判断数据集是否存在
     *
     * @param id 数据集id
     * @return boolean 判断结果
     */
    public boolean exist(Long id) {
        return getBaseMapper().selectById(id) != null;
    }

    /**
     * 修改数据集当前版本
     *
     * @param id          数据集id
     * @param versionName 版本名称
     */
    public void updateVersionName(Long id, String versionName) {
        baseMapper.updateVersionName(id, versionName);
    }


    /**
     * 查询有版本的数据集
     *
     * @param datasetIsVersionDTO 查询数据集(有版本)条件
     * @return Map<String, Object> 查询数据集(有版本)列表
     */
    @Override
    public Map<String, Object> dataVersionListVO(Page page, DatasetIsVersionDTO datasetIsVersionDTO) {
        Integer annotateType = AnnotateTypeEnum.getConvertAnnotateType(datasetIsVersionDTO.getAnnotateType());
        LambdaQueryWrapper<Dataset> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Dataset::getDeleted, MagicNumConstant.ZERO);
        lambdaQueryWrapper.isNotNull(Dataset::getCurrentVersionName);
        lambdaQueryWrapper.eq(ObjectUtil.isNotNull(annotateType), Dataset::getAnnotateType, annotateType);
        lambdaQueryWrapper.eq(ObjectUtil.isNotNull(datasetIsVersionDTO.getModule()), Dataset::getModule, datasetIsVersionDTO.getModule());
        lambdaQueryWrapper.in(CollectionUtil.isNotEmpty(datasetIsVersionDTO.getIds()), Dataset::getId, datasetIsVersionDTO.getIds());
        if (!BaseService.isAdmin()) {
            lambdaQueryWrapper.and(datasetLambdaQueryWrapper ->
                    datasetLambdaQueryWrapper.eq(Dataset::getType, DatasetTypeEnum.PUBLIC.getValue())
                            .or().eq(Dataset::getOriginUserId, userContextService.getCurUserId()));
        }
        List<Dataset> datasetList = baseMapper.selectList(lambdaQueryWrapper);
        return PageUtil.toPage(page, datasetList);
    }

    /**
     * 数据扩容
     *
     * @param datasetEnhanceRequestDTO 数据集增强请求详情
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void enhance(DatasetEnhanceRequestDTO datasetEnhanceRequestDTO) {
        // 判断数据集是否存在
        Dataset dataset = getById(datasetEnhanceRequestDTO.getDatasetId());
        if (ObjectUtil.isNull(dataset)) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT);
        }
        if (CollectionUtil.isEmpty(datasetEnhanceRequestDTO.getTypes())) {
            throw new BusinessException(ErrorEnum.DATASET_LABEL_EMPTY);
        }
        // 判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (datasetVersionService.getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        if (fileService.getOriginalFileCountOfDataset(datasetEnhanceRequestDTO.getDatasetId()
                , dataset.getCurrentVersionName()) == MagicNumConstant.ZERO) {
            throw new BusinessException(ErrorEnum.DATASET_ORIGINAL_FILE_IS_EMPTY);
        }
        // 只有是完成状态的数据集才可以进行增强操作
        DataStateEnum dataStateEnum = stateIdentify.getStatus(
                datasetEnhanceRequestDTO.getDatasetId(),
                dataset.getCurrentVersionName(),
                true
        );
        if (dataStateEnum == null || !COMPLETE_STATUS.contains(dataStateEnum.getCode())) {
            throw new BusinessException(ErrorEnum.DATASET_NOT_ENHANCE);
        }
        // 获取当前版本文件数据
        List<DatasetVersionFile> datasetVersionFiles =
                datasetVersionFileService.getNeedEnhanceFilesByDatasetIdAndVersionName(
                        dataset.getId(),
                        dataset.getCurrentVersionName()
                );
        // 保存原始状态
        redisUtils.hset("datasetOriginStatus", String.valueOf(dataset.getId()), dataset.getStatus(), 60 * 60);
        // 创建任务
        Task task = Task.builder()
                .status(TaskStatusEnum.INIT.getValue())
                .datasets(JSON.toJSONString(Arrays.asList(datasetEnhanceRequestDTO.getDatasetId())))
                .files(JSON.toJSONString(Collections.EMPTY_LIST))
                .dataType(dataset.getDataType())
                .labels(JSONArray.toJSONString(Collections.emptyList()))
                .annotateType(dataset.getAnnotateType())
                .finished(MagicNumConstant.ZERO)
                .createTime(new Date())
                .total(datasetEnhanceRequestDTO.getEnhanceCount())
                .enhanceType(JSON.toJSONString(datasetEnhanceRequestDTO.getTypes()))
                .datasetId(dataset.getId())
                .type(MagicNumConstant.THREE).build()
                .setTaskParams(datasetEnhanceRequestDTO.getTaskParams());
        taskMapper.insert(task);

        // 状态调用 修改数据集状态为标注完成/自动标注完成 -> 数据增强中
        StateMachineUtil.stateChange(StateChangeDTO.builder()
                .objectParam(
                        new Object[]{dataset.getId().intValue()})
                .eventMethodName(DataStateCodeConstant.AUTO_TAG_COMPLETE_STATE.compareTo(dataset.getStatus()) == 0
                        ? DataStateMachineConstant.DATA_STRENGTHENING_EVENT : DataStateMachineConstant.DATA_COMPLETE_STRENGTHENING_EVENT)
                .stateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE)
                .build());
        datasetOperationEventService.createAndInsertEvent(dataset.getId(),new Date(),"数据增强任务提交",DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_AUGMENTATION);

    }

    /**
     * 获取数据集标签类型
     *
     * @param datasetId 数据集ID
     * @return DatasetLabelEnum 数据集标签类型
     */
    @Override
    public DatasetLabelEnum getDatasetLabelType(Long datasetId) {
        List<Integer> datasetLabelTypes = labelService.getDatasetLabelTypes(datasetId);
        if (CollectionUtil.isNotEmpty(datasetLabelTypes)) {
            if (datasetLabelTypes.contains(DatasetLabelEnum.MS_COCO.getType())) {
                return DatasetLabelEnum.MS_COCO;
            } else if (datasetLabelTypes.contains(DatasetLabelEnum.IMAGE_NET.getType())) {
                return DatasetLabelEnum.IMAGE_NET;
            } else if (datasetLabelTypes.contains(DatasetLabelEnum.AUTO.getType())) {
                return DatasetLabelEnum.AUTO;
            }
            return DatasetLabelEnum.CUSTOM;
        }
        return null;
    }

    /**
     * 查询公共和个人数据集的数量
     *
     * @return DatasetCountVO 数据集数量
     */
    @Override
    public DatasetCountVO queryDatasetsCount() {
        Long curUserId = JwtUtils.getCurUserId();
        if (curUserId == null) {
            throw new BusinessException("当前未登录无资源信息");
        }
        Integer publicCount = baseMapper.selectCountByPublic(DatasetTypeEnum.PUBLIC.getValue(), NumberConstant.NUMBER_0);
        Integer privateCount = baseMapper.selectCount(
                new LambdaQueryWrapper<Dataset>() {{
                    eq(Dataset::getType, DatasetTypeEnum.PRIVATE.getValue());
                    eq(Dataset::getDeleted, NumberConstant.NUMBER_0);
                    if (!BaseService.isAdmin()) {
                        eq(Dataset::getCreateUserId, curUserId);
                    }
                }}
        );
        return new DatasetCountVO(publicCount, privateCount);
    }

    /**
     * 获取公开+私有数据集总数（不含已删除）
     *
     * @return Map 统计信息
     */
    @Override
    public Map<String, Object> getAllDatasetCount() {
        Integer publicCount = baseMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Dataset>() {{
                    eq(Dataset::getDeleted, NumberConstant.NUMBER_0);
                    eq(Dataset::getIsPublic, Dataset.IS_PUBLIC);
                }}
        );

        Long curUserId = JwtUtils.getCurUserId();
        if (curUserId == null) {
            throw new BusinessException("当前未登录无资源信息");
        }

        Integer privateCount = baseMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Dataset>() {{
                    eq(Dataset::getDeleted, NumberConstant.NUMBER_0);
                    eq(Dataset::getIsPublic, Dataset.IS_NOT_PUBLIC);
                    if (!BaseService.isAdmin()) {
                        eq(Dataset::getCreateUserId, curUserId);
                    }
                }}
        );

        long annotatedImageCount = 0L;
        Object cachedCountObj = redisUtils.get(Constant.DATASET_ANNOTATED_IMAGE_COUNT_KEY);
        if (cachedCountObj instanceof Number) {
            annotatedImageCount = ((Number) cachedCountObj).longValue();
        } else if (cachedCountObj != null) {
            try {
                annotatedImageCount = Long.parseLong(String.valueOf(cachedCountObj));
            } catch (Exception ignored) {
                annotatedImageCount = 0L;
            }
        }

        Map<String, Object> result = new HashMap<>(3);
        result.put("publicCount", publicCount);
        result.put("privateCount", privateCount);
        result.put("annotatedImageCount", annotatedImageCount);
        return result;
    }

    /**
     * 根据数据集ID获取数据集详情
     *
     * @param datasetId 数据集ID
     * @return dataset 数据集
     */
    @Override
    public Dataset getOneById(Long datasetId) {
        return getById(datasetId);
    }

    /**
     * 条件查询数据集
     *
     * @param datasetQueryWrapper
     * @return List 数据集列表
     */
    @Override
    public List<Dataset> queryList(QueryWrapper<Dataset> datasetQueryWrapper) {
        return list(datasetQueryWrapper);
    }

    /**
     * 导入用户自定义数据集
     *
     * @param datasetCustomCreateDTO 用户导入自定义数据集请求实体
     * @return Long 数据集ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long importDataset(DatasetCustomCreateDTO datasetCustomCreateDTO) {
        Dataset dataset = new Dataset(datasetCustomCreateDTO);
        dataset.setOriginUserId(JwtUtils.getCurUserId());
        try {
            baseMapper.insert(dataset);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorEnum.DATASET_NAME_DUPLICATED_ERROR);
        }
        dataset.setUri(fileUtil.getDatasetStoragePath(dataset.getId(), dataset.getDataType()));
        updateById(dataset);
        return dataset.getId();
    }


    /**
     * 数据集置顶
     *
     * @param datasetId 数据集id
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void topDataset(Long datasetId) {
        if (!exist(datasetId)) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT);
        }
        checkPublic(datasetId, OperationTypeEnum.UPDATE);
        Dataset dataset = getBaseMapper().selectById(datasetId);
        boolean isTop = dataset.isTop();
        if (isTop) {
            isTop = false;
        } else {
            isTop = true;
        }
        dataset.setTop(isTop);
        dataset.setUpdateTime(null);
        getBaseMapper().updateById(dataset);
    }

    /**
     * 记录两组进度和时间，并用这两组数据估算剩余时间,防止两次进度没变的情况
     * 结构：[prevProgress, prevTime, currProgress, currTime]
     * @param datasetId 数据集ID
     * @param progress 当前进度（0-100）
     * @return 剩余时间（秒），无法估算时返回-1
     */
    public Integer recordAndEstimateRemain(Long datasetId, Integer progress) {
        long now = System.currentTimeMillis() / 1000; // 当前时间（秒）

        // 获取历史记录
        List<Integer> record = progressTimeMap.get(datasetId);

        if (record == null) {
            // 没有历史记录，初始化，两组都存当前进度和时间
            List<Integer> initRecord = new ArrayList<>(4);
            initRecord.add(progress);      // prevProgress
            initRecord.add((int) now);     // prevTime
            initRecord.add(progress);      // currProgress
            initRecord.add((int) now);     // currTime
            progressTimeMap.put(datasetId, initRecord);
            return -1; // 无法估算
        }

        // 读取两组历史数据
        int prevProgress = record.get(0); // 上一次进度
        int prevTime = record.get(1);     // 上一次时间
        int currProgress = record.get(2); // 当前进度
        int currTime = record.get(3);     // 当前时间

        // 如果进度没变，不更新，只用现有两组数据估算
        if (progress.equals(currProgress)) {
            int deltaProgress = currProgress - prevProgress;
            int deltaTime = currTime - prevTime;
            if (deltaProgress <= 0 || deltaTime <= 0) return -1; // 无法估算
            double speed = deltaProgress * 1.0 / deltaTime; // 进度增长速度（百分比/秒）
            int remain = 100 - currProgress; // 剩余进度
            int remainTime = speed > 0 ? (int) Math.round(remain / speed) : -1; // 估算剩余秒数
            return remainTime;
        }

        // 进度有变化，更新两组数据
        record.set(0, currProgress); // prevProgress = currProgress
        record.set(1, currTime);     // prevTime = currTime
        record.set(2, progress);     // currProgress = 新进度
        record.set(3, (int) now);    // currTime = 新时间

        // 用新旧两组数据估算
        int deltaProgress = progress - prevProgress;
        int deltaTime = (int) (now - prevTime);
        if (deltaProgress <= 0 || deltaTime <= 0) return -1; // 无法估算
        double speed = deltaProgress * 1.0 / deltaTime; // 进度增长速度（百分比/秒）
        int remain = 100 - progress; // 剩余进度
        int remainTime = speed > 0 ? (int) Math.round(remain / speed) : -1; // 估算剩余秒数
        return remainTime;
    }

    // 在状态查询轮询结束时移除这个数据集的时间，进度记录，防止内存泄露和出错
    public boolean deleteMapRecordById(Long id) {
        // 删除Map中的记录
        Object removed = progressTimeMap.remove(id);
        return removed != null;
    }

    /**
     * 查询数据集状态
     *
     * @param datasetIds 数据集Id
     * @return Map<Long, IsImportVO> 返回数据集状态
     */
    @Override
    public Map<Long, IsImportVO> determineIfTheDatasetIsAnImport(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return Collections.emptyMap();
        }
        // 在更新状态的dataset
        List<Dataset> datasets = new ArrayList<>();
        datasetIds.forEach(datasetId -> {
            Dataset dataset = getBaseMapper().selectById(datasetId);
            datasets.add(dataset);
        });
        Map<Long, IsImportVO> res = new HashMap<>(datasets.size());
        datasets.forEach(dataset -> {
            IsImportVO isImportVO = IsImportVO.builder().build();
            isImportVO.setStatus(dataset.getStatus());
            // 如果数据集已发布，加入数据集发布信息
            if (dataset.getCurrentVersionName() != null) {
                isImportVO.setDataConversion(datasetVersionServiceImpl.getBaseMapper()
                        .selectOne(new LambdaQueryWrapper<DatasetVersion>()
                                .select(DatasetVersion::getDataConversion)
                                .eq(DatasetVersion::getVersionName, dataset.getCurrentVersionName())
                                .eq(DatasetVersion::getDatasetId, dataset.getId())).getDataConversion());
            }
            Integer status = dataset.getStatus();
            Task task = null;
            if (Objects.equals(status, DataStateCodeConstant.ZIP_IMPORT_STATE)) {
                // 压缩包导入
                task = JSON.parseObject((String) redisUtils.get("datasetImport" + dataset.getId()), Task.class);
                isImportVO.setProgress(task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                Integer remainTime = recordAndEstimateRemain(dataset.getId(), task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                isImportVO.setRemainTime(remainTime);
            } else if (Objects.equals(status, DataStateCodeConstant.STRENGTHENING_STATE)) {
                // 数据增强
                task = taskService.getDistinctTaskByDatasetIdAndTypeDESC(dataset.getId(), DataTaskTypeEnum.ENHANCE.getValue());
                isImportVO.setProgress(task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                Integer remainTime = recordAndEstimateRemain(dataset.getId(), task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                isImportVO.setRemainTime(remainTime);
            } else if (Objects.equals(status, DataStateCodeConstant.SAMPLING_STATE)) {
                // 视频抽帧
                task = taskService.getDistinctTaskByDatasetIdAndTypeDESC(dataset.getId(), DataTaskTypeEnum.VIDEO_SAMPLE.getValue());
                isImportVO.setProgress(task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                Integer remainTime = recordAndEstimateRemain(dataset.getId(), task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                isImportVO.setRemainTime(remainTime);
            } else if (Objects.equals(status, DataStateCodeConstant.AUTOMATIC_LABELING_STATE)) {
                // 自动标注
                task = JSON.parseObject((String) redisUtils.get("autoLabelTask:" + dataset.getId()), Task.class);
                isImportVO.setProgress(task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                Integer remainTime = recordAndEstimateRemain(dataset.getId(), task == null ? 0 : (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                isImportVO.setRemainTime(remainTime);
            } else if (Objects.equals(status, DataStateCodeConstant.ANNOTATION_COMPLETE_STATE) && isImportVO.getDataConversion() != null && isImportVO.getDataConversion() != 1) {
                // 发布中或保存版本中
                // 发布的进度目前的做法是第一次标注完成修改为25%, 文件复制完成修改为50%, 第二次标注完成修改为75%, 文件复制完成修改为100%
                String currentVersionName = dataset.getCurrentVersionName();
                if (currentVersionName == null) {
                    // 如果当前版本为空，则返回0
                    isImportVO.setProgress(0);
                    res.put(dataset.getId(), isImportVO);
                    log.error("获取发布或保存版本进度失败: 当前版本为空");
                    return;
                }
                if(dataset.getIsPublishing()) {
                    // 处于发布中
                    // 拿到上一版本的进度
                    String previousVersionName = "V" + String.format("%04d", Integer.parseInt(currentVersionName.substring(1)) - 1);

                    Task previousTask = JSON.parseObject((String) redisUtils.get("publishTask:" + dataset.getId() + ":" + previousVersionName), Task.class);


                    if (previousTask == null) {
                        // 第一个任务暂时不存在
                        isImportVO.setProgress(0);
                        res.put(dataset.getId(), isImportVO);
                        return;
                    }

                    if (previousTask.getFinished() == 0) {
                        // 第一个任务存在了但是还没开始
                        isImportVO.setProgress(0);
                        res.put(dataset.getId(), isImportVO);
                        Integer remainTime = recordAndEstimateRemain(dataset.getId(), 0);
                        isImportVO.setRemainTime(remainTime);
                        return;
                    }

                    if (previousTask.getFinished() > 0 && previousTask.getFinished() < previousTask.getTotal()) {
                        // 第一个任务开始了但是没结束
                        isImportVO.setProgress(25);
                        res.put(dataset.getId(), isImportVO);
                        Integer remainTime = recordAndEstimateRemain(dataset.getId(), 25);
                        // 先给个假数据，否则如果轮询错过了“第一个任务存在了但是还没开始”，第一阶段没剩余时间显示
                        if (Objects.isNull(remainTime)) {
                            isImportVO.setRemainTime(800);
                        } else {
                            isImportVO.setRemainTime(remainTime);
                        }
                        return;
                    }

                    if (Objects.equals(previousTask.getFinished(), previousTask.getTotal())) {
                        // 第一个任务完成, 检查第二个任务
                        task = JSON.parseObject((String) redisUtils.get("publishTask:" + dataset.getId() + ":" + currentVersionName), Task.class);


                        if (task == null) {
                            // 第二个任务不存在
                            isImportVO.setProgress(50);
                            res.put(dataset.getId(), isImportVO);
                            Integer remainTime = recordAndEstimateRemain(dataset.getId(), 50);
                            isImportVO.setRemainTime(remainTime);
                            return;
                        }

                        if (task.getFinished() == 0) {
                            // 第二个任务存在但是没开始
                            isImportVO.setProgress(50);
                            res.put(dataset.getId(), isImportVO);
                            Integer remainTime = recordAndEstimateRemain(dataset.getId(), 50);
                            isImportVO.setRemainTime(remainTime);
                            return;
                        }

                        if (task.getFinished() > 0 && task.getFinished() < task.getTotal()) {
                            // 第二个任务开始了但是没结束
                            isImportVO.setProgress(75);
                            res.put(dataset.getId(), isImportVO);
                            Integer remainTime = recordAndEstimateRemain(dataset.getId(), 75);
                            isImportVO.setRemainTime(remainTime);
                            return;
                        }

                        if (Objects.equals(task.getFinished(), task.getTotal())) {
                            if (dataset.getIsPublishing()) {
                                // 数据集还没完全完成
                                isImportVO.setProgress(99);
                                res.put(dataset.getId(), isImportVO);
                                Integer remainTime = recordAndEstimateRemain(dataset.getId(), 99);
                                isImportVO.setRemainTime(remainTime);
                                return;
                            }
                            // 第二个任务完成
                            isImportVO.setProgress(100);
                            res.put(dataset.getId(), isImportVO);
                            return;
                        }
                    }
                    // 防止有逻辑漏洞，这里理论上不会走到
                    isImportVO.setProgress(0);
                    res.put(dataset.getId(), isImportVO);

                }else {
                    // 处于保存版本中 正常获取进度即可
                    task = JSON.parseObject((String) redisUtils.get("publishTask:" + dataset.getId() + ":" + currentVersionName), Task.class);
                    if (task == null) {
                        isImportVO.setProgress(0);
                        res.put(dataset.getId(), isImportVO);
                        return;
                    }
                    if (Objects.equals(task.getFinished(), task.getTotal()) && isImportVO.getDataConversion() == 4) {
                        // 说明保存版本实际上没完成，但是标注文件写完了
                        isImportVO.setProgress(99);
                        res.put(dataset.getId(), isImportVO);
                        // 因为进度一直99，所以统计时间给默认值15s
                        isImportVO.setRemainTime(15);
                        return;
                    }
                    isImportVO.setProgress((int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                    Integer remainTime = recordAndEstimateRemain(dataset.getId(), (int) Math.floor((double) task.getFinished() / task.getTotal() * 100));
                    isImportVO.setRemainTime(remainTime);
                }

            }
            res.put(dataset.getId(), isImportVO);
        });
        return res;
    }


    /**
     * 整体删除数据还原
     *
     * @param dto 还原实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void allRollback(RecycleCreateDTO dto) {
        List<RecycleDetailCreateDTO> detailList = dto.getDetailList();
        if (CollectionUtil.isNotEmpty(detailList)) {
            for (RecycleDetailCreateDTO recycleDetailCreateDTO : detailList) {
                if (!Objects.isNull(recycleDetailCreateDTO) &&
                        RecycleTypeEnum.TABLE_DATA.getCode().compareTo(recycleDetailCreateDTO.getRecycleType()) == 0) {
                    Long datasetId = Long.valueOf(recycleDetailCreateDTO.getRecycleCondition());
                    // 根据数据集源ID查询数据集是否存在
                    Dataset dataset = baseMapper.selectById(datasetId);
                    if (!Objects.isNull(dataset) && Objects.isNull(dataset.getSourceId()) && DatasetTypeEnum.PUBLIC.getValue().compareTo(dataset.getType()) == 0) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "预置数据集ID：{} 已存在，禁止还原");
                        throw new BusinessException(DATASET_PUBLIC_LIMIT_ERROR);
                    }
                    // 还原数据集状态
                    baseMapper.updateStatusById(datasetId, false);
                    // 还原数据集标签状态
                    labelService.updateStatusByDatasetId(datasetId, false);
                    // 还原数据集版本状态
                    datasetVersionService.updateStatusByDatasetId(datasetId, false);
                    return;
                }
            }

        }

    }


    /**
     * 普通数据集转预置 整个方法异步处理
     *
     * @param datasetConvertPresetDTO 普通数据集转预置请求实体
     */
    @Override
    @RolePermission
    public void convertPreset(DatasetConvertPresetDTO datasetConvertPresetDTO) {
        // 1 数据集非空校验/名称校验/状态校验
        Dataset originDataset = verificationDatasetBaseInfo(datasetConvertPresetDTO);
        if (DatatypeEnum.AUTO_IMPORT.getValue().compareTo(originDataset.getAnnotateType()) != 0) {
            // 3 校验原版本数据集是否已转换过预制数据集
            List<Dataset> oldDatasets = baseMapper.selectList(new LambdaQueryWrapper<Dataset>().eq(Dataset::getSourceId, datasetConvertPresetDTO.getDatasetId()));
            // 4 如已转换， 删除数据集版本文件 数据集 信息 ，删除minio文件数据
            if (!Objects.isNull(oldDatasets)) {
                oldDatasets.forEach(oldDataset -> {
                    try {
                        addRecycleDataByDeleteDataset(oldDataset);
                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "add recycle task error: {}", e);
                    }
                    oldDataset.setDeleted(true);
                    baseMapper.updateById(oldDataset);
                });
            }
        }
        // 5 根据源版本数据集信息构建目标版本数据集信息
        Dataset targetDataset = buildTargetDataset(originDataset, datasetConvertPresetDTO);
        baseMapper.insert(targetDataset);
        targetDataset.setUri(fileUtil.getDatasetStoragePath(targetDataset.getId(), targetDataset.getDataType()));
        updateById(targetDataset);
        Task task = Task.builder()
                .status(TaskStatusEnum.INIT.getValue())
                .datasetId(datasetConvertPresetDTO.getDatasetId())
                .type(MagicNumConstant.ELEVEN)
                .status(MagicNumConstant.ZERO)
                .labels(JSONArray.toJSONString(Collections.emptyList()))
                .files(JSON.toJSONString(Collections.EMPTY_LIST))
                .targetId(targetDataset.getId())
                .versionName(datasetConvertPresetDTO.getVersionName())
                .build();
        taskService.createTask(task);
    }

    /**
     * 备份数据集DB和MINIO数据
     *
     * @param originDataset 原数据集实体
     * @param targetDataset 目标数据集实体
     * @param versionFiles  原版本列表
     */
    @Override
    public void backupDatasetDBAndMinioData(Dataset originDataset, Dataset targetDataset, List<DatasetVersionFile> versionFiles) {
        LogUtil.info(LogEnum.BIZ_DATASET, "备份数据集DB和MINIO数据 start");
        String versionName = SymbolConstant.BLANK;
        List<DatasetVersionFile> versionFilesSource = new ArrayList<>();
        versionFiles.forEach(versionFile -> {
            DatasetVersionFile datasetVersionFile = new DatasetVersionFile();
            BeanUtils.copyProperties(versionFile, datasetVersionFile);
            versionFilesSource.add(datasetVersionFile);
        });
        versionName = versionFilesSource.get(MagicNumConstant.ZERO).getVersionName();
        // 6 备份数据集标签关系数据
        datasetLabelService.backupDatasetLabelDataByDatasetId(originDataset.getId(), targetDataset);
        // 7 备份数据集版本数据
        datasetVersionService.backupDatasetVersionDataByDatasetId(originDataset, targetDataset, originDataset.getCurrentVersionName());
        if (!CollectionUtils.isEmpty(versionFiles)) {
            // 8 备份数据集文件数据
            List<File> files = fileService.backupFileDataByDatasetId(originDataset, targetDataset);
            if (targetDataset.getAnnotateType().equals(AnnotateTypeEnum.TEXT_CLASSIFICATION.getValue())
                    || targetDataset.getAnnotateType().equals(AnnotateTypeEnum.TEXT_SEGMENTATION.getValue())
                    || targetDataset.getAnnotateType().equals(AnnotateTypeEnum.NAMED_ENTITY_RECOGNITION.getValue())) {
                Map<String, Long> fileNameMap = files.stream().collect(Collectors.toMap(File::getName, File::getId));
//                datasetVersionService.insertEsData(versionName, versionName, originDataset.getId(), targetDataset.getId(), fileNameMap);
            }
            // 9 备份数据集版本文件数据
            datasetVersionFileService.backupDatasetVersionFileDataByDatasetId(originDataset, targetDataset, versionFiles, files);
            // 10 数据集需备份标注数据
            dataFileAnnotationService.backupDataFileAnnotationDataByDatasetId(originDataset, targetDataset, versionFiles);
        }
        LogUtil.info(LogEnum.BIZ_DATASET, "备份数据集DB end");
        // 11 备份MINIO文件数据
        LogUtil.info(LogEnum.BIZ_DATASET, "备份MINIO数据 start");
        copyMinioData(originDataset, targetDataset, versionName, versionFilesSource);
    }


    /**
     * 清理数据集老数据
     *
     * @param oldDataset 数据集实体
     */
    @Async
    public void clearOldDatasetData(Dataset oldDataset) {
        // 异步删除原minio文件数据
        recycleTool.delTempInvalidResources(prefixPath + bucket + SymbolConstant.SLASH + oldDataset.getUri());
    }


    /**
     * 数据集非空校验/名称校验/状态校验
     *
     * @param datasetConvertPresetDTO 普通数据集转预置请求实体
     * @return 原数据集实体
     */
    private Dataset verificationDatasetBaseInfo(DatasetConvertPresetDTO datasetConvertPresetDTO) {
        Dataset originDataset = baseMapper.selectOne(new LambdaQueryWrapper<Dataset>().eq(Dataset::getId, datasetConvertPresetDTO.getDatasetId()));
        if (Objects.isNull(originDataset)) {
            throw new BusinessException("数据集不存在");
        }
        if (Objects.isNull(originDataset.getCurrentVersionName())) {
            throw new BusinessException("数据集未发版");
        }
        if (DatasetTypeEnum.PRIVATE.getValue().compareTo(originDataset.getType()) != 0) {
            throw new BusinessException("只支持我的数据集转预置数据集");
        }
        if (!(DataStateEnum.AUTO_TAG_COMPLETE_STATE.getCode().compareTo(originDataset.getStatus()) == 0 ||
                DataStateEnum.ANNOTATION_COMPLETE_STATE.getCode().compareTo(originDataset.getStatus()) == 0 ||
                DataStateEnum.TARGET_COMPLETE_STATE.getCode().compareTo(originDataset.getStatus()) == 0)) {
            throw new BusinessException("数据集状态不支持转预置");
        }
        return originDataset;
    }


    /**
     * 复制minio文件数据
     *
     * @param originDataset 原数据集实体
     * @param targetDataset 目标数据集实体
     * @param versionName   版本名称
     */
    private void copyMinioData(Dataset originDataset, Dataset targetDataset, String versionName, List<DatasetVersionFile> versionFiles) {
        LogUtil.info(LogEnum.BIZ_DATASET, "复制minio文件数据 start");
        try {
            // 文件复制
            List<String> annotationNames = new ArrayList<>();
            List<String> picNames = new ArrayList<>();
            // 获取当前版本（新版本）的文件URL
            List<Long> fileIds = new ArrayList<>();
            versionFiles.forEach(dataVersionFile -> fileIds.add(dataVersionFile.getFileId()));
            Set<File> files = fileService.get(fileIds, originDataset.getId());
            files.forEach(file -> {
                picNames.add(StringUtils.substringAfter(file.getUrl(), "/"));
                String fileName = StringUtils.substringBeforeLast(StringUtils.substringAfterLast(file.getUrl(), "/"), ".");
                String annotationUrl = originDataset.getUri() + SymbolConstant.SLASH + "versionFile" + SymbolConstant.SLASH +
                        versionName + SymbolConstant.SLASH + "annotation" + SymbolConstant.SLASH + fileName;
                annotationNames.add(annotationUrl);
            });
            String fileTargetDir = targetDataset.getUri() + "/" + "origin";
            String fileTargetDirVersion = targetDataset.getUri() + "/" + "versionFile" + "/"
                    + targetDataset.getCurrentVersionName() + "/" + "origin";
            String annotationTargetDir = targetDataset.getUri() + "/" + "versionFile" + "/"
                    + targetDataset.getCurrentVersionName() + "/" + "annotation";
            minioUtil.copyDir(bucket, annotationNames, annotationTargetDir);
            minioUtil.copyDir(bucket, picNames, fileTargetDir);
            minioUtil.copyDir(bucket, picNames, fileTargetDirVersion);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "文件资源复制失败!  error:{}", e);
            throw new BusinessException(ResponseCode.ERROR, e.getMessage());
        }
    }


    /**
     * 构建目标数据集
     *
     * @param originDataset           原数据集实体
     * @param datasetConvertPresetDTO 数据集转预置实体
     * @return 目标数据集实体
     */
    private Dataset buildTargetDataset(Dataset originDataset, DatasetConvertPresetDTO datasetConvertPresetDTO) {
        return Dataset.builder()
                .annotateType(originDataset.getAnnotateType())
                .dataType(originDataset.getDataType())
                .type(MagicNumConstant.TWO)
                .archiveUrl(originDataset.getArchiveUrl())
                .deleted(originDataset.getDeleted())
                .originUserId(MagicNumConstant.ZERO_LONG)
                .currentVersionName(DEFAULT_VERSION)
                .remark(originDataset.getRemark())
                .name(datasetConvertPresetDTO.getName())
                .sourceId(originDataset.getId())
                .isImport(originDataset.isImport())
                .status(originDataset.getStatus())
                .decompressState(originDataset.getDecompressState())
                .decompressFailReason(originDataset.getDecompressFailReason())
                .labelGroupId(originDataset.getLabelGroupId())
                .build();
    }


    /**
     * 根据数据集ID查询数据集是否转换信息
     *
     * @param datasetId 数据集ID
     * @return true: 允许 false: 不允许
     */
    @Override
    @RolePermission
    public Boolean getConvertInfoByDatasetId(Long datasetId) {
        return !Objects.isNull(baseMapper.selectOne(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getSourceId, datasetId).eq(Dataset::getDeleted, false)));
    }


    /**
     * 根据数据集ID删除数据信息
     *
     * @param datasetId 数据集ID
     */
    @Override
    public void deleteInfoById(Long datasetId) {
        baseMapper.deleteInfoById(datasetId);
    }


    /**
     * 新增标签数据
     *
     * @param label     标签实体
     * @param datasetId 数据集ID
     */
    public void insertLabelData(Label label, Long datasetId) {
        labelService.insert(label);
        datasetLabelService.insert(DatasetLabel.builder().datasetId(datasetId).labelId(label.getId()).build());
    }


    /**
     * 修改数据集和标签的关系
     *
     * @param dataset          数据集实体
     * @param datasetCreateDTO 数据集修改实体
     * @param datasetId        原数据集ID
     */
    private void doDatasetLabelByUpdate(Dataset dataset, DatasetCreateDTO datasetCreateDTO, Long datasetId) {
        // 数据集未标注状态下可操作标签组
        if (DataStateCodeConstant.NOT_ANNOTATION_STATE.compareTo(dataset.getStatus()) == 0) {
            List<Label> labels = labelService.listByGroupId(datasetCreateDTO.getLabelGroupId());
            if (!Objects.isNull(dataset.getLabelGroupId()) &&
                    !dataset.getLabelGroupId().equals(datasetCreateDTO.getLabelGroupId())) {
                // 删除原先数据集与标签关系
                datasetLabelService.del(datasetId);
                insertDatasetLabelAndUpdateDataset(labels, datasetCreateDTO, datasetId);
            } else if (Objects.isNull(dataset.getLabelGroupId()) &&
                    !Objects.isNull(datasetCreateDTO.getLabelGroupId())) {
                // 新增数据集标签和修改数据集信息
                insertDatasetLabelAndUpdateDataset(labels, datasetCreateDTO, datasetId);
            }
        } else if (!Objects.isNull(dataset.getLabelGroupId()) && !Objects.isNull(datasetCreateDTO.getLabelGroupId()) &&
                !dataset.getLabelGroupId().equals(datasetCreateDTO.getLabelGroupId())) {
            throw new BusinessException(ErrorEnum.LABELGROUP_IN_USE_STATUS);
        }
    }


    /**
     * 新增数据集标签和修改数据集信息
     *
     * @param labels           标签列表
     * @param datasetCreateDTO 数据集修改实体
     * @param datasetId        原数据集ID
     */
    private void insertDatasetLabelAndUpdateDataset(List<Label> labels, DatasetCreateDTO datasetCreateDTO, Long datasetId) {
        // 修改数据集标签组ID
        baseMapper.updateById(Dataset.builder().id(datasetId).labelGroupId(datasetCreateDTO.getLabelGroupId()).build());
        // 新增数据集标签关系
        if (!CollectionUtils.isEmpty(labels)) {
            datasetLabelService.saveList(
                    labels.stream()
                            .map(a -> DatasetLabel.builder().datasetId(datasetId)
                                    .labelId(a.getId()).build()
                            ).collect(Collectors.toList())
            );
        }
    }

    /**
     * 获取预置数据集列表
     *
     * @return Map<String, Object> 数据集详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Dataset> getPresetDataset() {
        QueryWrapper<Dataset> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("type", MagicNumConstant.TWO)
                .ne("deleted", MagicNumConstant.ONE);
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void taskStop(Long datasetId) {
        Dataset dataset = baseMapper.selectById(datasetId);
        checkDatasetForTask(dataset);
        // 更新任务状态
        List<Task> tasks = taskService.selectRunningTask(datasetId);
        tasks.forEach(task -> {
            task.setStop(true);
            task.setStatus(MagicNumConstant.FOUR);
            taskService.updateByTaskId(task);
        });
        // 更新数据集状态
        DataStateEnum status = stateIdentify.getStatusForRollback(datasetId, dataset.getCurrentVersionName());
        LambdaUpdateWrapper<Dataset> wrapper = new LambdaUpdateWrapper<Dataset>() {{
            eq(Dataset::getStatus, dataset.getStatus());
            eq(Dataset::getId, datasetId);
            set(Dataset::getStatus, status.getCode());
        }};
        update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ofRecordStop(Long datasetId, String version) {
        DatasetVersion datasetVersion = datasetVersionService.getVersionByDatasetIdAndVersionName(datasetId, version);
        if (datasetVersion.getDataConversion() != MagicNumConstant.FIVE) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_STOP_OF_RECORD_ERROR);
        }
        QueryWrapper<Task> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(Task::getDatasetId, datasetId).eq(Task::getOfRecordVersion, version)
                .eq(Task::getType, MagicNumConstant.ONE).eq(Task::isStop, false).last(" limit 1");
        // 更新任务状态
        Task task = taskService.selectOne(queryWrapper);
        task.setStop(true);
        taskService.updateByTaskId(task);
        // 更新版本状态
        datasetVersion.setDataConversion(MagicNumConstant.ONE);
        datasetVersion.setOfRecord(MagicNumConstant.ZERO);
        datasetVersionService.updateByEntity(datasetVersion);
    }

    @Override
    public DatasetVO getPresetDatasetByName(String datasetName) {
        List<Dataset> datasets = baseMapper.selectList(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getType, MagicNumConstant.TWO)
                .eq(Dataset::getName, datasetName));
        DatasetVO datasetVO = new DatasetVO();
        BeanUtil.copyProperties(datasets.get(0), datasetVO);
        return datasetVO;
    }

    @Override
    public List<DatasetVO> getAllDatasets() {
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<Dataset>()
                .select("id", "name", "remark")
                .eq("deleted", 0);
        List<Dataset> datasets = baseMapper.selectList(datasetQueryWrapper);
        return BeanUtil.copyToList(datasets, DatasetVO.class);
    }

    @Override
    public List<DatasetVO> getAllDatasetsByType(Integer dataType) {
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<Dataset>()
                .select("id", "name", "remark")
                .eq("deleted", 0)
                .eq("data_type", dataType);
        List<Dataset> datasets = baseMapper.selectList(datasetQueryWrapper);
        return BeanUtil.copyToList(datasets, DatasetVO.class);
    }

    @Override
    public List<DatasetVersionVO> getAllDatasetNameAndVersion() {
        return datasetMapper.selectDatasetWithVersions();
    }

    @Override
    public DatasetPeriodDataVO getPeriodData() {

        Date date = DateUtil.date();
        Date beginOfDay = DateUtil.beginOfDay(date);
        Date endOfDay = DateUtil.endOfDay(date);
        DateTime beginOfWeek = DateUtil.beginOfWeek(date);
        DateTime endOfWeek = DateUtil.endOfWeek(date);
        DateTime beginOfMonth = DateUtil.beginOfMonth(date);
        DateTime endOfMonth = DateUtil.endOfMonth(date);
        Integer todayNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getDeleted, false)
                .between(Dataset::getCreateTime, LocalDateTimeUtil.of(beginOfDay), LocalDateTimeUtil.of(endOfDay)));
        Integer thisWeekNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getDeleted, false)
                .between(Dataset::getCreateTime, LocalDateTimeUtil.of(beginOfWeek), LocalDateTimeUtil.of(endOfWeek)));
        Integer thisMonthNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getDeleted, false)
                .between(Dataset::getCreateTime, LocalDateTimeUtil.of(beginOfMonth), LocalDateTimeUtil.of(endOfMonth)));
        Integer totalNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getDeleted, false));

        return DatasetPeriodDataVO.builder().today(todayNum).thisWeek(thisWeekNum).thisMonth(thisMonthNum).total(totalNum).build();


    }

    @Override
    public DatasetTypeStatVO getDatasetStatByType() {
        Integer detectionNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getDeleted, false)
                .eq(Dataset::getAnnotateType, AnnotateTypeEnum.OBJECT_DETECTION.getValue()));

        Integer segmentationNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getDeleted, false)
                .eq(Dataset::getAnnotateType, AnnotateTypeEnum.SEMANTIC_CUP.getValue()));

        Integer allNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                .eq(Dataset::getDeleted, false));

        return DatasetTypeStatVO.builder()
                .detection(detectionNum)
                .segmentation(segmentationNum)
                .all(allNum)
                .build();
    }

    @Override
    public DatasetTypeStatVO getDatasetStatByTypeAndDate(Integer dateNum) {
        List<String> dateList = new ArrayList<>();
        List<String> detectionNumList = new ArrayList<>();
        List<String> segmentationNumList = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd");
        for (int i = 0; i < dateNum; i++) {
            Date today = DateUtil.offsetDay(DateUtil.date(), -i);
            dateList.add(simpleDateFormat.format(today));
            Date beginOfDay = DateUtil.beginOfDay(today);
            Date endOfDay = DateUtil.endOfDay(today);
            Integer detectionNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                    .eq(Dataset::getDeleted, false)
                    .eq(Dataset::getAnnotateType, AnnotateTypeEnum.OBJECT_DETECTION.getValue())
                    .between(Dataset::getCreateTime, LocalDateTimeUtil.of(beginOfDay), LocalDateTimeUtil.of(endOfDay)));
            Integer segmentationNum = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                    .eq(Dataset::getDeleted, false)
                    .eq(Dataset::getAnnotateType, AnnotateTypeEnum.SEMANTIC_CUP.getValue())
                    .between(Dataset::getCreateTime, LocalDateTimeUtil.of(beginOfDay), LocalDateTimeUtil.of(endOfDay)));
            detectionNumList.add(String.valueOf(detectionNum));
            segmentationNumList.add(String.valueOf(segmentationNum));
        }
        Collections.reverse(dateList);
        Collections.reverse(detectionNumList);
        Collections.reverse(segmentationNumList);
        return DatasetTypeStatVO.builder()
                .dateList(dateList)
                .detectionNumList(detectionNumList)
                .segmentationNumList(segmentationNumList)
                .build();

    }

    @Override
    public DatasetPeriodDataVO getPeriodDataByDate(Integer dateNum) {
        List<String> dateList = new ArrayList<>();
        List<String> numList = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd");
        for (int i = 0; i < dateNum; i++) {
            Date today = DateUtil.offsetDay(DateUtil.date(), -i);
            dateList.add(simpleDateFormat.format(today));
            Date beginOfDay = DateUtil.beginOfDay(today);
            Date endOfDay = DateUtil.endOfDay(today);
            Integer num = baseMapper.selectCount(new LambdaQueryWrapper<Dataset>()
                    .eq(Dataset::getDeleted, false)
                    .between(Dataset::getCreateTime, LocalDateTimeUtil.of(beginOfDay), LocalDateTimeUtil.of(endOfDay)));
            numList.add(String.valueOf(num));
        }
        Collections.reverse(dateList);
        Collections.reverse(numList);
        return DatasetPeriodDataVO.builder()
                .dateList(dateList)
                .numList(numList)
                .build();
    }

    @Override
    public List<DatasetLabelInfoDTO> getDatasetLabelInfo(Long datasetId) {
        return datasetMapper.selectLabelInfoByDatasetId(datasetId);
    }

    /**
     * 根据数据类型查询数据
     *
     * @param dataType
     * @return
     */
    @Override
    public List<Dataset> selectByDataType(Integer dataType) {
        return datasetMapper.selectByDataType(dataType);
    }

    public void checkDatasetForTask(Dataset dataset) {
        if (ObjectUtil.isEmpty(dataset)) {
            throw new BusinessException("数据集不存在");
        }
        HashSet<Integer> set = new HashSet<Integer>() {{
            add(DataStateCodeConstant.AUTOMATIC_LABELING_STATE);
            add(DataStateCodeConstant.TARGET_FOLLOW_STATE);
            add(DataStateCodeConstant.SAMPLING_STATE);
            add(DataStateCodeConstant.STRENGTHENING_STATE);
            add(DataStateCodeConstant.IN_THE_IMPORT_STATE);
        }};
        if (!set.contains(dataset.getStatus())) {
            throw new BusinessException("当前数据集状态不允许操作");
        }
    }

    @Override
    public List<DatasetVO> getUnpublishedDatasets() {
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<Dataset>()
                .select("id", "name")
                .eq("deleted", 0)
                .isNull("current_version_name")
                .eq("module", DatasetModuleEnum.NO_TEAM.getStatus());
//                .notInSql("id", "select dataset_id from data_team_task where deleted = 0");
        UserContext curUser = userContextService.getCurUser();
        String roleName = curUser.getRoles().get(0).getName();
        long creatorId = curUser.getId();
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            datasetQueryWrapper.eq("create_user_id", creatorId);
        }
        List<Dataset> datasets = baseMapper.selectList(datasetQueryWrapper);
        datasets = datasets
                .stream()
                .filter(dataset -> datasetVersionFileMapper.getDatasetVersionAllFileCount(dataset.getId(), null) > 0)
                .collect(Collectors.toList());
        Collections.reverse(datasets);
        return BeanUtil.copyToList(datasets, DatasetVO.class);
    }

    @Override
    public List<DatasetVO> getUnpublishedDatasetsByGroupId(Long datasetGroupId) {
        // 获取数据集组下的所有数据集ID
        List<Long> datasetIds = datasetDatasetGroupMapper.selectList(
                new QueryWrapper<DatasetDatasetGroup>()
                        .eq("dataset_group_id", datasetGroupId)
        ).stream().map(DatasetDatasetGroup::getDatasetId).collect(Collectors.toList());

        if (datasetIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询符合条件的数据集
        QueryWrapper<Dataset> datasetQueryWrapper = new QueryWrapper<Dataset>()
                .select("id", "name")
                .eq("deleted", 0)
                .isNull("current_version_name")
                .eq("module", DatasetModuleEnum.NO_TEAM.getStatus())
                .in("id", datasetIds);

        UserContext curUser = userContextService.getCurUser();
        String roleName = curUser.getRoles().get(0).getName();
        long creatorId = curUser.getId();
        if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
            datasetQueryWrapper.eq("create_user_id", creatorId);
        }

        List<Dataset> datasets = baseMapper.selectList(datasetQueryWrapper);
        datasets = datasets
                .stream()
                .filter(dataset -> datasetVersionFileMapper.getDatasetVersionAllFileCount(dataset.getId(), null) > 0)
                .collect(Collectors.toList());
        Collections.reverse(datasets);
        return BeanUtil.copyToList(datasets, DatasetVO.class);
    }

    @Override
    public void importDatasetFromDataRepo(Long dataRepoId, Long datasetId, Integer dataType) {

        // 筛选图片或视频
        List<DataRepoFile> dataRepoFiles = dataRepoFileMapper.selectFilesByDatasetIdAndFileType(dataRepoId, dataType);
        // 数据集无图片或视频直接返回
        if (dataRepoFiles == null || dataRepoFiles.isEmpty()) {
            return;
        }

        // 重命名 + 复制文件
        CompletableFuture<List<FileCreateDTO>> orgFuture = CompletableFuture.supplyAsync(() -> {
            // 待插入的文件列表
            ArrayList<FileCreateDTO> fileCreateDTOS = new ArrayList<>();
            for (DataRepoFile dataRepoFile : dataRepoFiles) {
                // 重命名文件
                String newName = getDataRepoFileNewName(dataRepoFile.getName(), datasetId, dataRepoFile.getId());
                // 复制图片
                String sourceFile = dataRepoFile.getUrl();
                int index = sourceFile.indexOf('/');
                sourceFile = sourceFile.substring(index + 1);
                String targetFile = fileUtil.getDatasetFilePath(datasetId, newName, true);
                try {
                    minioUtil.copyObject(bucket, sourceFile, targetFile);
                } catch (Exception e) {
                    throw new RuntimeException("文件复制失败,终止入库");
                }
                // 构造入库文件信息
                FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                        .width(dataRepoFile.getWidth())
                        .height(dataRepoFile.getHeight())
                        .url(fileUtil.getBucketNameFilePath(datasetId, newName, true))
                        .build();
                // 插入到文件列表
                fileCreateDTOS.add(fileCreateDTO);
            }
            return fileCreateDTOS;
        }, pool.getExecutor());

        // 文件信息入库
        CompletableFuture<Void> thenAcceptFuture = orgFuture.thenAccept((fileCreateDTOS) -> {
            if (Objects.equals(DatatypeEnum.IMAGE.getValue(), dataType)) {
                uploadFilesFromDataRepo(datasetId, BatchFileCreateDTO.builder().files(fileCreateDTOS).ifImport(false).build());
            } else {
                uploadVideoFromDataRepo(datasetId, BatchFileCreateDTO.builder().files(fileCreateDTOS).ifImport(false).build());
            }
        });


        // 状态机转换
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{datasetId.intValue()});
            setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
    }

    @Override
    public void autoLabelStart(Long datasetId) {

        autoLabelStartDo(datasetId, null, false, true, null);

    }

    @Override
    public void autoLabelStartWithNamesToFilter(Long datasetId,List<String> labelNames) {

        autoLabelStartDo(datasetId, labelNames, false, true, null);

    }

    @Override
    public void autoLabelStartWithNamesToFilterAndClearOption(Long datasetId, List<String> labelNames, Boolean clearExistingLabels, Boolean requireManualConfirmation, String jobName) {

        autoLabelStartDo(datasetId, labelNames, clearExistingLabels != null ? clearExistingLabels : false, requireManualConfirmation != null ? requireManualConfirmation : true, jobName);

    }


    private  void autoLabelStartDo(Long datasetId,List<String> labelNames, Boolean clearExistingLabels, Boolean requireManualConfirmation, String jobName){
        String taskKey = "autoLabelTask:" + datasetId;

        // 检查是否已有自动标注任务在进行
        String existingTaskJson = (String) redisUtils.get(taskKey);
        LogUtil.info(LogEnum.BIZ_DATASET, "autoLabelStartDo: datasetId={}, jobName={}, existingTask={}", datasetId, jobName, existingTaskJson != null ? "存在(将跳过创建)" : "不存在(正常创建)");
        if (existingTaskJson != null && jobName != null) {
            // 如果已有任务且传入了 jobName，更新现有任务的 jobName（用于自迭代回调）
            try {
                Task existingTask = JSON.parseObject(existingTaskJson, Task.class);
                if (existingTask.getJobName() == null) {
                    existingTask.setJobName(jobName);
                    redisUtils.set(taskKey, JSON.toJSONString(existingTask), 100 * 60);
                    LogUtil.info(LogEnum.BIZ_DATASET, "更新已有自动标注任务的 jobName：datasetId={}, jobName={}", datasetId, jobName);
                } else {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "已有自动标注任务且 jobName 已存在，跳过本次标注启动：datasetId={}, existingJobName={}, newJobName={}", datasetId, existingTask.getJobName(), jobName);
                }
            } catch (Exception e) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "更新已有任务 jobName 失败：{}", e.getMessage());
            }
        }

        // 获取所有文件的标注状态
        FileScreenStatSearchDTO fileScreenStatSearchDTO = new FileScreenStatSearchDTO();
        fileScreenStatSearchDTO.setAnnotationResult(FileTypeEnum.HAVE_ANNOTATION.getValue());
        FileScreenStatVO fileCountByStatus = fileService.getFileCountByStatus(datasetId, fileScreenStatSearchDTO);

        // 用总数-视频数量得到总图片数量
        Long totalImages = fileCountByStatus.getHaveAnnotation() + fileCountByStatus.getNoAnnotation();
        // 创建任务信息

        Task task = Task.builder()
                .datasetId(datasetId)
                .type(MagicNumConstant.ZERO)
                .status(MagicNumConstant.ONE)
                .labels(JSONArray.toJSONString(Collections.emptyList()))
                .files(JSON.toJSONString(Collections.EMPTY_LIST))
                .total(totalImages.intValue())
                .finished(MagicNumConstant.ZERO)
                .labelNames(labelNames)
                .clearExistingLabels(clearExistingLabels)
                .requireManualConfirmation(requireManualConfirmation)
                .jobName(jobName)
                .stop(false)
                .build();

        // 将任务信息保存到 Redis
        redisUtils.set(taskKey, JSON.toJSONString(task), 100 * 60);


        // 状态机转换
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{datasetId.intValue()});
            setEventMethodName(DataStateMachineConstant.DATA_AUTO_ANNOTATIONS_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
        datasetOperationEventService.createAndInsertEvent(datasetId,new Date(),"自动标注任务开始", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.AUTO_LABELING);
    }



    @Async
    @Override
    public void autoLabelEnd(Long datasetId, Boolean isFailed) {
        long methodStartTime = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] autoLabelEnd方法开始执行，datasetId: " + datasetId);

        String taskKey = "autoLabelTask:" + datasetId;

        // Redis读取计时
        long redisGetStart = System.currentTimeMillis();
        String taskJson = (String) redisUtils.get(taskKey);
        long redisGetTime = System.currentTimeMillis() - redisGetStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] Redis获取任务信息耗时: " + redisGetTime + "ms");

        Task task = JSON.parseObject(taskJson, Task.class);
        List<String> labelNames = task.getLabelNames();
        Boolean clearExistingLabels = task.getClearExistingLabels();
        Boolean requireManualConfirmation = task.getRequireManualConfirmation();

        if (isFailed) {
            handleFailedTask(datasetId, task, taskKey);
        } else {
            handleSuccessfulTask(datasetId, task, taskKey, labelNames, clearExistingLabels, requireManualConfirmation);
        }

        long methodTotalTime = System.currentTimeMillis() - methodStartTime;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] ========== autoLabelEnd方法执行完成 ==========");
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 方法总执行时间: " + methodTotalTime + "ms");
    }

    /**
     * 处理失败的任务
     */
    private void handleFailedTask(Long datasetId, Task task, String taskKey) {
        task.setStatus(TaskStatusEnum.FAIL.getValue());
        task.setStop(false);

        // Redis写入计时
        long redisSetStart = System.currentTimeMillis();
        redisUtils.set(taskKey, JSON.toJSONString(task), 100 * 60);
        long redisSetTime = System.currentTimeMillis() - redisSetStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] Redis保存失败任务状态耗时: " + redisSetTime + "ms");

        LogUtil.info(LogEnum.BIZ_DATASET, "Fail:autoLabelEndfinally" + "状态机转换");

        // 数据库查询计时
        long dbQueryStart = System.currentTimeMillis();
        Dataset dataset = datasetMapper.selectById(datasetId);
        long dbQueryTime = System.currentTimeMillis() - dbQueryStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 数据库查询dataset耗时: " + dbQueryTime + "ms");

        // 状态机转换计时
        long stateMachineStart = System.currentTimeMillis();
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.DATA_AUTO_ANNOTATIONS_FINISH_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            datasetOperationEventService.createAndInsertEvent(datasetId, new Date(), "自动标注任务容器作业失败", DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.AUTO_LABELING);
        }});
        long stateMachineTime = System.currentTimeMillis() - stateMachineStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 状态机转换(失败)耗时: " + stateMachineTime + "ms");

        // 创建自动标注失败通知
        createAutoLabelNotification(dataset, false, "容器作业失败");

        // 回调 mineai 自迭代任务失败
        callbackSelfIterationFailedIfNeeded(datasetId, task);
    }

    /**
     * 如果是自迭代任务触发的自动标注失败，回调 mineai
     */
    private void callbackSelfIterationFailedIfNeeded(Long datasetId, Task task) {
        try {
            String jobName = task.getJobName();
            if (jobName != null && jobName.startsWith("autolabel-iter")) {
                String[] parts = jobName.split("-");
                if (parts.length >= 5) {
                    String jobPart = parts[3];
                    if (jobPart.startsWith("job")) {
                        Long selfIterJobId = Long.parseLong(jobPart.substring(3));
                        selfIterationClient.onAutoLabelFailed("", selfIterJobId, datasetId);
                        LogUtil.info(LogEnum.BIZ_DATASET, "自迭代任务自动标注失败回调成功：jobId={}, datasetId={}", selfIterJobId, datasetId);
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "自迭代任务失败回调失败：datasetId={}, error={}", datasetId, e.getMessage());
        }
    }

    /**
     * 处理成功的任务
     */
    private void handleSuccessfulTask(Long datasetId, Task task, String taskKey, List<String> labelNames, Boolean clearExistingLabels, Boolean requireManualConfirmation) {
        long successProcessStart = System.currentTimeMillis();

        String parentDir = fileUtil.getDatasetIdAbsPath(datasetId);
        String originDir = parentDir + "/origin";
        String outputDir = parentDir + "/output";
        String annotationFilePath = parentDir + "/annotation";

        // 数据库查询计时
        long dbQueryStart = System.currentTimeMillis();
        Dataset datasetById = datasetMapper.selectById(datasetId);
        long dbQueryTime = System.currentTimeMillis() - dbQueryStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 数据库查询dataset耗时: " + dbQueryTime + "ms");

        java.io.File imageDir = new java.io.File(originDir);

        try {
            // 处理标签映射
            Map<Long, Long> categoryMap = processLabels(datasetId, outputDir, labelNames);

            // 确保这是目录
            if (imageDir.isDirectory()) {
                // 获取目录中的文件和子目录
                long listFilesStart = System.currentTimeMillis();
                java.io.File[] files = imageDir.listFiles();
                long listFilesTime = System.currentTimeMillis() - listFilesStart;
                LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 获取文件列表耗时: " + listFilesTime + "ms");
                LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 待处理文件总数: " + (files != null ? files.length : 0));

                if (files != null && files.length > 0) {
                    // 先并行准备所有数据，再批量上传
                    processAndUploadInBatches(files, datasetId, datasetById, outputDir, annotationFilePath, categoryMap, task, taskKey, clearExistingLabels, requireManualConfirmation);
                }
            } else {
                LogUtil.info(LogEnum.BIZ_DATASET, imageDir + "不是路径");
            }
        } catch (Exception e) {
            e.printStackTrace();
            datasetOperationEventService.createAndInsertEvent(datasetId, new Date(), "自动标注保存结果出错" + e.getMessage(), DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.AUTO_LABELING);
        } finally {
            finalizeTasks(datasetId, task, taskKey);
        }

        long successProcessTime = System.currentTimeMillis() - successProcessStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 成功处理分支总耗时: " + successProcessTime + "ms");

        datasetOperationEventService.createAndInsertEvent(datasetId, new Date(), "自动标注结束", DatasetOperationEvent.EventType.INFO, DatasetOperationEvent.OperationType.AUTO_LABELING);

        // 创建自动标注成功通知
        createAutoLabelNotification(datasetById, true, null);
    }

    /**
     * 创建自动标注完成通知
     * @param dataset 数据集
     * @param success 是否成功
     * @param reason 失败原因（成功时为null）
     */
    private void createAutoLabelNotification(Dataset dataset, boolean success, String reason) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("datasetId", dataset.getId());
            payload.put("datasetName", dataset.getName());
            if (!success && reason != null) {
                payload.put("reason", reason);
            }

            Notification notification = new Notification();
            notification.setToUserId(dataset.getCreateUserId());
            notification.setNotificationType(success ? Notification.NotificationType.INFO : Notification.NotificationType.ERROR);
            notification.setOperationType(success
                    ? NotificationOperationTypeEnum.AUTO_LABEL_SUCCESS.getCode()
                    : NotificationOperationTypeEnum.AUTO_LABEL_FAILED.getCode());
            notification.setPayload(payload.toJSONString());
            notification.setReadStatus(0);
            Date now = new Date();
            notification.setCreateTime(now);
            notification.setUpdateTime(now);
            notification.setDeleted(false);

            notificationService.safeInsertAsync(notification);
        } catch (Exception e) {
            log.error("创建自动标注通知异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 标注上传数据的包装类
     */
    private static class AnnotationUploadData {
        // getters
        @Getter
        private final FileCreateDTO fileCreateDTO;
        @Getter
        private final JSONArray resultArray;
        private final boolean isModified;

        public AnnotationUploadData(FileCreateDTO fileCreateDTO, JSONArray resultArray, boolean isModified) {
            this.fileCreateDTO = fileCreateDTO;
            this.resultArray = resultArray;
            this.isModified = isModified;
        }

        public boolean isModified() { return isModified; }
    }


    /**
     * 批量上传标注数据
     */
    public void uploadAnnotationBatchForAutoLabel(List<AnnotationUploadData> uploadDataList, Long datasetId, Boolean requireManualConfirmation) {
        try {
            if (CollectionUtils.isEmpty(uploadDataList)) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "uploadDataList 为空");
                return;
            }

            // 1. 批量查询文件ID
            List<String> urlList = uploadDataList.stream()
                    .map(data -> data.getFileCreateDTO().getUrl())
                    .collect(Collectors.toList());

            Map<String, Long> urlToFileIdMap = fileService.getFileIdsByUrls(datasetId, urlList);

            // 2. 准备批量标注数据
            List<BatchAnnotationInfo> batchAnnotationInfos = new ArrayList<>();
            for (AnnotationUploadData uploadData : uploadDataList) {
                String url = uploadData.getFileCreateDTO().getUrl();
                Long fileId = urlToFileIdMap.get(url);
                if (fileId == null) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "找不到文件ID，URL: " + url);
                    continue;
                }

                JSONArray resultArray = uploadData.getResultArray();
                if (resultArray == null) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "resultArray 为空，fileId: " + fileId);
                    continue;
                }

                batchAnnotationInfos.add(BatchAnnotationInfo.builder()
                        .fileId(fileId)
                        .url(url)
                        .annotation(resultArray.toJSONString())
                        .isModified(uploadData.isModified())
                        .build());
            }

            if (CollectionUtils.isEmpty(batchAnnotationInfos)) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "没有有效的批量标注数据");
                return;
            }

            // 3. 批量处理标注
            annotationService.finishManualBatchForAutoLabel(batchAnnotationInfos, datasetId, requireManualConfirmation);

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "批量上传标注数据失败", e);
            throw e;
        }
    }

    /**
     * 分批准备数据，然后并行分批上传并更新进度
     */
    private void processAndUploadInBatches(java.io.File[] files, Long datasetId, Dataset datasetById,
                                           String outputDir, String annotationFilePath,
                                           Map<Long, Long> categoryMap, Task task, String taskKey, Boolean clearExistingLabels, Boolean requireManualConfirmation) {

        // 统计计时器
        AtomicLong totalImageProcessTime = new AtomicLong(0);
        AtomicLong totalTxtProcessTime = new AtomicLong(0);
        AtomicLong totalMinioReadTime = new AtomicLong(0);

        long dataPreparationStart = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 开始分批准备数据，文件总数: {}...", files.length);

        // 使用线程池分批处理数据准备
        List<AnnotationUploadData> uploadDataList = prepareDataWithBatchProcessing(files, datasetId, datasetById,
                outputDir, annotationFilePath, categoryMap, totalImageProcessTime, totalTxtProcessTime, totalMinioReadTime, clearExistingLabels, requireManualConfirmation);

        long dataPreparationTime = System.currentTimeMillis() - dataPreparationStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 数据准备完成，耗时: {}ms, 准备了 {}/{} 个文件的数据",
                dataPreparationTime, uploadDataList.size(), files.length);

        // 输出数据准备阶段的统计
        outputDataPreparationStatistics(uploadDataList.size(), totalImageProcessTime.get(),
                totalTxtProcessTime.get(), totalMinioReadTime.get());

        // 并行分批上传
        long uploadStart = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 开始并行分批上传标注数据...");

        AtomicLong totalUploadTime = new AtomicLong(0);
        AtomicLong totalRedisUpdateTime = new AtomicLong(0);
        AtomicInteger uploadedCount = new AtomicInteger(0);

        // 分批参数配置
        int batchSize = 200;  // 每批数量
        int maxConcurrentBatches = 5;  // 最大并发处理批数上限

        // 计算总批数
        int totalBatches = (int) Math.ceil((double) uploadDataList.size() / batchSize);

        // 实际线程池大小：不超过总批数，也不超过最大并发数
        int actualThreadPoolSize = Math.min(totalBatches, maxConcurrentBatches);

        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 分批配置 - 总文件数: {}, 每批大小: {}, 总批数: {}, 实际线程池大小: {}",
                uploadDataList.size(), batchSize, totalBatches, actualThreadPoolSize);

        // 创建线程池，大小根据实际需要确定
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                actualThreadPoolSize,  // 核心线程数
                actualThreadPoolSize,  // 最大线程数
                60L,                   // 空闲时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger threadNumber = new AtomicInteger(1);
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "batch-upload-" + threadNumber.getAndIncrement());
                        t.setDaemon(false);
                        return t;
                    }
                }
        );

        try {
            // 创建所有批次任务
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < totalBatches; i++) {
                int batchIndex = i;
                int start = i * batchSize;
                int end = Math.min(start + batchSize, uploadDataList.size());

                List<AnnotationUploadData> batch = uploadDataList.subList(start, end);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 线程 {} 开始处理第{}/{}批，范围: {}-{}, 批次大小: {}",
                                Thread.currentThread().getName(), batchIndex + 1, totalBatches, start + 1, end, batch.size());

                        // 批量上传当前批次
                        long batchUploadStart = System.currentTimeMillis();
                        uploadAnnotationBatchForAutoLabel(batch, datasetId, requireManualConfirmation);
                        long batchUploadTime = System.currentTimeMillis() - batchUploadStart;
                        totalUploadTime.addAndGet(batchUploadTime);

                        // 更新进度计数器
                        int currentBatchProcessed = uploadedCount.addAndGet(batch.size());

                        // 更新Redis进度
                        long redisUpdateStart = System.currentTimeMillis();
                        task.setFinished(Math.min(currentBatchProcessed, task.getTotal()));
                        redisUtils.set(taskKey, JSON.toJSONString(task), 100 * 60);
                        long redisUpdateTime = System.currentTimeMillis() - redisUpdateStart;
                        totalRedisUpdateTime.addAndGet(redisUpdateTime);

                        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 线程 {} 第{}/{}批处理完成，耗时: {}ms, 当前总进度: {}/{}",
                                Thread.currentThread().getName(), batchIndex + 1, totalBatches, batchUploadTime,
                                currentBatchProcessed, task.getTotal());

                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "批次 " + (batchIndex + 1) + " 处理失败", e);
                        throw new RuntimeException("批次处理失败", e);
                    }
                }, executor);

                futures.add(future);
            }

            // 等待所有批次完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0])
            );

            // 设置超时时间，避免无限等待
            allFutures.get(300, TimeUnit.MINUTES);

            long totalBatchUploadTime = System.currentTimeMillis() - uploadStart;
            LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 并行分批上传完成，总耗时: {}ms, 总批数: {}, 最终进度: {}/{}",
                    totalBatchUploadTime, totalBatches, uploadedCount.get(), task.getTotal());

            // 输出上传阶段统计
            outputUploadStatistics(uploadDataList.size(), totalUploadTime.get(), totalRedisUpdateTime.get());

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "并行分批上传失败", e);
            throw new RuntimeException("并行分批上传失败", e);
        } finally {
            // 关闭线程池
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 使用线程池分批处理数据准备
     */
    private List<AnnotationUploadData> prepareDataWithBatchProcessing(java.io.File[] files, Long datasetId,
                                                                      Dataset datasetById, String outputDir,
                                                                      String annotationFilePath,
                                                                      Map<Long, Long> categoryMap,
                                                                      AtomicLong totalImageProcessTime,
                                                                      AtomicLong totalTxtProcessTime,
                                                                      AtomicLong totalMinioReadTime,
                                                                      Boolean clearExistingLabels,
                                                                      Boolean requireManualConfirmation) {
        // 控制并发线程数和批次大小
        int threadPoolSize = 10;  // 较低的并发度，避免文件系统压力
        int batchSize = 100;     // 每批处理100个文件

        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "data-prepare-thread");
            t.setDaemon(false);
            return t;
        });

        List<AnnotationUploadData> allResults = Collections.synchronizedList(new ArrayList<>());

        try {
            // 将文件分批
            List<java.io.File> fileList = Arrays.stream(files)
                    .filter(java.io.File::isFile)
                    .collect(Collectors.toList());

            List<List<java.io.File>> batches = new ArrayList<>();

            for (int i = 0; i < fileList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, fileList.size());
                batches.add(fileList.subList(i, end));
            }

            LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 数据准备分批配置 - 总文件数: {}, 批次大小: {}, 总批数: {}, 线程数: {}",
                    fileList.size(), batchSize, batches.size(), threadPoolSize);

            // 提交批次任务
            List<Future<List<AnnotationUploadData>>> futures = new ArrayList<>();

            for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
                final int currentBatchIndex = batchIndex;
                final List<java.io.File> batch = batches.get(batchIndex);

                Future<List<AnnotationUploadData>> future = executor.submit(() -> {
                    List<AnnotationUploadData> batchResults = new ArrayList<>();

                    LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 线程 {} 开始处理数据准备批次 {}/{}, 文件数: {}",
                            Thread.currentThread().getName(), currentBatchIndex + 1, batches.size(), batch.size());

                    for (java.io.File file : batch) {
                        try {
                            AnnotationUploadData data = prepareUploadDataForFileWithRetry(file, datasetId, datasetById,
                                    outputDir, annotationFilePath, categoryMap, totalImageProcessTime,
                                    totalTxtProcessTime, totalMinioReadTime, clearExistingLabels, requireManualConfirmation);
                            if (data != null) {
                                batchResults.add(data);
                            }
                        } catch (Exception e) {
                            LogUtil.error(LogEnum.BIZ_DATASET, "准备文件数据失败: " + file.getName(), e);
                        }
                    }

                    LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 线程 {} 完成数据准备批次 {}/{}, 成功处理: {}/{}",
                            Thread.currentThread().getName(), currentBatchIndex + 1, batches.size(), batchResults.size(), batch.size());

                    return batchResults;
                });

                futures.add(future);
            }

            // 收集所有结果
            for (Future<List<AnnotationUploadData>> future : futures) {
                try {
                    List<AnnotationUploadData> batchResults = future.get(10, TimeUnit.MINUTES); // 设置超时
                    allResults.addAll(batchResults);
                } catch (TimeoutException e) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "数据准备批次超时", e);
                    throw new RuntimeException("数据准备批次超时", e);
                }
            }

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "分批数据准备失败", e);
            throw new RuntimeException("数据准备失败", e);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        return allResults;
    }

    /**
     * 添加重试机制的文件处理方法
     */
    private AnnotationUploadData prepareUploadDataForFileWithRetry(java.io.File file, Long datasetId,
                                                                   Dataset datasetById, String outputDir,
                                                                   String annotationFilePath,
                                                                   Map<Long, Long> categoryMap,
                                                                   AtomicLong totalImageProcessTime,
                                                                   AtomicLong totalTxtProcessTime,
                                                                   AtomicLong totalMinioReadTime,
                                                                   Boolean clearExistingLabels,
                                                                   Boolean requireManualConfirmation) {
        int maxRetries = 3;
        int retryDelay = 3000; // 3秒

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return prepareUploadDataForFile(file, datasetId, datasetById, outputDir, annotationFilePath,
                        categoryMap, totalImageProcessTime, totalTxtProcessTime, totalMinioReadTime, clearExistingLabels, requireManualConfirmation);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("网络连接") ||
                        e instanceof java.nio.file.FileSystemException) {

                    if (attempt == maxRetries) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "文件处理失败，已达到最大重试次数: " + file.getName() + ", 错误: " + e.getMessage());
                        throw new RuntimeException("文件处理失败: " + file.getName(), e);
                    } else {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "文件处理失败，进行第{}次重试: {}, 错误: {}", attempt, file.getName(), e.getMessage());
                        try {
                            Thread.sleep(retryDelay * attempt); // 递增延迟
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("重试被中断", ie);
                        }
                    }
                } else {
                    // 非网络相关错误，直接抛出
                    LogUtil.error(LogEnum.BIZ_DATASET, "文件处理失败: " + file.getName(), e);
                    throw new RuntimeException("文件处理失败: " + file.getName(), e);
                }
            }
        }
        return null;
    }

    /**
     * 为单个文件准备上传数据（不执行上传）
     */
    private AnnotationUploadData prepareUploadDataForFile(java.io.File file, Long datasetId, Dataset datasetById,
                                                          String outputDir, String annotationFilePath,
                                                          Map<Long, Long> categoryMap, AtomicLong totalImageProcessTime,
                                                          AtomicLong totalTxtProcessTime, AtomicLong totalMinioReadTime,
                                                          Boolean clearExistingLabels, Boolean requireManualConfirmation) throws IOException {

        String baseFileName = StringUtils.substringBeforeLast(file.getName(), ".");

        // 1. 处理TXT文件
        long txtProcessStart = System.currentTimeMillis();
        String annoFile = baseFileName + ".txt";
        List<JSONObject> txtToJsonObjectList = ConversionUtil.txtToJsonObjectList(outputDir + "/" + annoFile);
        long txtProcessTime = System.currentTimeMillis() - txtProcessStart;
        totalTxtProcessTime.addAndGet(txtProcessTime);

        if (txtToJsonObjectList == null) {
            return null;
        }

        // 2. 读取现有标注
        JSONArray annJsonArray = readExistingAnnotation(datasetById, annotationFilePath, baseFileName, totalMinioReadTime);

        // 3. 读取图片尺寸信息（不加载完整图片）
        long imageReadStart = System.currentTimeMillis();
        ImageDimension dimension = readImageDimensions(file);
        long imageReadTime = System.currentTimeMillis() - imageReadStart;
        totalImageProcessTime.addAndGet(imageReadTime);

        // 4. 构建标注数据
        FileCreateDTO fileCreateDTO = buildFileCreateDTO(dimension.getWidth(), dimension.getHeight(), file.getName(), datasetId);
        JSONArray resultArray = buildAnnotationResult(txtToJsonObjectList, annJsonArray, categoryMap,
                dimension.getWidth(), dimension.getHeight(), datasetById, clearExistingLabels);

        // 5. 判断是否有修改
        boolean isModified = !resultArray.equals(annJsonArray);

        return new AnnotationUploadData(fileCreateDTO, resultArray, isModified);
    }

    /**
     * 只读取图片尺寸信息，不加载完整图片数据
     */
    private ImageDimension readImageDimensions(java.io.File file) throws IOException {
        ImageInputStream iis = null;
        ImageReader reader = null;

        try {
            iis = ImageIO.createImageInputStream(file);
            if (iis == null) {
                throw new IOException("无法创建ImageInputStream: " + file.getName());
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                throw new IOException("无法找到合适的图片读取器: " + file.getName());
            }

            reader = readers.next();
            reader.setInput(iis);

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);

            //LogUtil.info(LogEnum.BIZ_DATASET, "读取图片尺寸: " + file.getName() + ", 尺寸: " + width + "x" + height);

            return new ImageDimension(width, height);

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "读取图片尺寸失败: " + file.getName(), e);
            throw new IOException("读取图片尺寸失败: " + file.getName() + ", 原因: " + e.getMessage(), e);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            if (iis != null) {
                iis.close();
            }
        }
    }

    /**
     * 处理标签并建立映射关系
     */
    private Map<Long, Long> processLabels(Long datasetId, String outputDir, List<String> labelNames) throws IOException {
        long readLabelFileStart = System.currentTimeMillis();

        Map<Long, Long> categoryMap = new HashMap<>();
        String labelFile = outputDir + "/classes.txt";
        AtomicInteger i = new AtomicInteger(0);

        DatasetImportDTO datasetImportDTO = new DatasetImportDTO();
        datasetImportDTO.setDatasetId(datasetId);

        // 算法包传入的允许标签清单：精确集合 + 小写兜底集合
        // 避免与镜像 classes.txt 大小写不一致（如算法包里是 "PERSON"，镜像输出是 "person"）时被全部过滤丢弃
        Set<String> allowedExact = labelNames != null ? new HashSet<>(labelNames) : new HashSet<>();
        Set<String> allowedLower = allowedExact.stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // 数据集已有标签的两级映射：exact 优先（保留库中同时存在 "Person"/"PERSON" 的可区分性），lower 兜底（避免重复创建大小写仅有差异的标签）
        Map<String, Long> existingExactMap = new HashMap<>(getLabelNameMap(datasetImportDTO));
        Map<String, Long> existingLowerMap = existingExactMap.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue, (a, b) -> a));

        Files.readAllLines(Paths.get(labelFile)).forEach(line -> {
            if (line != null && !line.trim().isEmpty()) {
                String categoryName = line.trim();
                int lineNumber = i.getAndIncrement();
                String categoryLower = categoryName.toLowerCase();

                // 算法包标签清单过滤：先精确，再忽略大小写
                boolean allowed = allowedExact.isEmpty()
                        || allowedExact.contains(categoryName)
                        || allowedLower.contains(categoryLower);
                if (!allowed) {
                    return;
                }

                // 数据集已有标签查找：先精确，再忽略大小写；都未中才新建
                Long labelId = existingExactMap.get(categoryName);
                if (labelId == null) {
                    labelId = existingLowerMap.get(categoryLower);
                }
                if (labelId == null) {
                    Label label = Label.builder()
                            .name(categoryName)
                            .color(ConversionUtil.generateRandomColor())
                            .build();
                    insertLabelData(label, datasetImportDTO.getDatasetId());
                    labelId = label.getId();
                    if (labelId != null) {
                        existingExactMap.put(categoryName, labelId);
                        existingLowerMap.putIfAbsent(categoryLower, labelId);
                    }
                }
                categoryMap.put((long) lineNumber, labelId);
            }
        });

        long readLabelFileTime = System.currentTimeMillis() - readLabelFileStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 读取和处理标签文件耗时: " + readLabelFileTime + "ms");

        return categoryMap;
    }

    /**
     * 读取现有的标注信息
     */
    private JSONArray readExistingAnnotation(Dataset datasetById, String annotationFilePath,
                                             String baseFileName, AtomicLong totalMinioReadTime) {
        String originAnnoFileDir = datasetById.getUri() + "/" + "annotation" + "/" + baseFileName;
        JSONArray annJsonArray = new JSONArray();

        java.io.File annotationFile = new java.io.File(annotationFilePath + "/" + baseFileName);
        if (annotationFile.exists()) {
            try {
                long minioReadStart = System.currentTimeMillis();
                String fileContent = minioUtil.readString(bucket, originAnnoFileDir);
                long minioReadTime = System.currentTimeMillis() - minioReadStart;
                totalMinioReadTime.addAndGet(minioReadTime);

                annJsonArray = JSONObject.parseArray(fileContent);
            } catch (Exception ignored) {
                // 读取失败时使用默认空数组
            }
        }
        return annJsonArray;
    }

    /**
     * 构建文件创建DTO（修改为直接接受宽高）
     */
    private FileCreateDTO buildFileCreateDTO(int width, int height, String fileName, Long datasetId) {
        return FileCreateDTO.builder()
                .width(width)
                .height(height)
                .url(fileUtil.getBucketNameFilePath(datasetId, fileName, true))
                .build();
    }

    /**
     * 构建标注结果
     */
    private JSONArray buildAnnotationResult(List<JSONObject> txtToJsonObjectList, JSONArray annJsonArray,
                                            Map<Long, Long> categoryMap, int width, int height, Dataset dataset, Boolean clearExistingLabels) {
        // 用于存储现有标注的去重信息
        Set<String> existingAnnotations = new HashSet<>();
        // 根据标注类型决定使用什么字段作为去重key
        boolean isSegmentation = (dataset.getAnnotateType() != null && dataset.getAnnotateType() == 103);
        String dataFieldName = isSegmentation ? "segmentation" : "bbox";
        
        // 如果不清除已有标签，则保留现有标注并建立去重集合
        JSONArray resultArray = new JSONArray();
        if (clearExistingLabels == null || !clearExistingLabels) {
            // 保留已有标注
            for (Object annObject : annJsonArray) {
                JSONObject ann = (JSONObject) annObject;
                Long categoryId = ann.getLong("category_id");
                Object dataField = ann.get(dataFieldName);
                String annotationKey = categoryId + ":" + (dataField != null ? dataField.toString() : "");
                existingAnnotations.add(annotationKey);
            }
            resultArray.addAll(annJsonArray);
        }
        // 如果清除已有标签，resultArray保持为空，existingAnnotations也为空

        for (JSONObject jsonObject : txtToJsonObjectList) {
            List<Double> coordinates = (List<Double>) jsonObject.get("coordinates");
            Long txtId = jsonObject.getLong("labelId");
            Long dubheLabelId = categoryMap.get(txtId);

            // 如果该 txtId 不在 categoryMap 中，说明不在 labelNames，跳过
            if (dubheLabelId == null) {
                continue;
            }

            JSONObject result = initDubheAnno(dubheLabelId);

            // 根据标注类型和坐标数量决定如何处理
            if (isSegmentation && coordinates != null && coordinates.size() > 4) {
                // 语义分割：将归一化的点坐标转换为像素坐标
                // 格式: [[x1, y1], [x2, y2], ...] 每个点是一个包含两个元素的数组
                JSONArray segmentation = new JSONArray();
                
                for (int i = 0; i < coordinates.size(); i += 2) {
                    if (i + 1 < coordinates.size()) {
                        double pixelX = coordinates.get(i) * width;
                        double pixelY = coordinates.get(i + 1) * height;
                        
                        JSONArray point = new JSONArray();
                        point.add(pixelX);
                        point.add(pixelY);
                        segmentation.add(point);
                    }
                }
                
                result.put("segmentation", segmentation);
            } else if (coordinates != null && coordinates.size() >= 4) {
                // 目标检测：使用前4个值作为bbox（向后兼容）
                Double w = coordinates.get(2) * width;
                Double h = coordinates.get(3) * height;
                Double x = coordinates.get(0) * width - 0.5 * w;
                Double y = coordinates.get(1) * height - 0.5 * h;

                result.put("bbox", Arrays.asList(x, y, w, h));
            } else {
                // 坐标数量不足，跳过
                continue;
            }

            Long categoryId = result.getLong("category_id");
            Object dataField = result.get(dataFieldName);
            String resultKey = categoryId + ":" + (dataField != null ? dataField.toString() : "");

            // 如果不是重复标注，添加到结果中
            if (!existingAnnotations.contains(resultKey)) {
                resultArray.add(result);
                existingAnnotations.add(resultKey);
            }
        }

        return resultArray;
    }

    /**
     * 批量更新任务进度
     */
    private void updateTaskProgressInBatch(AtomicInteger uploadedCount, Task task, String taskKey,
                                           AtomicLong totalRedisUpdateTime) {
        int currentUploadedCount = uploadedCount.incrementAndGet();

        // 每20个文件更新一次Redis
        if (currentUploadedCount % 20 == 0) {
            long redisUpdateStart = System.currentTimeMillis();
            task.setFinished(Math.min(currentUploadedCount, task.getTotal()));
            redisUtils.set(taskKey, JSON.toJSONString(task), 100 * 60);
            long redisUpdateTime = System.currentTimeMillis() - redisUpdateStart;
            totalRedisUpdateTime.addAndGet(redisUpdateTime);
        }
    }

    /**
     * 输出数据准备阶段统计信息
     */
    private void outputDataPreparationStatistics(int processedCount, long totalImageProcessTime,
                                                 long totalTxtProcessTime, long totalMinioReadTime) {
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] ========== 数据准备阶段耗时统计 ==========");
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 总处理文件数: " + processedCount);
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 图片读取总耗时: " + totalImageProcessTime + "ms, 平均: " +
                (processedCount > 0 ? totalImageProcessTime / processedCount : 0) + "ms/文件");
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] TXT处理总耗时: " + totalTxtProcessTime + "ms, 平均: " +
                (processedCount > 0 ? totalTxtProcessTime / processedCount : 0) + "ms/文件");
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] MinIO读取总耗时: " + totalMinioReadTime + "ms, 平均: " +
                (processedCount > 0 ? totalMinioReadTime / processedCount : 0) + "ms/文件");
    }

    /**
     * 输出上传阶段统计信息
     */
    private void outputUploadStatistics(int uploadCount, long totalUploadTime, long totalRedisUpdateTime) {
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] ========== 上传阶段耗时统计 ==========");
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 上传标注总耗时: " + totalUploadTime + "ms, 平均: " +
                (uploadCount > 0 ? totalUploadTime / uploadCount : 0) + "ms/文件");
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] Redis更新总耗时: " + totalRedisUpdateTime + "ms");
    }

    /**
     * 完成任务的最终处理
     */
    private void finalizeTasks(Long datasetId, Task task, String taskKey) {
        long finallyStart = System.currentTimeMillis();

        // 确保任务的 finished 状态更新为总文件数量
        task.setFinished(task.getTotal());
        task.setStatus(TaskStatusEnum.FINISHED.getValue());
        task.setStop(true);

        // 保存更新后的任务信息
        long redisFinalSetStart = System.currentTimeMillis();
        redisUtils.set(taskKey, JSON.toJSONString(task), 100 * 60);
        long redisFinalSetTime = System.currentTimeMillis() - redisFinalSetStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 最终Redis保存任务状态耗时: " + redisFinalSetTime + "ms");

        LogUtil.info(LogEnum.BIZ_DATASET, "autoLabelEndfinally" + "状态机转换");

        // 数据库查询计时
        long finalDbQueryStart = System.currentTimeMillis();
        Dataset dataset = datasetMapper.selectById(datasetId);
        long finalDbQueryTime = System.currentTimeMillis() - finalDbQueryStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 最终数据库查询dataset耗时: " + finalDbQueryTime + "ms");

        // 状态机转换计时
        long finalStateMachineStart = System.currentTimeMillis();
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.DATA_AUTO_AUTO_LABEL_COMPLETE_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
        long finalStateMachineTime = System.currentTimeMillis() - finalStateMachineStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] 最终状态机转换耗时: " + finalStateMachineTime + "ms");

        // 回调 mineai 自迭代任务（如果是自迭代任务触发的自动标注）
        callbackSelfIterationIfNeeded(datasetId, task);

        long finallyTime = System.currentTimeMillis() - finallyStart;
        LogUtil.info(LogEnum.BIZ_DATASET, "[TIMING] finally块执行耗时: " + finallyTime + "ms");
    }

    /**
     * 如果是自迭代任务触发的自动标注，回调 mineai
     */
    private void callbackSelfIterationIfNeeded(Long datasetId, Task task) {
        try {
            // 从 task 的 jobName 判断是否为自迭代任务（格式：autolabel-iter{taskId}-r{round}-job{jobId}-ds{datasetId}）
            String jobName = task.getJobName();
            LogUtil.info(LogEnum.BIZ_DATASET, "检查自迭代回调：datasetId={}, jobName={}", datasetId, jobName);
            if (jobName != null && jobName.startsWith("autolabel-iter")) {
                String[] parts = jobName.split("-");
                LogUtil.info(LogEnum.BIZ_DATASET, "jobName 解析：parts.length={}, parts={}", parts.length, String.join(",", parts));
                if (parts.length >= 5) {
                    String jobPart = parts[3]; // job{jobId}

                    if (jobPart.startsWith("job")) {
                        Long selfIterJobId = Long.parseLong(jobPart.substring(3)); // 提取 jobId

                        try {
                            LogUtil.info(LogEnum.BIZ_DATASET, "准备调用 Feign 回调：jobId={}, datasetId={}", selfIterJobId, datasetId);
                            String result = selfIterationClient.onAutoLabelComplete("", selfIterJobId, datasetId);
                            LogUtil.info(LogEnum.BIZ_DATASET, "Feign 回调返回：result={}", result);
                            LogUtil.info(LogEnum.BIZ_DATASET, "自迭代任务自动标注完成回调成功：jobId={}, datasetId={}", selfIterJobId, datasetId);
                        } catch (Exception e) {
                            LogUtil.error(LogEnum.BIZ_DATASET, "Feign 回调失败：jobId={}, datasetId={}, error={}", selfIterJobId, datasetId, e.getMessage(), e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "自迭代任务回调失败：datasetId={}, error={}", datasetId, e.getMessage());
        }
    }

    @Override
    public List<DatasetInfoVO> getPublishedDatasetsInfo() {
        return datasetMapper.selectDatasetsWithVersionFileCount();
    }


    /**
     * 导入数据集压缩包
     */
    @Override
    public void datasetImport(DatasetImportDTO datasetImportDTO) {
        String datasetType = datasetImportDTO.getDatasetType();
        Long transferTaskId = createZipTransferTask(datasetImportDTO);
        datasetImportDTO.setTransferTaskId(transferTaskId);
        zipTransferTaskIds.put(datasetImportDTO.getDatasetId(), transferTaskId);
        failedZipTransferTasks.put(datasetImportDTO.getDatasetId(), false);
        // 更新redis
        redisUtils.hset("datasetOriginStatus", String.valueOf(datasetImportDTO.getDatasetId()), datasetMapper.selectById(datasetImportDTO.getDatasetId()).getStatus(), 60 * 60 * 12);
        // 初始化计数器
        taskCounters.put(datasetImportDTO.getDatasetId(), new AtomicInteger(0));

        if (StringUtils.isBlank(datasetType)) {
            failZipTransferTask(datasetImportDTO.getDatasetId(), "数据集压缩包格式不能为空");
            cleanupZipTransferContext(datasetImportDTO.getDatasetId());
            throw new BusinessException("数据集压缩包格式不能为空");
        }

        // State must be updated before the asynchronous task can report a failure.
        try {
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{datasetImportDTO.getDatasetId().intValue()});
                setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_EVENT);
                setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            }});
            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), "数据集导入-开始", DatasetOperationEvent.EventType.INFO, DatasetOperationEvent.OperationType.DATA_IMPORT);
        } catch (Exception e) {
            failZipTransferTask(datasetImportDTO.getDatasetId(), e.getMessage());
            cleanupZipTransferContext(datasetImportDTO.getDatasetId());
            throw new BusinessException("数据集导入启动失败: " + e.getMessage());
        }
        
        switch (datasetType) {
            case "COCO":
                pool.getExecutor().execute(() -> {
                    executeZipImport(datasetImportDTO, () -> handleCOCO(datasetImportDTO));
                });
                break;
            case "VOC":
                pool.getExecutor().execute(() -> {
                    executeZipImport(datasetImportDTO, () -> handleVoc(datasetImportDTO));
                });
                break;
            case "YOLO":
                pool.getExecutor().execute(() -> {
                    executeZipImport(datasetImportDTO, () -> handleYOLO(datasetImportDTO));
                });
                break;
            case "IMAGES_ONLY":
                pool.getExecutor().execute(() -> {
                    executeZipImport(datasetImportDTO, () -> handleImagesOnly(datasetImportDTO));
                });
                break;
            case "Segment-YOLO":
                pool.getExecutor().execute(() -> {
                    executeZipImport(datasetImportDTO, () -> handleSegmentYOLO(datasetImportDTO));
                });
                break;
            case "Segment-COCO":
                pool.getExecutor().execute(() -> {
                    executeZipImport(datasetImportDTO, () -> handleSegmentCOCO(datasetImportDTO));
                });
                break;
            case "CreateML":
                failZipTransferTask(datasetImportDTO.getDatasetId(), "暂不支持 CreateML 格式导入");
                cleanupZipTransferContext(datasetImportDTO.getDatasetId());
                break;
            default:
                failZipTransferTask(datasetImportDTO.getDatasetId(), "不支持的数据集压缩包格式: " + datasetType);
                cleanupZipTransferContext(datasetImportDTO.getDatasetId());
                break;
        }
    }

    @FunctionalInterface
    private interface ZipImportHandler {
        void run() throws Exception;
    }

    private Long createZipTransferTask(DatasetImportDTO datasetImportDTO) {
        Dataset dataset = datasetMapper.selectById(datasetImportDTO.getDatasetId());
        ImportTransferTaskCreateDTO transferTask = new ImportTransferTaskCreateDTO();
        String datasetName = dataset == null || StringUtils.isBlank(dataset.getName())
                ? "图片数据集" : dataset.getName().trim();
        transferTask.setTaskName("数据集“" + datasetName + "”导入任务");
        transferTask.setDatasetType("IMAGE");
        transferTask.setDatasetId(datasetImportDTO.getDatasetId());
        transferTask.setSourceType("LOCAL");
        transferTask.setTotalFiles(0);
        return importTransferTaskService.create(transferTask);
    }

    private void executeZipImport(DatasetImportDTO datasetImportDTO, ZipImportHandler handler) {
        Long datasetId = datasetImportDTO.getDatasetId();
        try {
            handler.run();
            if (!Boolean.TRUE.equals(failedZipTransferTasks.get(datasetId))) {
                completeZipTransferTask(datasetImportDTO, "压缩包导入完成");
            }
        } catch (Exception e) {
            failZipTransferTask(datasetId, e.getMessage());
            log.error("数据集压缩包导入失败，datasetId: {}", datasetId, e);
        } finally {
            cleanupZipTransferContext(datasetId);
        }
    }

    private void completeZipTransferTask(DatasetImportDTO datasetImportDTO, String message) {
        if (datasetImportDTO.getTransferTaskId() == null) return;
        importTransferTaskService.complete(datasetImportDTO.getTransferTaskId(), message);
    }

    private void failZipTransferTask(Long datasetId, String message) {
        Long transferTaskId = zipTransferTaskIds.get(datasetId);
        if (transferTaskId == null) {
            return;
        }
        failedZipTransferTasks.put(datasetId, true);
        importTransferTaskService.fail(transferTaskId, message);
    }

    private void cleanupZipTransferContext(Long datasetId) {
        failedZipTransferTasks.remove(datasetId);
        zipTransferTaskIds.remove(datasetId);
    }

    /**
     * 获取某数据集下 标签名称 对应 标签id
     */
    public Map<String, Long> getLabelNameMap(DatasetImportDTO datasetImportDTO) {
        // 数据集下的所有标签、id
        List<Label> labels = datasetLabelService.listLabelByDatasetId(datasetImportDTO.getDatasetId());
        List<Label> uniqueLabels = labels.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                () -> new TreeSet<>(Comparator.comparing(Label::getName))), ArrayList::new));
        return uniqueLabels.stream().collect(Collectors.toMap(Label::getName, Label::getId));
    }

    public Map<String, Long> getLabelNameMap(Long datasetId) {
        // 数据集下的所有标签、id
        List<Label> labels = datasetLabelService.listLabelByDatasetId(datasetId);
        List<Label> uniqueLabels = labels.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(
                () -> new TreeSet<>(Comparator.comparing(Label::getName))), ArrayList::new));
        return uniqueLabels.stream().collect(Collectors.toMap(Label::getName, Label::getId));
    }

    /**
     * 文件入库（无状态机转换）
     */
    public void uploadFile(Long datasetId, BatchFileCreateDTO batchFileCreateDTO) {
        List<Long> fileIds = saveDbForUploadFiles(datasetId, batchFileCreateDTO, batchFileCreateDTO.getIfImport());
    }

    /**
     * 图片重命名
     */
    public String getNewName(String imageName) {
        String suffix = imageName.substring(imageName.lastIndexOf("."));
        String prefix = imageName.split(suffix)[0];
        return prefix + "_" + IdUtil.fastSimpleUUID().substring(0, 5) + suffix;
    }

    /**
     * dataRepo图片重命名
     */
    public String getDataRepoFileNewName(String imageName, Long datasetId, Long dataRepoId) {
        String suffix = imageName.substring(imageName.lastIndexOf("."));
        String prefix = imageName.split(suffix)[0];
        return prefix + "_" + IdUtil.fastSimpleUUID().substring(0, 5) + suffix;
    }

    /**
     * 初始化dubhe标注格式
     */
    public JSONObject initDubheAnno(Long categoryId) {
        JSONObject result = new JSONObject();
        result.put("id", IdUtil.fastSimpleUUID().substring(0, 5));
        result.put("name", "");
        result.put("category_id", categoryId);
        result.put("score", 1);
        return result;
    }

    /**
     * 给每张图片上传标注（带状态机）
     */
    public void uploadAnnotationPerImage(FileCreateDTO fileCreateDTO, Long datasetId, JSONArray resultArray) {
        try {
            if (resultArray == null) {
                System.out.println("resultArray 为空");
                System.out.println("fileCreateDTO = " + fileCreateDTO);
                return;
            }
            // 查询文件ID
            Long fileId = fileService.selectOne(new QueryWrapper<org.dubhe.data.domain.entity.File>() {{
                eq("url", fileCreateDTO.getUrl());
                eq("dataset_id", datasetId);
            }}).getId();

            // 创建注解对象
            AnnotationInfoCreateDTO annotationInfo = AnnotationInfoCreateDTO.builder()
                    .annotation(resultArray.toJSONString())
                    .build();
            // 调用服务
            annotationService.finishManualForUpload(fileId, datasetId, annotationInfo);

            // 进度相关

            String key = "datasetImport" + datasetId;

            Task task = JSONObject.parseObject((String) redisUtils.get(key), Task.class);

            if (task != null) {
                AtomicInteger count = taskCounters.get(datasetId);
                int i = count.incrementAndGet();
                // 导入任务会进入该if语句，自动标注不会
                if (i % 10 == 0 || i == task.getTotal()) {
                    redisUtils.set(key, JSONObject.toJSONString(task.setFinished(i)), 60 * 60);
                }
                if (i == task.getTotal()) {
                    redisUtils.del(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw  e;
        }
    }


    public void uploadAnnotationPerImageForAutoLabel(FileCreateDTO fileCreateDTO, Long datasetId, JSONArray resultArray, boolean isModified) {
        try {
            if (resultArray == null) {
                System.out.println("resultArray 为空");
                System.out.println("fileCreateDTO = " + fileCreateDTO);
                return;
            }
            // 查询文件ID
            Long fileId = fileService.selectOne(new QueryWrapper<org.dubhe.data.domain.entity.File>() {{
                eq("url", fileCreateDTO.getUrl());
                eq("dataset_id", datasetId);
            }}).getId();

            // 创建注解对象
            AnnotationInfoCreateDTO annotationInfo = AnnotationInfoCreateDTO.builder()
                    .annotation(resultArray.toJSONString())
                    .build();
            // 调用服务
            annotationService.finishManualForAutoLabel(fileId, datasetId, annotationInfo,isModified);

            // 进度相关

            String key = "datasetImport" + datasetId;

            Task task = JSONObject.parseObject((String) redisUtils.get(key), Task.class);

            if (task != null) {
                AtomicInteger count = taskCounters.get(datasetId);
                int i = count.incrementAndGet();
                // 导入任务会进入该if语句，自动标注不会
                if (i % 10 == 0 || i == task.getTotal()) {
                    redisUtils.set(key, JSONObject.toJSONString(task.setFinished(i)), 60 * 60);
                }
                if (i == task.getTotal()) {
                    redisUtils.del(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw  e;
        }
    }



    public void uploadAnnotationBatchWithoutProgress(
            List<FileCreateDTO> fileCreateDTOList,
            Long datasetId,
            List<JSONArray> resultArrayList
    ) {
        if (fileCreateDTOList == null || resultArrayList == null || fileCreateDTOList.size() != resultArrayList.size()) {
            throw new IllegalArgumentException("文件和标注数量不一致！");
        }

        List<Long> fileIdList = new ArrayList<>();
        List<AnnotationInfoCreateDTO> annotationInfoList = new ArrayList<>();

        for (int i = 0; i < fileCreateDTOList.size(); i++) {
            FileCreateDTO fileCreateDTO = fileCreateDTOList.get(i);
            JSONArray resultArray = resultArrayList.get(i);

            if (resultArray == null) {
                System.out.println("resultArray 为空");
                System.out.println("fileCreateDTO = " + fileCreateDTO);
                continue;
            }

            // 查询文件ID
            org.dubhe.data.domain.entity.File dbFile = fileService.selectOne(new QueryWrapper<org.dubhe.data.domain.entity.File>() {{
                eq("url", fileCreateDTO.getUrl());
                eq("dataset_id", datasetId);
            }});
            if (dbFile == null) {
                System.out.println("未找到文件: " + fileCreateDTO.getUrl());
                continue;
            }
            Long fileId = dbFile.getId();
            fileIdList.add(fileId);

            // 创建注解对象
            AnnotationInfoCreateDTO annotationInfo = AnnotationInfoCreateDTO.builder()
                    .annotation(resultArray.toJSONString())
                    .build();
            annotationInfoList.add(annotationInfo);
        }

        if (!fileIdList.isEmpty()) {
            try {
                annotationService.finishManualForUploadBatch(fileIdList, datasetId, annotationInfoList);
            }catch (Exception e){
                throw e;
            }


        }
    }



    public void uploadAnnotationBatch(
            List<FileCreateDTO> fileCreateDTOList,
            Long datasetId,
            List<JSONArray> resultArrayList
    ) throws InterruptedException {
        if (fileCreateDTOList == null || resultArrayList == null || fileCreateDTOList.size() != resultArrayList.size()) {
            throw new IllegalArgumentException("文件和标注数量不一致！");
        }


        // 批量上传
        try {
            uploadAnnotationBatchWithoutProgress(fileCreateDTOList, datasetId, resultArrayList);
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }

    }

    /**
     * 解压并获取桶下相对路径
     */
    public String depressAndGetBucketPath(DatasetImportDTO datasetImportDTO) {
        String archiveUrl = datasetImportDTO.getArchiveUrl();
        String archiveObjectName = getArchiveObjectName(archiveUrl);
        String basePrefix = "dataset/" + datasetImportDTO.getDatasetId() + "/" + "unzip/";
        log.info("从 MinIO 解压上传的压缩包：bucket={}, object={}, targetPrefix={}", bucket, archiveObjectName, basePrefix);
        try {
            List<String> filePaths = minioUtil.decompress(bucket, archiveObjectName, basePrefix);
            updateZipTransferProgress(datasetImportDTO, 0, filePaths.size(), "压缩包解压完成，开始解析文件");
            if (filePaths.isEmpty()) {
                createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(),
                        "解压失败或压缩包为空", datasetImportDTO.getDatasetType());
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),new Date(),"数据集导入-解压失败或压缩包为空", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_IMPORT);
                updateDatasetState(datasetImportDTO.getDatasetId(), "解压失败或压缩包为空");
                return null;
            }
            boolean foundImages = false;
            boolean foundAnnotations = false;
            String parentPath = null;
            
            for (String path : filePaths) {
                if (path.contains("/images/")) {
                    foundImages = true;
                    // 找到images文件夹的上一级目录
                    int idx = path.indexOf("/images/");
                    if (idx > 0) {
                        parentPath = path.substring(0, idx);
                    }
                }
                if (path.contains("/annotations/")) {
                    foundAnnotations = true;
                    // 找到annotations文件夹的上一级目录
                    int idx = path.indexOf("/annotations/");
                    if (idx > 0 && parentPath == null) {
                        parentPath = path.substring(0, idx);
                    }
                }
            }
            
            // 检查是否同时找到了images和annotations文件夹
            if (!foundImages || !foundAnnotations) {
                String errorMsg = String.format("数据集导入失败：压缩包中缺少必要文件夹。找到images: %s, 找到annotations: %s", 
                    foundImages, foundAnnotations);
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), 
                    errorMsg, DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                updateDatasetState(datasetImportDTO.getDatasetId(), errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            if (parentPath == null) {
                String errorMsg = "数据集导入失败：无法确定数据集根目录";
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), 
                    errorMsg, DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                updateDatasetState(datasetImportDTO.getDatasetId(), errorMsg);
                throw new RuntimeException(errorMsg);
            }
            
            return bucket + "/" + parentPath;


        } catch (Exception e) {
            createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(),
                    "解压失败", datasetImportDTO.getDatasetType());
            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),new Date(),"数据集导入-解压失败", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_IMPORT);
            updateDatasetState(datasetImportDTO.getDatasetId(), "解压失败: " + e.getMessage());
            return null;
        }
    }


    /**
     * 仅用于 IMAGES_ONLY：解压压缩包并返回所有图片对象在 MinIO 中的路径列表
     * 路径形如：dataset/{datasetId}/unzip/.../images/xxx.jpg
     */
    public List<String> decompressAndGetImagePaths(DatasetImportDTO datasetImportDTO) {
        String archiveUrl = datasetImportDTO.getArchiveUrl();
        String archiveObjectName = getArchiveObjectName(archiveUrl);
        String basePrefix = "dataset/" + datasetImportDTO.getDatasetId() + "/" + "unzip/";
        log.info("[IMAGES_ONLY] 从 MinIO 解压上传的压缩包：bucket={}, object={}, targetPrefix={}", bucket, archiveObjectName, basePrefix);

        try {
            List<String> filePaths = minioUtil.decompress(bucket, archiveObjectName, basePrefix);
            updateZipTransferProgress(datasetImportDTO, 0, filePaths.size(), "压缩包解压完成，开始解析图片");
            if (filePaths.isEmpty()) {
                createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(),
                        "解压失败或压缩包为空", datasetImportDTO.getDatasetType());
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(),
                        "数据集导入-解压失败或压缩包为空", DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                updateDatasetState(datasetImportDTO.getDatasetId(), "解压失败或压缩包为空");
                return Collections.emptyList();
            }

            // 过滤出图片文件对象路径
            List<String> imagePaths = new ArrayList<>();
            for (String path : filePaths) {
                String lower = path.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                        || lower.endsWith(".bmp") || lower.endsWith(".webp")) {
                    imagePaths.add(path);
                }
            }

            if (imagePaths.isEmpty()) {
                String errorMsg = "IMAGES_ONLY 导入失败：压缩包中未找到任何图片文件";
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(),
                        errorMsg, DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                updateDatasetState(datasetImportDTO.getDatasetId(), errorMsg);
            }

            return imagePaths;
        } catch (Exception e) {
            log.error("[IMAGES_ONLY] 解压并获取图片路径失败", e);
            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(),
                    "数据集导入-解压失败", DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
            updateDatasetState(datasetImportDTO.getDatasetId(), "解压失败：" + e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getArchiveObjectName(String archiveUrl) {
        if (StringUtils.isBlank(archiveUrl)) {
            throw new BusinessException("数据集压缩包地址不能为空");
        }

        String normalizedUrl = archiveUrl.replace('\\', '/');
        String bucketPrefix = bucket + SymbolConstant.SLASH;
        if (!normalizedUrl.startsWith(bucketPrefix)) {
            throw new BusinessException("数据集压缩包不在当前 MinIO bucket 中: " + archiveUrl);
        }

        String objectName = normalizedUrl.substring(bucketPrefix.length());
        if (StringUtils.isBlank(objectName)) {
            throw new BusinessException("数据集压缩包对象路径不能为空");
        }
        return objectName;
    }

    /**
     * IMAGES_ONLY：处理纯图片压缩包导入
     */
    public void handleImagesOnly(DatasetImportDTO datasetImportDTO) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());
        Long datasetId = datasetImportDTO.getDatasetId();

        try {
            // 1. 解压并获取所有图片对象路径（MinIO 相对路径）
            List<String> imagePaths = decompressAndGetImagePaths(datasetImportDTO);
            if (imagePaths == null || imagePaths.isEmpty()) {
                return;
            }

            int total = imagePaths.size();

            // 初始化任务进度
            Task originTask = Task.builder()
                    .total(total)
                    .datasetId(datasetId)
                    .status(TaskStatusEnum.ING.getValue())
                    .finished(MagicNumConstant.ZERO)
                    .build();
            redisUtils.set("datasetImport" + datasetId, JSON.toJSONString(originTask), 60 * 60);
            taskCounters.put(datasetId, new AtomicInteger(0));

            // 2. 按批构造 BatchFileCreateDTO 并调用 uploadFiles
            int batchSize = 100;
            for (int i = 0; i < total; i += batchSize) {
                int fromIndex = i;
                int toIndex = Math.min(i + batchSize, total);
                List<String> batchPaths = imagePaths.subList(fromIndex, toIndex);

                futures.add(executor.submit(() -> {
                    try {
                        BatchFileCreateDTO batch = buildBatchForImagesOnly(datasetId, batchPaths);
                        if (batch.getFiles() != null && !batch.getFiles().isEmpty()) {
                            uploadFilesWithoutState(datasetId, batch);
                        }

                        // 进度统计
                        AtomicInteger count = taskCounters.get(datasetId);
                        int finished = count.addAndGet(toIndex - fromIndex);
                        String key = "datasetImport" + datasetId;
                        Task task = JSONObject.parseObject((String) redisUtils.get(key), Task.class);
                        if (task != null) {
                            task.setFinished(finished >= total ? task.getTotal() : finished);
                            redisUtils.set(key, JSONObject.toJSONString(task), 60 * 60);
                        }
                        updateZipTransferProgress(datasetImportDTO, finished, total, "图片文件处理中");
                    } catch (Throwable e) {
                        String errorMsg = "处理 IMAGES_ONLY 文件批次 [" + fromIndex + "-" + (toIndex - 1) + "] 时出错: " + e.getMessage();
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, e);
                        datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                                "数据集导入-处理图片文件出错", DatasetOperationEvent.EventType.ERROR,
                                DatasetOperationEvent.OperationType.DATA_IMPORT);
                    }
                }));
            }

            // 3. 等待所有处理任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause();
                    String errorMsg = "线程执行失败: " + (cause != null ? cause.getMessage() : ee.getMessage());
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, ee);
                    datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                            "数据集导入-处理图片文件出错", DatasetOperationEvent.EventType.ERROR,
                            DatasetOperationEvent.OperationType.DATA_IMPORT);
                } catch (Exception e) {
                    String errorMsg = "等待过程中线程任务出错: " + e.getMessage();
                    errorMessages.add(errorMsg);
                    log.error(errorMsg, e);
                    datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                            "数据集导入-处理图片文件出错", DatasetOperationEvent.EventType.ERROR,
                            DatasetOperationEvent.OperationType.DATA_IMPORT);
                }
            }

            executor.shutdown();

            // 4. 根据处理结果决定状态，清理解压目录与原压缩包
            String archiveUrl = datasetImportDTO.getArchiveUrl();
            String basePrefix = "dataset/" + datasetId + "/" + "unzip/";
            if (!errorMessages.isEmpty()) {
                updateDatasetState(datasetId, "IMAGES_ONLY 部分文件处理失败: 共 " + errorMessages.size() + " 个报错");
                cleanupZipObjects(bucket, basePrefix, archiveUrl);
                redisUtils.del("datasetImport" + datasetId);
                return;
            }

            cleanupZipObjects(bucket, basePrefix, archiveUrl);

            // 5. 状态机：导入中 -> 已导入
            Dataset dataset = Dataset.builder().id(datasetId).currentVersionName(datasetImportDTO.getCurrentVersionName()).build();
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataset});
                setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
                setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            }});
            datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                    "数据集导入-完成(IMAGES_ONLY)", DatasetOperationEvent.EventType.INFO,
                    DatasetOperationEvent.OperationType.DATA_IMPORT);
            createDatasetImportSuccessNotification(datasetId, datasetImportDTO.getDatasetType(), total);
            redisUtils.del("datasetImport" + datasetId);

        } catch (Exception e) {
            updateDatasetState(datasetId, "IMAGES_ONLY 处理过程中发生异常: " + e.getMessage());
            redisUtils.del("datasetImport" + datasetId);
            throw e;
        }
    }

    /**
     * IMAGES_ONLY：根据解压得到的 MinIO 图片路径，批量构造 BatchFileCreateDTO
     */
    private BatchFileCreateDTO buildBatchForImagesOnly(Long datasetId, List<String> imagePaths) throws IOException {
        List<FileCreateDTO> fileCreateDTOList = new ArrayList<>();

        if (imagePaths == null) {
            return BatchFileCreateDTO.builder().files(fileCreateDTOList).build();
        }

        for (String objectPath : imagePaths) {
            if (StringUtils.isBlank(objectPath)) {
                continue;
            }

            String fileName = objectPath.substring(objectPath.lastIndexOf('/') + 1);

            // 这里的 objectPath 是 MinIO 中的对象键（例如 dataset/{id}/unzip/...jpg）。
            // 1. 优先尝试通过 bucket + "/" + objectPath 计算出挂载盘上的本地路径，读取图片宽高；
            // 2. 如果本地不存在，则退回到宽高为 0，仅做 MinIO 对象拷贝。
            int width = 0;
            int height = 0;

            try {
                String fullObjectPath = bucket + "/" + objectPath;
                String absImagePath = fileUtil.getOriginFileAbsPath(fullObjectPath);
                java.io.File localFile = new java.io.File(absImagePath);
                if (localFile.exists() && localFile.isFile()) {
                    ImageDimension dim = readImageDimensions(localFile);
                    width = dim.getWidth();
                    height = dim.getHeight();
                }
            } catch (Exception e) {
                // ignore dimension read error, keep width/height as 0
            }

            String newName = getNewName(fileName);
            String targetMinioUrl = fileUtil.getBucketNameFilePath(datasetId, newName, true);
            String targetObject = fileUtil.getDatasetFilePath(datasetId, newName, true);

            try {
                minioUtil.copyObject(bucket, objectPath, targetObject);
            } catch (Exception e) {
                throw new IOException("IMAGES_ONLY copyObject failed for file: " + objectPath, e);
            }

            fileCreateDTOList.add(FileCreateDTO.builder()
                    .width(width)
                    .height(height)
                    .url(targetMinioUrl)
                    .build());
        }

        return BatchFileCreateDTO.builder()
                .files(fileCreateDTOList)
                .build();
    }

    private void updateZipTransferProgress(DatasetImportDTO datasetImportDTO, int finished, int total, String message) {
        Long transferTaskId = datasetImportDTO.getTransferTaskId();
        if (transferTaskId == null) return;

        ImportTransferTaskProgressDTO progress = new ImportTransferTaskProgressDTO();
        progress.setStage("PROCESS");
        progress.setTotalFiles(Math.max(0, total));
        progress.setSuccessFiles(Math.max(0, finished));
        progress.setProgress(total <= 0 ? 0 : Math.min(99, Math.round(finished * 100.0f / total)));
        progress.setMessage(message);
        try {
            importTransferTaskService.progress(transferTaskId, progress);
        } catch (Exception e) {
            log.warn("更新 ZIP 传输任务进度失败，transferTaskId: {}", transferTaskId, e);
        }
    }

    private void cleanupZipObjects(String bucketName, String unzipPrefix, String archiveUrl) {
        try {
            minioUtil.delFolder(bucketName, unzipPrefix);
        } catch (Exception e) {
            log.warn("清理 ZIP 解压目录失败，不影响导入结果，prefix: {}", unzipPrefix, e);
        }

        deleteZipArchiveObject(archiveUrl);
    }

    private void deleteZipArchiveObject(String archiveUrl) {
        if (archiveUrl == null || !archiveUrl.contains(bucket + "/")) return;

        String archiveObjectName = archiveUrl.substring(archiveUrl.indexOf(bucket + "/") + bucket.length() + 1);
        try {
            minioUtil.deleteObject(bucket, archiveObjectName);
        } catch (Exception e) {
            log.warn("清理原 ZIP 文件失败，不影响导入结果，object: {}", archiveObjectName, e);
        }
    }

    private void updateDatasetState(Long datasetId, String errorMsg) {
        try {
            failZipTransferTask(datasetId, errorMsg);
        } catch (Exception e) {
            log.warn("更新 ZIP 传输任务失败，datasetId: {}", datasetId, e);
        }
        try {
            System.err.println(errorMsg);
            StateMachineUtil.stateChange(new StateChangeDTO() {
                {
                    setObjectParam(new Object[]{
                            Dataset.builder()
                                    .id(datasetId)
                                    .build()
                    });
                    setEventMethodName(DataStateMachineConstant.IMPORT_ERROR_EVENT);
                    setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
                }
            });
            datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                    "数据集导入-出错：" + errorMsg,
                    DatasetOperationEvent.EventType.Warning,
                    DatasetOperationEvent.OperationType.DATA_IMPORT);
        } catch (Exception e) {
            System.err.println("更新数据集状态失败: " + e.getMessage());
        }
    }

    /**
     * 创建数据集导入成功通知
     */
    private void createDatasetImportSuccessNotification(Long datasetId, String datasetType, Integer imageCount) {
        try {
            Dataset dataset = datasetMapper.selectById(datasetId);
            if (dataset == null) {
                log.warn("数据集不存在，无法创建通知: datasetId={}", datasetId);
                return;
            }

            // 构建通知 payload
            JSONObject payload = new JSONObject();
            payload.put("datasetId", datasetId);
            payload.put("datasetName", dataset.getName());
            payload.put("datasetType", datasetType);
            if (imageCount != null) {
                payload.put("imageCount", imageCount);
            }

            // 创建通知对象并异步保存
            Notification notification = new Notification();
            notification.setToUserId(dataset.getCreateUserId());
            notification.setNotificationType(Notification.NotificationType.INFO);
            notification.setOperationType(NotificationOperationTypeEnum.DATASET_IMPORT_SUCCESS.getCode());
            notification.setPayload(payload.toJSONString());
            notification.setReadStatus(0);
            Date now = new Date();
            notification.setCreateTime(now);
            notification.setUpdateTime(now);
            notification.setDeleted(false);

            notificationService.safeInsertAsync(notification);
        } catch (Exception e) {
            log.error("创建数据集导入成功通知异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建数据集导入失败通知
     */
    private void createDatasetImportFailedNotification(Long datasetId, String errorMsg, String datasetType) {
        try {
            Dataset dataset = datasetMapper.selectById(datasetId);
            if (dataset == null) {
                log.warn("数据集不存在，无法创建通知: datasetId={}", datasetId);
                return;
            }

            // 构建通知 payload
            JSONObject payload = new JSONObject();
            payload.put("datasetId", datasetId);
            payload.put("datasetName", dataset.getName());
            payload.put("datasetType", datasetType);
            payload.put("errorMessage", errorMsg);

            // 创建通知对象并异步保存
            Notification notification = new Notification();
            notification.setToUserId(dataset.getCreateUserId());
            notification.setNotificationType(Notification.NotificationType.ERROR);
            notification.setOperationType(NotificationOperationTypeEnum.DATASET_IMPORT_FAILED.getCode());
            notification.setPayload(payload.toJSONString());
            notification.setReadStatus(0);
            Date now = new Date();
            notification.setCreateTime(now);
            notification.setUpdateTime(now);
            notification.setDeleted(false);

            notificationService.safeInsertAsync(notification);
        } catch (Exception e) {
            log.error("创建数据集导入失败通知异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 验证VOC格式
     */
    private void validateVOCFormat(java.io.File imageDir, java.io.File annoDir) throws Exception {
        log.info("验证VOC格式");

        // 获取所有图片文件
        java.io.File[] imageFiles = imageDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                   lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
        });

        if (imageFiles == null || imageFiles.length == 0) {
            throw new RuntimeException("images文件夹中没有找到图片文件");
        }

        Set<String> imageNames = new HashSet<>();
        for (java.io.File imageFile : imageFiles) {
            String imageName = imageFile.getName();
            imageNames.add(StringUtils.substringBeforeLast(imageName, "."));
        }

        // 检查每个图片对应的XML标注文件格式（不要求每个图片都有标注文件）
        List<String> invalidAnnotations = new ArrayList<>();

        for (String imageBaseName : imageNames) {
            String xmlFileName = imageBaseName + ".xml";
            java.io.File xmlFile = new java.io.File(annoDir, xmlFileName);

            // 如果标注文件不存在，跳过验证（允许没有标注的图片）
            if (!xmlFile.exists()) {
                continue;
            }

            // 验证XML格式是否正确
            try {
                JSONObject xmlJson = ConversionUtil.xmlToJsonObject(xmlFile.getAbsolutePath());
                if (xmlJson == null) {
                    invalidAnnotations.add(xmlFileName + ": XML解析失败");
                    continue;
                }

                // 检查必要的字段
                if (!xmlJson.containsKey("size")) {
                    invalidAnnotations.add(xmlFileName + ": 缺少size字段");
                    continue;
                }

                JSONObject size = xmlJson.getJSONObject("size");
                if (size == null || !size.containsKey("width") || !size.containsKey("height")) {
                    invalidAnnotations.add(xmlFileName + ": size字段格式不正确");
                    continue;
                }

                // 检查是否有object字段（允许为空，但格式要正确）
                Object objectObj = xmlJson.get("object");
                if (objectObj != null) {
                    if (objectObj instanceof JSONArray) {
                        JSONArray objects = (JSONArray) objectObj;
                        for (int i = 0; i < objects.size(); i++) {
                            JSONObject obj = objects.getJSONObject(i);
                            if (!obj.containsKey("name") || !obj.containsKey("bndbox")) {
                                invalidAnnotations.add(xmlFileName + ": object字段格式不正确");
                                break;
                            }
                            JSONObject bndbox = obj.getJSONObject("bndbox");
                            if (bndbox == null || !bndbox.containsKey("xmin") || !bndbox.containsKey("ymin") ||
                                !bndbox.containsKey("xmax") || !bndbox.containsKey("ymax")) {
                                invalidAnnotations.add(xmlFileName + ": bndbox字段格式不正确");
                                break;
                            }
                        }
                    } else if (objectObj instanceof JSONObject) {
                        JSONObject obj = (JSONObject) objectObj;
                        if (!obj.containsKey("name") || !obj.containsKey("bndbox")) {
                            invalidAnnotations.add(xmlFileName + ": object字段格式不正确");
                        }
                    }
                }

            } catch (Exception e) {
                invalidAnnotations.add(xmlFileName + ": " + e.getMessage());
            }
        }

        // 汇总错误信息
        if (!invalidAnnotations.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("VOC格式验证失败: ");
            errorMsg.append("标注格式错误: ").append(String.join(", ", invalidAnnotations.subList(0, Math.min(5, invalidAnnotations.size()))));
            if (invalidAnnotations.size() > 5) {
                errorMsg.append(" 等共").append(invalidAnnotations.size()).append("个文件");
            }
            throw new RuntimeException(errorMsg.toString());
        }

        log.info("VOC格式验证通过，共{}张图片", imageFiles.length);
    }

    /**
     * 验证COCO格式
     */
    private void validateCOCOFormat(java.io.File imageDir, java.io.File annoDir, String unzipPath) throws Exception {
        log.info("验证COCO格式");

        // 检查是否有coco_info.json文件
        java.io.File jsonFile = new java.io.File(annoDir, "coco_info.json");
        if (!jsonFile.exists()) {
            throw new RuntimeException("annotations文件夹中缺少coco_info.json文件");
        }

        // 读取并验证JSON格式
        String jsonStr;
        try {
            String relativeJsonPath = unzipPath.split(bucket + "/")[1] + "/annotations/coco_info.json";
            jsonStr = minioUtil.readString(bucket, relativeJsonPath);
        } catch (Exception e) {
            // 如果minio读取失败，尝试直接读取文件
            jsonStr = new String(Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath())));
        }

        JSONObject annoObject;
        try {
            annoObject = JSON.parseObject(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException("coco_info.json文件格式错误: " + e.getMessage());
        }

        // 检查必要的字段
        if (!annoObject.containsKey("images") || !annoObject.containsKey("annotations") || !annoObject.containsKey("categories")) {
            throw new RuntimeException("coco_info.json缺少必要字段: 需要包含images、annotations、categories");
        }

        JSONArray images = annoObject.getJSONArray("images");
        JSONArray annotations = annoObject.getJSONArray("annotations");
        JSONArray categories = annoObject.getJSONArray("categories");

        if (images == null || images.isEmpty()) {
            throw new RuntimeException("coco_info.json中images数组为空");
        }

        if (categories == null || categories.isEmpty()) {
            throw new RuntimeException("coco_info.json中categories数组为空");
        }

        // 验证images数组格式
        Set<String> imageFileNames = new HashSet<>();
        for (int i = 0; i < images.size(); i++) {
            JSONObject image = images.getJSONObject(i);
            if (!image.containsKey("id") || !image.containsKey("file_name")) {
                throw new RuntimeException("coco_info.json中images数组格式错误: 缺少id或file_name字段");
            }
            imageFileNames.add(image.getString("file_name"));
        }

        // 验证annotations数组格式
        Set<Long> imageIds = new HashSet<>();
        for (int i = 0; i < annotations.size(); i++) {
            JSONObject annotation = annotations.getJSONObject(i);
            if (!annotation.containsKey("image_id") || !annotation.containsKey("category_id") || !annotation.containsKey("bbox")) {
                throw new RuntimeException("coco_info.json中annotations数组格式错误: 缺少必要字段");
            }
            imageIds.add(annotation.getLong("image_id"));

            // 验证bbox格式（应该是4个元素的数组）
            JSONArray bbox = annotation.getJSONArray("bbox");
            if (bbox == null || bbox.size() != 4) {
                throw new RuntimeException("coco_info.json中annotations的bbox格式错误: 应为4个元素的数组");
            }
        }

        // 验证categories数组格式
        Set<Long> categoryIds = new HashSet<>();
        for (int i = 0; i < categories.size(); i++) {
            JSONObject category = categories.getJSONObject(i);
            if (!category.containsKey("id") || !category.containsKey("name")) {
                throw new RuntimeException("coco_info.json中categories数组格式错误: 缺少id或name字段");
            }
            categoryIds.add(category.getLong("id"));
        }

        // 检查annotations中的category_id是否都在categories中存在
        for (int i = 0; i < annotations.size(); i++) {
            JSONObject annotation = annotations.getJSONObject(i);
            Long categoryId = annotation.getLong("category_id");
            if (!categoryIds.contains(categoryId)) {
                throw new RuntimeException("coco_info.json中annotations的category_id " + categoryId + " 在categories中不存在");
            }
        }

        // 检查images文件夹中是否有对应的图片文件（至少检查前几个）
        int checkedCount = 0;
        int maxCheck = Math.min(10, imageFileNames.size());
        for (String fileName : imageFileNames) {
            java.io.File imageFile = new java.io.File(imageDir, fileName);
            if (!imageFile.exists()) {
                throw new RuntimeException("coco_info.json中引用的图片文件不存在: " + fileName);
            }
            checkedCount++;
            if (checkedCount >= maxCheck) {
                break;
            }
        }

        log.info("COCO格式验证通过，共{}张图片，{}个标注", images.size(), annotations.size());
    }

    /**
     * 验证YOLO格式
     */
    private void validateYOLOFormat(java.io.File imageDir, java.io.File annoDir) throws Exception {
        log.info("验证YOLO格式");

        // 检查是否有classes.txt文件
        java.io.File classesFile = new java.io.File(annoDir, "classes.txt");
        if (!classesFile.exists()) {
            throw new RuntimeException("annotations文件夹中缺少classes.txt文件");
        }

        // 读取classes.txt
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(classesFile.getAbsolutePath()));
        } catch (Exception e) {
            throw new RuntimeException("读取classes.txt失败: " + e.getMessage());
        }

        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("classes.txt文件为空");
        }

        // 获取所有图片文件
        java.io.File[] imageFiles = imageDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                   lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
        });

        if (imageFiles == null || imageFiles.length == 0) {
            throw new RuntimeException("images文件夹中没有找到图片文件");
        }

        // 检查每个图片对应的txt标注文件格式（不要求每个图片都有标注文件）
        List<String> invalidAnnotations = new ArrayList<>();

        for (java.io.File imageFile : imageFiles) {
            String imageBaseName = StringUtils.substringBeforeLast(imageFile.getName(), ".");
            String txtFileName = imageBaseName + ".txt";
            java.io.File txtFile = new java.io.File(annoDir, txtFileName);

            // 如果标注文件不存在，跳过验证（允许没有标注的图片）
            if (!txtFile.exists()) {
                continue;
            }

            // 验证txt格式是否正确
            try {
                List<JSONObject> txtObjects = ConversionUtil.txtToJsonObjectList(txtFile.getAbsolutePath());
                if (txtObjects == null) {
                    // 空文件也是允许的（无标注的图片）
                    continue;
                }

                // 验证每行的格式
                for (JSONObject obj : txtObjects) {
                    if (!obj.containsKey("labelId")) {
                        invalidAnnotations.add(txtFileName + ": 缺少labelId字段");
                        break;
                    }

                    Long labelId = obj.getLong("labelId");
                    if (labelId < 0 || labelId >= lines.size()) {
                        invalidAnnotations.add(txtFileName + ": labelId " + labelId + " 超出classes.txt的范围(0-" + (lines.size() - 1) + ")");
                        break;
                    }

                    // YOLO格式（目标检测）必须有newBbox字段且为4个坐标值
                    if (!obj.containsKey("newBbox")) {
                        // 检查是否有coordinates字段，如果有且不是4个值，说明是分割格式
                        if (obj.containsKey("coordinates")) {
                            JSONArray coordinates = obj.getJSONArray("coordinates");
                            if (coordinates != null && coordinates.size() > 4) {
                                invalidAnnotations.add(txtFileName + ": 检测到分割格式（多个坐标点），但选择的是YOLO格式（目标检测，需要4个坐标值）。请选择Segment-YOLO格式");
                                break;
                            }
                        }
                        invalidAnnotations.add(txtFileName + ": YOLO格式（目标检测）必须包含4个坐标值，但缺少newBbox字段");
                        break;
                    }

                    JSONArray bbox = obj.getJSONArray("newBbox");
                    if (bbox == null || bbox.size() != 4) {
                        invalidAnnotations.add(txtFileName + ": YOLO格式（目标检测）的bbox应为4个值，当前为" + (bbox != null ? bbox.size() : 0) + "个值");
                        break;
                    }

                    // 验证坐标值范围（YOLO格式应该是0-1之间的归一化值）
                    for (int i = 0; i < 4; i++) {
                        double val = bbox.getDouble(i);
                        if (val < 0 || val > 1) {
                            invalidAnnotations.add(txtFileName + ": bbox坐标值超出范围[0,1]: " + val);
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                invalidAnnotations.add(txtFileName + ": " + e.getMessage());
            }
        }

        // 汇总错误信息
        if (!invalidAnnotations.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("YOLO格式验证失败: ");
            errorMsg.append("标注格式错误: ").append(String.join(", ", invalidAnnotations.subList(0, Math.min(5, invalidAnnotations.size()))));
            if (invalidAnnotations.size() > 5) {
                errorMsg.append(" 等共").append(invalidAnnotations.size()).append("个文件");
            }
            throw new RuntimeException(errorMsg.toString());
        }

        log.info("YOLO格式验证通过，共{}张图片", imageFiles.length);
    }

    /**
     * 验证Segment-YOLO格式
     */
    private void validateSegmentYOLOFormat(java.io.File imageDir, java.io.File annoDir) throws Exception {
        log.info("验证Segment-YOLO格式");

        // 检查是否有classes.txt文件
        java.io.File classesFile = new java.io.File(annoDir, "classes.txt");
        if (!classesFile.exists()) {
            throw new RuntimeException("annotations文件夹中缺少classes.txt文件");
        }

        // 读取classes.txt
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(classesFile.getAbsolutePath()));
        } catch (Exception e) {
            throw new RuntimeException("读取classes.txt失败: " + e.getMessage());
        }

        if (lines == null || lines.isEmpty()) {
            throw new RuntimeException("classes.txt文件为空");
        }

        // 获取所有图片文件
        java.io.File[] imageFiles = imageDir.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                   lowerName.endsWith(".png") || lowerName.endsWith(".bmp");
        });

        if (imageFiles == null || imageFiles.length == 0) {
            throw new RuntimeException("images文件夹中没有找到图片文件");
        }

        // 检查每个图片对应的txt标注文件格式（不要求每个图片都有标注文件）
        List<String> invalidAnnotations = new ArrayList<>();

        for (java.io.File imageFile : imageFiles) {
            String imageBaseName = StringUtils.substringBeforeLast(imageFile.getName(), ".");
            String txtFileName = imageBaseName + ".txt";
            java.io.File txtFile = new java.io.File(annoDir, txtFileName);

            // 如果标注文件不存在，跳过验证（允许没有标注的图片）
            if (!txtFile.exists()) {
                continue;
            }

            // 验证txt格式是否正确
            try {
                List<JSONObject> txtObjects = ConversionUtil.txtToJsonObjectList(txtFile.getAbsolutePath());
                if (txtObjects == null) {
                    // 空文件也是允许的（无标注的图片）
                    continue;
                }

                // 验证每行的格式
                for (JSONObject obj : txtObjects) {
                    if (!obj.containsKey("labelId")) {
                        invalidAnnotations.add(txtFileName + ": 缺少labelId字段");
                        break;
                    }

                    Long labelId = obj.getLong("labelId");
                    if (labelId < 0 || labelId >= lines.size()) {
                        invalidAnnotations.add(txtFileName + ": labelId " + labelId + " 超出classes.txt的范围(0-" + (lines.size() - 1) + ")");
                        break;
                    }

                    // Segment-YOLO格式（分割）必须有coordinates字段且坐标点数量大于4（至少3个点，6个坐标值）
                    if (!obj.containsKey("coordinates")) {
                        // 检查是否有newBbox字段，如果有且是4个值，说明是目标检测格式
                        if (obj.containsKey("newBbox")) {
                            JSONArray bbox = obj.getJSONArray("newBbox");
                            if (bbox != null && bbox.size() == 4) {
                                invalidAnnotations.add(txtFileName + ": 检测到目标检测格式（4个坐标值），但选择的是Segment-YOLO格式（分割，需要多个坐标点）。请选择YOLO格式");
                                break;
                            }
                        }
                        invalidAnnotations.add(txtFileName + ": Segment-YOLO格式（分割）必须包含多个坐标点，但缺少coordinates字段");
                        break;
                    }

                    JSONArray coordinates = obj.getJSONArray("coordinates");
                    if (coordinates == null || coordinates.size() < 6) {
                        invalidAnnotations.add(txtFileName + ": Segment-YOLO格式（分割）的coordinates至少需要6个值（3个点），当前为" + (coordinates != null ? coordinates.size() : 0) + "个值");
                        break;
                    }

                    // 坐标点数量必须是偶数（每个点有x和y两个坐标）
                    if (coordinates.size() % 2 != 0) {
                        invalidAnnotations.add(txtFileName + ": Segment-YOLO格式的coordinates数量必须是偶数（每个点有x和y两个坐标），当前为" + coordinates.size() + "个值");
                        break;
                    }

                    // 验证坐标值范围（应该是0-1之间的归一化值）
                    for (int i = 0; i < coordinates.size(); i++) {
                        double val = coordinates.getDouble(i);
                        if (val < 0 || val > 1) {
                            invalidAnnotations.add(txtFileName + ": coordinates坐标值超出范围[0,1]: " + val);
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                invalidAnnotations.add(txtFileName + ": " + e.getMessage());
            }
        }

        // 汇总错误信息
        if (!invalidAnnotations.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Segment-YOLO格式验证失败: ");
            errorMsg.append("标注格式错误: ").append(String.join(", ", invalidAnnotations.subList(0, Math.min(5, invalidAnnotations.size()))));
            if (invalidAnnotations.size() > 5) {
                errorMsg.append(" 等共").append(invalidAnnotations.size()).append("个文件");
            }
            throw new RuntimeException(errorMsg.toString());
        }

        log.info("Segment-YOLO格式验证通过，共{}张图片", imageFiles.length);
    }

    /**
     * 验证Segment-COCO格式
     */
    private void validateSegmentCOCOFormat(java.io.File imageDir, java.io.File annoDir, String unzipPath) throws Exception {
        log.info("验证Segment-COCO格式");

        // Segment-COCO格式与COCO类似，但annotations中包含segmentation字段而不是bbox
        // 检查是否有coco_info.json文件
        java.io.File jsonFile = new java.io.File(annoDir, "coco_info.json");
        if (!jsonFile.exists()) {
            throw new RuntimeException("annotations文件夹中缺少coco_info.json文件");
        }

        // 读取并验证JSON格式
        String jsonStr;
        try {
            String relativeJsonPath = unzipPath.split(bucket + "/")[1] + "/annotations/coco_info.json";
            jsonStr = minioUtil.readString(bucket, relativeJsonPath);
        } catch (Exception e) {
            jsonStr = new String(Files.readAllBytes(Paths.get(jsonFile.getAbsolutePath())));
        }

        JSONObject annoObject;
        try {
            annoObject = JSON.parseObject(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException("coco_info.json文件格式错误: " + e.getMessage());
        }

        // 检查必要的字段
        if (!annoObject.containsKey("images") || !annoObject.containsKey("annotations") || !annoObject.containsKey("categories")) {
            throw new RuntimeException("coco_info.json缺少必要字段: 需要包含images、annotations、categories");
        }

        JSONArray images = annoObject.getJSONArray("images");
        JSONArray annotations = annoObject.getJSONArray("annotations");
        JSONArray categories = annoObject.getJSONArray("categories");

        if (images == null || images.isEmpty()) {
            throw new RuntimeException("coco_info.json中images数组为空");
        }

        if (categories == null || categories.isEmpty()) {
            throw new RuntimeException("coco_info.json中categories数组为空");
        }

        // 验证annotations数组格式，检查是否有segmentation字段
        for (int i = 0; i < annotations.size(); i++) {
            JSONObject annotation = annotations.getJSONObject(i);
            if (!annotation.containsKey("image_id") || !annotation.containsKey("category_id")) {
                throw new RuntimeException("coco_info.json中annotations数组格式错误: 缺少必要字段");
            }

            // Segment-COCO应该有segmentation字段
            if (!annotation.containsKey("segmentation")) {
                throw new RuntimeException("coco_info.json中annotations缺少segmentation字段（Segment-COCO格式要求）");
            }

            JSONArray segmentation = annotation.getJSONArray("segmentation");
            if (segmentation == null || segmentation.isEmpty()) {
                throw new RuntimeException("coco_info.json中annotations的segmentation字段为空");
            }
        }

        log.info("Segment-COCO格式验证通过，共{}张图片，{}个标注", images.size(), annotations.size());
    }

    /**
     * 处理VOC格式
     */
    public void handleVoc(DatasetImportDTO datasetImportDTO) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

        try {
            // 1. 解压
            String unzipPath = depressAndGetBucketPath(datasetImportDTO);
            if (unzipPath == null) {
                // depressAndGetBucketPath 内部已处理状态更新
                return;
            }

            // 2. 处理解压后的文件
            String absUnzipPath = fileUtil.getOriginFileAbsPath(unzipPath);
            String absAnnoDir = absUnzipPath + "/annotations";
            String relativeImageDir = unzipPath.split(bucket + "/")[1] + "/images";
            java.io.File imageDir = new java.io.File(absUnzipPath, "images");
            java.io.File annoDir = new java.io.File(absAnnoDir);

            // 2.1 格式验证（解压后立即验证）
            try {
                validateVOCFormat(imageDir, annoDir);
            } catch (Exception e) {
                log.error("VOC格式验证失败: {}", e.getMessage(), e);
                updateDatasetState(datasetImportDTO.getDatasetId(), "VOC格式验证失败: " + e.getMessage());
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                    new Date(), "数据集导入-格式验证失败: " + e.getMessage(),
                    DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                // 创建通知
                createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(),
                    "VOC格式验证失败: " + e.getMessage(), datasetImportDTO.getDatasetType());
                // 清理解压文件
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                }
                return;
            }

            // 确保这是目录
            if (imageDir.isDirectory()) {
                log.info("处理图片文件夹: {} 中", imageDir.getAbsolutePath());
                // 获取目录中的文件和子目录
                java.io.File[] files = imageDir.listFiles();
                List<java.io.File> fileList = Arrays.asList(files);

                // 3. 预处理：收集所有需要创建的新标签并创建（在主线程中）
                log.info("开始预处理标签，文件总数: {}", fileList.size());
                Set<String> allNewLabels = collectAllNewLabels(fileList, absAnnoDir, datasetImportDTO);
                if (!allNewLabels.isEmpty()) {
                    log.info("发现需要创建的新标签: {}", allNewLabels);
                    createNewLabelsIfNotExists(allNewLabels, datasetImportDTO);
                    log.info("标签预处理完成");
                }

                Task originTask = Task.builder()
                        .total(files.length)
                        .datasetId(datasetImportDTO.getDatasetId())
                        .status(TaskStatusEnum.ING.getValue())
                        .finished(MagicNumConstant.ZERO)
                        .build();
                redisUtils.set("datasetImport" + datasetImportDTO.getDatasetId(), JSON.toJSONString(originTask), 60 * 60);

                // 4. 分批处理文件（此时标签已全部创建完成）
                int batchSize = 100;
                int total = fileList.size();
                for (int i = 0; i < total; i += batchSize) {
                    int fromIndex = i;
                    int toIndex = Math.min(i + batchSize, total);
                    List<java.io.File> batchFiles = fileList.subList(fromIndex, toIndex);

                    futures.add(executor.submit(() -> {
                        try {
                            // 调用批量处理VOC文件的方法
                            handleVOCFiles(datasetImportDTO, batchFiles, absAnnoDir, relativeImageDir);

                            // 进度统计
                            AtomicInteger count = taskCounters.get(datasetImportDTO.getDatasetId());
                            int i2 = count.addAndGet(toIndex - fromIndex);

                            // 进度缓存逻辑
                            String key = "datasetImport" + datasetImportDTO.getDatasetId();
                            final Task task = JSONObject.parseObject((String) redisUtils.get(key), Task.class);

                            if (task != null) {
                                task.setFinished(i2 >= total ? task.getTotal() : i2);
                                redisUtils.set(key, JSONObject.toJSONString(task), 60 * 60);
                            }
                            updateZipTransferProgress(datasetImportDTO, i2, total, "VOC 文件处理中");

                        } catch (Throwable e) {
                            String errorMsg = "处理VOC文件批次 [" + fromIndex + "-" + (toIndex - 1) + "] 时出错: " + e.getMessage();
                            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                    new Date(), "数据集导入-处理VOC文件出错",
                                    DatasetOperationEvent.EventType.ERROR,
                                    DatasetOperationEvent.OperationType.DATA_IMPORT);
                            errorMessages.add(errorMsg);
                            log.error(errorMsg, e);
                        }
                    }));
                }

                // 等待所有处理任务完成
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause();
                        String errorMsg = "线程执行失败: " + (cause != null ? cause.getMessage() : ee.getMessage());
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, ee);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                new Date(), "数据集导入-处理VOC文件出错",
                                DatasetOperationEvent.EventType.ERROR,
                                DatasetOperationEvent.OperationType.DATA_IMPORT);
                    } catch (Exception e) {
                        String errorMsg = "等待过程中线程任务出错: " + e.getMessage();
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, e);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                new Date(), "数据集导入-处理VOC文件出错",
                                DatasetOperationEvent.EventType.ERROR,
                                DatasetOperationEvent.OperationType.DATA_IMPORT);
                    }
                }

                executor.shutdown();

                // 5. 根据处理结果决定状态
                if (!errorMessages.isEmpty()) {
                    updateDatasetState(datasetImportDTO.getDatasetId(), "部分VOC文件处理失败: 共 " + errorMessages.size() + " 个报错");
                    // 删除文件夹
                    if (unzipPath.startsWith(bucket + "/")) {
                        String objectKey = unzipPath.substring(bucket.length() + 1);
                        minioUtil.delFolder(bucket, objectKey);
                        deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
                    }
                    redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
                    return;
                }

                // 删除文件夹
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                    deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
                }

                // 执行状态转换导入中->标注中/标注完成
                Dataset dataset = Dataset.builder().id(datasetImportDTO.getDatasetId()).currentVersionName(datasetImportDTO.getCurrentVersionName()).build();
                StateMachineUtil.stateChange(new StateChangeDTO() {{
                    setObjectParam(new Object[]{dataset});
                    setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
                    setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
                }});
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                        new Date(), "数据集导入-完成",
                        DatasetOperationEvent.EventType.INFO,
                        DatasetOperationEvent.OperationType.DATA_IMPORT);
                createDatasetImportSuccessNotification(datasetImportDTO.getDatasetId(), datasetImportDTO.getDatasetType(), files != null ? files.length : null);
                redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());

            } else {
                throw new Exception(imageDir + "不是路径");
            }

        } catch (Exception e) {
            updateDatasetState(datasetImportDTO.getDatasetId(), "处理过程中发生异常: " + e.getMessage());
            redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
            // 不想抛出直接return也行
            throw e;
        }
    }

    /**
     * 收集所有需要创建的新标签
     */
    private Set<String> collectAllNewLabels(List<java.io.File> fileList, String absAnnoDir, DatasetImportDTO datasetImportDTO) {
        Set<String> allNewLabels = new HashSet<>();
        Map<String, Long> labelNameMap = getLabelNameMap(datasetImportDTO);

        for (java.io.File file : fileList) {
            try {
                String annoFile = StringUtils.substringBeforeLast(file.getName(), ".") + ".xml";
                JSONObject xmlToJsonObject = ConversionUtil.xmlToJsonObject(absAnnoDir + "/" + annoFile);

                if (xmlToJsonObject == null) {
                    continue;
                }

                Object object = xmlToJsonObject.get("object");
                JSONArray xmlObject = new JSONArray();
                if (object instanceof JSONArray) {
                    xmlObject = (JSONArray) object;
                } else if (object instanceof JSONObject) {
                    xmlObject.add(object);
                }

                // 收集需要创建的新标签
                for (Object o : xmlObject) {
                    JSONObject jsonObject = (JSONObject) o;
                    String categoryName = jsonObject.getString("name");
                    if (!labelNameMap.containsKey(categoryName)) {
                        allNewLabels.add(categoryName);
                    }
                }
            } catch (Exception e) {
                // 记录但不中断处理
                log.warn("预处理文件失败: {}, 错误: {}", file.getName(), e.getMessage());
            }
        }

        return allNewLabels;
    }

    /**
     * 批量创建新标签（如果不存在的话）
     */
    private void createNewLabelsIfNotExists(Set<String> newLabels, DatasetImportDTO datasetImportDTO) {
        for (String categoryName : newLabels) {
            try {
                Label label = Label.builder()
                        .name(categoryName)
                        .color(ConversionUtil.generateRandomColor())
                        .build();
                insertLabelData(label, datasetImportDTO.getDatasetId());
                log.info("成功创建标签: {}", categoryName);
            } catch (Exception e) {
                throw new RuntimeException("创建标签失败: " + categoryName, e);
            }
        }
    }



    /**
     * 处理VOC格式下的每张图片
     */
    public void handleVocFile(DatasetImportDTO datasetImportDTO, java.io.File file, String annoDir, String relativeImageDir) throws Exception {
        // xml to jsonObject
        try {
            String annoFile = StringUtils.substringBeforeLast(file.getName(), ".") + ".xml";
            JSONObject xmlToJsonObject = ConversionUtil.xmlToJsonObject(annoDir + "/" + annoFile);
            JSONArray xmlObject = new JSONArray();
            JSONObject xmlSize = xmlToJsonObject.getJSONObject("size");
            Object object = xmlToJsonObject.get("object");
            if (object instanceof JSONArray) {
                xmlObject = (JSONArray) object;
            } else if (object instanceof JSONObject) {
                xmlObject.add(object);
            }
            // 图片文件信息
            String newName = getNewName(file.getName());
            FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                    .width(xmlSize.getInteger("width"))
                    .height(xmlSize.getInteger("height"))
                    .url(fileUtil.getBucketNameFilePath(datasetImportDTO.getDatasetId(), newName, true))
                    .build();
            // 构建默认标注文件
            JSONArray resultArray = new JSONArray();
            for (Object o : xmlObject) {
                JSONObject jsonObject = (JSONObject) o;
                JSONObject bndbox = jsonObject.getJSONObject("bndbox");
                String categoryName = jsonObject.getString("name");
                if (!getLabelNameMap(datasetImportDTO).containsKey(categoryName)) {
                    Label label = Label.builder()
                            .name(categoryName)
                            .color(ConversionUtil.generateRandomColor())
                            .build();
                    insertLabelData(label, datasetImportDTO.getDatasetId());
                }
                JSONObject result = initDubheAnno(getLabelNameMap(datasetImportDTO).get(categoryName));
                Double xmin = Double.parseDouble(bndbox.getString("xmin"));
                Double ymin = Double.parseDouble(bndbox.getString("ymin"));
                Double xmax = Double.parseDouble(bndbox.getString("xmax"));
                Double ymax = Double.parseDouble(bndbox.getString("ymax"));
                result.put("bbox", new ArrayList<Double>() {{
                    add(xmin);
                    add(ymin);
                    add(xmax - xmin);
                    add(ymax - ymin);
                }});
                resultArray.add(result);
            }
            // 转移图片
            String sourceFile = relativeImageDir + "/" + file.getName();
            String targetFile = fileUtil.getDatasetFilePath(datasetImportDTO.getDatasetId(), newName, true);
            minioUtil.copyObject(bucket, sourceFile, targetFile);
            // 文件上传接口
            uploadFile(datasetImportDTO.getDatasetId(), BatchFileCreateDTO.builder()
                    .files(new ArrayList<FileCreateDTO>() {{
                        add(fileCreateDTO);
                    }}).build());
            // 标注上传接口
            uploadAnnotationPerImage(fileCreateDTO, datasetImportDTO.getDatasetId(), resultArray);
        } catch (NumberFormatException e) {
            throw new Exception("处理VOC文件" + file.getName() + "失败: " + e.getMessage(), e);
        }
    }


    public void handleVOCFiles(
            DatasetImportDTO datasetImportDTO,
            List<java.io.File> files,
            String annoDir,
            String relativeImageDir
    ) throws Exception {
        List<FileCreateDTO> fileCreateDTOList = new ArrayList<>();
        List<JSONArray> resultArrayList = new ArrayList<>();

        // 获取标签映射（此时所有标签都已创建完成）
        Map<String, Long> labelNameMap = getLabelNameMap(datasetImportDTO);

        // 处理文件和标注数据
        for (java.io.File file : files) {
            try {
                // xml to jsonObject
                String annoFile = StringUtils.substringBeforeLast(file.getName(), ".") + ".xml";
                String annoFilePath = annoDir + "/" + annoFile;

                int width = 0;
                int height = 0;
                JSONArray xmlObject = new JSONArray();

                // 先检查标注文件是否存在
                java.io.File xmlFile = new java.io.File(annoFilePath);
                if (xmlFile.exists()) {
                    JSONObject xmlToJsonObject = ConversionUtil.xmlToJsonObject(annoFilePath);
                    if (xmlToJsonObject != null) {
                        JSONObject xmlSize = xmlToJsonObject.getJSONObject("size");
                        width = xmlSize.getInteger("width");
                        height = xmlSize.getInteger("height");
                        Object object = xmlToJsonObject.get("object");
                        if (object instanceof JSONArray) {
                            xmlObject = (JSONArray) object;
                        } else if (object instanceof JSONObject) {
                            xmlObject.add(object);
                        }
                    }
                }

                // 如果没有标注文件或解析失败，从图片文件读取宽高
                if (width == 0 || height == 0) {
                    try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
                        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                        if (readers.hasNext()) {
                            ImageReader reader = readers.next();
                            reader.setInput(stream);
                            width = reader.getWidth(0);
                            height = reader.getHeight(0);
                            reader.dispose();
                        }
                    }
                }

                // 图片文件信息
                String newName = getNewName(file.getName());
                FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                        .width(width)
                        .height(height)
                        .url(fileUtil.getBucketNameFilePath(datasetImportDTO.getDatasetId(), newName, true))
                        .build();

                // 构建默认标注文件（如果没有标注文件，则为空数组，图片状态仍为已标注）
                JSONArray resultArray = new JSONArray();
                for (Object o : xmlObject) {
                    JSONObject jsonObject = (JSONObject) o;
                    JSONObject bndbox = jsonObject.getJSONObject("bndbox");
                    String categoryName = jsonObject.getString("name");

                    Long labelId = labelNameMap.get(categoryName);
                    if (labelId != null) {
                        JSONObject result = initDubheAnno(labelId);
                        Double xmin = Double.parseDouble(bndbox.getString("xmin"));
                        Double ymin = Double.parseDouble(bndbox.getString("ymin"));
                        Double xmax = Double.parseDouble(bndbox.getString("xmax"));
                        Double ymax = Double.parseDouble(bndbox.getString("ymax"));
                        result.put("bbox", Arrays.asList(xmin, ymin, xmax - xmin, ymax - ymin));
                        resultArray.add(result);
                    } else {
                        // 理论上不应该发生，因为所有标签都已预创建
                        log.warn("未找到标签: {}，可能是预处理阶段遗漏", categoryName);
                    }
                }

                // 转移图片到目标存储
                String sourceFile = relativeImageDir + "/" + file.getName();
                String targetFile = fileUtil.getDatasetFilePath(datasetImportDTO.getDatasetId(), newName, true);
                try {
                    minioUtil.copyObject(bucket, sourceFile, targetFile);
                } catch (Exception e) {
                    throw new IOException("minioUtil.copyObject failed for file: " + file.getName(), e);
                }

                // 收集上传信息
                fileCreateDTOList.add(fileCreateDTO);
                resultArrayList.add(resultArray);

            } catch (NumberFormatException e) {
                throw new Exception("处理VOC文件" + file.getName() + "失败: " + e.getMessage(), e);
            } catch (Exception e) {
                throw new Exception("处理文件" + file.getName() + "时发生错误: " + e.getMessage(), e);
            }
        }

        // 批量上传图片信息
        if (!fileCreateDTOList.isEmpty()) {
            try {
                uploadFile(datasetImportDTO.getDatasetId(),
                        BatchFileCreateDTO.builder().files(fileCreateDTOList).build());
            } catch (Exception e) {
                throw new Exception("批量上传文件信息失败: " + e.getMessage(), e);
            }

            try {
                // 批量上传标注信息（不含进度统计）
                uploadAnnotationBatch(fileCreateDTOList, datasetImportDTO.getDatasetId(), resultArrayList);
            } catch (Exception e) {
                throw new Exception("批量上传标注信息失败: " + e.getMessage(), e);
            }
        }
    }
    /**
     * 处理COCO格式
     */
    public void handleCOCO(DatasetImportDTO datasetImportDTO) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

        try {
            // 1.解压
            String unzipPath = depressAndGetBucketPath(datasetImportDTO);
            if (unzipPath == null) {
                return;
            }

            // 2.标注文件转换(voc、coco、createML --> TS),重命名
            String relativeAnnoDir = unzipPath.split(bucket + "/")[1] + "/annotations";
            String relativeImageDir = unzipPath.split(bucket + "/")[1] + "/images";

            // 2.1 格式验证（解压后立即验证）
            String absUnzipPath = fileUtil.getOriginFileAbsPath(unzipPath);
            java.io.File imageDir = new java.io.File(absUnzipPath, "images");
            java.io.File annoDir = new java.io.File(absUnzipPath, "annotations");
            try {
                validateCOCOFormat(imageDir, annoDir, unzipPath);
            } catch (Exception e) {
                log.error("COCO格式验证失败: {}", e.getMessage(), e);
                updateDatasetState(datasetImportDTO.getDatasetId(), "COCO格式验证失败: " + e.getMessage());
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                    new Date(), "数据集导入-格式验证失败: " + e.getMessage(),
                    DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                // 创建通知
                createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(),
                    "COCO格式验证失败: " + e.getMessage(), datasetImportDTO.getDatasetType());
                // 清理解压文件
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                }
                return;
            }

            // 读取json
            String relativeJsonFile = relativeAnnoDir + "/coco_info.json";
            String jsonStr = minioUtil.readString(bucket, relativeJsonFile);
            JSONObject annoObject = JSON.parseObject(jsonStr);
            JSONArray images = annoObject.getJSONArray("images");
            JSONArray annotations = annoObject.getJSONArray("annotations");
            JSONArray categories = annoObject.getJSONArray("categories");

            // 字典 key: json image_id
            Map<Long, JSONArray> annoMap = new HashMap<>();
            // 标签管理 json_id -> dubhe_label_id
            Map<Long, Long> categoryMap = new HashMap<>();

            // 3. 处理类别数据
            try {
                categories.forEach(category -> {
                    JSONObject categoryJson = (JSONObject) category;
                    String categoryName = categoryJson.getString("name");
                    if (!getLabelNameMap(datasetImportDTO).containsKey(categoryName)) {
                        Label label = Label.builder()
                                .name(categoryName)
                                .color(ConversionUtil.generateRandomColor())
                                .build();
                        insertLabelData(label, datasetImportDTO.getDatasetId());
                    }
                    categoryMap.put(categoryJson.getLong("id"), getLabelNameMap(datasetImportDTO).get(categoryName));
                });
            } catch (Exception e) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "处理类别数据失败: " + e.getMessage());
                return;
            }

            // 4. 整理标注信息
            try {
                annotations.forEach(annotation -> {
                    JSONObject annotationJson = (JSONObject) annotation;
                    Long imageId = annotationJson.getLong("image_id");
                    JSONObject result = initDubheAnno(categoryMap.get(annotationJson.getLong("category_id")));
                    result.put("bbox", annotationJson.getJSONArray("bbox"));
                    if (!annoMap.containsKey(imageId)) {
                        annoMap.put(imageId, new JSONArray() {{
                            add(result);
                        }});
                    } else {
                        annoMap.get(imageId).add(result);
                    }
                });
            } catch (Exception e) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "处理标注数据失败: " + e.getMessage());
                return;
            }

            // 5. 批量处理图片文件信息
            try {
                // 初始化任务进度
                Task task = Task.builder()
                        .total(images.size())
                        .datasetId(datasetImportDTO.getDatasetId())
                        .status(TaskStatusEnum.ING.getValue())
                        .finished(MagicNumConstant.ZERO)
                        .build();
                redisUtils.set("datasetImport" + datasetImportDTO.getDatasetId(), JSON.toJSONString(task), 60 * 60);

                // 将JSONArray转换为List便于分批处理
                List<JSONObject> imageList = new ArrayList<>();
                for (int i = 0; i < images.size(); i++) {
                    imageList.add(images.getJSONObject(i));
                }

                // 分批处理
                int batchSize = 100;
                int total = imageList.size();
                for (int i = 0; i < total; i += batchSize) {
                    int fromIndex = i;
                    int toIndex = Math.min(i + batchSize, total);
                    List<JSONObject> batchImages = imageList.subList(fromIndex, toIndex);

                    futures.add(executor.submit(() -> {
                        try {
                            handleCOCOImageBatch(datasetImportDTO.getDatasetId(), batchImages, relativeImageDir, annoMap);

                            // 进度统计
                            AtomicInteger count = taskCounters.get(datasetImportDTO.getDatasetId());
                            int processedCount = count.addAndGet(toIndex - fromIndex);

                            // 更新进度缓存
                            String key = "datasetImport" + datasetImportDTO.getDatasetId();
                            final Task currentTask = JSONObject.parseObject((String) redisUtils.get(key), Task.class);

                            if (currentTask != null) {
                                currentTask.setFinished(processedCount >= total ? currentTask.getTotal() : processedCount);
                                redisUtils.set(key, JSONObject.toJSONString(currentTask), 60 * 60);
                            }
                            updateZipTransferProgress(datasetImportDTO, processedCount, total, "COCO 图片处理中");

                        } catch (Throwable e) {
                            String errorMsg = "处理图片批次 [" + fromIndex + "-" + (toIndex - 1) + "] 时出错: " + e.getMessage();
                            errorMessages.add(errorMsg);
                            log.error(errorMsg, e);
                            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                    new Date(), "数据集导入-处理图片出错",
                                    DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                        }
                    }));
                }

                // 等待所有处理任务完成
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause();
                        String errorMsg = "线程执行失败: " + (cause != null ? cause.getMessage() : ee.getMessage());
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, ee);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                new Date(), "数据集导入-处理图片出错",
                                DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                    } catch (Exception e) {
                        String errorMsg = "等待过程中线程任务出错: " + e.getMessage();
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, e);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                new Date(), "数据集导入-处理图片出错",
                                DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                    }
                }

                executor.shutdown();

            } catch (Exception e) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "处理图片数据失败: " + e.getMessage());
                return;
            }

            // 6. 根据处理结果决定状态
            if (!errorMessages.isEmpty()) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "部分图片处理失败: 共 " + errorMessages.size() + " 个报错");
                // 删除文件夹
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                    deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
                }
                redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
                return;
            }

            // 删除文件夹
            if (unzipPath.startsWith(bucket + "/")) {
                String objectKey = unzipPath.substring(bucket.length() + 1);
                minioUtil.delFolder(bucket, objectKey);
                deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
            }

            // 执行状态转换导入中->标注中/标注完成
            Dataset dataset = Dataset.builder().id(datasetImportDTO.getDatasetId()).currentVersionName(datasetImportDTO.getCurrentVersionName()).build();
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataset});
                setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
                setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            }});

            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                    new Date(), "数据集导入-完成",
                    DatasetOperationEvent.EventType.INFO, DatasetOperationEvent.OperationType.DATA_IMPORT);
            createDatasetImportSuccessNotification(datasetImportDTO.getDatasetId(), datasetImportDTO.getDatasetType(), images != null ? images.size() : null);
            redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());

        } catch (Exception e) {
            updateDatasetState(datasetImportDTO.getDatasetId(), "处理过程中发生异常: " + e.getMessage());
            redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
            // 不想抛出直接return也行
            throw e;
        }
    }

    /**
     * COCO图片批量处理方法
     */
    public void handleCOCOImageBatch(
            Long datasetId,
            List<JSONObject> imageList,
            String relativeImageDir,
            Map<Long, JSONArray> annoMap
    ) throws IOException {
        List<FileCreateDTO> fileCreateDTOList = new ArrayList<>();
        List<JSONArray> resultArrayList = new ArrayList<>();

        for (JSONObject imageJson : imageList) {
            String imageName = imageJson.getString("file_name");
            String newName = getNewName(imageName);

            // 转移图片
            String sourceFile = relativeImageDir + "/" + imageName;
            String targetFile = fileUtil.getDatasetFilePath(datasetId, newName, true);

            try {
                minioUtil.copyObject(bucket, sourceFile, targetFile);
            } catch (Exception e) {
                throw new IOException("minioUtil.copyObject failed for file: " + imageName, e);
            }

            // 图片信息
            FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                    .width(imageJson.getInteger("width"))
                    .height(imageJson.getInteger("height"))
                    .url(fileUtil.getBucketNameFilePath(datasetId, newName, true))
                    .build();

            // 获取对应的标注信息
            JSONArray annotations = annoMap.get(imageJson.getLong("id"));
            if (annotations == null) {
                annotations = new JSONArray(); // 如果没有标注，创建空数组
            }

            // 收集批量上传信息
            fileCreateDTOList.add(fileCreateDTO);
            resultArrayList.add(annotations);
        }

        // 批量上传图片信息
        if (!fileCreateDTOList.isEmpty()) {
            try {
                uploadFile(datasetId, BatchFileCreateDTO.builder().files(fileCreateDTOList).build());
            } catch (Exception e) {
                throw new BusinessException("uploadFile failed: ", e);
            }

            try {
                // 批量上传标注信息
                uploadAnnotationBatch(fileCreateDTOList, datasetId, resultArrayList);
            } catch (Exception e) {
                throw new BusinessException("uploadAnnotationBatch failed: ", e);
            }
        }
    }

    /**
     * 处理YOLO格式
     */
    public void handleYOLO(DatasetImportDTO datasetImportDTO) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());
        try {
            // 1. 解压
            String unzipPath = depressAndGetBucketPath(datasetImportDTO);
            if (unzipPath == null) {
                // depressAndGetBucketPath 内部已处理状态更新
                return;
            }

            // 2. 处理解压后的文件
            String absUnzipPath = fileUtil.getOriginFileAbsPath(unzipPath);
            String absAnnoDir = absUnzipPath + "/annotations";
            String relativeImageDir = unzipPath.split(bucket + "/")[1] + "/images";
            java.io.File imageDir = new java.io.File(absUnzipPath, "images");
            java.io.File annoDir = new java.io.File(absAnnoDir);

            // 2.1 格式验证（解压后立即验证）
            try {
                validateYOLOFormat(imageDir, annoDir);
            } catch (Exception e) {
                log.error("YOLO格式验证失败: {}", e.getMessage(), e);
                updateDatasetState(datasetImportDTO.getDatasetId(), "YOLO格式验证失败: " + e.getMessage());
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                    new Date(), "数据集导入-格式验证失败: " + e.getMessage(),
                    DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                // 创建通知
                createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(),
                    "YOLO格式验证失败: " + e.getMessage(), datasetImportDTO.getDatasetType());
                // 清理解压文件
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                }
                return;
            }

            // 3. 处理标签文件
            String labelFile = absAnnoDir + "/classes.txt";
            Map<Long, Long> categoryMap = new HashMap<>();
            try {
                log.info("处理标签文件: {} 中", labelFile);
                AtomicInteger i = new AtomicInteger(0);
                List<String> lines = Files.readAllLines(Paths.get(labelFile));
                lines.forEach(line -> {
                    if (StringUtils.isNotBlank(line)) {
                        String categoryName = line.trim();
                        if (!getLabelNameMap(datasetImportDTO).containsKey(categoryName)) {
                            Label label = Label.builder()
                                    .name(categoryName)
                                    .color(ConversionUtil.generateRandomColor())
                                    .build();
                            insertLabelData(label, datasetImportDTO.getDatasetId());
                        }
                        categoryMap.put(i.longValue(), getLabelNameMap(datasetImportDTO).get(categoryName));
                        i.incrementAndGet();
                    }
                });
            } catch (IOException e) {datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),new Date(),"数据集导入-处理classes.txt出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_IMPORT);
                updateDatasetState(datasetImportDTO.getDatasetId(), "标签文件读取失败: " + e.getMessage());
                return;
            }

            // 确保这是目录
            if (imageDir.isDirectory()) {
                log.info("处理图片文件夹: {} 中", imageDir.getAbsolutePath());
                // 获取目录中的文件和子目录
                java.io.File[] files = imageDir.listFiles();
                List<java.io.File> fileList = Arrays.asList(files);

                Task originTask = Task.builder()
                        .total(files.length)
                        .datasetId(datasetImportDTO.getDatasetId())
                        .status(TaskStatusEnum.ING.getValue())
                        .finished(MagicNumConstant.ZERO)
                        .build();
                redisUtils.set("datasetImport" + datasetImportDTO.getDatasetId(), JSON.toJSONString(originTask), 60 * 60);
                // 遍历文件和子目录
                int batchSize = 100;
                int total = fileList.size();
                for (int i = 0; i < total; i += batchSize) {
                    int fromIndex = i;
                    int toIndex = Math.min(i + batchSize, total);
                    List<java.io.File> batchFiles = fileList.subList(fromIndex, toIndex);

                    futures.add(executor.submit(() -> {
                        try {
                            handleYOLOFiles(datasetImportDTO.getDatasetId(), batchFiles, absAnnoDir, relativeImageDir, categoryMap);
                            // 进度统计
                            AtomicInteger count = taskCounters.get(datasetImportDTO.getDatasetId());
                            int i2 = count.addAndGet(toIndex - fromIndex + 1);

                            // 进度缓存逻辑
                            String key = "datasetImport" + datasetImportDTO.getDatasetId();
                            final Task task = JSONObject.parseObject((String) redisUtils.get(key), Task.class);

                            if (task != null) {
                                task.setFinished(i2 >= total ? task.getTotal() : i2);
                                redisUtils.set(key, JSONObject.toJSONString(task), 60 * 60);
                            }
                            updateZipTransferProgress(datasetImportDTO, i2, total, "YOLO 文件处理中");

                        } catch (Throwable e) {
                            String errorMsg = "处理文件批次 [" + fromIndex + "-" + (toIndex - 1) + "] 时出错: " + e.getMessage();
                            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),new Date(),"数据集导入-处理文件出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_IMPORT);
                            errorMessages.add(errorMsg);
                            log.error(errorMsg, e);
                        }
                    }));
                }


                // 等待所有处理任务完成
                for (Future<?> future : futures) {
                    try {
                        future.get();

                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause();
                        String errorMsg = "线程执行失败: " + (cause != null ? cause.getMessage() : ee.getMessage());
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, ee);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),new Date(),"数据集导入-处理文件出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_IMPORT);
                    } catch (Exception e) {
                        String errorMsg = "等待过程中线程任务出错: " + e.getMessage();
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, e);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),new Date(),"数据集导入-处理文件出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_IMPORT);
                    }
                }


                executor.shutdown();
                String basePrefix = "dataset/" + datasetImportDTO.getDatasetId() + "/" +"unzip/";
                // 5. 根据处理结果决定状态
                if (!errorMessages.isEmpty()) {
                    updateDatasetState(datasetImportDTO.getDatasetId(), "部分文件处理失败: 共 " + errorMessages.size() + " 个报错");
                    // 删除文件夹
                    if (unzipPath.startsWith(bucket + "/")) {
                        String objectKey = unzipPath.substring(bucket.length() + 1);
                        minioUtil.delFolder(bucket, objectKey);
                        deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
                    }
                    redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
                    return;
                }
                // 删除文件夹
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                    deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());

                }
                // 执行状态转换导入中->标注中/标注完成
                Dataset dataset = Dataset.builder().id(datasetImportDTO.getDatasetId()).currentVersionName(datasetImportDTO.getCurrentVersionName()).build();
                StateMachineUtil.stateChange(new StateChangeDTO() {{
                    setObjectParam(new Object[]{dataset});
                    setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
                    setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
                }});
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),new Date(),"数据集导入-完成", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_IMPORT);
                createDatasetImportSuccessNotification(datasetImportDTO.getDatasetId(), datasetImportDTO.getDatasetType(), files != null ? files.length : null);
                redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
            } else {
                throw new Exception(imageDir + "不是路径");
            }

        } catch (Exception e) {
            updateDatasetState(datasetImportDTO.getDatasetId(), "处理过程中发生异常: " + e.getMessage());
            redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
            // 不想抛出直接return也行
            throw e;
        }
    }

    /**
     * 处理YOLO格式下的每张图片
     */
    public void handleYOLOFile(Long datasetId, java.io.File file, String annoDir, String relativeImageDir, Map<Long, Long> categoryMap) throws IOException {
        // xml to jsonObject
        String annoFile = StringUtils.substringBeforeLast(file.getName(), ".") + ".txt";
        List<JSONObject> txtToJsonObjectList = ConversionUtil.txtToJsonObjectList(annoDir + "/" + annoFile);
        if (txtToJsonObjectList == null) {
            log.warn("文件 {} 对应的标注文件 {} 不存在或为空，跳过处理。", file.getName(), annoFile);
            return;
        }

        int width = 0;
        int height = 0;
        ImageDimension imageDimension = readImageDimensions(file);
        width = imageDimension.getWidth();
        height = imageDimension.getHeight();

        // 图片文件信息
        String newName = getNewName(file.getName());
        FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                .width(width)
                .height(height)
                .url(fileUtil.getBucketNameFilePath(datasetId, newName, true))
                .build();

        // 构建默认标注文件
        JSONArray resultArray = new JSONArray();
        for (JSONObject jsonObject : txtToJsonObjectList) {
            JSONArray newBbox = jsonObject.getJSONArray("newBbox");
            Long txtId = jsonObject.getLong("labelId");
            Long dubhe_label_id = categoryMap.get(txtId);
            //categoryMap里有说明这个序号对应的labelname在目标数据集中存在，目标数据集没有没有则不导入这个标签
            if (dubhe_label_id != null) {
                JSONObject result = initDubheAnno(dubhe_label_id);
                double w = newBbox.getDouble(2) * width;
                double h = newBbox.getDouble(3) * height;
                double x = newBbox.getDouble(0) * width - 0.5 * w;
                double y = newBbox.getDouble(1) * height - 0.5 * h;
                result.put("bbox", Arrays.asList(x, y, w, h));
                resultArray.add(result);
            }
        }

        // 转移图片
        String sourceFile = relativeImageDir + "/" + file.getName();
        String targetFile = fileUtil.getDatasetFilePath(datasetId, newName, true);

        minioUtil.copyObject(bucket, sourceFile, targetFile);

        uploadFile(datasetId, BatchFileCreateDTO.builder()
                .files(Collections.singletonList(fileCreateDTO))
                .build());

        // 标注上传接口
        uploadAnnotationPerImage(fileCreateDTO, datasetId, resultArray);
    }


    public void handleYOLOFiles(
            Long datasetId,
            List<java.io.File> files,
            String annoDir,
            String relativeImageDir,
            Map<Long, Long> categoryMap
    ) throws IOException, InterruptedException {
        List<FileCreateDTO> fileCreateDTOList = new ArrayList<>();
        List<JSONArray> resultArrayList = new ArrayList<>();

        for (java.io.File file : files) {
            // 标注文件名
            String annoFile = StringUtils.substringBeforeLast(file.getName(), ".") + ".txt";
            List<JSONObject> txtToJsonObjectList = ConversionUtil.txtToJsonObjectList(annoDir + "/" + annoFile);
            
            // 获取图片宽高
            int width = 0;
            int height = 0;
            try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(stream);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                    reader.dispose();
                }
            }

            // 文件新名字及DTO
            String newName = getNewName(file.getName());
            FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                    .width(width)
                    .height(height)
                    .url(fileUtil.getBucketNameFilePath(datasetId, newName, true))
                    .build();

            // 构建默认标注文件（如果没有标注文件，则为空数组，图片状态仍为已标注）
            JSONArray resultArray = new JSONArray();
            if (txtToJsonObjectList != null) {
                for (JSONObject jsonObject : txtToJsonObjectList) {
                    JSONArray newBbox = jsonObject.getJSONArray("newBbox");
                    Long txtId = jsonObject.getLong("labelId");
                    Long dubhe_label_id = categoryMap.get(txtId);
                    if (dubhe_label_id != null) {
                        JSONObject result = initDubheAnno(dubhe_label_id);
                        Double w = newBbox.getDouble(2) * width;
                        Double h = newBbox.getDouble(3) * height;
                        Double x = newBbox.getDouble(0) * width - 0.5 * w;
                        Double y = newBbox.getDouble(1) * height - 0.5 * h;
                        result.put("bbox", Arrays.asList(x, y, w, h));
                        resultArray.add(result);
                    }
                }
            }

            // 拷贝图片到目标存储
            String sourceFile = relativeImageDir + "/" + file.getName();
            String targetFile = fileUtil.getDatasetFilePath(datasetId, newName, true);
            try {
                minioUtil.copyObject(bucket, sourceFile, targetFile);
            } catch (Exception e) {

                throw new IOException("minioUtil.copyObject failed for file: " + file.getName(), e);
            }

            // 收集上传信息
            fileCreateDTOList.add(fileCreateDTO);
            resultArrayList.add(resultArray);
        }

        // 批量上传图片信息
        if (!fileCreateDTOList.isEmpty()) {

            try {
                uploadFile(datasetId, BatchFileCreateDTO.builder().files(fileCreateDTOList).build());
            } catch (Exception e) {
                throw new BusinessException("uploadFile failed: ", e);
            }

            try {
                // 批量上传标注信息（不含进度统计）
                uploadAnnotationBatch(fileCreateDTOList, datasetId, resultArrayList);
            } catch (Exception e) {
                throw new BusinessException("uploadAnnotationBatch failed: ", e);
            }
        }
    }


    /**
     * 处理YOLO格式的语义分割数据导入
     * 文件结构:
     * - images/          图片文件
     * - annotations/     标注文件(.txt格式,YOLO语义分割格式)
     * - classes.txt      标签列表
     * 
     * .txt文件格式: <class_id> <x1> <y1> <x2> <y2> ... <xn> <yn>
     * 坐标已归一化到 [0,1] 范围
     */
    public void handleSegmentYOLO(DatasetImportDTO datasetImportDTO) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());
        try {
            // 1. 解压
            String unzipPath = depressAndGetBucketPath(datasetImportDTO);
            if (unzipPath == null) {
                return;
            }

            // 2. 处理解压后的文件
            String absUnzipPath = fileUtil.getOriginFileAbsPath(unzipPath);
            String absAnnoDir = absUnzipPath + "/annotations";
            String relativeImageDir = unzipPath.split(bucket + "/")[1] + "/images";
            java.io.File imageDir = new java.io.File(absUnzipPath, "images");
            java.io.File annoDir = new java.io.File(absAnnoDir);
            
            // 2.1 格式验证（解压后立即验证）
            try {
                validateSegmentYOLOFormat(imageDir, annoDir);
            } catch (Exception e) {
                log.error("Segment-YOLO格式验证失败: {}", e.getMessage(), e);
                updateDatasetState(datasetImportDTO.getDatasetId(), "Segment-YOLO格式验证失败: " + e.getMessage());
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), 
                    new Date(), "数据集导入-格式验证失败: " + e.getMessage(), 
                    DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                // 创建通知
                createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(), 
                    "Segment-YOLO格式验证失败: " + e.getMessage(), datasetImportDTO.getDatasetType());
                // 清理解压文件
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                }
                return;
            }

            // 3. 处理标签文件 classes.txt
            String labelFile = absAnnoDir + "/classes.txt";
            Map<Long, Long> categoryMap = new HashMap<>();
            try {
                log.info("处理标签文件: {} 中", labelFile);
                AtomicInteger i = new AtomicInteger(0);
                List<String> lines = Files.readAllLines(Paths.get(labelFile));
                lines.forEach(line -> {
                    if (StringUtils.isNotBlank(line)) {
                        String categoryName = line.trim();
                        if (!getLabelNameMap(datasetImportDTO).containsKey(categoryName)) {
                            Label label = Label.builder()
                                    .name(categoryName)
                                    .color(ConversionUtil.generateRandomColor())
                                    .build();
                            insertLabelData(label, datasetImportDTO.getDatasetId());
                        }
                        categoryMap.put(i.longValue(), getLabelNameMap(datasetImportDTO).get(categoryName));
                        i.incrementAndGet();
                    }
                });
            } catch (IOException e) {
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), 
                    "数据集导入Segment-YOLO-处理classes.txt出错", DatasetOperationEvent.EventType.ERROR, 
                    DatasetOperationEvent.OperationType.DATA_IMPORT);
                updateDatasetState(datasetImportDTO.getDatasetId(), "标签文件读取失败: " + e.getMessage());
                return;
            }

            // 确保这是目录
            if (imageDir.isDirectory()) {
                log.info("处理图片文件夹: {} 中", imageDir.getAbsolutePath());
                java.io.File[] files = imageDir.listFiles();
                List<java.io.File> fileList = Arrays.asList(files);

                Task originTask = Task.builder()
                        .total(files.length)
                        .datasetId(datasetImportDTO.getDatasetId())
                        .status(TaskStatusEnum.ING.getValue())
                        .finished(MagicNumConstant.ZERO)
                        .build();
                redisUtils.set("datasetImport" + datasetImportDTO.getDatasetId(), JSON.toJSONString(originTask), 60 * 60);
                
                // 批量处理文件
                int batchSize = 100;
                int total = fileList.size();
                for (int i = 0; i < total; i += batchSize) {
                    int fromIndex = i;
                    int toIndex = Math.min(i + batchSize, total);
                    List<java.io.File> batchFiles = fileList.subList(fromIndex, toIndex);

                    futures.add(executor.submit(() -> {
                        try {
                            handleSegmentYOLOFiles(datasetImportDTO.getDatasetId(), batchFiles, absAnnoDir, 
                                relativeImageDir, categoryMap);
                            
                            // 进度统计
                            AtomicInteger count = taskCounters.get(datasetImportDTO.getDatasetId());
                            int i2 = count.addAndGet(toIndex - fromIndex + 1);

                            // 进度缓存逻辑
                            String key = "datasetImport" + datasetImportDTO.getDatasetId();
                            final Task task = JSONObject.parseObject((String) redisUtils.get(key), Task.class);

                            if (task != null) {
                                task.setFinished(i2 >= total ? task.getTotal() : i2);
                                redisUtils.set(key, JSONObject.toJSONString(task), 60 * 60);
                            }
                            updateZipTransferProgress(datasetImportDTO, i2, total, "Segment-YOLO 文件处理中");

                        } catch (Throwable e) {
                            String errorMsg = "处理Segment-YOLO文件批次 [" + fromIndex + "-" + (toIndex - 1) + "] 时出错: " + e.getMessage();
                            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), 
                                "数据集导入Segment-YOLO-处理文件出错", DatasetOperationEvent.EventType.ERROR, 
                                DatasetOperationEvent.OperationType.DATA_IMPORT);
                            errorMessages.add(errorMsg);
                            log.error(errorMsg, e);
                        }
                    }));
                }

                // 等待所有处理任务完成
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause();
                        String errorMsg = "线程执行失败: " + (cause != null ? cause.getMessage() : ee.getMessage());
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, ee);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), 
                            "数据集导入Segment-YOLO-处理文件出错", DatasetOperationEvent.EventType.ERROR, 
                            DatasetOperationEvent.OperationType.DATA_IMPORT);
                    } catch (Exception e) {
                        String errorMsg = "等待过程中线程任务出错: " + e.getMessage();
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, e);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), 
                            "数据集导入Segment-YOLO-处理文件出错", DatasetOperationEvent.EventType.ERROR, 
                            DatasetOperationEvent.OperationType.DATA_IMPORT);
                    }
                }

                executor.shutdown();

                // 根据处理结果决定状态
                if (!errorMessages.isEmpty()) {
                    updateDatasetState(datasetImportDTO.getDatasetId(), "部分文件处理失败: 共 " + errorMessages.size() + " 个报错");
                    if (unzipPath.startsWith(bucket + "/")) {
                        String objectKey = unzipPath.substring(bucket.length() + 1);
                        minioUtil.delFolder(bucket, objectKey);
                        deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
                    }
                    redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
                    return;
                }
                
                // 删除文件夹
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                    deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
                }
                
                // 执行状态转换导入中->标注中/标注完成
                Dataset dataset = Dataset.builder().id(datasetImportDTO.getDatasetId())
                    .currentVersionName(datasetImportDTO.getCurrentVersionName()).build();
                StateMachineUtil.stateChange(new StateChangeDTO() {{
                    setObjectParam(new Object[]{dataset});
                    setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
                    setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
                }});
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), new Date(), 
                    "数据集导入Segment-YOLO-完成", DatasetOperationEvent.EventType.INFO, 
                    DatasetOperationEvent.OperationType.DATA_IMPORT);
                createDatasetImportSuccessNotification(datasetImportDTO.getDatasetId(), datasetImportDTO.getDatasetType(), files != null ? files.length : null);
                redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
            } else {
                throw new Exception(imageDir + "不是路径");
            }

        } catch (Exception e) {
            updateDatasetState(datasetImportDTO.getDatasetId(), "处理Segment-YOLO过程中发生异常: " + e.getMessage());
            redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
            throw e;
        }
    }

    /**
     * 统一segmentation格式
     * 支持两种输入格式：
     * 1. COCO标准格式：[[x1, y1, x2, y2, x3, y3, ...]] - 扁平坐标数组
     * 2. 点对象数组格式：[[[x1, y1], [x2, y2], [x3, y3], ...]] - 嵌套点数组
     * 
     * 统一输出为前端期望的格式：[[x1, y1], [x2, y2], [x3, y3], ...]
     */
    private JSONArray normalizeSegmentation(JSONArray segmentation) {
        if (segmentation == null || segmentation.isEmpty()) {
            return new JSONArray();
        }

        // 获取第一个多边形（通常COCO格式的segmentation是多边形数组）
        Object firstPolygon = segmentation.get(0);
        
        if (!(firstPolygon instanceof JSONArray)) {
            return segmentation;
        }
        
        JSONArray polygon = (JSONArray) firstPolygon;
        if (polygon.isEmpty()) {
            return segmentation;
        }

        // 检查格式：查看第一个元素
        Object firstElement = polygon.get(0);
        
        // 情况1：如果第一个元素是数组 [[x, y], ...] 格式，已经是点数组格式
        if (firstElement instanceof JSONArray) {
            // 直接返回，已经是正确格式
            return polygon;
        }
        
        // 情况2：如果第一个元素是数字 [x1, y1, x2, y2, ...] 格式，需要转换为点数组
        if (firstElement instanceof Number) {
            JSONArray result = new JSONArray();
            // 每两个数字组成一个点
            for (int i = 0; i < polygon.size(); i += 2) {
                if (i + 1 < polygon.size()) {
                    JSONArray point = new JSONArray();
                    point.add(polygon.get(i));
                    point.add(polygon.get(i + 1));
                    result.add(point);
                }
            }
            return result;
        }
        
        // 其他情况，返回原始数据
        return segmentation;
    }

    /**
     * 处理COCO格式的语义分割数据导入
     * 文件结构:
     * - images/          图片文件
     * - annotations/     一个coco_info.json文件
     * 
     * JSON结构: {"images": [...], "annotations": [...], "categories": [...]}
     * annotations中包含segmentation字段而不是bbox字段
     */
    public void handleSegmentCOCO(DatasetImportDTO datasetImportDTO) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        List<String> errorMessages = Collections.synchronizedList(new ArrayList<>());

        try {
            // 1. 解压
            String unzipPath = depressAndGetBucketPath(datasetImportDTO);
            if (unzipPath == null) {
                return;
            }

            // 2. 标注文件转换
            String relativeAnnoDir = unzipPath.split(bucket + "/")[1] + "/annotations";
            String relativeImageDir = unzipPath.split(bucket + "/")[1] + "/images";
            
            // 2.1 格式验证（解压后立即验证）
            String absUnzipPath = fileUtil.getOriginFileAbsPath(unzipPath);
            java.io.File imageDir = new java.io.File(absUnzipPath, "images");
            java.io.File annoDir = new java.io.File(absUnzipPath, "annotations");
            try {
                validateSegmentCOCOFormat(imageDir, annoDir, unzipPath);
            } catch (Exception e) {
                log.error("Segment-COCO格式验证失败: {}", e.getMessage(), e);
                updateDatasetState(datasetImportDTO.getDatasetId(), "Segment-COCO格式验证失败: " + e.getMessage());
                datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(), 
                    new Date(), "数据集导入-格式验证失败: " + e.getMessage(), 
                    DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                // 创建通知
                createDatasetImportFailedNotification(datasetImportDTO.getDatasetId(), 
                    "Segment-COCO格式验证失败: " + e.getMessage(), datasetImportDTO.getDatasetType());
                // 清理解压文件
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                }
                return;
            }

            // 读取coco_info.json
            String relativeJsonFile = relativeAnnoDir + "/coco_info.json";
            String jsonStr = minioUtil.readString(bucket, relativeJsonFile);
            JSONObject annoObject = JSON.parseObject(jsonStr);
            JSONArray images = annoObject.getJSONArray("images");
            JSONArray annotations = annoObject.getJSONArray("annotations");
            JSONArray categories = annoObject.getJSONArray("categories");

            // 字典 key: json image_id
            Map<Long, JSONArray> annoMap = new HashMap<>();
            // 标签管理 json_id -> dubhe_label_id
            Map<Long, Long> categoryMap = new HashMap<>();

            // 3. 处理类别数据
            try {
                categories.forEach(category -> {
                    JSONObject categoryJson = (JSONObject) category;
                    String categoryName = categoryJson.getString("name");
                    if (!getLabelNameMap(datasetImportDTO).containsKey(categoryName)) {
                        Label label = Label.builder()
                                .name(categoryName)
                                .color(ConversionUtil.generateRandomColor())
                                .build();
                        insertLabelData(label, datasetImportDTO.getDatasetId());
                    }
                    categoryMap.put(categoryJson.getLong("id"), getLabelNameMap(datasetImportDTO).get(categoryName));
                });
            } catch (Exception e) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "处理Segment-COCO类别数据失败: " + e.getMessage());
                return;
            }

            // 4. 整理标注信息（segmentation字段）
            try {
                annotations.forEach(annotation -> {
                    JSONObject annotationJson = (JSONObject) annotation;
                    Long imageId = annotationJson.getLong("image_id");
                    JSONObject result = initDubheAnno(categoryMap.get(annotationJson.getLong("category_id")));
                    
                    // 获取segmentation并统一格式
                    JSONArray segmentation = annotationJson.getJSONArray("segmentation");
                    JSONArray normalizedSegmentation = normalizeSegmentation(segmentation);
                    
                    result.put("segmentation", normalizedSegmentation);
                    if (!annoMap.containsKey(imageId)) {
                        annoMap.put(imageId, new JSONArray() {{
                            add(result);
                        }});
                    } else {
                        annoMap.get(imageId).add(result);
                    }
                });
            } catch (Exception e) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "处理Segment-COCO标注数据失败: " + e.getMessage());
                return;
            }

            // 5. 批量处理图片文件信息
            try {
                // 初始化任务进度
                Task task = Task.builder()
                        .total(images.size())
                        .datasetId(datasetImportDTO.getDatasetId())
                        .status(TaskStatusEnum.ING.getValue())
                        .finished(MagicNumConstant.ZERO)
                        .build();
                redisUtils.set("datasetImport" + datasetImportDTO.getDatasetId(), JSON.toJSONString(task), 60 * 60);

                // 将JSONArray转换为List便于分批处理
                List<JSONObject> imageList = new ArrayList<>();
                for (int i = 0; i < images.size(); i++) {
                    imageList.add(images.getJSONObject(i));
                }

                // 分批处理
                int batchSize = 100;
                int total = imageList.size();
                for (int i = 0; i < total; i += batchSize) {
                    int fromIndex = i;
                    int toIndex = Math.min(i + batchSize, total);
                    List<JSONObject> batchImages = imageList.subList(fromIndex, toIndex);

                    futures.add(executor.submit(() -> {
                        try {
                            handleSegmentCOCOImageBatch(datasetImportDTO.getDatasetId(), batchImages, relativeImageDir, annoMap);

                            // 进度统计
                            AtomicInteger count = taskCounters.get(datasetImportDTO.getDatasetId());
                            int processedCount = count.addAndGet(toIndex - fromIndex);

                            // 更新进度缓存
                            String key = "datasetImport" + datasetImportDTO.getDatasetId();
                            final Task currentTask = JSONObject.parseObject((String) redisUtils.get(key), Task.class);

                            if (currentTask != null) {
                                currentTask.setFinished(processedCount >= total ? currentTask.getTotal() : processedCount);
                                redisUtils.set(key, JSONObject.toJSONString(currentTask), 60 * 60);
                            }
                            updateZipTransferProgress(datasetImportDTO, processedCount, total, "Segment-COCO 图片处理中");

                        } catch (Throwable e) {
                            String errorMsg = "处理Segment-COCO图片批次 [" + fromIndex + "-" + (toIndex - 1) + "] 时出错: " + e.getMessage();
                            errorMessages.add(errorMsg);
                            log.error(errorMsg, e);
                            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                    new Date(), "数据集导入Segment-COCO-处理图片出错",
                                    DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                        }
                    }));
                }

                // 等待所有处理任务完成
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException ee) {
                        Throwable cause = ee.getCause();
                        String errorMsg = "线程执行失败: " + (cause != null ? cause.getMessage() : ee.getMessage());
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, ee);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                new Date(), "数据集导入Segment-COCO-处理图片出错",
                                DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                    } catch (Exception e) {
                        String errorMsg = "等待过程中线程任务出错: " + e.getMessage();
                        errorMessages.add(errorMsg);
                        log.error(errorMsg, e);
                        datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                                new Date(), "数据集导入Segment-COCO-处理图片出错",
                                DatasetOperationEvent.EventType.ERROR, DatasetOperationEvent.OperationType.DATA_IMPORT);
                    }
                }

                executor.shutdown();

            } catch (Exception e) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "处理Segment-COCO图片数据失败: " + e.getMessage());
                return;
            }

            // 6. 根据处理结果决定状态
            if (!errorMessages.isEmpty()) {
                updateDatasetState(datasetImportDTO.getDatasetId(), "部分图片处理失败: 共 " + errorMessages.size() + " 个报错");
                // 删除文件夹
                if (unzipPath.startsWith(bucket + "/")) {
                    String objectKey = unzipPath.substring(bucket.length() + 1);
                    minioUtil.delFolder(bucket, objectKey);
                        deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
                }
                redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
                return;
            }

            // 删除文件夹
            if (unzipPath.startsWith(bucket + "/")) {
                String objectKey = unzipPath.substring(bucket.length() + 1);
                minioUtil.delFolder(bucket, objectKey);
                    deleteZipArchiveObject(datasetImportDTO.getArchiveUrl());
            }

            // 执行状态转换导入中->标注中/标注完成
            Dataset dataset = Dataset.builder().id(datasetImportDTO.getDatasetId())
                .currentVersionName(datasetImportDTO.getCurrentVersionName()).build();
            StateMachineUtil.stateChange(new StateChangeDTO() {{
                setObjectParam(new Object[]{dataset});
                setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
                setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            }});

            datasetOperationEventService.createAndInsertEvent(datasetImportDTO.getDatasetId(),
                    new Date(), "数据集导入Segment-COCO-完成",
                    DatasetOperationEvent.EventType.INFO, DatasetOperationEvent.OperationType.DATA_IMPORT);
            createDatasetImportSuccessNotification(datasetImportDTO.getDatasetId(), datasetImportDTO.getDatasetType(), images != null ? images.size() : null);
            redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());

        } catch (Exception e) {
            updateDatasetState(datasetImportDTO.getDatasetId(), "处理Segment-COCO过程中发生异常: " + e.getMessage());
            redisUtils.del("datasetImport" + datasetImportDTO.getDatasetId());
            throw e;
        }
    }

    /**
     * Segment-COCO图片批量处理方法
     * 与handleCOCOImageBatch类似，但处理的是segmentation字段
     * 
     * @param datasetId       数据集ID
     * @param imageList       图片信息列表（来自coco_info.json的images数组）
     * @param relativeImageDir 图片相对目录
     * @param annoMap         标注映射（image_id -> 标注数组，已包含segmentation）
     */
    public void handleSegmentCOCOImageBatch(
            Long datasetId,
            List<JSONObject> imageList,
            String relativeImageDir,
            Map<Long, JSONArray> annoMap
    ) throws IOException {
        List<FileCreateDTO> fileCreateDTOList = new ArrayList<>();
        List<JSONArray> resultArrayList = new ArrayList<>();

        for (JSONObject imageJson : imageList) {
            String imageName = imageJson.getString("file_name");
            String newName = getNewName(imageName);

            // 转移图片
            String sourceFile = relativeImageDir + "/" + imageName;
            String targetFile = fileUtil.getDatasetFilePath(datasetId, newName, true);

            try {
                minioUtil.copyObject(bucket, sourceFile, targetFile);
            } catch (Exception e) {
                throw new IOException("minioUtil.copyObject failed for file: " + imageName, e);
            }

            // 图片信息
            FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                    .width(imageJson.getInteger("width"))
                    .height(imageJson.getInteger("height"))
                    .url(fileUtil.getBucketNameFilePath(datasetId, newName, true))
                    .build();

            // 获取对应的标注信息（包含segmentation字段）
            JSONArray annotations = annoMap.get(imageJson.getLong("id"));
            if (annotations == null) {
                annotations = new JSONArray(); // 如果没有标注，创建空数组
            }

            // 收集批量上传信息
            fileCreateDTOList.add(fileCreateDTO);
            resultArrayList.add(annotations);
        }

        // 批量上传图片信息
        if (!fileCreateDTOList.isEmpty()) {
            try {
                uploadFile(datasetId, BatchFileCreateDTO.builder().files(fileCreateDTOList).build());
            } catch (Exception e) {
                throw new BusinessException("uploadFile failed: ", e);
            }

            try {
                // 批量上传标注信息
                uploadAnnotationBatch(fileCreateDTOList, datasetId, resultArrayList);
            } catch (Exception e) {
                throw new BusinessException("uploadAnnotationBatch failed: ", e);
            }
        }
    }

    /**
     * 批量处理YOLO格式语义分割文件
     * 
     * @param datasetId       数据集ID
     * @param files           文件列表
     * @param annoDir         标注文件目录（绝对路径）
     * @param relativeImageDir 图片相对目录
     * @param categoryMap     类别映射（YOLO class_id -> Dubhe label_id）
     */
    public void handleSegmentYOLOFiles(
            Long datasetId,
            List<java.io.File> files,
            String annoDir,
            String relativeImageDir,
            Map<Long, Long> categoryMap
    ) throws IOException, InterruptedException {
        List<FileCreateDTO> fileCreateDTOList = new ArrayList<>();
        List<JSONArray> resultArrayList = new ArrayList<>();

        for (java.io.File file : files) {
            // 标注文件名（.txt格式）
            String annoFile = StringUtils.substringBeforeLast(file.getName(), ".") + ".txt";
            String annoFilePath = annoDir + "/" + annoFile;
            
            // 读取YOLO格式的语义分割标注文件（如果存在）
            // 格式: <class_id> <x1> <y1> <x2> <y2> ... <xn> <yn>
            List<String> lines = new ArrayList<>();
            java.io.File txtFile = new java.io.File(annoFilePath);
            if (txtFile.exists()) {
                try {
                    lines = Files.readAllLines(Paths.get(annoFilePath));
                } catch (IOException e) {
                    log.error("读取Segment-YOLO标注文件 {} 失败: {}", annoFile, e.getMessage());
                    // 继续处理，使用空标注
                }
            }
            
            // 获取图片宽高
            int width = 0;
            int height = 0;
            try (ImageInputStream stream = ImageIO.createImageInputStream(file)) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(stream);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                    reader.dispose();
                }
            }

            // 文件新名字及DTO
            String newName = getNewName(file.getName());
            FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                    .width(width)
                    .height(height)
                    .url(fileUtil.getBucketNameFilePath(datasetId, newName, true))
                    .build();

            // 构建标注结果（如果没有标注文件，则为空数组，图片状态仍为已标注）
            JSONArray resultArray = new JSONArray();
            for (String line : lines) {
                if (StringUtils.isBlank(line)) {
                    continue;
                }
                
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 7) { // 至少需要 class_id + 3个点（6个坐标值）
                    log.warn("Segment-YOLO标注行格式错误，坐标点数不足: {}", line);
                    continue;
                }
                
                // 第一个是类别ID
                Long classId = Long.parseLong(parts[0]);
                Long dubheLabelId = categoryMap.get(classId);
                
                if (dubheLabelId == null) {
                    // 目标数据集没有这个标签，跳过
                    continue;
                }
                
                // 后续是归一化的坐标点 x1 y1 x2 y2 ... xn yn
                JSONArray segmentation = new JSONArray();
                for (int i = 1; i < parts.length; i += 2) {
                    if (i + 1 < parts.length) {
                        double normalizedX = Double.parseDouble(parts[i]);
                        double normalizedY = Double.parseDouble(parts[i + 1]);
                        
                        // 转换为像素坐标
                        double x = normalizedX * width;
                        double y = normalizedY * height;
                        
                        JSONArray point = new JSONArray();
                        point.add(x);
                        point.add(y);
                        segmentation.add(point);
                    }
                }
                
                // 创建Dubhe格式的标注对象
                JSONObject result = initDubheAnno(dubheLabelId);
                result.put("segmentation", segmentation);
                resultArray.add(result);
            }

            // 拷贝图片到目标存储
            String sourceFile = relativeImageDir + "/" + file.getName();
            String targetFile = fileUtil.getDatasetFilePath(datasetId, newName, true);
            try {
                minioUtil.copyObject(bucket, sourceFile, targetFile);
            } catch (Exception e) {
                throw new IOException("复制Segment-YOLO图片失败: " + file.getName(), e);
            }

            // 收集上传信息
            fileCreateDTOList.add(fileCreateDTO);
            resultArrayList.add(resultArray);
        }

        // 批量上传图片信息
        if (!fileCreateDTOList.isEmpty()) {
            try {
                uploadFile(datasetId, BatchFileCreateDTO.builder().files(fileCreateDTOList).build());
            } catch (Exception e) {
                throw new BusinessException("批量上传Segment-YOLO图片信息失败: ", e);
            }

            try {
                // 批量上传标注信息
                uploadAnnotationBatch(fileCreateDTOList, datasetId, resultArrayList);
            } catch (Exception e) {
                throw new BusinessException("批量上传Segment-YOLO标注信息失败: ", e);
            }
        }
    }

    /**
     * 处理单个YOLO格式语义分割文件（备用方法）
     * 
     * @param datasetId       数据集ID
     * @param file            图片文件
     * @param annoDir         标注文件目录（绝对路径）
     * @param relativeImageDir 图片相对目录
     * @param categoryMap     类别映射
     */
    public void handleSegmentYOLOFile(Long datasetId, java.io.File file, String annoDir, 
                                     String relativeImageDir, Map<Long, Long> categoryMap) throws IOException {
        // 标注文件名（.txt格式）
        String annoFile = StringUtils.substringBeforeLast(file.getName(), ".") + ".txt";
        String annoFilePath = annoDir + "/" + annoFile;
        
        // 检查标注文件是否存在
        java.io.File txtFile = new java.io.File(annoFilePath);
        if (!txtFile.exists()) {
            log.warn("Segment-YOLO标注文件 {} 不存在，跳过处理。", annoFile);
            return;
        }
        
        // 读取YOLO格式的语义分割标注文件
        List<String> lines = Files.readAllLines(Paths.get(annoFilePath));
        if (lines.isEmpty()) {
            log.warn("Segment-YOLO标注文件 {} 为空，跳过处理。", annoFile);
            return;
        }
        
        // 获取图片宽高
        ImageDimension imageDimension = readImageDimensions(file);
        int width = imageDimension.getWidth();
        int height = imageDimension.getHeight();

        // 文件新名字及DTO
        String newName = getNewName(file.getName());
        FileCreateDTO fileCreateDTO = FileCreateDTO.builder()
                .width(width)
                .height(height)
                .url(fileUtil.getBucketNameFilePath(datasetId, newName, true))
                .build();

        // 构建标注结果
        JSONArray resultArray = new JSONArray();
        for (String line : lines) {
            if (StringUtils.isBlank(line)) {
                continue;
            }
            
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 7) {
                log.warn("Segment-YOLO标注行格式错误，坐标点数不足: {}", line);
                continue;
            }
            
            // 第一个是类别ID
            Long classId = Long.parseLong(parts[0]);
            Long dubheLabelId = categoryMap.get(classId);
            
            if (dubheLabelId == null) {
                continue;
            }
            
            // 后续是归一化的坐标点
            JSONArray segmentation = new JSONArray();
            for (int i = 1; i < parts.length; i += 2) {
                if (i + 1 < parts.length) {
                    double normalizedX = Double.parseDouble(parts[i]);
                    double normalizedY = Double.parseDouble(parts[i + 1]);
                    
                    // 转换为像素坐标
                    double x = normalizedX * width;
                    double y = normalizedY * height;
                    
                    JSONArray point = new JSONArray();
                    point.add(x);
                    point.add(y);
                    segmentation.add(point);
                }
            }
            
            // 创建Dubhe格式的标注对象
            JSONObject result = initDubheAnno(dubheLabelId);
            result.put("segmentation", segmentation);
            resultArray.add(result);
        }

        // 转移图片
        String sourceFile = relativeImageDir + "/" + file.getName();
        String targetFile = fileUtil.getDatasetFilePath(datasetId, newName, true);
        minioUtil.copyObject(bucket, sourceFile, targetFile);

        uploadFile(datasetId, BatchFileCreateDTO.builder()
                .files(Collections.singletonList(fileCreateDTO))
                .build());

        // 标注上传接口
        uploadAnnotationPerImage(fileCreateDTO, datasetId, resultArray);
    }

    @Override
    public DatasetCardInfoVO getGuidedDatasetCardInfo(Long datasetId){
        DatasetVO datasetVO = this.get(datasetId);
        DatasetCardInfoVO datasetCardInfoVO = new DatasetCardInfoVO();
        datasetCardInfoVO.setId(datasetVO.getId());
        datasetCardInfoVO.setName(datasetVO.getName());
        datasetCardInfoVO.setAnnotateType(datasetVO.getAnnotateType());
        datasetCardInfoVO.setCurrentVersionName(datasetCardInfoVO.getCurrentVersionName());

        Map<Long, IsImportVO> importStatusMap = this.determineIfTheDatasetIsAnImport(Collections.singletonList(datasetId));
        IsImportVO statusVO = importStatusMap.get(datasetId);

        datasetCardInfoVO.setStatus(statusVO.getStatus());
        datasetCardInfoVO.setProgress(statusVO.getProgress());
        datasetCardInfoVO.setRemainTime(statusVO.getRemainTime());

        //获取图片和视频的数量
        VideoStatisticsDTO videoStatistics = fileService.getVideoStatistics(datasetId);
        //获取所有文件的标注状态
        FileScreenStatSearchDTO fileScreenStatSearchDTO = new FileScreenStatSearchDTO();
        fileScreenStatSearchDTO.setAnnotationResult(FileTypeEnum.HAVE_ANNOTATION.getValue());
        FileScreenStatVO fileCountByStatus = fileService.getFileCountByStatus(datasetId, fileScreenStatSearchDTO);

        Long totalImages = fileCountByStatus.getHaveAnnotation() + fileCountByStatus.getNoAnnotation();
        Long unlabeledImages = fileCountByStatus.getNoAnnotation();
        Long labeledImages = fileCountByStatus.getHaveAnnotation();

        datasetCardInfoVO.setTotalImageCount(totalImages);
        datasetCardInfoVO.setLabeledImageCount(labeledImages);
        datasetCardInfoVO.setUnLabeledImageCount(unlabeledImages);
        datasetCardInfoVO.setTotalVideoCount(videoStatistics.getTotalVideos());
        datasetCardInfoVO.setExtractedVideoCount(videoStatistics.getExtractedVideos());
        datasetCardInfoVO.setUnExtractedVideoCount(videoStatistics.getUnExtractedVideos());

        datasetCardInfoVO.setModule(datasetVO.getModule());

        return datasetCardInfoVO;
    }

    @Override
    public List<DatasetVersionVO> getAvailableVersionList(Long selectedDatasetId, Long targetDatasetId) {
        if (targetDatasetId == null || selectedDatasetId == null) {
            System.err.println("getAvailableVersionList datasetId is null");
            return Collections.emptyList();
        }
        // 获取选中的数据集对应的versionList
        List<DatasetVersionVO> datasetVersionList = datasetVersionServiceImpl.getDatasetVersionList(selectedDatasetId);
        // 只考虑已发布的
        datasetVersionList = datasetVersionList.stream()
                .filter(version -> version.getIsPublic() == 1)
                .collect(Collectors.toList());
        // 引导式自己的数据集的Labels
        List<LabelDTO> list = labelService.list(targetDatasetId);

        // 提取目标标签名称集合，用于快速匹配
        Set<String> targetLabelNames = list.stream()
                .map(LabelDTO::getName)
                .collect(Collectors.toSet());

        List<DatasetVersionVO> filteredVersions = new ArrayList<>();

        for (DatasetVersionVO version : datasetVersionList) {
            // 获取当前版本关联的标签映射
            List<LabelMapping> mappings = labelMappingServiceImpl.findByDatasetVersionId(version.getId());

            // 提取关联标签ID并查询标签详情
            List<Long> labelIds = mappings.stream()
                    .map(LabelMapping::getTargetLabelId)
                    .filter(Objects::nonNull) // 过滤 null 值
                    .collect(Collectors.toList());
            List<Label> versionLabels = labelService.findLabelByIds(labelIds);

            // 检查是否存在名称匹配的标签
            boolean hasMatch = versionLabels.stream()
                    .map(Label::getName)
                    .anyMatch(targetLabelNames::contains);

            if (hasMatch) {
                filteredVersions.add(version);
            }
        }

        return filteredVersions;
    }


    @Override
    public Boolean isPublishingByDatasetId(Long datasetId) {
        if (datasetId ==null) {
            return null;
        }

        Dataset dataset =  getBaseMapper().selectById(datasetId);
        Integer dataConversion = null;
            // 如果数据集已发布，加入数据集发布信息
            if (dataset.getCurrentVersionName() != null) {
               dataConversion =  datasetVersionServiceImpl.getBaseMapper()
                        .selectOne(new LambdaQueryWrapper<DatasetVersion>()
                                .select(DatasetVersion::getDataConversion)
                                .eq(DatasetVersion::getVersionName, dataset.getCurrentVersionName())
                                .eq(DatasetVersion::getDatasetId, dataset.getId())).getDataConversion();
            }
            Integer status = dataset.getStatus();
        // 发布中
        return Objects.equals(status, DataStateCodeConstant.ANNOTATION_COMPLETE_STATE) && dataConversion != null && dataConversion != 1 ;

    }

    @Override
    //从数据集发布产生的Yolo格式文件中导入到目标数据集（引导式训练-数据准备阶段用的，并且只导入目标数据集有的标签）
    public void importDatasetFromDatasetVersion(Long targetDatasetId , Long datasetVersionId){

        DatasetVersionVO datasetVersionVO= datasetVersionServiceImpl.getDatasetVersionById(datasetVersionId);
        Dataset Targetdataset =  datasetMapper.selectById(targetDatasetId);
        if(datasetVersionVO == null || Targetdataset == null){
            return;
        }
        String datasetType = datasetVersionVO.getFormat();
        // 更新redis
        redisUtils.hset("datasetOriginStatus", String.valueOf(targetDatasetId), Targetdataset.getStatus(), 60 * 60 * 12);
        // 初始化计数器
        taskCounters.put(targetDatasetId, new AtomicInteger(0));
        switch (datasetType) {
            case "COCO":
                pool.getExecutor().execute(() -> {
                    try {
                        //handleCOCO(datasetImportDTO);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case "VOC":
                pool.getExecutor().execute(() -> {
                    try {
                       // handleVoc(datasetImportDTO);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case "YOLO":
                pool.getExecutor().execute(() -> {
                    try {
                       // handleYOLO(datasetImportDTO);
                        handelYOLOImport(datasetVersionVO,Targetdataset);
                    } catch (Exception e) {
                        e.printStackTrace();
                        datasetOperationEventService.createAndInsertEvent(targetDatasetId,new Date(),"数据集从其他版本导入-出错："+ e.getMessage(), DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_IMPORT);
                    }
                });
                break;
            case "Segment-YOLO":
                pool.getExecutor().execute(() -> {
                    try {
                        //handleSegment(datasetImportDTO);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                break;
            case "CreateML":
                break;
            default:
                break;
        }

        // 执行上传事件
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{targetDatasetId.intValue()});
            setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
        datasetOperationEventService.createAndInsertEvent(targetDatasetId,new Date(),"数据集从其他版本导入-开始", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_IMPORT);
    }

    @Override
    public List<Dataset> getAllPublishedDatasets() {
        return baseMapper.getAllPublishedDatasets();
    }

    @Override
    public Boolean isGuided(Long datasetId) {
        return datasetMapper.isGuided(datasetId);
    }

    @Override
    public void batchRecoverDataset(@NotNull(message = "id不能为空") Long[] ids) {
        // 将对应id的数据集的isDeleted置为false
        baseMapper.batchRecoverDataset(ids);
    }

    /**
     * 清理超过24小时的已删除数据集对应的文件目录
     */
    @Override
    public void deletedDatasetFileClear() {
        // 从数据库获取标记为删除的数据集
        List<Dataset> allDeletedDataset = baseMapper.getAllDeletedDataset();
        if (allDeletedDataset == null || allDeletedDataset.isEmpty()) {
            log.info("No deleted datasets found to clean up.");
            return;
        }

        List<Long> datasetIdsToDelete = new ArrayList<>();
        for (Dataset dataset : allDeletedDataset) {
            // 判断更新时间是否已超过24小时
            if (dataset.getUpdateTime().getTime() + 24 * 60 * 60 * 1000 < System.currentTimeMillis()) {
                datasetIdsToDelete.add(dataset.getId());
            }
        }

        if (datasetIdsToDelete.isEmpty()) {
            log.info("No dataset files are old enough to be deleted yet.");
            return;
        }

        log.info("Found {} datasets to delete files for.", datasetIdsToDelete.size());
        // 打印所有的id
        log.info("Dataset IDs to delete: {}", datasetIdsToDelete);

        // 遍历ID列表，执行文件删除操作
        datasetIdsToDelete.forEach(datasetId -> {
            // 构造MinIO中的"目录"前缀。注意：前缀通常以'/'结尾
            String directoryPrefix = String.format("dataset/%d/", datasetId);

            try {
                log.info("Attempting to delete directory for datasetId {} using MinIO. Bucket: [{}], Prefix: [{}]", datasetId, bucket, directoryPrefix);

                // 调用我们新增的、健壮的目录删除方法
                minioUtil.deleteDirectory(bucket, directoryPrefix);

                log.info("Successfully submitted deletion request for datasetId {} in MinIO.", datasetId);

            } catch (Exception e) {
                // 捕获并记录任何MinIO操作异常
                log.error("Failed to delete directory for datasetId {} with prefix [{}] from MinIO. Error: ", datasetId, directoryPrefix, e);
            }
        });


        log.info("Dataset file cleanup task finished.");
    }

    private void handelYOLOImport(DatasetVersionVO datasetVersionVO, Dataset targetDataset) throws Exception {
        String versionUrl = prefixPath + bucket + "/" + datasetVersionVO.getVersionUrl();
        String yoloRootDir = versionUrl + "/" + "YOLO";
        String annotationDir = yoloRootDir + "/" + "annotations";
        String imagesDir = datasetVersionVO.getVersionUrl() + "/" + "YOLO" + "/" + "images";
        java.io.File imageDir = new java.io.File(yoloRootDir, "images");

        //某个数据集的label名与labelId映射关系
        Map<String, Long>  labelNameMap = getLabelNameMap(targetDataset.getId());

        // 处理标注信息, 主要是为了得到标签在yolo中的序号与数据集labelId的对应关系
        String labelFile = annotationDir + "/classes.txt";
        Map<Long, Long> categoryMap;
        try {
            log.info("处理标签文件: {}", labelFile);
            AtomicInteger i = new AtomicInteger(0);
            categoryMap = new ConcurrentHashMap<>();
            List<String> lines = Files.readAllLines(Paths.get(labelFile));
            lines.forEach(line -> {
                if (StringUtils.isNotBlank(line)) {
                    String categoryName = line.trim();
                    Long labelId = labelNameMap.get(categoryName);
                    if (labelId != null) {
                        categoryMap.put(i.longValue(), labelId);
                    }
                    i.incrementAndGet();
                }
            });
        } catch (IOException e) {
            updateDatasetState(targetDataset.getId(), "标签文件读取失败: " + e.getMessage());
            throw new Exception("标签文件读取失败", e);
        }

        if (!imageDir.isDirectory()) {
            throw new Exception(imageDir.getAbsolutePath() + " 不是一个有效的目录");
        }

        java.io.File[] files = imageDir.listFiles();
        if (files == null || files.length == 0) {
            log.warn("图片目录 {} 为空或无法读取", imageDir.getAbsolutePath());
        } else {
            final int MAX_THREADS = 50;
            // 实际线程数不超过文件数
            final int NUM_THREADS = Math.min(files.length, MAX_THREADS);
            ExecutorService executor = null;

            try {
                log.info("开始并行处理 {} 个文件，使用 {} 个线程...", files.length, NUM_THREADS);
                executor = Executors.newFixedThreadPool(NUM_THREADS);

                // 1. 将文件处理任务封装成 CompletableFuture 列表
                final ExecutorService finalExecutor = executor;

                // 2. 等待所有任务完成
                CompletableFuture.allOf(Arrays.stream(files)
                        .filter(java.io.File::isFile) // 只处理文件
                        .map(file -> CompletableFuture.runAsync(() -> {
                            try {
                                handleYOLOFile(targetDataset.getId(), file, annotationDir, imagesDir, categoryMap);
                            } catch (Exception e) {
                                throw new CompletionException("处理文件 " + file.getName() + " 时出错", e);
                            }
                        }, finalExecutor)).toArray(CompletableFuture[]::new)).join();
                log.info("所有文件处理成功完成。");

            } catch (CompletionException e) {
                // 3. 优先捕获并发任务中的异常
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                String errorMsg = "数据集导入过程中发生错误: " + cause.getMessage();
                log.error(errorMsg, cause);
                updateDatasetState(targetDataset.getId(), errorMsg);
                throw new Exception(errorMsg, cause);

            } finally {
                if (executor != null) {
                    log.info("任务执行完毕，正在关闭专用线程池...");
                    executor.shutdown();
                    try {
                        // 等待最多60秒，让任务执行完毕
                        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                            log.warn("线程池在60秒内未能完全终止，将尝试强制关闭。");
                            executor.shutdownNow();
                        }
                    } catch (InterruptedException ie) {
                        log.error("等待线程池关闭时被中断。", ie);
                        executor.shutdownNow();
                        Thread.currentThread().interrupt();
                    }
                    log.info("专用线程池已关闭。");
                }
            }
        }

        // 3. 执行状态转换导入中->标注中/标注完成
        Dataset dataset = Dataset.builder().id(targetDataset.getId()).currentVersionName(targetDataset.getCurrentVersionName()).build();
        StateMachineUtil.stateChange(new StateChangeDTO() {{
            setObjectParam(new Object[]{dataset});
            setEventMethodName(DataStateMachineConstant.ZIP_IMPORT_FINISH_EVENT);
            setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        }});
        datasetOperationEventService.createAndInsertEvent(targetDataset.getId(), new Date(), "数据集从其他版本导入-完成", DatasetOperationEvent.EventType.INFO, DatasetOperationEvent.OperationType.DATA_IMPORT);
    }


    /**
     * 根据数据集ID列表，获取一个 "数据集名称 -> 创建时间" 的 Map
     * @param datasetIds 数据集ID列表
     * @return Map<String, Date>
     */
    @Override
    public Map<String, Date> getDatasetNameTimeMap(List<Long> datasetIds) {
        if (datasetIds == null || datasetIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<DatasetNameTimeDTO> dtoList = datasetMapper.getDatasetNamesAndTimeList(datasetIds);

        // 如果查询结果为空，也返回一个空Map
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyMap();
        }

        return dtoList.stream()
                .sorted(Comparator.comparing(DatasetNameTimeDTO::getCreateTime).reversed())
                .collect(
                        Collectors.toMap(
                                DatasetNameTimeDTO::getName,   // Map的Key
                                dto -> {
                                    Instant instant = dto.getCreateTime().toInstant();
                                    Instant newInstant = instant.plus(8, ChronoUnit.HOURS);
                                    return Date.from(newInstant);
                                }, // Map的Value
                                (existingValue, newValue) -> existingValue, // Key冲突策略：保留先出现的（由于已经排序，这里会保留时间更新的）
                                LinkedHashMap::new // 3. 指定Map的实现为 LinkedHashMap，以保证顺序 (核心改动)
                        )
                );
    }

    /**
     * 获取当前文件之后最近的未标注图片信息（包含文件ID和距离）
     * 
     * @param datasetId       数据集ID
     * @param currentFileId   当前文件ID
     * @param versionName     版本名称（可选）
     * @param labelIds        标签ID列表（可选）
     * @return 最近未标注图片的信息，如果没有则返回null
     */
    @Override
    public NearestUnannotatedFileDTO getNearestUnannotatedFileInfo(Long datasetId,
                                                                                               Long currentFileId, 
                                                                                               String versionName, 
                                                                                               List<Long> labelIds) {
        try {
            // 1. 先查询最近的未标注文件ID（只查询未删除的图片，ddvf.status IN (0,2)）
            Long targetFileId = fileMapper.findNearestUnannotatedFileId(
                datasetId, 
                currentFileId, 
                versionName, 
                labelIds
            );
            
            // distance = -1 默认妹找着
            if (targetFileId == null) {
                return new NearestUnannotatedFileDTO(null, -1);
            }
            
            // 2. 计算当前文件和目标文件之间的距离
            Integer distance = fileMapper.calculateDistanceBetweenFiles(
                datasetId,
                currentFileId,
                targetFileId,
                versionName
            );
            
            // 3. 返回包含文件ID和距离的DTO
            return new NearestUnannotatedFileDTO(targetFileId, distance != null ? distance : 0);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, 
                "Failed to find nearest unannotated file for datasetId: {}, currentFileId: {}", 
                datasetId, currentFileId, e);
            return null;
        }
    }
}

