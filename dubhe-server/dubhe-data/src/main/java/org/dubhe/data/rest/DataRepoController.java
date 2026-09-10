package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.AuditLogConstants;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.cloud.authconfig.audit.AuditLogHelper;
import org.dubhe.cloud.authconfig.audit.SystemControllerLog;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.DataRepoCreateDTO;
import org.dubhe.data.domain.dto.DataRepoQueryDTO;
import org.dubhe.data.domain.dto.DataRepoUpdateDTO;
import org.dubhe.data.domain.entity.DataRepo;
import org.dubhe.data.domain.vo.DataRepoVO;
import org.dubhe.data.service.DataRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "数据处理：数据仓库管理")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datarepos")
public class DataRepoController {

    @Autowired
    private DataRepoService dataRepoService;
    @Autowired
    private AuditLogHelper auditLogHelper;

    @ApiOperation(value = "数据仓库创建")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "data_repo_add", recordParams = true, operationType = AuditLogConstants.OperationType.ADD)
    public DataResponseBody createDataRepo(@Validated @RequestBody DataRepoCreateDTO dataRepoCreateDTO) {
        boolean isCreated = dataRepoService.createDataRepo(dataRepoCreateDTO);
        return new DataResponseBody(isCreated);
    }

    @ApiOperation(value = "数据仓库查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryDataRepos(Page<DataRepo> page, DataRepoQueryDTO dataRepoQueryDTO) {
        dataRepoQueryDTO.setDeleted(false);
        Page<DataRepoVO> dataRepoVOPage = dataRepoService.listDataRepos(page, dataRepoQueryDTO);
        return new DataResponseBody(dataRepoVOPage);
    }

    @ApiOperation(value = "数据仓库详情")
    @GetMapping(value = "/{dataRepoId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getDataRepo(@PathVariable(name = "dataRepoId") Long dataRepoId) {
        DataRepoVO dataRepoVO = dataRepoService.getDataRepoById(dataRepoId);
        return new DataResponseBody(dataRepoVO);
    }

    @ApiOperation(value = "数据仓库更新")
    @PutMapping(value = "/{dataRepoId}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody updateDataRepo(@PathVariable(name = "dataRepoId") Long dataRepoId,
                                           @Validated @RequestBody DataRepoUpdateDTO dataRepoUpdateDTO) {
        boolean isUpdated = dataRepoService.updateDataRepo(dataRepoUpdateDTO, dataRepoId);
        if (isUpdated) {
            auditLogHelper.saveUpdateAuditLog("data_repo_update",
                    "  dataRepoId: " + dataRepoId + "  name: " + dataRepoUpdateDTO.getName());
        }
        return new DataResponseBody(isUpdated);
    }

    @ApiOperation(value = "数据仓库删除")
    @DeleteMapping(value = "/{dataRepoId}")
    @PreAuthorize(Permissions.DATA)
    @SystemControllerLog(description = "data_repo_delete", recordParams = true, operationType = AuditLogConstants.OperationType.DELETE)
    public DataResponseBody deleteDataRepo(@PathVariable(name = "dataRepoId") Long dataRepoId) {
        dataRepoService.deleteDataRepo(dataRepoId);
        return new DataResponseBody();
    }

    @ApiOperation(value = "数据仓库查询所有")
    @GetMapping(value = "/queryAllDataRepos")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryAllDataRepos() {
        List<DataRepoVO> dataRepoVOS = dataRepoService.queryAllDataRepos();
        return new DataResponseBody(dataRepoVOS);
    }

    @ApiOperation(value = "数据仓库查询所有非空")
    @GetMapping(value = "/queryAllDataReposNotNull")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody queryAllDataReposNotNull() {
        List<DataRepoVO> dataRepoVOS = dataRepoService.queryAllDataReposNotNull();
        return new DataResponseBody(dataRepoVOS);
    }

    @GetMapping("/isreponameexist/{name}")
    public DataResponseBody isDepartmentNameExists(@PathVariable String name) {
        boolean exists = dataRepoService.isNameExist(name);
        return new DataResponseBody(exists);
    }
}
