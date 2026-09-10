package org.dubhe.data.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.ArrayUtils;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.utils.StringUtils;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.statemachine.dto.StateChangeDTO;
import org.dubhe.data.constant.AnnotateTypeEnum;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.constant.DatatypeEnum;
import org.dubhe.data.constant.FileTypeEnum;
import org.dubhe.data.dao.DatasetVersionFileMapper;
import org.dubhe.data.domain.bo.FileUploadBO;
import org.dubhe.data.domain.dto.DatasetVersionFileDTO;
import org.dubhe.data.domain.entity.*;
import org.dubhe.data.domain.vo.DatasetVersionFileLabelStatVO;
import org.dubhe.data.machine.constant.FileStateCodeConstant;
import org.dubhe.data.machine.constant.FileStateMachineConstant;
import org.dubhe.data.machine.utils.StateMachineUtil;
import org.dubhe.data.service.DataFileAnnotationService;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.DatasetVersionFileService;
import org.dubhe.data.service.FileService;
import org.dubhe.data.util.GeneratorKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dubhe.data.constant.Constant.DEFAULT_VERSION;
import static org.dubhe.data.machine.constant.FileStateCodeConstant.ANNOTATION_COMPLETE_FILE_STATE;
import static org.dubhe.data.machine.constant.FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE;

/**
 * @description 数据集版本文件关系 服务实现类
 * @date 2020-05-14
 */
@Service
public class DatasetVersionFileServiceImpl extends ServiceImpl<DatasetVersionFileMapper, DatasetVersionFile>
        implements DatasetVersionFileService, IService<DatasetVersionFile> {

    @Resource
    private DatasetVersionFileMapper datasetVersionFileMapper;

    @Resource
    @Lazy
    private DatasetService datasetService;

    @Resource
    @Lazy
    private FileService fileService;

    @Autowired
    private GeneratorKeyUtil generatorKeyUtil;

    @Autowired
    private DataFileAnnotationService dataFileAnnotationService;

    @Autowired
    private DataFileAnnotationServiceImpl dataFileAnnotationServiceImpl;

    @Autowired
    private UserContextService userContextService;


    /**
     * 查询数据集指定版本文件列表
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @return List<DatasetVersionFile> 版本文件列表
     */
    @Override
    public List<DatasetVersionFile> findByDatasetIdAndVersionName(Long datasetId, String versionName) {
        return datasetVersionFileMapper.findByDatasetIdAndVersionName(datasetId, versionName);
    }

    @Override
    public List<DatasetVersionFile> findByDatasetIdAndVersionNameWithoutStatus(Long datasetId, String versionName) {
        return datasetVersionFileMapper.findByDatasetIdAndVersionNameWithoutStatus(datasetId, versionName);
    }

    /**
     * 批量写入数据集版本文件
     *
     * @param data 版本文件列表
     */
    @Override
    public void insertList(List<DatasetVersionFile> data) {
        LogUtil.debug(LogEnum.BIZ_DATASET, "save dataset version files start, file size {}", data.size());
        Long start = System.currentTimeMillis();
        data.stream().forEach(datasetVersionFile -> {
            if (null == datasetVersionFile.getAnnotationStatus()) {
                datasetVersionFile.setAnnotationStatus(NOT_ANNOTATION_FILE_STATE);
            }
        });
        Queue<Long> dataFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_VERSION_FILE, data.size());
        for (DatasetVersionFile datasetVersionFile : data) {
            datasetVersionFile.setId(dataFileIds.poll());
        }
        datasetVersionFileMapper.saveList(data);
        LogUtil.debug(LogEnum.BIZ_DATASET, "save dataset version files end, times {}", (System.currentTimeMillis() - start));
    }

    /**
     * 新增关系版本变更
     *
     * @param datasetId     数据集id
     * @param versionSource 源版本
     * @param versionTarget 目标版本
     */
    @Override
    public void newShipVersionNameChange(Long datasetId, String versionSource, String versionTarget) {
        // 1）迁移源版本中 status=0 的版本文件到新版本
        datasetVersionFileMapper.newShipVersionNameChange(datasetId, versionSource, versionTarget);

        // 2）同步更新对应的标注记录，将新增标注从 0 调整为 2，并标记为不可变
        List<DataFileAnnotation> dataFileAnnotations =
                dataFileAnnotationService.getAnnotationByVersion(datasetId, versionSource, MagicNumConstant.ZERO);
        List<DataFileAnnotation> updateAnnotations = new ArrayList<>();
        dataFileAnnotations.stream()
                .filter(a -> a.getStatus().equals(MagicNumConstant.ZERO))
                .forEach(a -> {
                    a.setStatus(MagicNumConstant.TWO);
                    a.setInvariable(MagicNumConstant.ONE);
                    updateAnnotations.add(a);
                });
        dataFileAnnotationService.updateDataFileAnnotations(updateAnnotations);
    }

    /**
     * 版本文件删除标记
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @param fileIds     文件id列表
     */
    @Override
    public void deleteShip(Long datasetId, String versionName, List<Long> fileIds) {
        //删除数据集标注接口
        List<DatasetVersionFile> files = datasetVersionFileMapper.selectByDatasetIdAndVersionNameAndFileIds(datasetId, versionName, fileIds);
        if (!CollectionUtils.isEmpty(files) && Objects.isNull(versionName)) {
            List<Long> ids = files.stream().map(a -> a.getId()).collect(Collectors.toList());
            dataFileAnnotationService.updateStatusByVersionIds(datasetId, ids, true);
        }
        datasetVersionFileMapper.updateStatusByFileIdAndDatasetId(datasetId, versionName, fileIds);
    }


    /**
     * 数据集版本文件标注状态修改
     *
     * @param datasetId    数据集id
     * @param versionName  版本名称
     * @param fileId       文件id
     * @param sourceStatus 源状态
     * @param targetStatus 目标状态
     * @return int         标注修改的数量
     */
    @Override
    public int updateAnnotationStatus(Long datasetId, String versionName, Set<Long> fileId, Integer sourceStatus, Integer targetStatus) {
        //获取当前版本数据集下第一张图片
        DatasetVersionFile versionFile = StringUtils.isBlank(versionName) ? null : getFirstByDatasetIdAndVersionNum(datasetId, versionName, null);

        UpdateWrapper<DatasetVersionFile> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("dataset_id", datasetId);
        updateWrapper.in("file_id", fileId);
        DatasetVersionFile datasetVersionFile = new DatasetVersionFile() {{
            setAnnotationStatus(targetStatus);
        }};
        if (StringUtils.isNotEmpty(versionName)) {
            updateWrapper.eq("version_name", versionName);
        }
        if (sourceStatus != null) {
            updateWrapper.eq("annotation_status", sourceStatus);
        }
        if (versionFile != null) {
            datasetVersionFile.setChanged(Constant.CHANGED);
        }
        //=======================嵌入状态机===================================//
        datasetVersionFile = baseMapper.selectOne(updateWrapper);
        if (versionFile != null) {
            datasetVersionFile.setChanged(Constant.CHANGED);
        }
        //创建入参请求体
        StateChangeDTO stateChangeDTO = new StateChangeDTO();
        //创建需要执行事件的方法的传入参数
        Object[] objects = new Object[1];
        objects[0] = datasetVersionFile;
        stateChangeDTO.setObjectParam(objects);
        stateChangeDTO.setStateMachineType(FileStateMachineConstant.FILE_STATE_MACHINE);
        //执行自动标注算法事件
        if (targetStatus.equals(FileStateCodeConstant.MANUAL_ANNOTATION_FILE_STATE)) {
            stateChangeDTO.setEventMethodName(FileStateMachineConstant.FILE_MANUAL_ANNOTATION_SAVE_EVENT);
        }
        if (targetStatus.equals(ANNOTATION_COMPLETE_FILE_STATE)) {
            stateChangeDTO.setEventMethodName(FileStateMachineConstant.FILE_SAVE_COMPLETE_EVENT);
        }
        //=======================嵌入状态机===================================//
        StateMachineUtil.stateChange(stateChangeDTO);
        return 0;
    }

    /**
     * 获取数据集指定版本下文件列表(有效文件)
     *
     * @param datasetId   数据id
     * @param versionName 版本名称
     * @return List<DatasetVersionFile> 版本文件列表
     */
    @Override
    public List<DatasetVersionFile> getFilesByDatasetIdAndVersionName(Long datasetId, String versionName) {
        QueryWrapper<DatasetVersionFile> queryWrapper = new QueryWrapper();
        queryWrapper.eq("dataset_id", datasetId);
        if (StringUtils.isNotEmpty(versionName)) {
            queryWrapper.eq("version_name", versionName);
        }
        queryWrapper.notIn("status", MagicNumConstant.ONE);
        return baseMapper.selectList(queryWrapper);
    }


    /**
     * 数据集标注状态
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @param status      状态
     * @param offset      偏移量
     * @param limit       页容量
     * @param order       排序方式
     * @return 数据集版本文件列表
     */
    @Override
    public List<DatasetVersionFileDTO> getListByDatasetIdAndAnnotationStatus(Long datasetId, String versionName, Integer[] status, Long offset, Integer limit, String orderByName, String order, Long[] labelId) {
        order = Objects.isNull(order) ? "asc" : order;
        Dataset oneById = datasetService.getOneById(datasetId);

        List<DataFileAnnotation> labelIdByDatasetIdAndVersionId = new ArrayList<>();
        if (!ArrayUtils.isEmpty(labelId) || (DatatypeEnum.AUDIO.getValue().equals(oneById.getDataType()) &&
                !Arrays.asList(status).contains(FileTypeEnum.UNFINISHED_FILE.getValue()) && !Arrays.asList(status).contains(FileTypeEnum.UNFINISHED.getValue())) &&
                !oneById.getAnnotateType().equals(AnnotateTypeEnum.SPEECH_RECOGNITION.getValue())) {

            labelIdByDatasetIdAndVersionId = dataFileAnnotationService.getLabelIdByDatasetIdAndVersionId(labelId, datasetId, offset, limit, oneById.getCurrentVersionName());

            if (labelIdByDatasetIdAndVersionId.isEmpty()) {
                return null;
            }
        }

        List<DatasetVersionFileDTO> idByDatasetIdAndAnnotationStatus = datasetVersionFileMapper.getIdByDatasetIdAndAnnotationStatus(
                datasetId, versionName, status == null ? null : new HashSet<Integer>(Arrays.asList(status)),
                orderByName, offset, limit, order, labelIdByDatasetIdAndVersionId, oneById.getAnnotateType());

        for (DataFileAnnotation dataFileAnnotation : labelIdByDatasetIdAndVersionId) {
            for (DatasetVersionFileDTO byDatasetIdAndAnnotationStatus : idByDatasetIdAndAnnotationStatus) {
                if (byDatasetIdAndAnnotationStatus.getId().equals(dataFileAnnotation.getVersionFileId())) {
                    byDatasetIdAndAnnotationStatus.setLabelId(new Long[]{dataFileAnnotation.getLabelId()});
                    byDatasetIdAndAnnotationStatus.setPrediction(dataFileAnnotation.getPrediction());
                }
            }
        }
        return idByDatasetIdAndAnnotationStatus;
    }

    /**
     * 获取数据集指定版本第一张图片
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @param status      状态
     * @return DatasetVersionFile 版本首张图片
     */
    @Override
    public DatasetVersionFile getFirstByDatasetIdAndVersionNum(Long datasetId, String versionName, Collection<Integer> status) {
        QueryWrapper<DatasetVersionFile> queryWrapper = buildQueryWrapperWithDatasetIdVersionNameAndStatus(datasetId, versionName, status);
        queryWrapper.orderByAsc("id");
        queryWrapper.last(" limit 1");
        return baseMapper.selectOne(queryWrapper);
    }

    public QueryWrapper<DatasetVersionFile> buildQueryWrapperWithDatasetIdVersionNameAndStatus(Long datasetId, String versionName, Collection<Integer> status) {
        QueryWrapper<DatasetVersionFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dataset_id", datasetId);
        if (!CollectionUtils.isEmpty(status)) {
            queryWrapper.in("annotation_status", status);
        }
        if (StringUtils.isNotEmpty(versionName)) {
            queryWrapper.eq("version_name", versionName);
        }
        queryWrapper.ne("status", MagicNumConstant.ONE);
        return queryWrapper;
    }

    @Override
    public Integer getFileCountByDatasetIdAndAnnotationStatus(Long datasetId, String versionName, Collection<Integer> status) {
        QueryWrapper<DatasetVersionFile> queryWrapper = buildQueryWrapperWithDatasetIdVersionNameAndStatus(datasetId, versionName, status);
        return baseMapper.selectCount(queryWrapper);
    }

    /**
     * 根据版本列表中的文件id获取文件列表
     *
     * @param datasetVersionFiles 版本文件中间表列表
     * @return List<File> 文件列表
     */
    @Override
    public List<File> getFileListByVersionFileList(List<DatasetVersionFile> datasetVersionFiles) {
        QueryWrapper<File> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(File::getDatasetId, datasetVersionFiles.get(0).getDatasetId())
                .in(File::getId, datasetVersionFiles.stream().map(DatasetVersionFile::getFileId).collect(Collectors.toList()))
                .eq(File::getFileType, DatatypeEnum.IMAGE)
                .orderByAsc(File::getId);
        return fileService.listFile(wrapper);
    }

    /**
     * 通过数据集和版本获取文件状态
     *
     * @param datasetId   数据集id
     * @param versionName 版本版本名称
     * @return List<File> 文件状态列表
     */
    @Override
    public List<Integer> getFileStatusListByDatasetAndVersion(Long datasetId, String versionName) {
        if (datasetId == null) {
            LogUtil.error(LogEnum.BIZ_DATASET, "datasetId isEmpty");
            return null;
        }
        return datasetVersionFileMapper.findFileStatusListByDatasetAndVersion(datasetId, versionName);
    }

    /**
     * 版本回退
     *
     * @param dataset 数据集对象
     */
    @Override
    public void rollbackDataset(Dataset dataset) {
        if (StringUtils.isNoneBlank(dataset.getCurrentVersionName()) && isNeedToRollback(dataset)) {
            doRollback(dataset);
        }
    }

    /**
     * 判断当前数据集是否需要回滚
     *
     * @param dataset 需要回滚的数据集
     * @return boolean 数据集是否需要回退
     */
    @Override
    public boolean isNeedToRollback(Dataset dataset) {
        LambdaQueryWrapper<DatasetVersionFile> isChanged = new LambdaQueryWrapper<DatasetVersionFile>()
                .eq(DatasetVersionFile::getDatasetId, dataset.getId())
                .eq(DatasetVersionFile::getVersionName, dataset.getCurrentVersionName())
                .eq(DatasetVersionFile::getChanged, Constant.CHANGED);
        return baseMapper.selectCount(isChanged) > 0;
    }


    /**
     * 根据当前数据集和当前版本号修改数据集是否改变
     *
     * @param id          数据集id
     * @param versionName 版本号
     */
    @Override
    public void updateChanged(Long id, String versionName) {
        baseMapper.updateChanged(id, versionName);
    }

    /**
     * 回滚数据集
     *
     * @param dataset 需要回滚的数据集
     * @return boolean 数据集回退是否成功
     */
    public void doRollback(Dataset dataset) {
        //文件状态为删除新增的和标记为改变的
        datasetVersionFileMapper.rollbackFileAndAnnotationStatus(dataset.getId(), dataset.getCurrentVersionName(), Constant.CHANGED);
    }

    /**
     * 获取当前数据集版本的原始文件数量
     *
     * @param dataset 当前数据集
     * @return 原始文件数量
     */
    @Override
    public Integer getSourceFileCount(Dataset dataset) {
        return datasetVersionFileMapper.getSourceFileCount(dataset);
    }

    /**
     * 获取可以增强的文件列表
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @return List<DatasetVersionFile> 增强文件列表
     */
    @Override
    public List<DatasetVersionFile> getNeedEnhanceFilesByDatasetIdAndVersionName(Long datasetId, String versionName) {
        return baseMapper.getNeedEnhanceFilesByDatasetIdAndVersionName(datasetId, versionName);
    }

    /**
     * 获取文件对应增强文件列表
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @param fileId      文件id
     * @return List<File> 增强文件列表
     */
    @Override
    public List<File> getEnhanceFileList(Long datasetId, String versionName, Long fileId) {
        return baseMapper.getEnhanceFileList(datasetId, versionName, fileId);
    }

    /**
     * 获取增强文件数量
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     * @return Integer    当前版本增强文件数量
     */
    @Override
    public Integer getEnhanceFileCount(Long datasetId, String versionName) {
        return baseMapper.getEnhanceFileCount(datasetId, versionName);
    }

    /**
     * 根据当前数据集版本获取图片的数量
     *
     * @param datasetId   数据集id
     * @param versionName 数据集版本名称
     * @return 当前数据集版本获取图片的数量
     */
    @Override
    public Integer getImageCountsByDatasetIdAndVersionName(Long datasetId, String versionName) {
        QueryWrapper<DatasetVersionFile> datasetVersionFileQueryWrapper = new QueryWrapper<>();
        datasetVersionFileQueryWrapper.eq("dataset_id", datasetId)
                .eq("version_name", versionName);
        return baseMapper.selectCount(datasetVersionFileQueryWrapper);
    }


    /**
     * 分页获取数据集版本文件数据
     *
     * @param offset      偏移量
     * @param pageSize    页容量
     * @param datasetId   数据集ID
     * @param versionName 数据集版本名称
     * @return 数据集版本文件列表
     */
    @Override
    public List<DatasetVersionFile> getPages(int offset, int pageSize, Long datasetId, String versionName) {
        QueryWrapper<DatasetVersionFile> datasetVersionFileQueryWrapper = new QueryWrapper<>();
        datasetVersionFileQueryWrapper.eq("dataset_id", datasetId);
        if (StringUtils.isNotEmpty(versionName)) {
            datasetVersionFileQueryWrapper.eq("version_name", versionName);
        }
        datasetVersionFileQueryWrapper.last("limit " + offset + "," + pageSize);
        return baseMapper.selectList(datasetVersionFileQueryWrapper);
    }


    /**
     * 获取数据集当前版本文件数量
     *
     * @param queryWrapper 查询条件
     * @return Integer 数据集当前版本文件数量
     */
    @Override
    public Integer getFileCountByDatasetIdAndVersion(LambdaQueryWrapper<DatasetVersionFile> queryWrapper) {
        return baseMapper.selectCount(queryWrapper);
    }

    /**
     * 获取数据集文件状态统计数据
     *
     * @param datasetId   数据集ID
     * @param versionName 数据集版本名称
     * @return Map 数据集当前版本文件数量
     */
    @Override
    public Map<Integer, Integer> getDatasetVersionFileCount(Long datasetId, String versionName) {
        return baseMapper.getDatasetVersionFileCount(datasetId, versionName);
    }

    /**
     * 获取数据集文件数量统计数据
     *
     * @param datasetVersion 数据集版本
     * @return 数据集文件统计
     */
    @Override
    public Integer selectDatasetVersionFileCount(DatasetVersion datasetVersion) {
        return baseMapper.selectCount(new LambdaQueryWrapper<DatasetVersionFile>() {{
            eq(DatasetVersionFile::getDatasetId, datasetVersion.getDatasetId());
            eq(DatasetVersionFile::getVersionName, datasetVersion.getVersionName());
            ne(DatasetVersionFile::getStatus, NumberConstant.NUMBER_0);
            ne(DatasetVersionFile::getStatus, NumberConstant.NUMBER_1);
        }});
    }

    /**
     * 获取offset
     *
     * @param datasetId 数据集id
     * @param fileId    文件id
     * @param type      数据集类型
     * @return Integer 获取到offset
     */
    @Override
    public Integer getOffset(Long fileId, Long datasetId, Integer[] type, Long[] labelIds) {
        Dataset dataset = datasetService.getOneById(datasetId);
        DatasetVersionFile datasetVersionFile = baseMapper.selectOne(new LambdaQueryWrapper<DatasetVersionFile>() {{
            eq(DatasetVersionFile::getDatasetId, dataset.getId());
            if (StringUtils.isBlank(dataset.getCurrentVersionName())) {
                isNull(DatasetVersionFile::getVersionName);
            } else {
                eq(DatasetVersionFile::getVersionName, datasetService.getOneById(datasetId).getCurrentVersionName());
            }
            eq(DatasetVersionFile::getFileId, fileId);
        }});
        if (ObjectUtil.isNull(datasetVersionFile)) {
            return 0;
        }
        Set<Integer> annotationStatus = null;
        if (type != null && type.length > 0) {
            annotationStatus = new HashSet<>();
            for (Integer in : type) {
                annotationStatus.addAll(FileTypeEnum.getStatus(in));
            }
        }
        return baseMapper.getOffset(datasetId, annotationStatus, dataset.getCurrentVersionName(),
                datasetVersionFile.getId(), labelIds);
    }


    /**
     * 获取多人标注场景下特定类型文件的绝对偏移量
     * @param datasetId 数据集ID
     * @param annotationStatus 标注状态（如101未标注）
     * @param startOffset 用户分配的起始偏移量
     * @param total 用户分配的文件总数
     * @return 找到的第一个符合条件文件的绝对偏移量，如果没找到则返回-1
     */
    @Override
    public Integer getAbsoluteOffsetForAnnotationStatus(Long datasetId, Integer annotationStatus,
                                                        Integer startOffset, Integer total) {
        Dataset dataset = datasetService.getOneById(datasetId);
        if (dataset == null) {
            return -1;
        }

        // 构建查询条件，只查询用户分配范围内的文件
        LambdaQueryWrapper<DatasetVersionFile> queryWrapper = new LambdaQueryWrapper<DatasetVersionFile>() {{
            eq(DatasetVersionFile::getDatasetId, datasetId);
            if (StringUtils.isBlank(dataset.getCurrentVersionName())) {
                isNull(DatasetVersionFile::getVersionName);
            } else {
                eq(DatasetVersionFile::getVersionName, dataset.getCurrentVersionName());
            }
            in(DatasetVersionFile::getStatus, Arrays.asList(0, 2)); // 只查询正常状态的文件
            orderByAsc(DatasetVersionFile::getId); // 按ID升序排列，保持与原逻辑一致
        }};

        // 使用LIMIT和OFFSET直接获取指定范围内的文件
        queryWrapper.last("LIMIT " + total + " OFFSET " + startOffset);
        List<DatasetVersionFile> rangeFiles = baseMapper.selectList(queryWrapper);

        if (CollectionUtil.isEmpty(rangeFiles)) {
            return -1;
        }

        // 在范围内查找第一个符合annotation_status条件的文件
        DatasetVersionFile firstMatchedFile = null;

        for (DatasetVersionFile file : rangeFiles) {
            if (file.getAnnotationStatus().equals(annotationStatus)) {
                firstMatchedFile = file;
                break;
            }
        }

        // 如果在分配的范围内没找到符合条件的文件
        if (firstMatchedFile == null) {
            return -1;
        }

        // 直接计算绝对偏移量，整合原getOffset方法的逻辑，避免重复查询
        Set<Integer> statusSet = new HashSet<>(FileTypeEnum.getStatus(annotationStatus));

        Integer absoluteOffset = baseMapper.getOffset(datasetId, statusSet,
                dataset.getCurrentVersionName(),
                firstMatchedFile.getId(), null);

        return absoluteOffset - MagicNumConstant.ONE;
    }


    /**
     * 条件查询数据集文件表的数据量
     *
     * @param eq 条件
     * @return long 版本文件数量
     */
    @Override
    public long selectCount(LambdaQueryWrapper<DatasetVersionFile> eq) {
        return baseMapper.selectCount(eq);
    }

    /**
     * 获取单个的文件版本信息
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名称
     * @param fileId      文件ID
     * @return 文件版本信息
     */
    @Override
    public DatasetVersionFile getDatasetVersionFile(Long datasetId, String versionName, Long fileId) {
        return baseMapper.selectOne(new LambdaQueryWrapper<DatasetVersionFile>() {{
            eq(DatasetVersionFile::getDatasetId, datasetId);
            if (StringUtils.isBlank(versionName)) {
                isNull(DatasetVersionFile::getVersionName);
            } else {
                eq(DatasetVersionFile::getVersionName, versionName);
            }
            eq(DatasetVersionFile::getFileId, fileId);
        }});
    }

    /**
     * 批量获取数据版本文件信息
     *
     * @param datasetId    数据集ID
     * @param versionName  当前版本
     * @param fileIds      文件Id列表
     * @return Map<Long, DatasetVersionFile> key为文件ID，value为版本文件实体
     */
    @Override
    public Map<Long, DatasetVersionFile> batchGetDatasetVersionFile(Long datasetId, String versionName, List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Collections.emptyMap();
        }
        
        LambdaQueryWrapper<DatasetVersionFile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DatasetVersionFile::getDatasetId, datasetId);
        if (StringUtils.isBlank(versionName)) {
            queryWrapper.isNull(DatasetVersionFile::getVersionName);
        } else {
            queryWrapper.eq(DatasetVersionFile::getVersionName, versionName);
        }
        queryWrapper.in(DatasetVersionFile::getFileId, fileIds);
        
        List<DatasetVersionFile> versionFiles = baseMapper.selectList(queryWrapper);
        
        return versionFiles.stream()
                .collect(Collectors.toMap(DatasetVersionFile::getFileId, Function.identity(), (v1, v2) -> v1));
    }

    /**
     * 重新标注操作更新文件状态
     *
     * @param datasetId 数据集ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteAnnotating(Long datasetId) {
        Dataset dataset = datasetService.getOneById(datasetId);
        datasetVersionFileMapper.update(
                new DatasetVersionFile() {{
                    setAnnotationStatus(NOT_ANNOTATION_FILE_STATE);
                    setChanged(Constant.CHANGED);
                }},
                new UpdateWrapper<DatasetVersionFile>()
                        .lambda()
                        .eq(DatasetVersionFile::getDatasetId, dataset.getId())
                        .eq(dataset.getCurrentVersionName() != null, DatasetVersionFile::getVersionName, dataset.getCurrentVersionName())
        );
    }


    /**
     * 获取数据集版本文件ID
     *
     * @param id                 数据集ID
     * @param currentVersionName 版本名称
     * @param fileIds            文件id
     * @return 获取数据集版本文件
     */
    @Override
    public List<DatasetVersionFile> getVersionFileByDatasetAndFile(Long id, String currentVersionName, Set<Long> fileIds) {
        return datasetVersionFileMapper.selectList(new LambdaQueryWrapper<DatasetVersionFile>() {{
                                                       eq(DatasetVersionFile::getDatasetId, id);
                                                       if (StringUtils.isBlank(currentVersionName)) {
                                                           isNull(DatasetVersionFile::getVersionName);
                                                       } else {
                                                           eq(DatasetVersionFile::getVersionName, currentVersionName);
                                                       }
                                                       in(DatasetVersionFile::getFileId, fileIds);
                                                   }}
        );
    }

    /**
     * 修改文件状态
     *
     * @param datasetVersionFile 数据集版本文件实体
     */
    @Override
    public void updateStatusById(DatasetVersionFile datasetVersionFile) {
        datasetVersionFileMapper.updateAnnotationStatusById(datasetVersionFile.getAnnotationStatus(), datasetVersionFile.getDatasetId(), datasetVersionFile.getId());
    }

    @Override
    public void updateStatusByIds(Integer annotationStatus, Long datasetId, List<Long> ids) {
        datasetVersionFileMapper.updateAnnotationStatusByIds(annotationStatus, datasetId, ids);
    }




    /**
     * 根据数据集分页条件查询文件总数量
     *
     * @param datasetId          数据集ID
     * @param currentVersionName 当前版本号码
     * @param status             状态
     * @param labelIds           标签ID
     * @return 文件数量
     */
    @Override
    public int selectFileListTotalCount(Long datasetId, String currentVersionName, Integer[] status, Long[] labelIds) {
        Set<Integer> objects = new HashSet<>();
        if (ArrayUtil.isEmpty(status)) {
            objects = FileTypeEnum.getStatus(NumberConstant.NUMBER_0);
        } else {
            for (Integer sta : status) {
                objects.addAll(FileTypeEnum.getStatus(sta));
            }
        }

        List<Long> versionId = null;
        if (!ArrayUtils.isEmpty(labelIds)) {
            versionId = baseMapper.findByDatasetIdAndVersionNameAndStatus(datasetId, currentVersionName, labelIds);
        }
        return datasetVersionFileMapper.selectFileListTotalCount(datasetId, currentVersionName, objects, versionId);
    }


    /**
     * 根据数据集ID和版本号查询版本文件信息
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名称
     * @return 数据集版本文件列表
     */
    @Override
    public List<DatasetVersionFile> getDatasetVersionFileByDatasetIdAndVersion(Long datasetId, String versionName) {
        return baseMapper.findByDatasetIdAndVersionName(datasetId, versionName);
    }


    /**
     * 备份版本文件数据
     *
     * @param originDataset 原数据集实体
     * @param targetDataset 目标数据集实体
     * @param versionFiles  原版本文件列表
     * @param files         已转换文件列表
     */
    @Override
    public void backupDatasetVersionFileDataByDatasetId(Dataset originDataset, Dataset targetDataset, List<DatasetVersionFile> versionFiles, List<File> files) {
        Map<String, Long> fileNameMap = files.stream().collect(Collectors.toMap(File::getName, File::getId));
        Queue<Long> dataFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_VERSION_FILE, versionFiles.size());
        for (DatasetVersionFile f : versionFiles) {
            f.setId(dataFileIds.poll());
            f.setFileId(fileNameMap.get(f.getFileName()));
            f.setVersionName(DEFAULT_VERSION);
            f.setDatasetId(targetDataset.getId());
            f.setFileName(f.getFileName());
        }
        List<List<DatasetVersionFile>> splitVersionFiles = CollectionUtil.split(versionFiles, MagicNumConstant.FOUR_THOUSAND);
        splitVersionFiles.forEach(splitVersionFile -> baseMapper.insertBatch(splitVersionFile));
    }

    /**
     * 文件id获取版本文件id
     *
     * @param datasetId 数据集id
     * @param fileIds   文件id
     * @return List<Long>       版本文件id
     */
    @Override
    public List<Long> getVersionFileIdsByFileIds(Long datasetId, List<Long> fileIds) {
        return baseMapper.getVersionFileIdsByFileIds(datasetId, fileIds);
    }

    /**
     * 获取版本文件id
     *
     * @param datasetId   数据集id
     * @param fileName    文件名称
     * @param versionName 版本名称
     */
    @Override
    public Long getVersionFileIdByFileName(Long datasetId, String fileName, String versionName) {
        return baseMapper.getVersionFileIdByFileName(datasetId, fileName, versionName);
    }

    /**
     * 获取导入文件所需信息
     *
     * @param datasetId 数据集id
     * @return List<FileUploadBO>
     */
    @Override
    public List<FileUploadBO> getFileUploadContent(Long datasetId, List<Long> fileIds) {
        return baseMapper.getFileUploadContent(datasetId, fileIds);
    }

    @Override
    public Long getVersionFileCountByStatusVersionAndLabelId(Long datasetId, Set<Integer> annotationStatus, String versionName, List<Long> labelIds) {
        List<Long> versionId = null;
        if (CollectionUtil.isNotEmpty(labelIds)) {
            Long[] labelArr = new Long[labelIds.size()];
            labelIds.toArray(labelArr);
            versionId = baseMapper.findByDatasetIdAndVersionNameAndStatus(datasetId, versionName, labelArr);
            if (CollectionUtil.isEmpty(versionId)) {
                return 0L;
            }
        }
        int count = datasetVersionFileMapper.selectFileListTotalCount(datasetId, versionName, annotationStatus, versionId);
        return Long.valueOf(count);
    }

    /**
     * 删除旧标注信息
     *
     * @param datasetId 数据集id
     * @param fileId    文件id
     */
    @Override
    public void deleteByFileId(Long datasetId, Long fileId) {
        Dataset dataset = datasetService.getOneById(datasetId);
        Long versionFileId = datasetVersionFileMapper.getVersionFileIdByFileName(datasetId, fileService.get(fileId, datasetId).getName()
                , dataset.getCurrentVersionName());
        QueryWrapper<DataFileAnnotation> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(DataFileAnnotation::getDatasetId, datasetId).eq(DataFileAnnotation::getVersionFileId, versionFileId);
        dataFileAnnotationServiceImpl.getBaseMapper().delete(queryWrapper);
    }


    @Override
    public void deleteByFileIds(Long datasetId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        Dataset dataset = datasetService.getOneById(datasetId);
        String currentVersionName = dataset.getCurrentVersionName();

        List<String> fileNameList = new ArrayList<>();
        for (Long fileId : fileIds) {
            fileNameList.add(fileService.get(fileId, datasetId).getName());
        }
        // 批量查所有 versionFileId
        List<Long> versionFileIds = datasetVersionFileMapper.getVersionFileIdsByFileNames(datasetId, fileNameList, currentVersionName);

        if (versionFileIds == null || versionFileIds.isEmpty()) {
            return;
        }

        QueryWrapper<DataFileAnnotation> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda()
                .eq(DataFileAnnotation::getDatasetId, datasetId)
                .in(DataFileAnnotation::getVersionFileId, versionFileIds);
        dataFileAnnotationServiceImpl.getBaseMapper().delete(queryWrapper);
    }


    @Override
    public DatasetVersionFileLabelStatVO countByFileAnnotate() {
        Integer annotatedFiles = baseMapper.selectCount(new LambdaQueryWrapper<DatasetVersionFile>()
                .eq(DatasetVersionFile::getAnnotationStatus, ANNOTATION_COMPLETE_FILE_STATE)
                .ne(DatasetVersionFile::getStatus, MagicNumConstant.ONE));

        Integer unannotatedFiles = baseMapper.selectCount(new LambdaQueryWrapper<DatasetVersionFile>()
                .eq(DatasetVersionFile::getAnnotationStatus, NOT_ANNOTATION_FILE_STATE)
                .ne(DatasetVersionFile::getStatus, MagicNumConstant.ONE));

        Integer allFiles = baseMapper.selectCount(new LambdaQueryWrapper<DatasetVersionFile>()
                .ne(DatasetVersionFile::getStatus, MagicNumConstant.ONE));

        return DatasetVersionFileLabelStatVO.builder().annotatedFiles(annotatedFiles).unannotatedFiles(unannotatedFiles).allFiles(allFiles).build();
    }


    @Override
    public Map<Long, DatasetVersionFile> getVersionFileMapByFileIds(Long datasetId, String versionName, List<Long> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return new HashMap<>();
        }
        List<DatasetVersionFile> versionFiles = this.baseMapper.getVersionFilesByFileIds(datasetId, versionName, fileIds);
        return versionFiles.stream().collect(Collectors.toMap(
                DatasetVersionFile::getFileId,
                Function.identity(),
                (existing, replacement) -> existing
        ));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatusBatch(List<DatasetVersionFile> datasetVersionFiles) {
        if (CollectionUtils.isEmpty(datasetVersionFiles)) {
            return;
        }
        // 分批处理，避免SQL过长
        int batchSize = 100;
        for (int i = 0; i < datasetVersionFiles.size(); i += batchSize) {
            int end = Math.min(i + batchSize, datasetVersionFiles.size());
            List<DatasetVersionFile> batch = datasetVersionFiles.subList(i, end);
            this.baseMapper.updateStatusBatch(batch);
        }
    }

    @Override
    public Integer getTotalFileCountByDatasetIds(List<Long> datasetIds) {
        if (CollectionUtils.isEmpty(datasetIds)) {
            return 0;
        }
        return datasetVersionFileMapper.getDatasetVersionAllFileCountByIds(datasetIds);
    }

    /**
     * 批量查询多个数据集的 changed 文件计数
     * @param datasetInfos 数据集信息列表（包含 datasetId 和 versionName）
     * @return Map<datasetId, count> 每个数据集的 changed 文件计数
     */
    public Map<Long, Integer> batchCountChangedFiles(List<DatasetVersionFileMapper.DatasetChangedInfo> datasetInfos) {
        if (CollectionUtils.isEmpty(datasetInfos)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> resultList = datasetVersionFileMapper.batchCountChangedFiles(datasetInfos);
        Map<Long, Integer> result = new HashMap<>();
        if (!CollectionUtils.isEmpty(resultList)) {
            for (Map<String, Object> map : resultList) {
                Long datasetId = ((Number) map.get("datasetId")).longValue();
                Integer count = ((Number) map.get("count")).intValue();
                result.put(datasetId, count);
            }
        }
        return result;
    }

    @Override
    public void deleteStatus01ByDatasetAndVersion(Long datasetId, String versionName) {
        if (datasetId == null || StringUtils.isBlank(versionName)) {
            return;
        }
        datasetVersionFileMapper.deleteStatus01ByDatasetAndVersion(datasetId, versionName);
    }

    @Override
    public void resetChangedByDatasetAndVersion(Long datasetId, String versionName) {
        if (datasetId == null || StringUtils.isBlank(versionName)) {
            return;
        }
        datasetVersionFileMapper.resetChangedByDatasetIdAndVersionName(datasetId, versionName);
    }
}