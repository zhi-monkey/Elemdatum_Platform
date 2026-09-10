package org.dubhe.data.rest;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.domain.dto.DataRepoFileCreateDTO;
import org.dubhe.data.domain.dto.DataRepoFileQueryDTO;
import org.dubhe.data.domain.dto.DataRepoFileUpdateDTO;
import org.dubhe.data.domain.dto.FileTypeCountDTO;
import org.dubhe.data.domain.entity.DataRepoFile;
import org.dubhe.data.domain.vo.DataRepoFileVO;
import org.dubhe.data.service.DataRepoFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Api(tags = "数据处理：数据仓库文件管理")
@RestController
@RequestMapping("/datarepofiles") // 注意这里的 URL 前缀应该与您的项目实际情况相匹配
public class DataRepoFileController {

    @Autowired
    private DataRepoFileService dataRepoFileService;

    @ApiOperation(value = "数据仓库文件创建")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody createDataRepoFile(@Validated @RequestBody DataRepoFileCreateDTO dataRepoFileCreateDTO) {
        boolean isCreated = dataRepoFileService.createDataRepoFile(dataRepoFileCreateDTO);
        return new DataResponseBody(isCreated);
    }

    @ApiOperation(value = "批量创建数据仓库文件")
    @PostMapping("/batch-create")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody createDataRepoFiles(@Validated @RequestBody List<DataRepoFileCreateDTO> createDTOList) {
        boolean isCreated = dataRepoFileService.createDataRepoFiles(createDTOList);
        return new DataResponseBody(isCreated);
    }

    @ApiOperation(value = "数据仓库文件查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryDataRepoFiles(Page<DataRepoFile> page, DataRepoFileQueryDTO dataRepoFileQueryDTO) {
        Page<DataRepoFileVO> dataRepoFileVOPage = dataRepoFileService.listDataRepoFiles(page, dataRepoFileQueryDTO);
        return new DataResponseBody(dataRepoFileVOPage);
    }

    @ApiOperation(value = "数据仓库文件详情")
    @GetMapping(value = "/{dataRepoFileId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getDataRepoFile(@PathVariable(name = "dataRepoFileId") Long dataRepoFileId) {
        DataRepoFileVO dataRepoFileVO = dataRepoFileService.getDataRepoFileById(dataRepoFileId);
        return new DataResponseBody(dataRepoFileVO);
    }

    @ApiOperation(value = "数据仓库文件更新")
    @PutMapping(value = "/{dataRepoFileId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody updateDataRepoFile(@PathVariable(name = "dataRepoFileId") Long dataRepoFileId,
                                               @Validated @RequestBody DataRepoFileUpdateDTO dataRepoFileUpdateDTO) {
        boolean isUpdated = dataRepoFileService.updateDataRepoFile(dataRepoFileUpdateDTO, dataRepoFileId);
        return new DataResponseBody(isUpdated);
    }

    @ApiOperation(value = "数据仓库文件删除")
    @DeleteMapping(value = "/{dataRepoFileId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteDataRepoFile(@PathVariable(name = "dataRepoFileId") Long dataRepoFileId) {
        dataRepoFileService.deleteDataRepoFile(dataRepoFileId);
        return new DataResponseBody();
    }

    @ApiOperation(value = "获取数据集内的文件类型统计")
    @GetMapping(value = "countFileTypes/{datasetId}")
    public DataResponseBody countFileTypesInDataset(@PathVariable(name = "datasetId") Long datasetId) {
        FileTypeCountDTO fileTypeCount = dataRepoFileService.countFileTypesInDataset(datasetId);
        return new DataResponseBody(fileTypeCount);
    }

    @ApiOperation(value = "获取所有数据集的文件类型统计")
    @GetMapping(value = "countAllDatasetFileNumsByType")
    public DataResponseBody countAllDatasetFileNumsByType() {
        FileTypeCountDTO fileTypeCount = dataRepoFileService.countAllDatasetFileNumsByType();
        return new DataResponseBody(fileTypeCount);
    }

    @ApiOperation(value = "批量删除数据仓库文件")
    @DeleteMapping("/batch")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody deleteBatch(@RequestBody JSONObject jsonObject) {
        JSONArray jsonArray = jsonObject.getJSONArray("ids");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            ids.add(jsonArray.getLong(i));
        }
        boolean result = dataRepoFileService.deleteBatchByIds(ids);
        if (result) {
            return new DataResponseBody("删除成功");
        } else {
            return new DataResponseBody("删除失败");
        }
    }
}
