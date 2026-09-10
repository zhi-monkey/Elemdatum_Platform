package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.biz.db.utils.PageUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.FileSearchDTO;
import org.dubhe.data.service.FileSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @description 图片多条件检索
 * @date 2026-08-26
 */
@Api(tags = "数据处理：文件检索")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets")
public class FileSearchController {

    @Autowired
    private FileSearchService fileSearchService;

    @ApiOperation("多条件检索图片")
    @PostMapping(value = "/files/search")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody search(@RequestBody FileSearchDTO query) {
        return new DataResponseBody(PageUtil.toPage(fileSearchService.search(query)));
    }

    @ApiOperation("检索标签列表（现存全部标签）")
    @GetMapping(value = "/files/search/labels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listLabels() {
        return new DataResponseBody(fileSearchService.listAllLabels());
    }
}
