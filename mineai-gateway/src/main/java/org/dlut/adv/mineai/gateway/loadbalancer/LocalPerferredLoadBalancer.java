package org.dlut.adv.mineai.gateway.loadbalancer;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.commons.util.InetUtilsProperties;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

@Slf4j
public class LocalPerferredLoadBalancer {
    private final DiscoveryClient discoveryClient;

    private final InetUtils inetUtils;

    public LocalPerferredLoadBalancer(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;

        InetUtilsProperties inetUtilsProperties = new InetUtilsProperties();
        inetUtilsProperties.setPreferredNetworks(Collections.singletonList("10.240.0"));
        this.inetUtils = new InetUtils(inetUtilsProperties);
    }

    /**
     * 根据serviceId 筛选可用服务
     *
     * @param serviceId 服务ID
     * @param request   当前请求
     */
    public ServiceInstance choose(String serviceId, ServerHttpRequest request) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        InetAddress host = inetUtils.findFirstNonLoopbackAddress();
        if (host == null) {
            return instances.get(RandomUtil.randomInt(instances.size()));
        }
        //注册中心无实例 抛出异常
        if (CollUtil.isEmpty(instances)) {
            log.warn("No instance available for {}", serviceId);
            throw new NotFoundException("No instance available for " + serviceId);
        }

        String resourceIp = host.getHostAddress();
        List<ServiceInstance> targetList = Lists.newArrayList();
        for (ServiceInstance instance : instances) {
            if (resourceIp.equals(instance.getHost())) {
                targetList.add(instance);
            }
        }
        if (CollectionUtil.isEmpty(targetList)) {
            return instances.get(RandomUtil.randomInt(instances.size()));
        }

        return targetList.get(RandomUtil.randomInt(targetList.size()));
    }
}

