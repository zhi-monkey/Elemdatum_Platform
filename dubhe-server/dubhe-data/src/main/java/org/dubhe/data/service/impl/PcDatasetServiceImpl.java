package org.dubhe.data.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.dao.PcDatasetFileMapper;
import org.dubhe.data.dao.PcDatasetDatasetGroupMapper;
import org.dubhe.data.dao.PcDatasetMapper;
import org.dubhe.data.dao.DatasetGroupMapper;
import org.dubhe.data.dao.DatasetMapper;
import org.dubhe.data.domain.dto.DatasetLabelInfoDTO;
import org.dubhe.data.domain.dto.PcDatasetAnnotationDTO;
import org.dubhe.data.domain.dto.PcDatasetCreateDTO;
import org.dubhe.data.domain.dto.PcDatasetFileCommitDTO;
import org.dubhe.data.domain.dto.PcDatasetUploadUrlDTO;
import org.dubhe.data.domain.entity.PcDataset;
import org.dubhe.data.domain.entity.PcDatasetDatasetGroup;
import org.dubhe.data.domain.entity.PcDatasetFile;
import org.dubhe.data.domain.vo.PcDatasetDetailVO;
import org.dubhe.data.domain.vo.PcDatasetFileVO;
import org.dubhe.data.domain.vo.PcDatasetUploadUrlVO;
import org.dubhe.data.service.PcDatasetService;
import org.dubhe.data.service.PcDatasetUploadStatusService;
import org.dubhe.data.service.pcd.PcdAsciiParser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PcDatasetServiceImpl implements PcDatasetService {
    private static final String FILE_TYPE = "pcd";
    private static final String UPLOAD_CREATING = "CREATING";
    private static final String UPLOAD_UPLOADING = "UPLOADING";
    private static final String UPLOAD_READY = "READY";

    private final PcDatasetMapper datasetMapper;
    private final PcDatasetFileMapper fileMapper;
    private final PcDatasetDatasetGroupMapper pcDatasetGroupMapper;
    private final DatasetGroupMapper groupMapper;
    private final DatasetMapper datasetMapperForLabel;
    private final MinioUtil minioUtil;
    private final UserContextService userContextService;
    private final PcDatasetUploadStatusService uploadStatusService;

    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.presignedUrlExpiryTime:604800}")
    private Integer presignedUrlExpiry;

    public PcDatasetServiceImpl(PcDatasetMapper datasetMapper,
                                PcDatasetFileMapper fileMapper,
                                PcDatasetDatasetGroupMapper pcDatasetGroupMapper,
                                DatasetGroupMapper groupMapper,
                                DatasetMapper datasetMapperForLabel,
                                MinioUtil minioUtil,
                                UserContextService userContextService,
                                PcDatasetUploadStatusService uploadStatusService) {
        this.datasetMapper = datasetMapper;
        this.fileMapper = fileMapper;
        this.pcDatasetGroupMapper = pcDatasetGroupMapper;
        this.groupMapper = groupMapper;
        this.datasetMapperForLabel = datasetMapperForLabel;
        this.minioUtil = minioUtil;
        this.userContextService = userContextService;
        this.uploadStatusService = uploadStatusService;
    }

    @Override
    @Transactional
    public PcDataset create(PcDatasetCreateDTO dto) {
        if (groupMapper.selectById(dto.getDatasetGroupId()) == null) {
            throw new BusinessException("Point-cloud dataset group does not exist");
        }
        PcDataset dataset = new PcDataset()
                .setName(dto.getName().trim())
                .setLabelGroupId(dto.getLabelGroupId())
                .setFileCount(0L)
                .setStatus(1001)
                .setUploadStatus(UPLOAD_CREATING)
                .setRemark(dto.getRemark());
        dataset.setDeleted(false);
        dataset.setCreateUserId(userContextService.getCurUserId());
        datasetMapper.insert(dataset);
        pcDatasetGroupMapper.insert(new PcDatasetDatasetGroup(dto.getDatasetGroupId(), dataset.getId()));
        dataset.setStoragePrefix("dataset/pc/" + dataset.getId() + "/origin");
        datasetMapper.updateById(dataset);
        return dataset;
    }

    @Override
    public PcDatasetUploadUrlVO createUploadUrl(Long datasetId, PcDatasetUploadUrlDTO dto) {
        PcDataset dataset = requireDataset(datasetId);
        String name = cleanFileName(dto.getName());
        validatePcdName(name);
        String prefix = ensurePrefix(dataset);
        String objectKey = prefix + "/" + UUID.randomUUID().toString().replace("-", "") + "-" + name;
        String uploadUrl = minioUtil.getEncryptedPutUrl(bucketName, objectKey, presignedUrlExpiry);
        if (!UPLOAD_UPLOADING.equals(dataset.getUploadStatus())) {
            datasetMapper.markUploading(datasetId);
        }
        return new PcDatasetUploadUrlVO(datasetId, name, bucketName, objectKey, uploadUrl);
    }

    @Override
    @Transactional
    public PcDatasetFileVO commitFile(Long datasetId, PcDatasetFileCommitDTO dto) {
        PcDataset dataset = requireDataset(datasetId);
        String name = cleanFileName(dto.getName());
        validatePcdName(name);
        String prefix = ensurePrefix(dataset);
        String objectKey = dto.getObjectKey();
        if (!objectKey.startsWith(prefix + "/")) {
            throw new BusinessException("Object does not belong to this dataset");
        }
        if (fileMapper.selectCount(new QueryWrapper<PcDatasetFile>()
                .eq("dataset_id", datasetId).eq("object_key", objectKey)) > 0) {
            throw new BusinessException("PCD file has already been committed");
        }
        Map<String, Object> details = minioUtil.getFileDetails(bucketName, objectKey);
        if (details == null) {
            markUploadFailed(datasetId, "Uploaded object was not found in MinIO");
            throw new BusinessException("Uploaded object was not found in MinIO");
        }

        PcdAsciiParser.ParsedPcd parsed;
        try (InputStream input = minioUtil.getObjectInputStream(bucketName, objectKey)) {
            parsed = PcdAsciiParser.parse(input);
        } catch (BusinessException ex) {
            deleteInvalidObject(objectKey);
            markUploadFailed(datasetId, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            deleteInvalidObject(objectKey);
            markUploadFailed(datasetId, ex.getMessage());
            throw new BusinessException("Unable to read PCD object", ex);
        }

        PcDatasetFile file = new PcDatasetFile()
                .setName(name)
                .setFileType(FILE_TYPE)
                .setDatasetId(datasetId)
                .setUrl(objectKey)
                .setObjectKey(objectKey)
                .setStorageBucket(bucketName)
                .setFileSize((Long) details.get("size"))
                .setContentType((String) details.get("type"))
                .setEtag((String) details.get("etag"))
                .setUploadStatus(UPLOAD_READY)
                .setDataEncoding("ascii")
                .setPointCount(parsed.getPointCount())
                .setPcdMetadata(parsed.getMetadata());
        file.setDeleted(false);
        file.setCreateUserId(userContextService.getCurUserId());
        try {
            fileMapper.insert(file);
        } catch (RuntimeException ex) {
            markUploadFailed(datasetId, ex.getMessage());
            throw ex;
        }

        if (datasetMapper.incrementFileCountAfterUpload(datasetId) != 1) {
            throw new BusinessException("Unable to update point-cloud dataset status");
        }
        return toVO(file);
    }

    private PcDataset requireDataset(Long datasetId) {
        PcDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new BusinessException("Point-cloud dataset does not exist");
        }
        return dataset;
    }

    private void markUploadFailed(Long datasetId, String error) {
        uploadStatusService.markFailed(datasetId, error);
    }

    private void deleteInvalidObject(String objectKey) {
        try {
            minioUtil.del(bucketName, objectKey);
        } catch (Exception ignored) {
            // The validation error remains the user-visible failure; object cleanup is best effort.
        }
    }

    private String ensurePrefix(PcDataset dataset) {
        if (dataset.getStoragePrefix() != null && !dataset.getStoragePrefix().trim().isEmpty()) {
            return dataset.getStoragePrefix().replaceAll("/+", "/").replaceAll("/$", "");
        }
        String prefix = "dataset/pc/" + dataset.getId() + "/origin";
        datasetMapper.updateById(new PcDataset().setId(dataset.getId()).setStoragePrefix(prefix));
        return prefix;
    }

    private static String cleanFileName(String name) {
        String clean = name == null ? "" : name.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        return (slash >= 0 ? clean.substring(slash + 1) : clean).trim();
    }

    private static void validatePcdName(String name) {
        if (name.isEmpty() || !name.toLowerCase(Locale.ROOT).endsWith(".pcd")) {
            throw new BusinessException("Only .pcd files are supported");
        }
    }

    private static PcDatasetFileVO toVO(PcDatasetFile file) {
        PcDatasetFileVO vo = new PcDatasetFileVO();
        vo.setId(file.getId());
        vo.setDatasetId(file.getDatasetId());
        vo.setName(file.getName());
        vo.setFileType(file.getFileType());
        vo.setObjectKey(file.getObjectKey());
        vo.setStorageBucket(file.getStorageBucket());
        vo.setFileSize(file.getFileSize());
        vo.setContentType(file.getContentType());
        vo.setEtag(file.getEtag());
        vo.setUploadStatus(file.getUploadStatus());
        vo.setDataEncoding(file.getDataEncoding());
        vo.setPointCount(file.getPointCount());
        vo.setPcdMetadata(file.getPcdMetadata());
        vo.setCreateTime(file.getCreateTime());
        return vo;
    }

    @Override
    public PcDatasetDetailVO detail(Long datasetId) {
        PcDataset dataset = requireDataset(datasetId);
        PcDatasetDetailVO vo = new PcDatasetDetailVO();
        BeanUtils.copyProperties(dataset, vo);

        // 查询该数据集下所有未删除的 PCD 文件
        List<PcDatasetFile> files = fileMapper.selectList(new QueryWrapper<PcDatasetFile>()
                .eq("dataset_id", datasetId)
                .eq("deleted", 0));
        // 统计总点数、总大小（默认 0，避免前端拿到 null）
        long totalPointCount = files.stream()
                .filter(f -> f.getPointCount() != null)
                .mapToLong(PcDatasetFile::getPointCount).sum();
        long totalFileSize = files.stream()
                .filter(f -> f.getFileSize() != null)
                .mapToLong(PcDatasetFile::getFileSize).sum();
        vo.setTotalPointCount(totalPointCount);
        vo.setTotalFileSize(totalFileSize);
        return vo;
    }

    @Override
    public IPage<PcDatasetFileVO> listFiles(Long datasetId, Page<PcDatasetFileVO> page) {
        requireDataset(datasetId);
        // 分页参数必须与实体类型一致，这里用实体 Page 接收查询结果
        Page<PcDatasetFile> filePage = new Page<>(page.getCurrent(), page.getSize());
        fileMapper.selectPage(filePage, new QueryWrapper<PcDatasetFile>()
                .eq("dataset_id", datasetId)
                .eq("deleted", 0)
                .orderByDesc("create_time"));
        // 将实体分页转为 VO 分页（VO 与实体字段基本一致，保留扩展字段）
        List<PcDatasetFileVO> voList = new ArrayList<>();
        for (PcDatasetFile file : filePage.getRecords()) {
            voList.add(toVO(file));
        }
        Page<PcDatasetFileVO> voPage = new Page<>(filePage.getCurrent(), filePage.getSize(), filePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public void deleteFile(Long datasetId, Long fileId) {
        requireDataset(datasetId);
        PcDatasetFile file = fileMapper.selectOne(new QueryWrapper<PcDatasetFile>()
                .eq("id", fileId)
                .eq("dataset_id", datasetId)
                .eq("deleted", 0));
        if (file == null) {
            throw new BusinessException("PCD file does not exist");
        }
        // 软删除（逻辑删除）单个 PCD 文件
        // 说明：不走 MyBatis-Plus 的 @TableLogic + setDeleted 链（旧版本存在类型推断暗病），
        // 改用 Mapper 里显式 SQL 的 logicDeleteById，逻辑一致且更稳定。
        fileMapper.logicDeleteById(file.getId());
        // 文件数减一
        datasetMapper.decrementFileCountAfterDelete(datasetId);
        // 尽力删除 MinIO 对象（失败不阻塞主流程）
        try {
            minioUtil.del(bucketName, file.getObjectKey());
        } catch (Exception ignored) {
            // 对象删除失败仅记录，不影响数据删除结果
        }
    }

    @Override
    public List<DatasetLabelInfoDTO> getLabels(Long datasetId) {
        requireDataset(datasetId);
        // 点云数据集与图片数据集共用 data_dataset_label + data_label 标签关联表
        return datasetMapperForLabel.selectLabelInfoByDatasetId(datasetId);
    }

    @Override
    @Transactional
    public PcDatasetFileVO saveAnnotations(Long datasetId, Long fileId, PcDatasetAnnotationDTO dto) {
        PcDatasetFile file = fileMapper.selectOne(new QueryWrapper<PcDatasetFile>()
                .eq("id", fileId)
                .eq("dataset_id", datasetId)
                .eq("deleted", 0));
        if (file == null) {
            throw new BusinessException("PCD file does not exist");
        }

        JSONObject metadata = new JSONObject();
        if (file.getPcdMetadata() != null && !file.getPcdMetadata().trim().isEmpty()) {
            try {
                metadata = JSONObject.parseObject(file.getPcdMetadata());
            } catch (Exception ignored) {
                metadata = new JSONObject();
            }
        }
        metadata.put("annotations", dto.getBoxes());
        metadata.put("annotationUpdatedAt", System.currentTimeMillis());

        PcDatasetFile update = new PcDatasetFile();
        update.setId(fileId);
        update.setPcdMetadata(metadata.toJSONString());
        fileMapper.updateById(update);

        return toVO(fileMapper.selectById(fileId));
    }

    @Override
    public String getDownloadUrl(Long datasetId, Long fileId) {
        requireDataset(datasetId);
        PcDatasetFile file = fileMapper.selectOne(new QueryWrapper<PcDatasetFile>()
                .eq("id", fileId)
                .eq("dataset_id", datasetId)
                .eq("deleted", 0));
        if (file == null) {
            throw new BusinessException("PCD file does not exist");
        }
        if (StringUtils.isEmpty(file.getObjectKey())) {
            throw new BusinessException("PCD file object key is empty");
        }
        String downloadBucket = StringUtils.isEmpty(file.getStorageBucket()) ? bucketName : file.getStorageBucket();
        return minioUtil.getEncryptedGetUrl(downloadBucket, file.getObjectKey(), presignedUrlExpiry);
    }

    @Override
    @Transactional
    public void deleteDataset(Long datasetId) {
        PcDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new BusinessException("点云数据集不存在");
        }

        // 软删除数据集
        PcDataset update = new PcDataset();
        update.setId(datasetId);
        update.setDeleted(true);
        update.setUpdateUserId(userContextService.getCurUserId());
        datasetMapper.updateById(update);

        // 删除数据集组关联关系
        pcDatasetGroupMapper.delete(new QueryWrapper<PcDatasetDatasetGroup>()
                .eq("pc_dataset_id", datasetId));

        // 软删除所有关联的文件
        PcDatasetFile fileUpdate = new PcDatasetFile();
        fileUpdate.setDeleted(true);
        fileUpdate.setUpdateUserId(userContextService.getCurUserId());
        fileMapper.update(fileUpdate, new QueryWrapper<PcDatasetFile>()
                .eq("dataset_id", datasetId)
                .eq("deleted", 0));
    }
}
