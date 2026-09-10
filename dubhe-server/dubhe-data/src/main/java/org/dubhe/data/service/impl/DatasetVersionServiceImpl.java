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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.compress.utils.Lists;
import org.apache.logging.log4j.util.Strings;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.constant.SymbolConstant;
import org.dubhe.biz.base.dto.UserDTO;
import org.dubhe.biz.base.dto.UserSmallDTO;
import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.biz.base.enums.OperationTypeEnum;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.cloud.authconfig.service.AdminClient;
import org.dubhe.data.client.TrainServerClient;
import org.dubhe.data.constant.*;
import org.dubhe.data.dao.DatasetDatasetGroupMapper;
import org.dubhe.data.dao.DatasetGroupMapper;
import org.dubhe.data.dao.DatasetMapper;
import org.dubhe.data.dao.DatasetVersionMapper;
import org.dubhe.data.domain.bo.FileAnnotationBO;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.*;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.machine.constant.DataStateCodeConstant;
import org.dubhe.data.machine.constant.FileStateCodeConstant;
import org.dubhe.data.machine.enums.DataStateEnum;
import org.dubhe.data.machine.utils.StateIdentifyUtil;
import org.dubhe.data.pool.BasePool;
import org.dubhe.data.service.*;
import org.dubhe.data.service.task.TaskStatusService;
import org.dubhe.data.util.ConversionUtil;
import org.dubhe.data.util.FileUtil;
import org.dubhe.data.util.GeneratorKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.dubhe.data.constant.Constant.*;

/**
 * @description 数据集版本功能 服务实现类
 * @date 2020-05-14
 */
@Service
public class DatasetVersionServiceImpl extends ServiceImpl<DatasetVersionMapper, DatasetVersion> implements DatasetVersionService {


    private static final String ANNOTATION = "annotation";
    private static final String VERSION_FILE = "versionFile";
    private static final String FORMAT_YOLO = "YOLO";
    private static final String FORMAT_SEGMENT_YOLO = "Segment-YOLO";
    private static final String DIR_ANNOTATIONS = "annotations";
    private static final String FILE_DATA_YAML = "data.yaml";

    private final ConcurrentHashMap<Long, Boolean> fileCopyFlag = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, String> toZipFlag = new ConcurrentHashMap<>();

    // 用于存储从origin导出的状态，key是datasetId，value是导出状态
    private final ConcurrentHashMap<Long, String> originExportFlag = new ConcurrentHashMap<>();
    // 用于存储 origin 导出的下载 URL
    private final ConcurrentHashMap<Long, String> originExportUrl = new ConcurrentHashMap<>();

    // 用于存储从origin导出未标注数据的状态，key是datasetId，value是导出状态
    private final ConcurrentHashMap<Long, String> originUnannotatedExportFlag = new ConcurrentHashMap<>();
    // 用于存储 origin 导出未标注数据的下载 URL
    private final ConcurrentHashMap<Long, String> originUnannotatedExportUrl = new ConcurrentHashMap<>();


    /**
     * 文件服务
     */
    @Resource
    @Lazy
    public FileService fileService;
    /**
     * 数据集服务
     */
    @Resource
    private DatasetServiceImpl datasetService;
    /**
     * 数据集版本mapper
     */
    @Resource
    private DatasetVersionMapper datasetVersionMapper;
    /**
     * 数据集版本文件服务
     */
    @Resource
    private DatasetVersionFileService datasetVersionFileService;
    /**
     * 数据集版本文件服务实现类
     */
    @Resource
    private DatasetVersionFileServiceImpl datasetVersionFileServiceImpl;
    /**
     * minIo客户端工具
     */
    @Resource
    private MinioUtil minioUtil;
    /**
     * 线程池
     */
    @Autowired
    private BasePool pool;
    /**
     * bucketName
     */
    @Value("${minio.bucketName}")
    private String bucketName;

    @Autowired
    private FileUtil fileUtil;

    @Autowired
    private DatasetDatasetGroupMapper datasetDatasetGroupMapper;

    @Autowired
    private DatasetGroupMapper datasetGroupMapper;
    /**
     * 获取数据集实时状态工具类
     */
    @Resource
    private StateIdentifyUtil stateIdentify;
    /**
     * 用户服务
     */
    @Resource
    private AdminClient adminClient;

    @Autowired
    private UserContextService userContextService;

    @Autowired
    private FileServiceImpl FileServiceImpl;

    /**
     * feign调用训练服务
     */
    @Resource
    private TrainServerClient trainServiceClient;
    /**
     * 文本格式转换(json to txt)
     */
    @Autowired
    private ConversionUtil conversionUtil;
    @Autowired
    private TaskService taskService;
    @Autowired
    private DataFileAnnotationServiceImpl dataFileAnnotationServiceImpl;
    @Autowired
    private DataFileAnnotationService dataFileAnnotationService;
    @Autowired
    private GeneratorKeyUtil generatorKeyUtil;
    @Autowired
    private DatasetLabelService datasetLabelService;
    @Autowired
    private DatasetMapper datasetMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private LabelMappingService labelMappingService;
    @Autowired
    private LabelServiceImpl labelServiceImpl;
    @Autowired
    private LabelTemplateServiceImpl labelTemplateServiceImpl;
//    @Autowired
//    private RestHighLevelClient restHighLevelClient;
//    @Resource
//    private BulkProcessor bulkProcessor;

    @Autowired
    private DatasetOperationEventService datasetOperationEventService;

    @Autowired
    private TaskStatusService taskStatusService;

    @Autowired
    private TransferExportTaskService transferExportTaskService;


    /**
     * 数据集版本保存, 不要标签映射
     *
     * @param datasetVersionCreateDTO 数据集版本条件
     * @return String 版本名
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String publish(DatasetVersionCreateDTO datasetVersionCreateDTO) {
        Dataset dataset = validateAndPrepareForPublish(datasetVersionCreateDTO);
        // labelMapping自己映射自己就相当于没映射, labelMapping是必须的
        List<Label> labels = datasetLabelService.listLabelByDatasetId(dataset.getId());
        List<LabelMappingDTO> selfMappingDTOS = new LinkedList<>();

        for (Label label : labels) {
            LabelMappingDTO labelMappingDTO = new LabelMappingDTO();
            labelMappingDTO.setSourceLabelId(label.getId());
            labelMappingDTO.setTargetLabelId(label.getId());
            selfMappingDTOS.add(labelMappingDTO);
        }
        datasetVersionCreateDTO.setLabelMappings(selfMappingDTOS);
        try{
            datasetOperationEventService.createAndInsertEvent(datasetVersionCreateDTO.getDatasetId(),new Date(),"数据集保存版本-开始", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_SAVE_VERSION);
            LogUtil.info(LogEnum.BIZ_DATASET,"PublishDo执行");
            publishDo(dataset, datasetVersionCreateDTO);
        }catch (Exception e){
            datasetOperationEventService.createAndInsertEvent(datasetVersionCreateDTO.getDatasetId(),new Date(),"数据集保存版本-数据库操作出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_SAVE_VERSION);
        }
        return datasetVersionCreateDTO.getVersionName();
    }

    /**
     * 数据集版本发布（支持选择是否存档）
     * @param datasetVersionCreateDTO 数据集版本创建DTO
     * @return 版本ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long releaseWithOption(DatasetVersionCreateDTO datasetVersionCreateDTO) {
        Boolean needArchive = datasetVersionCreateDTO.getNeedArchive();

        // 如果需要存档（或未设置，默认为true保持向后兼容），执行原有逻辑
        if (needArchive == null || needArchive) {
            return release(datasetVersionCreateDTO);
        }

        // 不需要存档：直接发布一个带有标签映射的版本
        String versionName = getNextVersionName(datasetVersionCreateDTO.getDatasetId());
        datasetVersionCreateDTO.setVersionName(versionName);

        // 验证并准备发布
        Dataset dataset = validateAndPrepareForPublish(datasetVersionCreateDTO);

        // 发布版本（使用标签映射）
        datasetOperationEventService.createAndInsertEvent(datasetVersionCreateDTO.getDatasetId(),
                new Date(), "数据集发布-开始", DatasetOperationEvent.EventType.INFO,
                DatasetOperationEvent.OperationType.DATA_PUBLISH);

        Long versionId = publishDo(dataset, datasetVersionCreateDTO);
        final Long datasetId = dataset.getId();
        final String finalVersionName = versionName;

        // 修改数据集isPublishing
        datasetMapper.updateIsPublishingById(datasetId, true);

        // 创建异步任务，等待版本发布完成后，直接公开该版本，不切换回上一个版本
        CompletableFuture.runAsync(() -> {
            try {
                // 等待直到版本发布完成
                boolean publishCompleted = false;
                int maxRetries = 20000; // 最大重试次数
                int retryCount = 0;

                while (!publishCompleted && retryCount < maxRetries) {
                    // 查询版本的发布状态
                    DatasetVersion version = datasetVersionMapper.selectById(versionId);

                    // 检查版本是否完成发布 (dataConversion=1 表示发布完成)
                    if (version != null &&
                            ConversionStatusEnum.NOT_CONVERSION.getValue().equals(version.getDataConversion())) {
                        publishCompleted = true;
                    } else {
                        // 等待一段时间后重试
                        Thread.sleep(5000); // 5秒重试一次
                        retryCount++;
                    }
                }

                // 如果版本已发布完成，直接公开该版本
                if (publishCompleted) {
                    LogUtil.info(LogEnum.BIZ_DATASET, "数据集版本发布完成，开始公开版本: {}", finalVersionName);
                    // 发布完成，改一下isPublishing
                    datasetMapper.updateIsPublishingById(datasetId, false);
                    // 公开数据集版本
                    publishDatasetVersion(datasetId, finalVersionName);
                    datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                            "数据集发布-完成", DatasetOperationEvent.EventType.INFO,
                            DatasetOperationEvent.OperationType.DATA_PUBLISH);
                } else {
                    LogUtil.error(LogEnum.BIZ_DATASET, "数据集版本发布失败: {}", finalVersionName);
                    datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                            "数据集版本发布失败", DatasetOperationEvent.EventType.ERROR,
                            DatasetOperationEvent.OperationType.DATA_PUBLISH);
                    datasetMapper.updateIsPublishingById(datasetId, false);
                }
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "数据集版本发布失败: {}", finalVersionName);
                datasetOperationEventService.createAndInsertEvent(datasetId, new Date(),
                        "数据集发布-出错", DatasetOperationEvent.EventType.ERROR,
                        DatasetOperationEvent.OperationType.DATA_PUBLISH);
                datasetMapper.updateIsPublishingById(datasetId, false);
            }
        });

        return versionId;
    }


    /**
     * 数据集版本发布并返回版本ID
     *
     * @param datasetVersionCreateDTO 数据集版本条件
     * @return Long 版本ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long publish2(DatasetVersionCreateDTO datasetVersionCreateDTO) {
        Dataset dataset = validateAndPrepareForPublish(datasetVersionCreateDTO);
        return publishDo(dataset, datasetVersionCreateDTO);
    }

    /**
     * 数据集发布两个版本：一个不带标签映射，一个带标签映射
     *
     * @param datasetVersionCreateDTO 数据集版本条件
     * @return Long 版本ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long release(DatasetVersionCreateDTO datasetVersionCreateDTO) {
        // 设置并保存第一个版本名称（不带标签映射的版本）
        List<LabelMappingDTO> originLabelMappings = datasetVersionCreateDTO.getLabelMappings();
        String firstReleaseVersionName = getNextVersionName(datasetVersionCreateDTO.getDatasetId());
        datasetVersionCreateDTO.setVersionName(firstReleaseVersionName);

        // 验证并准备发布
        Dataset dataset = validateAndPrepareForPublish(datasetVersionCreateDTO);
        List<Label> labels = datasetLabelService.listLabelByDatasetId(dataset.getId());
        List<LabelMappingDTO> selfMappingDTOS = new LinkedList<>();

        for (Label label : labels) {
            LabelMappingDTO labelMappingDTO = new LabelMappingDTO();
            labelMappingDTO.setSourceLabelId(label.getId());
            labelMappingDTO.setTargetLabelId(label.getId());
            selfMappingDTOS.add(labelMappingDTO);
        }
        datasetVersionCreateDTO.setLabelMappings(selfMappingDTOS);

        // 发布第一个版本（自映射标签）
        datasetOperationEventService.createAndInsertEvent(datasetVersionCreateDTO.getDatasetId(),new Date(),"数据集发布-开始", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_PUBLISH);
        Long firstVersionId = publishDo(dataset, datasetVersionCreateDTO);
        final String firstVersionName = datasetVersionCreateDTO.getVersionName();

        // 准备发布第二个版本（使用原始标签映射）
        String secondVersionName = getNextVersionName(datasetVersionCreateDTO.getDatasetId());
        datasetVersionCreateDTO.setVersionName(secondVersionName);
        datasetVersionCreateDTO.setLabelMappings(originLabelMappings);
        // 重新查一下库确保currentVersionName被改掉
        Dataset datasetAfterFirstPublish = datasetMapper.selectById(dataset.getId());
        Long secondVersionId = publishDo(datasetAfterFirstPublish, datasetVersionCreateDTO);

        // 切换回未进行标签映射的版本
        // 创建异步任务，等待两个版本都发布完成后，切换回第一个版本
        final Long datasetId = datasetAfterFirstPublish.getId();
        // 修改数据集isPublishing
        datasetMapper.updateIsPublishingById(datasetId, true);
        CompletableFuture.runAsync(() -> {
            try {
                // 等待直到两个版本都发布完成
                boolean publishCompleted = false;
                int maxRetries = 20000; // 最大重试次数
                int retryCount = 0;

                while (!publishCompleted && retryCount < maxRetries) {
                    // 查询两个版本的发布状态
                    DatasetVersion firstVersion = datasetVersionMapper.selectById(firstVersionId);
                    DatasetVersion secondVersion = datasetVersionMapper.selectById(secondVersionId);

                    // 检查两个版本是否都完成发布 (dataConversion=1 表示发布完成)
                    if (firstVersion != null && secondVersion != null &&
                            ConversionStatusEnum.NOT_CONVERSION.getValue().equals(firstVersion.getDataConversion()) &&
                            ConversionStatusEnum.NOT_CONVERSION.getValue().equals(secondVersion.getDataConversion())) {
                        publishCompleted = true;
                    } else {
                        // 等待一段时间后重试
                        Thread.sleep(5000); // 5秒重试一次
                        retryCount++;
                    }
                }

                // 如果两个版本都已发布完成，执行版本切换
                if (publishCompleted) {
                    LogUtil.info(LogEnum.BIZ_DATASET, "数据集版本发布完成，开始切换回版本: {}", firstVersionName);
                    versionSwitch(datasetId, firstVersionName);
                    // 发布完成，改一下isPublishing
                    datasetMapper.updateIsPublishingById(datasetId, false);
                    // 发布数据集
                    publishDatasetVersion(datasetId, secondVersionName);
                } else {
                    LogUtil.error(LogEnum.BIZ_DATASET, "数据集版本发布失败，无法切换回版本: {}", firstVersionName);
                    datasetOperationEventService.createAndInsertEvent(datasetVersionCreateDTO.getDatasetId(),new Date(),"数据集版本发布失败，无法切换回版本", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_PUBLISH);
                }
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "数据集版本发布失败，无法切换回版本: {}", firstVersionName);
                datasetOperationEventService.createAndInsertEvent(datasetVersionCreateDTO.getDatasetId(),new Date(),"数据集发布-出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_PUBLISH);
            }
            datasetOperationEventService.createAndInsertEvent(datasetVersionCreateDTO.getDatasetId(),new Date(),"数据集发布-完成", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_PUBLISH);
        });

        return firstVersionId;
    }

    @Override
    public List<Label> getDatasetVersionLabels(Long datasetId, String versionName) {
        // nfs\cz-dev\dataset\617\versionFile\V0002\annotation\labels.text
        String labelTextUrl = fileUtil.getDatasetIdAbsPath(datasetId) + "/versionFile/" + versionName + "/annotation/labels.text";
        List<String> names = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(labelTextUrl))) {
            // 读取整个文件内容（假设只有一行）
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                // 按逗号分割并去除空格
                String[] categories = line.split(",");
                for (String category : categories) {
                    names.add(category.trim());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("读取标签文件失败: " + labelTextUrl, e);
        }

        List<Label> labels = new ArrayList<>();
        for (String categoryName : names) {
            Label label = Label.builder()
                    .name(categoryName)
                    .color(ConversionUtil.generateRandomColor())
                    .build();
            labels.add(label);
        }

        return labels;
    }

    /**
     * 从文件中读取数据集版本图片数量
     * @param datasetId 数据集ID
     * @param versionName 版本名称
     * @return {@link Integer }
     */
    @Override
    public Integer getVersionImgCount(Long datasetId, String versionName) {
        Map<String, Object> imageSizeStats = new HashMap<>();
        Integer fileCount = 0;

        String imageSizeStatsPath = "dataset" + "/" + datasetId + "/versionFile" + "/" +
                versionName + "/annotation/imageSizeStats.text";

        // 检查文件是否存在
        if (minioUtil.doesObjectExistSilent(bucketName, imageSizeStatsPath)) {
            try {
                String imageSizeStatsString = minioUtil.readString(bucketName, imageSizeStatsPath);
                imageSizeStats = JSONObject.parseObject(imageSizeStatsString, Map.class);

                // 读取文件数量
                if (imageSizeStats.containsKey("fileCount")) {
                    fileCount = (Integer) imageSizeStats.get("fileCount");
                }

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "Read imageSizeStats.text failed:{}", e);
            }
        } else {
            LogUtil.warn(LogEnum.BIZ_DATASET, "imageSizeStats.text file does not exist: {}", imageSizeStatsPath);
        }
        return fileCount;
    }

    @Override
    public Integer getFilteredVersionImgCount(Long datasetId, String versionName, List<String> labelNames) {
        Integer importableImageCount = 0;
        try {
            Map<String, Object> stringObjectMap = quickCalculateImportStats(datasetId, labelNames);
            importableImageCount = (Integer) stringObjectMap.get("totalImportedFiles");
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "CalculateImportStats is failed:{}", e);
        }
        return importableImageCount;
    }

    @Override
    public ImportingVersionInfoDTO getImportingVersionInfo(Long fromVersionId, Long targetDatasetId) {
        DatasetVersion datasetVersion = datasetVersionMapper.getById(fromVersionId);

        // 获取重合标签
        // 目标标签列表 引导式自己的数据集的Labels
        List<LabelDTO> list = labelServiceImpl.list(targetDatasetId);

        // 提取目标标签名称集合，用于快速匹配
        Set<String> targetLabelNames = list.stream()
                .map(LabelDTO::getName)
                .collect(Collectors.toSet());
        // 源标签列表
        List<LabelMapping> mappings = labelMappingService.findByDatasetVersionId(datasetVersion.getId());

        // 提取关联标签ID并查询标签详情
        List<Long> labelIds = mappings.stream()
                .map(LabelMapping::getTargetLabelId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Label> versionLabels = labelServiceImpl.findLabelByIds(labelIds);

        List<Label> mutualLabels = versionLabels.stream()
                .filter(label -> targetLabelNames.contains(label.getName()))
                .collect(Collectors.toList());
        // 获取图片数量
        Integer versionImgCount = fileService.getVersionImgCount(datasetVersion.getDatasetId(), datasetVersion.getVersionName());
        return new ImportingVersionInfoDTO(mutualLabels, versionImgCount);
    }

    @Override
    public Map<Long, List<Label>> getLabelsForVersions(List<Long> versionIds) {
        Map<Long, List<Label>> resultMap = new HashMap<>();

        List<DatasetVersion> versions = datasetVersionMapper.findByIds(versionIds);

        // 2. 遍历查询到的版本信息，为每个版本读取标签文件
        for (DatasetVersion version : versions) {
            try {
                List<Label> labels = getDatasetVersionLabels(version.getDatasetId(), version.getVersionName());
                resultMap.put(version.getId(), labels);
            } catch (Exception e) {
                // 即使某个版本失败，也不应中断整个批量操作
                log.error("Failed to read labels for datasetVersionId: {}");
                resultMap.put(version.getId(), Collections.emptyList());
            }
        }
        return resultMap;
    }

    /**
     * 验证数据集并准备发布
     *
     * @param datasetVersionCreateDTO 数据集版本条件
     * @return Dataset 已验证的数据集
     */
    private Dataset validateAndPrepareForPublish(DatasetVersionCreateDTO datasetVersionCreateDTO) {
        // 设置版本名称
        if (datasetVersionCreateDTO.getVersionName() == null) {
            datasetVersionCreateDTO.setVersionName(getNextVersionName(datasetVersionCreateDTO.getDatasetId()));
        }

        // 获取数据集
        Dataset dataset = datasetService.getById(datasetVersionCreateDTO.getDatasetId());
        // 1.判断数据集是否存在
        if (null == dataset) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT, "id:" + datasetVersionCreateDTO.getDatasetId(), null);
        }
        //判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (getDatasetVersionSourceVersion(dataset).getDataConversion().equals(ConversionStatusEnum.PUBLISHING.getValue())) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }

        // 3. 检查特定条件
        if ("V0001".equals(dataset.getCurrentVersionName()) && dataset.getDataType().equals(DatatypeEnum.TEXT.getValue())) {
            throw new BusinessException(ErrorEnum.DATASET_PUBLISH_REJECT);
        }

        // 4. 检查格式兼容性
        if (datasetVersionCreateDTO.getFormat() != null) {
            //coco yolo voc segment格式支持图片和视频类型数据集
            if (!checkSupportFormat(dataset.getDataType(), datasetVersionCreateDTO.getFormat())) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_FORMAT_REJECT);
            }
        }

        // 5. 检查权限
        datasetService.checkPublic(dataset, OperationTypeEnum.UPDATE);

        // 6. 检查数据集标注状态
        DataStateEnum currentDatasetStatus = stateIdentify.getStatus(dataset.getId(), dataset.getCurrentVersionName(), false);
        dataset.setStatus(currentDatasetStatus.getCode());
        if (!dataset.getStatus().equals(DataStateCodeConstant.ANNOTATION_COMPLETE_STATE)
                && !dataset.getStatus().equals(DataStateCodeConstant.AUTO_TAG_COMPLETE_STATE)
                && !dataset.getStatus().equals(DataStateCodeConstant.TARGET_COMPLETE_STATE)) {
            throw new BusinessException(ErrorEnum.DATASET_ANNOTATION_NOT_FINISH, "id:" + datasetVersionCreateDTO.getDatasetId(), null);
        }

        // 7. 检查版本是否已存在
        List<DatasetVersion> datasetVersionList = datasetVersionMapper.findDatasetVersion(
                datasetVersionCreateDTO.getDatasetId(), datasetVersionCreateDTO.getVersionName());
        if (CollectionUtil.isNotEmpty(datasetVersionList)) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_EXIST, null, null);
        }

        return dataset;
    }

    /**
     * 数据集版本切分发布
     *
     * @param versionId
     * @param splitSize
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> publishSplit(Long versionId, Double splitSize) {
        DatasetVersion srcVersion = datasetVersionMapper.getById(versionId);

        List<DatasetVersionFile> datasetVersionFiles = datasetVersionFileService.
                findByDatasetIdAndVersionName(srcVersion.getDatasetId(), srcVersion.getVersionName());
        if (datasetVersionFiles != null && datasetVersionFiles.size() > MagicNumConstant.ZERO) {
            TreeSet<DatasetVersionFile> datasetVersionFileSet = new TreeSet<>(Comparator.comparing(DatasetVersionFile::getFileId));
            datasetVersionFileSet.addAll(datasetVersionFiles);
            ArrayList<DatasetVersionFile> datasetVersionFilesList = new ArrayList<>(datasetVersionFileSet);
            int floor = (int) Math.floor(datasetVersionFilesList.size() * splitSize);
            if (floor == 0 || floor == datasetVersionFilesList.size()) {
                return null;
            }
        } else {
            return null;
        }
        DatasetVersionCreateDTO datasetVersionCreateDTO = new DatasetVersionCreateDTO();
        datasetVersionCreateDTO.setDatasetId(srcVersion.getDatasetId());
        datasetVersionCreateDTO.setFormat(srcVersion.getFormat());
        datasetVersionCreateDTO.setVersionNote("数据集切分");

        datasetVersionCreateDTO.setVersionName(getNextSplitVersionName(datasetVersionCreateDTO.getDatasetId()));

        String versionSource = srcVersion.getVersionName();

        Dataset dataset = datasetService.getById(datasetVersionCreateDTO.getDatasetId());
        // 1.判断数据集是否存在
        if (null == dataset) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT, "id:" + datasetVersionCreateDTO.getDatasetId(), null);
        }
        //判断数据集是否在发布中
        if (!StringUtils.isBlank(versionSource)) {
            if (getDatasetVersionSourceVersion(dataset).getDataConversion().equals(ConversionStatusEnum.PUBLISHING.getValue())) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        if ("V0001".equals(versionSource) && dataset.getDataType().equals(DatatypeEnum.TEXT.getValue())) {
            throw new BusinessException(ErrorEnum.DATASET_PUBLISH_REJECT);
        }
        if (datasetVersionCreateDTO.getFormat() != null) {
            //coco yolo voc segment格式支持图片和视频类型数据集
            if (!checkSupportFormat(dataset.getDataType(), datasetVersionCreateDTO.getFormat())) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_FORMAT_REJECT);
            }
        }

        datasetService.checkPublic(dataset, OperationTypeEnum.UPDATE);
        // 数据集标注完成才能发布
        DataStateEnum currentDatasetStatus = stateIdentify.getStatus(dataset.getId(), versionSource, false);
        dataset.setStatus(currentDatasetStatus.getCode());
        if (!dataset.getStatus().equals(DataStateCodeConstant.ANNOTATION_COMPLETE_STATE) && !dataset.getStatus().equals(DataStateCodeConstant.AUTO_TAG_COMPLETE_STATE)
                && !dataset.getStatus().equals(DataStateCodeConstant.TARGET_COMPLETE_STATE)) {
            throw new BusinessException(ErrorEnum.DATASET_ANNOTATION_NOT_FINISH, "id:" + datasetVersionCreateDTO.getDatasetId(), null);
        }
        // 2.判断用户输入的版本是否已经存在
        List<DatasetVersion> datasetVersionList = datasetVersionMapper.
                findDatasetVersion(datasetVersionCreateDTO.getDatasetId(), datasetVersionCreateDTO.getVersionName());

        if (CollectionUtil.isNotEmpty(datasetVersionList)) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_EXIST, null, null);
        }
        publishSplitDo(dataset, datasetVersionCreateDTO, versionSource, splitSize, true);

        DatasetVersionCreateDTO anotherCreateDTO = new DatasetVersionCreateDTO();
        anotherCreateDTO.setDatasetId(datasetVersionCreateDTO.getDatasetId());
        anotherCreateDTO.setVersionNote(datasetVersionCreateDTO.getVersionNote());
        anotherCreateDTO.setFormat(datasetVersionCreateDTO.getFormat());
        anotherCreateDTO.setVersionName(getNextSplitVersionName(datasetVersionCreateDTO.getDatasetId()));

        List<DatasetVersion> anotherVersionList = datasetVersionMapper.
                findDatasetVersion(datasetVersionCreateDTO.getDatasetId(), anotherCreateDTO.getVersionName());

        if (CollectionUtil.isNotEmpty(anotherVersionList)) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_EXIST, null, null);
        }
        publishSplitDo(dataset, anotherCreateDTO, versionSource, splitSize, false);

        DatasetVersion datasetVersion = datasetVersionMapper.
                findDatasetVersion(datasetVersionCreateDTO.getDatasetId(), datasetVersionCreateDTO.getVersionName()).get(0);

        DatasetVersion anotherVersion = datasetVersionMapper.
                findDatasetVersion(datasetVersionCreateDTO.getDatasetId(), anotherCreateDTO.getVersionName()).get(0);

        return Arrays.asList(datasetVersion.getId(), anotherVersion.getId());
    }

    /**
     * 根据数据集的id，返回发布的版本号的信息
     *
     * @param datasetId
     * @return
     */
    @Override
    public List<String> getDatasetVersionNameList(Long datasetId) {
        LambdaQueryWrapper<DatasetVersion> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(!Objects.isNull(datasetId), DatasetVersion::getDatasetId, datasetId);
        List<DatasetVersion> datasetVersionFiles = datasetVersionMapper.selectList(lambdaQueryWrapper);
        return datasetVersionFiles.stream().map(DatasetVersion::getVersionName).collect(Collectors.toList());
    }

    private boolean checkSupportFormat(Integer dataType, String format) {
        if (format.equals("COCO") || format.equals("YOLO") || format.equals("VOC") || format.equals("Segment-YOLO")) {
            return dataType.equals(DatatypeEnum.IMAGE.getValue()) || dataType.equals(DatatypeEnum.VIDEO.getValue());
        }
        return true;
    }

    /**
     * 发布部分数据库操作
     *
     * @param dataset                 数据集
     * @param datasetVersionCreateDTO 数据集版本参数
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publishDo(Dataset dataset, DatasetVersionCreateDTO datasetVersionCreateDTO) {
        String versionUrl = buildVersionUrl(dataset.getUri(), datasetVersionCreateDTO.getVersionName());
        DatasetVersion datasetVersion = new DatasetVersion(dataset.getCurrentVersionName(), versionUrl, datasetVersionCreateDTO);
        datasetVersion.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        datasetVersion.setOriginUserId(dataset.getCreateUserId());
        datasetVersion.setDataConversion(ConversionStatusEnum.PUBLISHING.getValue());
        datasetVersion.setOfRecord(datasetVersionCreateDTO.getOfRecord());
        datasetVersion.setFormat(datasetVersionCreateDTO.getFormat());
        datasetVersion.setCreateUserId(userContextService.getCurUserId());
        datasetVersion.setVersionNote(StringUtils.isNotBlank(datasetVersionCreateDTO.getVersionNote()) ? datasetVersionCreateDTO.getVersionNote() : " ");
        //新增数据集版本信息
        datasetVersionMapper.insert(datasetVersion);
        List<LabelMappingDTO> labelMappings = datasetVersionCreateDTO.getLabelMappings();

        // 获取所有label templates
        List<LabelTemplate> labelTemplates = labelTemplateServiceImpl.getAllLabelTemplate();

        // 获取当前dataset所有 labels
        List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(dataset.getId());

        for (LabelMappingDTO mapping : labelMappings) {
            Long targetLabelId = mapping.getTargetLabelId();

            // Step 1: 根据id拿 label template 的 name
            Optional<LabelTemplate> labelTemplate = labelTemplates.stream()
                    .filter(template -> template.getId().equals(targetLabelId))
                    .findFirst();

            if (labelTemplate.isPresent()) {
                String labelName = labelTemplate.get().getName();

                // Step 2: 匹配名字相同的 label
                Optional<Label> matchedLabel = datasetLabels.stream()
                        .filter(label -> label.getName().equals(labelName))
                        .findFirst();

                // Step 3: 将id 换为 label库中的id
                matchedLabel.ifPresent(label -> mapping.setTargetLabelId(label.getId()));
            }
        }

        // 收集所有的sourceLabelId
        Set<Long> sourceLabelIds = labelMappings.stream()
                .map(LabelMappingDTO::getSourceLabelId)
                .collect(Collectors.toSet());

        // 收集所有非null的targetLabelId
        Set<Long> targetLabelIds = labelMappings.stream()
                .map(LabelMappingDTO::getTargetLabelId)
                .filter(Objects::nonNull)  // 过滤掉null值
                .collect(Collectors.toSet());

        // 为那些作为target但不作为source的labelId添加自映射
        for (Long targetId : targetLabelIds) {
            if (!sourceLabelIds.contains(targetId)) {
                // 创建新的映射：targetId -> targetId
                LabelMappingDTO selfMapping = new LabelMappingDTO();
                selfMapping.setSourceLabelId(targetId);
                selfMapping.setTargetLabelId(targetId);
                labelMappings.add(selfMapping);
            }
        }

        if(datasetVersionCreateDTO.getLabelMappings() != null){
            labelMappingService.insertLabelMapping(labelMappings,datasetVersion.getId());
        }
//        //根据标签映射修改标注信息表dataFileAnnotation
//        dataFileAnnotationService.updateDataFileAnnotationsByLabelMapping(dataset.getId(),datasetVersionCreateDTO.getLabelMappings());


        // 更新数据集当前版本
        datasetService.updateVersionName(dataset.getId(), datasetVersionCreateDTO.getVersionName());
        return datasetVersion.getId();
    }

    /**
     * 发布切分部分数据库操作
     *
     * @param dataset                 数据集
     * @param datasetVersionCreateDTO 数据集版本参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishSplitDo(Dataset dataset, DatasetVersionCreateDTO datasetVersionCreateDTO, String versionSource, Double splitSize, Boolean isUpperHalf) {
        String versionUrl = dataset.getUri() + "/"
                + "versionFile" + "/" + datasetVersionCreateDTO.getVersionName();
        DatasetVersion datasetVersion = new DatasetVersion(versionSource, versionUrl, datasetVersionCreateDTO);
        datasetVersion.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        datasetVersion.setOriginUserId(dataset.getCreateUserId());
        datasetVersion.setDataConversion(ConversionStatusEnum.PUBLISHING.getValue());
        datasetVersion.setOfRecord(datasetVersionCreateDTO.getOfRecord());
        datasetVersion.setFormat(datasetVersionCreateDTO.getFormat());
        datasetVersion.setIsSplit(true);
        datasetVersion.setSplitSize(splitSize);
        datasetVersion.setIsUpperHalf(isUpperHalf);

        //新增数据集版本信息
        datasetVersionMapper.insert(datasetVersion);
    }

    /**
     * 发布时文件复制
     *
     * @param dataset        数据集
     * @param datasetVersion 数据集版本
     */
    public void publishCopyFile(Dataset dataset, DatasetVersion datasetVersion) {
        //标记开始复制操作
        fileCopyFlag.put(datasetVersion.getId(), false);

        try {
            copyFile(dataset, datasetVersion);
//            fileCopyFlagNew.put(datasetVersion.getId(), false);
            fileCopyFlag.remove(datasetVersion.getId());
            datasetOperationEventService.createAndInsertEvent(dataset.getId(),new Date(),"数据集保存版本-图片文件写入完成", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_SAVE_VERSION);
            if (dataset.getAnnotateType().equals(AnnotateTypeEnum.CLASSIFICATION.getValue()) && datasetVersion.getOfRecord().equals(MagicNumConstant.ONE)) {
                //获取新发布版本的数据集版本中间表的数据
                List<DatasetVersionFile> datasetVersionFiles =
                        datasetVersionFileService.findByDatasetIdAndVersionName(dataset.getId(), datasetVersion.getVersionName());
                Task task = Task.builder().total(datasetVersionFiles.size())
                        .datasetId(dataset.getId())
                        .type(DataTaskTypeEnum.OFRECORD.getValue())
                        .labels("")
                        .ofRecordVersion(datasetVersion.getVersionName())
                        .datasetVersionId(datasetVersion.getId()).build();
                taskService.createTask(task);
                datasetVersion.setDataConversion(MagicNumConstant.FIVE);
                baseMapper.updateById(datasetVersion);
            }
            datasetOperationEventService.createAndInsertEvent(dataset.getId(),new Date(),"数据集保存版本-完成", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_SAVE_VERSION);

        } catch (Exception e) {
            fileCopyFlag.put(datasetVersion.getId(), true);
            LogUtil.error(LogEnum.BIZ_DATASET, "fail to copy or conversion:{}", e);
            datasetOperationEventService.createAndInsertEvent(dataset.getId(),new Date(),"数据集保存版本-图片文件写入出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_SAVE_VERSION);
            throw new BusinessException(ErrorEnum.DATASET_VERSION_ANNOTATION_COPY_EXCEPTION);
        }
    }

    /**
     * 发布数据集时复制文件
     *
     * @param dataset        数据集
     * @param datasetVersion 数据集版本
     */
    private void copyFile(Dataset dataset, DatasetVersion datasetVersion) {
        // targetDir = dataset/25/versionFile/V0001（未复制版本版本号（新版本））
        String targetDir = dataset.getUri() + "/" + VERSION_FILE + "/"
                + datasetVersion.getVersionName() + "/";

        //获取当前版本（新版本）的文件URL 转换为文件名
        List<String> picNames = new ArrayList<>();
        List<String> picUrls = fileService.selectUrls(dataset.getId(), datasetVersion.getVersionName());
        picUrls.forEach(picUrl -> picNames.add(StringUtils.substringAfter(picUrl, "/")));

        //由于页面标注信息读取只支持TS格式，生成TS之外的其他格式数据集版本时，要同时生成TS格式数据集版本
        if (datasetVersion.getFormat().equals("TS")) {
            copyTSFile(dataset, datasetVersion, targetDir, picNames);
        } else if (datasetVersion.getFormat().equals("COCO")) {
            copyTSFile(dataset, datasetVersion, targetDir, picNames);
            copyCOCOFile(datasetVersion, targetDir + "CreateML/", picNames);
        } else if (datasetVersion.getFormat().equals("YOLO")) {
//            System.out.println("targetDir = " + targetDir);
//            System.out.println("picNames = " + picNames);
            //copyTSFile(dataset, datasetVersion, targetDir, picNames);
            //copyYOLOFileNew(datasetVersion, targetDir + "YOLO/", picNames);
        } else if (datasetVersion.getFormat().equals("VOC")) {
            copyTSFile(dataset, datasetVersion, targetDir, picNames);
            copyVOCFile(datasetVersion, targetDir + "VOC/", picNames);
        } else if (datasetVersion.getFormat().equals("Segment-YOLO")) {
            // Segment-YOLO格式类似YOLO，不复制origin目录
            //copySegmentFile(datasetVersion, targetDir + "Segment-YOLO/", picNames);
        }

        datasetVersion.setDataConversion(ConversionStatusEnum.NOT_CONVERSION.getValue());
        getBaseMapper().updateById(datasetVersion);
    }

    private void copyCOCOFile(DatasetVersion datasetVersion, String targetDir, List<String> picNames) {
        minioUtil.copyDir(bucketName, picNames, targetDir + "images");
    }

    private void copyYOLOFile(DatasetVersion datasetVersion, String targetDir, List<String> picNames) {
        minioUtil.copyDir(bucketName, picNames, targetDir + "obj_train_data");
    }

    private void copyYOLOFileNew(DatasetVersion datasetVersion, String targetDir, List<String> picNames) {
        minioUtil.copyDir(bucketName, picNames, targetDir + "images");
    }

    private void copyVOCFile(DatasetVersion datasetVersion, String targetDir, List<String> picNames) {
        minioUtil.copyDir(bucketName, picNames, targetDir + "images");
    }

    private void copySegmentFile(DatasetVersion datasetVersion, String targetDir, List<String> picNames) {
        minioUtil.copyDir(bucketName, picNames, targetDir + "images");
    }

    private void copyTSFile(Dataset dataset, DatasetVersion datasetVersion, String targetDir, List<String> picNames) {
        targetDir = targetDir + "origin";
        minioUtil.copyDir(bucketName, picNames, targetDir);
        if (AnnotateTypeEnum.OBJECT_DETECTION.getValue().equals(dataset.getAnnotateType())) {
            LogUtil.info(LogEnum.BIZ_DATASET, "json conversion start");
            conversionUtil.txtConversion(targetDir, dataset.getId());
            LogUtil.info(LogEnum.BIZ_DATASET, "json conversion end");
        }
    }

    /**
     * 训练任务所需版本
     *
     * @param id 数据集id
     * @return List<DatasetVersionVO> 版本列表
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public List<DatasetVersionVO> versionList(Long id) {
        List<DatasetVersionVO> list = new ArrayList<>();
        DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria = new DatasetVersionQueryCriteriaDTO();
        datasetVersionQueryCriteria.setDatasetId(id);
        datasetVersionQueryCriteria.setDeleted(NOT_DELETED);
        List<DatasetVersion> datasetVersions = datasetVersionMapper.selectList(WrapperHelp.getWrapper(datasetVersionQueryCriteria));
        datasetVersions.forEach(datasetVersion -> {
            Integer imageCounts = datasetVersionFileService.getImageCountsByDatasetIdAndVersionName(
                    datasetVersion.getDatasetId(),
                    datasetVersion.getVersionName()
            );
            DatasetVersionVO datasetVersionVO = new DatasetVersionVO();
            datasetVersionVO.setId(datasetVersion.getId());
            datasetVersionVO.setVersionName(datasetVersion.getVersionName());
            datasetVersionVO.setVersionNote(datasetVersion.getVersionNote());
            datasetVersionVO.setImageCounts(imageCounts);
            datasetVersionVO.setFormat(datasetVersion.getFormat());
            datasetVersionVO.setIsPublic(datasetVersion.getIsPublic());
            datasetVersionVO.setCreateUserId(datasetVersion.getCreateUserId());
            if (!ConversionStatusEnum.NOT_COPY.getValue().equals(datasetVersion.getDataConversion())) {
                datasetVersionVO.setVersionUrl(datasetVersion.getVersionUrl());
            }
            if (ConversionStatusEnum.IS_CONVERSION.getValue().equals(datasetVersion.getDataConversion())) {
                String binaryUrl = datasetVersion.getVersionUrl() + "/" + OFRECORD + "/" + TRAIN;
                datasetVersionVO.setVersionOfRecordUrl(binaryUrl);
            }
            list.add(datasetVersionVO);
        });
        return list;
    }

    /**
     * 保存版本文件
     *
     * @param version 数据集版本
     */
    public void saveDatasetVersionFiles(DatasetVersion version) {
        List<DatasetVersionFile> datasetVersionFiles = datasetVersionFileService.
                findByDatasetIdAndVersionName(version.getDatasetId(), version.getVersionSource());
        if (datasetVersionFiles != null && datasetVersionFiles.size() > MagicNumConstant.ZERO) {
            TreeSet<DatasetVersionFile> datasetVersionFileSet = new TreeSet<>(Comparator.comparing(DatasetVersionFile::getFileId));
            // 只保留源版本中 status 为 2 的记录（1 已删除；0 新增由 newShipVersionNameChange 迁移）
            List<DatasetVersionFile> filteredSourceFiles = datasetVersionFiles.stream()
                    .filter(f -> f.getStatus() == 2)
                    .collect(Collectors.toList());
            datasetVersionFileSet.addAll(filteredSourceFiles);


            ArrayList<DatasetVersionFile> datasetVersionFilesList = new ArrayList<>(datasetVersionFileSet);
            List<DatasetVersionFile> datasetVersionFilesListResult;
            if (version.getIsSplit()) {
                int floor = (int) Math.floor(datasetVersionFilesList.size() * version.getSplitSize());
                System.out.println("floor:" + floor);
                if (version.getIsUpperHalf()) {
                    datasetVersionFilesListResult = datasetVersionFilesList.subList(0, floor);
                } else {
                    datasetVersionFilesListResult = datasetVersionFilesList.subList(floor, datasetVersionFilesList.size());
                }
            } else {
                datasetVersionFilesListResult = datasetVersionFilesList;
            }
            datasetVersionFilesListResult.forEach(datasetVersionFile -> {
                datasetVersionFile.setVersionName(version.getVersionName());
                // 新版本中统一视为“正常文件”
                datasetVersionFile.setStatus(2);
                datasetVersionFile.setBackupStatus(datasetVersionFile.getAnnotationStatus());
                // 新版本发布完成后视为未改变基线
                datasetVersionFile.setChanged(Constant.UNCHANGED);
            });
            saveDatasetFileAnnotation(datasetVersionFilesListResult, version.getVersionSource());
        }
    }

    /**
     * 发布保存标注信息
     *
     * @param datasetVersionFiles 版本文件列表
     * @param versionSource       版本来源
     */
    public void saveDatasetFileAnnotation(List<DatasetVersionFile> datasetVersionFiles, String versionSource) {
        //versionSource已经存在的文件，需要写新的versionFile记录
        datasetVersionFiles.stream().forEach(datasetVersionFile -> {
            if (null == datasetVersionFile.getAnnotationStatus()) {
                datasetVersionFile.setAnnotationStatus(FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE);
            }
        });
        Queue<Long> dataFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_VERSION_FILE, datasetVersionFiles.size());
        Queue<Long> versionFileIds = new LinkedList<>();
        for (DatasetVersionFile datasetVersionFile : datasetVersionFiles) {
            Long dataFileId = dataFileIds.poll();
            datasetVersionFile.setId(dataFileId);
            versionFileIds.add(dataFileId);
        }
        List<List<DatasetVersionFile>> splitVersionFiles = CollectionUtil.split(datasetVersionFiles, MagicNumConstant.FOUR_THOUSAND);
        splitVersionFiles.forEach(splitVersionFile -> datasetVersionFileServiceImpl.getBaseMapper().saveList(splitVersionFile));
        //需要写新的dataFileAnnotation记录
        List<DataFileAnnotation> dataFileAnnotations = dataFileAnnotationService.getAnnotationByVersion(
                datasetVersionFiles.get(0).getDatasetId(), versionSource, MagicNumConstant.TWO);
        List<Long> oldVersionFileIds = datasetVersionFiles.stream().map(DatasetVersionFile::getId).collect(Collectors.toList());
        //获取 versionFileId 和其对应的dataFileAnnotation 的 map
        Map<Long, List<DataFileAnnotation>> versionFileAnnotations = dataFileAnnotations.stream()
                .filter(dataFileAnnotation -> oldVersionFileIds.contains(dataFileAnnotation.getVersionFileId()))
                .collect(Collectors.toMap(DataFileAnnotation::getVersionFileId, dataFileAnnotation -> {
                    List<DataFileAnnotation> dataFileAnnotationList = new ArrayList<>();
                    dataFileAnnotationList.add(dataFileAnnotation);
                    return dataFileAnnotationList;
                }, (oldVal, newVal) -> {
                    oldVal.addAll(newVal);
                    return oldVal;
                }));
        List<DataFileAnnotation> dataFileAnnotationList = new ArrayList<>();
        List<DataFileAnnotation> updateAnnotations = new ArrayList<>();
        //根据键值排序
        LinkedHashMap<Long, List<DataFileAnnotation>> sortAnnotations = versionFileAnnotations.entrySet().stream().
                sorted(Map.Entry.comparingByKey()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        sortAnnotations.entrySet().stream().map(Map.Entry::getValue).forEach(versionFileAnnotation -> {
            Long versionFileId = versionFileIds.poll();
            versionFileAnnotation.forEach(annotation -> {
                if (annotation.getStatus().equals(MagicNumConstant.TWO) && annotation.getInvariable().equals(MagicNumConstant.ONE)) {
                    annotation.setStatus(MagicNumConstant.TWO);
                    annotation.setInvariable(MagicNumConstant.ONE);
                    annotation.setVersionFileId(versionFileId);
                    dataFileAnnotationList.add(annotation);
                }
                if (annotation.getStatus().equals(MagicNumConstant.ONE) && annotation.getInvariable().equals(MagicNumConstant.ONE)) {
                    annotation.setStatus(MagicNumConstant.TWO);
                    updateAnnotations.add(annotation);
                }
                if (annotation.getStatus().equals(MagicNumConstant.ZERO) && annotation.getInvariable().equals(MagicNumConstant.ZERO)) {
                    annotation.setStatus(MagicNumConstant.TWO);
                    annotation.setInvariable(MagicNumConstant.ONE);
                    annotation.setVersionFileId(versionFileId);
                    updateAnnotations.add(annotation);
                }
            });
        });
        if (!CollectionUtils.isEmpty(dataFileAnnotationList)) {
            Queue<Long> dataFileAnnotionIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE_ANNOTATION, dataFileAnnotationList.size());
            for (DataFileAnnotation dataFileAnnotation : dataFileAnnotationList) {
                dataFileAnnotation.setId(dataFileAnnotionIds.poll());
            }
            List<List<DataFileAnnotation>> splitAnnotations = CollectionUtil.split(dataFileAnnotationList, MagicNumConstant.FOUR_THOUSAND);
            splitAnnotations.forEach(splitAnnotation -> dataFileAnnotationServiceImpl.getBaseMapper().insertBatch(splitAnnotation));
        }
        dataFileAnnotationService.updateDataFileAnnotations(updateAnnotations);
    }

    /**
     * 获取用户信息
     *
     * @param userDtoMap     用户信息
     * @param datasetVersion 数据集版本
     * @return UserSmallDTO  用户信息
     */
    public UserSmallDTO getUserSmallDTO(Map<Long, UserSmallDTO> userDtoMap, DatasetVersion datasetVersion) {
        UserSmallDTO userSmallDTO = null;
        if (!userDtoMap.containsKey(datasetVersion.getCreateUserId())) {
            UserDTO userDTO = adminClient.getUsers(datasetVersion.getCreateUserId()).getData();
            if (ObjectUtil.isNotNull(userDTO)) {
                userSmallDTO = new UserSmallDTO(userDTO);
                userDtoMap.put(datasetVersion.getCreateUserId(), userSmallDTO);
            }
        } else {
            userSmallDTO = userDtoMap.get(datasetVersion.getCreateUserId());
        }
        return userSmallDTO;
    }

    /**
     * 数据集版本列表
     *
     * @param datasetVersionQueryCriteria 查询条件
     * @return Map<String, Object>        版本列表
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Map<String, Object> getList(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        //校验入参
        if (datasetVersionQueryCriteria.getCurrent() == null || datasetVersionQueryCriteria.getSize() == null) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR);
        }
        //校验数据集是否合法
        Dataset dataset = datasetService.getById(datasetVersionQueryCriteria.getDatasetId());
        if (dataset == null) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR);
        }

        //查询数据集版本历史列表
        QueryWrapper<DatasetVersion> wrapper = WrapperHelp.getWrapper(datasetVersionQueryCriteria);
        Page<DatasetVersionVO> pages = new Page<DatasetVersionVO>() {{

            setCurrent(datasetVersionQueryCriteria.getCurrent());
            setSize(datasetVersionQueryCriteria.getSize());
            setTotal(datasetVersionMapper.selectCount(wrapper));

            // 从数据库查询DatasetVersion列表
            List<DatasetVersion> datasetVersionList = datasetVersionMapper.selectList(
                    wrapper.orderByDesc("id")
                            .last(" limit " + (datasetVersionQueryCriteria.getCurrent() - NumberConstant.NUMBER_1) * datasetVersionQueryCriteria.getSize() + ", " + datasetVersionQueryCriteria.getSize())
                            .ne("deleted", MagicNumConstant.ONE).likeRight("version_name", DATASET_VERSION_PREFIX)
            );

            // 创建结果列表
            List<DatasetVersionVO> collect = new ArrayList<>();

            // 循环遍历，将DatasetVersion转换为DatasetVersionVO
            for (DatasetVersion val : datasetVersionList) {
                DatasetVersionVO datasetVersionVO = DatasetVersionVO.from(
                        val,
                        dataset,
                        datasetService.progress(new ArrayList<Long>() {{
                            add(dataset.getId());
                        }}).get(dataset.getId()),
                        datasetVersionFileService.selectDatasetVersionFileCount(val),
                        getUserSmallDTO(new HashMap<>(MagicNumConstant.SIXTEEN), val),
                        getUserSmallDTO(new HashMap<>(MagicNumConstant.SIXTEEN), val),
                        DatasetTypeEnum.PUBLIC.getValue().compareTo(dataset.getType()) != 0,
                        datasetVersionMapper.isVersionPublic(dataset.getId(), val.getVersionName())
                );
                collect.add(datasetVersionVO);
            }

            if (!CollectionUtils.isEmpty(collect)) {
                setRecords(collect);
            }
        }};
        return PageUtil.toPage(pages);
    }

    /**
     * 数据集版本详细列表
     *
     * @param datasetVersionQueryCriteria 查询条件
     * @return Map<String, Object>        版本详细列表
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Map<String, Object> getDetailList(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        //校验入参
        if (datasetVersionQueryCriteria.getCurrent() == null || datasetVersionQueryCriteria.getSize() == null) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR);
        }
        //校验数据集是否合法
        Dataset dataset = datasetService.getById(datasetVersionQueryCriteria.getDatasetId());
        if (dataset == null) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR);
        }

        //查询数据集版本历史列表
        QueryWrapper<DatasetVersion> wrapper = WrapperHelp.getWrapper(datasetVersionQueryCriteria);

        // 创建分页对象
        Page<DatasetVersionDetailVO> pages = new Page<>();
        pages.setCurrent(datasetVersionQueryCriteria.getCurrent());
        pages.setSize(datasetVersionQueryCriteria.getSize());
        pages.setTotal(datasetVersionMapper.selectCount(wrapper));


        if (!datasetVersionQueryCriteria.getIsIncludePublishing()) {
            wrapper.eq("data_conversion", MagicNumConstant.ONE);
        }

        // 获取用户信息
        Long currentUserId = userContextService.getCurUser().getId();
        String roleName = userContextService.getCurUser().getRoles().get(0).getName();
        boolean isAdminOrManager = "管理员".equals(roleName) || "管理人员".equals(roleName);

        List<DatasetVersion> datasetVersionList = datasetVersionMapper.selectList(
                wrapper.orderByDesc("id")
                        .last(" limit " + (datasetVersionQueryCriteria.getCurrent() - NumberConstant.NUMBER_1) * datasetVersionQueryCriteria.getSize() + ", " + datasetVersionQueryCriteria.getSize())
                        .ne("deleted", MagicNumConstant.ONE)
                        .likeRight("version_name", DATASET_VERSION_PREFIX)

                        // 如果不是管理员：
                        // 1) 当isPublic未传时：可见自己的版本或公开版本
                        // 2) 当isPublic传入时：维持原逻辑，只保留自己的版本
                        .and(!isAdminOrManager, w -> {
                            if (datasetVersionQueryCriteria.getIsPublic() == null) {
                                w.and(c -> c.eq("create_user_id", currentUserId)
                                        .or()
                                        .eq("is_public", Dataset.IS_PUBLIC));
                            } else {
                                w.eq("create_user_id", currentUserId);
                            }
                        })
        );

        // 创建结果列表
        List<DatasetVersionDetailVO> resultList = new ArrayList<>();

        // 循环遍历，将DatasetVersion转换为DatasetVersionDetailVO
        for (DatasetVersion val : datasetVersionList) {
            DatasetVersionDetailVO detailVO = new DatasetVersionDetailVO();

            // 设置基本信息
            detailVO.setId(val.getId());
            detailVO.setDatasetId(dataset.getId());
            detailVO.setAnnotateType(dataset.getAnnotateType());
            detailVO.setName(dataset.getName());
            detailVO.setVersionName(val.getVersionName());
            detailVO.setCreateTime(val.getCreateTime());
            detailVO.setVersionNote(val.getVersionNote());
            detailVO.setVersionUrl(val.getVersionUrl());
            detailVO.setIsCurrent(dataset.getCurrentVersionName() == null ||
                    dataset.getCurrentVersionName().equals(val.getVersionName()));
            detailVO.setDataConversion(val.getDataConversion());
            detailVO.setIsPublic(datasetVersionMapper.isVersionPublic(dataset.getId(), val.getVersionName()));
            detailVO.setFormat(val.getFormat());
            detailVO.setVersionNote(val.getVersionNote());


            // 从imageSizeStats.text文件读取统计信息
            Map<String, Object> imageSizeStats = new HashMap<>();
            Integer fileCount = 0;
            Long totalFileSize = 0L;

            String imageSizeStatsPath = "dataset" + "/" + dataset.getId() + "/versionFile" + "/" +
                    val.getVersionName() + "/annotation/imageSizeStats.text";

            // 检查文件是否存在
            if (minioUtil.doesObjectExistSilent(bucketName, imageSizeStatsPath)) {
                try {
                    String imageSizeStatsString = minioUtil.readString(bucketName, imageSizeStatsPath);
                    imageSizeStats = JSONObject.parseObject(imageSizeStatsString, Map.class);

                    // 读取文件数量
                    if (imageSizeStats.containsKey("fileCount")) {
                        fileCount = (Integer) imageSizeStats.get("fileCount");
                    }

                    // 读取总文件大小（字节）
                    if (imageSizeStats.containsKey("totalSizeBytes")) {
                        totalFileSize = Long.valueOf(imageSizeStats.get("totalSizeBytes").toString());
                    }

                } catch (Exception e) {
                    // 如果读取失败，使用默认值或数据库查询作为后备方案
                    LogUtil.error(LogEnum.BIZ_DATASET, "Read imageSizeStats.text failed:{}", e);
                    // 后备方案：从数据库查询文件数量
                    fileCount = datasetVersionFileService.selectDatasetVersionFileCount(val);
                    totalFileSize = 1024L * 1024L * 100L; // 默认值：100MB
                }
            } else {
                // 文件不存在时的处理
                LogUtil.warn(LogEnum.BIZ_DATASET, "imageSizeStats.text file does not exist: {}", imageSizeStatsPath);
                // 后备方案：从数据库查询文件数量
                fileCount = datasetVersionFileService.selectDatasetVersionFileCount(val);
                totalFileSize = 1024L * 1024L * 100L; // 默认值：100MB
            }


            // 设置图片数量
            detailVO.setImageCount(fileCount);
            // 设置数据集文件大小
            detailVO.setTotalFileSize(totalFileSize);

            Map<String, Integer> labelCountMap = new HashMap<>();
            String labelCountPath = "dataset" + "/" + dataset.getId() + "/versionFile" + "/" +
                    val.getVersionName() + "/annotation/labelsCount.text";

            // 检查文件是否存在
            if (minioUtil.doesObjectExistSilent(bucketName, labelCountPath)) {
                try {
                    String labelIdsString = minioUtil.readString(bucketName, labelCountPath);
                    labelCountMap = JSONObject.parseObject(labelIdsString, Map.class);
                } catch (Exception e) {
                    labelCountMap = new HashMap<>();
                    LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
                }
            } else {
                // 文件不存在时的处理
                LogUtil.warn(LogEnum.BIZ_DATASET, "labelsCount.text file does not exist: {}", labelCountPath);
                labelCountMap = new HashMap<>();
            }
            detailVO.setLabelCountMap(labelCountMap);

            resultList.add(detailVO);
        }

        // 设置查询结果
        if (!CollectionUtils.isEmpty(resultList)) {
            pages.setRecords(resultList);
        }

        return PageUtil.toPage(pages);
    }

    @Override
    public Map<String, Object> getPublicDetailList(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        //校验入参
        if (datasetVersionQueryCriteria.getCurrent() == null || datasetVersionQueryCriteria.getSize() == null) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR);
        }
        //校验数据集是否合法
        Dataset dataset = datasetService.getById(datasetVersionQueryCriteria.getDatasetId());
        if (dataset == null) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR);
        }

        //查询数据集版本历史列表
        QueryWrapper<DatasetVersion> wrapper = WrapperHelp.getWrapper(datasetVersionQueryCriteria);

        // 创建分页对象
        Page<DatasetVersionDetailVO> pages = new Page<>();
        pages.setCurrent(datasetVersionQueryCriteria.getCurrent());
        pages.setSize(datasetVersionQueryCriteria.getSize());
        pages.setTotal(datasetVersionMapper.selectCount(wrapper));

        // 从数据库查询DatasetVersion列表
        List<DatasetVersion> datasetVersionList = datasetVersionMapper.selectList(
                wrapper.orderByDesc("id")
                        .last(" limit " + (datasetVersionQueryCriteria.getCurrent() - NumberConstant.NUMBER_1) * datasetVersionQueryCriteria.getSize() + ", " + datasetVersionQueryCriteria.getSize())
                        .ne("deleted", MagicNumConstant.ONE)
                        .likeRight("version_name", DATASET_VERSION_PREFIX)
        );

        // 创建结果列表
        List<DatasetVersionDetailVO> resultList = new ArrayList<>();
        List<String> labelList = datasetVersionQueryCriteria.getLabelList();

        // 循环遍历，将DatasetVersion转换为DatasetVersionDetailVO
        for (DatasetVersion val : datasetVersionList) {
            DatasetVersionDetailVO detailVO = new DatasetVersionDetailVO();

            Map<String, Integer> labelCountMap = new HashMap<>();
            try {
                String labelIdsString = minioUtil.readString(bucketName, "dataset" + "/" +dataset.getId() + "/versionFile"+"/" + val.getVersionName()+"/annotation/labelsCount.text");
                labelCountMap = JSONObject.parseObject(labelIdsString, Map.class);
            } catch (Exception e) {
                labelCountMap = new HashMap<>();
                LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
            }

            Map<String, Integer> filterLabelCountMap = new HashMap<>();

            if (!labelCountMap.isEmpty()) {
                Map<String, Integer> lowercaseLabelCountMap = new HashMap<>();
                labelCountMap.forEach((key, value) -> {
                    lowercaseLabelCountMap.merge(key.toLowerCase(), value, Integer::sum);
                });

                labelList.forEach(labelToFilter -> {
                    String lowercaseKey = labelToFilter.toLowerCase();
                    if (lowercaseLabelCountMap.containsKey(lowercaseKey)) {
                        filterLabelCountMap.put(labelToFilter, lowercaseLabelCountMap.get(lowercaseKey));
                    }
                });
                if (filterLabelCountMap.isEmpty()) {
                    //.如果筛选的标签数量为空，则跳过
                    continue;
                }
                detailVO.setLabelCountMap(filterLabelCountMap);
            } else {
                continue;
            }

            // 设置基本信息
            detailVO.setId(val.getId());
            detailVO.setDatasetId(dataset.getId());
            detailVO.setAnnotateType(dataset.getAnnotateType());
            detailVO.setName(dataset.getName());
            detailVO.setVersionName(val.getVersionName());
            detailVO.setCreateTime(val.getCreateTime());
            detailVO.setVersionNote(val.getVersionNote());
            detailVO.setVersionUrl(val.getVersionUrl());
            detailVO.setIsCurrent(dataset.getCurrentVersionName() == null ||
                    dataset.getCurrentVersionName().equals(val.getVersionName()));
            detailVO.setDataConversion(val.getDataConversion());
            detailVO.setIsPublic(datasetVersionMapper.isVersionPublic(dataset.getId(), val.getVersionName()));
            detailVO.setFormat(val.getFormat());


            // 从imageSizeStats.text文件读取统计信息
            Map<String, Object> imageSizeStats = new HashMap<>();
            Integer fileCount = 0;
            Long totalFileSize = 0L;
            try {
                String imageSizeStatsString = minioUtil.readString(bucketName,
                        "dataset" + "/" + dataset.getId() + "/versionFile" + "/" +
                                val.getVersionName() + "/annotation/imageSizeStats.text");

                imageSizeStats = JSONObject.parseObject(imageSizeStatsString, Map.class);

                // 读取文件数量
                if (imageSizeStats.containsKey("fileCount")) {
                    fileCount = (Integer) imageSizeStats.get("fileCount");
                }

                // 读取总文件大小（字节）
                if (imageSizeStats.containsKey("totalSizeBytes")) {
                    totalFileSize = Long.valueOf(imageSizeStats.get("totalSizeBytes").toString());
                }

            } catch (Exception e) {
                // 如果读取失败，使用默认值或数据库查询作为后备方案
                LogUtil.error(LogEnum.BIZ_DATASET, "Read imageSizeStats.text failed:{}", e);

                // 后备方案：从数据库查询文件数量
                fileCount = datasetVersionFileService.selectDatasetVersionFileCount(val);
                totalFileSize = 1024L * 1024L * 100L; // 默认值：100MB
            }

            // 设置图片数量
            detailVO.setImageCount(fileCount);
            // 设置数据集文件大小
            detailVO.setTotalFileSize(totalFileSize);

            resultList.add(detailVO);
        }

        // 设置查询结果
        if (!CollectionUtils.isEmpty(resultList)) {
            pages.setRecords(resultList);
        }

        return PageUtil.toPage(pages);
    }

    @Override
    public List<DatasetVersionInfoVO> getVersionIdsWithDatasetInfo(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return Collections.emptyList();
        }

        List<Dataset> datasets = datasetMapper.selectList(new QueryWrapper<Dataset>().in("id", datasetIds));

        if (CollectionUtils.isEmpty(datasets)) {
            return Collections.emptyList();
        }

        ArrayList<DatasetVersionInfoVO> infoList = new ArrayList<>();
        for (Dataset dataset : datasets) {
            List<Long> idListByDatasetId = datasetVersionMapper.getIdListByDatasetId(dataset.getId());
            infoList.add(
                    DatasetVersionInfoVO.builder()
                            .datasetName(dataset.getName())
                            .datasetId(dataset.getId())
                            .datasetVersionIds(idListByDatasetId)
                            .build()
            );
        }
        return infoList;
    }

    @Override
    public List<DatasetVersionDetailVO> getPublicDetailListAll(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria) {
        // 校验数据集是否合法
        Dataset dataset = datasetService.getById(datasetVersionQueryCriteria.getDatasetId());
        if (dataset == null) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR);
        }

        QueryWrapper<DatasetVersion> wrapper = WrapperHelp.getWrapper(datasetVersionQueryCriteria);
        List<DatasetVersion> datasetVersionList = datasetVersionMapper.selectList(
                wrapper.orderByDesc("id")
                        .ne("deleted", MagicNumConstant.ONE)
                        .likeRight("version_name", DATASET_VERSION_PREFIX)
        );

        List<DatasetVersionDetailVO> resultList = new ArrayList<>();
        List<String> labelList = datasetVersionQueryCriteria.getLabelList();
        if(labelList == null){
            labelList = new ArrayList<>();
        }

        for (DatasetVersion val : datasetVersionList) {
            DatasetVersionDetailVO detailVO = new DatasetVersionDetailVO();

            Map<String, Integer> labelCountMap = new HashMap<>();
            try {
                String labelIdsString = minioUtil.readString(bucketName, "dataset" + "/" + dataset.getId() + "/versionFile" + "/" + val.getVersionName() + "/annotation/labelsCount.text");
                labelCountMap = JSONObject.parseObject(labelIdsString, Map.class);
            } catch (Exception e) {
                labelCountMap = new HashMap<>();
                LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
            }

            // 如果传入了标签过滤条件
            if (!labelList.isEmpty()) {
                Map<String, Integer> filterLabelCountMap = new HashMap<>();
                if (!labelCountMap.isEmpty()) {
                    Map<String, Integer> lowercaseLabelCountMap = new HashMap<>();
                    labelCountMap.forEach((key, value) -> lowercaseLabelCountMap.merge(key.toLowerCase(), value, Integer::sum));

                    labelList.forEach(labelToFilter -> {
                        String lowercaseKey = labelToFilter.toLowerCase();
                        if (lowercaseLabelCountMap.containsKey(lowercaseKey)) {
                            filterLabelCountMap.put(labelToFilter, lowercaseLabelCountMap.get(lowercaseKey));
                        }
                    });

                    if (filterLabelCountMap.isEmpty()) {
                        // 如果筛选后标签为空，则跳过此版本
                        continue;
                    }
                    detailVO.setLabelCountMap(filterLabelCountMap);
                } else {
                    continue;
                }
            } else {
                // 如果没有传入标签过滤，则显示所有标签
                detailVO.setLabelCountMap(labelCountMap);
            }

            // 设置基本信息
            detailVO.setId(val.getId());
            detailVO.setDatasetId(dataset.getId());
            detailVO.setAnnotateType(dataset.getAnnotateType());
            detailVO.setName(dataset.getName());
            detailVO.setVersionName(val.getVersionName());
            detailVO.setCreateTime(val.getCreateTime());
            detailVO.setVersionNote(val.getVersionNote());
            detailVO.setIsCurrent(dataset.getCurrentVersionName() != null &&
                    dataset.getCurrentVersionName().equals(val.getVersionName()));
            detailVO.setDataConversion(val.getDataConversion());
            detailVO.setIsPublic(datasetVersionMapper.isVersionPublic(dataset.getId(), val.getVersionName()));
            detailVO.setFormat(val.getFormat());

            // 从imageSizeStats.text文件读取统计信息
            Integer fileCount = 0;
            Long totalFileSize = 0L;
            try {
                String imageSizeStatsString = minioUtil.readString(bucketName,
                        "dataset" + "/" + dataset.getId() + "/versionFile" + "/" +
                                val.getVersionName() + "/annotation/imageSizeStats.text");

                Map<String, Object> imageSizeStats = JSONObject.parseObject(imageSizeStatsString, Map.class);
                if (imageSizeStats != null) {
                    fileCount = (Integer) imageSizeStats.getOrDefault("fileCount", 0);
                    totalFileSize = Long.valueOf(String.valueOf(imageSizeStats.getOrDefault("totalSizeBytes", 0L)));
                }
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "Read imageSizeStats.text failed:{}, fallback to DB query", e.getMessage());
            }


            Integer importableImageCount = 0;
            try {
                Map<String, Object> stringObjectMap = quickCalculateImportStats(val.getId(), labelList);
                importableImageCount = (Integer) stringObjectMap.get("totalImportedFiles");
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "CalculateImportStats is failed:{}", e);
                importableImageCount = 0;
            }
            detailVO.setImportableImageCount(importableImageCount);
            detailVO.setImageCount(fileCount);
            detailVO.setTotalFileSize(totalFileSize);
            resultList.add(detailVO);
        }

        // 直接返回列表
        return resultList;
    }

    /**
     * 格式化文件大小
     *
     * @param bytes 字节数
     * @return 格式化后的文件大小字符串
     */
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }


    /**
     * 删除版本
     *
     * @param datasetId          数据集id
     * @param versionName        版本名
     * @param datasetVersionUrls 数据集版本url
     */
    public void delVersion(Long datasetId, String versionName, List<String> datasetVersionUrls) {
        Dataset dataset = datasetService.getById(datasetId);
        if (null == dataset) {
            throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH, "id:" + datasetId, null);
        }
        datasetService.checkPublic(dataset, OperationTypeEnum.UPDATE);
        if (versionName.equals(dataset.getCurrentVersionName())) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_DELETE_CURRENT_ERROR);
        }
        UpdateWrapper<DatasetVersion> datasetVersionUpdateWrapper = new UpdateWrapper<>();
        datasetVersionUpdateWrapper.eq("dataset_id", datasetId)
                .eq("version_name", versionName);
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDeleted(true);
        baseMapper.update(datasetVersion, datasetVersionUpdateWrapper);

        UpdateWrapper<DatasetVersionFile> datasetVersionFileUpdateWrapper = new UpdateWrapper<>();
        datasetVersionFileUpdateWrapper.eq("dataset_id", datasetId)
                .eq("version_name", versionName);
        DatasetVersionFile datasetVersionFile = new DatasetVersionFile();
        datasetVersionFile.setStatus(MagicNumConstant.ONE);
        datasetVersionFileServiceImpl.getBaseMapper().update(datasetVersionFile, datasetVersionFileUpdateWrapper);
//        //删除版本对应的minio文件
//        datasetVersionUrls.forEach(dataseturl -> {
//            try {
//                minioUtil.del(bucketName, dataseturl);
//            } catch (Exception e) {
//                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO delete the dataset version file error", e);
//            }
//        });
    }

    /**
     * 数据集版本删除
     *
     * @param datasetVersionDeleteDTO 数据集版本删除条件
     */
    @Override
    @DataPermissionMethod
    public void versionDelete(DatasetVersionDeleteDTO datasetVersionDeleteDTO) {
        datasetService.checkPublic(datasetVersionDeleteDTO.getDatasetId(), OperationTypeEnum.UPDATE);
        //取出当前数据集版本的url
        List<String> thisUrls = datasetVersionMapper.selectVersionUrl(datasetVersionDeleteDTO.getDatasetId(), datasetVersionDeleteDTO.getVersionName());
        List<String> datasetVersionUrls = new ArrayList<>();
        thisUrls.forEach(url -> {
            datasetVersionUrls.add(url);
            datasetVersionUrls.add(url + StrUtil.SLASH + "ofrecord" + StrUtil.SLASH + "train");
        });

        this.delVersion(datasetVersionDeleteDTO.getDatasetId(), datasetVersionDeleteDTO.getVersionName(), datasetVersionUrls);
//        if (!CollectionUtils.isEmpty(datasetVersionUrls)) {
//            //训练中的url进行比较
//            PtTrainDataSourceStatusQueryDTO dto = new PtTrainDataSourceStatusQueryDTO();
//            DataResponseBody<Map<String, Boolean>> trainDataSourceStatusData = trainServiceClient.getTrainDataSourceStatus(dto.setDataSourcePath(datasetVersionUrls));
//            if (!trainDataSourceStatusData.succeed() || Objects.isNull(trainDataSourceStatusData.getData())) {
//                throw new BusinessException(ErrorEnum.DATASET_VERSION_PTJOB_STATUS);
//            }
//            if (!trainDataSourceStatusData.getData().containsValue(false)) {
//                this.delVersion(datasetVersionDeleteDTO.getDatasetId(), datasetVersionDeleteDTO.getVersionName(), datasetVersionUrls);
//            }
//        } else {
//            this.delVersion(datasetVersionDeleteDTO.getDatasetId(), datasetVersionDeleteDTO.getVersionName(), datasetVersionUrls);
//        }
    }

    /**
     * 数据集版本切换
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @DataPermissionMethod
    public void versionSwitch(Long datasetId, String versionName) {
        datasetService.checkPublic(datasetId, OperationTypeEnum.UPDATE);
        Dataset dataset = datasetService.getById(datasetId);
        // 业务判断
        // 1.判断数据集是否存在
        if (null == dataset) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT, "id:" + datasetId, null);
        }
        //判断目标版本是否存在
        QueryWrapper<DatasetVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DatasetVersion::getDatasetId, datasetId).eq(DatasetVersion::getVersionName, versionName);
        DatasetVersion datasetVersion = baseMapper.selectOne(queryWrapper);
        if (datasetVersion == null) {
            throw new BusinessException(ErrorEnum.DATASET_CHECK_VERSION_ERROR);
        }
        //判断数据集是否在发布中
        if (!StringUtils.isBlank(dataset.getCurrentVersionName())) {
            if (getDatasetVersionSourceVersion(dataset).getDataConversion().equals(NumberConstant.NUMBER_4)) {
                throw new BusinessException(ErrorEnum.DATASET_PUBLISH_ERROR);
            }
        }
        // 自动标注中不允许版本切换
        if (dataset.getStatus().equals(DataStateCodeConstant.AUTOMATIC_LABELING_STATE)
                || dataset.getStatus().equals(DataStateCodeConstant.TARGET_FOLLOW_STATE)
                || dataset.getStatus().equals(DataStateCodeConstant.TARGET_FAILURE_STATE)
        ) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_STATUS_NO_SWITCH, "id:" + datasetId, null);
        }
        dataFileAnnotationService.rollbackAnnotation(dataset.getId(), dataset.getCurrentVersionName()
                , MagicNumConstant.ONE, MagicNumConstant.ZERO);
        dataFileAnnotationService.rollbackAnnotation(dataset.getId(), dataset.getCurrentVersionName()
                , MagicNumConstant.TWO, MagicNumConstant.ONE);
        //版本回退
        datasetVersionFileService.rollbackDataset(dataset);


        String prefixPath = dataset.getUri() + "/";
            String annotationSourceDir = prefixPath + VERSION_FILE + '/' + datasetVersion.getVersionName() + '/'+ ANNOTATION;

        Map<String, Integer> labelMap = new HashMap<>();
        try {
            String labelIdsString = minioUtil.readString(bucketName, annotationSourceDir + "/labelsIds.text");
            Map<Integer, String> idLabelMap = JSONObject.parseObject(labelIdsString, Map.class);
            labelMap = idLabelMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
        }

        //版本切换的时候，需要同时把dataset外面的标注文件改成目标版本对应的标注内容
        //不然切換版本后，会直接发布会发布当前dataset外面的标注文件，导致不一致问题（当前外面的标注文件和切换版本的标注信息不一致）
        Map<String, Integer> finalLabelMap = labelMap;
        pool.getExecutor().submit(() -> {
            processAnnotationsAsync(dataset, datasetVersion, finalLabelMap);
        });

        // 2.版本切换
        datasetService.updateVersionName(datasetId, versionName);
        //更新当前版本数据集的状态
        DataStateEnum status = stateIdentify.getStatus(dataset.getId(), versionName, true);
        datasetService.updateStatus(dataset.getId(), status);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAnnotationsAsync(Dataset dataset, DatasetVersion datasetVersion, Map<String, Integer> labelMap) {
        try {
            String prefixPath = dataset.getUri() + "/";
            List<FileAnnotationBO> files = fileService.listByDatasetIdAndVersionName(dataset.getId(), datasetVersion.getVersionName());
            String annotationSourceDir = prefixPath + VERSION_FILE + '/' + datasetVersion.getVersionName() + '/' + ANNOTATION;

            List<FileAnnotationBO> annotationFiles = new ArrayList<>();
            files.forEach(file -> {
                String annotationUrl = annotationSourceDir + "/" + file.getFileName();
                file.setAnnotationUrl(annotationUrl);
                annotationFiles.add(file);
            });

            Map<String, Integer> finalLabelMap = labelMap;
            annotationFiles.forEach(annotation -> {
                try {
                    String annotationUrl = annotation.getAnnotationUrl();
                    String jsonStr = minioUtil.readString(bucketName, annotationUrl);

                    JSONArray jsonArray = JSON.parseArray(jsonStr);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String categoryIdStr = jsonObject.getString("category_id");
                        Integer labelId = finalLabelMap.get(categoryIdStr);
                        if (labelId != null) {
                            jsonObject.put("category_id", (long) labelId);
                        } else {
                            LogUtil.warn(LogEnum.BIZ_DATASET, "Label mapping not found for category: {}", categoryIdStr);
                        }
                    }

                    String updatedJsonStr = jsonArray.toJSONString();
                    String targetPath = dataset.getUri() + "/" + ANNOTATION + "/" +
                            annotationUrl.substring(annotationUrl.lastIndexOf("/") + 1);

                    minioUtil.writeString(bucketName, targetPath, updatedJsonStr);

                } catch (Exception e) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "Failed to process annotation file: {}, Error: {}",
                            annotation.getFileName(), e.getMessage());
                }
            });
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Async annotation processing failed: {}", e.getMessage());
        }
    }




    /**
     * 获取下一个可用版本号
     *
     * @param datasetId 数据集id
     * @return String 下一个可用版本名称
     */
    @Override
    @DataPermissionMethod
    public String getNextVersionName(Long datasetId) {
        Dataset dataset = datasetService.getById(datasetId);
        if (null == dataset) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT, "id:" + datasetId, null);
        }
        String maxVersionName = datasetVersionMapper.getMaxVersionName(datasetId);
        if (StringUtils.isEmpty(maxVersionName)) {
            return Constant.DEFAULT_VERSION;
        } else {
            Integer versionName = Integer.parseInt(maxVersionName.substring(1)) + MagicNumConstant.ONE;
            return Constant.DATASET_VERSION_PREFIX + StringUtils.stringFillIn(versionName.toString(), MagicNumConstant.FOUR, MagicNumConstant.ZERO);
        }
    }

    /**
     * 获取下一个可用切分版本号
     *
     * @param datasetId 数据集id
     * @return String 下一个可用版本名称
     */
    @Override
    @DataPermissionMethod
    public String getNextSplitVersionName(Long datasetId) {
        Dataset dataset = datasetService.getById(datasetId);
        if (null == dataset) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT, "id:" + datasetId, null);
        }
        String maxVersionName = datasetVersionMapper.getMaxSplitVersionName(datasetId);
        if (StringUtils.isEmpty(maxVersionName)) {
            return Constant.DEFAULT_SPLIT_VERSION;
        } else {
            Integer versionName = Integer.parseInt(maxVersionName.substring(1)) + MagicNumConstant.ONE;
            return Constant.DATASET_SPLIT_VERSION_PREFIX + StringUtils.stringFillIn(versionName.toString(), MagicNumConstant.FOUR, MagicNumConstant.ZERO);
        }
    }

    /**
     * 数据集版本数据删除
     *
     * @param datasetId 数据集id
     */
    @Override
    public void updateStatusByDatasetId(Long datasetId, Boolean deleteFlag) {
        baseMapper.updateById(DatasetVersion.builder().datasetId(datasetId).deleted(deleteFlag).build());
    }


    /**
     * 数据转换回调接口
     *
     * @param datasetVersionId 版本id
     * @return int 影响版本数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int finishConvert(Long datasetVersionId, ConversionCreateDTO conversionCreateDTO) {
        LogUtil.info(LogEnum.BIZ_DATASET, "conversion call-back id:{},msg:{}", datasetVersionId, conversionCreateDTO.getMsg());
        DatasetVersion datasetVersion = getBaseMapper().selectById(datasetVersionId);
        if (CONVERSION_SUCCESS.equals(conversionCreateDTO.getMsg())) {
            datasetVersion.setDataConversion(ConversionStatusEnum.IS_CONVERSION.getValue());
        } else {
            datasetVersion.setDataConversion(ConversionStatusEnum.UNABLE_CONVERSION.getValue());
        }
        datasetVersion.setUpdateTime(new Timestamp(System.currentTimeMillis()));
        return getBaseMapper().updateById(datasetVersion);
    }

    /**
     * 文件复制
     */
    @Override
    public void fileCopy() {
        DatasetVersionCriteriaVO needFileCopy = DatasetVersionCriteriaVO.builder()
                .deleted(NOT_DELETED).dataConversion(ConversionStatusEnum.NOT_COPY.getValue()).build();
        //查处所有转换状态是未复制的版本
        List<DatasetVersion> versions = list(WrapperHelp.getWrapper(needFileCopy));
        if (CollectionUtil.isEmpty(versions)) {
            LogUtil.info(LogEnum.BIZ_DATASET, "No version data to copy");
            return;
        }
        versions.forEach(version -> {
            fileCopyFlag.putIfAbsent(version.getId(), true);
            //如果当前版本状态为未复制
            if (fileCopyFlag.get(version.getId())) {
                try {
                    Dataset dataset = datasetService.getBaseMapper().selectById(version.getDatasetId());
                    //未复制状态的版本
                    pool.getExecutor().submit(() -> publishCopyFile(dataset, version));
                } catch (Exception e) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "copy task is refused", e);
                }
            }
        });
    }

    /**
     * 标注文件复制
     */
    /**
     * 标注文件复制
     */
    @Override
    public void annotationFileCopy() {

        DatasetVersionCriteriaVO needFileCopy = DatasetVersionCriteriaVO.builder()
                .deleted(NOT_DELETED).dataConversion(ConversionStatusEnum.PUBLISHING.getValue()).build();
        List<DatasetVersion> versions = list(WrapperHelp.getWrapper(needFileCopy));
        versions.forEach(version -> {
            Dataset dataset = datasetService.getBaseMapper().selectById(version.getDatasetId());
            //写入标注文件
            try{
                // 写入版本文件关系数据(新版本) - 正常情况
                saveDatasetVersionFiles(version);
                // 更改新增关系版本信息
                datasetVersionFileService.newShipVersionNameChange(dataset.getId(),
                        version.getVersionSource(), version.getVersionName());
                wirteAnnotationFile(version, dataset);
                datasetOperationEventService.createAndInsertEvent(dataset.getId(),new Date(),"数据集保存版本-标注文件写入完成", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_SAVE_VERSION);
            }catch (Exception e){
                datasetOperationEventService.createAndInsertEvent(dataset.getId(),new Date(),"数据集保存版本-标注文件写入出错", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_SAVE_VERSION);
            }

            version.setDataConversion(ConversionStatusEnum.NOT_COPY.getValue());
            getBaseMapper().updateById(version);
            rollbackVersion(version, dataset);


        });
    }

    private void rollbackVersion(DatasetVersion version, Dataset dataset) {
        //版本回退
        dataset.setCurrentVersionName(version.getVersionSource());
        dataFileAnnotationService.rollbackAnnotation(dataset.getId(), version.getVersionSource()
                , MagicNumConstant.TWO, MagicNumConstant.ONE);
        dataFileAnnotationService.rollbackAnnotation(dataset.getId(), version.getVersionSource()
                , MagicNumConstant.ONE, MagicNumConstant.ZERO);
        datasetVersionFileService.rollbackDataset(dataset);
    }

    private void wirteAnnotationFile(DatasetVersion version, Dataset dataset) {

        String prefixPath = dataset.getUri() + "/";
        String annVersionTargetDir = prefixPath + VERSION_FILE + "/"
                + version.getVersionName() + "/";
        List<FileAnnotationBO> files = fileService.listByDatasetIdAndVersionName(dataset.getId(), version.getVersionName());


        Task task = Task.builder()
                .datasetId(dataset.getId())
                .status(MagicNumConstant.ONE)
                .labels(JSONArray.toJSONString(Collections.emptyList()))
                .files(JSON.toJSONString(Collections.EMPTY_LIST))
                .total(files.size())
                .finished(MagicNumConstant.ZERO)
                .stop(false)
                .build();

        String publishTaskKey = "publishTask:" + dataset.getId() + ":" + version.getVersionName();

        redisUtils.set(publishTaskKey, JSONObject.toJSONString(task), 100 * 60);

        if (version.getVersionSource() == null) {
            String annotationSourceDir = prefixPath + ANNOTATION;
            List<FileAnnotationBO> annotationFiles = new ArrayList<>();
            files.forEach(file -> {
                String annotationUrl = annotationSourceDir + "/" + file.getFileName();
                file.setAnnotationUrl(annotationUrl);
                annotationFiles.add(file);
            });
            wirteMinoAnnotationFile(version, dataset, annotationFiles, annVersionTargetDir);
        } else {
            List<FileAnnotationBO> unChangedFiles = fileService.selectFileAnnotations(dataset.getId(), MagicNumConstant.ZERO, version.getVersionName());
            String unChangedAnnotationSourceDir = prefixPath + ANNOTATION;
            List<FileAnnotationBO> unChangedAnnotationFiles = new ArrayList<>();
            unChangedFiles.forEach(unChangedFile -> {
                String annotationUrl = unChangedAnnotationSourceDir + "/" + unChangedFile.getFileName();
                unChangedFile.setAnnotationUrl(annotationUrl);
                unChangedAnnotationFiles.add(unChangedFile);
            });
            //wirteMinoAnnotationFile(version, dataset, unChangedAnnotationFiles, annVersionTargetDir);
            List<FileAnnotationBO> changedFiles = fileService.selectFileAnnotations(dataset.getId(), MagicNumConstant.ONE, version.getVersionName());
            String changedAnnotationSourceDir = prefixPath + ANNOTATION;
            List<FileAnnotationBO> changedAnnotationFiles = new ArrayList<>();
            changedFiles.forEach(changedFile -> {
                String annotationUrl = changedAnnotationSourceDir + "/" + changedFile.getFileName();
                changedFile.setAnnotationUrl(annotationUrl);
                changedAnnotationFiles.add(changedFile);
            });
            List<FileAnnotationBO> allAnnotationFiles = new ArrayList<>();
            allAnnotationFiles.addAll(unChangedAnnotationFiles);
            allAnnotationFiles.addAll(changedAnnotationFiles);
            //wirteMinoAnnotationFile(version, dataset, changedAnnotationFiles, annVersionTargetDir);
            wirteMinoAnnotationFile(version, dataset, allAnnotationFiles, annVersionTargetDir);
        }

    }

    /**
     * 优化版本：减少重复IO，增加并行处理
     */
    public void writeAllAnnotationFormatsOptimized(DatasetVersion version, Dataset dataset,
                                                   List<FileAnnotationBO> sourceFiles, String targetDir,
                                                   Map<Long, String> labelMaps, List<Label> datasetLabels,
                                                   Long datasetID, String versionName, Map<Long, Long> labelMappings) {
        long startTime = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-开始处理所有格式，文件数量: " + sourceFiles.size());
        if (CollectionUtils.isEmpty(sourceFiles)) {
            return;
        }

        // 线程安全的 labelName 计数器
        ConcurrentHashMap<String, LongAdder> labelCounts = new ConcurrentHashMap<>();
        // 记录每个标签出现在哪些文件中（用于统计文件数量）
        ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> labelFileMap = new ConcurrentHashMap<>();
        // 记录每个文件包含的标签集合（用于导入匹配统计）
        ConcurrentHashMap<String, Set<String>> fileLabelsMap = new ConcurrentHashMap<>();

        // 1. 预处理：保持原有逻辑
        long prepStart = System.currentTimeMillis();

        // TS格式预处理 - 保持原有逻辑
        Map<Long, String> AllTargetLabelMaps = new HashMap<>();
        datasetLabels.forEach(label -> {
            AllTargetLabelMaps.put(label.getId(), label.getName());
        });

        Map<Long, String> targetLabelMaps = new HashMap<>();
        labelMappings.forEach((sourceCategoryId, targetCategoryId) -> {
            if (targetCategoryId != null) {
                String labelName = labelServiceImpl.findLabelByIds(Collections.singletonList(targetCategoryId)).get(0).getName();
                targetLabelMaps.putIfAbsent(targetCategoryId, labelName);
            }
        });

        // YOLO格式预处理 - 保持原有逻辑
        List<Long> categoryIds = Lists.newArrayList();
        conversionUtil.writeYOLOCommonNew(targetDir + "YOLO/", datasetLabels, categoryIds, labelMappings);

        Map<Long, Integer> labelToIndexMap = new HashMap<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            labelToIndexMap.put(categoryIds.get(i), i);
        }

        long prepEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-预处理完成，耗时: " + (prepEnd - prepStart) + "ms");


        long sizeStart = System.currentTimeMillis();
        Map<String, Long> fileSizes = new HashMap<>();
        try {
            // 构建批量查询的参数 Map<objectName, bucketName>
            Map<String, String> bucketAndObjectNames = new HashMap<>();
            for (FileAnnotationBO sourceFile : sourceFiles) {
                String fileUrl = sourceFile.getFileUrl(); // 如: cz-dev/dataset/995/origin/1_194e6.jpg

                // 从fileUrl中提取objectName（去掉bucketName前缀）
                String objectName;
                if (fileUrl.startsWith(bucketName + "/")) {
                    objectName = fileUrl.substring(bucketName.length() + 1); // dataset/995/origin/1_194e6.jpg
                } else {
                    objectName = fileUrl; // 如果没有前缀，直接使用
                }

                bucketAndObjectNames.put(objectName, bucketName);
            }

            // 批量获取文件大小
            fileSizes = minioUtil.getFilesSizes(bucketAndObjectNames);
            LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-批量获取文件大小完成，获取到 " + fileSizes.size() + " 个文件大小");
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-批量获取文件大小失败", e);
        }
        long sizeEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-文件大小获取完成，耗时: " + (sizeEnd - sizeStart) + "ms");

        // 2. Redis任务初始化 - 保持原有逻辑
        String publishTaskKey = "publishTask:" + datasetID + ":" + versionName;
        String taskJson = (String) redisUtils.get(publishTaskKey);
        Task task = JSON.parseObject(taskJson, Task.class);
        Integer originFinished = task.getFinished();
        AtomicInteger processedCount = new AtomicInteger(0);

        // 3. 并行处理 - 主要优化点
        int threadCount = Math.min(8, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-使用 " + threadCount + " 个线程并行处理");

        long processStart = System.currentTimeMillis();

        try {
            // 手动分批，避免依赖Guava
            int batchSize = 50;
            List<List<FileAnnotationBO>> batches = new ArrayList<>();
            for (int i = 0; i < sourceFiles.size(); i += batchSize) {
                int end = Math.min(i + batchSize, sourceFiles.size());
                batches.add(sourceFiles.subList(i, end));
            }

            List<Future<Void>> futures = new ArrayList<>();

            for (List<FileAnnotationBO> batch : batches) {
                Future<Void> future = executor.submit(() -> {
                    processBatchOptimized(batch, targetDir, labelMappings, targetLabelMaps,
                            labelToIndexMap, processedCount, task, publishTaskKey,
                            originFinished, sourceFiles.size(),labelCounts,labelFileMap,fileLabelsMap);
                    return null;
                });
                futures.add(future);
            }

            // 等待所有任务完成
            for (Future<Void> future : futures) {
                future.get();
            }

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-并行处理异常", e);
        } finally {
            executor.shutdown();
        }

        long processEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-文件处理完成，耗时: " + (processEnd - processStart) + "ms");

        // 4. 写入标签文件 - 保持原有逻辑
        long labelStart = System.currentTimeMillis();
        try {
            List<String> labelStr = new ArrayList<>(targetLabelMaps.values());
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labels.text", Strings.join(labelStr, ','));
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelsIds.text", JSONObject.toJSONString(AllTargetLabelMaps));
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-标签文件写入异常", e);
        }
        long labelEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-标签文件写入完成，耗时: " + (labelEnd - labelStart) + "ms");

        long countStart = System.currentTimeMillis();
        try {
            Map<String, Long> labelCountResult = labelCounts.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().longValue()
                    ));
            // 统计每个标签出现在多少个文件中
            Map<String, Integer> labelFileCountResult = labelFileMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().size()
                    ));
            Map<String, Integer> labelCombinationStats = new HashMap<>();
            for (Set<String> fileLabels : fileLabelsMap.values()) {
                String labelCombination;
                if (fileLabels == null || fileLabels.isEmpty()) {
                    labelCombination = ""; // 空标签用空字符串表示
                } else {
                    // 将标签排序后用逗号连接，确保相同标签组合有相同的key
                    labelCombination = fileLabels.stream()
                            .sorted()
                            .collect(Collectors.joining(","));
                }
                labelCombinationStats.put(labelCombination,
                        labelCombinationStats.getOrDefault(labelCombination, 0) + 1);
            }



            // 与 labelsIds.text 一样写成文本（内容为 JSON）
            // 写入标签总数统计
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelsCount.text",
                    JSONObject.toJSONString(labelCountResult));

            // 写入标签出现的文件数统计
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelsFileCount.text",
                    JSONObject.toJSONString(labelFileCountResult));

            // 写入每个文件的标签信息（用于导入匹配计算）
           // Map<String, Set<String>> fileLabelsResult = new HashMap<>(fileLabelsMap);
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelCombinationStats.text",
                    JSONObject.toJSONString(labelCombinationStats));

            // 写入图片文件大小统计
            long totalSizeBytes = fileSizes.values().stream().mapToLong(Long::longValue).sum();
            Map<String, Object> sizeStats = new HashMap<>();
            sizeStats.put("totalSizeBytes", totalSizeBytes);
            sizeStats.put("totalSizeMB", String.format("%.2f", totalSizeBytes / (1024.0 * 1024.0)));
            sizeStats.put("totalSizeGB", String.format("%.2f", totalSizeBytes / (1024.0 * 1024.0 * 1024.0)));
            sizeStats.put("averageSizePerFileBytes", fileSizes.isEmpty() ? 0 : totalSizeBytes / fileSizes.size());
            sizeStats.put("fileCount", fileSizes.size());
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/imageSizeStats.text",
                    JSONObject.toJSONString(sizeStats));

            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/annoFileCount.text", String.valueOf(sourceFiles.size()));

            LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-标签统计结果: 总标签数=" + labelCountResult.size() +
                    ", 标签出现次数统计=" + labelCountResult +
                    ", 标签文件数统计=" + labelFileCountResult +
                    ", 图片总大小=" + String.format("%.2f MB", totalSizeBytes / (1024.0 * 1024.0)) +
                    ", 成功获取大小的文件数=" + fileSizes.size());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-标签统计文件写入异常", e);
        }
        long countEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-标签统计文件写入完成，耗时: " + (countEnd - countStart) + "ms");


        long totalEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-全部处理完成，总耗时: " + (totalEnd - startTime) + "ms");
    }

    /**
     * 批量处理文件 - 核心优化：一次读取，三种处理
     */
    private void processBatchOptimized(List<FileAnnotationBO> batch, String targetDir,
                                       Map<Long, Long> labelMappings, Map<Long, String> targetLabelMaps,
                                       Map<Long, Integer> labelToIndexMap, AtomicInteger processedCount,
                                       Task task, String publishTaskKey, Integer originFinished, int totalFiles,
                                       ConcurrentHashMap<String, LongAdder> labelCounts,
                                       ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> labelFileMap,
                                       ConcurrentHashMap<String, Set<String>> fileLabelsMap) {

        for (FileAnnotationBO sourceFile : batch) {
            try {
                // 关键优化：只读取一次文件
                String jsonStr = minioUtil.readString(bucketName, sourceFile.getAnnotationUrl());
                // 三种处理，但只读取一次
                processTS(jsonStr, sourceFile, targetDir, labelMappings, targetLabelMaps, labelCounts, labelFileMap, fileLabelsMap);
                processYOLO(jsonStr, sourceFile, targetDir, labelMappings, labelToIndexMap);
                updateOriginalFile(jsonStr, sourceFile, labelMappings);

                // 更新进度 (降低Redis更新频率)
                int processed = processedCount.incrementAndGet();
                if (processed % 100 == 0 || processed == totalFiles) { // 每100个文件或最后一个文件更新一次
                    synchronized (task) {
                        int newFinished = Math.min(originFinished + processed, originFinished + totalFiles);
                        task.setFinished(newFinished);
                        redisUtils.set(publishTaskKey, JSONObject.toJSONString(task), 100 * 60);
                    }
                    LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-已处理: " + processed + "/" + totalFiles);
                }

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-处理文件异常: " + sourceFile.getFileName(), e);
            }
        }
    }

    /**
     * 处理TS格式
     */
    private void processTS(String jsonStr, FileAnnotationBO sourceFile, String targetDir,
                           Map<Long, Long> labelMappings, Map<Long, String> targetLabelMaps,
                           ConcurrentHashMap<String, LongAdder> labelCounts,
                           ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> labelFileMap,
                           ConcurrentHashMap<String, Set<String>> fileLabelsMap) {
        try {
            // 完全保持原writeTSAnnotationMinoFile的处理逻辑
            JSONArray jsonArray = JSON.parseArray(jsonStr);

            // 用于记录当前文件中出现的标签（避免重复记录同一文件）
            Set<String> currentFileLabels = new HashSet<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String categoryIdStr = jsonObject.getString("category_id");

                if (NumberUtil.isNumber(categoryIdStr)) {
                    Long sourceCategoryId = Long.parseLong(categoryIdStr);
                    Long targetCategoryId = labelMappings.get(sourceCategoryId);

                    if (targetCategoryId != null) {
                        String labelName = targetLabelMaps.get(targetCategoryId);
                        jsonObject.put("category_id", labelName);

                        if (StringUtils.isNotBlank(labelName)) {
                            // 统计标签总出现次数
                            labelCounts.computeIfAbsent(labelName, k -> new LongAdder()).increment();

                            // 记录当前文件包含的标签
                            currentFileLabels.add(labelName);
                        }
                    } else {
                        jsonArray.remove(i);
                        i--;
                    }
                }
            }

            // 获取文件名（用作文件的唯一标识）
            String fileName = sourceFile.getAnnotationUrl().substring(sourceFile.getAnnotationUrl().lastIndexOf("/") + 1);

            // 为当前文件中出现的每个标签记录文件信息
            for (String labelName : currentFileLabels) {
                labelFileMap.computeIfAbsent(labelName, k -> new ConcurrentHashMap<>()).put(fileName, true);
            }

            // 新增：记录每个文件包含的所有标签（包括空标签情况）
            fileLabelsMap.put(fileName, new HashSet<>(currentFileLabels));

            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/" + fileName, JSON.toJSONString(jsonArray));

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-TS处理异常", e);
        }
    }

    /**
     * 处理YOLO格式 - 完全保持原有逻辑
     */
    private void processYOLO(String jsonStr, FileAnnotationBO sourceFile, String targetDir,
                                               Map<Long, Long> labelMappings, Map<Long, Integer> labelToIndexMap) {
        try {
            // 完全保持原writeYOLOAnnotationMinoFileNew的处理逻辑
            StringBuilder annotations = new StringBuilder();
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Integer categoryIndex = null;
                String categoryIdStr = jsonObject.getString("category_id");

                if (NumberUtil.isNumber(categoryIdStr)) {
                    Long categoryId = Long.parseLong(categoryIdStr);
                    if (labelMappings.containsKey(categoryId)) {
                        Long mappedCategoryId = labelMappings.get(categoryId);
                        if (labelToIndexMap.containsKey(mappedCategoryId)) {
                            categoryIndex = labelToIndexMap.get(mappedCategoryId);
                        }
                    }
                }

                if (categoryIndex == null) {
                    continue;
                }

                JSONArray bboxArray = (JSONArray) jsonObject.get("bbox");
                String annotation = ConversionUtil.buildYoloAnnotation(categoryIndex, bboxArray, sourceFile.getFileWidth(), sourceFile.getFileHeight());
                annotations.append(annotation);
            }

            minioUtil.writeString(bucketName, targetDir + "YOLO/annotations/" + sourceFile.getFileName() + ".txt", annotations.toString());

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-YOLO处理异常", e);
        }
    }

    /**
     * 更新原文件分类信息
     */
    private void updateOriginalFile(String jsonStr, FileAnnotationBO sourceFile,
                                                      Map<Long, Long> labelMappings) {
        try {
            // 完全保持原updateCategoryAndWriteToMinIO的处理逻辑
            JSONArray jsonArray = JSON.parseArray(jsonStr);

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String categoryIdStr = jsonObject.getString("category_id");

                if (NumberUtil.isNumber(categoryIdStr)) {
                    Long sourceCategoryId = Long.parseLong(categoryIdStr);
                    Long targetCategoryId = labelMappings.get(sourceCategoryId);

                    if (targetCategoryId != null) {
                        jsonObject.put("category_id", targetCategoryId);
                    } else {
                        jsonArray.remove(i);
                        i--;
                    }
                }
            }

            minioUtil.writeString(bucketName, sourceFile.getAnnotationUrl(), JSON.toJSONString(jsonArray));

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-原文件更新异常", e);
        }
    }

    private void wirteMinoAnnotationFile(DatasetVersion version, Dataset dataset, List<FileAnnotationBO> sourceFiles, String targetDir) {
        if (CollectionUtils.isEmpty(sourceFiles)) {
            return;
        }
        //获取数据集标签信息
        List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(dataset.getId());
        Map<Long, String> labelMaps = datasetLabels.stream().collect(Collectors.toMap(Label::getId, Label::getName));
        List<String> sourceFileUrls = sourceFiles.stream()
                .map(FileAnnotationBO::getAnnotationUrl)
                .collect(Collectors.toList());

        Long datasetId = dataset.getId();
        String versionName = version.getVersionName();
        Map<Long,Long> labelMappings = LabelMapping.toMap(labelMappingService.findByDatasetVersionId(version.getId()));
        //由于页面标注信息读取只支持TS格式，生成TS之外的其他格式数据集版本时，要同时生成TS格式数据集版本
        if (version.getFormat().equals("TS")) {
            writeTSAnnotationMinoFile(labelMaps, sourceFileUrls, targetDir, datasetLabels, datasetId, versionName,labelMappings);
        } else if (version.getFormat().equals("COCO")) {
            writeTSAnnotationMinoFile(labelMaps, sourceFileUrls, targetDir, datasetLabels, datasetId, versionName,labelMappings);
            writeCOCOAnnotationMinoFile(labelMaps, sourceFiles, targetDir + "CreateML/");
            // 还需要生成 单个标注文件到CreateML文件夹
            writeTSAnnotationMinoFile(labelMaps, sourceFileUrls, targetDir + "CreateML/", datasetLabels, datasetId, versionName,labelMappings);
        } else if (version.getFormat().equals("YOLO")) {

//            writeYOLOAnnotationMinoFile(datasetLabels, sourceFiles, targetDir + "YOLO/");

//            writeTSAnnotationMinoFile(labelMaps, sourceFileUrls, targetDir, datasetLabels, datasetId, versionName,labelMappings);
//            writeYOLOAnnotationMinoFileNew(datasetLabels, sourceFiles, targetDir + "YOLO/",labelMappings);
//            updateCategoryAndWriteToMinIO(sourceFiles,labelMappings);
            long yoloStart = System.currentTimeMillis();
            LogUtil.info(LogEnum.BIZ_DATASET, "写入MinIO标注文件-开始处理YOLO格式(优化版本)");

            // 使用优化版本，一次读取生成所有格式
            writeAllAnnotationFormatsOptimized(version, dataset, sourceFiles, targetDir,
                    labelMaps, datasetLabels, datasetId, versionName, labelMappings);

            long yoloEnd = System.currentTimeMillis();
            LogUtil.info(LogEnum.BIZ_DATASET, "写入MinIO标注文件-YOLO格式全部处理完成(优化版本)，总耗时: " + (yoloEnd - yoloStart) + "ms");

        } else if (version.getFormat().equals("VOC")) {
            writeTSAnnotationMinoFile(labelMaps, sourceFileUrls, targetDir, datasetLabels, datasetId, versionName,labelMappings);
            writeVOCAnnotationMinoFile(labelMaps, sourceFiles, targetDir + "VOC/");
        } else if (version.getFormat().equals("Segment-YOLO")) {
            long segmentYoloStart = System.currentTimeMillis();
            LogUtil.info(LogEnum.BIZ_DATASET, "写入MinIO标注文件-开始处理Segment-YOLO格式(优化版本)");

            // 使用优化版本，一次读取生成所有格式（语义分割）
            writeAllSegmentAnnotationFormatsOptimized(version, dataset, sourceFiles, targetDir,
                    labelMaps, datasetLabels, datasetId, versionName, labelMappings);

            long segmentYoloEnd = System.currentTimeMillis();
            LogUtil.info(LogEnum.BIZ_DATASET, "写入MinIO标注文件-Segment-YOLO格式全部处理完成(优化版本)，总耗时: " + (segmentYoloEnd - segmentYoloStart) + "ms");
        }

    }
    /**
     * 发布版本时，生成VOC格式标注文件
     *
     * @param labelMaps   标签信息
     * @param sourceFiles 需要复制的源文件
     * @param targetDir   复制后文件保存地址
     */
    public void writeVOCAnnotationMinoFile(Map<Long, String> labelMaps, List<FileAnnotationBO> sourceFiles, String targetDir) {

        //组装图片和标注数据
        sourceFiles.stream().forEach(sourceFile -> {
            try {
                // 获取json格式的标注信息
                String jsonStr = minioUtil.readString(bucketName, sourceFile.getAnnotationUrl());

                // 构建并存储
                minioUtil.writeString(bucketName, targetDir + "annotations/" + sourceFile.getFileName() + ".xml", ConversionUtil.buildVOCAnnotation(jsonStr, labelMaps, sourceFile));
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file voc annotation exception, {}", e);
            }
        });

    }

    /**
     * 发布版本时，生成YOLO格式标注文件
     *
     * @param labels      标签信息
     * @param sourceFiles 需要复制的源文件
     * @param targetDir   复制后文件保存地址
     */
    public void writeYOLOAnnotationMinoFile(List<Label> labels, List<FileAnnotationBO> sourceFiles, String targetDir) {

        List<Long> categoryIds = Lists.newArrayList();
        conversionUtil.writeYOLOCommon(targetDir, labels, categoryIds);

        StringBuilder train = new StringBuilder();
        //组装图片和标注数据
        sourceFiles.stream().forEach(sourceFile -> {
            //组装 train.txt
            String fileName = StringUtils.substringAfterLast(sourceFile.getFileUrl(), "/");
            train.append("data/obj_train_data/").append(fileName).append("\n");
            try {
                String jsonStr = minioUtil.readString(bucketName, sourceFile.getAnnotationUrl());

                StringBuilder annotations = new StringBuilder();
                JSONArray jsonArray = JSON.parseArray(jsonStr);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Integer categoryIndex = null;
                    String categoryIdStr = jsonObject.getString("category_id");
                    if (NumberUtil.isNumber(categoryIdStr)) {
                        Long categoryId = Long.parseLong(categoryIdStr);
                        if (categoryIds.contains(categoryId)) {
                            categoryIndex = categoryIds.indexOf(categoryId);
                        }
                    }
                    if (categoryIndex == null) {
                        continue;
                    }
                    JSONArray bboxArray = (JSONArray) jsonObject.get("bbox");
                    String annotation = ConversionUtil.buildYoloAnnotation(categoryIndex, bboxArray, sourceFile.getFileWidth(), sourceFile.getFileHeight());
                    annotations.append(annotation);
                }
                minioUtil.writeString(bucketName, targetDir + "obj_train_data/" + sourceFile.getFileName() + ".txt", annotations.toString());
                minioUtil.writeString(bucketName, targetDir + "annotations/" + sourceFile.getFileName() + ".txt", annotations.toString());
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file yolo annotation exception, {}", e);
            }
        });


        try {
            minioUtil.writeString(bucketName, targetDir + "train.txt", train.toString());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file write  yolo train.txt exception, {}", e);
        }
    }


    /**
     * 发布版本时，生成YOLO格式标注文件(New)
     *
     * @param labels      标签信息
     * @param sourceFiles 需要复制的源文件
     * @param targetDir   复制后文件保存地址
     */
    public void writeYOLOAnnotationMinoFileNew(List<Label> labels, List<FileAnnotationBO> sourceFiles, String targetDir, Map<Long, Long> labelMappings) {

        List<Long> categoryIds = Lists.newArrayList();
        conversionUtil.writeYOLOCommonNew(targetDir, labels, categoryIds, labelMappings);


        // 创建一个标签到索引的映射
        Map<Long, Integer> labelToIndexMap = new HashMap<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            labelToIndexMap.put(categoryIds.get(i), i);
        }


        // 处理每个图片的标注
        sourceFiles.stream().forEach(sourceFile -> {
            try {
                String jsonStr = minioUtil.readString(bucketName, sourceFile.getAnnotationUrl());

                StringBuilder annotations = new StringBuilder();
                JSONArray jsonArray = JSON.parseArray(jsonStr);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Integer categoryIndex = null;
                    String categoryIdStr = jsonObject.getString("category_id");

                    // 根据映射关系获取新的类别索引
                    if (NumberUtil.isNumber(categoryIdStr)) {
                        Long categoryId = Long.parseLong(categoryIdStr);
                        if (labelMappings.containsKey(categoryId)) {
                            Long mappedCategoryId = labelMappings.get(categoryId);
                            if (labelToIndexMap.containsKey(mappedCategoryId)) {
                                categoryIndex = labelToIndexMap.get(mappedCategoryId);
                            }
                        }
                    }

                    // 如果类别没有对应的映射，则跳过
                    if (categoryIndex == null) {
                        continue;
                    }

                    JSONArray bboxArray = (JSONArray) jsonObject.get("bbox");
                    String annotation = ConversionUtil.buildYoloAnnotation(categoryIndex, bboxArray, sourceFile.getFileWidth(), sourceFile.getFileHeight());
                    annotations.append(annotation);
                }

                minioUtil.writeString(bucketName, targetDir + "annotations/" + sourceFile.getFileName() + ".txt", annotations.toString());
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO文件YOLO标注异常, {}", e);
            }
        });
    }



    /**
     * 发布版本时，生成coco格式标注文件
     *
     * @param labelMaps   标签信息
     * @param sourceFiles 需要复制的源文件
     * @param targetDir   复制后文件保存地址
     */
    public void writeCOCOAnnotationMinoFile(Map<Long, String> labelMaps, List<FileAnnotationBO> sourceFiles, String targetDir) {
        JSONObject cocoObject = ConversionUtil.buildCOCOCommon();
        JSONArray imageArray = new JSONArray();
        JSONArray annotationArray = new JSONArray();

        Set<Long> categoryIdSet = new HashSet<>();
        int annotationIndex = 0;
        //组装图片和标注数据
        for (FileAnnotationBO sourceFile : sourceFiles) {
            //组装 image
            JSONObject image = buildImageObject(sourceFile);
            imageArray.add(image);
            try {
                String jsonStr = minioUtil.readString(bucketName, sourceFile.getAnnotationUrl());
                JSONArray jsonArray = JSON.parseArray(jsonStr);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject sourceAnnotationObject = jsonArray.getJSONObject(i);
                    JSONObject annotationObject = buildAnnotationObject(sourceFile.getFileId(), sourceAnnotationObject);
                    annotationObject.put("id", annotationIndex);
                    categoryIdSet.add(Long.valueOf(annotationObject.get("category_id").toString()));
                    annotationArray.add(annotationObject);
                    annotationIndex++;
                }
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file read exception, {}", e);
            }
        }

        cocoObject.put("images", imageArray);
        cocoObject.put("annotations", annotationArray);

        //组装标签
        JSONArray categoryArray = buildCategoryArray(labelMaps, categoryIdSet);
        cocoObject.put("categories", categoryArray);

        try {
            minioUtil.writeString(bucketName, targetDir + "annotations/coco_info.json", JSON.toJSONString(cocoObject));
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file write exception, {}", e);
        }
    }

    private JSONObject buildAnnotationObject(Long fileId, JSONObject sourceAnnotationObject) {
        JSONObject annotation = new JSONObject();
        annotation.put("image_id", fileId);
        annotation.put("category_id", sourceAnnotationObject.get("category_id"));
        annotation.put("segmentation", new JSONArray());
        JSONArray bbox = sourceAnnotationObject.getJSONArray("bbox");
        double w, h, area;
        if (bbox != null) {
            w = bbox.getDouble(2);
            h = bbox.getDouble(3);
            area = w * h;
        } else {
            area = 0;
        }
        annotation.put("area", area);
        annotation.put("bbox", sourceAnnotationObject.get("bbox"));
        annotation.put("iscrowd", 0);

        return annotation;
    }


    private JSONArray buildCategoryArray(Map<Long, String> labelMaps, Set<Long> categoryIdSet) {
        JSONArray categoryArray = new JSONArray();
        for (Long categoryId : categoryIdSet) {
            JSONObject category = new JSONObject();
            category.put("id", categoryId);
            category.put("name", labelMaps.get(categoryId));
            category.put("supercategory", "");
            categoryArray.add(category);
        }
        return categoryArray;
    }

    private JSONObject buildImageObject(FileAnnotationBO fileAnnotationBO) {
        JSONObject image = new JSONObject();
        image.put("id", fileAnnotationBO.getFileId());
        image.put("license", 0);
        String fileName = StringUtils.substringAfterLast(fileAnnotationBO.getFileUrl(), "/");
        image.put("file_name", fileName);
        image.put("coco_url", "");
        image.put("height", fileAnnotationBO.getFileHeight());
        image.put("width", fileAnnotationBO.getFileWidth());
        image.put("date_captured", "");
        image.put("flickr_url", "");
        return image;
    }


    /**
     * 发布版本时，生成TS格式标注文件（原始版本 + 计时）
     *
     * @param labelMaps   标签信息
     * @param sourceFiles 需要复制的源文件
     * @param targetDir   复制后文件保存地址
     */
    /**
     * 发布版本时，生成TS格式标注文件（原始版本 + 计时）
     *
     * @param labelMaps   标签信息
     * @param sourceFiles 需要复制的源文件
     * @param targetDir   复制后文件保存地址
     */
    public void writeTSAnnotationMinoFile(Map<Long, String> labelMaps, List<String> sourceFiles, String targetDir, List<Label> datasetLabels, Long datasetID, String versionName, Map<Long, Long> labelMappings) {

        /**
         * 1.读取源文件
         * 2.替换其中category_id为标签名称
         * 3.把新文件内容写入到新文件中
         */
        String publishTaskKey = "publishTask:" + datasetID + ":" + versionName;
        String taskJson = (String) redisUtils.get(publishTaskKey);
        Task task = JSON.parseObject(taskJson, Task.class);
        Integer originFinished = task.getFinished();

        Map<Long, String> AllTargetLabelMaps = new HashMap<>();
        datasetLabels.forEach(label -> {
            AllTargetLabelMaps.put(label.getId(), label.getName());
        });

        //构建目标label的id与name映射
        Map<Long, String> targetLabelMaps = new HashMap<>();
        labelMappings.forEach((sourceCategoryId, targetCategoryId) -> {
            if (targetCategoryId != null) {
                String labelName = labelServiceImpl.findLabelByIds(Collections.singletonList(targetCategoryId)).get(0).getName();
                targetLabelMaps.putIfAbsent(targetCategoryId, labelName); // 如果targetCategoryId已经存在，则不做覆盖
            }
        });




        // 计算每次增加的数量，假设我们每10个文件增加一次finished数量
        final int batchSize = 10;
        final int totalFiles = sourceFiles.size();
        AtomicInteger finishedCount = new AtomicInteger(0);

        sourceFiles.stream().forEach(annotationUrl -> {
            try {
                String jsonStr = minioUtil.readString(bucketName, annotationUrl);
                JSONArray jsonArray = JSON.parseArray(jsonStr);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String categoryIdStr = jsonObject.getString("category_id");

                    if (NumberUtil.isNumber(categoryIdStr)) {
                        Long sourceCategoryId = Long.parseLong(categoryIdStr);
                        Long targetCategoryId = labelMappings.get(sourceCategoryId); // 获取映射后的标签ID

                        if (targetCategoryId != null) {
                            // 如果映射关系存在，获取标签名称
                            String labelName = targetLabelMaps.get(targetCategoryId);
                            jsonObject.put("category_id", labelName); // 更新为标签名称
                        } else {
                            // 如果映射为空，则删除该标注框
                            jsonArray.remove(i);
                            i--;  // 由于移除元素，当前索引需要减一
                        }
                    }
                }

                // 将更新后的文件内容写入MinIO
                minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/" + annotationUrl.substring(annotationUrl.lastIndexOf("/") + 1), JSON.toJSONString(jsonArray));

                // 更新finished的数量，控制每隔batchSize个文件更新一次
                int filesProcessed = finishedCount.incrementAndGet(); // 增加已处理文件数量
                if (filesProcessed % batchSize == 0 || filesProcessed == totalFiles) {
                    task.setFinished(task.getFinished() + batchSize <= totalFiles ? task.getFinished() + batchSize : originFinished + totalFiles);
                    redisUtils.set(publishTaskKey, JSONObject.toJSONString(task), 100 * 60); // 更新Redis中的task
                }

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file write exception, {}", e);
            }
        });

        // 保存标签信息到标注文件夹中
        // 从targetLabelMaps获取所有的labelName
        List<String> labelStr = new ArrayList<>(targetLabelMaps.values());
        try {
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labels.text", Strings.join(labelStr, ','));
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelsIds.text", JSONObject.toJSONString(AllTargetLabelMaps));
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file write exception, {}", e);
        }

    }



    private void updateCategoryAndWriteToMinIO(List<FileAnnotationBO> sourceFiles, Map<Long, Long> labelMappings) {
        sourceFiles.forEach(annotation -> {
            try {
                String annotationUrl =  annotation.getAnnotationUrl();
                // 1. 从MinIO读取源文件
                String jsonStr = minioUtil.readString(bucketName, annotationUrl);
                JSONArray jsonArray = JSON.parseArray(jsonStr);

                // 2. 遍历每个标注框，更新category_id
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String categoryIdStr = jsonObject.getString("category_id");

                    if (NumberUtil.isNumber(categoryIdStr)) {
                        Long sourceCategoryId = Long.parseLong(categoryIdStr);
                        Long targetCategoryId = labelMappings.get(sourceCategoryId); // 获取映射后的标签ID

                        if (targetCategoryId != null) {
                            // 更新源category_id为目标category_id
                            jsonObject.put("category_id", targetCategoryId); // 更新为目标ID
                        } else {
                            // 如果映射为空，则删除该标注框
                            jsonArray.remove(i);
                            i--;  // 由于移除元素，当前索引需要减一
                        }
                    }
                }

                // 3. 将更新后的文件内容写回MinIO，直接覆盖原文件
                minioUtil.writeString(bucketName, annotationUrl, JSON.toJSONString(jsonArray));

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file write exception, {}", e);
            }
        });
    }




    /**
     * 语义分割YOLO格式优化版本：减少重复IO，增加并行处理
     * 参考 writeAllAnnotationFormatsOptimized 方法，但处理 segmentation 字段
     */
    public void writeAllSegmentAnnotationFormatsOptimized(DatasetVersion version, Dataset dataset,
                                                          List<FileAnnotationBO> sourceFiles, String targetDir,
                                                          Map<Long, String> labelMaps, List<Label> datasetLabels,
                                                          Long datasetID, String versionName, Map<Long, Long> labelMappings) {
        long startTime = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-开始处理Segment-YOLO格式，文件数量: " + sourceFiles.size());
        if (CollectionUtils.isEmpty(sourceFiles)) {
            return;
        }

        // 线程安全的 labelName 计数器
        ConcurrentHashMap<String, LongAdder> labelCounts = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> labelFileMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Set<String>> fileLabelsMap = new ConcurrentHashMap<>();

        // 1. 预处理
        long prepStart = System.currentTimeMillis();

        Map<Long, String> AllTargetLabelMaps = new HashMap<>();
        datasetLabels.forEach(label -> {
            AllTargetLabelMaps.put(label.getId(), label.getName());
        });

        Map<Long, String> targetLabelMaps = new HashMap<>();
        labelMappings.forEach((sourceCategoryId, targetCategoryId) -> {
            if (targetCategoryId != null) {
                String labelName = labelServiceImpl.findLabelByIds(Collections.singletonList(targetCategoryId)).get(0).getName();
                targetLabelMaps.putIfAbsent(targetCategoryId, labelName);
            }
        });

        // Segment-YOLO格式预处理 - 建立标签映射
        List<Long> categoryIds = Lists.newArrayList();
        conversionUtil.writeYOLOCommonNew(targetDir + "Segment-YOLO/", datasetLabels, categoryIds, labelMappings);

        Map<Long, Integer> labelToIndexMap = new HashMap<>();
        for (int i = 0; i < categoryIds.size(); i++) {
            labelToIndexMap.put(categoryIds.get(i), i);
        }

        long prepEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO预处理完成，耗时: " + (prepEnd - prepStart) + "ms");

        // 2. 批量获取文件大小
        long sizeStart = System.currentTimeMillis();
        Map<String, Long> fileSizes = new HashMap<>();
        try {
            Map<String, String> bucketAndObjectNames = new HashMap<>();
            for (FileAnnotationBO sourceFile : sourceFiles) {
                String fileUrl = sourceFile.getFileUrl();
                String objectName;
                if (fileUrl.startsWith(bucketName + "/")) {
                    objectName = fileUrl.substring(bucketName.length() + 1);
                } else {
                    objectName = fileUrl;
                }
                bucketAndObjectNames.put(objectName, bucketName);
            }
            fileSizes = minioUtil.getFilesSizes(bucketAndObjectNames);
            LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-批量获取文件大小完成，获取到 " + fileSizes.size() + " 个文件大小");
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-批量获取文件大小失败", e);
        }
        long sizeEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-文件大小获取完成，耗时: " + (sizeEnd - sizeStart) + "ms");

        // 3. Redis任务初始化
        String publishTaskKey = "publishTask:" + datasetID + ":" + versionName;
        String taskJson = (String) redisUtils.get(publishTaskKey);
        Task task = JSON.parseObject(taskJson, Task.class);
        Integer originFinished = task.getFinished();
        AtomicInteger processedCount = new AtomicInteger(0);

        // 4. 并行处理
        int threadCount = Math.min(8, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-使用 " + threadCount + " 个线程并行处理");

        long processStart = System.currentTimeMillis();

        try {
            int batchSize = 50;
            List<List<FileAnnotationBO>> batches = new ArrayList<>();
            for (int i = 0; i < sourceFiles.size(); i += batchSize) {
                int end = Math.min(i + batchSize, sourceFiles.size());
                batches.add(sourceFiles.subList(i, end));
            }

            List<Future<Void>> futures = new ArrayList<>();

            for (List<FileAnnotationBO> batch : batches) {
                Future<Void> future = executor.submit(() -> {
                    processSegmentBatchOptimized(batch, targetDir, labelMappings, targetLabelMaps,
                            labelToIndexMap, processedCount, task, publishTaskKey,
                            originFinished, sourceFiles.size(), labelCounts, labelFileMap, fileLabelsMap);
                    return null;
                });
                futures.add(future);
            }

            for (Future<Void> future : futures) {
                future.get();
            }

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO并行处理异常", e);
        } finally {
            executor.shutdown();
        }

        long processEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO文件处理完成，耗时: " + (processEnd - processStart) + "ms");

        // 5. 写入标签文件
        long labelStart = System.currentTimeMillis();
        try {
            List<String> labelStr = new ArrayList<>(targetLabelMaps.values());
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labels.text", Strings.join(labelStr, ','));
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelsIds.text", JSONObject.toJSONString(AllTargetLabelMaps));
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-标签文件写入异常", e);
        }
        long labelEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-标签文件写入完成，耗时: " + (labelEnd - labelStart) + "ms");

        // 6. 写入统计文件
        long countStart = System.currentTimeMillis();
        try {
            Map<String, Long> labelCountResult = labelCounts.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().longValue()
                    ));
            
            Map<String, Integer> labelFileCountResult = labelFileMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().size()
                    ));
            
            Map<String, Integer> labelCombinationStats = new HashMap<>();
            for (Set<String> fileLabels : fileLabelsMap.values()) {
                String labelCombination;
                if (fileLabels == null || fileLabels.isEmpty()) {
                    labelCombination = "";
                } else {
                    labelCombination = fileLabels.stream()
                            .sorted()
                            .collect(Collectors.joining(","));
                }
                labelCombinationStats.put(labelCombination,
                        labelCombinationStats.getOrDefault(labelCombination, 0) + 1);
            }

            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelsCount.text",
                    JSONObject.toJSONString(labelCountResult));
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelsFileCount.text",
                    JSONObject.toJSONString(labelFileCountResult));
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/labelCombinationStats.text",
                    JSONObject.toJSONString(labelCombinationStats));

            long totalSizeBytes = fileSizes.values().stream().mapToLong(Long::longValue).sum();
            Map<String, Object> sizeStats = new HashMap<>();
            sizeStats.put("totalSizeBytes", totalSizeBytes);
            sizeStats.put("totalSizeMB", String.format("%.2f", totalSizeBytes / (1024.0 * 1024.0)));
            sizeStats.put("totalSizeGB", String.format("%.2f", totalSizeBytes / (1024.0 * 1024.0 * 1024.0)));
            sizeStats.put("averageSizePerFileBytes", fileSizes.isEmpty() ? 0 : totalSizeBytes / fileSizes.size());
            sizeStats.put("fileCount", fileSizes.size());
            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/imageSizeStats.text",
                    JSONObject.toJSONString(sizeStats));

            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/annoFileCount.text", String.valueOf(sourceFiles.size()));

            LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO标签统计结果: 总标签数=" + labelCountResult.size() +
                    ", 标签出现次数统计=" + labelCountResult +
                    ", 标签文件数统计=" + labelFileCountResult +
                    ", 图片总大小=" + String.format("%.2f MB", totalSizeBytes / (1024.0 * 1024.0)) +
                    ", 成功获取大小的文件数=" + fileSizes.size());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO标签统计文件写入异常", e);
        }
        long countEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO标签统计文件写入完成，耗时: " + (countEnd - countStart) + "ms");

        long totalEnd = System.currentTimeMillis();
        LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO全部处理完成，总耗时: " + (totalEnd - startTime) + "ms");
    }

    /**
     * 批量处理语义分割文件 - 核心优化：一次读取，三种处理（TS、Segment-YOLO、原文件更新）
     */
    private void processSegmentBatchOptimized(List<FileAnnotationBO> batch, String targetDir,
                                              Map<Long, Long> labelMappings, Map<Long, String> targetLabelMaps,
                                              Map<Long, Integer> labelToIndexMap, AtomicInteger processedCount,
                                              Task task, String publishTaskKey, Integer originFinished, int totalFiles,
                                              ConcurrentHashMap<String, LongAdder> labelCounts,
                                              ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> labelFileMap,
                                              ConcurrentHashMap<String, Set<String>> fileLabelsMap) {

        for (FileAnnotationBO sourceFile : batch) {
            try {
                // 关键优化：只读取一次文件
                String jsonStr = minioUtil.readString(bucketName, sourceFile.getAnnotationUrl());
                
                // 三种处理，但只读取一次
                processSegmentTS(jsonStr, sourceFile, targetDir, labelMappings, targetLabelMaps, labelCounts, labelFileMap, fileLabelsMap);
                processSegmentYOLO(jsonStr, sourceFile, targetDir, labelMappings, labelToIndexMap);
                updateSegmentCategoryAndWriteToMinIO(jsonStr, sourceFile, labelMappings);

                // 更新进度
                int processed = processedCount.incrementAndGet();
                if (processed % 100 == 0 || processed == totalFiles) {
                    synchronized (task) {
                        int newFinished = Math.min(originFinished + processed, originFinished + totalFiles);
                        task.setFinished(newFinished);
                        redisUtils.set(publishTaskKey, JSONObject.toJSONString(task), 100 * 60);
                    }
                    LogUtil.info(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO已处理: " + processed + "/" + totalFiles);
                }

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO处理文件异常: " + sourceFile.getFileName(), e);
            }
        }
    }

    /**
     * 处理TS格式（语义分割）
     */
    private void processSegmentTS(String jsonStr, FileAnnotationBO sourceFile, String targetDir,
                                  Map<Long, Long> labelMappings, Map<Long, String> targetLabelMaps,
                                  ConcurrentHashMap<String, LongAdder> labelCounts,
                                  ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> labelFileMap,
                                  ConcurrentHashMap<String, Set<String>> fileLabelsMap) {
        try {
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            Set<String> currentFileLabels = new HashSet<>();

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String categoryIdStr = jsonObject.getString("category_id");

                if (NumberUtil.isNumber(categoryIdStr)) {
                    Long sourceCategoryId = Long.parseLong(categoryIdStr);
                    Long targetCategoryId = labelMappings.get(sourceCategoryId);

                    if (targetCategoryId != null) {
                        String labelName = targetLabelMaps.get(targetCategoryId);
                        jsonObject.put("category_id", labelName);

                        if (StringUtils.isNotBlank(labelName)) {
                            labelCounts.computeIfAbsent(labelName, k -> new LongAdder()).increment();
                            currentFileLabels.add(labelName);
                        }
                    } else {
                        jsonArray.remove(i);
                        i--;
                    }
                }
            }

            String fileName = sourceFile.getAnnotationUrl().substring(sourceFile.getAnnotationUrl().lastIndexOf("/") + 1);
            for (String labelName : currentFileLabels) {
                labelFileMap.computeIfAbsent(labelName, k -> new ConcurrentHashMap<>()).put(fileName, true);
            }
            fileLabelsMap.put(fileName, new HashSet<>(currentFileLabels));

            minioUtil.writeString(bucketName, targetDir + ANNOTATION + "/" + fileName, JSON.toJSONString(jsonArray));

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-Segment-TS处理异常", e);
        }
    }

    /**
     * 处理Segment-YOLO格式 - 将segmentation点坐标转换为YOLO格式
     */
    private void processSegmentYOLO(String jsonStr, FileAnnotationBO sourceFile, String targetDir,
                                    Map<Long, Long> labelMappings, Map<Long, Integer> labelToIndexMap) {
        try {
            StringBuilder annotations = new StringBuilder();
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            
            int imgWidth = sourceFile.getFileWidth();
            int imgHeight = sourceFile.getFileHeight();
            
            if (imgWidth <= 0 || imgHeight <= 0) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "图片{}尺寸信息无效，跳过Segment-YOLO处理", sourceFile.getFileName());
                return;
            }
            
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Integer categoryIndex = null;
                String categoryIdStr = jsonObject.getString("category_id");

                if (NumberUtil.isNumber(categoryIdStr)) {
                    Long categoryId = Long.parseLong(categoryIdStr);
                    if (labelMappings.containsKey(categoryId)) {
                        Long mappedCategoryId = labelMappings.get(categoryId);
                        if (labelToIndexMap.containsKey(mappedCategoryId)) {
                            categoryIndex = labelToIndexMap.get(mappedCategoryId);
                        }
                    }
                }

                if (categoryIndex == null) {
                    continue;
                }

                // 获取segmentation字段
                JSONArray segmentation = jsonObject.getJSONArray("segmentation");
                if (segmentation == null || segmentation.size() < 3) { // 至少3个点
                    continue;
                }

                annotations.append(categoryIndex);
                
                // 遍历所有点进行归一化
                for (int j = 0; j < segmentation.size(); j++) {
                    JSONArray point = segmentation.getJSONArray(j);
                    if (point != null && point.size() >= 2) {
                        double x = point.getDouble(0);
                        double y = point.getDouble(1);
                        
                        // 归一化到[0,1]
                        double normalizedX = x / imgWidth;
                        double normalizedY = y / imgHeight;
                        
                        annotations.append(" ").append(normalizedX).append(" ").append(normalizedY);
                    }
                }
                annotations.append("\n");
            }

            minioUtil.writeString(bucketName, targetDir + "Segment-YOLO/annotations/" + sourceFile.getFileName() + ".txt", annotations.toString());

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-Segment-YOLO处理异常", e);
        }
    }

    /**
     * 更新原文件分类信息（语义分割）
     */
    private void updateSegmentCategoryAndWriteToMinIO(String jsonStr, FileAnnotationBO sourceFile,
                                                      Map<Long, Long> labelMappings) {
        try {
            JSONArray jsonArray = JSON.parseArray(jsonStr);

            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String categoryIdStr = jsonObject.getString("category_id");

                if (NumberUtil.isNumber(categoryIdStr)) {
                    Long sourceCategoryId = Long.parseLong(categoryIdStr);
                    Long targetCategoryId = labelMappings.get(sourceCategoryId);

                    if (targetCategoryId != null) {
                        jsonObject.put("category_id", targetCategoryId);
                    } else {
                        jsonArray.remove(i);
                        i--;
                    }
                }
            }

            minioUtil.writeString(bucketName, sourceFile.getAnnotationUrl(), JSON.toJSONString(jsonArray));

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "优化版本-Segment原文件更新异常", e);
        }
    }

    /**
     * 发布版本时，生成Segment-YOLO格式标注文件（语义分割YOLO格式）- 旧版本，已废弃
     *
     * @param labelMaps   标签信息
     * @param sourceFiles 需要复制的源文件URL列表
     * @param targetDir   复制后文件保存地址
     * @param datasetLabels 数据集标签列表
     * @deprecated 使用 writeAllSegmentAnnotationFormatsOptimized 替代
     */
    @Deprecated
    public void writeSegmentAnnotationMinoFile(Map<Long, String> labelMaps, List<String> sourceFiles, String targetDir, List<Label> datasetLabels) {
        /**
         * 语义分割YOLO格式说明：
         * 每行格式: <class_id> <x1> <y1> <x2> <y2> ... <xn> <yn>
         * 其中坐标需要归一化到 [0,1] 范围
         * 
         * 原始标注格式示例:
         * [{"score":1,"category_id":5690,"segmentation":[[111,120.609375],[286,51.609375],[176,227.609375]],"id":"c5613"}]
         */
        
        // 建立标签ID到索引的映射
        Map<Long, Integer> labelIdToIndexMap = new HashMap<>();
        List<Long> sortedLabelIds = labelMaps.keySet().stream().sorted().collect(Collectors.toList());
        for (int i = 0; i < sortedLabelIds.size(); i++) {
            labelIdToIndexMap.put(sortedLabelIds.get(i), i);
        }
        
        // 提取所有文件名用于批量查询图片尺寸
        List<String> fileNames = sourceFiles.stream()
                .map(annotationUrl -> annotationUrl.substring(annotationUrl.lastIndexOf("/") + 1))
                .collect(Collectors.toList());
        
        // 批量查询图片尺寸信息
        Map<String, FileCreateDTO> fileInfoMap = new HashMap<>();
        if (!fileNames.isEmpty()) {
            try {
                // 从标注文件URL推断数据集ID（假设URL格式为 dataset/{datasetId}/annotation/{fileName}）
                String firstUrl = sourceFiles.get(0);
                String[] urlParts = firstUrl.split("/");
                Long datasetId = null;
                for (int i = 0; i < urlParts.length - 1; i++) {
                    if ("dataset".equals(urlParts[i]) && i + 1 < urlParts.length) {
                        try {
                            datasetId = Long.parseLong(urlParts[i + 1]);
                            break;
                        } catch (NumberFormatException e) {
                            // 继续寻找
                        }
                    }
                }
                
                if (datasetId != null) {
                    List<FileCreateDTO> fileInfoList = FileServiceImpl.getBaseMapper().selectWidthAndHeightBatch(fileNames, datasetId);
                    fileInfoMap = fileInfoList.stream()
                            .collect(Collectors.toMap(FileCreateDTO::getName, Function.identity()));
                }
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "批量查询图片尺寸失败", e);
            }
        }
        
        final Map<String, FileCreateDTO> finalFileInfoMap = fileInfoMap;
        
        sourceFiles.stream().forEach(annotationUrl -> {
            try {
                String jsonStr = minioUtil.readString(bucketName, annotationUrl);
                JSONArray jsonArray = JSON.parseArray(jsonStr);
                
                // 从annotationUrl中提取文件名
                String fileName = annotationUrl.substring(annotationUrl.lastIndexOf("/") + 1);
                
                // 获取图片尺寸信息
                FileCreateDTO fileInfo = finalFileInfoMap.get(fileName);
                Integer imgWidth = null;
                Integer imgHeight = null;
                
                if (fileInfo != null) {
                    imgWidth = fileInfo.getWidth();
                    imgHeight = fileInfo.getHeight();
                }
                
                if (imgWidth == null || imgHeight == null || imgWidth <= 0 || imgHeight <= 0) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "文件{}的尺寸信息不可用，跳过处理", fileName);
                    return;
                }
                
                StringBuilder yoloContent = new StringBuilder();
                
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String categoryIdStr = jsonObject.getString("category_id");
                    
                    // 获取分割点坐标
                    JSONArray segmentation = jsonObject.getJSONArray("segmentation");
                    if (segmentation == null || segmentation.isEmpty()) {
                        continue;
                    }
                    
                    // 获取类别索引
                    Integer classIndex = null;
                    if (NumberUtil.isNumber(categoryIdStr)) {
                        Long categoryId = Long.parseLong(categoryIdStr);
                        if (labelIdToIndexMap.containsKey(categoryId)) {
                            classIndex = labelIdToIndexMap.get(categoryId);
                        }
                    }
                    
                    if (classIndex == null) {
                        continue; // 跳过无法识别的类别
                    }
                    
                    // segmentation格式为: [[x1,y1],[x2,y2],[x3,y3],...]
                    // 每个元素是一个二元数组[x, y]
                    if (segmentation.size() < 3) { // 至少需要3个点才能形成多边形
                        continue;
                    }
                    
                    yoloContent.append(classIndex);
                    
                    // 遍历所有点，进行归一化并写入
                    for (int j = 0; j < segmentation.size(); j++) {
                        JSONArray point = segmentation.getJSONArray(j);
                        if (point != null && point.size() >= 2) {
                            double x = point.getDouble(0);
                            double y = point.getDouble(1);
                            
                            // 归一化坐标到 [0,1] 范围
                            double normalizedX = x / imgWidth;
                            double normalizedY = y / imgHeight;
                            
                            yoloContent.append(" ").append(normalizedX).append(" ").append(normalizedY);
                        }
                    }
                    
                    yoloContent.append("\n");
                }
                
                // 写入YOLO格式的.txt文件
                String outputFileName = fileName;
                if (fileName.contains(".")) {
                    outputFileName = fileName.substring(0, fileName.lastIndexOf(".")) + ".txt";
                } else {
                    outputFileName = fileName + ".txt";
                }
                
                minioUtil.writeString(bucketName, targetDir + "annotations/" + outputFileName, yoloContent.toString());
                
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "MinIO file write Segment-YOLO annotation exception, {}", e);
            }
        });
        
        // 写入classes.txt文件
        try {
            List<String> labelNames = sortedLabelIds.stream()
                    .map(labelMaps::get)
                    .collect(Collectors.toList());
            String classesContent = String.join("\n", labelNames);
            minioUtil.writeString(bucketName, targetDir + "classes.txt", classesContent);
            LogUtil.info(LogEnum.BIZ_DATASET, "成功写入Segment-YOLO格式的classes.txt，包含{}个类别", labelNames.size());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "MinIO write classes.txt exception, {}", e);
        }
    }


//    /**
//     * 插入es数据
//     *
//     * @param versionSource 源版本
//     * @param versionTarget 目标版本
//     * @param datasetId     数据集id
//     * @param fileNameMap   文件列表
//     */
//    @Override
//    public void insertEsData(String versionSource, String versionTarget, Long datasetId, Long datasetIdTarget, Map<String, Long> fileNameMap) {
//        SearchRequest searchRequest = new SearchRequest(esIndex);
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery().must(QueryBuilders.termQuery("datasetId", datasetId.toString()))
//                .must(QueryBuilders.matchPhraseQuery("versionName", versionSource));
//        QueryBuilder queryBuilder = boolQueryBuilder;
//        sourceBuilder.query(queryBuilder).size(MagicNumConstant.MILLION * MagicNumConstant.TEN);
//        searchRequest.source(sourceBuilder);
//        try {
//            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//            SearchHit[] hits = searchResponse.getHits().getHits();
//            for (int i = 0; i < hits.length; i++) {
//                EsDataFileDTO esDataFileDTO = JSON.parseObject(hits[i].getSourceAsString(), EsDataFileDTO.class);
//                esDataFileDTO.setVersionName(versionTarget);
//                Map<String, Object> jsonMap = new HashMap<>();
//                jsonMap.put("content", esDataFileDTO.getContent());
//                jsonMap.put("name", esDataFileDTO.getName());
//                jsonMap.put("status", esDataFileDTO.getStatus().toString());
//                jsonMap.put("datasetId", datasetIdTarget.toString());
//                jsonMap.put("createUserId", esDataFileDTO.getCreateUserId() == null ? null : esDataFileDTO.getCreateUserId().toString());
//                jsonMap.put("createTime", esDataFileDTO.getCreateTime() == null ? null : esDataFileDTO.getCreateTime().toString());
//                jsonMap.put("updateUserId", esDataFileDTO.getUpdateUserId() == null ? null : esDataFileDTO.getUpdateUserId().toString());
//                jsonMap.put("updateTime", esDataFileDTO.getUpdateTime() == null ? null : esDataFileDTO.getUpdateTime().toString());
//                jsonMap.put("fileType", esDataFileDTO.getFileType() == null ? null : esDataFileDTO.getFileType().toString());
//                jsonMap.put("enhanceType", esDataFileDTO.getEnhanceType() == null ? null : esDataFileDTO.getEnhanceType().toString());
//                jsonMap.put("originUserId", esDataFileDTO.getOriginUserId().toString());
//                jsonMap.put("prediction", esDataFileDTO.getPrediction() == null ? null : esDataFileDTO.getPrediction().toString());
//                jsonMap.put("labelId", esDataFileDTO.getLabelId() == null ? null : esDataFileDTO.getLabelId());
//                jsonMap.put("annotation", esDataFileDTO.getAnnotation() == null ? null : esDataFileDTO.getAnnotation());
//                jsonMap.put("versionName", versionTarget);
//                IndexRequest request = new IndexRequest(esIndex);
//                request.source(jsonMap);
//                if (fileNameMap != null) {
//                    request.id(fileNameMap.get(esDataFileDTO.getName()).toString());
//                } else {
//                    request.id(hits[i].getId());
//                }
//                bulkProcessor.add(request);
//            }
//            bulkProcessor.flush();
//        } catch (Exception e) {
//            LogUtil.error(LogEnum.BIZ_DATASET, "publish text to es error:{}", e);
//        }
//    }

    /**
     * 查询当前数据集版本的原始文件数量
     *
     * @param datasetId 数据集id
     * @return Integer  原始文件数量
     */
    @Override
    @DataPermissionMethod
    public Integer getSourceFileCount(Long datasetId) {
        Dataset dataset = datasetService.getBaseMapper().selectById(datasetId);
        return datasetVersionFileService.getSourceFileCount(dataset);
    }

    /**
     * 获取数据集版本详情
     *
     * @param datasetVersionId 数据集版本ID
     * @return 数据集版本信息
     */
    @Override
    public DatasetVersion detail(Long datasetVersionId) {
        return baseMapper.selectById(datasetVersionId);
    }

    /**
     * 数据集版本数据更新
     *
     * @param id           数据集版本ID
     * @param sourceStatus 原状态
     * @param targetStatus 目的状态
     */
    @Override
    public void update(Long id, Integer sourceStatus, Integer targetStatus) {
        UpdateWrapper<DatasetVersion> datasetVersionUpdateWrapper = new UpdateWrapper<>();
        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataConversion(targetStatus);
        datasetVersionUpdateWrapper.eq("id", id);
        datasetVersionUpdateWrapper.eq("data_conversion", sourceStatus);
        baseMapper.update(datasetVersion, datasetVersionUpdateWrapper);
    }

    @Override
    public DatasetVersion getDatasetVersionSourceVersion(Dataset dataset) {
        return baseMapper.selectOne(new LambdaQueryWrapper<DatasetVersion>() {{
            eq(DatasetVersion::getDatasetId, dataset.getId());
            eq(DatasetVersion::getVersionName, dataset.getCurrentVersionName());
        }});
    }

    /**
     * 获取数据集版本
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名
     * @return DatasetVersion 数据集版本
     */
    @Override
    public DatasetVersion getVersionByDatasetIdAndVersionName(Long datasetId, String versionName) {
        QueryWrapper<DatasetVersion> datasetVersionQueryWrapper = new QueryWrapper<>();
        datasetVersionQueryWrapper.lambda().eq(DatasetVersion::getDatasetId, datasetId)
                .eq(DatasetVersion::getVersionName, versionName);
        return getBaseMapper().selectOne(datasetVersionQueryWrapper);
    }


    /**
     * 根据数据集ID删除数据信息
     *
     * @param datasetId 数据集ID
     */
    @Override
    public void deleteByDatasetId(Long datasetId) {
        baseMapper.deleteByDatasetId(datasetId);
    }


    /**
     * 备份数据集版本数据
     *
     * @param originDataset      原数据集实体
     * @param targetDateset      目标数据集实体
     * @param currentVersionName 版本名称
     */
    @Override
    public void backupDatasetVersionDataByDatasetId(Dataset originDataset, Dataset targetDateset, String currentVersionName) {
        DatasetVersion datasetVersion = getVersionByDatasetIdAndVersionName(originDataset.getId(), currentVersionName);
        if (!Objects.isNull(datasetVersion)) {
            if (!Objects.isNull(datasetVersion.getVersionUrl())) {
                String url = StringUtils.substringBeforeLast(datasetVersion.getVersionUrl(), SymbolConstant.SLASH)
                        .replace(originDataset.getId().toString(), targetDateset.getId().toString()) +
                        SymbolConstant.SLASH + DEFAULT_VERSION;
                datasetVersion.setVersionUrl(url);
            }
            DatasetVersion version = DatasetVersion.builder()
                    .datasetId(targetDateset.getId())
                    .dataConversion(datasetVersion.getDataConversion())
                    .versionName(DEFAULT_VERSION)
                    .versionUrl(datasetVersion.getVersionUrl())
                    .teamId(datasetVersion.getTeamId())
                    .originUserId(MagicNumConstant.ZERO_LONG)
                    .versionNote(datasetVersion.getVersionNote())
                    .build();
            version.setCreateUserId(targetDateset.getCreateUserId());
            version.setUpdateUserId(version.getCreateUserId());
            baseMapper.insert(version);
        }
    }


    /**
     * 根据数据集ID查询版本名称列表
     *
     * @param datasetId 数据集ID
     * @return 版本名称列表
     */
    @Override
    public List<String> getDatasetVersionNameListByDatasetId(Long datasetId) {
        return baseMapper.getDatasetVersionNameListByDatasetId(datasetId);
    }

    /**
     * 生成ofRecord文件
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名称
     */
    @Override
    public void createOfRecord(Long datasetId, String versionName) {
        List<DatasetVersion> datasetVersionList = baseMapper.findDatasetVersion(datasetId, versionName);
        DatasetVersion datasetVersion = datasetVersionList.get(0);
        if (datasetVersion != null) {
            Task task = Task.builder().total(getBaseMapper().getCountByDatasetVersionId(datasetVersion.getDatasetId()
                            , datasetVersion.getVersionName()))
                    .datasetId(datasetVersion.getDatasetId())
                    .type(DataTaskTypeEnum.OFRECORD.getValue())
                    .labels("")
                    .ofRecordVersion(versionName)
                    .datasetVersionId(datasetVersion.getId()).build();
            taskService.createTask(task);
            datasetVersion.setOfRecord(MagicNumConstant.ONE);
            datasetVersion.setDataConversion(MagicNumConstant.FIVE);
            baseMapper.updateById(datasetVersion);
        }
    }

    /**
     * 生成版本数据
     *
     * @param datasetVersion 版本详情
     */
    @Override
    public void insertOne(DatasetVersion datasetVersion) {
        baseMapper.insert(datasetVersion);
    }

    /**
     * 更新版本
     *
     * @param datasetVersion 数据集版本
     */
    @Override
    public void updateByEntity(DatasetVersion datasetVersion) {
        baseMapper.updateById(datasetVersion);
    }

    /**
     * 更新版本
     *
     * @return 数据集发布情况
     */
    @Override
    public DatasetVersionStatVO getDatasetVersionStat() {

        Integer publishedDatasetNum = baseMapper.selectCount(new QueryWrapper<DatasetVersion>().select("DISTINCT dataset_id").eq("deleted", false));
        Integer allDatasetNum = datasetService.getBaseMapper().selectCount(new LambdaQueryWrapper<Dataset>().eq(Dataset::getDeleted, false));
        Integer unpublishedDatasetNum = allDatasetNum - publishedDatasetNum;
        return DatasetVersionStatVO.builder()
                .allDatasetNum(allDatasetNum)
                .publishedDatasetNum(publishedDatasetNum)
                .unpublishedDatasetNum(unpublishedDatasetNum)
                .build();
    }

    @Override
    public DatasetVersionVO getDatasetVersionById(Long datasetVersionId) {
        try {
            // 查询数据集版本
            DatasetVersion datasetVersion = datasetVersionMapper.getById(datasetVersionId);
            // 查询数据集
            Dataset dataset = datasetService.getOneById(datasetVersion.getDatasetId());
            // 成功查询到数据，返回封装的 VO 对象
            return new DatasetVersionVO(datasetVersion, dataset);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public List<DatasetVersionVO> listByIds(List<Long> ids) {

        List<DatasetVersion> byIds = datasetVersionMapper.findByIds(ids);

        if (byIds == null) {
            return new ArrayList<>();
        }
        return byIds.stream().map(datasetVersion -> {
            Dataset dataset = datasetService.getOneById(datasetVersion.getDatasetId());
            if(dataset!=null){
                return new DatasetVersionVO(datasetVersion, dataset);
            }
            return new DatasetVersionVO();
        }).collect(Collectors.toList());


    }

    /**
     * 按照数据集 id 获取对应得 datasetversion 实体列表
     *
     * @param datasetId
     * @return
     */
    @Override
    public List<DatasetVersionVO> getDatasetVersionList(Long datasetId) {
        LambdaQueryWrapper<DatasetVersion> lqw = new LambdaQueryWrapper<>();
        lqw.eq(datasetId != 0, DatasetVersion::getDatasetId, datasetId);
        Dataset dataset = datasetService.getOneById(datasetId);
        List<DatasetVersionVO> result = this.list(lqw).stream().map(
                datasetVersion -> {
                    return new DatasetVersionVO(datasetVersion, dataset);
                }
        ).collect(Collectors.toList());
        return result;
    }

    @Override
    public List<DatasetVersionVO> getPublicDatasetVersionList(Long datasetId) {
        LambdaQueryWrapper<DatasetVersion> lqw = new LambdaQueryWrapper<>();
        lqw.eq(datasetId != 0, DatasetVersion::getDatasetId, datasetId);
        // 添加对于是否公开的判断
        lqw.eq(DatasetVersion::getIsPublic, Dataset.IS_PUBLIC);

        Dataset dataset = datasetService.getOneById(datasetId);

        List<DatasetVersionVO> datasetVersionVOS = datasetVersionMapper
                .selectDatasetVersionWithUserName(lqw)
                .stream()
                .map(datasetVersion -> {
                    DatasetVersionVO vo = new DatasetVersionVO(datasetVersion, dataset);

                    // 修改：从文件读取图片数量和文件大小
                    Integer imageCount = 0;
                    Long totalFileSize = 0L;

                    // 读取图片数量
                    try {
                        String filePath = "dataset/" + dataset.getId() + "/versionFile/" +
                                datasetVersion.getVersionName() + "/annotation/annoFileCount.text";
                        String fileCountStr = minioUtil.readString(bucketName, filePath);
                        if (StringUtils.isNotBlank(fileCountStr)) {
                            imageCount = Integer.parseInt(fileCountStr.trim());
                        }
                    } catch (Exception e) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "读取公开版本图片数量文件失败: {}-{}, 使用默认值0",
                                dataset.getId(), datasetVersion.getVersionName());
                        imageCount = 0;
                    }

                    // 读取文件大小
                    try {
                        String imageSizeStatsPath = "dataset/" + dataset.getId() + "/versionFile/" +
                                datasetVersion.getVersionName() + "/annotation/imageSizeStats.text";

                        if (minioUtil.doesObjectExistSilent(bucketName, imageSizeStatsPath)) {
                            String imageSizeStatsString = minioUtil.readString(bucketName, imageSizeStatsPath);
                            Map<String, Object> imageSizeStats = JSONObject.parseObject(imageSizeStatsString, Map.class);

                            if (imageSizeStats.containsKey("totalSizeBytes")) {
                                totalFileSize = Long.valueOf(imageSizeStats.get("totalSizeBytes").toString());
                            }
                        } else {
                            // 文件不存在时使用默认值
                            totalFileSize = 1024L * 1024L * 100L; // 默认值：100MB
                        }
                    } catch (Exception e) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "读取公开版本文件大小失败: {}-{}, 使用默认值",
                                dataset.getId(), datasetVersion.getVersionName());
                        totalFileSize = 1024L * 1024L * 100L; // 默认值：100MB
                    }

                    vo.setImageCounts(imageCount);
                    vo.setTotalFileSize(totalFileSize);

                    Map<String, Integer> labelCountMap = new HashMap<>();
                    try {
                        String labelCountPath = "dataset/" + dataset.getId() + "/versionFile/" +
                                datasetVersion.getVersionName() + "/annotation/labelsCount.text";
                        String labelCountString = minioUtil.readString(bucketName, labelCountPath);
                        if (StringUtils.isNotBlank(labelCountString)) {
                            // 解析JSON格式的标签计数
                            Map<String, Object> rawLabelCountMap = JSONObject.parseObject(labelCountString, Map.class);

                            // 确保所有值都是Integer类型（防止JSON解析问题）
                            if (rawLabelCountMap != null) {
                                for (Map.Entry<String, Object> entry : rawLabelCountMap.entrySet()) {
                                    try {
                                        Integer count = 0;
                                        if (entry.getValue() instanceof Number) {
                                            count = ((Number) entry.getValue()).intValue();
                                        } else if (entry.getValue() instanceof String) {
                                            count = Integer.valueOf((String) entry.getValue());
                                        }
                                        labelCountMap.put(entry.getKey(), count);
                                    } catch (Exception e) {
                                        LogUtil.warn(LogEnum.BIZ_DATASET, "解析标签计数失败: {} -> {}",
                                                entry.getKey(), entry.getValue());
                                        labelCountMap.put(entry.getKey(), 0);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "读取公开版本标签计数失败: {}-{}, 错误: {}",
                                dataset.getId(), datasetVersion.getVersionName(), e.getMessage());
                    }

                    // 设置标签计数映射
                    vo.setLabelCountMap(labelCountMap);

                    return vo;
                })
                .collect(Collectors.toList());

        return datasetVersionVOS;
    }

    @Override
    public boolean publishDatasetVersion(Long datasetId, String versionName) {
        List<Long> datasetGroupIds = datasetDatasetGroupMapper.getGroupIdsByDatasetId(datasetId);
        if (datasetGroupIds.isEmpty()) {
            log.error("数据集没有分组");
        }
        // 发布数据集组织-数据集-数据集版本
        return datasetGroupMapper.publishDatasetGroups(datasetGroupIds) &&
                datasetVersionMapper.publishDatasetVersion(datasetId, versionName) > 0 &&
                datasetMapper.publishDataset(datasetId) > 0;
    }

    @Override
    public boolean cancelPublishDatasetVersion(Long datasetId, String versionName) {
        DatasetVersion datasetVersion = datasetVersionMapper.getByDatasetIdAndVersionName(datasetId, versionName);
        Integer publishedDatasetVersionCount = datasetVersionMapper.getPublishedDatasetVersionCountByDatasetId(datasetId);
        if (datasetVersion == null) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_NOT_EXIST);
        }
        if (datasetVersion.getIsPublic() == Dataset.IS_NOT_PUBLIC) {
            throw new BusinessException(ErrorEnum.DATASET_VERSION_NOT_PUBLISH);
        }
        datasetVersion.setIsPublic(Dataset.IS_NOT_PUBLIC);
        if (publishedDatasetVersionCount > 1) {
            return datasetVersionMapper.updateById(datasetVersion) > 0;
        } else {
            // 这是最后一个公开版本，需要同时取消数据集的公开状态
            Dataset dataset = datasetService.getById(datasetId);
            dataset.setIsPublic(Dataset.IS_NOT_PUBLIC);

            // 检查该数据集所属的数据集组是否还有其他公开数据集
            List<Long> groupIds = datasetDatasetGroupMapper.getDatasetGroupIdsByDatasetId(datasetId);
            for (Long groupId : groupIds) {
                // 获取该组的所有数据集
                List<Long> groupDatasetIds = datasetDatasetGroupMapper.getDatasetsByDatasetGroupId(groupId);
                boolean hasOtherPublicDatasets = false;

                for (Long otherDatasetId : groupDatasetIds) {
                    if (!otherDatasetId.equals(datasetId)) {
                        // 关键修改：直接检查该数据集是否有公开版本
                        Integer otherPublicVersions = datasetVersionMapper.getPublishedDatasetVersionCountByDatasetId(otherDatasetId);
                        if (otherPublicVersions != null && otherPublicVersions > 0) {
                            hasOtherPublicDatasets = true;
                            break;
                        }
                    }
                }

                // 如果该组没有其他公开数据集，将数据集组设为非公开
                if (!hasOtherPublicDatasets) {
                    DatasetGroup datasetGroup = datasetGroupMapper.selectById(groupId);
                    if (datasetGroup != null && Boolean.TRUE.equals(datasetGroup.getIsPublic())) {
                        datasetGroup.setIsPublic(false);
                        datasetGroupMapper.updateById(datasetGroup);
                    }
                }
            }

            return datasetVersionMapper.updateById(datasetVersion) > 0 && datasetService.updateById(dataset);
        }
    }

    /**
     * 构造数据集版本 URL
     *
     * @param datasetUri 数据集 URI
     * @param versionName 版本名称
     * @return 版本 URL
     */
    private String buildVersionUrl(String datasetUri, String versionName) {
        return datasetUri + "/" + VERSION_FILE + "/" + versionName;
    }

    /**
     * 导出 YOLO 格式数据集（包括 YOLO 和 Segment-YOLO）
     *
     * @param format 格式名称（YOLO 或 Segment-YOLO）
     * @param versionFileRootPath 版本文件根路径
     * @param picNames 图片文件名列表
     * @param datasetVersion 数据集版本对象
     * @param datasetId 数据集 ID
     * @return 是否成功提交导出任务
     */
    private boolean exportYoloFormat(String format, String versionFileRootPath, List<String> picNames,
                                     DatasetVersion datasetVersion, Long datasetId, Long transferTaskId) {
        String versionFilePath = versionFileRootPath + "/" + format;
        String annoPath = versionFilePath + "/" + DIR_ANNOTATIONS;
        String yamlPath = versionFilePath + "/" + FILE_DATA_YAML;
        String zipName = datasetId + "_" + format + "_" + datasetVersion.getVersionName() + ".zip";

        try {
            pool.getExecutor().submit(() -> zipYoloLayoutAndUploadToMinio(
                    bucketName, annoPath, yamlPath, picNames, zipName,
                    versionFilePath, datasetVersion.getId(), datasetId));
            if (transferTaskId != null) {
                transferExportTaskService.progress(transferTaskId, "PACKAGE", 20, picNames.size(), 0, "开始打包导出文件");
                pool.getExecutor().submit(() -> {
                    try {
                        while (true) {
                            String status = toZipFlag.get(datasetVersion.getId());
                            if ("success".equals(status)) {
                                transferExportTaskService.complete(transferTaskId, versionFilePath + "/" + zipName, "导出文件打包完成");
                                break;
                            }
                            if ("failed".equals(status)) {
                                transferExportTaskService.fail(transferTaskId, "导出文件打包失败");
                                break;
                            }
                            if ("CANCELLED".equals(transferExportTaskService.get(transferTaskId).getStatus())) break;
                            Thread.sleep(500L);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        transferExportTaskService.fail(transferTaskId, "导出任务被中断");
                    } catch (Exception e) {
                        transferExportTaskService.fail(transferTaskId, e.getMessage());
                    }
                });
            }
            return true;
        } catch (Exception e) {
            if (transferTaskId != null) {
                transferExportTaskService.fail(transferTaskId, e.getMessage());
            }
            LogUtil.error(LogEnum.BIZ_DATASET, "Dataset version export error: format={}, datasetId={}, versionId={}",
                    format, datasetId, datasetVersion.getId(), e);
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-打包失败",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return false;
        }
    }

    @Override
    public boolean datasetExport(Long datasetId, String versionName) {
        DatasetVersion datasetVersion = datasetVersionMapper.getByDatasetIdAndVersionName(datasetId, versionName);
        Dataset dataset = datasetService.getOneById(datasetId);

        if(datasetVersion.getDataConversion() != 1){
            return false;
        }

        // Use the dataset's persisted storage URI so exports work for both legacy
        // dataset/{id} paths and newer paths such as dataset/image/{id}, dataset/video/{id}, dataset/pc/{id}.
        String versionFileRootPath = fileUtil.getDatasetAbsPath(datasetId) + "/versionFile/" + versionName;
        List<String> picNames = new ArrayList<>();
        List<String> picUrls = fileService.selectUrlsFromFileStatusNormalOrDeleted(datasetId, versionName);
        // 从URL中提取MinIO对象键：正确去掉 bucketName/ 前缀
        // URL格式: bucketName/dataset/image/{id}/origin/xxx.jpg
        picUrls.forEach(picUrl -> {
            String objectKey;
            if (picUrl.startsWith(bucketName + "/")) {
                objectKey = picUrl.substring(bucketName.length() + 1);
            } else {
                // 兼容没有bucketName前缀的情况（直接是对象键）
                objectKey = picUrl;
            }
            picNames.add(objectKey);
        });

        String taskName = dataset.getName() + " / " + versionName + " / " + datasetVersion.getFormat();
        Long transferTaskId = transferExportTaskService.createDatasetVersionTask(
                taskName, datasetId, datasetVersion.getId(), versionName, datasetVersion.getFormat(), picNames.size());

        switch (datasetVersion.getFormat()) {
            case FORMAT_YOLO:
                return exportYoloFormat(FORMAT_YOLO, versionFileRootPath, picNames, datasetVersion, datasetId, transferTaskId);
            case FORMAT_SEGMENT_YOLO:
                return exportYoloFormat(FORMAT_SEGMENT_YOLO, versionFileRootPath, picNames, datasetVersion, datasetId, transferTaskId);
            default:
                transferExportTaskService.fail(transferTaskId, "不支持的导出格式");
                LogUtil.warn(LogEnum.BIZ_DATASET, "Unsupported export format: {}", datasetVersion.getFormat());
                return false;
        }
    }

    @Override
    public String datasetExportFromOrigin(Long datasetId) {
        // 快速校验数据集是否存在
        Dataset dataset = datasetService.getOneById(datasetId);
        if (dataset == null) {
            LogUtil.error(LogEnum.BIZ_DATASET, "dataset not found: {}", datasetId);
            return "数据集不存在";
        }

        // 快速校验数据类型
        Integer dataType = dataset.getDataType();
        if (DatatypeEnum.IMAGE.getValue().equals(dataType)) {
            // 图片类型数据集，支持目标检测和目标分割
        } else if (DatatypeEnum.VIDEO.getValue().equals(dataType)) {
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-暂不支持视频类型导出",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return "暂不支持视频类型导出";
        } else {
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-不支持的数据类型",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return "不支持的数据类型";
        }

        // 快速校验目录是否存在
        String datasetRootPath = fileUtil.getDatasetIdAbsPath(datasetId);
        String annotationDir = datasetRootPath + "/annotation";
        String originDir = datasetRootPath + "/origin";

        File annotationDirFile = new File(annotationDir);
        File originDirFile = new File(originDir);
        if (!annotationDirFile.exists() || !annotationDirFile.isDirectory()) {
            LogUtil.error(LogEnum.BIZ_DATASET, "annotation directory not found: {}", annotationDir);
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-标注目录不存在",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return "未找到已标注文件信息";
        }
        if (!originDirFile.exists() || !originDirFile.isDirectory()) {
            LogUtil.error(LogEnum.BIZ_DATASET, "origin directory not found: {}", originDir);
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-原始图片目录不存在",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return "未找到原始图片文件信息";
        }

        // 快速检查是否有标注文件（不读取内容，只检查文件存在）
        File[] annotationFiles = annotationDirFile.listFiles((dir, name) -> {
            File f = new File(dir, name);
            return f.isFile() && !name.startsWith(".");
        });
        if (annotationFiles == null || annotationFiles.length == 0) {
            LogUtil.error(LogEnum.BIZ_DATASET, "no annotation files found in: {}", annotationDir);
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-没有标注文件",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return "未找到已标注文件信息";
        }

        // 设置导出状态为进行中
        originExportFlag.put(datasetId, "going");

        // 记录任务提交事件
        datasetOperationEventService.createAndInsertEvent(
                datasetId, new Date(), "数据集导出任务已提交，正在处理中...",
                DatasetOperationEvent.EventType.INFO,
                DatasetOperationEvent.OperationType.DATA_EXPORT);

        // 🔥 将所有耗时操作移到异步线程中执行
        pool.getExecutor().submit(() -> {
            try {
                LogUtil.info(LogEnum.BIZ_DATASET, "datasetExportFromOrigin async task start, datasetId={}", datasetId);

                // 获取数据集标注类型
                Integer annotateType = dataset.getAnnotateType();
                boolean isSegmentation = AnnotateTypeEnum.SEMANTIC_CUP.getValue().equals(annotateType);
                String formatType = isSegmentation ? "Segment-YOLO" : "YOLO";

                // 查询数据集的标签映射：category_id -> label name
                List<Label> labels = datasetLabelService.listLabelByDatasetId(datasetId);
                Map<Long, Integer> labelNameToIndex = new HashMap<>();
                List<String> labelNames = new ArrayList<>();
                for (Label label : labels) {
                    if (!labelNameToIndex.containsKey(label.getId())) {
                        int index = labelNames.size();
                        labelNames.add(label.getName());
                        labelNameToIndex.put(label.getId(), index);
                    }
                }

                // 遍历标注文件，匹配 origin 中的图片并转换格式
                List<String> matchedImageNames = new ArrayList<>();
                List<String> yoloAnnoLines = new ArrayList<>();
                int exportedCount = 0;

                for (File annoFile : annotationFiles) {
                    String baseName = annoFile.getName(); // 无后缀的文件名

                    // 在 origin 目录查找同名图片（可能有不同后缀）
                    String[] extensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
                    File matchedImageFile = null;
                    String imageExt = "";

                    for (String ext : extensions) {
                        File candidate = new File(originDirFile, baseName + ext);
                        if (candidate.exists() && candidate.isFile()) {
                            matchedImageFile = candidate;
                            imageExt = ext;
                            break;
                        }
                    }

                    if (matchedImageFile == null) {
                        // origin 中没有对应的图片，跳过
                        continue;
                    }

                    // 读取标注文件内容
                    String annoContent;
                    try {
                        annoContent = readFileToString(annoFile);
                    } catch (Exception e) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "failed to read annotation file: {}", annoFile.getAbsolutePath());
                        continue;
                    }

                    // 解析 JSON 标注内容
                    List<Map<String, Object>> annotations;
                    try {
                        annotations = JSON.parseObject(annoContent, new TypeReference<List<Map<String, Object>>>() {});
                    } catch (Exception e) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "failed to parse annotation JSON: {}", annoFile.getAbsolutePath());
                        continue;
                    }

                    if (annotations == null || annotations.isEmpty()) {
                        // 空标注跳过，不导出
                        continue;
                    }

                    // 获取图片尺寸
                    int[] imgSize = getImageSize(matchedImageFile);
                    if (imgSize[0] == 0 || imgSize[1] == 0) {
                        continue;
                    }

                    // 根据标注类型转换为对应的 YOLO 格式
                    String yoloLine;
                    if (isSegmentation) {
                        yoloLine = convertAnnotationToSegmentYolo(annotations, labelNameToIndex, imgSize[0], imgSize[1]);
                    } else {
                        yoloLine = convertAnnotationToYolo(annotations, labelNameToIndex, imgSize[0], imgSize[1]);
                    }

                    if (yoloLine != null && !yoloLine.isEmpty()) {
                        matchedImageNames.add(baseName + imageExt);
                        yoloAnnoLines.add(yoloLine);
                        exportedCount++;
                    }
                }

                if (exportedCount == 0) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "no matched images with annotations found");
                    originExportFlag.put(datasetId, "failed");
                    datasetOperationEventService.createAndInsertEvent(
                            datasetId, new Date(), "数据集导出-没有找到已标注的图片",
                            DatasetOperationEvent.EventType.ERROR,
                            DatasetOperationEvent.OperationType.DATA_EXPORT);
                    return;
                }

                // 创建临时导出目录
                long exportTimestamp = System.currentTimeMillis();
                String tempExportDir = datasetRootPath + "/export_temp_" + exportTimestamp;
                String tempAnnoDir = tempExportDir + "/annotations";
                String tempImageDir = tempExportDir + "/images";
                String tempYamlPath = tempExportDir + "/data.yaml";
                String relativeTempDir = "dataset/" + datasetId + "/export_temp_" + exportTimestamp;

                // 创建目录
                new File(tempAnnoDir).mkdirs();
                new File(tempImageDir).mkdirs();

                // 写入 YOLO 标注文件
                for (int i = 0; i < matchedImageNames.size(); i++) {
                    String imageName = matchedImageNames.get(i);
                    String baseNameForFile = imageName.substring(0, imageName.lastIndexOf('.'));
                    String annoFileName = baseNameForFile + ".txt";
                    File annoFileToWrite = new File(tempAnnoDir, annoFileName);
                    writeStringToFile(annoFileToWrite, yoloAnnoLines.get(i));
                }

                // 复制图片文件
                for (String imageName : matchedImageNames) {
                    File srcImage = new File(originDirFile, imageName);
                    File destImage = new File(tempImageDir, imageName);
                    copyFile(srcImage, destImage);
                }

                // 生成 data.yaml
                String yamlContent = "train: images\n" +
                        "val: images\n" +
                        "nc: " + labelNames.size() + "\n" +
                        "names: " + labelNames;
                writeStringToFile(new File(tempYamlPath), yamlContent);

                // 打包并上传
                String zipFileName = datasetId + "_" + formatType + "_origin.zip";
                String targetDir = "dataset/" + datasetId + "/export_temp_" + exportTimestamp;

                // 确保目标目录存在
                File targetDirFile = new File(fileUtil.getNfsPath() + bucketName + "/" + targetDir);
                if (!targetDirFile.exists()) {
                    targetDirFile.mkdirs();
                }

                // 创建 ZIP 文件
                String zipFilePath = targetDir + "/" + zipFileName;
                List<String> imagePaths = matchedImageNames.stream()
                        .map(name -> relativeTempDir + "/images/" + name)
                        .collect(Collectors.toList());
                boolean ok = fileUtil.createYoloZipDirectly(
                        relativeTempDir + "/annotations",
                        relativeTempDir + "/data.yaml",
                        imagePaths,
                        zipFilePath,
                        labelNames);

                if (ok) {
                    // 存储下载 URL
                    originExportUrl.put(datasetId, zipFilePath);
                    originExportFlag.put(datasetId, "success");
                    LogUtil.info(LogEnum.BIZ_DATASET, "datasetExportFromOrigin success, datasetId={}, format={}", datasetId, formatType);
                    datasetOperationEventService.createAndInsertEvent(
                            datasetId, new Date(),
                            "数据集导出-" + formatType + "打包成功，共导出 " + exportedCount + " 张图片",
                            DatasetOperationEvent.EventType.INFO,
                            DatasetOperationEvent.OperationType.DATA_EXPORT);
                } else {
                    originExportFlag.put(datasetId, "failed");
                    datasetOperationEventService.createAndInsertEvent(
                            datasetId, new Date(), "数据集导出-打包失败",
                            DatasetOperationEvent.EventType.ERROR,
                            DatasetOperationEvent.OperationType.DATA_EXPORT);
                }

                // 注意：临时目录不删除，由定时任务 autoCleanOriginExportTempFiles 清理

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "datasetExportFromOrigin async task error", e);
                originExportFlag.put(datasetId, "failed");
                datasetOperationEventService.createAndInsertEvent(
                        datasetId, new Date(), "数据集导出-处理失败: " + e.getMessage(),
                        DatasetOperationEvent.EventType.ERROR,
                        DatasetOperationEvent.OperationType.DATA_EXPORT);
            }
        });

        // 立即返回，不等待异步任务完成
        LogUtil.info(LogEnum.BIZ_DATASET, "datasetExportFromOrigin task submitted, datasetId={}", datasetId);
        return null;
    }

    /**
     * 读取文件内容为字符串
     */
    private String readFileToString(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * 写字符串到文件
     */
    private void writeStringToFile(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(content);
        }
    }

    /**
     * 复制文件
     */
    private void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * 压缩目录为ZIP文件
     * @param sourceDir 源目录
     * @param zipFile 目标ZIP文件
     * @return 是否成功
     */
    private boolean zipDirectory(File sourceDir, File zipFile) {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipDirectoryHelper(sourceDir, sourceDir.getName(), zos);
            return true;
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Failed to create zip file", e);
            return false;
        }
    }

    /**
     * 递归压缩目录的辅助方法
     */
    private void zipDirectoryHelper(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zos.putNextEntry(new ZipEntry(fileName));
                zos.closeEntry();
            } else {
                zos.putNextEntry(new ZipEntry(fileName + "/"));
                zos.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipDirectoryHelper(childFile, fileName + "/" + childFile.getName(), zos);
                }
            }
            return;
        }
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[8192];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    /**
     * 获取图片尺寸
     * @return int[2] = {width, height}
     */
    private int[] getImageSize(File imageFile) {
        int[] size = {0, 0};
        try {
            BufferedImage img = ImageIO.read(imageFile);
            if (img != null) {
                size[0] = img.getWidth();
                size[1] = img.getHeight();
            }
        } catch (Exception e) {
            LogUtil.warn(LogEnum.BIZ_DATASET, "failed to read image dimensions: {}", imageFile.getAbsolutePath());
        }
        return size;
    }

    /**
     * 将JSON标注转换为YOLO格式
     * @param annotations JSON标注列表
     * @param labelNameToIndex 标签名到索引的映射
     * @param imgWidth 图片宽度
     * @param imgHeight 图片高度
     * @return YOLO格式的标注行
     */
    private String convertAnnotationToYolo(List<Map<String, Object>> annotations,
                                            Map<Long, Integer> labelNameToIndex,
                                            int imgWidth, int imgHeight) {
        StringBuilder yoloLine = new StringBuilder();

        for (Map<String, Object> ann : annotations) {
            Object categoryIdObj = ann.get("category_id");
            Object bboxObj = ann.get("bbox");

            if (categoryIdObj == null || bboxObj == null) {
                continue;
            }

            Long categoryId = ((Number) categoryIdObj).longValue();
            Integer labelIndex = labelNameToIndex.get(categoryId);
            if (labelIndex == null) {
                continue;
            }

            // 解析 bbox: [x_min, y_min, width, height]
            List<Double> bbox;
            try {
                bbox = JSON.parseArray(bboxObj.toString(), Double.class);
            } catch (Exception e) {
                continue;
            }

            if (bbox == null || bbox.size() < 4) {
                continue;
            }

            double xMin = bbox.get(0);
            double yMin = bbox.get(1);
            double width = bbox.get(2);
            double height = bbox.get(3);

            // 转换为 YOLO 格式：center_x, center_y, width, height (归一化)
            double centerX = (xMin + width / 2.0) / imgWidth;
            double centerY = (yMin + height / 2.0) / imgHeight;
            double normWidth = width / imgWidth;
            double normHeight = height / imgHeight;

            // 限制在 [0, 1] 范围内
            centerX = Math.max(0, Math.min(1, centerX));
            centerY = Math.max(0, Math.min(1, centerY));
            normWidth = Math.max(0, Math.min(1, normWidth));
            normHeight = Math.max(0, Math.min(1, normHeight));

            if (yoloLine.length() > 0) {
                yoloLine.append("\n");
            }
            yoloLine.append(String.format("%d %.6f %.6f %.6f %.6f",
                    labelIndex, centerX, centerY, normWidth, normHeight));
        }

        return yoloLine.toString();
    }

    /**
     * 将JSON标注转换为Segment-YOLO格式（目标分割）
     * @param annotations JSON标注列表
     * @param labelNameToIndex 标签名到索引的映射
     * @param imgWidth 图片宽度
     * @param imgHeight 图片高度
     * @return Segment-YOLO格式的标注行
     */
    private String convertAnnotationToSegmentYolo(List<Map<String, Object>> annotations,
                                                   Map<Long, Integer> labelNameToIndex,
                                                   int imgWidth, int imgHeight) {
        StringBuilder yoloLine = new StringBuilder();

        for (Map<String, Object> ann : annotations) {
            Object categoryIdObj = ann.get("category_id");
            Object segmentationObj = ann.get("segmentation");

            if (categoryIdObj == null || segmentationObj == null) {
                continue;
            }

            Long categoryId = ((Number) categoryIdObj).longValue();
            Integer labelIndex = labelNameToIndex.get(categoryId);
            if (labelIndex == null) {
                continue;
            }

            // 解析 segmentation: [[x1, y1], [x2, y2], [x3, y3], ...]
            List<List<Double>> segmentation;
            try {
                segmentation = JSON.parseObject(segmentationObj.toString(), new TypeReference<List<List<Double>>>() {});
            } catch (Exception e) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "failed to parse segmentation data");
                continue;
            }

            if (segmentation == null || segmentation.size() < 3) {
                // 至少需要3个点才能构成多边形
                continue;
            }

            if (yoloLine.length() > 0) {
                yoloLine.append("\n");
            }

            // 添加类别索引
            yoloLine.append(labelIndex);

            // 遍历所有点进行归一化
            for (List<Double> point : segmentation) {
                if (point != null && point.size() >= 2) {
                    double x = point.get(0);
                    double y = point.get(1);

                    // 归一化到[0,1]
                    double normalizedX = x / imgWidth;
                    double normalizedY = y / imgHeight;

                    // 限制在 [0, 1] 范围内
                    normalizedX = Math.max(0, Math.min(1, normalizedX));
                    normalizedY = Math.max(0, Math.min(1, normalizedY));

                    yoloLine.append(" ").append(normalizedX).append(" ").append(normalizedY);
                }
            }
        }

        return yoloLine.toString();
    }

    void zipAndUploadToMinio(String bucketName, List<String> minioFilePaths, String zipFileName, String targetDir,Long datasetVersionId,Long datasetID){

        //如果这个版本导出完成过并且没被清除。或者正在进行，就不用再去导出一份
        if(toZipFlag.get(datasetVersionId) == null || Objects.equals(toZipFlag.get(datasetVersionId), "failed")){
            toZipFlag.put(datasetVersionId,"going");
            if(minioUtil.zipAndUploadToMinio(bucketName,minioFilePaths,zipFileName,targetDir)){
                toZipFlag.put(datasetVersionId,"success");
                datasetOperationEventService.createAndInsertEvent(datasetID,new Date(),"数据集导出-打包成功", DatasetOperationEvent.EventType.INFO,DatasetOperationEvent.OperationType.DATA_EXPORT);
            }else {
                toZipFlag.put(datasetVersionId,"failed");
                datasetOperationEventService.createAndInsertEvent(datasetID,new Date(),"数据集导出-打包失败", DatasetOperationEvent.EventType.ERROR,DatasetOperationEvent.OperationType.DATA_EXPORT);
            }
        }
    }


    public void zipYoloLayoutAndUploadToMinio(
            String bucketName,
            String annoDirPrefix,          // 例如 ".../temp/annotations/"（必须是目录前缀）
            String yamlObjectKey,          // 例如 ".../YOLO/data.yaml"
            List<String> imageObjectKeys,  // 同一目录下挑选的图片对象键列表
            String zipFileName,            // 目标 ZIP 文件名
            String targetDir,              // 例如 ".../YOLO"
            Long datasetVersionId,
            Long datasetId) {

        // 已完成或正在进行则不重复导出；仅在 null 或 failed 时执行
        String flag = toZipFlag.get(datasetVersionId);
        if (flag != null && !"failed".equals(flag)) {
            return;
        }

        toZipFlag.put(datasetVersionId, "going");
        boolean ok = false;
        try {
            LogUtil.info(LogEnum.BIZ_DATASET,
                    "开始导出ZIP: datasetId={}, versionId={}, bucket={}, annoPrefix={}, yamlKey={}, imageCount={}, firstImageKey={}, zipName={}, targetDir={}",
                    datasetId,
                    datasetVersionId,
                    bucketName,
                    annoDirPrefix,
                    yamlObjectKey,
                    imageObjectKeys == null ? 0 : imageObjectKeys.size(),
                    imageObjectKeys == null || imageObjectKeys.isEmpty() ? null : imageObjectKeys.get(0),
                    zipFileName,
                    targetDir);
            ok = minioUtil.zipYoloLayoutToMinio(bucketName, annoDirPrefix, yamlObjectKey, imageObjectKeys, zipFileName, targetDir);
            toZipFlag.put(datasetVersionId, ok ? "success" : "failed");

            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(),
                    ok ? "数据集导出-打包成功" : "数据集导出-打包失败",
                    ok ? DatasetOperationEvent.EventType.INFO : DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT
            );

        } catch (Exception e) {
            toZipFlag.put(datasetVersionId, "failed");
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-打包失败",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT
            );
            LogUtil.error(LogEnum.BIZ_DATASET, "zip yolo layout error", e);
        }
    }

    public void zipYoloLayoutDirectly(
            String bucketName,
            String annoDirPrefix,          // 例如 "dataset/123/versionFile/v1/YOLO/annotations/"
            String yamlObjectKey,          // 例如 "dataset/123/versionFile/v1/YOLO/data.yaml"
            List<String> imageObjectKeys,  // 图片对象键列表
            String zipFileName,            // 目标 ZIP 文件名
            String targetDir,              // 例如 "dataset/123/versionFile/v1/YOLO"
            Long datasetVersionId,
            Long datasetId) {

        // 已完成或正在进行则不重复导出
        String flag = toZipFlag.get(datasetVersionId);
        if (flag != null && !"failed".equals(flag)) {
            return;
        }

        toZipFlag.put(datasetVersionId, "going");
        boolean ok = false;

        try {
            // 构建实际的NFS文件系统路径
            String zipFilePath =  targetDir + "/" + zipFileName;
            System.out.println("zipFilePath = " + zipFilePath);
            // 确保目标目录存在
            java.io.File zipDir = new File(zipFilePath).getParentFile();
            if (!zipDir.exists()) {
                zipDir.mkdirs();
            }

            ok = fileUtil.createYoloZipDirectly(annoDirPrefix, yamlObjectKey,
                    imageObjectKeys, zipFilePath, null);

            toZipFlag.put(datasetVersionId, ok ? "success" : "failed");

            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(),
                    ok ? "数据集导出-打包成功" : "数据集导出-打包失败",
                    ok ? DatasetOperationEvent.EventType.INFO : DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT
            );

        } catch (Exception e) {
            toZipFlag.put(datasetVersionId, "failed");
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-打包失败",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT
            );
            LogUtil.error(LogEnum.BIZ_DATASET, "zip yolo layout directly error", e);
        }
    }





    @Override
    public String checkTaskStatus(Long datasetVersionId){
        return toZipFlag.get(datasetVersionId);
    }

    @Override
    public String checkOriginExportStatus(Long datasetId) {
        String status = originExportFlag.get(datasetId);
        LogUtil.info(LogEnum.BIZ_DATASET, "checkOriginExportStatus datasetId={} status={}", datasetId, status);
        return status;
    }

    /**
     * 获取 origin 导出的下载 URL
     * @param datasetId 数据集ID
     * @return 下载 URL，格式为 "dataset/xxx/export_temp_xxx/xxx.zip"
     */
    public String getOriginExportUrl(Long datasetId) {
        return originExportUrl.get(datasetId);
    }

    /**
     * 从origin导出未标注的数据（包括空标注和无标注文件）
     * @param datasetId 数据集ID
     * @return 错误信息，null表示成功
     */
    @Override
    public String datasetExportUnannotatedFromOrigin(Long datasetId) {
        LogUtil.info(LogEnum.BIZ_DATASET, "datasetExportUnannotatedFromOrigin start, datasetId={}", datasetId);

        // 快速校验数据集是否存在
        Dataset dataset = datasetService.getById(datasetId);
        if (dataset == null) {
            LogUtil.error(LogEnum.BIZ_DATASET, "dataset not found: {}", datasetId);
            return "数据集不存在";
        }

        // 快速校验数据集类型
        if (dataset.getDataType() != 0) {
            return "暂不支持非图片类型导出";
        }

        // 快速校验目录是否存在
        String datasetRootPath = fileUtil.getDatasetIdAbsPath(datasetId);
        String originDir = datasetRootPath + "/origin";
        File originDirFile = new File(originDir);
        if (!originDirFile.exists() || !originDirFile.isDirectory()) {
            LogUtil.error(LogEnum.BIZ_DATASET, "origin directory not found: {}", originDir);
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-原始图片目录不存在",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return "未找到原始图片文件信息";
        }

        // 快速检查origin目录下是否有图片文件
        String[] extensions = {".jpg", ".jpeg", ".png", ".bmp", ".gif"};
        File[] originFiles = originDirFile.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            for (String ext : extensions) {
                if (lowerName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        });

        if (originFiles == null || originFiles.length == 0) {
            LogUtil.error(LogEnum.BIZ_DATASET, "no image files found in origin directory: {}", originDir);
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-原始图片目录为空",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT);
            return "未找到原始图片文件";
        }

        // 设置导出状态为进行中
        originUnannotatedExportFlag.put(datasetId, "going");

        // 记录任务提交事件
        datasetOperationEventService.createAndInsertEvent(
                datasetId, new Date(), "数据集导出任务已提交，正在处理中...",
                DatasetOperationEvent.EventType.INFO,
                DatasetOperationEvent.OperationType.DATA_EXPORT);

        // 🔥 将所有耗时操作移到异步线程中执行
        pool.getExecutor().submit(() -> {
            try {
                LogUtil.info(LogEnum.BIZ_DATASET, "datasetExportUnannotatedFromOrigin async task start, datasetId={}", datasetId);

                String annotationDir = datasetRootPath + "/annotation";
                File annotationDirFile = new File(annotationDir);

                // 获取所有标注文件（用于判断哪些图片是未标注的）
                Set<String> annotatedBaseNames = new HashSet<>();
                if (annotationDirFile.exists() && annotationDirFile.isDirectory()) {
                    File[] annotationFiles = annotationDirFile.listFiles((dir, name) -> {
                        File f = new File(dir, name);
                        return f.isFile() && !name.startsWith(".");
                    });

                    if (annotationFiles != null) {
                        for (File annoFile : annotationFiles) {
                            String baseName = annoFile.getName();
                            // 读取标注文件内容
                            try {
                                String annoContent = readFileToString(annoFile);
                                List<Map<String, Object>> annotations = JSON.parseObject(annoContent,
                                    new TypeReference<List<Map<String, Object>>>() {});

                                // 如果标注不为空，则认为是已标注的
                                if (annotations != null && !annotations.isEmpty()) {
                                    annotatedBaseNames.add(baseName);
                                }
                            } catch (Exception e) {
                                LogUtil.warn(LogEnum.BIZ_DATASET, "failed to read/parse annotation file: {}",
                                    annoFile.getAbsolutePath());
                            }
                        }
                    }
                }

                // 筛选出未标注的图片（包括空标注[]和无标注文件的图片）
                List<String> unannotatedImageNames = new ArrayList<>();
                for (File imageFile : originFiles) {
                    String imageName = imageFile.getName();
                    String baseName = imageName.substring(0, imageName.lastIndexOf('.'));

                    // 如果不在已标注列表中，则认为是未标注的
                    if (!annotatedBaseNames.contains(baseName)) {
                        unannotatedImageNames.add(imageName);
                    }
                }

                if (unannotatedImageNames.isEmpty()) {
                    LogUtil.info(LogEnum.BIZ_DATASET, "no unannotated images found for dataset: {}", datasetId);
                    originUnannotatedExportFlag.put(datasetId, "failed");
                    datasetOperationEventService.createAndInsertEvent(
                            datasetId, new Date(), "数据集导出-没有找到未标注的图片",
                            DatasetOperationEvent.EventType.INFO,
                            DatasetOperationEvent.OperationType.DATA_EXPORT);
                    return;
                }

                // 创建临时导出目录
                long exportTimestamp = System.currentTimeMillis();
                String tempExportDir = datasetRootPath + "/export_unannotated_temp_" + exportTimestamp;
                String tempImageDir = tempExportDir + "/images";

                // 创建目录
                new File(tempImageDir).mkdirs();

                // 复制未标注的图片文件
                for (String imageName : unannotatedImageNames) {
                    File srcImage = new File(originDirFile, imageName);
                    File destImage = new File(tempImageDir, imageName);
                    copyFile(srcImage, destImage);
                }

                // 打包并上传
                String zipFileName = datasetId + "_unannotated_origin.zip";
                String targetDir = "dataset/" + datasetId + "/export_unannotated_temp_" + exportTimestamp;

                // 确保目标目录存在
                File targetDirFile = new File(fileUtil.getNfsPath() + bucketName + "/" + targetDir);
                if (!targetDirFile.exists()) {
                    targetDirFile.mkdirs();
                }

                // 创建 ZIP 文件
                String zipFilePath = targetDir + "/" + zipFileName;
                String fullZipPath = fileUtil.getNfsPath() + bucketName + "/" + zipFilePath;

                // 直接压缩临时目录中的images文件夹
                boolean ok = zipDirectory(new File(tempImageDir), new File(fullZipPath));

                if (ok) {
                    // 存储下载 URL
                    originUnannotatedExportUrl.put(datasetId, zipFilePath);
                    originUnannotatedExportFlag.put(datasetId, "success");
                    LogUtil.info(LogEnum.BIZ_DATASET, "datasetExportUnannotatedFromOrigin success, datasetId={}", datasetId);
                    datasetOperationEventService.createAndInsertEvent(
                            datasetId, new Date(),
                            "数据集导出-打包成功，共导出 " + unannotatedImageNames.size() + " 张未标注图片",
                            DatasetOperationEvent.EventType.INFO,
                            DatasetOperationEvent.OperationType.DATA_EXPORT);
                } else {
                    originUnannotatedExportFlag.put(datasetId, "failed");
                    datasetOperationEventService.createAndInsertEvent(
                            datasetId, new Date(), "数据集导出-打包失败",
                            DatasetOperationEvent.EventType.ERROR,
                            DatasetOperationEvent.OperationType.DATA_EXPORT);
                }

                // 临时目录不删除，由定时任务清理

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "datasetExportUnannotatedFromOrigin async task error", e);
                originUnannotatedExportFlag.put(datasetId, "failed");
                datasetOperationEventService.createAndInsertEvent(
                        datasetId, new Date(), "数据集导出-处理失败: " + e.getMessage(),
                        DatasetOperationEvent.EventType.ERROR,
                        DatasetOperationEvent.OperationType.DATA_EXPORT);
            }
        });

        // 立即返回，不等待异步任务完成
        LogUtil.info(LogEnum.BIZ_DATASET, "datasetExportUnannotatedFromOrigin task submitted, datasetId={}", datasetId);
        return null;
    }

    /**
     * 检查从origin导出未标注数据的任务状态
     * @param datasetId 数据集ID
     * @return 导出状态：going, success, failed
     */
    @Override
    public String checkOriginUnannotatedExportStatus(Long datasetId) {
        String status = originUnannotatedExportFlag.get(datasetId);
        LogUtil.info(LogEnum.BIZ_DATASET, "checkOriginUnannotatedExportStatus datasetId={} status={}", datasetId, status);
        return status;
    }

    /**
     * 获取从origin导出未标注数据的下载 URL
     * @param datasetId 数据集ID
     * @return 下载 URL
     */
    @Override
    public String getOriginUnannotatedExportUrl(Long datasetId) {
        return originUnannotatedExportUrl.get(datasetId);
    }

    //清理导出产生的zip，将toZipFlag中对应条目移除
    @Override
    public void cleanZips() {
        // 1. 找出所有标记为 "successes" 的 datasetVersionId
        List<Long> successIds = toZipFlag.entrySet().stream()
                .filter(entry -> "success".equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (successIds.isEmpty()) {
            return;
        }

        //根据 ID 查询对应的 DatasetVersion
        List<DatasetVersion> datasetVersions = datasetVersionMapper.findByIds(successIds);

        //构建 ID -> DatasetVersion 的 Map
        Map<Long, DatasetVersion> successVersionMap = datasetVersions.stream()
                .collect(Collectors.toMap(DatasetVersion::getId, dv -> dv));

        // 4. 遍历成功的 DatasetVersion 进行操作
        for (Map.Entry<Long, DatasetVersion> entry : successVersionMap.entrySet()) {
            Long versionId = entry.getKey();
            DatasetVersion version = entry.getValue();
            String datasetId = version.getDatasetId().toString();
            String versionFileRootPath = fileUtil.getDatasetAbsPath(version.getDatasetId())
                    + "/versionFile/" + version.getVersionName();
            String zipPath = versionFileRootPath + "/" + version.getFormat() + "/" + datasetId + "_" + version.getFormat() + "_" + version.getVersionName()  + ".zip";
            System.out.println("zipPathToClean = " + zipPath);
            try {
                minioUtil.del(bucketName,zipPath);
                toZipFlag.remove(versionId);
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行一次
    void autoCleanZips(){
        cleanZips();
    }

    /**
     * 清理origin导出的临时文件（export_temp目录）
     * 每天凌晨2点30分执行一次
     */
    @Scheduled(cron = "0 30 2 * * ?")
    void autoCleanOriginExportTempFiles() {
        cleanOriginExportTempFiles();
    }

    /**
     * 清理origin导出产生的临时文件
     */
    private void cleanOriginExportTempFiles() {
        // 找出所有状态为 success 或 failed 的 datasetId
        List<Long> datasetIds = new ArrayList<>();
        for (Map.Entry<Long, String> entry : originExportFlag.entrySet()) {
            if ("success".equals(entry.getValue()) || "failed".equals(entry.getValue())) {
                datasetIds.add(entry.getKey());
            }
        }

        if (datasetIds.isEmpty()) {
            return;
        }

        for (Long datasetId : datasetIds) {
            try {
                // 获取存储的 URL 路径并删除 zip 文件
                String zipPath = originExportUrl.get(datasetId);
                if (zipPath != null) {
                    // zipPath 格式: dataset/xxx/export_temp_xxx/xxx.zip
                    // 我们需要获取目录前缀: dataset/xxx/export_temp_xxx/
                    minioUtil.del(bucketName, zipPath);

                    // 提取临时目录路径并删除目录
                    int lastSlashIndex = zipPath.lastIndexOf('/');
                    if (lastSlashIndex > 0) {
                        String tempDirPath = zipPath.substring(0, lastSlashIndex + 1);
                        List<String> objects = minioUtil.listObjectsInFolder(bucketName, tempDirPath);
                        for (String obj : objects) {
                            try {
                                minioUtil.del(bucketName, obj);
                            } catch (Exception e) {
                                LogUtil.warn(LogEnum.BIZ_DATASET, "Failed to delete temp file: {}", obj);
                            }
                        }
                    }

                    LogUtil.info(LogEnum.BIZ_DATASET, "Cleaned origin export files for dataset: {}", datasetId);
                }

                // 移除状态记录
                originExportFlag.remove(datasetId);
                originExportUrl.remove(datasetId);

            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "Failed to clean origin export temp files for dataset: {}", datasetId, e);
            }
        }
    }






    @Override
    public boolean datasetAssembleFromMultipleVersion(List<Long> versions, String targetDir, List<String> keepLabels) {
        long startTime = System.currentTimeMillis();

        //标准化保留集合（全部转小写；允许 null，null -> 保留全部）
        final Set<String> keepSet = (keepLabels == null || keepLabels.isEmpty())
                ? null
                : keepLabels.stream()
                .map(s -> s == null ? null : s.trim().toLowerCase())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));


        // 数据集版本与其对应图片文件的映射关系
        Map<DatasetVersion, List<String>> picsMap = new HashMap<>();
        // 存储每个版本的标签映射信息
        Map<DatasetVersion, VersionLabelInfo> versionLabelInfoMap = new HashMap<>();
        // 全局统一的标签列表（去重后）
        Set<String> globalLabelsSet = new LinkedHashSet<>();

        LogUtil.info(LogEnum.BIZ_DATASET, "开始合并{}个数据集版本，保留标签：{}", versions.size(),
                (keepSet == null ? "[全部]" : keepSet));
        DatasetVersion datasetVersion = null;
        Set<String> allAvailableLabels = new LinkedHashSet<>();
        // 第一步：收集所有版本的标签信息和图片列表
        for (Long versionId : versions) {
            try {
                datasetVersion = datasetVersionMapper.getById(versionId);
                if (datasetVersion.getDataConversion() != 1) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "数据集版本{}发布未完成，无法导出", versionId);
                    return false;
                }

                // 获取图片列表
                List<String> pics = new ArrayList<>();
                List<String> picUrls = fileService.selectUrlsFromFileStatusNormalOrDeleted(datasetVersion.getDatasetId(), datasetVersion.getVersionName());
                picUrls.forEach(picUrl -> pics.add(StringUtils.substringAfter(picUrl, "/")));
                picsMap.put(datasetVersion, pics);

                // 读取版本的标签信息
                String versionFileRootPath = "dataset" + "/" + datasetVersion.getDatasetId() + "/" + "versionFile" + "/" + datasetVersion.getVersionName();
                String labelIdsPath = versionFileRootPath + "/annotation/labelsIds.text";
                String labelsPath = versionFileRootPath + "/annotation/labels.text";

                VersionLabelInfo labelInfo = new VersionLabelInfo();

                // 读取 labelsIds.text（id -> name）
                try {
                    String labelIdsString = minioUtil.readString(bucketName, labelIdsPath);
                    Map<Integer, String> raw = JSONObject.parseObject(labelIdsString, Map.class);
                    labelInfo.idLabelMap = new HashMap<>();
                    if (raw != null) {
                        for (Map.Entry<Integer, String> e : raw.entrySet()) {
                            String v = e.getValue();
                            labelInfo.idLabelMap.put(e.getKey(), v == null ? null : v.trim().toLowerCase()); // ★ 小写
                        }
                    }

                } catch (Exception e) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "读取{}的labelsIds.text失败，使用空映射", datasetVersion.getId());
                    labelInfo.idLabelMap = new HashMap<>();
                }

                // 读取 labels.text（YOLO原始顺序）
                try {
                    String labelsTextRaw = minioUtil.readString(bucketName, labelsPath);
                    labelInfo.originalYoloLabels = Arrays.stream(labelsTextRaw.replace("\r", " ").replace("\n", " ")
                                    .split(","))
                            .map(String::toLowerCase)  // 全部转小写
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());

                    // 建立原始的 labelName -> yoloIndex 映射
                    labelInfo.originalYoloIndexMap = new HashMap<>();
                    for (int i = 0; i < labelInfo.originalYoloLabels.size(); i++) {
                        labelInfo.originalYoloIndexMap.put(labelInfo.originalYoloLabels.get(i), i);
                    }
                    allAvailableLabels.addAll(labelInfo.originalYoloLabels);

                } catch (Exception e) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "读取{}的labels.text失败", datasetVersion.getId());
                    labelInfo.originalYoloLabels = new ArrayList<>();
                    labelInfo.originalYoloIndexMap = new HashMap<>();
                }

                versionLabelInfoMap.put(datasetVersion, labelInfo);

                LogUtil.info(LogEnum.BIZ_DATASET, "版本{}-{}：图片{}张，标签{}个",
                        datasetVersion.getDatasetId(), datasetVersion.getVersionName(), pics.size(), labelInfo.originalYoloLabels.size());

            } catch (Exception e) {
                if (datasetVersion != null) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "处理版本{}-{}时发生异常", datasetVersion.getDatasetId(), datasetVersion.getVersionName(), e);
                }
                return false;
            }
        }
        if (keepSet == null) {
            // 如果没有指定保留标签，则保留所有标签（按发现顺序）
            globalLabelsSet.addAll(allAvailableLabels);
        } else {
            // 按照 keepLabels 中的顺序来添加标签
            for (String keepLabel : keepLabels) {
                String normalizedKeepLabel = keepLabel == null ? null : keepLabel.trim().toLowerCase();
                if (normalizedKeepLabel != null && allAvailableLabels.contains(normalizedKeepLabel)) {
                    globalLabelsSet.add(normalizedKeepLabel);
                }
            }
        }

        // 第二步：生成全局统一的标签列表和索引映射
        List<String> globalYoloLabels = new ArrayList<>(globalLabelsSet);
        Map<String, Integer> globalYoloIndexMap = new HashMap<>();
        for (int i = 0; i < globalYoloLabels.size(); i++) {
            globalYoloIndexMap.put(globalYoloLabels.get(i), i);
        }

        LogUtil.info(LogEnum.BIZ_DATASET, "全局合并后标签数量：{}", globalYoloLabels.size());

        // 为每个版本建立标签索引映射关系（原索引 -> 新索引）
        for (Map.Entry<DatasetVersion, VersionLabelInfo> entry : versionLabelInfoMap.entrySet()) {
            VersionLabelInfo labelInfo = entry.getValue();
            labelInfo.indexMapping = new HashMap<>();

            for (Map.Entry<String, Integer> originalEntry : labelInfo.originalYoloIndexMap.entrySet()) {
                String labelName = originalEntry.getKey();
                Integer originalIndex = originalEntry.getValue();
                Integer newIndex = globalYoloIndexMap.get(labelName);
                if (keepSet != null && !keepSet.contains(labelName)) {
                    continue;
                }

                if (newIndex != null) {
                    labelInfo.indexMapping.put(originalIndex, newIndex);
                }
            }
        }

        // 第三步：复制图片文件并生成新的标注文件
        String annotationsDir = targetDir + "/annotations/";
        String imgDir = targetDir + "/images/";
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        int threadCount = Math.min(2, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger totalProcessed = new AtomicInteger(0);
        AtomicInteger totalImages = new AtomicInteger(0);

        for (Map.Entry<DatasetVersion, List<String>> entry : picsMap.entrySet()) {
            DatasetVersion key = entry.getKey();
            List<String> pics = entry.getValue();
            VersionLabelInfo labelInfo = versionLabelInfoMap.get(key);

            totalImages.addAndGet(pics.size());

            // 复制图片文件
            try {
                minioUtil.copyDir(bucketName, pics, targetDir + "/images");
                LogUtil.info(LogEnum.BIZ_DATASET, "已复制版本{}-{}的{}张图片", key.getDatasetId(), key.getVersionName(), pics.size());
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "复制图片失败", e);
                executor.shutdown();
                return false;
            }

            // 并行处理标注文件转换
            String versionFileRootPath = "dataset" + "/" + key.getDatasetId() + "/" + "versionFile" + "/" + key.getVersionName();
            String annPath = versionFileRootPath + "/annotation/";

            // 批量查询文件信息
            List<String> fileNamesToProcess = pics.stream()
                    .filter(imagePath -> !imagePath.endsWith(".txt"))
                    .map(imagePath -> {
                        String imageName = StringUtils.substringAfterLast(imagePath, "/");
                        return StringUtils.substringBeforeLast(imageName, ".");
                    })
                    .collect(Collectors.toList());

            Map<String, FileCreateDTO> fileInfoMap = new HashMap<>();
            if (!fileNamesToProcess.isEmpty()) {
                try {
                    List<FileCreateDTO> fileInfoList = FileServiceImpl.getBaseMapper().selectWidthAndHeightBatch(fileNamesToProcess, key.getDatasetId());
                    fileInfoMap = fileInfoList.stream().collect(Collectors.toMap(FileCreateDTO::getName, Function.identity()));
                } catch (Exception e) {
                    LogUtil.error(LogEnum.BIZ_DATASET, "批量查询数据库失败", e);
                    continue;
                }
            }

            final Map<String, FileCreateDTO> finalFileInfoMap = fileInfoMap;

            for (String imagePath : pics) {
                if (imagePath.endsWith(".txt")) {
                    continue;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        String imageName = StringUtils.substringAfterLast(imagePath, "/");
                        String annName = StringUtils.substringBeforeLast(imageName, ".");

                        FileCreateDTO fileCreateDTO = finalFileInfoMap.get(annName);
                        if (fileCreateDTO == null) {
                            LogUtil.warn(LogEnum.BIZ_DATASET, "文件{}在数据库中不存在，跳过处理", annName);
                            return;
                        }

                        Integer widthObj = fileCreateDTO.getWidth();
                        Integer heightObj = fileCreateDTO.getHeight();
                        if (widthObj == null || heightObj == null) {
                            LogUtil.warn(LogEnum.BIZ_DATASET, "文件{}的宽高信息缺失(width={}, height={})，跳过处理", annName, widthObj, heightObj);
                            return;
                        }
                        int width = widthObj;
                        int height = heightObj;

                        // 读取原始标注
                        JSONArray objects;
                        boolean hasOriginalAnnotations = false;
                        try {
                            objects = JSONObject.parseArray(minioUtil.readString(bucketName, annPath + annName));
                            hasOriginalAnnotations = objects != null && !objects.isEmpty();
                        } catch (Exception e) {
                            LogUtil.error(LogEnum.BIZ_DATASET, "读取标注文件{}失败", annName, e);
                            return;
                        }

                        // 转换标注格式 - 根据格式类型分别处理
                        StringBuffer content = new StringBuffer();
                        String format = key.getFormat();
                        
                        for (Object object : objects) {
                            JSONObject jsonObject = (JSONObject) object;
                            String rawCategory = jsonObject.getString("category_id");

                            String normName;
                            if (StringUtils.isNumeric(rawCategory)) {
                                try {
                                    Integer cid = Integer.valueOf(rawCategory);
                                    String mappedName = labelInfo.idLabelMap.get(cid); // 已经是小写
                                    normName = (mappedName == null ? String.valueOf(rawCategory) : mappedName);
                                } catch (NumberFormatException ignore) {
                                    normName = rawCategory;
                                }
                            } else {
                                normName = rawCategory;
                            }
                            normName = normName == null ? "" : normName.trim().toLowerCase(); // 再保险

                            if (keepSet != null && !keepSet.contains(normName)) {
                                continue;
                            }

                            Integer newIndex = globalYoloIndexMap.get(normName);
                            if (newIndex == null) {
                                LogUtil.warn(LogEnum.BIZ_DATASET, "标签'{}'未出现在全局标签集中，跳过该对象", normName);
                                continue;
                            }

                            // 根据格式类型分别处理
                            if ("Segment-YOLO".equals(format)) {
                                // 处理语义分割数据
                                JSONArray segmentation = jsonObject.getJSONArray("segmentation");
                                if (segmentation == null || segmentation.size() < 3) {
                                    continue;
                                }

                                content.append(newIndex);
                                // 遍历所有分割点进行归一化
                                for (int j = 0; j < segmentation.size(); j++) {
                                    JSONArray point = segmentation.getJSONArray(j);
                                    if (point != null && point.size() >= 2) {
                                        double x = point.getDouble(0);
                                        double y = point.getDouble(1);
                                        
                                        // 归一化到[0,1]
                                        double normalizedX = x / width;
                                        double normalizedY = y / height;
                                        
                                        content.append(" ").append(normalizedX).append(" ").append(normalizedY);
                                    }
                                }
                                content.append("\n");
                            } else if ("YOLO".equals(format)) {
                                // 处理YOLO格式的边界框数据
                                JSONArray jsonArray = (JSONArray) jsonObject.get("bbox");
                                BigDecimal[] bbox = new BigDecimal[4];
                                for (int j = 0; j < 4; j++) {
                                    bbox[j] = new BigDecimal(jsonArray.get(j).toString());
                                }

                                double[] newBbox = bboxCocoYolo(bbox[0].doubleValue(), bbox[1].doubleValue(),
                                        bbox[2].doubleValue(), bbox[3].doubleValue(), width, height);

                                content.append(newIndex).append(" ")
                                        .append(newBbox[0]).append(" ")
                                        .append(newBbox[1]).append(" ")
                                        .append(newBbox[2]).append(" ")
                                        .append(newBbox[3]).append("\n");
                            }
                        }

                        // 写入新的标注文件
                        try {

                            // 只有当原始标注不为空但处理后为空时才删除图片
                            if (content.length() == 0) {
                                if (hasOriginalAnnotations) {
                                    // 原始标注不为空，但经过筛选后变成空 → 删除图片
                                    String imgToDel = targetDir + "images/" + imageName;
                                    minioUtil.del(bucketName, imgToDel);
                                    LogUtil.debug(LogEnum.BIZ_DATASET, "图片{}的所有标注都被过滤，已删除图片", imageName);
                                    return;
                                } else {
                                    // 原始标注为空 → 保留图片，生成空的标注文件
                                    LogUtil.debug(LogEnum.BIZ_DATASET, "图片{}原始无标注，保留图片并生成空标注文件", imageName);
                                    minioUtil.writeString(bucketName, annotationsDir + annName + ".txt", "");
                                    return;
                                }
                            }
                            minioUtil.writeString(bucketName, annotationsDir + annName + ".txt", content.toString());
                            int processed = totalProcessed.incrementAndGet();
                            if (processed % 100 == 0) {
                                LogUtil.info(LogEnum.BIZ_DATASET, "已处理标注文件：{}/{}", processed, totalImages.get());
                            }
                        } catch (Exception e) {
                            LogUtil.error(LogEnum.BIZ_DATASET, "写入标注文件{}失败", annName, e);
                        }

                    } catch (Exception e) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "处理标注文件时发生异常", e);
                    }
                }, executor);

                futures.add(future);
            }
        }

        // 等待所有标注文件处理完成
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            LogUtil.info(LogEnum.BIZ_DATASET, "所有标注文件处理完成，共处理{}个文件", totalProcessed.get());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "等待标注文件处理完成时发生异常", e);
            return false;
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

        // 第四步：生成全局的classes.txt
        try {
            String classesContent = String.join("\n", globalYoloLabels);
            minioUtil.writeString(bucketName, targetDir + "/annotations" + "/classes.txt", classesContent);
            LogUtil.info(LogEnum.BIZ_DATASET, "已生成全局classes.txt，包含{}个类别", globalYoloLabels.size());
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "生成classes.txt失败", e);
            return false;
        }

        // 第五步：生成data.yaml文件
        try {
            StringBuilder yamlContent = new StringBuilder();
            // yamlContent.append("train: images\n");
            // yamlContent.append("val: images\n");
            // yamlContent.append("nc: ").append(globalYoloLabels.size()).append("\n");
            yamlContent.append("names:\n");
            for (int i = 0; i < globalYoloLabels.size(); i++) {
                yamlContent.append("  ").append(i).append(": ").append(globalYoloLabels.get(i)).append("\n");
            }

            minioUtil.writeString(bucketName, targetDir + "/data.yaml", yamlContent.toString());
            LogUtil.info(LogEnum.BIZ_DATASET, "已生成data.yaml文件");

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "生成data.yaml失败", e);
            return false;
        }

        long totalTime = System.currentTimeMillis() - startTime;
        LogUtil.info(LogEnum.BIZ_DATASET, "多版本数据集合并完成 - 总耗时:{}ms, 合并版本数:{}, 图片总数:{}, 全局类别数:{}",
                totalTime, versions.size(), totalImages.get(), globalYoloLabels.size());

        return true;
    }

    // 辅助类：存储版本标签信息
    private static class VersionLabelInfo {
        Map<Integer, String> idLabelMap;           // id -> labelName
        List<String> originalYoloLabels;           // 原始YOLO标签顺序
        Map<String, Integer> originalYoloIndexMap; // labelName -> 原始索引
        Map<Integer, Integer> indexMapping;        // 原始索引 -> 新的全局索引
    }

    // COCO到YOLO格式转换方法（需要从原代码中引用）
    private double[] bboxCocoYolo(double x, double y, double w, double h, int imgWidth, int imgHeight) {
        double centerX = (x + w / 2.0) / imgWidth;
        double centerY = (y + h / 2.0) / imgHeight;
        double width = w / imgWidth;
        double height = h / imgHeight;
        return new double[]{centerX, centerY, width, height};
    }

    @Override
    public DatasetSplitResult datasetSplit(String sourceDir, String targetDir, String splitRatio){
        String[] split = splitRatio.split("-");
        List<Integer> datasetCutRadio = Arrays.stream(split).map(Integer::parseInt).collect(Collectors.toList());
        Integer total = datasetCutRadio.stream().reduce(0, Integer::sum);
        String cutRootRadio = datasetCutRadio.get(0) + "-" + datasetCutRadio.get(1) + "-" + datasetCutRadio.get(2);
        DatasetSplitResult result = new DatasetSplitResult();
        try {
            List<String> getSplitDir = minioUtil.listObjectsInFolder(bucketName, targetDir)
                    .stream()
                    .filter(s -> s.contains(splitRatio))
                    .collect(Collectors.toList());
            if (getSplitDir.isEmpty()) {
                final int threadPoolSize = 30;
                ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

                try {
                    LogUtil.info(LogEnum.BIZ_DATASET,"未找到指定比例的切分目录，开始新的切分流程...");

                    LogUtil.info(LogEnum.BIZ_DATASET,"处理YOLO格式数据集的切分...");

                            minioUtil.splitImageFiles(bucketName,
                                    sourceDir + "images",
                                    sourceDir + "annotations",
                                    targetDir  + cutRootRadio + "/train/images",
                                    targetDir  + cutRootRadio + "/test/images",
                                    targetDir  + cutRootRadio + "/validation/images",
                                    targetDir  + cutRootRadio + "/train/annotations",
                                    targetDir  + cutRootRadio + "/test/annotations",
                                    targetDir  + cutRootRadio + "/validation/annotations",
                                    datasetCutRadio.get(0) * 1.0 / total,
                                    datasetCutRadio.get(1) * 1.0 / total,
                                    datasetCutRadio.get(2) * 1.0 / total);


                    result.setTrainImagesPath(targetDir + cutRootRadio + "/train/images");
                    result.setTestImagesPath(targetDir + cutRootRadio + "/test/images");
                    result.setValImagesPath(targetDir + cutRootRadio + "/validation/images");
                    result.setTrainAnnotationsPath(targetDir + cutRootRadio + "/train/annotations");
                    result.setTestAnnotationsPath(targetDir + cutRootRadio + "/test/annotations");
                    result.setValAnnotationsPath(targetDir + cutRootRadio + "/validation/annotations");

                } catch (Exception e) {
                    LogUtil.info(LogEnum.BIZ_DATASET,"切分数据集过程中发生错误: ", e);
                    // 如果是子线程的错误，会被包装在ExecutionException中，最终被捕获
                    throw new RuntimeException("切分数据集出现错误~", e);
                } finally {
                    if (!executor.isShutdown()) {
                        LogUtil.info(LogEnum.BIZ_DATASET,"正在关闭 datasetCut 的线程池...");
                        executor.shutdown();
                    }
                }
            } else {
                LogUtil.info(LogEnum.BIZ_DATASET,"指定比例 {} 的切分目录已存在，跳过切分。", splitRatio);
                result.setTrainImagesPath(targetDir + cutRootRadio + "/train/images");
                result.setTestImagesPath(targetDir + cutRootRadio + "/test/images");
                result.setValImagesPath(targetDir + cutRootRadio + "/validation/images");
                result.setTrainAnnotationsPath(targetDir + cutRootRadio + "/train/annotations");
                result.setTestAnnotationsPath(targetDir + cutRootRadio + "/test/annotations");
                result.setValAnnotationsPath(targetDir + cutRootRadio + "/validation/annotations");
            }
        } catch (Exception e) {
            throw new RuntimeException("路径查询存在错误~~", e);
        }

        return result;
    }


    public Integer countVersionsByDatasetIds(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return 0;
        }
        return datasetVersionMapper.countVersionsByDatasetIds(datasetIds);
    }

    public List<DatasetVersion> getVersionsByDatasetId(Long datasetId) {
        QueryWrapper<DatasetVersion> wrapper = new QueryWrapper<>();
        wrapper.eq("dataset_id", datasetId);
        wrapper.ne("deleted", 1); // 如果有deleted字段的话
        return list(wrapper);
    }

    public Integer calculateDatasetTotalFiles(Long datasetId) {
        try {
            // 获取用户信息
            Long currentUserId = userContextService.getCurUser().getId();
            String roleName = userContextService.getCurUser().getRoles().get(0).getName();
            boolean isAdminOrManager = "管理员".equals(roleName) || "管理人员".equals(roleName);
            // 检查参数
            if (datasetId == null) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "数据集ID为空，无法计算总图片数");
                return 0;
            }

            // 批量查询该数据集的所有版本（一次数据库查询）
            LambdaQueryWrapper<DatasetVersion> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DatasetVersion::getDatasetId, datasetId)
                    .eq(DatasetVersion::getDeleted, false)
                    .likeRight(DatasetVersion::getVersionName, "V")
                    .and(!isAdminOrManager, w -> w
                            .eq(DatasetVersion::getCreateUserId, currentUserId))  // 自己创建的版本
                    .select(DatasetVersion::getDatasetId, DatasetVersion::getVersionName);

            List<DatasetVersion> allVersions = datasetVersionMapper.selectList(queryWrapper);

            if (CollectionUtils.isEmpty(allVersions)) {
                //LogUtil.info(LogEnum.BIZ_DATASET, "数据集{}没有找到任何版本", datasetId);
                return 0;
            }

            int totalFiles = 0;
            int successCount = 0;

            // 遍历所有版本，读取 annoFileCount.text
            for (DatasetVersion version : allVersions) {
                if (version == null || StringUtils.isBlank(version.getVersionName())) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "数据集{}的版本信息不完整", datasetId);
                    continue;
                }

                try {
                    String filePath = "dataset/" + datasetId + "/versionFile/"
                            + version.getVersionName() + "/annotation/annoFileCount.text";

                    String fileCountStr = minioUtil.readString(bucketName, filePath);

                    if (StringUtils.isNotBlank(fileCountStr)) {
                        int versionFileCount = Integer.parseInt(fileCountStr.trim());
                        totalFiles += versionFileCount;
                        successCount++;
                    }
                } catch (NumberFormatException e) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "数据集{}版本{}的annoFileCount.text文件内容不是有效数字: {}",
                            datasetId, version.getVersionName(), e.getMessage());
                } catch (Exception e) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "读取数据集{}版本{}的annoFileCount.text文件时出错: {}",
                            datasetId, version.getVersionName(), e.getMessage());
                }
            }

            return totalFiles;
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "计算数据集{}的总文件数时发生异常: {}", datasetId, e.getMessage(), e);
            return 0;
        }
    }

    public Integer calculateDatasetTotalFilesForSystem(Long datasetId) {
        try {
            if (datasetId == null) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "数据集ID为空，无法计算总图片数");
                return 0;
            }

            LambdaQueryWrapper<DatasetVersion> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DatasetVersion::getDatasetId, datasetId)
                    .eq(DatasetVersion::getDeleted, false)
                    .likeRight(DatasetVersion::getVersionName, "V")
                    .select(DatasetVersion::getDatasetId, DatasetVersion::getVersionName);

            List<DatasetVersion> allVersions = datasetVersionMapper.selectList(queryWrapper);
            if (CollectionUtils.isEmpty(allVersions)) {
                return 0;
            }

            int totalFiles = 0;
            for (DatasetVersion version : allVersions) {
                if (version == null || StringUtils.isBlank(version.getVersionName())) {
                    continue;
                }

                try {
                    String filePath = "dataset/" + datasetId + "/versionFile/"
                            + version.getVersionName() + "/annotation/annoFileCount.text";
                    String fileCountStr = minioUtil.readString(bucketName, filePath);
                    if (StringUtils.isNotBlank(fileCountStr)) {
                        totalFiles += Integer.parseInt(fileCountStr.trim());
                    }
                } catch (NumberFormatException e) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "数据集{}版本{}的annoFileCount.text文件内容不是有效数字: {}",
                            datasetId, version.getVersionName(), e.getMessage());
                } catch (Exception e) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "读取数据集{}版本{}的annoFileCount.text文件时出错: {}",
                            datasetId, version.getVersionName(), e.getMessage());
                }
            }

            return totalFiles;
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "计算数据集{}的总文件数时发生异常: {}", datasetId, e.getMessage(), e);
            return 0;
        }
    }


    /**
     * 异步处理数据集
     */
    @Override
    public void processDatasetAsync(String taskId, DatasetMergeRequest request) {
        String targetDir = null;
        try {
            if (taskStatusService.isTaskCancelled(taskId)) {
                return;
            }
            // 更新状态为处理中
            taskStatusService.updateTaskStatus(taskId, TaskStatus.PROCESSING, null, null);
            taskStatusService.updateTaskProgress(taskId, 10);
            // 确保目标目录以/结尾
            targetDir = request.getTargetDir();
            if (!targetDir.endsWith("/")) {
                targetDir += "/";
            }
            minioUtil.delFolder(bucketName, targetDir);
            if (!request.getIsSplit()) {
                // 只合并，不切分
                taskStatusService.updateTaskProgress(taskId, 50);
                // 检查是否被取消
                if (taskStatusService.isTaskCancelled(taskId)) {
                    return;
                }
                boolean success = datasetAssembleFromMultipleVersion(
                        request.getVersions(), targetDir, request.getKeepLabels());
                // 检查是否被取消
                if (taskStatusService.isTaskCancelled(taskId)) {
                    return;
                }
                taskStatusService.updateTaskProgress(taskId, 100);
                taskStatusService.updateTaskStatus(taskId,
                        success ? TaskStatus.COMPLETED : TaskStatus.FAILED,
                        success ? null : "合并失败",
                        success);

            } else {
                // 合并 + 切分
                taskStatusService.updateTaskProgress(taskId, 30);
                // 检查是否被取消
                if (taskStatusService.isTaskCancelled(taskId)) {
                    return;
                }
                datasetAssembleFromMultipleVersion(
                        request.getVersions(), targetDir, request.getKeepLabels());
                taskStatusService.updateTaskProgress(taskId, 70);
                // 检查是否被取消
                if (taskStatusService.isTaskCancelled(taskId)) {
                    return;
                }
                DatasetSplitResult datasetSplitResult =datasetSplit(
                        targetDir, targetDir, request.getSplitRatio());
                // 检查是否被取消
                if (taskStatusService.isTaskCancelled(taskId)) {
                    return;
                }
                taskStatusService.updateTaskProgress(taskId, 100);
                taskStatusService.updateTaskStatus(taskId, TaskStatus.COMPLETED, null, datasetSplitResult);
            }

        } catch (Exception e) {
            taskStatusService.updateTaskStatus(taskId, TaskStatus.FAILED, e.getMessage(), null);
            minioUtil.delFolder(bucketName,targetDir);
        }
    }

    @Override
    public void processQuantizedAsync(String taskId, QuantizedMergeRequest request) {
        String targetDir = null;
        try {
            if (taskStatusService.isTaskCancelled(taskId)) {
                return;
            }
            // 更新状态为处理中
            taskStatusService.updateTaskStatus(taskId, TaskStatus.PROCESSING, null, null);
            taskStatusService.updateTaskProgress(taskId, 10);
            // 确保目标目录以/结尾
            targetDir = request.getTargetDir();
            if (!targetDir.endsWith("/")) {
                targetDir += "/";
            }
            minioUtil.delFolder(bucketName, targetDir);
                // 只合并，不切分
            taskStatusService.updateTaskProgress(taskId, 50);
                // 检查是否被取消
            if (taskStatusService.isTaskCancelled(taskId)) {
                    return;
                }
            boolean success = quantizedFromMultipleVersion(request.getVersions(), targetDir,request.getTotal());
                // 检查是否被取消
            if (taskStatusService.isTaskCancelled(taskId)) {
                    return;
                }
            taskStatusService.updateTaskProgress(taskId, 100);
            taskStatusService.updateTaskStatus(taskId,
                        success ? TaskStatus.COMPLETED : TaskStatus.FAILED,
                        success ? null : "量化图片合并失败",
                        success);


        } catch (Exception e) {
            taskStatusService.updateTaskStatus(taskId, TaskStatus.FAILED, e.getMessage(), null);
            minioUtil.delFolder(bucketName,targetDir);
        }
    }

    public boolean quantizedFromMultipleVersion(List<Long> versions, String targetDir, Long total) {
        try {
            Map<DatasetVersion, List<String>> picsMap = new HashMap<>();
            int totalImageCount = 0;

            // 获取所有版本的图片信息
            for (Long versionId : versions) {
                DatasetVersion datasetVersion = datasetVersionMapper.getById(versionId);
                List<String> pics = new ArrayList<>();
                List<String> picUrls = fileService.selectUrlsFromFileStatusNormalOrDeleted(datasetVersion.getDatasetId(), datasetVersion.getVersionName());

                picUrls.forEach(picUrl -> pics.add(StringUtils.substringAfter(picUrl, "/")));
                picsMap.put(datasetVersion, pics);
                totalImageCount += pics.size();

                LogUtil.info(LogEnum.BIZ_DATASET, "版本{}-{}共有{}张图片",
                        datasetVersion.getDatasetId(), datasetVersion.getVersionName(), pics.size());
            }

            if (totalImageCount == 0) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "没有找到任何图片");
                return false;
            }

            if (total > totalImageCount) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "总抽取数量{}超过了可用图片数量{}", total, totalImageCount);
                total = (long) totalImageCount;
            }

            AtomicInteger totalCopied = new AtomicInteger(0);
            Set<String> allSelectedPicsSet = new HashSet<>(); // 使用Set避免重复

            // 按比例从每个版本抽取图片
            for (Map.Entry<DatasetVersion, List<String>> entry : picsMap.entrySet()) {
                DatasetVersion datasetVersion = entry.getKey();
                List<String> pics = entry.getValue();

                // 计算当前版本应该抽取的图片数量
                double proportion = (double) pics.size() / totalImageCount;
                int currentVersionCount = (int) Math.round(proportion * total);

                // 确保不超过该版本的实际图片数量
                currentVersionCount = Math.min(currentVersionCount, pics.size());

                if (currentVersionCount > 0) {
                    // 随机抽取图片
                    List<String> selectedPics = selectRandomImages(pics, currentVersionCount);
                    LogUtil.info(LogEnum.BIZ_DATASET, "期望抽取{}张，实际选择{}张图片",
                            currentVersionCount, selectedPics.size());
                    allSelectedPicsSet.addAll(selectedPics);
                    totalCopied.addAndGet(selectedPics.size());

                    LogUtil.info(LogEnum.BIZ_DATASET, "版本{}-{}按比例{}抽取{}张图片",
                            datasetVersion.getDatasetId(), datasetVersion.getVersionName(),
                            String.format("%.2f", proportion), selectedPics.size());
                }
            }

            // 如果由于四舍五入导致总数不够，随机补充
            if (totalCopied.get() < total) {
                int shortage = Math.toIntExact(total - totalCopied.get());
                List<String> allRemainingPics = new ArrayList<>();

                // 收集所有未被选中的图片
                for (Map.Entry<DatasetVersion, List<String>> entry : picsMap.entrySet()) {
                    List<String> pics = entry.getValue();
                    pics.stream()
                            .filter(pic -> !allSelectedPicsSet.contains(pic))
                            .forEach(allRemainingPics::add);
                }

                if (shortage <= allRemainingPics.size()) {
                    List<String> additionalPics = selectRandomImages(allRemainingPics, shortage);
                    allSelectedPicsSet.addAll(additionalPics);
                    totalCopied.addAndGet(additionalPics.size());

                    LogUtil.info(LogEnum.BIZ_DATASET, "随机补充{}张图片以达到目标数量", shortage);
                }
            }

            // 复制选中的图片到目标目录
            if (!allSelectedPicsSet.isEmpty()) {
                minioUtil.copyDir(bucketName, new ArrayList<>(allSelectedPicsSet), targetDir);
                LogUtil.info(LogEnum.BIZ_DATASET, "成功复制{}张图片到目标目录", allSelectedPicsSet.size());
            }

            LogUtil.info(LogEnum.BIZ_DATASET, "从{}个版本中按比例抽取图片完成，目标数量：{}，实际数量：{}",
                    versions.size(), total, totalCopied.get());

            return true;

        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "按比例抽取图片失败", e);
            return false;
        }
    }

    /**
     * 从图片列表中随机选择指定数量的图片
     */
    private List<String> selectRandomImages(List<String> pics, int count) {
        if (count >= pics.size()) {
            return new ArrayList<>(pics);
        }
        List<String> shuffled = new ArrayList<>(pics);
        Collections.shuffle(shuffled);
        List<String> result = shuffled.subList(0, count);
        LogUtil.debug(LogEnum.BIZ_DATASET, "从{}张图片中选择{}张", pics.size(), result.size());
        return result;
    }

    @Override
    public List<DatasetVersionDetailVO> batchGetVersionsLabelCountMap(List<Long> versionIds, List<String> labelNames) {
        if (CollectionUtils.isEmpty(versionIds)) {
            return Collections.emptyList();
        }
        List<DatasetVersionDetailVO> resultList = new ArrayList<>();
        List<DatasetVersion> datasetVersions = datasetVersionMapper.selectBatchIds(versionIds);
        for (DatasetVersion datasetVersion : datasetVersions) {
            DatasetVersionDetailVO detailVO = new DatasetVersionDetailVO();

            Map<String, Object> imageSizeStats = new HashMap<>();
            Integer fileCount = 0;
            Long totalFileSize = 0L;
            String imageSizeStatsPath = datasetVersion.getVersionUrl() + "/annotation/imageSizeStats.text";

            // 检查文件是否存在
            if (minioUtil.doesObjectExistSilent(bucketName, imageSizeStatsPath)) {
                try {
                    String imageSizeStatsString = minioUtil.readString(bucketName, imageSizeStatsPath);
                    imageSizeStats = JSONObject.parseObject(imageSizeStatsString, Map.class);

                    // 读取文件数量
                    if (imageSizeStats.containsKey("fileCount")) {
                        fileCount = (Integer) imageSizeStats.get("fileCount");
                    }

                    // 读取总文件大小（字节）
                    if (imageSizeStats.containsKey("totalSizeBytes")) {
                        totalFileSize = Long.valueOf(imageSizeStats.get("totalSizeBytes").toString());
                    }

                } catch (Exception e) {
                    // 如果读取失败，使用默认值或数据库查询作为后备方案
                    LogUtil.error(LogEnum.BIZ_DATASET, "Read imageSizeStats.text failed:{}", e);
                    // 后备方案：从数据库查询文件数量
                    fileCount = datasetVersionFileService.selectDatasetVersionFileCount(datasetVersion);
                    totalFileSize = 1024L * 1024L * 100L; // 默认值：100MB
                }
            } else {
                // 文件不存在时的处理
                LogUtil.warn(LogEnum.BIZ_DATASET, "imageSizeStats.text file does not exist: {}", imageSizeStatsPath);
                // 后备方案：从数据库查询文件数量
                fileCount = datasetVersionFileService.selectDatasetVersionFileCount(datasetVersion);
                totalFileSize = 1024L * 1024L * 100L; // 默认值：100MB
            }

            Map<String, Integer> labelCountMap = new HashMap<>();
            String labelCountPath = datasetVersion.getVersionUrl() + "/annotation/labelsCount.text";

            // 检查文件是否存在
            if (minioUtil.doesObjectExistSilent(bucketName, labelCountPath)) {
                try {
                    String labelIdsString = minioUtil.readString(bucketName, labelCountPath);
                    labelCountMap = JSONObject.parseObject(labelIdsString, Map.class);
                } catch (Exception e) {
                    labelCountMap = new HashMap<>();
                    LogUtil.error(LogEnum.BIZ_DATASET, "ReadJson is failed:{}", e);
                }
            } else {
                // 文件不存在时的处理
                LogUtil.warn(LogEnum.BIZ_DATASET, "labelsCount.text file does not exist: {}", labelCountPath);
                labelCountMap = new HashMap<>();
            }

            Integer importableImageCount = 0;
            try {
                Map<String, Object> stringObjectMap = quickCalculateImportStats(datasetVersion.getId(), labelNames);
                importableImageCount = (Integer) stringObjectMap.get("totalImportedFiles");
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "CalculateImportStats is failed:{}", e);
                importableImageCount = 0;
            }
            detailVO.setImportableImageCount(importableImageCount);
            detailVO.setLabelCountMap(labelCountMap);
            detailVO.setId(datasetVersion.getId());
            detailVO.setImageCount(fileCount);
            detailVO.setTotalFileSize(totalFileSize);

            resultList.add(detailVO);
        }
        return resultList;
    }
    /**
     * 统计数据集组中公开版本的总数
     * @param datasetIds 数据集ID列表
     * @return 公开版本总数
     */
    public Integer countPublicVersionsByDatasetIds(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return 0;
        }

        return datasetVersionMapper.countPublicVersionsByDatasetIds(datasetIds);
    }

    /**
     * 统计数据集组中公开版本的总文件数
     * @param datasetIds 数据集ID列表
     * @return 总文件数
     */
    public Long calculatePublicDatasetTotalFiles(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return 0L;
        }

        long totalFiles = 0L;

        // 获取所有公开版本
        LambdaQueryWrapper<DatasetVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(DatasetVersion::getDatasetId, datasetIds)
                .eq(DatasetVersion::getIsPublic, 1)  // 只统计公开版本
                .eq(DatasetVersion::getDeleted, false)
                .likeRight(DatasetVersion::getVersionName, "V")
                .select(DatasetVersion::getDatasetId, DatasetVersion::getVersionName);

        List<DatasetVersion> publicVersions = datasetVersionMapper.selectList(wrapper);

        for (DatasetVersion version : publicVersions) {
            try {
                String filePath = "dataset/" + version.getDatasetId() + "/versionFile/"
                        + version.getVersionName() + "/annotation/annoFileCount.text";
                String fileCountStr = minioUtil.readString(bucketName, filePath);
                if (StringUtils.isNotBlank(fileCountStr)) {
                    totalFiles += Integer.parseInt(fileCountStr.trim());
                }
            } catch (Exception e) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "读取公开版本文件计数失败: {}-{}",
                        version.getDatasetId(), version.getVersionName());
            }
        }

        return totalFiles;
    }



    @Override
    public Map<String, Object> quickCalculateImportStats(Long sourceVersionId, List<String> targetLabelNames) throws Exception {
        DatasetVersion version = datasetVersionMapper.selectById(sourceVersionId);
        String labelCombinationStatsContent = minioUtil.readString(bucketName, version.getVersionUrl() + "/annotation/labelCombinationStats.text");
        Map<String, Object> result = new HashMap<>();

        try {
            // 解析标签组合统计数据
            Map<String, Integer> labelCombinationStats = JSON.parseObject(labelCombinationStatsContent,
                    new TypeReference<Map<String, Integer>>() {});

            if (labelCombinationStats == null || labelCombinationStats.isEmpty()) {
                result.put("error", "标签组合统计数据为空");
                return result;
            }

            Map<String, String> lowerToOriginalTargetLabel = targetLabelNames.stream()
                    .collect(Collectors.toMap(
                            String::toLowerCase,
                            label -> label,
                            (existing, replacement) -> existing
                    ));
            // 转换目标标签为小写的Set，便于快速、不区分大小写地查找
            Set<String> lowerTargetLabelSet = lowerToOriginalTargetLabel.keySet();


            // 统计计数器
            int totalFiles = labelCombinationStats.values().stream().mapToInt(Integer::intValue).sum();
            int matchedFiles = 0;
            int emptyLabelFiles = labelCombinationStats.getOrDefault("", 0); // 空标签组合的文件数
            int unmatchedFiles = 0;

            // 统计每个目标标签的匹配情况 (使用原始标签名作为key)
            Map<String, Integer> targetLabelMatchCount = new HashMap<>();
            targetLabelNames.forEach(label -> targetLabelMatchCount.put(label, 0));

            for (Map.Entry<String, Integer> entry : labelCombinationStats.entrySet()) {
                String labelCombination = entry.getKey();
                int fileCount = entry.getValue();

                if (labelCombination == null || labelCombination.isEmpty()) {
                    // 空标签组合已经在上面统计过了
                    continue;
                } else {
                    Set<String> combinationLabelsLower = Arrays.stream(labelCombination.split(","))
                            .map(String::toLowerCase)
                            .collect(Collectors.toSet());

                    boolean hasTargetLabel = false;
                    // 检查该组合是否包含目标标签
                    for (String comboLabel : combinationLabelsLower) {
                        if (lowerTargetLabelSet.contains(comboLabel)) {
                            hasTargetLabel = true;
                            String originalTargetLabel = lowerToOriginalTargetLabel.get(comboLabel);
                            targetLabelMatchCount.compute(originalTargetLabel, (k, v) -> (v == null) ? fileCount : v + fileCount);
                        }
                    }

                    if (hasTargetLabel) {
                        matchedFiles += fileCount;
                    } else {
                        unmatchedFiles += fileCount;
                    }
                }
            }

            // 总导入文件数
            int totalImportedFiles = matchedFiles + emptyLabelFiles;

            // 构建结果
            result.put("totalFiles", totalFiles);
            result.put("totalImportedFiles", totalImportedFiles);
            result.put("matchedFiles", matchedFiles);
            result.put("emptyLabelFiles", emptyLabelFiles);
            result.put("unmatchedFiles", unmatchedFiles);
            result.put("importSuccessRate", String.format("%.2f%%", (totalImportedFiles * 100.0 / totalFiles)));
            result.put("targetLabelNames", targetLabelNames);
            result.put("targetLabelMatchCount", targetLabelMatchCount);

            // 简化的摘要信息
            Map<String, Object> summary = new HashMap<>();
            summary.put("result", String.format("总计 %d 个文件，其中 %d 个会被导入（匹配标签 %d 个，空标签 %d 个）",
                    totalFiles, totalImportedFiles, matchedFiles, emptyLabelFiles));
            summary.put("importRate", result.get("importSuccessRate").toString());
            result.put("summary", summary);

        } catch (Exception e) {
            result.put("error", "计算异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 语义分割YOLO格式导出方法
     * 与普通YOLO格式类似，但专门处理语义分割的标注数据
     */
    public void zipSegmentYoloLayoutDirectly(
            String bucketName,
            String annoDirPrefix,          // 例如 "dataset/123/versionFile/v1/Segment-YOLO/annotations/"
            String yamlObjectKey,          // 例如 "dataset/123/versionFile/v1/Segment-YOLO/data.yaml"
            List<String> imageObjectKeys,  // 图片对象键列表
            String zipFileName,            // 目标 ZIP 文件名
            String targetDir,              // 例如 "dataset/123/versionFile/v1/Segment-YOLO"
            Long datasetVersionId,
            Long datasetId) {

        // 已完成或正在进行则不重复导出
        String flag = toZipFlag.get(datasetVersionId);
        if (flag != null && !"failed".equals(flag)) {
            return;
        }

        toZipFlag.put(datasetVersionId, "going");
        boolean ok = false;

        try {
            // 构建实际的NFS文件系统路径
            String zipFilePath = targetDir + "/" + zipFileName;
            System.out.println("Segment-YOLO zipFilePath = " + zipFilePath);
            
            // 确保目标目录存在
            java.io.File zipDir = new File(zipFilePath).getParentFile();
            if (!zipDir.exists()) {
                zipDir.mkdirs();
            }

            // 使用FileUtil的语义分割YOLO导出方法
            ok = fileUtil.createYoloZipDirectly(annoDirPrefix, yamlObjectKey,
                    imageObjectKeys, zipFilePath, null);

            toZipFlag.put(datasetVersionId, ok ? "success" : "failed");

            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(),
                    ok ? "数据集导出-目标分割YOLO打包成功" : "数据集导出-目标分割YOLO打包失败",
                    ok ? DatasetOperationEvent.EventType.INFO : DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT
            );

        } catch (Exception e) {
            toZipFlag.put(datasetVersionId, "failed");
            datasetOperationEventService.createAndInsertEvent(
                    datasetId, new Date(), "数据集导出-目标分割YOLO打包失败",
                    DatasetOperationEvent.EventType.ERROR,
                    DatasetOperationEvent.OperationType.DATA_EXPORT
            );
            LogUtil.error(LogEnum.BIZ_DATASET, "zip segment yolo layout directly error", e);
        }
    }

}
