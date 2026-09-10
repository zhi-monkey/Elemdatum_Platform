package org.dubhe.cloud.authconfig.filter;

import org.dubhe.biz.base.context.UserContext;
import org.dubhe.cloud.authconfig.dto.JwtUserDTO;
import org.dubhe.cloud.authconfig.online.OnlineUserRegistry;
import org.dubhe.cloud.authconfig.utils.JwtUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 记录在线用户的简单过滤器：
 * 每次请求解析当前登录用户并更新其最近活跃时间。
 */
public class OnlineUserTrackFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            JwtUserDTO jwtUserDTO = JwtUtils.getCurUser();
            if (jwtUserDTO != null) {
                UserContext user = jwtUserDTO.getUser();
                if (user != null && user.getId() != null) {
                    OnlineUserRegistry.markActive(user);
                }
            }
        } catch (Exception ignored) {
            // 解析用户信息失败时不影响正常请求
        }

        filterChain.doFilter(request, response);
    }
}
