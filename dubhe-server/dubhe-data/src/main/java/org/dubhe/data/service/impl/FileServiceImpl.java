package org.dubhe.data.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.constant.NumberConstant;
import org.dubhe.biz.base.constant.SymbolConstant;
import org.dubhe.biz.base.context.DataContext;
import org.dubhe.biz.base.dto.CommonPermissionDataDTO;
import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.base.vo.ProgressVO;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.biz.db.utils.WrapperHelp;
import org.dubhe.biz.file.api.FileStoreApi;
import org.dubhe.biz.file.dto.FileDTO;
import org.dubhe.biz.file.dto.FilePageDTO;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.biz.statemachine.dto.StateChangeDTO;
import org.dubhe.cloud.authconfig.utils.JwtUtils;
import org.dubhe.data.constant.*;
import org.dubhe.data.dao.DatasetMapper;
import org.dubhe.data.dao.FileMapper;
import org.dubhe.data.dao.ImageFileDTOMapper;
import org.dubhe.data.dao.VideoFileDTOMapper;
import org.dubhe.data.domain.bo.FileAnnotationBO;
import org.dubhe.data.domain.bo.TaskSplitBO;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.*;
import org.dubhe.data.domain.vo.*;
import org.dubhe.data.machine.constant.DataStateMachineConstant;
import org.dubhe.data.machine.constant.FileStateCodeConstant;
import org.dubhe.data.machine.enums.FileStateEnum;
import org.dubhe.data.machine.utils.StateMachineUtil;
import org.dubhe.data.service.*;
import org.dubhe.data.service.store.IStoreService;
import org.dubhe.data.service.store.MinioStoreServiceImpl;
import org.dubhe.data.util.ConversionUtil;
import org.dubhe.data.util.FileUtil;
import org.dubhe.data.util.GeneratorKeyUtil;
import org.dubhe.data.util.TaskUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.dubhe.data.constant.Constant.ABSTRACT_NAME_PREFIX;
import static org.dubhe.data.constant.Constant.PID_OF_VIDEO;


/**
 * @description 文件信息 服务实现类
 * @date 2020-04-10
 */
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    /**
     * 采样算法完成队列
     */
    private static final String SAMPLE_FINISHED_QUEUE_NAME = "videoSample_finished_queue";
    /**
     * 采样算法失败队列
     */
    private static final String SAMPLE_FAILED_QUEUE_NAME = "videoSample_failed_queue";
    /**
     * 采样算法执行中队列
     */
    private static final String START_SAMPLE_QUEUE = "videoSample_processing_queue";
    /**
     * 采样算法未处理队列
     */
    private static final String SAMPLE_PENDING_QUEUE = "videoSample_task_queue";
    /**
     * 采样算法任务详情
     */
    private static final String DETAIL_NAME = "videoSample_pictures:";
    /**
     * 单个标注任务数量
     */
    @Value("${data.annotation.task.splitSize:16}")
    private Integer taskSplitSize;
    /**
     * 默认标注页面文件列表分页大小
     */
    @Value("${data.file.pageSize:20}")
    private Integer defaultFilePageSize;
    /**
     * 路径名前缀
     */
    @Value("${storage.file-store-root-path:/nfs/}")
    private String prefixPath;
    /**
     * minIO公钥
     */
    @Value("${minio.accessKey}")
    private String accessKey;
    /**
     * minIO私钥
     */
    @Value("${minio.secretKey}")
    private String secretKey;
    /**
     * 加密字符串
     */
    @Value("${minio.url}")
    private String url;
    /**
     * 桶名称
     */
    @Value("${minio.bucketName}")
    private String bucketName;
    /**
     * zlm录制文件路径拼接前缀
     */
    @Value("${zlm.urlPrefix}")
    private String zlmUrlPrefix;
    /**
     * 文件转换
     */
    @Autowired
    private FileConvert fileConvert;
    /**
     * 文件工具类
     */
    @Autowired
    private FileUtil fileUtil;
    @Autowired
    private MinioUtil minioUtil;
    /**
     * 任务类
     */
    @Autowired
    @Lazy
    private TaskService taskService;
    /**
     * 文件存储服务实现类
     */
    @Resource(type = MinioStoreServiceImpl.class)
    private IStoreService storeService;
    /**
     * 数据集服务实现类
     */
    @Resource
    @Lazy
    private DatasetService datasetService;
    /**
     * 数据集版本文件服务实现类
     */
    @Resource
    @Lazy
    private DatasetVersionFileService datasetVersionFileService;
    @Autowired
    private TaskUtils taskUtils;
    //    @Autowired
//    private RestHighLevelClient restHighLevelClient;
//    @Resource
//    private BulkProcessor bulkProcessor;
    @Resource
    private RedisUtils redisUtils;
    /**
     * 数据集版本文件服务实现类
     */
    @Resource
    @Lazy
    private FileMapper fileMapper;
    @Resource
    @Lazy
    private DatasetMapper datasetMapper;
    @Autowired
    private ImageFileDTOMapper imageFileDTOMapper;
    @Autowired
    private VideoFileDTOMapper videoFileDTOMapper;
    @Resource
    private UserContextService contextService;
    @Autowired
    private DatasetLabelService datasetLabelService;
    @Autowired
    private DataFileAnnotationService dataFileAnnotationService;
    @Resource(name = "hostFileStoreApiImpl")
    private FileStoreApi fileStoreApi;
    @Autowired
    private GeneratorKeyUtil generatorKeyUtil;
    @Autowired
    private DatasetOperationEventService datasetOperationEventService;




    /**
     * 文件详情
     *
     * @param fileId 文件ID
     * @return FileVO 文件信息
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public FileVO get(Long fileId, Long datasetId) {
        File file = fileMapper.selectFile(fileId, datasetId);
        Dataset dataset = datasetService.getOneById(datasetId);
        DatasetVersionFile datasetVersionFile = datasetVersionFileService.getDatasetVersionFile(datasetId, dataset.getCurrentVersionName(), fileId);
        if (file == null) {
            return null;
        }
        FileVO fileVO = fileConvert.toDto(file,
                getAnnotation(file.getDatasetId(), FileUtil.interceptFileNameAndDatasetId(datasetId, file.getName()),
                        datasetVersionFile.getVersionName(), datasetVersionFile.getChanged() == NumberConstant.NUMBER_0));
        //file里面的status不知道是作什么的，感觉不太对一直是101，应该换成versionfile的annotationStatus （这里之前用错用成status了，导致前端一直不对，注意一下）
        fileVO.setStatus(datasetVersionFile.getAnnotationStatus());
        if (datasetVersionFile.getChanged() == NumberConstant.NUMBER_0) {//未改变时需要替换标签名称为id
            String annotation = fileVO.getAnnotation();
            if (StringUtils.isNotEmpty(annotation)) {
                List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(dataset.getId());
                Map<String, Long> labelMaps = new HashMap<>();
                datasetLabels.stream().forEach(label -> {
                    labelMaps.put(label.getName(), label.getId());
                });
                JSONArray jsonArray = JSON.parseArray(annotation);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String categoryIdStr = jsonObject.getString("category_id");
                    if (!NumberUtil.isNumber(categoryIdStr)) {
                        if (labelMaps.containsKey(categoryIdStr)) {
                            jsonObject.put("category_id", labelMaps.get(categoryIdStr));
                        }
                    }
                }
                fileVO.setAnnotation(JSON.toJSONString(jsonArray));
            }
        }
        return fileVO;
    }

    /**
     * 批量获取文件并返回Map
     *
     * @param fileIds   文件id集合
     * @param datasetId 数据集ID
     * @return Map<Long, FileVO> key为文件ID，value为文件VO
     */
    @Override
    public Map<Long, FileVO> batchGetFileMap(List<Long> fileIds, Long datasetId) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Collections.emptyMap();
        }
        
        Map<Long, FileVO> resultMap = new HashMap<>(fileIds.size());
        Dataset dataset = datasetService.getOneById(datasetId);
        
        // 批量查询文件
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(File::getDatasetId, datasetId)
                .in(File::getId, fileIds);
        List<File> files = baseMapper.selectList(queryWrapper);
        
        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptyMap();
        }
        
        // 批量查询版本文件
        Map<Long, DatasetVersionFile> versionFileMap = datasetVersionFileService.batchGetDatasetVersionFile(
                datasetId, dataset.getCurrentVersionName(), fileIds);
        
        // 转换为FileVO
        for (File file : files) {
            DatasetVersionFile datasetVersionFile = versionFileMap.get(file.getId());
            if (datasetVersionFile == null) {
                continue;
            }
            
            FileVO fileVO = fileConvert.toDto(file,
                    getAnnotation(file.getDatasetId(), FileUtil.interceptFileNameAndDatasetId(datasetId, file.getName()),
                            datasetVersionFile.getVersionName(), datasetVersionFile.getChanged() == NumberConstant.NUMBER_0));
            fileVO.setStatus(datasetVersionFile.getAnnotationStatus());
            
            if (datasetVersionFile.getChanged() == NumberConstant.NUMBER_0) {
                String annotation = fileVO.getAnnotation();
                if (StringUtils.isNotEmpty(annotation)) {
                    List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(dataset.getId());
                    Map<String, Long> labelMaps = new HashMap<>();
                    datasetLabels.stream().forEach(label -> {
                        labelMaps.put(label.getName(), label.getId());
                    });
                    JSONArray jsonArray = JSON.parseArray(annotation);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String categoryIdStr = jsonObject.getString("category_id");
                        if (!NumberUtil.isNumber(categoryIdStr)) {
                            if (labelMaps.containsKey(categoryIdStr)) {
                                jsonObject.put("category_id", labelMaps.get(categoryIdStr));
                            }
                        }
                    }
                    fileVO.setAnnotation(JSON.toJSONString(jsonArray));
                }
            }
            
            resultMap.put(file.getId(), fileVO);
        }
        
        return resultMap;
    }

    /**
     * 获取图片文件信息
     *
     * @param fileId 文件ID
     * @return FileAnnotationBO 文件信息
     */
    private FileAnnotationBO getImageFile(Long fileId, Long datasetId) {
        File file = fileMapper.selectFile(fileId, datasetId);
        Dataset dataset = datasetService.getOneById(datasetId);
        DatasetVersionFile datasetVersionFile = datasetVersionFileService.getDatasetVersionFile(datasetId, dataset.getCurrentVersionName(), fileId);
        if (file == null) {
            return null;
        }
        FileAnnotationBO fileAnnotationBO = FileAnnotationBO.builder()
                .fileId(file.getId())
                .fileName(file.getName())
                .fileHeight(file.getHeight())
                .fileWidth(file.getWidth())
                .fileUrl(file.getUrl())
                .annotationUrl(getAnnotation(file.getDatasetId(), FileUtil.interceptFileNameAndDatasetId(datasetId, file.getName()),
                        datasetVersionFile.getVersionName(), datasetVersionFile.getChanged() == NumberConstant.NUMBER_0)).build();
        if (datasetVersionFile.getChanged() == NumberConstant.NUMBER_0) { //未改变时需要替换标签名称为id
            String annotation = fileAnnotationBO.getAnnotationUrl();
            if (StringUtils.isNotEmpty(annotation)) {
                List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(dataset.getId());
                Map<String, Long> labelMaps = new HashMap<>();
                datasetLabels.stream().forEach(label -> {
                    labelMaps.put(label.getName(), label.getId());
                });
                JSONArray jsonArray = JSON.parseArray(annotation);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String categoryIdStr = jsonObject.getString("category_id");
                    if (!NumberUtil.isNumber(categoryIdStr)) {
                        if (labelMaps.containsKey(categoryIdStr)) {
                            jsonObject.put("category_id", labelMaps.get(categoryIdStr));
                        }
                    }
                }
                fileAnnotationBO.setAnnotationUrl(JSON.toJSONString(jsonArray));
            }
        }
        return fileAnnotationBO;
    }

    /**
     * 获取标注信息
     *
     * @param datasetId 数据集ID
     * @param fileName  文件名
     * @return String
     */
    public String getAnnotation(Long datasetId, String fileName, String versionName, boolean change) {
        String path = fileUtil.getReadAnnotationAbsPath(datasetId, fileName, versionName, change);
        return storeService.read(path);
    }

    /**
     * 判断视频数据集是否已存在视频
     *
     * @param datasetId 数据集ID
     */
    @Override
    public void isExistVideo(Long datasetId) {
        QueryWrapper<File> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper.lambda().eq(File::getDatasetId, datasetId);
        if (getBaseMapper().selectCount(fileQueryWrapper) > MagicNumConstant.ZERO) {
            throw new BusinessException(ErrorEnum.VIDEO_EXIST);
        }
    }

    /**
     * 删除文件
     *
     * @param datasetId 数据集ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Long datasetId) {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(File::getDatasetId, datasetId);
        remove(queryWrapper);
    }

    /**
     * 数据集标注进度
     *
     * @param datasets 数据集
     * @return Map<Long, ProgressVO> 数据集标注进度map
     */
    @Override
    public Map<Long, ProgressVO> listStatistics(List<Dataset> datasets) {
        if (CollectionUtils.isEmpty(datasets)) {
            return Collections.emptyMap();
        }
        Map<Long, ProgressVO> res = new HashMap<>(datasets.size());

        // 封装数据集版本数据
        datasets.forEach(dataset -> {
            Map<Integer, Integer> fileStatus = datasetVersionFileService.getDatasetVersionFileCount(dataset.getId(), dataset.getCurrentVersionName());
            ProgressVO progressVO = ProgressVO.builder().build();
            if (fileStatus != null) {
                for (Map.Entry<Integer, Integer> entry : fileStatus.entrySet()) {
                    JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(entry.getValue()));
                    if (entry.getKey().equals(FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE) || entry.getKey().equals(FileStateCodeConstant.MANUAL_ANNOTATION_FILE_STATE)) {
                        progressVO.setUnfinished(progressVO.getUnfinished() + jsonObject.getInteger("count"));
                    } else if (entry.getKey().equals(FileStateCodeConstant.AUTO_TAG_COMPLETE_FILE_STATE)) {
                        progressVO.setAutoFinished(progressVO.getAutoFinished() + jsonObject.getInteger("count"));
                    } else if (entry.getKey().equals(FileStateCodeConstant.ANNOTATION_COMPLETE_FILE_STATE)) {
                        progressVO.setFinished(progressVO.getFinished() + jsonObject.getInteger("count"));
                    } else if (entry.getKey().equals(FileStateCodeConstant.TARGET_COMPLETE_FILE_STATE)) {
                        progressVO.setFinishAutoTrack(progressVO.getFinishAutoTrack() + jsonObject.getInteger("count"));
                    } else if (entry.getKey().equals(FileStateCodeConstant.ANNOTATION_NOT_DISTINGUISH_FILE_STATE)) {
                        progressVO.setAnnotationNotDistinguishFile(progressVO.getAnnotationNotDistinguishFile() + jsonObject.getInteger("count"));
                    }
                }
            }
            res.put(dataset.getId(), progressVO);
        });
        return res;
    }


    /**
     * 将整体任务分割
     *
     * @param files 文件集合
     * @param task  任务
     * @return List<TaskSplitBO> 任务集合
     */
    @Override
    public List<TaskSplitBO> split(Collection<File> files, Task task) {
        if (CollectionUtils.isEmpty(files)) {
            return new LinkedList<>();
        }
        LogUtil.info(LogEnum.BIZ_DATASET, "split file. file size:{}", files.size());
        Map<Long, List<File>> groupedFiles = files.stream().collect(Collectors.groupingBy(File::getDatasetId));
        List<TaskSplitBO> ts = groupedFiles.values().stream()
                .flatMap(fs -> CollectionUtil.split(fs, taskSplitSize).stream())
                .map(fs -> TaskSplitBO.from(fs, task)).filter(Objects::nonNull).collect(Collectors.toList());
        LogUtil.info(LogEnum.BIZ_DATASET, "split result. split size:{}", ts.size());
        return ts;
    }

    /**
     * 执行文件更新
     *
     * @param ids            文件ID
     * @param fileStatusEnum 文件状态
     * @return int 更新结果
     */
    public int doUpdate(Collection<Long> ids, FileStateEnum fileStatusEnum) {
        if (CollectionUtils.isEmpty(ids)) {
            return MagicNumConstant.ZERO;
        }
        File newObj = File.builder().status(fileStatusEnum.getCode()).build();
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(File::getId, ids);
        return baseMapper.update(newObj, queryWrapper);
    }

    /**
     * 更新文件状态
     *
     * @param files          文件集合
     * @param fileStatusEnum 文件状态
     * @return int 更新结果
     */
    @Override
    public int update(Collection<File> files, FileStateEnum fileStatusEnum) {
        Collection<Long> ids = toIds(files);
        if (CollectionUtils.isEmpty(files)) {
            return MagicNumConstant.ZERO;
        }
        int count = doUpdate(ids, fileStatusEnum);
        if (count == MagicNumConstant.ZERO) {
            throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH);
        }
        return count;
    }


    /**
     * 通过文件获取ID
     *
     * @param files file文件
     * @return Collection<Long> 文件ID
     */
    private Collection<Long> toIds(Collection<File> files) {
        if (CollectionUtils.isEmpty(files)) {
            return Collections.emptySet();
        }
        return files.stream().map(File::getId).collect(Collectors.toSet());
    }

    /**
     * 更新文件
     *
     * @param fileId         文件ID
     * @param fileStatusEnum 文件状态
     * @return int 更行结果
     */
    public int update(Long fileId, FileStateEnum fileStatusEnum) {
        File newObj = File.builder()
                .id(fileId)
                .status(fileStatusEnum.getCode())
                .build();
        return baseMapper.updateById(newObj);
    }

    /**
     * 更新文件
     *
     * @param fileId         文件ID
     * @param fileStatusEnum 文件状态
     * @param originStatus   文件状态
     * @return boolean 更行结果
     */
    public boolean update(Long fileId, FileStateEnum fileStatusEnum, FileStateEnum originStatus) {
        if (getById(fileId) == null) {
            return true;
        }
        UpdateWrapper<File> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().eq(File::getId, fileId).eq(File::getStatus, originStatus.getCode())
                .set(File::getId, fileId).set(File::getStatus, fileStatusEnum.getCode());
        return update(updateWrapper);
    }

    /**
     * 保存文件
     *
     * @param fileId 文件ID
     * @param files  file文件
     * @return List<Long> 保存的文件id集合
     */
    @Override
    public List<File> saveFiles(Long fileId, List<FileCreateDTO> files) {
        LogUtil.debug(LogEnum.BIZ_DATASET, "save files start, file size {}", files.size());
        Long start = System.currentTimeMillis();
        Map<String, String> fail = new HashMap<>(files.size());
        List<File> newFiles = new ArrayList<>();
        Long datasetUserId = datasetService.getOneById(fileId).getCreateUserId();
        files.stream().map(file -> FileCreateDTO.toFile(file, fileId, datasetUserId)).forEach(f -> {
            try {
                newFiles.add(f);
            } catch (DuplicateKeyException e) {
                fail.put(f.getName(), "the file already exists");
            }
        });
        if (!CollectionUtils.isEmpty(fail)) {
            throw new BusinessException(ErrorEnum.FILE_EXIST, JSON.toJSONString(fail), null);
        }
        
        // 批量保存，带重试机制处理主键冲突
        int maxRetries = 3;
        int retryCount = 0;
        List<File> remainingFiles = new ArrayList<>(newFiles);
        List<File> savedFiles = new ArrayList<>();
        
        while (!remainingFiles.isEmpty() && retryCount < maxRetries) {
            try {
                // 为剩余文件分配ID
                Queue<Long> dataFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE, remainingFiles.size());
                for (File f : remainingFiles) {
                    f.setId(dataFileIds.poll());
                }
                
                // 尝试批量保存
                baseMapper.saveList(remainingFiles, JwtUtils.getCurUserId(), datasetUserId);
                savedFiles.addAll(remainingFiles);
                remainingFiles.clear();
                
            } catch (Exception e) {
                // 检查是否是主键冲突异常
                Throwable cause = e.getCause();
                if (cause instanceof SQLIntegrityConstraintViolationException || 
                    (cause != null && cause.getCause() instanceof SQLIntegrityConstraintViolationException)) {
                    
                    SQLIntegrityConstraintViolationException sqlException = null;
                    if (cause instanceof SQLIntegrityConstraintViolationException) {
                        sqlException = (SQLIntegrityConstraintViolationException) cause;
                    } else if (cause != null && cause.getCause() instanceof SQLIntegrityConstraintViolationException) {
                        sqlException = (SQLIntegrityConstraintViolationException) cause.getCause();
                    }
                    
                    if (sqlException != null && sqlException.getMessage() != null && 
                        sqlException.getMessage().contains("Duplicate entry") && 
                        sqlException.getMessage().contains("for key 'PRIMARY'")) {
                        
                        retryCount++;
                        LogUtil.warn(LogEnum.BIZ_DATASET, "主键冲突，准备重试。重试次数: {}/{}, 剩余文件数: {}", 
                            retryCount, maxRetries, remainingFiles.size());
                        
                        // 从异常信息中提取冲突的ID（如果可能）
                        // 由于批量插入时无法精确知道哪个ID冲突，我们重新为所有剩余文件分配ID
                        // 清空已分配的ID，准备重新分配
                        for (File f : remainingFiles) {
                            f.setId(null);
                        }
                        
                        // 如果达到最大重试次数，记录错误并抛出异常
                        if (retryCount >= maxRetries) {
                            LogUtil.error(LogEnum.BIZ_DATASET, "主键冲突重试失败，已达到最大重试次数: {}", maxRetries);
                            throw new BusinessException(ErrorEnum.DB_INSERT_ERROR, 
                                "批量保存文件时发生主键冲突，重试" + maxRetries + "次后仍失败，请稍后重试", e);
                        }
                        
                        // 继续重试循环
                        continue;
                    }
                }
                
                // 如果不是主键冲突异常，直接抛出
                throw e;
            }
        }
        
        if (!remainingFiles.isEmpty()) {
            LogUtil.error(LogEnum.BIZ_DATASET, "保存文件失败，仍有 {} 个文件未保存", remainingFiles.size());
            throw new BusinessException(ErrorEnum.DB_INSERT_ERROR, 
                "批量保存文件失败，仍有 " + remainingFiles.size() + " 个文件未保存，请稍后重试", null);
        }
        
        LogUtil.debug(LogEnum.BIZ_DATASET, "save files end, times {}, saved count: {}", 
            (System.currentTimeMillis() - start), savedFiles.size());
        return savedFiles;
    }

    /**
     * 保存视频文件
     *
     * @param fileId 视频文件ID
     * @param files  file文件
     * @param type   文件类型
     * @param pid    文件父ID
     * @param userId 用户ID
     * @return List<File> 文件列表
     */
    @Override
    public List<File> saveVideoFiles(Long fileId, List<FileCreateDTO> files, int type, Long pid, Long userId) {
        List<File> list = new ArrayList<>();
        Long createUserId = datasetService.getOneById(fileId).getCreateUserId();
        files.forEach(fileCreateDTO -> {
            File file = FileCreateDTO.toFile(fileCreateDTO, fileId, type, pid);
            list.add(file);
        });
        Queue<Long> dataFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE, list.size());
        for (File f : list) {
            f.setId(dataFileIds.poll());
        }
        baseMapper.saveList(list, userId, createUserId);
        return list;
    }

    /**
     * 创建查询
     *
     * @param datasetId 数据集ID
     * @param status    状态
     * @return QueryWrapper<File> 查询条件
     */
    public QueryWrapper<File> buildQuery(Long datasetId, Set<Integer> status) {
        FileQueryCriteriaVO criteria = FileQueryCriteriaVO.builder()
                .datasetId(datasetId).order("id ASC").build();
        return WrapperHelp.getWrapper(criteria);
    }

    /**
     * 获取offset
     *
     * @param datasetId 数据集ID
     * @param fileId    文件ID
     * @param type      数据集类型
     * @return Integer 获取到offset
     */
    @Override
    public Integer getOffset(Long fileId, Long datasetId, Integer[] type, Long[] labelIds) {
        Integer offset = datasetVersionFileService.getOffset(fileId, datasetId, type, labelIds);
        return offset == MagicNumConstant.ZERO ? null : offset - MagicNumConstant.ONE;
    }



    /**
     * 获取多人标注场景下特定类型文件的绝对偏移量（Service层）
     * @param datasetId 数据集ID
     * @param annotationStatus 标注状态（如101未标注）
     * @param startOffset 用户分配的起始偏移量
     * @param total 用户分配的文件总数
     * @return Integer 找到的第一个符合条件文件的偏移量，如果没找到则返回null
     */
    @Override
    public Integer getAbsoluteOffsetForAnnotationStatus(Long datasetId, Integer annotationStatus, Integer startOffset, Integer total) {
        Integer offset = datasetVersionFileService.getAbsoluteOffsetForAnnotationStatus(datasetId, annotationStatus, startOffset, total);
        return offset == MagicNumConstant.NEGATIVE_ONE ? null : offset;
    }


    /**
     * 文件查询，物体检测标注页面使用
     *
     * @param datasetId 数据集ID
     * @param offset    Offset
     * @param limit     页容量
     * @param page      分页条件
     * @param type      数据集类型
     * @return Page<File> 文件查询分页列表
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Page<File> listByLimit(Long datasetId, Long offset, Integer limit, Integer page, Integer[] type, Long[] labelId) {
        if (page == null) {
            page = MagicNumConstant.ONE;
        }
        if (offset == null) {
            offset = getDefaultOffset();
        }
        if (limit == null) {
            limit = defaultFilePageSize;
        }
        //查询数据集
        Dataset dataset = datasetService.getOneById(datasetId);
        //查询当前数据集下所有的文件(中间表)
        List<DatasetVersionFileDTO> datasetVersionFiles = datasetVersionFileService
                .getListByDatasetIdAndAnnotationStatus(dataset.getId(), dataset.getCurrentVersionName(), type, offset,
                        limit, "id", null, labelId);
        if (datasetVersionFiles == null || datasetVersionFiles.isEmpty()) {
            Page<File> filePage = new Page<>();
            filePage.setCurrent(page);
            filePage.setSize(limit);
            filePage.setTotal(NumberConstant.NUMBER_0);
            return filePage;
        }
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", datasetVersionFiles
                .stream()
                .map(DatasetVersionFileDTO::getFileId)
                .collect(Collectors.toSet())).eq("dataset_id", dataset.getId());
        List<File> files = baseMapper.selectList(queryWrapper);
        //将所有文件的状态放入
        files.forEach(v -> {
            datasetVersionFiles.forEach(d -> {
                if (v.getId().equals(d.getFileId())) {
                    v.setStatus(d.getAnnotationStatus());
                }
            });
        });
        //文件重排序（按照版本文件排序）
        List<File> fileArrayList = new ArrayList<>();
        datasetVersionFiles.forEach(v -> {
            files.forEach(f -> {
                if (v.getFileId().equals(f.getId())) {
                    f.setName(FileUtil.interceptFileNameAndDatasetId(datasetId, f.getName()));
                    fileArrayList.add(f);
                }
            });
        });
        Page<File> pages = new Page<>();
        if (!ArrayUtils.isEmpty(labelId)) {
            pages.setTotal(dataFileAnnotationService.selectDetectionCount(datasetId, dataset.getCurrentVersionName(), labelId));
        } else {
            pages.setTotal(datasetVersionFileService.selectFileListTotalCount(dataset.getId(),
                    dataset.getCurrentVersionName(), type, labelId));
        }
        pages.setRecords(fileArrayList);
        pages.setSize(limit);
        pages.setCurrent(page);
        return pages;
    }

    /**
     * 文件查询
     *
     * @param datasetId     数据集ID
     * @param page          分页条件
     * @param queryCriteria 查询条件
     * @return Map<String, Object> 文件查询列表
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Map<String, Object> listPage(Long datasetId, Page page, FileQueryCriteriaVO queryCriteria) {
        Dataset dataset = datasetService.getOneById(queryCriteria.getDatasetId());
        List<DatasetVersionFileDTO> datasetVersionFiles = commDatasetVersionFiles(datasetId, dataset.getCurrentVersionName(), page, queryCriteria);
        if (datasetVersionFiles == null || datasetVersionFiles.isEmpty()) {
            return buildPage(page);
        }
        List<File> files = getFileList(datasetVersionFiles, datasetId);
        //将所有文件的状态放入
        files.forEach(v -> {
            datasetVersionFiles.forEach(d -> {
                if (v.getId().equals(d.getFileId())) {
                    v.setStatus(d.getAnnotationStatus());
                }
                d.setVersionName(dataset.getCurrentVersionName());
            });
        });
        //文件重排序（按照版本文件排序）
        List<File> fileArrayList = new ArrayList<>();
        datasetVersionFiles.forEach(v -> {
            files.forEach(f -> {
                if (v.getFileId().equals(f.getId())) {
                    fileArrayList.add(f);
                }
            });
        });
        Map<Long, File> fileListMap = files.stream().collect(Collectors.toMap(File::getId, obj -> obj));
        List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(dataset.getId());
        Map<String, Long> labelMaps = new HashMap<>();
        datasetLabels.stream().forEach(label -> {
            labelMaps.put(label.getName(), label.getId());
        });
        List<FileVO> vos = datasetVersionFiles.stream().map(versionFile -> {
            FileVO fileVO = FileVO.builder().build();
            if (!Objects.isNull(fileListMap.get(versionFile.getFileId()))) {
                File file = fileListMap.get(versionFile.getFileId());
                BeanUtil.copyProperties(file, fileVO);
                fileVO.setLabelId(versionFile.getLabelId());
                fileVO.setPrediction(versionFile.getPrediction());
                fileVO.setAnnotation(getAnnotation(datasetId, FileUtil.interceptFileNameAndDatasetId(datasetId, file.getName()), versionFile.getVersionName(), versionFile.getChanged() == NumberConstant.NUMBER_0));
            }
            if (versionFile.getChanged() == NumberConstant.NUMBER_0) {
                String annotation = fileVO.getAnnotation();
                if (StringUtils.isNotEmpty(annotation)) {
                    JSONArray jsonArray = JSON.parseArray(annotation);
                    Long[] labels = new Long[jsonArray.size()];
                    for (int i = 0; i < jsonArray.size(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String categoryIdStr = jsonObject.getString("category_id");
                        if (!NumberUtil.isNumber(categoryIdStr)) {
                            if (labelMaps.containsKey(categoryIdStr)) {
                                jsonObject.put("category_id", labelMaps.get(categoryIdStr));
                                labels[i] = labelMaps.get(categoryIdStr);
                            }
                        } else {
                            labels[i] = jsonObject.getLong("category_id");
                        }
                    }
                    fileVO.setAnnotation(JSON.toJSONString(jsonArray));
                    fileVO.setLabelId(labels);
                }
            }
            return fileVO;
        }).collect(Collectors.toList());
        Page<File> pages = buildPages(page, files, dataset, queryCriteria);
        return PageUtil.toPage(pages, vos);
    }

    /**
     * 获取首个文件
     *
     * @param datasetId 数据集id
     * @param type      数据集类型
     * @return Long 首个文件Id
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Long getFirst(Long datasetId, String versionName, Integer type) {
        Dataset dataset = datasetService.getOneById(datasetId);
        DatasetVersionFile datasetVersionFile = datasetVersionFileService
                .getFirstByDatasetIdAndVersionNum(datasetId, StringUtils.isBlank(versionName) ? dataset.getCurrentVersionName() : versionName, FileTypeEnum.getStatus(type));
        return datasetVersionFile == null ? null : datasetVersionFile.getFileId();
    }


    /**
     * 默认offset
     *
     * @return Long 默认offset
     */
    public Long getDefaultOffset() {
        return MagicNumConstant.ZERO_LONG;
    }

    /**
     * 如果ids为空，则返回空
     *
     * @param fileIds 文件id集合
     * @return Set<File> 文件集合
     */
    @Override
    public Set<File> get(List<Long> fileIds, Long datasetId) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return new HashSet<>();
        }
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dataset_id", datasetId);
        queryWrapper.eq("id", fileIds.get(MagicNumConstant.ZERO));
        File fileOne = baseMapper.selectOne(queryWrapper);
        if (fileOne == null) {
            return new HashSet<>();
        }
        QueryWrapper<File> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper.eq("dataset_id", fileOne.getDatasetId());
        fileQueryWrapper.in("id", fileIds);
        return new HashSet(baseMapper.selectList(fileQueryWrapper));
    }

    /**
     * 视频采样任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void videoSample(String finishedQueue, String failedQueue) {
        try {
            Object object = taskUtils.getFinishedTask(finishedQueue);
            if (ObjectUtil.isNotNull(object)) {
                JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(redisUtils.get(object.toString())));
                String datasetIdAndSub = jsonObject.getString("datasetIdAndSub");
                List<String> pictureNames = JSON.parseObject(jsonObject.getString("pictureNames"), ArrayList.class);
                Integer height = Integer.valueOf(jsonObject.getString("height"));
                Integer width = Integer.valueOf(jsonObject.getString("width"));
                Long datasetId = Long.valueOf(StringUtils.substringBefore(String.valueOf(datasetIdAndSub), ":"));
                QueryWrapper<Task> taskQueryWrapper = new QueryWrapper<>();
                taskQueryWrapper.lambda().eq(Task::getId, Long.valueOf(jsonObject.getString("id")));
                Task task = taskService.selectOne(taskQueryWrapper);
                if (taskService.isStop(task.getId())) {
                    redisUtils.del(object.toString());
                    redisUtils.del(object.toString().replace("annotation", "detail"));
                    return;
                }
                Integer segment = Integer.valueOf(StringUtils.substringAfter(String.valueOf(datasetIdAndSub), ":"));
                if (segment.equals(task.getFinished() + MagicNumConstant.ONE)) {
                    try {
                        videSampleFinished(pictureNames, task, height, width);
                    } catch (Exception exception) {
                        LogUtil.error(LogEnum.BIZ_DATASET, "videoFinishedTask exception:{}", exception);
                    }
                    redisUtils.del(object.toString());
                    redisUtils.del(object.toString().replace("annotation", "detail"));
                } else {
                    //再将元素放入队列
                    redisUtils.zAdd(object.toString().replace("task", "finished"), System.currentTimeMillis() / 1000, ("\"" + object + "\"").getBytes(StandardCharsets.UTF_8));
                }
            } else {
                TimeUnit.MILLISECONDS.sleep(MagicNumConstant.THREE_THOUSAND);
            }
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "get videoSample finish task failed:{}", e);
        }
        try {
            Object object = taskUtils.getFailedTask(failedQueue);
            if (ObjectUtil.isNotNull(object)) {
                String taskId = object.toString();
                JSONObject jsonObject = JSONObject.parseObject(JSON.toJSONString(redisUtils.get(taskId)));
                String datasetIdAndSub = jsonObject.getString("datasetIdAndSub");
                videoSampleFailed(datasetIdAndSub);
            }
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "get videoSample failed task failed:{}", e);
        }
    }

    /**
     * 采样失败任务处理
     *
     * @param failedId 采样失败任务ID
     */
    public void videoSampleFailed(String failedId) {
        Long datasetId = Long.valueOf(StringUtils.substringBefore(String.valueOf(failedId), ":"));
        //创建入参请求体
        StateChangeDTO stateChangeDTO = new StateChangeDTO();
        //创建需要执行事件的方法的传入参数
        Object[] objects = new Object[1];
        objects[0] = datasetId.intValue();
        stateChangeDTO.setObjectParam(objects);
        //添加需要执行的状态机类
        stateChangeDTO.setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        //采样失败事件
        stateChangeDTO.setEventMethodName(DataStateMachineConstant.DATA_SAMPLING_FAILURE_EVENT);
        StateMachineUtil.stateChange(stateChangeDTO);
    }

    /**
     * 采样完成任务处理
     *
     * @param picNames 完成后图片名称
     * @param task     采样任务
     */
    public void videSampleFinished(List<String> picNames, Task task, Integer height, Integer width) {
    QueryWrapper<File> queryWrapper = new QueryWrapper<>();
    //重复抽帧 不进行mp4状态判断
    queryWrapper.lambda().eq(File::getDatasetId, task.getDatasetId())
            .eq(File::getFileType, MagicNumConstant.ONE)
//                .eq(File::getStatus, FileTypeEnum.UNFINISHED.getValue())
            .eq(File::getId, task.getTargetId());
    File file = getBaseMapper().selectOne(queryWrapper);
    saveVideoPic(picNames, file, height, width);
    // finished已经在VideoSampleQueueExecute中通过finishFileIfMatch原子更新了，这里不需要再更新
    // 重新查询task获取最新的finished值
    QueryWrapper<Task> taskQueryWrapper = new QueryWrapper<>();
    taskQueryWrapper.lambda().eq(Task::getId, task.getId());
    task = taskService.selectOne(taskQueryWrapper);
    //单个视频采样完成
    if (task.getTotal().equals(task.getFinished())) {
        file.setStatus(FileStateCodeConstant.AUTO_TAG_COMPLETE_FILE_STATE);
        getBaseMapper().updateFileStatus(file.getDatasetId(), file.getId(), file.getStatus());
        //判断有无没完成的抽帧任务,有就不变更状态机
        QueryWrapper<Task> taskQueryWrapper2 = new QueryWrapper<>();
        taskQueryWrapper2.lambda().eq(Task::getDatasetId, task.getDatasetId())
                .eq(Task::getType, task.getType())
                .in(Task::getStatus, Arrays.asList(MagicNumConstant.ZERO, MagicNumConstant.ONE));
        List<Task> unfinishedTaskList = taskService.selectByQueryWrapper(taskQueryWrapper2);

        if (unfinishedTaskList.isEmpty()) {
            //创建入参请求体
            StateChangeDTO stateChangeDTO = new StateChangeDTO();
            //创建需要执行事件的方法的传入参数
            Object[] objects = new Object[1];
            objects[0] = file.getDatasetId().intValue();
            stateChangeDTO.setObjectParam(objects);
            //添加需要执行的状态机类
            stateChangeDTO.setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
            //采样事件
            stateChangeDTO.setEventMethodName(DataStateMachineConstant.DATA_SAMPLING_EVENT);
            StateMachineUtil.stateChange(stateChangeDTO);
        }
    }
}

    /**
     * 保存采样后文件
     *
     * @param picNames 图片文件名字
     * @param file     视频文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveVideoPic(List<String> picNames, File file, Integer height, Integer width) {
        Collections.reverse(picNames);
        List<FileCreateDTO> fileCreateDTOS = new ArrayList<>();
        picNames.forEach(picName -> {
            picName = StringUtils.substringAfter(picName, prefixPath);
            FileCreateDTO f = FileCreateDTO.builder()
                    .height(height)
                    .width(width)
                    .url(picName)
                    .build();
            fileCreateDTOS.add(f);
        });
        List<File> files = saveVideoFiles(file.getDatasetId(), fileCreateDTOS, DatatypeEnum.IMAGE.getValue(), file.getId(), file.getCreateUserId());
        List<DatasetVersionFile> datasetVersionFiles = new ArrayList<>();
        //当前dataset和版本号
        Dataset dataset = datasetMapper.selectById(file.getDatasetId());
        String currentVersionName = Optional.ofNullable(dataset).map(Dataset::getCurrentVersionName).orElse(null);
        files.forEach(fileOne -> {
            DatasetVersionFile datasetVersionFile = new DatasetVersionFile(file.getDatasetId(), currentVersionName, fileOne.getId(), fileOne.getName());
            datasetVersionFiles.add(datasetVersionFile);
        });
        datasetVersionFileService.insertList(datasetVersionFiles);
    }

    /**
     * 批量更新file
     *
     * @param datasetVersionFiles 文件列表
     * @param init                更新结果
     */
    public void updateStatus(List<DatasetVersionFile> datasetVersionFiles, FileStateEnum init) {
        List<Long> fileIds = datasetVersionFiles
                .stream().map(DatasetVersionFile::getFileId)
                .collect(Collectors.toList());
        UpdateWrapper<File> fileUpdateWrapper = new UpdateWrapper();
        fileUpdateWrapper.in("id", fileIds);
        File file = new File();
        file.setStatus(init.getCode());
        baseMapper.update(file, fileUpdateWrapper);
    }


    /**
     * 获取文件对应所有增强文件
     *
     * @param fileId 文件id
     * @return List<File> 文件对应所有增强文件
     */
    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public List<File> getEnhanceFileList(Long fileId, Long datasetId) {
        File file = baseMapper.getOneById(fileId, datasetId);

        if (ObjectUtil.isNull(file)) {
            throw new BusinessException(ErrorEnum.FILE_ABSENT);
        }
        Dataset dataset = datasetService.getOneById(file.getDatasetId());
        if (ObjectUtil.isNull(dataset)) {
            throw new BusinessException(ErrorEnum.DATASET_ABSENT);
        }
        int enhanceFileCount = datasetVersionFileService.getEnhanceFileCount(dataset.getId(), dataset.getCurrentVersionName());
        if (enhanceFileCount > 0) {
            return datasetVersionFileService.getEnhanceFileList(dataset.getId(), dataset.getCurrentVersionName(), fileId);
        }
        return null;
    }

    /**
     * 获取文件详情
     *
     * @param fileId 文件ID
     * @return File 文件详情
     */
    @Override
    public File selectById(Long fileId, Long datasetId) {
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dataset_id", datasetId);
        queryWrapper.eq("id", fileId);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 条件搜索获取文件详情
     *
     * @param queryWrapper 查询条件
     * @return 文件详情
     */
    @Override
    public File selectOne(QueryWrapper<File> queryWrapper) {
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 获取文件列表
     *
     * @param wrapper 查询条件
     * @return 文件列表
     */
    @Override
    public List<File> listFile(QueryWrapper<File> wrapper) {
        return list(wrapper);
    }

    /**
     * 批量获取文件列表
     *
     * @param datasetId 数据集ID
     * @param offset    偏移量
     * @param batchSize 批大小
     * @param status    文件标注状态
     * @return 文件列表
     */
    @Override
    public List<File> listBatchFile(Long datasetId, int offset, int batchSize, Collection<Integer> status) {
        try {
            Dataset dataset = datasetService.getOneById(datasetId);
            return baseMapper.selectListOne(datasetId, dataset.getCurrentVersionName(), offset, batchSize, status);
        } catch (Exception e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "get annotation files error {}", e);
            return null;
        }
    }

    /**
     * 判断执行中的采样任务是否过期
     */
    @Override
    public void expireSampleTask() {
        Set<ZSetOperations.TypedTuple<Object>> typedTuples = taskUtils.zGetWithScore(START_SAMPLE_QUEUE);
        typedTuples.forEach(value -> {
            String timestampString = new BigDecimal(StringUtils.substringBefore(value.getScore().toString(), "."))
                    .toPlainString();
            long timestamp = Long.parseLong(timestampString);
            String keyId = JSONObject.parseObject(JSON.toJSONString(value.getValue())).getString("datasetIdKey");
            long timestampNow = System.currentTimeMillis() / 1000;
            if (timestampNow - timestamp > MagicNumConstant.TWO_HUNDRED) {
                LogUtil.info(LogEnum.BIZ_DATASET, "restart videoSample task keyId:{}", keyId);
//                taskUtils.restartTask(keyId, START_SAMPLE_QUEUE, SAMPLE_PENDING_QUEUE, DETAIL_NAME
//                        , JSON.toJSONString(value.getValue()));
            }
        });
    }

    /**
     * 根据版本和数据集ID获取文件url
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名
     * @return List<String> url列表
     */
    @Override
    public List<String> selectUrls(Long datasetId, String versionName) {
        return baseMapper.selectUrls(datasetId, versionName);
    }

    @Override
    public List<String> selectUrlsFromFileStatusNormalOrDeleted(Long datasetId, String versionName) {
        return baseMapper.selectUrlsFromFileStatusNormalOrDeleted(datasetId, versionName);
    }

    /**
     * 根据version.changed获取文件name列表
     *
     * @param datasetId   数据集ID
     * @param changed     版本文件是否改动
     * @param versionName 版本名称
     * @return List<FileAnnotationBO>   名称列表
     */
    @Override
    public List<FileAnnotationBO> selectFileAnnotations(Long datasetId, Integer changed, String versionName) {
        return baseMapper.selectFileAnnotations(datasetId, changed, versionName);
    }

    /**
     * 公共获取版本文件列表
     *
     * @param datasetId          数据集ID
     * @param currentVersionName 当前版本文件
     * @param page               分页
     * @param queryCriteria      查询实体
     * @return List<DatasetVersionFileDTO> 版本文件列表
     */
    private List<DatasetVersionFileDTO> commDatasetVersionFiles(Long datasetId, String currentVersionName, Page page, FileQueryCriteriaVO queryCriteria) {
        queryCriteria.setDatasetId(datasetId);
        queryCriteria.setFileType(DatatypeEnum.IMAGE.getValue());

        Integer[] status = findStatus(queryCriteria.getStatus(), queryCriteria.getAnnotateStatus(), queryCriteria.getAnnotateType());

        //根据数据集ID和版本名称以及状态查询出当前数据集的所有文件
        List<DatasetVersionFileDTO> datasetVersionFiles = datasetVersionFileService
                .getListByDatasetIdAndAnnotationStatus(datasetId,
                        currentVersionName,
                        status,
                        (page.getCurrent() - 1) * page.getSize(),
                        (int) page.getSize(),
                        queryCriteria.getSort(),
                        queryCriteria.getOrder(),
                        queryCriteria.getLabelId()
                );
        return datasetVersionFiles;
    }

    /**
     * 图像分类筛选状态
     *
     * @param status         数据集状态
     * @param annotateStatus 数据集标注状态
     * @param annotateType   数据集标注方式
     * @return Integer[] 状态数组
     */
    private Integer[] findStatus(Integer[] status, Integer[] annotateStatus, Integer[] annotateType) {
        Set<Integer> statusResult = new HashSet<>();
        // 根据有无标注信息参数获取对应文件状态列表
        statusResult.addAll(FileTypeEnum.getStatus(status[0]));
        // 如果有标注状态，则需要获取标注状态，并和有无标注信息对应状态取交集
        if (annotateStatus != null && annotateStatus.length > 0) {
            statusResult.retainAll(FileTypeEnum.getStatus(Arrays.asList(annotateStatus)));
        }
        // 如果有标注方式，则需要获取对应文件状态，并和有无标注信息对应状态取交集
        if (annotateType != null && annotateType.length > 0) {
            statusResult.retainAll(FileTypeEnum.getStatus(Arrays.asList(annotateType)));
        }
        // 用于解决当用户选择状态，但是交集为空时会查出所有从而导致条件失效的问题
        statusResult.add(-1);
        Integer[] statusList = new Integer[statusResult.size()];
        statusResult.toArray(statusList);
        return statusList;
    }


    /**
     * 构建分页数据
     *
     * @param page 分页参数
     * @return Map<String, Object> 分页实体
     */
    private Map<String, Object> buildPage(Page page) {
        return PageUtil.toPage(new Page<File>() {{
            setCurrent(page.getCurrent());
            setSize(page.getSize());
            setTotal(NumberConstant.NUMBER_0);
        }}, new ArrayList<FileVO>());
    }

    /**
     * 音频数据集文件查询
     *
     * @param datasetId     数据集id
     * @param page          分页条件
     * @param queryCriteria 查询文件参数
     * @return Map<String, Object> 文件查询列表
     */
    @Override
    public Map<String, Object> audioFilesByPage(Long datasetId, Page page, FileQueryCriteriaVO queryCriteria) {
        //查询数据集
        Dataset dataset = datasetService.getOneById(queryCriteria.getDatasetId());
        if (DatasetTypeEnum.PUBLIC.getValue().compareTo(dataset.getType()) == 0) {
            DataContext.set(CommonPermissionDataDTO.builder().type(true).build());
        }
        List<File> files = new ArrayList<>();
        List<TxtFileVO> vos = new ArrayList<>();
        try {
            List<DatasetVersionFileDTO> datasetVersionFiles = commDatasetVersionFiles(datasetId, dataset.getCurrentVersionName(), page, queryCriteria);
            if (datasetVersionFiles == null || datasetVersionFiles.isEmpty()) {
                return buildPage(page);
            }
            files = getFileList(datasetVersionFiles, datasetId);
            Map<Long, File> fileListMap = files.stream().collect(Collectors.toMap(File::getId, obj -> obj));
            vos = datasetVersionFiles.stream().map(versionFile -> {
                TxtFileVO fileVO = TxtFileVO.builder().build();
                if (!Objects.isNull(fileListMap.get(versionFile.getFileId()))) {
                    File file = fileListMap.get(versionFile.getFileId());
                    BeanUtil.copyProperties(file, fileVO);
                    fileVO.setPrediction(versionFile.getPrediction());
                    fileVO.setLabelId(versionFile.getLabelId());
                    fileVO.setAbstractName(Constant.ABSTRACT_NAME_PREFIX + file.getName());
                    String afterPath = StringUtils.substringAfterLast(fileVO.getUrl(), SymbolConstant.SLASH);
                    String beforePath = StringUtils.substringBeforeLast(fileVO.getUrl(), SymbolConstant.SLASH);
                    String newPath = beforePath + SymbolConstant.SLASH + ABSTRACT_NAME_PREFIX + afterPath;
                    fileVO.setAbstractUrl(newPath);
                    fileVO.setStatus(versionFile.getAnnotationStatus());
                    fileVO.setAnnotation(getAnnotation(datasetId, FileUtil.interceptFileNameAndDatasetId(datasetId, file.getName()), versionFile.getVersionName(), versionFile.getChanged() == NumberConstant.NUMBER_0));
                }
                return fileVO;
            }).collect(Collectors.toList());
        } finally {
            if (DatasetTypeEnum.PUBLIC.getValue().compareTo(dataset.getType()) == 0) {
                DataContext.remove();
            }
        }
        Page<File> pages = buildPages(page, files, dataset, queryCriteria);
        return PageUtil.toPage(pages, vos);
    }

//    /**
//     * 文本数据集文件查询
//     *
//     * @param datasetId         数据集id
//     * @param page              分页条件
//     * @param fileQueryCriteria 查询文件参数
//     * @return Map<String, Object> 文件查询列表
//     */
//    @Override
//    public Map<String, Object> txtContentByPage(Long datasetId, Page page, FileQueryCriteriaVO fileQueryCriteria) {
//        SearchRequest searchRequest = new SearchRequest(esIndex);
//        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        QueryBuilder queryBuilder = null;
//        BoolQueryBuilder boolQueryBuilder = null;
//        if (fileQueryCriteria.getAnnotateType() == null || fileQueryCriteria.getAnnotateType().length == 0) {
//            if (fileQueryCriteria.getStatus()[0].equals(FileTypeEnum.UNFINISHED_FILE.getValue())) {
//                boolQueryBuilder = QueryBuilders.boolQuery()
//                        .must(fileQueryCriteria.getContent() == null ? QueryBuilders.matchAllQuery()
//                                : QueryBuilders.matchPhraseQuery("content", fileQueryCriteria.getContent()))
//                        .must(QueryBuilders.termQuery("datasetId", fileQueryCriteria.getDatasetId().toString()))
//                        .must(QueryBuilders.termsQuery("status"
//                                , FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE.toString()
//                                , FileStateCodeConstant.ANNOTATION_NOT_DISTINGUISH_FILE_STATE.toString()));
//            }
//            if (fileQueryCriteria.getStatus()[0].equals(FileTypeEnum.FINISHED_FILE.getValue())) {
//                boolQueryBuilder = QueryBuilders.boolQuery()
//                        .must(fileQueryCriteria.getContent() == null ? QueryBuilders.matchAllQuery()
//                                : QueryBuilders.matchPhraseQuery("content", fileQueryCriteria.getContent()))
//                        .must(QueryBuilders.termQuery("datasetId", fileQueryCriteria.getDatasetId().toString()))
//                        .must(QueryBuilders.termsQuery("status"
//                                , FileStateCodeConstant.AUTO_TAG_COMPLETE_FILE_STATE.toString()
//                                , FileStateCodeConstant.ANNOTATION_COMPLETE_FILE_STATE.toString()));
//            }
//            if (fileQueryCriteria.getStatus()[0].equals(FileTypeEnum.HAVE_ANNOTATION.getValue())) {
//                boolQueryBuilder = QueryBuilders.boolQuery()
//                        .must(fileQueryCriteria.getContent() == null ? QueryBuilders.matchAllQuery()
//                                : QueryBuilders.matchPhraseQuery("content", fileQueryCriteria.getContent()))
//                        .must(QueryBuilders.termQuery("datasetId", fileQueryCriteria.getDatasetId().toString()))
//                        .must(QueryBuilders.termsQuery("status"
//                                , FileStateCodeConstant.MANUAL_ANNOTATION_FILE_STATE.toString()
//                                , FileStateCodeConstant.AUTO_TAG_COMPLETE_FILE_STATE.toString()
//                                , FileStateCodeConstant.ANNOTATION_COMPLETE_FILE_STATE.toString()
//                                , FileStateCodeConstant.TARGET_COMPLETE_FILE_STATE.toString()));
//            }
//            if (fileQueryCriteria.getStatus()[0].equals(FileTypeEnum.NO_ANNOTATION.getValue())) {
//                boolQueryBuilder = QueryBuilders.boolQuery()
//                        .must(fileQueryCriteria.getContent() == null ? QueryBuilders.matchAllQuery()
//                                : QueryBuilders.matchPhraseQuery("content", fileQueryCriteria.getContent()))
//                        .must(QueryBuilders.termQuery("datasetId", fileQueryCriteria.getDatasetId().toString()))
//                        .must(QueryBuilders.termsQuery("status"
//                                , FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE.toString()
//                                , FileStateCodeConstant.ANNOTATION_NOT_DISTINGUISH_FILE_STATE.toString()));
//            }
//        } else {
//            if (fileQueryCriteria.getAnnotateType().length == MagicNumConstant.ONE) {
//                boolQueryBuilder = QueryBuilders.boolQuery()
//                        .must(fileQueryCriteria.getContent() == null ? QueryBuilders.matchAllQuery()
//                                : QueryBuilders.matchPhraseQuery("content", fileQueryCriteria.getContent()))
//                        .must(QueryBuilders.termQuery("datasetId", fileQueryCriteria.getDatasetId().toString()))
//                        .must(QueryBuilders.termsQuery("status"
//                                , fileQueryCriteria.getAnnotateType()[0].toString()));
//            } else if (fileQueryCriteria.getAnnotateType().length == MagicNumConstant.TWO) {
//                boolQueryBuilder = QueryBuilders.boolQuery()
//                        .must(fileQueryCriteria.getContent() == null ? QueryBuilders.matchAllQuery()
//                                : QueryBuilders.matchPhraseQuery("content", fileQueryCriteria.getContent()))
//                        .must(QueryBuilders.termQuery("datasetId", fileQueryCriteria.getDatasetId().toString()))
//                        .must(QueryBuilders.termsQuery("status"
//                                , fileQueryCriteria.getAnnotateType()[0].toString()
//                                , fileQueryCriteria.getAnnotateType()[1].toString()));
//            }
//        }
//        Dataset dataset = datasetService.getOneById(datasetId);
//        boolQueryBuilder.must(QueryBuilders.matchPhraseQuery("versionName", StringUtils.isEmpty(dataset.getCurrentVersionName()) ? "V0000" : dataset.getCurrentVersionName()));
//        if (fileQueryCriteria.getLabelId() != null) {
//            queryBuilder = boolQueryBuilder.must(QueryBuilders.termsQuery("labelId", fileQueryCriteria.getLabelId()));
//        } else {
//            queryBuilder = boolQueryBuilder;
//        }
//        sourceBuilder.query(queryBuilder);
//        sourceBuilder.from((int) (page.getSize() * (page.getCurrent() - 1)));
//        sourceBuilder.size((int) page.getSize());
//        sourceBuilder.sort(new FieldSortBuilder("updateTime.keyword").order(SortOrder.DESC).unmappedType("long"));
//        sourceBuilder.sort(new FieldSortBuilder("createTime.keyword").order(SortOrder.DESC).unmappedType("long"));
//        HighlightBuilder highlightBuilder = new HighlightBuilder();
//        highlightBuilder.preTags("<font class='highlight'>");
//        highlightBuilder.postTags("</font>");
//        highlightBuilder.field("content");
//        sourceBuilder.highlighter(highlightBuilder);
//        sourceBuilder.trackTotalHits(true);
//        searchRequest.source(sourceBuilder);
//        List<TxtFileVO> vos = new ArrayList<>();
//        try {
//            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//            SearchHit[] hits = searchResponse.getHits().getHits();
//            for (int i = 0; i < hits.length; i++) {
//                EsDataFileDTO esDataFileDTO = JSON.parseObject(hits[i].getSourceAsString(), EsDataFileDTO.class);
//                StringBuilder highlightContent = new StringBuilder();
//                if (fileQueryCriteria.getContent() != null) {
//                    Map<String, HighlightField> highlightFields = hits[i].getHighlightFields();
//                    Text[] fragments = highlightFields.get("content").getFragments();
//                    for (Text text : fragments) {
//                        highlightContent.append(text);
//                    }
//                }
//                TxtFileVO txtFileVO = new TxtFileVO();
//                txtFileVO.setPrediction(esDataFileDTO.getPrediction());
//                txtFileVO.setContent(fileQueryCriteria.getContent() == null ? esDataFileDTO.getContent() : highlightContent.toString());
//                txtFileVO.setName(esDataFileDTO.getName());
//                txtFileVO.setDatasetId(esDataFileDTO.getDatasetId());
//                txtFileVO.setStatus(esDataFileDTO.getStatus());
//                txtFileVO.setId(Long.parseLong(hits[i].getId()));
//                txtFileVO.setLabelId(esDataFileDTO.getLabelId());
//                txtFileVO.setAnnotation(esDataFileDTO.getAnnotation());
//                vos.add(txtFileVO);
//            }
//            page.setTotal(searchResponse.getHits().getTotalHits().value);
//        } catch (IOException e) {
//            LogUtil.error(LogEnum.BIZ_DATASET, "search text from es error:{}", e);
//        }
//        return PageUtil.toPage(page, vos);
//    }

    /**
     * 文本状态数量统计
     *
     * @param datasetId               数据集ID
     * @param fileScreenStatSearchDTO 文件查询条件
     * @return ProgressVO 文本状态数量统计
     */
    @Override
    public FileScreenStatVO getFileCountByStatus(Long datasetId, FileScreenStatSearchDTO fileScreenStatSearchDTO) {
        Dataset dataset = datasetService.getOneById(datasetId);
        Set<Integer> statusResult = Arrays.stream(findStatus(new Integer[]{fileScreenStatSearchDTO.getAnnotationResult()},
                fileScreenStatSearchDTO.getAnnotationStatus(),
                fileScreenStatSearchDTO.getAnnotationMethod())).collect(Collectors.toSet());
        Long haveAnnotation = FileTypeEnum.HAVE_ANNOTATION.getValue() == fileScreenStatSearchDTO.getAnnotationResult().intValue()
                ? getFileCount(dataset, statusResult, fileScreenStatSearchDTO.getLabelIds())
                : getFileCount(dataset, FileTypeEnum.getStatus(FileTypeEnum.HAVE_ANNOTATION.getValue()), null);
        Long noAnnotation = FileTypeEnum.NO_ANNOTATION.getValue() == fileScreenStatSearchDTO.getAnnotationResult().intValue()
                ? getFileCount(dataset, statusResult, fileScreenStatSearchDTO.getLabelIds())
                : getFileCount(dataset, FileTypeEnum.getStatus(FileTypeEnum.NO_ANNOTATION.getValue()), null);
        return FileScreenStatVO.builder().haveAnnotation(haveAnnotation).noAnnotation(noAnnotation).build();
    }

    /**
     * 获取数据集数量
     *
     * @param dataset    数据集
     * @param fileStatus 查询文件状态
     * @return Long              文本文件数量
     */
    private Long getFileCount(Dataset dataset, Set<Integer> fileStatus, List<Long> labelIds) {
        return datasetVersionFileService.getVersionFileCountByStatusVersionAndLabelId(dataset.getId(), fileStatus, dataset.getCurrentVersionName(), labelIds);
    }

    /**
     * 获取文件列表
     *
     * @param datasetVersionFiles 数据集版本文件列表
     * @param datasetId           数据集ID
     * @return List<File> 文件列表
     */
    private List<File> getFileList(List<DatasetVersionFileDTO> datasetVersionFiles, Long datasetId) {
        Set<Long> set = datasetVersionFiles
                .stream()
                .map(DatasetVersionFileDTO::getFileId)
                .collect(Collectors.toSet());
        QueryWrapper queryWrapper = new QueryWrapper<>()
                .in("id", set)
                .eq("dataset_id", datasetId);
        List<File> files = baseMapper.selectList(queryWrapper);

        return files;
    }


    /**
     * 构建文件列表分页
     *
     * @param page          分页条件
     * @param files         文件列表
     * @param dataset       数据集实体
     * @param queryCriteria 查询条件
     * @return ge<File> 分页结果
     */
    private Page<File> buildPages(Page page, List<File> files, Dataset dataset, FileQueryCriteriaVO queryCriteria) {
        Page<File> pages = new Page<>();
        Integer[] status = findStatus(queryCriteria.getStatus(), queryCriteria.getAnnotateStatus(), queryCriteria.getAnnotateType());
        pages.setTotal(datasetVersionFileService.selectFileListTotalCount(dataset.getId(),
                dataset.getCurrentVersionName(), status, queryCriteria.getLabelId()));
        pages.setRecords(files);
        pages.setSize(page.getSize());
        pages.setCurrent(page.getCurrent());
        return pages;
    }

    /**
     * 获取数据集文件数量
     *
     * @param datasetId 数据集ID
     * @return 数据集文件数量
     */
    @Override
    public int getFileCountByDatasetId(Long datasetId) {
        QueryWrapper<File> queryWrapper = new QueryWrapper();
        queryWrapper.lambda().eq(File::getDeleted, 0).eq(File::getDatasetId, datasetId);
        return baseMapper.selectCount(queryWrapper);
    }

    /**
     * 获取数据集原图文件数量
     *
     * @param datasetId   数据集ID
     * @param versionName 版本名称
     * @return 数据集原图文件数量
     */
    @Override
    public int getOriginalFileCountOfDataset(Long datasetId, String versionName) {
        return fileMapper.getOriginalFileCountOfDataset(datasetId, versionName);
    }

    /**
     * 备份数据集文件数据
     *
     * @param originDataset 原数据集实体
     * @param targetDataset 目标数据集实体
     * @return 文件数据
     */
    @Override
    public List<File> backupFileDataByDatasetId(Dataset originDataset, Dataset targetDataset) {
        Long pid = 0L;
        List<File> fileList = null;
        List<File> files = baseMapper.selectList(new LambdaQueryWrapper<File>().eq(File::getDatasetId, originDataset.getId())
                .ne(File::getFileType, MagicNumConstant.ONE).or().isNull(File::getFileType));
        if (!CollectionUtils.isEmpty(files)) {
            Queue<Long> dataFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE, files.size());
            for (int i = 0; i < files.size(); i++) {
                File f = files.get(i);
                f.setId(dataFileIds.poll());
                if (!Objects.isNull(f.getFileType()) && f.getFileType().compareTo(MagicNumConstant.ONE) == 0) {
                    f.setUrl(f.getUrl().replace(originDataset.getId().toString(), targetDataset.getId().toString()));
                    f.setName(FileUtil.spliceFileNameAndDatasetId(targetDataset.getId(), f.getName()));
                    baseMapper.insert(f);
                    pid = f.getId();
                    files.remove(i);
                }
            }
            Long finalPid = pid;
            fileList = files.stream().map(a -> {
                File file = File.builder()
                        .id(a.getId())
                        .fileType(a.getFileType())
                        .datasetId(targetDataset.getId())
                        .enhanceType(a.getEnhanceType())
                        .frameInterval(a.getFrameInterval())
                        .height(a.getHeight())
                        .name(a.getName())
                        .status(a.getStatus())
                        .pid(finalPid)
                        .originUserId(MagicNumConstant.ZERO_LONG)
                        .width(a.getWidth())
                        .url(a.getUrl().replace(originDataset.getId().toString() + SymbolConstant.SLASH
                                , targetDataset.getId().toString() + SymbolConstant.SLASH))
                        .build();
                file.setCreateUserId(targetDataset.getCreateUserId());
                file.setUpdateUserId(file.getCreateUserId());
                file.setDeleted(false);
                return file;
            }).collect(Collectors.toList());
            List<List<File>> splitFiles = CollectionUtil.split(fileList, MagicNumConstant.FOUR_THOUSAND);
            splitFiles.forEach(splitFile -> baseMapper.insertBatch(splitFile));
        }

        return fileList;

    }

//    /**
//     * 将文本数据同步至ES
//     *
//     * @param dataset 数据集
//     */
//    @Override
//    public void transportTextToEs(Dataset dataset, List<Long> fileIdsNotToEs, Boolean ifImport) {
//        List<EsTransportDTO> esTransportDTOList = fileMapper.selectTextDataNoTransport(dataset.getId(), fileIdsNotToEs, ifImport);
//        if (ifImport != null && ifImport) {
//            List<TextAnnotationBO> textAnnotationBOS = fileMapper.selectTextAnnotation(dataset.getId(), fileIdsNotToEs);
//            Map<Long, List<TextAnnotationBO>> annotationGroup = textAnnotationBOS.stream().collect(Collectors.groupingBy(TextAnnotationBO::getId));
//            esTransportDTOList.stream().forEach(esTransportDTO -> {
//                List<TextAnnotationBO> annotationsById = annotationGroup.get(esTransportDTO.getId());
//                List<Long> labelIds = annotationsById.stream().map(TextAnnotationBO::getLabelId).collect(Collectors.toList());
//                esTransportDTO.setLabelId(labelIds.toArray(new Long[labelIds.size()]));
//                JSONArray annotations = new JSONArray();
//                annotationsById.forEach(annotation -> {
//                    JSONObject jsonObject = new JSONObject();
//                    jsonObject.put("category_id", annotation.getLabelId());
//                    jsonObject.put("prediction", annotation.getPrediction());
//                    annotations.add(jsonObject);
//                });
//                esTransportDTO.setAnnotation(annotations.toJSONString());
//            });
//        }
//        esTransportDTOList.forEach(esTransportDTO -> {
//            FileInputStream fileInputStream = null;
//            InputStreamReader reader = null;
//            BufferedReader bufferedReader = null;
//            try {
//                String url = prefixPath + esTransportDTO.getUrl();
//                fileInputStream = new FileInputStream(url);
//                reader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
//                bufferedReader = new BufferedReader(reader);
//                StringBuffer testContent = new StringBuffer();
//                String tempContent;
//                while ((tempContent = bufferedReader.readLine()) != null) {
//                    testContent.append(tempContent);
//                }
//                Map<String, Object> jsonMap = new HashMap<>();
//                jsonMap.put("content", testContent.toString());
//                jsonMap.put("name", esTransportDTO.getFileName());
//                jsonMap.put("status", esTransportDTO.getAnnotationStatus().toString());
//                jsonMap.put("datasetId", dataset.getId().toString());
//                jsonMap.put("createUserId", esTransportDTO.getCreateUserId() == null ? null : esTransportDTO.getCreateUserId().toString());
//                jsonMap.put("createTime", esTransportDTO.getCreateTime().toString());
//                jsonMap.put("updateUserId", esTransportDTO.getUpdateUserId() == null ? null : esTransportDTO.getUpdateUserId().toString());
//                jsonMap.put("updateTime", esTransportDTO.getUpdateTime().toString());
//                jsonMap.put("fileType", esTransportDTO.getFileType() == null ? null : esTransportDTO.getFileType().toString());
//                jsonMap.put("enhanceType", esTransportDTO.getEnhanceType() == null ? null : esTransportDTO.getEnhanceType().toString());
//                jsonMap.put("originUserId", esTransportDTO.getOriginUserId().toString());
//                jsonMap.put("prediction", esTransportDTO.getPrediction() == null ? null : esTransportDTO.getPrediction().toString());
//                jsonMap.put("labelId", esTransportDTO.getLabelId() == null ? null : esTransportDTO.getLabelId());
//                jsonMap.put("annotation", esTransportDTO.getAnnotation() == null ? null : esTransportDTO.getAnnotation());
//                jsonMap.put("versionName", StringUtils.isEmpty(dataset.getCurrentVersionName()) ? "V0000" : dataset.getCurrentVersionName());
//                IndexRequest request = new IndexRequest(esIndex);
//                request.source(jsonMap);
//                request.id(esTransportDTO.getId().toString());
//                bulkProcessor.add(request);
//                LogUtil.info(LogEnum.BIZ_DATASET, "transport one text to es:{}", esTransportDTO.getUrl());
//            } catch (Exception e) {
//                LogUtil.error(LogEnum.BIZ_DATASET, "transport text to es error:{}", e);
//            } finally {
//                try {
//                    fileInputStream.close();
//                    reader.close();
//                    bufferedReader.close();
//                } catch (Exception e) {
//                    LogUtil.error(LogEnum.BIZ_DATASET, "transport text to es error:{}", e);
//                }
//            }
//        });
//        bulkProcessor.flush();
//        List<Long> fileIds = new ArrayList<>();
//        esTransportDTOList.forEach(esTransportDTO -> fileIds.add(esTransportDTO.getId()));
//        fileMapper.updateEsStatus(dataset.getId(), fileIds);
//    }

//    /**
//     * 还原es_transport状态
//     *
//     * @param datasetId 数据集ID
//     * @param fileId    文件ID
//     */
//    @Override
//    public void recoverEsStatus(Long datasetId, Long fileId) {
//        fileMapper.recoverEsStatus(datasetId, fileId);
//    }

//    /**
//     * 删除es中数据
//     *
//     * @param fileIds 文件ID数组
//     */
//    @Override
//    public void deleteEsData(Long[] fileIds) {
//        for (Long fileId : fileIds) {
//            DeleteRequest deleteRequest = new DeleteRequest(esIndex, fileId.toString());
//            deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
//            try {
//                DeleteResponse delete = restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
//                ReplicationResponse.ShardInfo shardInfo = delete.getShardInfo();
//                if (shardInfo.getFailed() > MagicNumConstant.ZERO) {
//                    throw new BusinessException(ErrorEnum.ES_DATA_DELETE_ERROR);
//                }
//            } catch (IOException e) {
//                LogUtil.error(LogEnum.BIZ_DATASET, "delete es data error:{}", e);
//                throw new BusinessException(ErrorEnum.ES_DATA_DELETE_ERROR);
//            }
//        }
//
//    }

    /**
     * 文本数据集csv导入
     * 1.文本地址写入到datafile表
     * 2.生成一条任务数据
     *
     * @param datasetCsvImportDTO 导入信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tableImport(DatasetCsvImportDTO datasetCsvImportDTO) {
        Dataset dataset = datasetService.getOneById(datasetCsvImportDTO.getDatasetId());
        File file = File.builder().build().setName(datasetCsvImportDTO.getFileName())
                .setStatus(FileStateCodeConstant.NOT_ANNOTATION_FILE_STATE)
                .setDatasetId(datasetCsvImportDTO.getDatasetId())
                .setUrl(datasetCsvImportDTO.getFilePath())
                .setFileType(MagicNumConstant.TWO)
                .setPid(0L)
                .setOriginUserId(contextService.getCurUserId())
                .setExcludeHeader(datasetCsvImportDTO.getExcludeHeader() == null || datasetCsvImportDTO.getExcludeHeader());
        Queue<Long> dataFileIds = generatorKeyUtil.getSequenceByBusinessCode(Constant.DATA_FILE, 1);
        file.setId(dataFileIds.poll());
        baseMapper.saveList(Collections.singletonList(file), contextService.getCurUserId(), dataset.getCreateUserId());
        Task task = Task.builder().build().setDatasetId(datasetCsvImportDTO.getDatasetId())
                .setCreateUserId(contextService.getCurUserId())
                .setLabels("")
                .setMergeColumn(StringUtils.join(datasetCsvImportDTO.getMergeColumn(), ','))
                .setFiles(Strings.join(Collections.singletonList(file.getId()), ','))
                .setType(DataTaskTypeEnum.CSV_IMPORT.getValue());
        taskService.createTask(task);
        //创建入参请求体
        StateChangeDTO stateChangeDTO = new StateChangeDTO();
        //更新数据集状态为导入中
        stateChangeDTO.setObjectParam(new Object[]{datasetCsvImportDTO.getDatasetId().intValue()});
        //添加需要执行的状态机类
        stateChangeDTO.setStateMachineType(DataStateMachineConstant.DATA_STATE_MACHINE);
        //采样失败事件
        stateChangeDTO.setEventMethodName(DataStateMachineConstant.TABLE_IMPORT_EVENT);
        StateMachineUtil.stateChange(stateChangeDTO);
    }

    /**
     * 获取文件列表
     *
     * @param datasetId 数据集ID
     * @param prefix    匹配前缀
     * @param recursive 是否递归
     * @return List<FileListDTO> 文件列表
     */
    @Override
    public List<FileDTO> fileList(Long datasetId, String prefix, boolean recursive, String versionName, boolean isVersionFile) {
        /**
         * if(prefix == 空) {
         *     if(isVersionFile) {
         *         获取版本目录下文件数据
         *     } else {
         *         获取数据集当前版本目录下数据
         *     }
         * } else {
         *     调用minio接口查询
         * }
         *
         */
        if (StringUtils.isEmpty(prefix)) {
            if (isVersionFile) {
                if (StringUtils.isEmpty(versionName)) {
                    versionName = datasetService.getOneById(datasetId).getCurrentVersionName();
                }
                prefix = "dataset/" + datasetId + "/versionFile/" + versionName + "/";
            } else {
                prefix = datasetService.getOneById(datasetId).getUri() + "/";
            }
        }
        return minioUtil.fileList(bucketName, prefix, recursive);
    }

    /**
     * 分页获取文件列表
     *
     * @param filePageDTO 文件查询和响应实体
     */
    @Override
    public void filePage(FilePageDTO filePageDTO, Long datasetId) {
        Dataset dataset = datasetService.getOneById(datasetId);
        filePageDTO.setFilePath(prefixPath + bucketName + "/" + dataset.getUri() + filePageDTO.getFilePath());
        fileStoreApi.filterFilePageWithPath(filePageDTO);
        if (!CollectionUtils.isEmpty(filePageDTO.getRows())) {
            for (FileDTO fileDto : filePageDTO.getRows()) {
                fileDto.setPath(fileDto.getPath().replaceFirst(filePageDTO.getFilePath(), ""));
            }
        }
    }

    @Override
    public List<FileAnnotationBO> listByDatasetIdAndVersionName(Long datasetId, String versionName) {
        return baseMapper.listByDatasetIdAndVersionName(datasetId, versionName);
    }

    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public IPage<ImageFileVO> listImages(Long datasetId, Page page, FileQueryCriteriaVO queryCriteria) {
        Dataset dataset = datasetService.getOneById(datasetId);
        Page<DatasetVersionFileDTO> pageData = new Page<>(page.getCurrent(), page.getSize());

        IPage<DatasetVersionFileDTO> dtoPage = imageFileDTOMapper.selectDatasetVersionFilesByConditions(pageData, datasetId, dataset.getCurrentVersionName(), queryCriteria.getStatus()[0], queryCriteria.getName(), queryCriteria.getOrder());


        // 创建VO分页对象，但不包括记录（records），因为我们需要手动转换
        IPage<ImageFileVO> voPage = new Page<>();
        BeanUtils.copyProperties(dtoPage, voPage, "records");

        // 手动将DTO对象列表转换为VO对象列表
        List<ImageFileVO> voList = dtoPage.getRecords().stream().map(dto -> {
            ImageFileVO vo = new ImageFileVO();
            BeanUtils.copyProperties(dto, vo);

            try {
                // 从URL中解析出bucketName和objectName
                String url = vo.getUrl();
                if (url == null || url.isEmpty()) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "图片URL为空，文件ID: {}", dto.getFileId());
                    // 设置默认值
                    vo.setFileType("unknown");
                    vo.setFileSize("0.00MB");
                } else {
                    String[] parts = url.split("/", 3);
                    if (parts.length < 3) {
                        LogUtil.warn(LogEnum.BIZ_DATASET, "图片URL格式不正确: {}, 文件ID: {}", url, dto.getFileId());
                        // 设置默认值
                        int dotIndex = url.lastIndexOf('.');
                        vo.setFileType(dotIndex > 0 ? url.substring(dotIndex + 1) : "unknown");
                        vo.setFileSize("0.00MB");
                    } else {
                        String bucketName = parts[0];
                        String objectName = parts[1] + "/" + parts[2];

                        // 调用接口查询额外的属性
                        Map<String, Object> fileDetails = minioUtil.getFileDetails(bucketName, objectName);

                        if (fileDetails != null && fileDetails.containsKey("size") && fileDetails.containsKey("type")) {
                            // 格式化文件大小，转换为MB并保留两位小数
                            long fileSizeInBytes = Long.parseLong(fileDetails.get("size").toString());
                            double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
                            String formattedFileSize = String.format("%.2fMB", fileSizeInMB);
                            
                            // 格式化文件类型，去掉前面的"image/"
                            String fileType = fileDetails.get("type").toString();
                            String formattedFileType = fileType.replace("image/", "");

                            // 设置额外的属性
                            vo.setFileType(formattedFileType);
                            vo.setFileSize(formattedFileSize);
                        } else {
                            LogUtil.warn(LogEnum.BIZ_DATASET, "MinIO文件详情为空或不完整: {}, 文件ID: {}", url, dto.getFileId());
                            int dotIndex = url.lastIndexOf('.');
                            vo.setFileType(dotIndex > 0 ? url.substring(dotIndex + 1) : "unknown");
                            vo.setFileSize("0.00MB");
                        }
                    }
                }
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "获取图片文件详情失败，文件ID: {}, URL: {}, 错误: {}", 
                    dto.getFileId(), vo.getUrl(), e.getMessage());
                // 设置默认值，避免整个列表查询失败
                String url = vo.getUrl();
                if (url != null && url.contains(".")) {
                    int dotIndex = url.lastIndexOf('.');
                    vo.setFileType(dotIndex > 0 ? url.substring(dotIndex + 1) : "unknown");
                } else {
                    vo.setFileType("unknown");
                }
                vo.setFileSize("0.00MB");
            }

            // 设置文件名称和状态
            vo.setName(dto.getFileName());
            vo.setStatus(dto.getAnnotationStatus());

            return vo;
        }).collect(Collectors.toList());

        // 将转换后的VO列表设置到VO分页对象中
        voPage.setRecords(voList);

        // 返回VO分页对象
        return voPage;
    }


    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Long getImagesSize(Long datasetId, String versionName) {
        Dataset dataset = datasetService.getOneById(datasetId);
        List<DatasetVersionFileDTO> datasetVersionFileDTOS = imageFileDTOMapper.selectAllDatasetVersionFiles(datasetId, versionName);

        Map<String, String> bucketAndObjectNames = new HashMap<>();
        for (DatasetVersionFileDTO dto : datasetVersionFileDTOS) {
            String[] parts = dto.getUrl().split("/", 3);
            String bucketName = parts[0];
            String objectName = parts[1] + "/" + parts[2];
            bucketAndObjectNames.put(objectName, bucketName);
        }

        Map<String, Long> fileSizes = minioUtil.getFilesSizes(bucketAndObjectNames);

        // Accumulate the total size
        long totalSize = fileSizes.values().stream().mapToLong(Long::longValue).sum();

        // Format the total size with two decimal places
        return totalSize;
    }


    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Long getVideosSize(Long datasetId) {
        List<VideoFileDTO> videoFileDTOS = videoFileDTOMapper.selectAllVideosByDatasetId(datasetId);

        Map<String, String> bucketAndObjectNames = new HashMap<>();
        for (VideoFileDTO dto : videoFileDTOS) {
            String[] parts = dto.getUrl().split("/", 3);
            String bucketName = parts[0];
            String objectName = parts[1] + "/" + parts[2];
            bucketAndObjectNames.put(objectName, bucketName);
        }

        Map<String, Long> fileSizes = minioUtil.getFilesSizes(bucketAndObjectNames);

        // Accumulate the total size
        long totalSize = fileSizes.values().stream().mapToLong(Long::longValue).sum();

        // Format the total size with two decimal places
        return totalSize;
    }

    @Override
    public String getFileAnnotation(Long fileId, Long datasetId, String labelType) {
        // 获取图片文件信息
        FileAnnotationBO imageFile = getImageFile(fileId, datasetId);
        String annotations = null;
        if (imageFile != null) {
            annotations = imageFile.getAnnotationUrl();
        }
        // 获取数据集标签信息
        List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(datasetId);
        Map<Long, String> labelMaps = datasetLabels.stream().collect(Collectors.toMap(Label::getId, Label::getName));
        if (imageFile != null) {
            // VOC格式
            if ("VOC".equals(labelType)) {
                return ConversionUtil.buildVOCAnnotation(annotations, labelMaps, imageFile);
            } else if ("YOLO".equals(labelType)) {
                return ConversionUtil.buildYoloAnnotationLabelFile(annotations, datasetLabels, imageFile);
            }
        }
        return "";
    }

    @Override
    public String getYoloLabelsFile(Long datasetId) {
        // 获取数据集标签信息
        List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(datasetId);
        return ConversionUtil.buildYOLOLabelsString(datasetLabels);
    }


    @Override
    public IPage<VideoFileVO> listVideos(Long datasetId, Integer page, Integer pageSize, Integer status, String name, List<Long> createTime, String order) {
        Page<ImageFileDTO> pageData = new Page<>(page, pageSize);

        // 检查数据集名称字段是否存在
        if (name != null && name.isEmpty()) {
            name = null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String createTimeStart = null;
        String createTimeEnd = null;
        if (createTime != null && createTime.size() == 2) {
            createTimeStart = sdf.format(new Date(createTime.get(0)));
            createTimeEnd = sdf.format(new Date(createTime.get(1)));
        }

        IPage<VideoFileDTO> dtoPage = videoFileDTOMapper.selectVideosByDatasetId(pageData, datasetId, status, name, createTimeStart, createTimeEnd, order);

        // 创建一个新的IPage对象，用于存放VO对象
        IPage<VideoFileVO> voPage = new Page<>();
        BeanUtils.copyProperties(dtoPage, voPage);

        // 将DTO对象转换为VO对象
        List<VideoFileVO> voList = dtoPage.getRecords().stream().map(dto -> {
            VideoFileVO vo = new VideoFileVO();
            BeanUtils.copyProperties(dto, vo);

            // 从URL中解析出bucketName和objectName
            String[] parts = vo.getUrl().split("/", 3);
            String bucketName = parts[0];
            String objectName = parts[1] + "/" + parts[2];

            // 调用接口查询额外的属性
            Map<String, Object> fileDetails = minioUtil.getFileDetails(bucketName, objectName);

            // 格式化文件大小，转换为MB并保留两位小数
            long fileSizeInBytes = Long.parseLong(fileDetails.get("size").toString());
            double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
            String formattedFileSize = String.format("%.2fMB", fileSizeInMB);

            // 格式化文件类型，去掉前面的"video/"
            String fileType = fileDetails.get("type").toString();
            String formattedFileType = fileType.replace("video/", "");

            // 设置额外的属性
            vo.setFileSize(formattedFileSize);
            vo.setFileType(dto.getUrl().substring(dto.getUrl().lastIndexOf('.') + 1));

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }


    @Override
    public VideoStatisticsDTO getVideoStatistics(Long datasetId) {
        return videoFileDTOMapper.getVideoStatistics(datasetId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveVideoInfo(MP4RecordDTO mp4RecordDTO) {

        String videoPath = mp4RecordDTO.getFilePath();
        // 路径格式 /opt/media/bin/www/record/21/video/live/obs/2023-12-17/15-53-02.mp4
        String[] parts = videoPath.split("/");
        // 提取数据集id
        Long datasetId = -1L;
        for (String part : parts) {
            if (part.matches("\\d+")) {
                datasetId = Long.parseLong(part);
                break;
            }
        }
        // 提取视频名称
        String videoName = parts[(parts.length - 1)];
        // 检查数据集是否存在
        if (datasetMapper.selectById(datasetId) == null) {
            throw new BusinessException(ErrorEnum.DATA_ABSENT_OR_NO_AUTH, "id:" + datasetId, null);
        }
        // 存库
        List<FileCreateDTO> videoFiles = new ArrayList<>();
        // url拼接 dubhe-prod/dataset/ + 21/video/live/obs/2023-12-17/15-53-02.mp4
        FileCreateDTO videoFile = FileCreateDTO.builder()
                .url(zlmUrlPrefix.concat(videoPath.split("/", 7)[6]))
                .frameInterval(1)
                .pid(MagicNumConstant.ONE_LONG)
                .status(null)
                .enhanceType(null)
                .createUserId(null)
                .width(null)
                .height(null)
                .name(videoName)
                .content(null)
                .build();
        videoFiles.add(videoFile);
        saveVideoFiles(datasetId, videoFiles, DatatypeEnum.VIDEO.getValue(), PID_OF_VIDEO, null);
    }

    @Override
    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    public Page<JSONObject> listByLimitWithAnnotation(Long datasetId, String versionName, Long offset, Integer limit, Integer page, Integer[] type, Long[] labelId, String labelType) {
        if (page == null) {
            page = MagicNumConstant.ONE;
        }
        if (offset == null) {
            offset = getDefaultOffset();
        }
        if (limit == null) {
            limit = defaultFilePageSize;
        }

        //查询数据集
        Dataset dataset = datasetService.getOneById(datasetId);
        //查询当前数据集下所有的文件(中间表)
        List<DatasetVersionFileDTO> datasetVersionFiles = datasetVersionFileService
                .getListByDatasetIdAndAnnotationStatus(dataset.getId(), StringUtils.isBlank(versionName) ? dataset.getCurrentVersionName() : versionName, type, offset,
                        limit, "id", null, labelId);


        if (datasetVersionFiles == null || datasetVersionFiles.isEmpty()) {
            Page<JSONObject> filePage = new Page<>();
            filePage.setCurrent(page);
            filePage.setSize(limit);
            filePage.setTotal(NumberConstant.NUMBER_0);
            return filePage;
        }

        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", datasetVersionFiles
                .stream()
                .map(DatasetVersionFileDTO::getFileId)
                .collect(Collectors.toSet())).eq("dataset_id", dataset.getId());
        List<File> files = baseMapper.selectList(queryWrapper);


        // 构建DatasetVersionFileDTO的Map，方便后续查找
        Map<Long, DatasetVersionFileDTO> versionFileMap = datasetVersionFiles.stream()
                .collect(Collectors.toMap(DatasetVersionFileDTO::getFileId, Function.identity()));

        //将所有文件的状态放入
        files.forEach(v -> {
            DatasetVersionFileDTO versionFile = versionFileMap.get(v.getId());
            if (versionFile != null) {
                v.setStatus(versionFile.getAnnotationStatus());
            }
        });

        // 批量获取标签信息（只查询一次）
        List<Label> datasetLabels = datasetLabelService.listLabelByDatasetId(datasetId);
        Map<Long, String> labelIdToNameMap = datasetLabels.stream()
                .collect(Collectors.toMap(Label::getId, Label::getName));
        Map<String, Long> labelNameToIdMap = datasetLabels.stream()
                .collect(Collectors.toMap(Label::getName, Label::getId));

        //文件重排序（按照版本文件排序）并添加annotation
        List<JSONObject> fileArrayList = new ArrayList<>();
        datasetVersionFiles.forEach(versionFileDTO -> {
            files.forEach(file -> {
                if (versionFileDTO.getFileId().equals(file.getId())) {

                    file.setName(FileUtil.interceptFileNameAndDatasetId(datasetId, file.getName()));

                    // 将File转换为JSONObject
                    JSONObject fileJson = (JSONObject) JSON.toJSON(file);

                    // 生成annotation
                    String annotation = generateAnnotation(file, dataset, versionFileDTO, labelType, datasetLabels, labelIdToNameMap, labelNameToIdMap);

                    // 添加annotation字段
                    fileJson.put("annotation", annotation);

                    fileArrayList.add(fileJson);
                }
            });
        });

        Page<JSONObject> pages = new Page<>();
        if (!ArrayUtils.isEmpty(labelId)) {
            pages.setTotal(dataFileAnnotationService.selectDetectionCount(datasetId, dataset.getCurrentVersionName(), labelId));
        } else {
            pages.setTotal(datasetVersionFileService.selectFileListTotalCount(dataset.getId(),
                    dataset.getCurrentVersionName(), type, labelId));
        }
        pages.setRecords(fileArrayList);
        pages.setSize(limit);
        pages.setCurrent(page);
        return pages;
    }
    @Override
    public Integer getVersionImgCount(Long datasetId, String versionName) {
        return fileMapper.getVersionImgCount(datasetId, versionName);
    }

    /**
     * 生成单个文件的annotation
     */
    private String generateAnnotation(File file, Dataset dataset, DatasetVersionFileDTO versionFileDTO,
                                      String labelType, List<Label> datasetLabels,
                                      Map<Long, String> labelIdToNameMap, Map<String, Long> labelNameToIdMap) {

        // 获取annotation URL
        String annotationUrl = getAnnotation(file.getDatasetId(),
                FileUtil.interceptFileNameAndDatasetId(dataset.getId(), file.getName()),
                versionFileDTO.getVersionName(),
                versionFileDTO.getChanged() == NumberConstant.NUMBER_0);

        if (StringUtils.isEmpty(annotationUrl)) {
            return "";
        }

        // 构建FileAnnotationBO
        FileAnnotationBO fileAnnotationBO = FileAnnotationBO.builder()
                .fileId(file.getId())
                .fileName(file.getName())
                .fileHeight(file.getHeight())
                .fileWidth(file.getWidth())
                .fileUrl(file.getUrl())
                .annotationUrl(annotationUrl)
                .build();

        // 处理标签名称到ID的转换（未改变的情况）
        if (versionFileDTO.getChanged() == NumberConstant.NUMBER_0) {
            String annotation = fileAnnotationBO.getAnnotationUrl();
            if (StringUtils.isNotEmpty(annotation)) {
                JSONArray jsonArray = JSON.parseArray(annotation);
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String categoryIdStr = jsonObject.getString("category_id");
                    if (!NumberUtil.isNumber(categoryIdStr)) {
                        if (labelNameToIdMap.containsKey(categoryIdStr)) {
                            jsonObject.put("category_id", labelNameToIdMap.get(categoryIdStr));
                        }
                    }
                }
                fileAnnotationBO.setAnnotationUrl(JSON.toJSONString(jsonArray));
            }
        }

        // 根据labelType生成对应格式的annotation
        if ("VOC".equals(labelType)) {
            return ConversionUtil.buildVOCAnnotation(fileAnnotationBO.getAnnotationUrl(), labelIdToNameMap, fileAnnotationBO);
        } else if ("YOLO".equals(labelType)) {
            return ConversionUtil.buildYoloAnnotationLabelFile(fileAnnotationBO.getAnnotationUrl(), datasetLabels, fileAnnotationBO);
        }

        // 如果不是VOC或YOLO格式，返回原始JSON
        return fileAnnotationBO.getAnnotationUrl();
    }

    @Override
    public Map<String, Long> getFileIdsByUrls(Long datasetId, List<String> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return new HashMap<>();
        }
        List<FileUrlIdMapping> mappings = this.baseMapper.getFileIdsByUrls(datasetId, urls);
        return mappings.stream().collect(Collectors.toMap(
                FileUrlIdMapping::getUrl,
                FileUrlIdMapping::getId,
                (existing, replacement) -> existing
        ));
    }

    @Override
    public VideoInfoVO getVideoInfo(Long datasetId, Long fileId) {
        // 1. 查询文件信息
        File file = selectById(fileId, datasetId);
        if (file == null) {
            throw new BusinessException(ErrorEnum.FILE_ABSENT, null);
        }
        
        // 2. 检查是否为视频文件
        if (!Objects.equals(file.getFileType(), MagicNumConstant.ONE)) {
            throw new BusinessException(ErrorEnum.PARAM_ERROR, "文件类型不是视频", null);
        }
        
        // 3. 返回默认值（前端通过HTML5 Video元素获取实际分辨率）
        // 帧率硬编码为25fps， 分辨率由前端获取
        return VideoInfoVO.builder()
                .width(1920)  // 默认宽度
                .height(1080) // 默认高度
                .frameRate(25.0) // 硬编码帧率
                .duration(0)  // 时长由前端获取
                .build();
    }
}