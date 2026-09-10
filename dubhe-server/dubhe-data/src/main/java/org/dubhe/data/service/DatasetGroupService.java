package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dubhe.biz.base.vo.DatasetVO;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.DatasetDatasetGroup;
import org.dubhe.data.domain.entity.DatasetGroup;

import java.util.List;
import java.util.Map;

/**
 * @description 数据集分组服务
 * @author mingming
 * @date 2025/09/05
 */
public interface DatasetGroupService {
    List<DatasetGroup> getAllDatasetGroup();

    IPage<DatasetVO> getDatasetsPageByDatasetGroupId(Long datasetGroupId, Page<DatasetDatasetGroup> page);


    Map<String, Object> getAllDatasetGroupPage(Page<DatasetGroup> page, String name);

    /**
     * 分页查询数据集分组
     * @param page 分页参数
     * @return 分页结果
     */
    IPage<DatasetGroup> getDatasetGroupByPage(Page<DatasetGroup> page);


    /**
     * 按名称分页搜索数据集分组
     * @param page 分页参数
     * @param name 搜索关键词
     * @return 分页结果
     */
    IPage<DatasetGroup> searchDatasetGroupByName(Page<DatasetGroup> page, String name);

    DatasetGroup create(DatasetGroup datasetGroup);


    List<DatasetVO> getDatasetsByDatasetGroupId(Long datasetGroupId);

    IPage<DatasetGroup> searchPublicDatasetGroupByName(Page<DatasetGroup> page, String name);

    IPage<DatasetGroup> getPublicDatasetGroupByPage(Page<DatasetGroup> page);

    Map<String, Object> getPublicDatasetGroupByPageTYX(Page<DatasetGroup> page, String name, Boolean isGuided);

    Map<DatasetGroup, List<Dataset>> getDatasetGroupsMapByDatasetIds(List<Long> datasetIds);

    List<DatasetVO> getPublicDatasetsByDatasetGroupId(Long datasetGroupId);

    String getDatasetGroupNameByDatasetId(Long datasetId);

    boolean datasetGroupExists(String datasetGroupName);

    DatasetGroup getDatasetGroupByName(String datasetGroupName);

    boolean deleteDatasetGroup(Long datasetGroupId);
    /**
     * 分页获取公开数据集组中的公开数据集
     * @param datasetGroupId 数据集组ID
     * @param page 分页参数
     * @return 分页结果
     */
    IPage<DatasetVO> getPublicDatasetsPageByDatasetGroupId(Long datasetGroupId, Page<DatasetDatasetGroup> page);

    boolean datasetGroupExistsById(Long datasetGroupId);

    /**
     * 按名称分页搜索私有数据集分组
     * @param page 分页参数
     * @param name 搜索关键词
     * @return 分页结果
     */
    IPage<DatasetGroup> searchPrivateDatasetGroupByName(Page<DatasetGroup> page, String name);

    /**
     * 分页查询私有数据集分组
     * @param page 分页参数
     * @return 分页结果
     */
    IPage<DatasetGroup> getPrivateDatasetGroupByPage(Page<DatasetGroup> page);

    /**
     * 根据数据集组ID和标注类型获取数据集列表
     * @param datasetGroupId 数据集组ID
     * @param annotateType 标注类型（102:目标检测, 103:语义分割）
     * @return 数据集列表
     */
    List<DatasetVO> getDatasetsByDatasetGroupIdAndAnnotateType(Long datasetGroupId, Integer annotateType);

    /**
     * 根据数据集组ID和标注类型获取公开数据集列表
     * @param datasetGroupId 数据集组ID
     * @param annotateType 标注类型（102:目标检测, 103:语义分割）
     * @return 公开数据集列表
     */
    List<DatasetVO> getPublicDatasetsByDatasetGroupIdAndAnnotateType(Long datasetGroupId, Integer annotateType);

    /**
     * 获取包含未发布数据集的数据集组列表（用于多人标注任务创建）
     * @return 数据集组列表
     */
    List<DatasetGroup> getDatasetGroupsWithUnpublishedDatasets();
}
