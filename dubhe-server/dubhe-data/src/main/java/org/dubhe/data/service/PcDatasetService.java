package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dubhe.data.domain.dto.DatasetLabelInfoDTO;
import org.dubhe.data.domain.dto.PcDatasetCreateDTO;
import org.dubhe.data.domain.dto.PcDatasetFileCommitDTO;
import org.dubhe.data.domain.dto.PcDatasetAnnotationDTO;
import org.dubhe.data.domain.dto.PcDatasetUploadUrlDTO;
import org.dubhe.data.domain.entity.PcDataset;
import org.dubhe.data.domain.vo.PcDatasetDetailVO;
import org.dubhe.data.domain.vo.PcDatasetFileVO;
import org.dubhe.data.domain.vo.PcDatasetUploadUrlVO;

import java.util.List;

public interface PcDatasetService {
    PcDataset create(PcDatasetCreateDTO dto);

    PcDatasetUploadUrlVO createUploadUrl(Long datasetId, PcDatasetUploadUrlDTO dto);

    PcDatasetFileVO commitFile(Long datasetId, PcDatasetFileCommitDTO dto);

    /**
     * 查询点云数据集详情（基础信息 + 文件统计）
     *
     * @param datasetId 点云数据集ID
     * @return 点云数据集详情VO
     */
    PcDatasetDetailVO detail(Long datasetId);

    /**
     * 分页查询点云数据集的 PCD 文件列表
     *
     * @param datasetId 点云数据集ID
     * @param page      分页对象
     * @return 分页结果
     */
    IPage<PcDatasetFileVO> listFiles(Long datasetId, com.baomidou.mybatisplus.extension.plugins.pagination.Page<PcDatasetFileVO> page);

    /**
     * 删除单个 PCD 文件
     *
     * @param datasetId 点云数据集ID
     * @param fileId    文件ID
     */
    void deleteFile(Long datasetId, Long fileId);

    /**
     * 保存点云 PCD 文件的 3D Box 标注。
     * 标注内容写入 pc_dataset_file.pcd_metadata JSON 字段的 annotations 节点，
     * 不会新建数据库表，确保已部署环境的向后兼容。
     *
     * @param datasetId 点云数据集ID
     * @param fileId    PCD 文件ID
     * @param dto       前端回写的标注内容
     * @return 更新后的 PCD 文件 VO
     */
    PcDatasetFileVO saveAnnotations(Long datasetId, Long fileId, PcDatasetAnnotationDTO dto);

    /**
     * 生成 PCD 文件下载用的 MinIO 预签名 GET URL。
     * 浏览器直接通过此 URL 下载二进制 PCD，避免被网关 fallback 成 HTML。
     *
     * @param datasetId 点云数据集ID
     * @param fileId    PCD 文件ID
     * @return MinIO 预签名 URL（默认 7 天过期）
     */
    String getDownloadUrl(Long datasetId, Long fileId);

    /**
     * 查询点云数据集的标签列表
     * <p>
     * 复用 data_dataset_label + data_label 关联表（与图片数据集标签查询逻辑一致）
     * </p>
     *
     * @param datasetId 点云数据集ID
     * @return 标签信息列表
     */
    List<DatasetLabelInfoDTO> getLabels(Long datasetId);

    /**
     * 删除点云数据集（软删除）
     * <p>
     * 将 pc_dataset.deleted 设置为 1，同时删除关联的数据集组关系
     * </p>
     *
     * @param datasetId 点云数据集ID
     */
    void deleteDataset(Long datasetId);
}
