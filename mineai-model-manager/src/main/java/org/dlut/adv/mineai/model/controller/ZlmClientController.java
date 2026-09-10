package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.service.ZLMediaKitClient;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.ZlmRtspPullRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@RequestMapping("/zlm")
@Api(tags = "ZLMediaKit联调测试接口")
public class ZlmClientController {

    @Resource
    private ZLMediaKitClient zlMediaKitClient;

    @GetMapping("/testConnection")
    @ApiOperation("测试后端与ZLM连通性")
    public Msg<JSONObject> testConnection() {
        try {
            JSONObject result = zlMediaKitClient.testConnection();
            if (isSuccess(result)) {
                return new Msg<>(MsgCode.SUCCEED, result);
            }
            return new Msg<>(MsgCode.FAILED, result);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED, buildError("test connection failed", e));
        }
    }

    @PostMapping("/pullRtsp")
    @ApiOperation("通知ZLM拉取RTSP流")
    public Msg<JSONObject> pullRtsp(@RequestBody ZlmRtspPullRequest request) {
        if (request == null || !StringUtils.hasText(request.getRtspUrl()) || !StringUtils.hasText(request.getStream())) {
            JSONObject error = new JSONObject();
            error.put("msg", "rtspUrl and stream are required");
            return new Msg<>(MsgCode.FAILED, error);
        }
        try {
            boolean closeOld = request.getCloseOld() == null || request.getCloseOld();
            JSONObject result = zlMediaKitClient.addRtspProxy(request.getRtspUrl(), request.getStream(), request.getApp(), closeOld);
            if (isSuccess(result)) {
                return new Msg<>(MsgCode.SUCCEED, result);
            }
            return new Msg<>(MsgCode.FAILED, result);
        } catch (Exception e) {
            return new Msg<>(MsgCode.FAILED, buildError("pull rtsp failed", e));
        }
    }

    @GetMapping("/snapshot")
    @ApiOperation("从ZLM截取图片")
    public ResponseEntity<byte[]> snapshot(@RequestParam("url") String url,
                                           @RequestParam(value = "timeoutSec", defaultValue = "10") int timeoutSec,
                                           @RequestParam(value = "expireSec", defaultValue = "30") int expireSec) {
        if (!StringUtils.hasText(url)) {
            return ResponseEntity.badRequest().body("url is required".getBytes());
        }
        try {
            byte[] image = zlMediaKitClient.getSnap(url, timeoutSec, expireSec);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(image, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("snapshot failed: " + e.getMessage()).getBytes());
        }
    }

    private boolean isSuccess(JSONObject result) {
        if (result == null || !result.containsKey("code")) {
            return false;
        }
        String code = result.getString("code");
        return "0".equals(code);
    }

    private JSONObject buildError(String message, Exception e) {
        JSONObject error = new JSONObject();
        error.put("msg", message);
        error.put("error", e.getMessage());
        return error;
    }
}
