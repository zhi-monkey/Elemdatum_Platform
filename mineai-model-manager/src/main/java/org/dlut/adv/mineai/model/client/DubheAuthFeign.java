package org.dlut.adv.mineai.model.client;

import com.alibaba.fastjson.JSONObject;
import feign.Headers;
import org.dlut.adv.mineai.core.entity.UserContext;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.bind.annotation.*;

@Component
@FeignClient(value = "dubhe-auth")
public interface DubheAuthFeign {

    @Headers({"Content-Type: application/x-www-form-urlencoded;charset=UTF-8"})
    @PostMapping("/oauth/token")
    JSONObject dubheAuth(@RequestBody LinkedMultiValueMap<String, Object> params);

    @GetMapping("/oauth/user")
    UserContext getCurUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String token);

}
