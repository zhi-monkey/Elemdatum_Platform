package org.dlut.adv.mineai.model.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.GpuUrlTarget;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SelfIterationGpuDispatchService {

    private final MultipartUploader multipartUploader;

    public SelfIterationGpuDispatchService() {
        this(new RestTemplateMultipartUploader());
    }

    SelfIterationGpuDispatchService(MultipartUploader multipartUploader) {
        this.multipartUploader = multipartUploader;
    }

    public DispatchResult dispatchToTargets(byte[] appZipBytes, List<GpuUrlTarget> targets) {
        if (appZipBytes == null || appZipBytes.length == 0) {
            throw new IllegalArgumentException("appZip 二进制内容为空");
        }

        List<GpuUrlTarget> normalizedTargets = normalizeTargets(targets);
        DispatchResult result = new DispatchResult();
        result.setTargetCount(normalizedTargets.size());
        result.setBinaryBytes(appZipBytes.length);

        for (GpuUrlTarget target : normalizedTargets) {
            String uploadUrl = buildUploadUrl(target);
            String gpuUrl = target.getGpuUrl().trim();
            DispatchTargetResult targetResult = new DispatchTargetResult();
            targetResult.setTargetId(target.getId());
            targetResult.setUploadUrl(uploadUrl);
            targetResult.setGpuUrl(gpuUrl);
            try {
                DispatchHttpResponse response = multipartUploader.upload(uploadUrl, buildRequest(appZipBytes, gpuUrl));
                targetResult.setHttpStatus(response.getStatusCode());
                targetResult.setResponseBody(response.getBody());
                targetResult.setSuccess(response.getStatusCode() >= 200 && response.getStatusCode() < 300);
                if (targetResult.isSuccess()) {
                    log.info("自迭代模型包下发成功：targetId={}, platform={}:{}, status={}, response={}",
                            target.getId(), target.getPlatformIp(), target.getPlatformPort(),
                            response.getStatusCode(), safeResponseBody(response.getBody()));
                } else {
                    targetResult.setMessage("平台返回非2xx状态码");
                    log.warn("自迭代模型包下发返回非2xx：targetId={}, platform={}:{}, status={}, response={}",
                            target.getId(), target.getPlatformIp(), target.getPlatformPort(),
                            response.getStatusCode(), safeResponseBody(response.getBody()));
                }
            } catch (Exception e) {
                targetResult.setSuccess(false);
                targetResult.setMessage(e.getMessage());
                log.error("自迭代模型包下发失败：targetId={}, platform={}:{}, bytes={}",
                        target.getId(), target.getPlatformIp(), target.getPlatformPort(), appZipBytes.length, e);
            }
            result.getTargets().add(targetResult);
        }

        result.setSuccess(result.getTargets().stream().allMatch(DispatchTargetResult::isSuccess));
        return result;
    }

    private String safeResponseBody(String body) {
        if (body == null) {
            return "";
        }
        String normalized = body.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) + "..." : normalized;
    }

    private String buildUploadUrl(GpuUrlTarget target) {
        String platformIp = target.getPlatformIp() == null ? "" : target.getPlatformIp().trim();
        Integer platformPort = target.getPlatformPort();
        if (platformIp.isEmpty()) {
            throw new IllegalArgumentException("平台IP不能为空");
        }
        if (platformPort == null || platformPort <= 0 || platformPort > 65535) {
            throw new IllegalArgumentException("平台端口不合法");
        }
        return "http://" + platformIp + ":" + platformPort + "/api/sys-ai-dev/uploadAppZip";
    }

    private HttpEntity<MultiValueMap<String, Object>> buildRequest(byte[] appZipBytes, String gpuUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentDispositionFormData("appZip", "app.zip");
        fileHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<NamedByteArrayResource> fileEntity = new HttpEntity<>(
                new NamedByteArrayResource(appZipBytes, "app.zip"),
                fileHeaders
        );

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("appZip", fileEntity);
        body.add("gpuUrl", gpuUrl);
        return new HttpEntity<>(body, headers);
    }

    private List<GpuUrlTarget> normalizeTargets(List<GpuUrlTarget> targets) {
        List<GpuUrlTarget> result = new ArrayList<>();
        Map<String, Boolean> seen = new LinkedHashMap<>();
        if (targets == null) {
            return result;
        }
        for (GpuUrlTarget target : targets) {
            if (target == null || target.getGpuUrl() == null) {
                continue;
            }
            String gpuUrl = target.getGpuUrl().trim();
            if (gpuUrl.isEmpty()) {
                continue;
            }
            validateGpuUrlHasExplicitPort(gpuUrl);
            String key = target.getPlatformIp() + ":" + target.getPlatformPort() + "|" + gpuUrl;
            if (!seen.containsKey(key)) {
                seen.put(key, true);
                result.add(target);
            }
        }
        return result;
    }

    private void validateGpuUrlHasExplicitPort(String gpuUrl) {
        URI uri = URI.create(gpuUrl);
        if (uri.getHost() == null || uri.getHost().trim().isEmpty() || uri.getPort() <= 0) {
            throw new IllegalArgumentException("GPU服务URL必须显式包含主机和端口");
        }
    }

    public interface MultipartUploader {
        DispatchHttpResponse upload(String uploadUrl, HttpEntity<MultiValueMap<String, Object>> requestEntity);
    }

    private static class RestTemplateMultipartUploader implements MultipartUploader {
        private final RestTemplate restTemplate = new RestTemplate();

        @Override
        public DispatchHttpResponse upload(String uploadUrl, HttpEntity<MultiValueMap<String, Object>> requestEntity) {
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, requestEntity, String.class);
            DispatchHttpResponse result = new DispatchHttpResponse();
            result.setStatusCode(response.getStatusCodeValue());
            result.setBody(response.getBody());
            return result;
        }
    }

    @Data
    public static class DispatchHttpResponse {
        private int statusCode;
        private String body;
    }

    static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    @Data
    public static class DispatchResult {
        private boolean success;
        private int targetCount;
        private int binaryBytes;
        private List<DispatchTargetResult> targets = new ArrayList<>();
    }

    @Data
    public static class DispatchTargetResult {
        private Long targetId;
        private String uploadUrl;
        private String gpuUrl;
        private Integer httpStatus;
        private String responseBody;
        private boolean success;
        private String message;
    }
}
