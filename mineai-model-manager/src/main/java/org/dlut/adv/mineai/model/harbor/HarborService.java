package org.dlut.adv.mineai.model.harbor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ContainerConfig;
import com.spotify.docker.client.exceptions.DockerException;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HarborService {

    @Resource
    DockerClient dockerClient;
    @Resource
    com.spotify.docker.client.DockerClient client;

    @Value("${harbor-server.url}")
    String harborUrl;
    @Value("${harbor-server.project}")
    String harborProject;

    @Value("${allowed_hyperParams}")
    String allowedHyperParams;

    public Map<String, String> uploadImage(MultipartFile file, String trgImageName, String tag) throws IOException {
        InputStream inputStream = file.getInputStream();
        return uploadHarbor(inputStream, trgImageName, tag);
    }

    private Map<String, String> uploadHarbor(InputStream inputStream, String trgImageName, String tag) {
        try {
            Set<String> imageSet = client.load(inputStream);
            Map<String, String> envMap = null;
            for (String realImageName : imageSet) {
                String imagesName = harborUrl + "/" + harborProject + "/" + trgImageName;
                String userImageAddr = imagesName + ":" + tag;
                // tag   docker tag : 标记本地镜像，将其归入某一仓库
                dockerClient.tagImageCmd(realImageName, imagesName, tag).exec();
                ContainerConfig config = dockerClient.inspectImageCmd(realImageName).exec().getConfig();
                if (config == null) {
                    throw new BusinessException("config找不到");
                }
                String[] envs = config.getEnv();
                if (envs != null) {
                    envMap = Arrays.stream(envs).filter(this::isHyperParam).map(env -> env.split("=")).collect(Collectors.toMap(e -> e[0], e -> e[1]));
                }
                if (envMap == null) {
                    throw new BusinessException("获取超参数失败");
                }
                // push   docker push : 将本地的镜像上传到镜像仓库
                dockerClient.pushImageCmd(userImageAddr).start().awaitCompletion();
                // 删除镜像
                dockerClient.removeImageCmd(realImageName).exec();
                dockerClient.removeImageCmd(userImageAddr).exec();
                break;
            }
            log.info("上传成功");
            return envMap;
        } catch (DockerException e) {
            System.out.println(e);
            return null;
        } catch (Exception e) {
            System.out.println("文件上传失败:" + e);
            return null;
        }
    }

    public Map<String, String> urlUploadImage(String srcImageName, String trgImageName, String tag) {
        Map<String, String> result = new HashMap<>();
        String targetImage = harborUrl + "/" + harborProject + "/" + trgImageName + ":" + tag;

        try {
            // 拉取源镜像
            dockerClient.pullImageCmd(srcImageName).start().awaitCompletion();

            // 打标签
            dockerClient.tagImageCmd(srcImageName, targetImage,  tag).exec();

            // 推送镜像
            dockerClient.pushImageCmd(targetImage).start().awaitCompletion();

            log.info("镜像上传成功");
        } catch (RuntimeException e) {
            log.error("镜像上传失败: {}", e.getMessage());
            result.put("ERROR_MESSAGE", e.getMessage());
        } catch (InterruptedException e) {
            log.error("镜像上传失败: {}", e.getMessage());
            Thread.currentThread().interrupt();
            result.put("ERROR_MESSAGE", MsgCode.CONNECT_TO_HARBOR_TIMEOUT.getText());
        } catch (Exception e) {
            log.error("镜像上传失败: {}", e.getMessage());
            result.put("ERROR_MESSAGE", MsgCode.UN_FILED_ERROR.getText());
        } finally {
            // 清理本地镜像
            try {
                dockerClient.removeImageCmd(srcImageName).withForce(true).exec();
            } catch (Exception ignored) {}
            try {
                dockerClient.removeImageCmd(targetImage).withForce(true).exec();
            } catch (Exception ignored) {}
        }
        return result;
    }

    public Map<String, String> getImageEnvFromSource(String srcImageName) {
        Map<String, String> envMap = new HashMap<>();
        try {
            // 拉取源镜像
            log.info("正在尝试拉取镜像: {}", srcImageName);
            dockerClient.pullImageCmd(srcImageName).start().awaitCompletion();
            log.info("镜像拉取成功: {}", srcImageName);

            // 获取环境变量
            String[] envs = dockerClient.inspectImageCmd(srcImageName).exec().getConfig().getEnv();
            if (envs == null || envs.length == 0) {
                log.warn("镜像 '{}' 成功拉取，但未找到任何环境变量。", srcImageName);
                envMap.put("ERROR_MESSAGE", MsgCode.FAILED_TO_GET_IMAGE_ENV.getText());
                return envMap;
            }

            log.info("成功获取镜像 '{}' 的环境变量。", srcImageName);
            return Arrays.stream(envs)
                    .map(env -> env.split("=", 2))
                    .collect(Collectors.toMap(
                            arr -> arr[0],
                            arr -> arr.length > 1 ? arr[1] : "",
                            (existingValue, newValue) -> newValue
                    ));
        } catch (NotFoundException e) {
            log.error("获取镜像环境失败：无法找到或访问镜像 '{}'。", srcImageName, e);
            envMap.put("ERROR_MESSAGE", MsgCode.IMAGE_NOT_FOUND.getText());
        } catch (InternalServerErrorException e) {
            String errorMessage = e.getMessage();
            // 尝试从错误消息中提取 JSON 部分
            try {
                // 提取 JSON 部分（在大括号内的内容）
                int jsonStart = errorMessage.indexOf("{");
                int jsonEnd = errorMessage.lastIndexOf("}");
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonStr = errorMessage.substring(jsonStart, jsonEnd + 1);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode = mapper.readTree(jsonStr);

                    String detailMessage = jsonNode.path("message").asText();
                    if (detailMessage != null && detailMessage.toLowerCase().contains("not found")) {
                        log.error("获取镜像环境失败：镜像 '{}' 不存在。", srcImageName, e);
                        envMap.put("ERROR_MESSAGE", MsgCode.IMAGE_NOT_FOUND.getText());
                    } else {
                        log.error("Docker registry内部错误 for image '{}': {}", srcImageName, detailMessage, e);
                        envMap.put("ERROR_MESSAGE", MsgCode.UN_FILED_ERROR.getText());
                    }
                } else {
                    // 如果没有找到 JSON，回退到字符串匹配
                    handleFallbackErrorCheck(errorMessage, srcImageName, envMap, e);
                }
            } catch (Exception jsonParseException) {
                // JSON 解析失败，回退到字符串匹配
                log.warn("无法解析错误消息的JSON格式，使用回退方案");
                handleFallbackErrorCheck(errorMessage, srcImageName, envMap, e);
            }
        } catch (RuntimeException e) {
            log.error("镜像上传时出错: {}", e.getMessage());
            if (MsgCode.FAILED_TO_GET_IMAGE_ENV.getText().equals(e.getMessage())) {
                envMap.put("ERROR_MESSAGE", MsgCode.FAILED_TO_GET_IMAGE_ENV.getText());
            }else {
                envMap.put("ERROR_MESSAGE", MsgCode.UN_FILED_ERROR.getText());
            }
        } catch (InterruptedException e) {
            log.error("镜像操作被中断 (可能超时): {}", e.getMessage());
            Thread.currentThread().interrupt();
            envMap.put("ERROR_MESSAGE", MsgCode.CONNECT_TO_HARBOR_TIMEOUT.getText());
        } catch (Exception e) {
            log.error("获取镜像环境时发生未知错误 for image '{}': {}", srcImageName, e.getMessage(), e);
            envMap.put("ERROR_MESSAGE", MsgCode.UN_FILED_ERROR.getText());
        }
        return envMap;
    }
    private void handleFallbackErrorCheck(String errorMessage, String srcImageName,
                                          Map<String, String> envMap, Exception e) {
        if (errorMessage != null && errorMessage.toLowerCase().contains("not found")) {
            log.error("获取镜像环境失败：镜像 '{}' 不存在。", srcImageName, e);
            envMap.put("ERROR_MESSAGE", MsgCode.IMAGE_NOT_FOUND.getText());
        } else {
            log.error("Docker registry内部错误 for image '{}': {}", srcImageName, errorMessage, e);
            envMap.put("ERROR_MESSAGE", MsgCode.UN_FILED_ERROR.getText());
        }
    }

    private boolean isHyperParam(String str) {
        String[] hyperParams = allowedHyperParams.split("\\|");
        System.out.println();
        for (String param : hyperParams) {
            if (str.contains(param)) {
                return true;
            }
        }
        return false;
    }
}
