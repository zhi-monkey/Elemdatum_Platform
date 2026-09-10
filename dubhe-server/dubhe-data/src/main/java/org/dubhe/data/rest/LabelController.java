

package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.constant.DatasetLabelEnum;
import org.dubhe.data.domain.dto.LabelDeleteDTO;
import org.dubhe.data.domain.dto.LabelUpdateDTO;
import org.dubhe.data.domain.dto.LabelZipDTO;
import org.dubhe.data.domain.entity.Label;
import org.dubhe.data.service.DatasetService;
import org.dubhe.data.service.LabelService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


/**
 * @description 标签管理
 * @date 2020-04-10
 */
@Api(tags = "数据处理：标签管理")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets")
public class LabelController {

    @Autowired
    private LabelService labelService;
    @Autowired
    private DatasetService datasetService;

    @ApiOperation(value = "标签创建")
    @PostMapping(value = "/{datasetId}/labels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody create(@RequestBody Label label, @PathVariable(name = "datasetId") Long datasetId) {
        datasetService.saveLabel(label, datasetId);
        return new DataResponseBody();
    }

    @ApiOperation(value = "保存标签并绑定标签和应用任务(算法包)")
    @PostMapping(value = "/saveLabelByReadFile")
    // @PreAuthorize(Permissions.DATA) // 不确定非管理员上传算法包时能不能过这个permission的校验, 以防万一先注释掉
    public DataResponseBody<List<Long>> saveLabelByReadFile(@RequestBody String appZipPath) {
        return new DataResponseBody(labelService.saveLabelByReadFile(appZipPath));
    }

    /**
     * 根据 id list 标签删除 请求示例: /labels?labelIds=1&labelIds=2&labelIds=3
     * @param labelIds 标签id集合
     * @return {@link DataResponseBody }
     */
    @ApiOperation(value = "标签删除")
    @DeleteMapping(value = "/removeLabelByIds")
    // @PreAuthorize(Permissions.DATA) // 同上注掉
    public DataResponseBody removeLabelByIds(@RequestParam List<Long> labelIds) {
        labelService.deleteByIds(labelIds);
        return new DataResponseBody();
    }

    @ApiOperation(value = "根据id集合查询标签")
    @GetMapping(value = "/labels/ids")
    // @PreAuthorize(Permissions.DATA)
    public DataResponseBody list(@RequestParam("labelIds") List<Long> labelIds) {
        List<Label> labels = labelService.findLabelByIds(labelIds);
        List<LabelZipDTO> labelZipDTOList = labels.stream()
                .map(label -> {
                    LabelZipDTO labelZipDTO = new LabelZipDTO();
                    BeanUtils.copyProperties(label, labelZipDTO);
                    return labelZipDTO;
                })
                .collect(Collectors.toList());
        return new DataResponseBody(labelZipDTOList);
    }

    @ApiOperation(value = "标签查询")
    @GetMapping(value = "/{datasetId}/labels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(@PathVariable(name = "datasetId") Long datasetId) {
        return new DataResponseBody(labelService.list(datasetId));
    }

    @ApiOperation(value = "根据类型获取预置标签集合")
    @GetMapping(value = "/labels/auto/{labelGroupType}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody listSupportAutoByType(@PathVariable(value = "labelGroupType") Integer labelGroupType) {
        return new DataResponseBody(labelService.listSupportAutoByType(labelGroupType));
    }

    @ApiOperation(value = "获取预置标签类型")
    @GetMapping(value = "/presetLabels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getPresetLabels() {
        return new DataResponseBody(DatasetLabelEnum.getPresetLabels());
    }

    @ApiOperation(value = "获取coco预置标签")
    @GetMapping(value = "/pubLabels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody getPubLabels(Integer labelGroupType) {
        return new DataResponseBody(labelService.getPubLabels(labelGroupType));
    }

    @ApiOperation(value = "根据标签组类型获取标签数据")
    @GetMapping(value = "/labels/{labelGroupType}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody query(@PathVariable(name = "labelGroupType") Integer labelGroupType) {
        return new DataResponseBody(labelService.findByLabelGroupType(labelGroupType));
    }

    @ApiOperation(value = "删除标签")
    @DeleteMapping(value = "/labels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody delete(@Validated @RequestBody LabelDeleteDTO labelDeleteDTO) {
        labelService.delete(labelDeleteDTO);
        return new DataResponseBody();
    }

    @ApiOperation(value = "标签修改")
    @PutMapping(value = "/labels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody update(@Validated @RequestBody LabelUpdateDTO labelUpdateDTO) {
        return new DataResponseBody(labelService.update(labelUpdateDTO));
    }


    @ApiOperation(value = "批量创建数据集和标签绑定")
    @PostMapping(value = "/{datasetId}/bandLabels")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody create(@RequestBody List<Long> labelIds, @PathVariable(name = "datasetId") Long datasetId) {
         labelService.findLabelByIds(labelIds).forEach(label -> {
             labelService.save(label, datasetId);
         });

        return new DataResponseBody();
    }

}
