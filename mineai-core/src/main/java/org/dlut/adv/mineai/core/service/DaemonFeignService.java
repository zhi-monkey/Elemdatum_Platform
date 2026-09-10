package org.dlut.adv.mineai.core.service;


import com.alibaba.fastjson.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.net.URI;

/**
 * 守护进程通信类通信时需指定ip port
 *
 * @author dingyadong
 */
@Component
@FeignClient(value = "daemon", url = "http://test.com", contextId = "DaemonFeignService")
public interface DaemonFeignService {

    /**
     * 向守护进程发送心跳检测异常
     *
     * @param uri 守护进程通信地址
     * @return 回复的json
     */
    @GetMapping("/controller/heartBeat")
    JSONObject heartBeat(URI uri);

    /**
     * 向守护进程同步它所属的相关服务模型配置
     *
     * @param uri  守护进程通信地址
     * @param json 传输的json
     * @return 回复的json
     */
    @PostMapping("/controller/config/update")
    JSONObject configSync(URI uri, JSONObject json);

    /**
     * 向守护进程获取负载
     *
     * @param uri 守护进程通信地址
     * @return 回复的json
     */
    @GetMapping("/controller/load")
    JSONObject getLoad(URI uri);

    /**
     * 向守护进程获取Gpu信息
     *
     * @param uri 守护进程通信地址
     * @return 回复的json
     */
    @GetMapping("/controller/getGpuInfo")
    JSONObject getGpuInfo(URI uri);
}
