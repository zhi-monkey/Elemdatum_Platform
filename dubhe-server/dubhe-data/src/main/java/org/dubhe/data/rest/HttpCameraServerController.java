package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.dto.HttpCameraSyncResult;
import org.dubhe.data.domain.entity.HttpCamera;
import org.dubhe.data.domain.entity.HttpCameraServer;
import org.dubhe.data.service.HttpCameraService;
import org.dubhe.data.service.HttpCameraServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "数据处理：HTTP摄像头服务器")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/http-camera-servers")
public class HttpCameraServerController {

    @Value("${rsa.public_key:}")
    private String rsaPublicKey;

    @Autowired
    private HttpCameraServerService httpCameraServerService;

    @Autowired
    private HttpCameraService httpCameraService;

    @ApiOperation(value = "获取加密公钥（Token加密用）")
    @GetMapping("/public-key")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<String> getPublicKey() {
        return new DataResponseBody<>(rsaPublicKey);
    }

    @ApiOperation(value = "HTTP摄像头服务器列表（全量）")
    @GetMapping("/all")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<HttpCameraServer>> findAll() {
        return new DataResponseBody<>(httpCameraServerService.findAllAvailable());
    }

    @ApiOperation(value = "HTTP摄像头服务器分页查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Page<HttpCameraServer>> page(
            Page<HttpCameraServer> page,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String serverUrl,
            @RequestParam(required = false) String description) {
        HttpCameraServer query = new HttpCameraServer();
        query.setName(name);
        query.setServerUrl(serverUrl);
        query.setDescription(description);
        return new DataResponseBody<>(httpCameraServerService.page(page, query));
    }

    @ApiOperation(value = "新增HTTP摄像头服务器")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<HttpCameraServer> create(@RequestBody HttpCameraServerSaveDTO dto) {
        HttpCameraServer server = buildEntity(dto);
        return new DataResponseBody<>(httpCameraServerService.saveOrUpdate(server, dto.getAuthToken()));
    }

    @ApiOperation(value = "更新HTTP摄像头服务器")
    @PutMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<HttpCameraServer> update(@PathVariable Long id, @RequestBody HttpCameraServerSaveDTO dto) {
        HttpCameraServer server = buildEntity(dto);
        server.setId(id);
        return new DataResponseBody<>(httpCameraServerService.saveOrUpdate(server, dto.getAuthToken()));
    }

    @ApiOperation(value = "删除HTTP摄像头服务器")
    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> delete(@PathVariable Long id) {
        return new DataResponseBody<>(httpCameraServerService.softDelete(id));
    }

    @ApiOperation(value = "获取指定服务器下的摄像机实体列表")
    @GetMapping("/{id}/cameras")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<HttpCamera>> listCameras(@PathVariable Long id) {
        return new DataResponseBody<>(httpCameraService.listByServerId(id));
    }

    @ApiOperation(value = "同步指定服务器的摄像机列表")
    @PostMapping("/{id}/cameras/sync")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<HttpCameraSyncResult> syncCameras(@PathVariable Long id) {
        HttpCameraServer server = httpCameraServerService.getById(id);
        if (server == null || Integer.valueOf(1).equals(server.getIsDelete())) {
            throw new IllegalArgumentException("服务器不存在或已删除");
        }
        return new DataResponseBody<>(httpCameraService.syncFromServer(server));
    }

    @ApiOperation(value = "获取指定摄像头截图")
    @GetMapping(value = "/{id}/cameras/{cameraId}/snapshot", produces = MediaType.IMAGE_JPEG_VALUE)
    @PreAuthorize(Permissions.DATA)
    public byte[] snapshot(@PathVariable Long id, @PathVariable String cameraId) {
        return httpCameraServerService.captureSnapshot(id, cameraId);
    }

    private HttpCameraServer buildEntity(HttpCameraServerSaveDTO dto) {
        HttpCameraServer server = new HttpCameraServer();
        server.setId(dto.getId());
        server.setName(dto.getName());
        server.setServerUrl(dto.getServerUrl());
        server.setDescription(dto.getDescription());
        server.setIsDelete(dto.getIsDelete() == null ? 0 : dto.getIsDelete());
        return server;
    }

    /**
     * 新增/更新 DTO（authToken 字段前端用 RSA 公钥加密后传入）
     */
    public static class HttpCameraServerSaveDTO {
        private Long id;
        private String name;
        private String serverUrl;
        /**
         * 访问令牌（明文，前端用 RSA 公钥加密后传入）
         * 编辑时为空则保持原 Token
         */
        private String authToken;
        private String description;
        private Integer isDelete;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getServerUrl() { return serverUrl; }
        public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getIsDelete() { return isDelete; }
        public void setIsDelete(Integer isDelete) { this.isDelete = isDelete; }
    }
}
