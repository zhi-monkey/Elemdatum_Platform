package org.dubhe.auth.config;

import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;

public class SSOTokenServices extends DefaultTokenServices {
    private TokenStore tokenStore;

    @Setter
    private String clusterUser;

    public void setTokenStore(TokenStore tokenStore) {
        super.setTokenStore(tokenStore);
        this.tokenStore = tokenStore;
    }

    @Override
    public OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) {
        //检查一下是否为服务间调用使用的cluster user
        // 检查是否有用户认证信息（排除client_credentials等模式）
        if (authentication.getUserAuthentication() != null) {
            String username = authentication.getUserAuthentication().getName();
            if(!username.equals(clusterUser)){
                // 删除该用户的所有现有令牌（强制单点登录）
                revokeExistingTokens(authentication);
            }
        }
        return super.createAccessToken(authentication);
    }

    private void revokeExistingTokens(OAuth2Authentication authentication) {
        // 再次检查，确保用户认证信息存在
        if (authentication.getUserAuthentication() == null) {
            return;
        }
        String username = authentication.getUserAuthentication().getName();
        String clientId = authentication.getOAuth2Request().getClientId();

        // 查找并删除所有关联token
        tokenStore.findTokensByClientIdAndUserName(clientId, username).forEach(token -> {
            tokenStore.removeAccessToken(token);
            if (token.getRefreshToken() != null) {
                tokenStore.removeRefreshToken(token.getRefreshToken());
            }
        });
    }
}

