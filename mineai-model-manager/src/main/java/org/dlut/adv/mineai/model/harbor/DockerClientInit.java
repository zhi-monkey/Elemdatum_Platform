package org.dlut.adv.mineai.model.harbor;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.spotify.docker.client.DefaultDockerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;

@Component
public class DockerClientInit {

    @Value("${docker-server.ip}")
    String dockerIp;
    @Value("${docker-server.port}")
    String dockerPort;
    @Value("${harbor-server.url}")
    String harborUrl;
    @Value("${harbor-server.username}")
    String harborUsername;
    @Value("${harbor-server.password}")
    String harborPwd;

    @Bean("dockerClient")
    public DockerClient dockerClient() {
        //创建DefaultDockerClientConfig
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                // 服务器ip
                .withDockerHost("tcp://"+dockerIp+":"+dockerPort)
                .withDockerTlsVerify(false)
                .withRegistryUrl(harborUrl)
                .withRegistryUsername(harborUsername)
                .withRegistryPassword(harborPwd)
                .build();
        //创建DockerHttpClient
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Bean("client")
    public com.spotify.docker.client.DockerClient client() {
        return DefaultDockerClient.builder()
                .uri(URI.create("http://"+dockerIp+":"+dockerPort))
                .build();
    }
}

