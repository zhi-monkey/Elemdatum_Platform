package org.dlut.adv.mineai.core.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

/**
 * 主要不要和功能模块命名冲突
 * 注入失败请使用 @EnableFeignClients 添加扫描路径
 *
 * @author dean
 */
@Component
@FeignClient(value = "zlm-server", url = "${zlm.address}", contextId = "MediaServerFeignService")
public interface MediaServerFeignService {

    /**
     * 获取媒体信息 推拉流/mp4录制
     *
     * @param data 请求参数
     * @return 服务器返回数据
     */
    @RequestMapping("/getMediaList")
    String getMediaList(@RequestBody Map<String, Object> data);

    /**
     * 关闭流
     *
     * @param data 请求参数
     * @return 服务器返回数据
     */
    @RequestMapping("/close_streams")
    String closeStreams(@RequestBody Map<String, Object> data);

    /**
     * 添加流代理 只推rtmp
     *
     * @param data 请求参数
     * @return 服务器返回数据
     */
    @RequestMapping("/addStreamProxy")
    String addStreamProxy(@RequestBody Map<String, Object> data);

    /**
     * 开始录制  指定落盘位置 切片时间
     *
     * @param data 请求参数
     * @return 服务器返回数据
     */
    @RequestMapping("/startRecord")
    String startRecord(@RequestBody Map<String, Object> data);

    /**
     * 停止录制
     *
     * @param data 请求参数
     * @return 服务器返回数据
     */
    @RequestMapping("/stopRecord")
    String stopRecord(@RequestBody Map<String, Object> data);

    /**
     * 停止流代理
     *
     * @param data 请求参数
     * @return 服务器返回数据
     */
    @RequestMapping("/delStreamProxy")
    String delStreamProxy(@RequestBody Map<String, Object> data);
}
