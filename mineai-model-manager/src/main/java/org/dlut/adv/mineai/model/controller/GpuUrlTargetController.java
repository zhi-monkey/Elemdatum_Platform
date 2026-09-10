package org.dlut.adv.mineai.model.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.GpuUrlTarget;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.GpuUrlTargetService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/gpuUrlTarget")
@Api(tags = "GPU模型下发地址")
@Slf4j
public class GpuUrlTargetController {

    @Resource
    private GpuUrlTargetService gpuUrlTargetService;

    @GetMapping("/list")
    @ApiOperation("按名称或IP查询GPU模型下发地址")
    public Msg<?> list(@RequestParam(required = false) String keyword) {
        try {
            return new Msg<>(MsgCode.SUCCEED, gpuUrlTargetService.searchAvailable(keyword));
        } catch (Exception e) {
            log.error("查询GPU模型下发地址失败", e);
            return new Msg<>(MsgCode.FAILED, "查询GPU模型下发地址失败");
        }
    }

    @GetMapping("/page")
    @ApiOperation("分页查询GPU模型下发地址")
    public Msg<?> page(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String ip,
                       @RequestParam(required = false) String platformIp,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            String searchText = firstNonBlank(keyword, name, ip, platformIp);
            return new Msg<>(MsgCode.SUCCEED, gpuUrlTargetService.pageAvailable(searchText, page, pageSize));
        } catch (Exception e) {
            log.error("分页查询GPU模型下发地址失败", e);
            return new Msg<>(MsgCode.FAILED, "分页查询GPU模型下发地址失败");
        }
    }

    @PostMapping("/saveOrUpdate")
    @ApiOperation("保存GPU模型下发地址")
    public Msg<?> saveOrUpdate(@RequestBody GpuUrlTarget target) {
        try {
            return new Msg<>(MsgCode.SUCCEED, gpuUrlTargetService.saveOrUpdate(target));
        } catch (Exception e) {
            log.error("保存GPU模型下发地址失败", e);
            return new Msg<>(MsgCode.FAILED, "保存GPU模型下发地址失败，请检查参数");
        }
    }

    @DeleteMapping("/delete/{id}")
    @ApiOperation("删除GPU模型下发地址")
    public Msg<?> delete(@PathVariable Long id) {
        try {
            gpuUrlTargetService.deleteById(id);
            return new Msg<>(MsgCode.SUCCEED);
        } catch (Exception e) {
            log.error("删除GPU模型下发地址失败：id={}", id, e);
            return new Msg<>(MsgCode.FAILED, "删除GPU模型下发地址失败");
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }
}
