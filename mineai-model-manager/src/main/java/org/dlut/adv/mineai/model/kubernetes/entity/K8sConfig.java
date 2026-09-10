package org.dlut.adv.mineai.model.kubernetes.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kubernetes")
@Data
public class K8sConfig {
    private String ip;
    private Integer port;
    private Integer hamiPort;
    private String config;
    private String nameSuffix;
    private String namespace;
    private PvcConfig pvc;
    private RootPathConfig rootPath;
    private NodeSelectorConfig nodeSelector;

    @Data
    public static class PvcConfig {
        private String outputPVC;
        private String inputPVC;
    }

    @Data
    public static class RootPathConfig {
        private String weightRootPath;
        private String datasetRootPath;
    }

    @Data
    public static class NodeSelectorConfig {
        private String key;
        private String value;
    }
}