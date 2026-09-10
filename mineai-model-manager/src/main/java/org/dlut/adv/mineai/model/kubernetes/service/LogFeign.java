package org.dlut.adv.mineai.model.kubernetes.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(value = "loki", url = "${loki.ip}:${loki.port}/loki/api/v1/")
public interface LogFeign {

    @GetMapping("/query_range")
    JSONObject queryLog(@RequestParam String query,@RequestParam String direction,@RequestParam(required = false) Long limit,@RequestParam(required = false) Long start, @RequestParam(required = false) Long end);
}
