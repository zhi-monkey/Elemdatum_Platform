package org.dlut.adv.mineai.core.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(value = "mineai-system", contextId = "FeignService")
public interface FeignService {
    @GetMapping("/login/login")
    String login(@RequestParam String username,
                 @RequestParam String password);
}
