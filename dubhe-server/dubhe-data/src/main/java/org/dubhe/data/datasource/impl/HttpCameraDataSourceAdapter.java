package org.dubhe.data.datasource.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dubhe.data.datasource.CameraInfo;
import org.dubhe.data.datasource.DataSourceHealthStatus;
import org.dubhe.data.datasource.DataSourceType;
import org.dubhe.data.datasource.IDataSource;
import org.dubhe.data.domain.entity.HttpCameraServer;
import org.dubhe.data.util.RtspPasswordCryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP 摄像头服务器数据源适配器
 * <p>
 * 通过调用甲方提供的 HTTP 接口获取摄像头列表、截图等能力。
 * <p>
 * 甲方接口约定：
 * <ul>
 *   <li>GET {serverUrl}/dataclt/rawtask/infos - 原始采集任务列表（taskid 作为 cameraId）</li>
 *   <li>GET {serverUrl}/dataclt/file/download?dir_name={taskid}&delRaw={true|false} - 下载原始图片压缩包</li>
 * </ul>
 * 所有请求携带 Authorization: Bearer {token} 请求头（如果配置了 authToken）。
 */
public class HttpCameraDataSourceAdapter implements IDataSource {

    private static final Logger log = LoggerFactory.getLogger(HttpCameraDataSourceAdapter.class);

    private static final String API_RAWTASK_INFOS = "/dataclt/rawtask/infos";
    private static final String API_FILE_DOWNLOAD = "/dataclt/file/download";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpCameraServer server;
    private final String aesKey;
    private final RestTemplate restTemplate;
    private final String downloadDirName;

    public HttpCameraDataSourceAdapter(HttpCameraServer server, String aesKey, RestTemplate restTemplate, String downloadDirName) {
        this.server = server;
        this.aesKey = aesKey;
        this.restTemplate = restTemplate;
        this.downloadDirName = downloadDirName;
    }

    @Override
    public Long getId() {
        return server.getId();
    }

    @Override
    public String getName() {
        return server.getName();
    }

    @Override
    public DataSourceType getSourceType() {
        return DataSourceType.HTTP;
    }

    @Override
    public String getDescription() {
        return server.getDescription();
    }

    @Override
    public DataSourceHealthStatus checkHealth() {
        String url = server.getServerUrl();
        if (url == null || url.trim().isEmpty()) {
            return DataSourceHealthStatus.offline("HTTP服务器地址未配置");
        }
        try {
            long start = System.currentTimeMillis();
            HttpHeaders headers = buildHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    url + API_RAWTASK_INFOS,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            long latency = System.currentTimeMillis() - start;
            if (response.getStatusCode().is2xxSuccessful()) {
                return DataSourceHealthStatus.online(latency);
            } else {
                return DataSourceHealthStatus.offline("HTTP响应异常: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("HTTP摄像头服务器健康检测失败: id={}, url={}, error={}", server.getId(), url, e.getMessage());
            return DataSourceHealthStatus.offline("连接失败: " + e.getMessage());
        }
    }

    @Override
    public List<CameraInfo> listCameras() {
        String url = server.getServerUrl();
        if (url == null || url.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            HttpHeaders headers = buildHeaders();
            ResponseEntity<String> response = restTemplate.exchange(
                    url + API_RAWTASK_INFOS,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("获取摄像头列表失败: id={}, status={}", server.getId(), response.getStatusCode());
                return Collections.emptyList();
            }
            Map<String, Object> body = OBJECT_MAPPER.readValue(
                    response.getBody(), new TypeReference<Map<String, Object>>() {}
            );
            Object data = body.get("data");
            if (!(data instanceof List)) {
                return Collections.emptyList();
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) data;
            List<CameraInfo> result = new ArrayList<>();
            for (Map<String, Object> raw : rawList) {
                String taskId = stringValue(raw.get("taskid"));
                if (taskId.isEmpty()) {
                    continue;
                }
                // 提取状态字段
                boolean deleted = parseBoolean(raw.get("isdeleted"));
                boolean running = parseBoolean(raw.get("running"));
                boolean cltstart = parseBoolean(raw.get("cltstart"));
                // 构建 CameraInfo 对象
                CameraInfo info = new CameraInfo();
                info.setCameraId(taskId);
                info.setName(stringValue(raw.get("devName")));
                info.setStreamUrl(stringValue(raw.get("input_streamurl")));
                info.setSourceId(server.getId());
                info.setSourceType(DataSourceType.HTTP);
                info.setDescription("running=" + running + ",cltstart=" + cltstart + ",isdeleted=" + deleted
                        + ",cltnum=" + stringValue(raw.get("cltnum")));
                if (deleted) {
                    info.setStatus("DELETED");
                } else if (running && cltstart) {
                    info.setStatus("ONLINE");
                } else {
                    info.setStatus("INACTIVE");
                }
                result.add(info);
            }
            return result;
        } catch (Exception e) {
            log.error("获取HTTP摄像头列表异常: id={}, error={}", server.getId(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public byte[] captureSnapshot(String cameraId) {
        List<byte[]> snapshots = downloadSnapshots(cameraId, false);
        return snapshots.isEmpty() ? null : snapshots.get(0);
    }

    public List<byte[]> downloadSnapshots(String cameraId, boolean delRaw) {
        if (cameraId == null || cameraId.trim().isEmpty()) {
            log.warn("downloadSnapshots: cameraId 为空. serverId={}", server.getId());
            return Collections.emptyList();
        }
        String url = server.getServerUrl();
        if (url == null || url.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            HttpHeaders headers = buildHeaders();
            String downloadDirName = buildDownloadDirName(cameraId);
            String snapshotUrl = UriComponentsBuilder.fromHttpUrl(url + API_FILE_DOWNLOAD)
                    .queryParam("dir_name", downloadDirName)
                    .queryParam("delRaw", delRaw)
                    .toUriString();
            // 发起 HTTP GET 请求下载图片压缩包，携带认证头并接收二进制响应
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    snapshotUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<byte[]> images = extractImagesFromZip(response.getBody());
                log.info("HTTP拉图完成: serverId={}, streamId={}, dirName={}, zipBytes={}, imageCount={}",
                        server.getId(), cameraId, downloadDirName, response.getBody().length, images.size());
                return images;
            }
            log.warn("下载图片压缩包失败: serverId={}, cameraId={}, status={}", server.getId(), cameraId, response.getStatusCode());
            return Collections.emptyList();
        } catch (HttpClientErrorException e) {
            log.warn("下载图片压缩包返回业务异常: serverId={}, cameraId={}, status={}, body={}",
                    server.getId(), cameraId, e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("下载图片压缩包异常: serverId={}, cameraId={}, error={}", server.getId(), cameraId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private String buildDownloadDirName(String cameraId) {
        String cameraIdValue = cameraId.trim();
        if (downloadDirName == null || downloadDirName.trim().isEmpty()) {
            return cameraIdValue;
        }
        return downloadDirName.trim().replace("{cameraId}", cameraIdValue);
    }

    @Override
    public String getStreamUrl(String cameraId) {
        if (cameraId == null || cameraId.trim().isEmpty()) {
            return null;
        }
        String url = server.getServerUrl();
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        try {
            HttpHeaders headers = buildHeaders();
            String streamUrlApi = url + API_RAWTASK_INFOS;
            ResponseEntity<String> response = restTemplate.exchange(
                    streamUrlApi,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = OBJECT_MAPPER.readValue(
                        response.getBody().trim(), new TypeReference<Map<String, Object>>() {}
                );
                Object data = body.get("data");
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> rawList = (List<Map<String, Object>>) data;
                    for (Map<String, Object> raw : rawList) {
                        if (cameraId.equals(stringValue(raw.get("taskid")))) {
                            return stringValue(raw.get("input_streamurl"));
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("获取流地址异常: serverId={}, cameraId={}, error={}", server.getId(), cameraId, e.getMessage(), e);
            return null;
        }
    }

    private List<byte[]> extractImagesFromZip(byte[] zipBytes) {
        if (zipBytes == null || zipBytes.length == 0) {
            return Collections.emptyList();
        }
        List<byte[]> images = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName() == null ? "" : entry.getName().toLowerCase();
                if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp"))) {
                    continue;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                byte[] imageBytes = baos.toByteArray();
                if (imageBytes.length > 0) {
                    log.info("解压图片: serverId={}, entry={}, bytes={}", server.getId(), entry.getName(), imageBytes.length);
                    images.add(imageBytes);
                }
            }
        } catch (Exception e) {
            log.error("解压图片压缩包失败: serverId={}, error={}", server.getId(), e.getMessage(), e);
        }
        return images;
    }

    private boolean parseBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * 构建 HTTP 请求头（携带认证 Token）
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (server.getAuthToken() != null && !server.getAuthToken().trim().isEmpty()) {
            try {
                String plainToken = RtspPasswordCryptoUtil.decrypt(server.getAuthToken(), aesKey);
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + plainToken);
            } catch (Exception e) {
                log.warn("Token解密失败，使用密文: serverId={}", server.getId());
                headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + server.getAuthToken());
            }
        }
        return headers;
    }

    /** 获取原始 HttpCameraServer 实体 */
    public HttpCameraServer getServer() {
        return server;
    }
}
