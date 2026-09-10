/**
 * Copyright 2020 Tianshu AI Platform. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================
 */
package org.dubhe.auth.config;

import org.dubhe.cloud.authconfig.factory.PasswordEncoderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.WebResponseExceptionTranslator;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;

import javax.sql.DataSource;

/**
 * @description 授权配置
 * @date 2020-11-05
 */
@EnableAuthorizationServer
@Configuration
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JdbcTokenStore tokenStore;

    @Autowired
    private JwtAccessTokenConverter accessTokenConverter;

    @Autowired
    @Qualifier("customerOauthWebResponseExceptionTranslator")
    private WebResponseExceptionTranslator webResponseExceptionTranslator;

    @Value("${dubhe.cluster.user}")
    private String clusterUser;

    @Primary
    @Bean
    public AuthorizationServerTokenServices tokenServices() {
        // 使用自定义的SSOTokenServices替代DefaultTokenServices
        SSOTokenServices tokenServices = new SSOTokenServices();

        // 保持原有配置不变
        tokenServices.setAccessTokenValiditySeconds(-1);
        tokenServices.setRefreshTokenValiditySeconds(-1);
        tokenServices.setSupportRefreshToken(true);
        tokenServices.setReuseRefreshToken(false);
        tokenServices.setTokenStore(tokenStore);  // 关键：维持原有的tokenStore
        tokenServices.setClusterUser(clusterUser);
        return tokenServices;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients
                .jdbc(dataSource)
                .passwordEncoder(PasswordEncoderFactory.getPasswordEncoder())
        ;
    }


    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
//        endpoints
//                .tokenStore(tokenStore)
//                .accessTokenConverter(accessTokenConverter)
//                .authenticationManager(authenticationManager)
//                // 刷新token必须设置userDetailsService
//                .userDetailsService(userDetailsService)
//                .exceptionTranslator(webResponseExceptionTranslator)//认证异常处理器
//                // 重用刷新token
//                .reuseRefreshTokens(true);
        endpoints.accessTokenConverter(accessTokenConverter)
                .authenticationManager(authenticationManager)
                // 刷新token必须设置userDetailsService
                .userDetailsService(userDetailsService)
                .exceptionTranslator(webResponseExceptionTranslator)//认证异常处理器
                .tokenServices(tokenServices());
    }


    /**
     * 允许所有人请求令牌
     * 已验证的可客户端才能请求check_token端点
     *
     * @param security
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) {
        security
                .passwordEncoder(PasswordEncoderFactory.getPasswordEncoder())
                .tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()")
                .allowFormAuthenticationForClients();
    }
}
