package org.dubhe.cloud.authconfig.online;

import org.dubhe.biz.base.context.UserContext;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 简单的在线用户注册表：记录最近一段时间内活跃的用户
 */
public class OnlineUserRegistry {

    // 仅保存必要的用户基础信息，key 为 userId，value 为精简后的 UserContext
    private static final Map<Long, UserContext> USER_INFO = new ConcurrentHashMap<>();
    // 最近活跃时间
    private static final Map<Long, Long> USER_ACTIVITY = new ConcurrentHashMap<>();
    // 首次在线时间（用于计算在线时长）
    private static final Map<Long, Long> USER_FIRST_SEEN = new ConcurrentHashMap<>();
    // 在线窗口：最近多少毫秒内有请求就认为在线，这里先用 10 分钟
    private static final long ONLINE_WINDOW_MILLIS = TimeUnit.MINUTES.toMillis(10);

    private OnlineUserRegistry() {
    }

    public static void markActive(UserContext user) {
        if (user == null || user.getId() == null) {
            return;
        }

        // 精简用户信息（只保留 id / username）
        UserContext userContext = new UserContext();
        userContext.setId(user.getId());
        userContext.setUsername(user.getUsername());

        long now = System.currentTimeMillis();
        USER_INFO.put(userContext.getId(), userContext);
        USER_ACTIVITY.put(userContext.getId(), now);
        USER_FIRST_SEEN.putIfAbsent(userContext.getId(), now);
    }

    public static long getOnlineUserCount() {
        removeOfflineUser();
        return USER_ACTIVITY.size();
    }
    public static Set<Long> getOnlineUserIds() {
        removeOfflineUser();
        return USER_ACTIVITY.keySet();
    }
    public static Set<UserContext> getOnlineUsers() {
        removeOfflineUser();
        // 兼容低版本 JDK：使用不可变副本返回
        return Collections.unmodifiableSet(new HashSet<>(USER_INFO.values()));
    }

    /**
     * 获取当前在线用户的在线时长（毫秒），key 为 userId，value 为 onlineDurationMillis
     */
    public static Map<Long, Long> getOnlineDurations() {
        removeOfflineUser();
        long now = System.currentTimeMillis();
        Map<Long, Long> durations = new ConcurrentHashMap<>();
        USER_FIRST_SEEN.forEach((id, firstSeen) -> durations.put(id, Math.max(0, now - firstSeen)));
        return durations;
    }

    private static void removeOfflineUser() {
        long now = System.currentTimeMillis();
        USER_ACTIVITY.entrySet().removeIf(e -> {
            boolean offline = now - e.getValue() > ONLINE_WINDOW_MILLIS;
            if (offline) {
                USER_INFO.remove(e.getKey());
                USER_FIRST_SEEN.remove(e.getKey());
            }
            return offline;
        });
    }
}
