package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.PcFileSearchDTO;
import org.dubhe.data.service.PcFileSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description 点云文件检索
 * @date 2026-08-27
 */
@Api(tags = "数据处理：点云文件检索")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/pointcloud/datasets")
public class PcFileSearchController {

    @Autowired
    private PcFileSearchService pcFileSearchService;

    @ApiOperation("多条件检索点云文件")
    @PostMapping(value = "/files/search")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody search(@RequestBody PcFileSearchDTO query) {
        return new DataResponseBody(PageUtil.toPage(pcFileSearchService.search(query)));
    }

    @ApiOperation("点云数据集列表（检索数据集下拉用）")
    @GetMapping(value = "/list")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listDatasets() {
        return new DataResponseBody(pcFileSearchService.listDatasets());
    }
}
