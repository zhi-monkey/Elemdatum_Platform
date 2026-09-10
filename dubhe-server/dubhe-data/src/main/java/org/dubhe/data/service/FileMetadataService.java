package org.dubhe.data.service;

import org.dubhe.data.domain.dto.FileMetadataDTO;

import java.util.List;

/**
 * @description 文件元信息服务接口
 * @date 2026-08-26
 */
public interface FileMetadataService {

    /**
     * 批量保存文件元信息（每个文件一条记录）
     *
     * @param fileIds   文件ID列表
     * @param datasetId 数据集ID
     * @param metadata  元信息（可选）
     */
    void saveBatch(List<Long> fileIds, Long datasetId, FileMetadataDTO metadata);
}
