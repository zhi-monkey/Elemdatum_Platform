package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.VideoFileSearchDTO;
import org.dubhe.data.service.VideoFileSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description 视频文件检索
 * @date 2026-08-27
 */
@Api(tags = "数据处理：视频文件检索")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/video/datasets")
public class VideoFileSearchController {

    @Autowired
    private VideoFileSearchService videoFileSearchService;

    @ApiOperation("多条件检索视频文件")
    @PostMapping(value = "/files/search")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody search(@RequestBody VideoFileSearchDTO query) {
        return new DataResponseBody(PageUtil.toPage(videoFileSearchService.search(query)));
    }

    @ApiOperation("视频数据集列表（检索数据集下拉用）")
    @GetMapping(value = "/list")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listDatasets() {
        return new DataResponseBody(videoFileSearchService.listDatasets());
    }
}
