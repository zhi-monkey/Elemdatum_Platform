package org.dubhe.data.client.fallback;

import org.dubhe.data.client.UserClient;

public class UserClientFallback implements UserClient {
    @Override
    public String getNameById(Long userId) {
        return "feign error";
    }
}
