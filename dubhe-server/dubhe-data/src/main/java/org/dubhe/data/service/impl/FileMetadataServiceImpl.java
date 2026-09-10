package org.dubhe.data.service.impl;

import org.dubhe.data.dao.FileMetadataMapper;
import org.dubhe.data.domain.dto.FileMetadataDTO;
import org.dubhe.data.domain.entity.FileMetadata;
import org.dubhe.data.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @description 文件元信息服务实现
 * @date 2026-08-26
 */
@Service
public class FileMetadataServiceImpl implements FileMetadataService {

    private static final String DEFAULT_SOURCE_TYPE = "系统登记";

    @Autowired
    private FileMetadataMapper fileMetadataMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<Long> fileIds, Long datasetId, FileMetadataDTO metadata) {
        if (CollectionUtils.isEmpty(fileIds) || metadata == null) {
            return;
        }
        String sourceType = trimToNull(metadata.getSourceType());
        if (sourceType == null) {
            sourceType = DEFAULT_SOURCE_TYPE;
        }
        List<FileMetadata> list = new ArrayList<>(fileIds.size());
        for (Long fileId : fileIds) {
            list.add(FileMetadata.builder()
                    .fileId(fileId)
                    .datasetId(datasetId)
                    .sourceType(sourceType)
                    .captureTime(trimToNull(metadata.getCaptureTime()))
                    .device(trimToNull(metadata.getDevice()))
                    .deviceSn(trimToNull(metadata.getDeviceSn()))
                    .location(trimToNull(metadata.getLocation()))
                    .scenario(trimToNull(metadata.getScenario()))
                    .lighting(trimToNull(metadata.getLighting()))
                    .quality(trimToNull(metadata.getQuality()))
                    .build());
        }
        for (FileMetadata item : list) {
            fileMetadataMapper.insert(item);
        }
    }

    /**
     * 空串转 null（避免空字符串写入 DATETIME 等列）
     */
    private static String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
