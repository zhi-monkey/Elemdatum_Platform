package org.dlut.adv.mineai.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestInfoContext {
    private String ipAddress;        // 请求IP
    private String requestUri;       // 请求URI
    private String requestMethod;    // 请求方法
}
