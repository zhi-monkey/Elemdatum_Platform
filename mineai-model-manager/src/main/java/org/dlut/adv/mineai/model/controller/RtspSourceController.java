package org.dlut.adv.mineai.model.controller;

import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.entity.RtspSource;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.service.RtspSourceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rtspSource")
public class RtspSourceController {

    @Value("${rsa.public_key:}")
    private String rsaPublicKey;

    @Resource
    private RtspSourceService rtspSourceService;

    @GetMapping("/findAll")
    public Msg<List<RtspSource>> findAll() {
        return new Msg<>(MsgCode.SUCCEED, rtspSourceService.findAllAvailable());
    }

    @GetMapping("/getPublicKey")
    public Msg<String> getPublicKey() {
        return new Msg<>(MsgCode.SUCCEED, rsaPublicKey);
    }

    @GetMapping("/dynamicFindRtspSourcePage")
    public Msg<Page<RtspSource>> dynamicFindRtspSourcePage(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "desc") String order,
                                                           @RequestParam(defaultValue = "15") int pageSize,
                                                           @RequestParam(required = false) String name,
                                                           @RequestParam(required = false) String rtspUrl,
                                                           @RequestParam(required = false) String username,
                                                           @RequestParam(required = false) String description) {
        String end = "end";
        if (order.contains(end)) {
            order = order.substring(0, order.indexOf(end));
        }
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.fromString(order), "id");
        Page<RtspSource> queryPage = rtspSourceService.dynamicFindRtspSourcePage(pageable, name, rtspUrl, username, description);
        List<RtspSource> list = new ArrayList<>(queryPage.getContent());
        return new Msg<>(MsgCode.SUCCEED, new PageImpl<>(list, pageable, queryPage.getTotalElements()));
    }

    @PostMapping("/saveOrUpdate")
    public Msg<RtspSource> saveOrUpdate(@RequestBody RtspSourceSaveDTO dto) {
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
        return new Msg<>(MsgCode.SUCCEED, rtspSourceService.saveOrUpdate(rtspSource, dto.getPassword()));
    }

    @DeleteMapping("/delete/{id}")
    public Msg<Void> delete(@PathVariable Long id) {
        rtspSourceService.deleteById(id);
        return new Msg<>(MsgCode.SUCCEED);
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
