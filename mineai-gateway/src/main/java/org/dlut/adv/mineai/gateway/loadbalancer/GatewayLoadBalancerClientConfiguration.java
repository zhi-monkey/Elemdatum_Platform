package org.dlut.adv.mineai.gateway.loadbalancer;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.config.GatewayReactiveLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ Author: dong
 * @ Date: 2021/7/6 11:23
 * @ Description:
 */
@Configuration
//@EnableConfigurationProperties(LoadBalancerProperties.class)
@AutoConfigureBefore(GatewayReactiveLoadBalancerClientAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class GatewayLoadBalancerClientConfiguration {

    @Bean
    public LocalPerferredLoadBalancer getLocalPreferredLoadBalancer(DiscoveryClient discoveryClient) {
        return new LocalPerferredLoadBalancer(discoveryClient);
    }

    @Bean
    public ReactiveLoadBalancerClientFilter gatewayLoadBalancerClientFilter(LocalPerferredLoadBalancer localPerferredLoadBalancer, GatewayLoadBalancerProperties properties) {
        return new GatewayReactiveLoadBalancerClientFilter(properties, localPerferredLoadBalancer);
    }
}
