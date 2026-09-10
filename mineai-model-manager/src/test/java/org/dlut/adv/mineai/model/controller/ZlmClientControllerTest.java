package org.dlut.adv.mineai.model.controller;

import com.alibaba.fastjson.JSONObject;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.core.service.ZLMediaKitClient;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.dto.ZlmRtspPullRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZlmClientControllerTest {

    @Mock
    private ZLMediaKitClient zlMediaKitClient;

    @InjectMocks
    private ZlmClientController zlmClientController;

    @Test
    void testConnectionShouldReturnSuccessWhenZlmCodeIsZero() {
        JSONObject payload = new JSONObject();
        payload.put("code", 0);
        when(zlMediaKitClient.testConnection()).thenReturn(payload);

        Msg<JSONObject> result = zlmClientController.testConnection();

        Assertions.assertEquals(MsgCode.SUCCEED.getCode(), result.getCode());
        Assertions.assertEquals("0", result.getPayload().getString("code"));
    }

    @Test
    void pullRtspShouldReturnFailedWhenRequestInvalid() {
        ZlmRtspPullRequest request = new ZlmRtspPullRequest();

        Msg<JSONObject> result = zlmClientController.pullRtsp(request);

        Assertions.assertEquals(MsgCode.FAILED.getCode(), result.getCode());
        Assertions.assertTrue(result.getPayload().getString("msg").contains("required"));
    }

    @Test
    void pullRtspShouldReturnSuccessWhenZlmCodeIsZero() {
        ZlmRtspPullRequest request = new ZlmRtspPullRequest();
        request.setRtspUrl("rtsp://example/live/stream");
        request.setStream("stream_1");
        request.setApp("monitor");

        JSONObject payload = new JSONObject();
        payload.put("code", "0");
        payload.put("msg", "success");
        when(zlMediaKitClient.addRtspProxy(eq("rtsp://example/live/stream"), eq("stream_1"), eq("monitor"), anyBoolean()))
                .thenReturn(payload);

        Msg<JSONObject> result = zlmClientController.pullRtsp(request);

        Assertions.assertEquals(MsgCode.SUCCEED.getCode(), result.getCode());
    }

    @Test
    void testConnectionShouldReturnFailedWhenClientThrows() {
        when(zlMediaKitClient.testConnection()).thenThrow(new RuntimeException("connect refused"));

        Msg<JSONObject> result = zlmClientController.testConnection();

        Assertions.assertEquals(MsgCode.FAILED.getCode(), result.getCode());
        Assertions.assertTrue(result.getPayload().getString("msg").contains("test connection failed"));
    }

    @Test
    void snapshotShouldReturnImageBytesWhenSuccess() throws IOException {
        byte[] image = new byte[]{1, 2, 3};
        when(zlMediaKitClient.getSnap("rtsp://example/live", 10, 30)).thenReturn(image);

        ResponseEntity<byte[]> response = zlmClientController.snapshot("rtsp://example/live", 10, 30);

        Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
        Assertions.assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        Assertions.assertArrayEquals(image, response.getBody());
    }

    @Test
    void snapshotShouldReturnInternalServerErrorWhenClientThrows() throws IOException {
        when(zlMediaKitClient.getSnap("rtsp://example/live", 10, 30)).thenThrow(new IOException("timeout"));

        ResponseEntity<byte[]> response = zlmClientController.snapshot("rtsp://example/live", 10, 30);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Assertions.assertEquals(MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
    }
}
