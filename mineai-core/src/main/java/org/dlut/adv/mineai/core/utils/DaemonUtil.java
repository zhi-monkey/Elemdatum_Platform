package org.dlut.adv.mineai.core.utils;

import com.alibaba.fastjson.JSONObject;
import org.dlut.adv.mineai.core.service.DaemonFeignService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URI;

/**
 * @author dingyadong
 */
@Component
public class DaemonUtil {

    @Resource
    DaemonFeignService daemonFeignService;

    /**
     * 向守护进程建立初始通信
     *
     * @param ip   守护进程ip
     * @param port 守护进程端口port
     */
    public void heartBeat(String ip, int port, String centerServerAddress) {
        daemonFeignService.heartBeat(URI.create("http://" + ip + ":" + port + "?address=" + centerServerAddress));
    }

    /**
     * 向守护进程同步它所属的相关服务算法配置
     *
     * @param ip   守护进程ip
     * @param port 守护进程端口port
     * @return 回复的json
     */
    public JSONObject configSync(String ip, int port, JSONObject json) {
        return daemonFeignService.configSync(URI.create("http://" + ip + ":" + port), json);
    }

    /**
     * 向守护进程获取负载信息
     *
     * @param ip   守护进程ip
     * @param port 守护进程端口port
     * @return 回复的json
     */
    public JSONObject getLoad(String ip, int port) {
        return daemonFeignService.getLoad(URI.create("http://" + ip + ":" + port));
    }

    /**
     * 向守护进程获取gpu信息
     *
     * @param ip   守护进程ip
     * @param port 守护进程端口port
     * @return 回复的json
     */
    public JSONObject getGpuInfo(String ip, int port) {
        return daemonFeignService.getGpuInfo(URI.create("http://" + ip + ":" + port));
    }
}
