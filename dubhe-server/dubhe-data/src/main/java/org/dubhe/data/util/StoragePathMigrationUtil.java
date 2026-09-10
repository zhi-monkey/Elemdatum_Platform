package org.dubhe.data.util;

import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.dao.PcDatasetMapper;
import org.dubhe.data.dao.VideoDatasetMapper;
import org.dubhe.data.domain.entity.PcDataset;
import org.dubhe.data.domain.entity.VideoDataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * MinIO 存储路径迁移工具
 * 用于将视频和点云数据集从旧路径（dataset/video_{id}）迁移到新路径（dataset/video/{id}）
 *
 * 使用方法：
 * 1. 调用 migrateVideoDatasets() 迁移所有视频数据集
 * 2. 调用 migratePcDatasets() 迁移所有点云数据集
 * 3. 或调用 migrateAll() 迁移所有数据集
 *
 * 注意：此工具只负责复制 MinIO 文件，不删除旧文件（需要手动确认后删除）
 */
@Slf4j
@Component
public class StoragePathMigrationUtil {

    @Resource
    private VideoDatasetMapper videoDatasetMapper;

    @Resource
    private PcDatasetMapper pcDatasetMapper;

    @Resource
    private MinioUtil minioUtil;

    @Value("${minio.bucketName}")
    private String bucketName;

    /**
     * 迁移所有视频数据集
     */
    public void migrateVideoDatasets() {
        log.info("开始迁移视频数据集存储路径...");
        List<VideoDataset> datasets = videoDatasetMapper.selectList(null);
        int total = datasets.size();
        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (VideoDataset dataset : datasets) {
            if (dataset.getDeleted() != null && dataset.getDeleted()) {
                skipped++;
                continue;
            }

            String oldPrefix = "dataset/video_" + dataset.getId() + "/";
            String newPrefix = "dataset/video/" + dataset.getId() + "/";

            // 检查是否已经是新路径格式
            if (dataset.getStoragePrefix() != null && dataset.getStoragePrefix().startsWith("dataset/video/")) {
                log.info("视频数据集 {} (ID: {}) 已经是新路径格式，跳过", dataset.getName(), dataset.getId());
                skipped++;
                continue;
            }

            try {
                // 检查旧路径是否存在文件
                if (!hasFilesInPrefix(oldPrefix)) {
                    log.info("视频数据集 {} (ID: {}) 旧路径下没有文件，直接更新数据库", dataset.getName(), dataset.getId());
                    updateVideoDatasetPrefix(dataset.getId(), newPrefix + "origin");
                    success++;
                    continue;
                }

                // 复制文件到新路径
                log.info("开始迁移视频数据集 {} (ID: {}) 从 {} 到 {}",
                        dataset.getName(), dataset.getId(), oldPrefix, newPrefix);
                copyFilesRecursive(oldPrefix, newPrefix);

                // 更新数据库
                updateVideoDatasetPrefix(dataset.getId(), newPrefix + "origin");

                success++;
                log.info("成功迁移视频数据集 {} (ID: {})", dataset.getName(), dataset.getId());
            } catch (Exception e) {
                failed++;
                log.error("迁移视频数据集 {} (ID: {}) 失败: {}", dataset.getName(), dataset.getId(), e.getMessage(), e);
            }
        }

        log.info("视频数据集迁移完成: 总数={}, 成功={}, 跳过={}, 失败={}", total, success, skipped, failed);
    }

    /**
     * 迁移所有点云数据集
     */
    public void migratePcDatasets() {
        log.info("开始迁移点云数据集存储路径...");
        List<PcDataset> datasets = pcDatasetMapper.selectList(null);
        int total = datasets.size();
        int success = 0;
        int skipped = 0;
        int failed = 0;

        for (PcDataset dataset : datasets) {
            if (dataset.getDeleted() != null && dataset.getDeleted()) {
                skipped++;
                continue;
            }

            String oldPrefix = "dataset/pc_" + dataset.getId() + "/";
            String newPrefix = "dataset/pc/" + dataset.getId() + "/";

            // 检查是否已经是新路径格式
            if (dataset.getStoragePrefix() != null && dataset.getStoragePrefix().startsWith("dataset/pc/")) {
                log.info("点云数据集 {} (ID: {}) 已经是新路径格式，跳过", dataset.getName(), dataset.getId());
                skipped++;
                continue;
            }

            try {
                // 检查旧路径是否存在文件
                if (!hasFilesInPrefix(oldPrefix)) {
                    log.info("点云数据集 {} (ID: {}) 旧路径下没有文件，直接更新数据库", dataset.getName(), dataset.getId());
                    updatePcDatasetPrefix(dataset.getId(), newPrefix + "origin");
                    success++;
                    continue;
                }

                // 复制文件到新路径
                log.info("开始迁移点云数据集 {} (ID: {}) 从 {} 到 {}",
                        dataset.getName(), dataset.getId(), oldPrefix, newPrefix);
                copyFilesRecursive(oldPrefix, newPrefix);

                // 更新数据库
                updatePcDatasetPrefix(dataset.getId(), newPrefix + "origin");

                success++;
                log.info("成功迁移点云数据集 {} (ID: {})", dataset.getName(), dataset.getId());
            } catch (Exception e) {
                failed++;
                log.error("迁移点云数据集 {} (ID: {}) 失败: {}", dataset.getName(), dataset.getId(), e.getMessage(), e);
            }
        }

        log.info("点云数据集迁移完成: 总数={}, 成功={}, 跳过={}, 失败={}", total, success, skipped, failed);
    }

    /**
     * 迁移所有数据集
     */
    public void migrateAll() {
        log.info("开始迁移所有数据集存储路径...");
        migrateVideoDatasets();
        migratePcDatasets();
        log.info("所有数据集迁移完成");
    }

    /**
     * 检查指定前缀下是否有文件
     */
    private boolean hasFilesInPrefix(String prefix) {
        try {
            List<String> objects = minioUtil.getObjects(bucketName, prefix);
            return objects != null && !objects.isEmpty();
        } catch (Exception e) {
            log.error("检查前缀 {} 是否有文件时出错: {}", prefix, e.getMessage());
            return false;
        }
    }

    /**
     * 递归复制文件从旧路径到新路径
     */
    private void copyFilesRecursive(String oldPrefix, String newPrefix) {
        try {
            // 获取所有需要复制的文件
            List<String> objectNames = minioUtil.listObjectsInFolder(bucketName, oldPrefix);

            if (objectNames == null || objectNames.isEmpty()) {
                log.info("旧路径 {} 下没有文件需要复制", oldPrefix);
                return;
            }

            log.info("找到 {} 个文件需要复制", objectNames.size());

            // 逐个复制文件
            for (String oldKey : objectNames) {
                String newKey = oldKey.replace(oldPrefix, newPrefix);
                log.debug("复制文件: {} -> {}", oldKey, newKey);

                // 使用 MinioUtil 的 copyObject 方法进行服务端复制
                minioUtil.copyObject(bucketName, oldKey, newKey);
            }

            log.info("成功复制 {} 个文件", objectNames.size());
        } catch (Exception e) {
            throw new RuntimeException("复制文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新视频数据集的存储前缀
     */
    private void updateVideoDatasetPrefix(Long datasetId, String newPrefix) {
        VideoDataset update = new VideoDataset();
        update.setId(datasetId);
        update.setStoragePrefix(newPrefix);
        videoDatasetMapper.updateById(update);
    }

    /**
     * 更新点云数据集的存储前缀
     */
    private void updatePcDatasetPrefix(Long datasetId, String newPrefix) {
        PcDataset update = new PcDataset();
        update.setId(datasetId);
        update.setStoragePrefix(newPrefix);
        pcDatasetMapper.updateById(update);
    }

    /**
     * 清理旧路径的文件（谨慎使用！建议手动验证后再调用）
     */
    public void cleanupOldPaths(String datasetType, Long datasetId) {
        String oldPrefix = "dataset/" + datasetType + "_" + datasetId + "/";
        log.warn("准备删除旧路径文件: {}", oldPrefix);

        try {
            minioUtil.delFolder(bucketName, oldPrefix);
            log.info("成功清理旧路径: {}", oldPrefix);
        } catch (Exception e) {
            log.error("清理旧路径 {} 失败: {}", oldPrefix, e.getMessage(), e);
        }
    }
}
