package org.dlut.adv.mineai.core.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.core.entity.SubsystemMonitor;
import org.dlut.adv.mineai.core.service.MediaServerFeignService;
import org.dlut.adv.mineai.core.service.ZlmService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * 对比ZLM和数据库推拉流/是否录制落盘的信息 并进行同步
 *
 * @author dean
 */
@Component
public class SyncMediaServerUtil {
    @Resource
    MediaServerFeignService mediaServerFeignService;

    @Resource
    ZlmService zlmService;

    @Value("${zlm.secret}")
    String secret;

    @Value("${zlm.app}")
    String app;

    @Value("${zlm.host}")
    String host;

    @Value("${zlm.record.path}")
    String recordRootPath;


    /**
     * 根据MediaList的信息关闭流
     *
     * @param item getMediaList接口返回的一个data数据
     */
    private void closeStream(Object item) {
        Map<String, Object> map = new HashMap<>(5);
        map.put("secret", secret);
        map.put("vhost", JSON.parseObject(item.toString()).getString("vhost"));
        map.put("app", JSON.parseObject(item.toString()).getString("app"));
        map.put("stream", JSON.parseObject(item.toString()).getString("stream"));
        map.put("force", 1);
        System.out.println(map.get("stream") + "关闭流");
        System.out.println(mediaServerFeignService.closeStreams(map));
    }

    /**
     * 关闭流代理确保新代理能添加成功
     *
     * @param stream stream
     */
    private void delStreamProxy(String stream) {
        Map<String, Object> streamProxyMap = new HashMap<>(5);
        streamProxyMap.put("secret", secret);
        streamProxyMap.put("key", host + "/" + app + "/" + stream);
        try {
            mediaServerFeignService.delStreamProxy(streamProxyMap);
        } catch (Exception ignore) {
            System.out.println(stream + "删除流代理失败");
        }
    }

    /**
     * 添加推拉流代理
     *
     * @param originUrl 拉取流的源
     * @param stream    代理推流的stream
     */
    private void addStream(String originUrl, String stream) {
        Map<String, Object> map = new HashMap<>(10);
        map.put("secret", secret);
        map.put("vhost", host);
        map.put("app", app);
        map.put("stream", stream);
        map.put("url", originUrl);
        map.put("enable_rtmp", 1);
        try {
            String resp = mediaServerFeignService.addStreamProxy(map);
            if (!"0".equals(JSON.parseObject(resp).getString("code"))) {
                delStreamProxy(map.get("stream").toString());
            }
        } catch (Exception e) {
            System.out.println(stream + "添加流代理异常");
        }
    }

    /**
     * 改变录制状态
     *
     * @param item getMediaList接口返回的一个data数据
     * @param flag true表示开启录制 false表示停止录制
     */
    private void changeRecord(Object item, boolean flag, String customizedPath, Integer maxSecond) {
        Map<String, Object> map = new HashMap<>(7);
        map.put("secret", secret);
        map.put("type", 1);
        map.put("vhost", JSON.parseObject(item.toString()).getString("vhost"));
        map.put("app", JSON.parseObject(item.toString()).getString("app"));
        map.put("stream", JSON.parseObject(item.toString()).getString("stream"));
        //true 开始录制
        if (flag && maxSecond != null) {
            //半小时一切片
            map.put("customized_path", customizedPath);
            map.put("max_second", maxSecond);
            System.out.println(map.get("stream") + "开始录制" + customizedPath);
            System.out.println(mediaServerFeignService.startRecord(map));
        } else {
            System.out.println(map.get("stream") + "停止录制");
            System.out.println(mediaServerFeignService.stopRecord(map));
        }
    }

    /**
     * 对比需要推拉流(IsPushStreamAndStatusUsing)的信息进行同步 多了删 少了加
     * 默认服务器只会拉取rtsp推rtmp rtsp和rtmp一一对应 没有其他流
     */
    public void syncPushStream() {
        List<Monitor> monitorList = zlmService.getPushStreamList();
        List<SubsystemMonitor> subsystemMonitorList = zlmService.getSubsystemMonitorList();
        //存储所有需要推拉流的url 和 stream monitor_id作为编号
        List<String> urlList = new ArrayList<>();
        List<String> streamList = new ArrayList<>();
        for (Monitor monitor : monitorList) {
            String monitorUrl = monitor.getProtocolMetaData();
            if (StringUtils.isNotBlank(monitorUrl)) {
                urlList.add(monitorUrl);
                streamList.add("stream_" + monitor.getId());
            }
        }
        for (SubsystemMonitor subsystemMonitor : subsystemMonitorList) {
            //md5加密取前六位作为id的替代
            String monitorId = DigestUtils.md5DigestAsHex(subsystemMonitor.getMonitorName().getBytes()).substring(0, 6);
            String modelId = DigestUtils.md5DigestAsHex(subsystemMonitor.getAlgName().getBytes()).substring(0, 6);
//            //原始流 /monitor/stream_id
//            urlList.add(subsystemMonitor.getMonitorUrl());
//            streamList.add("stream_" + monitorId);
            //推理流 /monitor/model_id/monitor_id
            String streamUrl = subsystemMonitor.getStreamUrl();
            if (StringUtils.isNotBlank(streamUrl)) {
                urlList.add(streamUrl);
                streamList.add("model_" + modelId + "/monitor_" + monitorId);
            }
        }
        //从媒体服务器获取信息 rtmp协议 live
        Map<String, Object> data = new HashMap<>(5);
        data.put("secret", secret);
        data.put("app", app);
        data.put("schema", "rtmp");
        data.put("originTypeStr", "pull");
        String resp = mediaServerFeignService.getMediaList(data);
        //取data
        JSONArray zlmList = new JSONArray();
        if (JSON.parseObject(resp).getJSONArray("data") != null) {
            zlmList = JSON.parseObject(resp).getJSONArray("data");
        }
        //取originUrl以及对应的 vhost app stream  以stream和originUrl作为对比
        for (Object item : zlmList) {
            int index = streamList.indexOf(JSON.parseObject(item.toString()).getString("stream"));
            //如果数据库中没有这个流就直接关闭这个流的所有协议
            if (index != -1 && urlList.get(index).equals(JSON.parseObject(item.toString()).getString("originUrl"))) {
                //originUrl stream都匹配说明是需要的流可以从数据库List移除 最后剩余的数据库List就是新增的
                streamList.remove(index);
                urlList.remove(index);
            } else {
                //1. stream匹配但是originUrl不匹配 重新拉取 先关闭流再新增 不删除数据库List即可
                //2. 完全不匹配 关闭流即可
                this.closeStream(item);
            }
        }
        //新增流(包含重新拉取的)   stream：stream + Monitor_id
        for (int i = 0; i < urlList.size(); i++) {
            String stream = streamList.get(i);
            String url = urlList.get(i);
            new Thread(() -> delStreamProxy(stream)).start();
            new Thread(() -> addStream(url, stream)).start();
        }
        System.out.println("推拉流同步完成: " + new Date());
    }

    /**
     * 对比需要落盘的信息进行同步 多了停止录制，少了开始录制
     * 落盘位置为根目录 文件夹名为关联的dataset的名称 由于多对多的映射关系 暂时默认路径保存一份数据
     */
    public void syncRecord() {
        //所有绑定了数据集的monitor及其dataSet信息
        List<Monitor> monitorList = zlmService.getRecordStreamList();
        Map<String, Long> streamLists = new HashMap<>(monitorList.size());
        for (Monitor monitor : monitorList) {
            String stream = "stream_" + monitor.getId();
            streamLists.put(stream, monitor.getDatasetId());
        }
        //从媒体服务器获取流是否录制的相关信息
        Map<String, Object> data = new HashMap<>(2);
        data.put("secret", secret);
        data.put("schema", "rtmp");
        String resp = mediaServerFeignService.getMediaList(data);
        //取data
        JSONArray zlmList = new JSONArray();
        if (JSON.parseObject(resp).getJSONArray("data") != null) {
            zlmList = JSON.parseObject(resp).getJSONArray("data");
        }
        //取出所有推拉流的vhost app stream originUrl isRecordingMP4
        for (Object item : zlmList) {
            //不需要落盘的流可以停止录制
            if (!streamLists.containsKey(JSON.parseObject(item.toString()).getString("stream"))) {
                if ("true".equals(JSON.parseObject(item.toString()).getString("isRecordingMP4"))) {
                    new Thread(() -> changeRecord(item, false, "", null)).start();
                }
            } else {
                //过滤已经在录制的流
                if ("true".equals(JSON.parseObject(item.toString()).getString("isRecordingMP4"))) {
                    continue;
                }
                //开始录制为/opt/media/bin/www/record/:datasetId/video/:videoName.mp4
                String customizedPath = recordRootPath + streamLists.get(JSON.parseObject(item.toString()).getString("stream")) + "/video";
                Monitor monitor = zlmService.findMonitorById(Long.parseLong(JSON.parseObject(item.toString()).getString("stream").split("_")[1]));
                new Thread(() -> changeRecord(item, true, customizedPath, monitor.getMaxSecond())).start();
            }
        }
        System.out.println("落盘同步完成: " + new Date());
    }
}
