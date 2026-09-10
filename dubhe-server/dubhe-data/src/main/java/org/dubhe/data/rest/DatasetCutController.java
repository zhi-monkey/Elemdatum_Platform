package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.CutRuleDTO;
import org.dubhe.data.service.DataSetCutService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;

/**
 * @package: org.dubhe.data.rest
 * @author: chystart
 * @create: 2024-04-06 15:34
 * @description: 数据集切分测试请求控制器
 **/
@Slf4j
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets/cut")
@Api(tags = "数据集标注信息逻辑切分")
public class DatasetCutController {

    @Resource
    private DataSetCutService dataSetCutService;

    @ApiOperation(value = "数据集逻辑切分", notes = "对coco标注格式进行切分")
    @GetMapping("/{datasetId}")
    @PreAuthorize(Permissions.DATA)
    public void cutXmlAnnotationFile(@PathVariable("datasetId") Long dataSetId, @RequestBody CutRuleDTO cutRuleDTO) {
        dataSetCutService.cutCocoAnnotationDataSet(dataSetId, cutRuleDTO);
    }
}
