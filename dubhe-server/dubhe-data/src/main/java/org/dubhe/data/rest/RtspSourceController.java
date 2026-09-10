package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.entity.RtspSource;
import org.dubhe.data.service.RtspSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Api(tags = "数据处理：RTSP数据源")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/rtsp-sources")
public class RtspSourceController {

    @Value("${rsa.public_key:}")
    private String rsaPublicKey;

    @Autowired
    private RtspSourceService rtspSourceService;

    @ApiOperation(value = "RTSP数据源列表")
    @GetMapping("/all")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<List<RtspSource>> findAll() {
        return new DataResponseBody<>(rtspSourceService.findAllAvailable());
    }

    @ApiOperation(value = "获取RTSP密码加密公钥")
    @GetMapping("/public-key")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<String> getPublicKey() {
        return new DataResponseBody<>(rsaPublicKey);
    }

    @ApiOperation(value = "RTSP数据源分页查询")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Page<RtspSource>> page(Page<RtspSource> page,
                                                   @RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String rtspUrl,
                                                   @RequestParam(required = false) String username,
                                                   @RequestParam(required = false) String description) {
        RtspSource query = new RtspSource();
        query.setName(name);
        query.setRtspUrl(rtspUrl);
        query.setUsername(username);
        query.setDescription(description);
        return new DataResponseBody<>(rtspSourceService.page(page, query));
    }

    @ApiOperation(value = "新增RTSP数据源")
    @PostMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<RtspSource> create(@RequestBody RtspSourceSaveDTO dto) {
        RtspSource rtspSource = buildEntity(dto);
        return new DataResponseBody<>(rtspSourceService.saveOrUpdate(rtspSource, dto.getPassword()));
    }

    @ApiOperation(value = "更新RTSP数据源")
    @PutMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<RtspSource> update(@PathVariable Long id, @RequestBody RtspSourceSaveDTO dto) {
        RtspSource rtspSource = buildEntity(dto);
        rtspSource.setId(id);
        return new DataResponseBody<>(rtspSourceService.saveOrUpdate(rtspSource, dto.getPassword()));
    }

    @ApiOperation(value = "删除RTSP数据源")
    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Boolean> delete(@PathVariable Long id) {
        return new DataResponseBody<>(rtspSourceService.softDelete(id));
    }

    private RtspSource buildEntity(RtspSourceSaveDTO dto) {
        RtspSource rtspSource = new RtspSource();
        rtspSource.setId(dto.getId());
        rtspSource.setName(dto.getName());
        rtspSource.setRtspUrl(dto.getRtspUrl());
        if (Boolean.FALSE.equals(dto.getAuthRequired())) {
            rtspSource.setUsername(null);
            dto.setPassword(null);
        } else {
            rtspSource.setUsername(emptyToNull(dto.getUsername()));
        }
        rtspSource.setDescription(dto.getDescription());
        rtspSource.setIsDelete(dto.getIsDelete() == null ? 0 : dto.getIsDelete());
        return rtspSource;
    }

    private String emptyToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    public static class RtspSourceSaveDTO {
        private Long id;
        private String name;
        private String rtspUrl;
        private String username;
        private String password;
        private String description;
        private Integer isDelete;
        private Boolean authRequired;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRtspUrl() {
            return rtspUrl;
        }

        public void setRtspUrl(String rtspUrl) {
            this.rtspUrl = rtspUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getIsDelete() {
            return isDelete;
        }

        public void setIsDelete(Integer isDelete) {
            this.isDelete = isDelete;
        }

        public Boolean getAuthRequired() {
            return authRequired;
        }

        public void setAuthRequired(Boolean authRequired) {
            this.authRequired = authRequired;
        }
    }
}
