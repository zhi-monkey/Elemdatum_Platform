package org.dlut.adv.mineai.model.utils;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.springframework.beans.factory.annotation.Value;

import java.io.FileReader;

/**
 * @author oyjp
 * @create 2023/9/25 21:03
 */
public class K8sUtils {
    private static ApiClient apiClient;

    @Value("${kubernetes.config}")
    private static String k8sConfig;

    static {
        try {
            String k8sConfig = K8sUtils.class.getResource("/k8s.cfg").getPath();
            System.out.println("k8sconfig-path:"+k8sConfig);
            apiClient = ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(k8sConfig))).build();
        } catch (Exception e) {
            System.out.println("构建K8s-Client异常" + e);
        }
    }

    public static ApiClient getApiClient() {
        return apiClient;
    }
}
