

package org.dubhe.data.service;

import org.dubhe.biz.base.enums.DatasetTypeEnum;
import org.dubhe.biz.permission.annotation.DataPermissionMethod;
import org.dubhe.data.domain.dto.*;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.DatasetVersion;
import org.dubhe.data.domain.entity.Label;
import org.dubhe.data.domain.vo.*;

import java.util.List;
import java.util.Map;

/**
 * @description 数据集版本信息服务
 * @date 2020-05-14
 */
public interface DatasetVersionService {

    /**
     * 数据集版本发布
     *
     * @param datasetVersionCreateDTO
     * @return String 版本名
     */
    String publish(DatasetVersionCreateDTO datasetVersionCreateDTO);


    /**
     * 数据集版本发布2
     *
     * @param datasetVersionCreateDTO
     * @return long 版本id
     */
    Long publish2(DatasetVersionCreateDTO datasetVersionCreateDTO);

    /**
     * 数据集版本列表
     *
     * @param datasetVersionQueryCriteria 查询条件
     * @return Map<String, Object> 版本列表
     */
    Map<String, Object> getList(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria);

    @DataPermissionMethod(dataType = DatasetTypeEnum.PUBLIC)
    Map<String, Object> getDetailList(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria);

    /**
     * 数据集版本删除
     *
     * @param datasetVersionDeleteDTO 数据集版本删除条件
     */
    void versionDelete(DatasetVersionDeleteDTO datasetVersionDeleteDTO);

    /**
     * 数据集版本切换
     *
     * @param datasetId   数据集id
     * @param versionName 版本名称
     */
    void versionSwitch(Long datasetId, String versionName);

    /**
     * 获取下一个可用版本号
     *
     * @param datasetId 数据集id
     * @return String 下一个可用版本名称
     */
    String getNextVersionName(Long datasetId);

    /**
     * 获取下一个可用切分版本号
     *
     * @param datasetId 数据集id
     * @return String 下一个可用版本名称
     */
    String getNextSplitVersionName(Long datasetId);

    /**
     * 数据集版本数据删除
     *
     * @param datasetId     数据集id
     * @param deleteFlag    删除标识
     */
    void updateStatusByDatasetId(Long datasetId, Boolean deleteFlag);

    /**
     * 训练任务所需版本
     *
     * @param id 数据集id
     * @return List<DatasetVersionVO> 版本列表
     */
    List<DatasetVersionVO> versionList(Long id);

    /**
     * 数据转换回调接口
     *
     * @param datasetVersionId    版本id
     * @param conversionCreateDTO 数据转换回调参数
     * @return int 影响版本数量
     */
    int finishConvert(Long datasetVersionId, ConversionCreateDTO conversionCreateDTO);

    /**
     * 文件复制
     */
    void fileCopy();

    /**
     * 标注文件复制
     */
    void annotationFileCopy();

    /**
     * 查询当前数据集版本的原始文件数量
     *
     * @param datasetId 数据集id
     * @return Integer 原始文件数量
     */
    Integer getSourceFileCount(Long datasetId);

    /**
     * 获取数据集版本详情
     *
     * @param datasetVersionId 数据集版本ID
     * @return 数据集版本详情
     */
    DatasetVersion detail(Long datasetVersionId);

    /**
     * 更新数据集版本状态
     *
     * @param id            数据集ID
     * @param sourceStatus  原状态
     * @param targetStatus  目标状态
     */
    void update(Long id, Integer sourceStatus, Integer targetStatus);

    /**
     * 获取数据集版本数据
     *
     * @param dataset 数据集实体
     * @return 数据集版本信息
     */
    DatasetVersion getDatasetVersionSourceVersion(Dataset dataset);

    /**
     * 获取数据集版本
     *
     * @param datasetId 数据集ID
     * @param versionName  版本名
     * @return DatasetVersion 数据集版本
     */
    DatasetVersion getVersionByDatasetIdAndVersionName(Long datasetId, String versionName);

    /**
     * 根据数据集ID删除数据信息
     *
     * @param datasetId 数据集ID
     */
    void deleteByDatasetId(Long datasetId);

    /**
     * 备份数据集版本数据
     * @param originDataset      原数据集实体
     * @param targetDateset      目标数据集实体
     * @param currentVersionName 版本名称
     */
    void backupDatasetVersionDataByDatasetId(Dataset originDataset, Dataset targetDateset, String currentVersionName);

    /**
     * 根据数据集ID查询版本名称列表
     * @param datasetId 数据集ID
     * @return  版本名称
     */
    List<String> getDatasetVersionNameListByDatasetId(Long datasetId);

    /**
     * 生成ofRecord文件
     *
     * @param datasetId          数据集ID
     * @param versionName        版本名称
     */
    void createOfRecord(Long datasetId, String versionName);

//    /**
//     * 插入es数据
//     *
//     * @param versionSource         源版本
//     * @param versionTarget         目标版本
//     * @param datasetId             数据集id
//     * @param datasetIdTarget       目标数据集id
//     * @param fileNameMap           文件列表
//     */
//    void insertEsData(String versionSource, String versionTarget, Long datasetId, Long datasetIdTarget, Map<String, Long> fileNameMap);

    /**
     * 生成版本数据
     *
     * @param datasetVersion  版本详情
     */
    void insertOne(DatasetVersion datasetVersion);

    /**
     * 更新版本
     *
     * @param datasetVersion 数据集版本
     */
    void updateByEntity(DatasetVersion datasetVersion);



    /**
     * 更新版本
     *
     * @return 版本发布情况
     */
    DatasetVersionStatVO getDatasetVersionStat();


    /**
     * 根据id查询具体的是数据集版本
     */
    DatasetVersionVO getDatasetVersionById(Long datasetVersionId);

    /**
     * 根据id list 查询数据集版本 list
     * @param ids
     * @return
     */
    List<DatasetVersionVO> listByIds(List<Long> ids);

    /**
     * 数据集版本切分发布
     * @param versionId
     * @param splitSize
     * @return
     */
    List<Long> publishSplit(Long versionId, Double splitSize);

    /**
     * 根据数据集的 id 获取对应发布的数据集版本
     *
     * @param datasetId
     * @return
     */
    List<String> getDatasetVersionNameList(Long datasetId);


    /**
     * 按照数据集id获取对应datasetversion实体列表
     * @param datasetId
     * @return
     */
    List<DatasetVersionVO> getDatasetVersionList(Long datasetId);

    /**
     * 按照数据集id获取对应已公开的datasetversion实体列表
     * @param datasetId
     * @return
     */
    List<DatasetVersionVO> getPublicDatasetVersionList(Long datasetId);

    boolean publishDatasetVersion(Long datasetId, String versionName);

    boolean cancelPublishDatasetVersion(Long datasetId, String versionName);

    boolean datasetExport(Long datasetId, String versionName);

    /**
     * 从origin目录导出已标注的数据（未版本化导出）
     * @param datasetId 数据集ID
     * @return null表示成功，其他表示失败原因
     */
    String datasetExportFromOrigin(Long datasetId);

    String checkTaskStatus(Long datasetVersionId);

    /**
     * 检查从origin导出的任务状态
     * @param datasetId 数据集ID
     * @return 导出状态：going, success, failed
     */
    String checkOriginExportStatus(Long datasetId);

    /**
     * 获取从origin导出的下载URL
     * @param datasetId 数据集ID
     * @return 下载URL
     */
    String getOriginExportUrl(Long datasetId);

    /**
     * 从origin导出未标注的数据（包括空标注和无标注文件）
     * @param datasetId 数据集ID
     * @return 错误信息，null表示成功
     */
    String datasetExportUnannotatedFromOrigin(Long datasetId);

    /**
     * 检查从origin导出未标注数据的任务状态
     * @param datasetId 数据集ID
     * @return 导出状态：going, success, failed
     */
    String checkOriginUnannotatedExportStatus(Long datasetId);

    /**
     * 获取从origin导出未标注数据的下载 URL
     * @param datasetId 数据集ID
     * @return 下载 URL
     */
    String getOriginUnannotatedExportUrl(Long datasetId);

     void cleanZips();

    Long release(DatasetVersionCreateDTO datasetVersionCreateDTO);

    /**
     * 数据集版本发布（支持选择是否存档）
     * @param datasetVersionCreateDTO 数据集版本创建DTO
     * @return 版本ID
     */
    Long releaseWithOption(DatasetVersionCreateDTO datasetVersionCreateDTO);

    List<Label> getDatasetVersionLabels(Long datasetId, String versionName);

    Integer getVersionImgCount(Long datasetId, String versionName);

    ImportingVersionInfoDTO getImportingVersionInfo(Long fromVersionId, Long targetDatasetId);

    Map<Long, List<Label>> getLabelsForVersions(List<Long> versionIds);

//    boolean datasetAssembleFromMultipleVersion(List<DatasetVersionKey> versions, String targetDir ,List<String> keepLabels);

    boolean datasetAssembleFromMultipleVersion(List<Long> versions, String targetDir, List<String> keepLabels);

    DatasetSplitResult datasetSplit(String sourceDir, String targetDir, String splitRatio);

    void processDatasetAsync(String taskId, DatasetMergeRequest request);

    void processQuantizedAsync(String taskId, QuantizedMergeRequest request);

    List<DatasetVersionDetailVO> batchGetVersionsLabelCountMap(List<Long> versionIds, List<String> labelNames);

    Map<String, Object> getPublicDetailList(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria);

    List<DatasetVersionInfoVO> getVersionIdsWithDatasetInfo(List<Long> datasetIds);

    List<DatasetVersionDetailVO> getPublicDetailListAll(DatasetVersionQueryCriteriaDTO datasetVersionQueryCriteria);

    Map<String, Object> quickCalculateImportStats(Long sourceVersionId, List<String> targetLabelNames) throws Exception;

    Integer getFilteredVersionImgCount(Long datasetId, String versionName, List<String> labelNames);
}
