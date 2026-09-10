package org.dubhe.data.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.entity.HttpCamera;
import org.dubhe.data.service.HttpCameraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "数据处理：HTTP摄像机")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/http-cameras")
public class HttpCameraController {

    @Autowired
    private HttpCameraService httpCameraService;

    @ApiOperation(value = "获取全部可用摄像机")
    @GetMapping("/all")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<HttpCamera>> findAll() {
        return new DataResponseBody<>(httpCameraService.listAllAvailable());
    }
}
