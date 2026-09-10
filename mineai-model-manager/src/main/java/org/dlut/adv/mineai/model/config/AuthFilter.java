package org.dlut.adv.mineai.model.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.model.client.DubheAuthFeign;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    private DubheAuthFeign dubheAuthFeign;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/selfIteration/callback/")
                || "/auditLog/save".equals(uri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 从请求头中获取 token
        String token = request.getHeader("Authorization");

        // 如果 token 为空，直接返回错误信息
        if (token == null || token.isEmpty()) {
            // 设置响应的 code 和 message
            Msg<String> msg = new Msg<>();
            msg.setCode("403"); // Unauthorized
            msg.setText("Token is missing");
            msg.setPayload(null);

            // 将 Msg 对象转换为 JSON 并写入响应
            response.setContentType("application/json");
            response.getWriter().write(new ObjectMapper().writeValueAsString(msg));

            return;  // 不继续执行后续的过滤链
        }

        // 调用 dubhe-auth 服务获取用户信息
        UserContext userContext = null;
        try {
            userContext = dubheAuthFeign.getCurUser(token);
            //如果是无效token，也直接返回
            if(userContext.getUsername() == null){
                // 设置响应的 code 和 message
                Msg<String> msg = new Msg<>();
                msg.setCode("403"); // Unauthorized
                msg.setText("Token is missing");
                msg.setPayload(null);

                // 将 Msg 对象转换为 JSON 并写入响应
                response.setContentType("application/json");
                response.getWriter().write(new ObjectMapper().writeValueAsString(msg));
                return;  // 不继续执行后续的过滤链
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        // 将用户信息放入 UserContextHolder
        UserContextHolder.setUserContext(userContext);
        UserContextHolder.setUserToken(token);

        // 获取用户访问的 IP 地址
        String ipAddress = request.getRemoteAddr();
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getHeader("X-Forwarded-For");
        }

        // 获取请求路径
        String requestUri = request.getRequestURI();

        // 获取请求方法
        String requestMethod = request.getMethod();
        // 设置请求信息上下文
        RequestInfoContext requestInfoContext = new RequestInfoContext();
        requestInfoContext.setIpAddress(ipAddress);
        requestInfoContext.setRequestUri(requestUri);
        requestInfoContext.setRequestMethod(requestMethod);
        RequestInfoContextHolder.setRequestInfoContext(requestInfoContext);

        try {
            // 继续处理请求
            filterChain.doFilter(request, response);
        } finally {
            // 清理 ThreadLocal
            UserContextHolder.clear();
            RequestInfoContextHolder.clear();
        }
    }
}

