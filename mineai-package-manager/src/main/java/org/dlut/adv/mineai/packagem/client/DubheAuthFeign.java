package org.dlut.adv.mineai.packagem.client;

import org.dlut.adv.mineai.core.entity.UserContext;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(value = "dubhe-auth")
public interface DubheAuthFeign {

    @GetMapping("/oauth/user")
    UserContext getCurUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String token);
}
