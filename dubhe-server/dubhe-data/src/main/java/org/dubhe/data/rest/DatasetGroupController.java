package org.dubhe.data.rest;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.constant.ResponseCode;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.base.vo.DatasetVO;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.cloud.authconfig.audit.SystemControllerLog;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.entity.Dataset;
import org.dubhe.data.domain.entity.DatasetGroup;
import org.dubhe.data.domain.vo.DatasetGroupMappingVO;
import org.dubhe.data.domain.vo.DatasetGroupVO;
import org.dubhe.data.service.impl.DatasetGroupServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets/group")
public class DatasetGroupController {

    private final DatasetGroupServiceImpl datasetGroupServiceImpl;
    private final AuditLogHelper auditLogHelper;

    public DatasetGroupController(DatasetGroupServiceImpl datasetGroupServiceImpl, AuditLogHelper auditLogHelper) {
        this.datasetGroupServiceImpl = datasetGroupServiceImpl;
        this.auditLogHelper = auditLogHelper;
    }

    @PostMapping("/createGroup")
    @SystemControllerLog(description = "dataset_group_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody<DatasetGroup> createGroup(@RequestBody DatasetGroup datasetGroup) {
        return new DataResponseBody<>(datasetGroupServiceImpl.create(datasetGroup));
    }

    @PutMapping("/editGroup")
    public DataResponseBody<DatasetGroup> editGroup(@RequestBody DatasetGroup datasetGroup) {
        DatasetGroup datasetGroupByName = datasetGroupServiceImpl.getDatasetGroupByName(datasetGroup.getName());
        if (datasetGroupByName != null && !datasetGroupByName.getId().equals(datasetGroup.getId())) {
            // 说明被修改的名称已存在, 且不是当前数据集组
            return new DataResponseBody<>(ResponseCode.ERROR, "组名已存在");
        }else {
            boolean updated = datasetGroupServiceImpl.updateById(datasetGroup);
            if (updated) {
                auditLogHelper.saveUpdateAuditLog("dataset_group_update",
                        "  datasetGroupId: " + datasetGroup.getId() + "  name: " + datasetGroup.getName());
            }
        }

        return new DataResponseBody<>();
    }

    @DeleteMapping("/deleteDatasetGroup/{datasetGroupId}")
    @SystemControllerLog(description = "dataset_group_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody deleteDatasetGroup(@PathVariable("datasetGroupId") Long datasetGroupId) {
        if (datasetGroupServiceImpl.deleteDatasetGroup(datasetGroupId)) {
            return new DataResponseBody<>();
        } else {
            return new DataResponseBody<>(ResponseCode.ERROR, "组内存在数据集, 请清空数据集后再删除");
        }
    }

    @GetMapping("/getAllDatasetGroup")
    public DataResponseBody<List<DatasetGroup>> getAllDatasetGroup() {
        return new DataResponseBody<>(datasetGroupServiceImpl.getAllDatasetGroup());
    }
    @GetMapping("/getAllDatasetGroupPage")
    public DataResponseBody<Map<String, Object>> getAllDatasetGroupPage(
            Page page,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isGuided) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getAllDatasetGroupPage(page, name, isGuided));
    }

    @GetMapping("/getDatasetsByDatasetGroupId")
    public DataResponseBody<List<DatasetVO>> getDatasetsByDatasetGroupId(Long datasetGroupId) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getDatasetsByDatasetGroupId(datasetGroupId));
    }


    @GetMapping("/getDatasetsPageByDatasetGroupId")
    public DataResponseBody<IPage<DatasetVO>> getDatasetsPageByDatasetGroupId(Long datasetGroupId, Page  page) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getDatasetsPageByDatasetGroupId(datasetGroupId, page));
    }

    /**
     * 分页查询数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @return 分页结果
     */
    @GetMapping("/getDatasetGroupByPage")
    public DataResponseBody<IPage<DatasetGroup>> getDatasetGroupByPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.getDatasetGroupByPage(page);
        return new DataResponseBody<>(result);
    }

    /**
     * 分页查询数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @return 分页结果
     */
    @GetMapping("/getPrivateAndPublicDatasetGroupByPage")
    public DataResponseBody<IPage<DatasetGroup>> getPrivateAndPublicDatasetGroupByPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.getPrivateAndPublicDatasetGroupByPage(page);
        return new DataResponseBody<>(result);
    }

    /**
     * 按名称分页搜索数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @param name 搜索的名称关键词
     * @return 分页结果
     */
    @GetMapping("/searchDatasetGroupByName")
    public DataResponseBody<IPage<DatasetGroup>> searchDatasetGroupByName(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.searchDatasetGroupByName(page, name);
        return new DataResponseBody<>(result);
    }


    /**
     * 按名称分页搜索数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @param name 搜索的名称关键词
     * @return 分页结果
     */
    @GetMapping("/searchPrivateAndPublicDatasetGroupByName")
    public DataResponseBody<IPage<DatasetGroup>> searchPrivateAndPublicDatasetGroupByName(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.searchPrivateAndPublicDatasetGroupByName(page, name);
        return new DataResponseBody<>(result);
    }
    /**
     * 按名称分页搜索公开数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @param name 搜索的名称关键词
     * @return 分页结果
     */
    @GetMapping("/searchPublicDatasetGroupByName")
    public DataResponseBody<IPage<DatasetGroup>> searchPublicDatasetGroupByName(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.searchPublicDatasetGroupByName(page, name);
        return new DataResponseBody<>(result);
    }

    /**
     * 分页查询数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @return 分页结果
     */
    @GetMapping("/getPublicDatasetGroupByPage")
    public DataResponseBody<IPage<DatasetGroup>> getPublicDatasetGroupByPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.getPublicDatasetGroupByPage(page);
        return new DataResponseBody<>(result);
    }
    @GetMapping("/getPublicDatasetGroupByPageTYX")
    public DataResponseBody<Map<String, Object>> getPublicDatasetGroupByPageTYX(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isGuided) {

        Page<DatasetGroup> page = new Page<>(current, size);
        Map<String, Object> result = datasetGroupServiceImpl.getPublicDatasetGroupByPageTYX(page, name, isGuided);
        return new DataResponseBody<>(result);
    }




    @GetMapping("/getDatasetGroupsMapByDatasetIds")
    public DataResponseBody<List<DatasetGroupMappingVO>> getDatasetGroupsMapByDatasetIds(@RequestParam List<Long> datasetIds) {
        Map<DatasetGroup, List<Dataset>> map = datasetGroupServiceImpl.getDatasetGroupsMapByDatasetIds(datasetIds);

        List<DatasetGroupMappingVO> resultList = map.entrySet().stream()
                .map(entry -> {
                    DatasetGroupMappingVO vo = new DatasetGroupMappingVO();
                    vo.setGroup(BeanUtil.copyProperties(entry.getKey(), DatasetGroupVO.class));
                    vo.setDatasets(BeanUtil.copyToList(entry.getValue(), DatasetVO.class));
                    return vo;
                })
                .collect(Collectors.toList());

        return new DataResponseBody<>(resultList);
    }

    @GetMapping("getPublicDatasetsByDatasetGroupId")
    public DataResponseBody<List<DatasetVO>> getPublicDatasetsByDatasetGroupId(Long datasetGroupId) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getPublicDatasetsByDatasetGroupId(datasetGroupId));
    }

    @GetMapping("/getDatasetGroupNameByDatasetId")
    public DataResponseBody<String> getDatasetGroupNameByDatasetId(@RequestParam Long datasetId) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getDatasetGroupNameByDatasetId(datasetId));
    }

    /**
     * 根据数据集组ID和标注类型获取数据集列表
     * @param datasetGroupId 数据集组ID
     * @param annotateType 标注类型（102:目标检测, 103:语义分割）
     * @return 数据集列表
     */
    @GetMapping("/getDatasetsByDatasetGroupIdAndAnnotateType")
    public DataResponseBody<List<DatasetVO>> getDatasetsByDatasetGroupIdAndAnnotateType(
            @RequestParam Long datasetGroupId,
            @RequestParam Integer annotateType) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getDatasetsByDatasetGroupIdAndAnnotateType(datasetGroupId, annotateType));
    }

    /**
     * 根据数据集组ID和标注类型获取公开数据集列表
     * @param datasetGroupId 数据集组ID
     * @param annotateType 标注类型（102:目标检测, 103:语义分割）
     * @return 公开数据集列表
     */
    @GetMapping("/getPublicDatasetsByDatasetGroupIdAndAnnotateType")
    public DataResponseBody<List<DatasetVO>> getPublicDatasetsByDatasetGroupIdAndAnnotateType(
            @RequestParam Long datasetGroupId,
            @RequestParam Integer annotateType) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getPublicDatasetsByDatasetGroupIdAndAnnotateType(datasetGroupId, annotateType));
    }

    /**
     * 分页获取公开数据集组中的公开数据集
     * @param datasetGroupId 数据集组ID
     * @param page 分页参数
     * @return 分页结果
     */
    @GetMapping("/getPublicDatasetsPageByDatasetGroupId")
    public DataResponseBody<IPage<DatasetVO>> getPublicDatasetsPageByDatasetGroupId(Long datasetGroupId, Page page) {
        return new DataResponseBody<>(datasetGroupServiceImpl.getPublicDatasetsPageByDatasetGroupId(datasetGroupId, page));
    }

    /**
     * 按名称分页搜索私有数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @param name 搜索的名称关键词
     * @return 分页结果
     */
    @GetMapping("/searchPrivateDatasetGroupByName")
    public DataResponseBody<IPage<DatasetGroup>> searchPrivateDatasetGroupByName(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.searchPrivateDatasetGroupByName(page, name);
        return new DataResponseBody<>(result);
    }

    /**
     * 分页查询私有数据集分组
     * @param current 当前页码，默认为1
     * @param size 每页大小，默认为10
     * @return 分页结果
     */
    @GetMapping("/getPrivateDatasetGroupByPage")
    public DataResponseBody<IPage<DatasetGroup>> getPrivateDatasetGroupByPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {

        Page<DatasetGroup> page = new Page<>(current, size);
        IPage<DatasetGroup> result = datasetGroupServiceImpl.getPrivateDatasetGroupByPage(page);
        return new DataResponseBody<>(result);
    }

    /**
     * 获取包含未发布数据集的数据集组列表（用于多人标注任务创建）
     * @return 数据集组列表
     */
    @GetMapping("/getDatasetGroupsWithUnpublishedDatasets")
    public DataResponseBody<List<DatasetGroup>> getDatasetGroupsWithUnpublishedDatasets() {
        return new DataResponseBody<>(datasetGroupServiceImpl.getDatasetGroupsWithUnpublishedDatasets());
    }

}
