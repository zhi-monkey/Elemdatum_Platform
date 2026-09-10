package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.base.exception.BusinessException;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.dao.DatasetGroupMapper;
import org.dubhe.data.dao.VideoDatasetDatasetGroupMapper;
import org.dubhe.data.dao.VideoDatasetFileMapper;
import org.dubhe.data.dao.VideoDatasetMapper;
import org.dubhe.data.domain.dto.VideoDatasetCreateDTO;
import org.dubhe.data.domain.dto.VideoDatasetFileCommitDTO;
import org.dubhe.data.domain.dto.VideoDatasetUploadUrlDTO;
import org.dubhe.data.domain.entity.VideoDataset;
import org.dubhe.data.domain.entity.VideoDatasetDatasetGroup;
import org.dubhe.data.domain.entity.VideoDatasetFile;
import org.dubhe.data.domain.vo.VideoDatasetDetailVO;
import org.dubhe.data.domain.vo.VideoDatasetFileVO;
import org.dubhe.data.domain.vo.VideoDatasetUploadUrlVO;
import org.dubhe.data.service.VideoDatasetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class VideoDatasetServiceImpl implements VideoDatasetService {
    private static final String FILE_TYPE = "video";
    private static final String UPLOAD_CREATING = "CREATING";
    private static final String UPLOAD_UPLOADING = "UPLOADING";
    private static final String UPLOAD_READY = "READY";
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            "mp4", "avi", "mov", "mkv", "flv", "wmv", "webm", "m4v"));

    private final VideoDatasetMapper datasetMapper;
    private final VideoDatasetFileMapper fileMapper;
    private final VideoDatasetDatasetGroupMapper datasetGroupRelationMapper;
    private final DatasetGroupMapper groupMapper;
    private final MinioUtil minioUtil;
    private final UserContextService userContextService;
    private final VideoMediaProbeService videoMediaProbeService;

    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.presignedUrlExpiryTime:604800}")
    private Integer presignedUrlExpiry;

    public VideoDatasetServiceImpl(VideoDatasetMapper datasetMapper,
                                   VideoDatasetFileMapper fileMapper,
                                   VideoDatasetDatasetGroupMapper datasetGroupRelationMapper,
                                   DatasetGroupMapper groupMapper,
                                   MinioUtil minioUtil,
                                   UserContextService userContextService,
                                   VideoMediaProbeService videoMediaProbeService) {
        this.datasetMapper = datasetMapper;
        this.fileMapper = fileMapper;
        this.datasetGroupRelationMapper = datasetGroupRelationMapper;
        this.groupMapper = groupMapper;
        this.minioUtil = minioUtil;
        this.userContextService = userContextService;
        this.videoMediaProbeService = videoMediaProbeService;
    }

    @Override
    @Transactional
    public VideoDataset create(VideoDatasetCreateDTO dto) {
        if (groupMapper.selectById(dto.getDatasetGroupId()) == null) {
            throw new BusinessException("Video dataset group does not exist");
        }
        VideoDataset dataset = new VideoDataset()
                .setName(dto.getName().trim())
                .setLabelGroupId(dto.getLabelGroupId())
                .setFileCount(0L)
                .setStatus(2001)
                .setUploadStatus(UPLOAD_CREATING)
                .setRemark(dto.getRemark());
        dataset.setDeleted(false);
        dataset.setCreateUserId(userContextService.getCurUserId());
        datasetMapper.insert(dataset);
        datasetGroupRelationMapper.insert(new VideoDatasetDatasetGroup(dto.getDatasetGroupId(), dataset.getId()));
        dataset.setStoragePrefix("dataset/video/" + dataset.getId() + "/origin");
        datasetMapper.updateById(dataset);
        return dataset;
    }

    @Override
    public VideoDatasetUploadUrlVO createUploadUrl(Long datasetId, VideoDatasetUploadUrlDTO dto) {
        VideoDataset dataset = requireDataset(datasetId);
        String name = cleanFileName(dto.getName());
        validateVideoName(name);
        String objectKey = ensurePrefix(dataset) + "/" + UUID.randomUUID().toString().replace("-", "") + "-" + name;
        String uploadUrl = minioUtil.getEncryptedPutUrl(bucketName, objectKey, presignedUrlExpiry);
        if (!UPLOAD_UPLOADING.equals(dataset.getUploadStatus())) {
            datasetMapper.markUploading(datasetId);
        }
        return new VideoDatasetUploadUrlVO(datasetId, name, bucketName, objectKey, uploadUrl);
    }

    @Override
    @Transactional
    public VideoDatasetFileVO commitFile(Long datasetId, VideoDatasetFileCommitDTO dto) {
        VideoDataset dataset = requireDataset(datasetId);
        String name = cleanFileName(dto.getName());
        validateVideoName(name);
        String prefix = ensurePrefix(dataset);
        String objectKey = dto.getObjectKey();
        if (!objectKey.startsWith(prefix + "/")) {
            throw new BusinessException("Object does not belong to this dataset");
        }
        Map<String, Object> details = minioUtil.getFileDetails(bucketName, objectKey);
        if (details == null) {
            markUploadFailed(datasetId, "Uploaded object was not found in MinIO");
            throw new BusinessException("Uploaded object was not found in MinIO");
        }

        VideoDatasetFile file = new VideoDatasetFile()
                .setName(name)
                .setFileType(FILE_TYPE)
                .setFileExt(getExtension(name))
                .setDatasetId(datasetId)
                .setUrl(objectKey)
                .setObjectKey(objectKey)
                .setStorageBucket(bucketName)
                .setFileSize((Long) details.get("size"))
                .setContentType((String) details.get("type"))
                .setEtag((String) details.get("etag"))
                .setUploadStatus(UPLOAD_READY)
                .setMediaStatus("PENDING")
                .setExtractStatus("NOT_STARTED");
        file.setDeleted(false);
        file.setCreateUserId(userContextService.getCurUserId());
        try {
            fileMapper.insert(file);
        } catch (RuntimeException ex) {
            markUploadFailed(datasetId, ex.getMessage());
            throw ex;
        }
        if (datasetMapper.incrementFileCountAfterUpload(datasetId) != 1) {
            throw new BusinessException("Unable to update video dataset status");
        }
        videoMediaProbeService.probe(file);
        return toVO(file);
    }

    @Override
    public IPage<VideoDatasetFileVO> listFiles(Long datasetId, Page<VideoDatasetFileVO> page) {
        requireDataset(datasetId);
        Page<VideoDatasetFile> filePage = new Page<>(page.getCurrent(), page.getSize());
        fileMapper.selectPage(filePage, new QueryWrapper<VideoDatasetFile>()
                .eq("dataset_id", datasetId)
                .eq("deleted", 0)
                .orderByDesc("create_time"));

        List<VideoDatasetFileVO> voList = new ArrayList<>();
        for (VideoDatasetFile file : filePage.getRecords()) {
            voList.add(toVO(file));
        }
        Page<VideoDatasetFileVO> voPage = new Page<>(filePage.getCurrent(), filePage.getSize(), filePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public VideoDatasetDetailVO getDetail(Long datasetId) {
        VideoDataset dataset = requireDataset(datasetId);
        VideoDatasetDetailVO vo = new VideoDatasetDetailVO();
        vo.setId(dataset.getId());
        vo.setName(dataset.getName());
        vo.setFileCount(dataset.getFileCount());
        vo.setStatus(dataset.getStatus());
        vo.setUploadStatus(dataset.getUploadStatus());
        vo.setUploadError(dataset.getUploadError());
        vo.setRemark(dataset.getRemark());
        vo.setCreateTime(dataset.getCreateTime());
        vo.setUpdateTime(dataset.getUpdateTime());
        // 抽帧统计：EXTRACTED 已抽帧，其余（NOT_STARTED/PENDING/EXTRACTING）未抽帧
        long extracted = 0L;
        long unextracted = 0L;
        Map<String, Object> stats = fileMapper.countByExtractStatus(datasetId);
        if (stats != null) {
            extracted = stats.get("extracted") == null ? 0L : ((Number) stats.get("extracted")).longValue();
            unextracted = stats.get("unextracted") == null ? 0L : ((Number) stats.get("unextracted")).longValue();
        }
        vo.setExtractedVideos(extracted);
        vo.setUnextractedVideos(unextracted);
        return vo;
    }

    private VideoDataset requireDataset(Long datasetId) {
        VideoDataset dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) {
            throw new BusinessException("Video dataset does not exist");
        }
        return dataset;
    }

    private void markUploadFailed(Long datasetId, String error) {
        datasetMapper.updateById(new VideoDataset().setId(datasetId)
                .setStatus(2006).setUploadStatus("FAILED").setUploadError(error));
    }

    private String ensurePrefix(VideoDataset dataset) {
        if (dataset.getStoragePrefix() != null && !dataset.getStoragePrefix().trim().isEmpty()) {
            return dataset.getStoragePrefix().replaceAll("/+", "/").replaceAll("/$", "");
        }
        String prefix = "dataset/video/" + dataset.getId() + "/origin";
        datasetMapper.updateById(new VideoDataset().setId(dataset.getId()).setStoragePrefix(prefix));
        return prefix;
    }

    private static String cleanFileName(String name) {
        String clean = name == null ? "" : name.replace('\\', '/');
        int slash = clean.lastIndexOf('/');
        return (slash >= 0 ? clean.substring(slash + 1) : clean).trim();
    }

    private static void validateVideoName(String name) {
        if (name.isEmpty() || !VIDEO_EXTENSIONS.contains(getExtension(name))) {
            throw new BusinessException("Only supported video files can be uploaded");
        }
    }

    private static String getExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 && dot < name.length() - 1 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static VideoDatasetFileVO toVO(VideoDatasetFile file) {
        VideoDatasetFileVO vo = new VideoDatasetFileVO();
        vo.setId(file.getId());
        vo.setDatasetId(file.getDatasetId());
        vo.setName(file.getName());
        vo.setFileType(file.getFileType());
        vo.setUrl(file.getUrl());
        vo.setObjectKey(file.getObjectKey());
        vo.setStorageBucket(file.getStorageBucket());
        vo.setFileSize(file.getFileSize());
        vo.setContentType(file.getContentType());
        vo.setEtag(file.getEtag());
        vo.setUploadStatus(file.getUploadStatus());
        vo.setMediaStatus(file.getMediaStatus());
        vo.setExtractStatus(file.getExtractStatus());
        vo.setConvertedUrl(file.getConvertedUrl());
        vo.setAnnotationStatus(file.getAnnotationStatus());
        vo.setCreateTime(file.getCreateTime());
        return vo;
    }
}
