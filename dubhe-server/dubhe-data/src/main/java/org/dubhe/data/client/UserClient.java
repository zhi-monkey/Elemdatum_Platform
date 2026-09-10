package org.dubhe.data.client;

import org.dubhe.biz.base.constant.ApplicationNameConst;
import org.dubhe.data.client.fallback.UserClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(value = ApplicationNameConst.SERVER_ADMIN, contextId = "userClient", fallback = UserClientFallback.class)
public interface UserClient {

    @GetMapping("/users/findNameById")
    String getNameById(@RequestParam(value = "userId") Long userId);

}
