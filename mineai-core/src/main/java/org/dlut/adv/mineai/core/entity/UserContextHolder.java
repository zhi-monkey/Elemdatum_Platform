package org.dlut.adv.mineai.core.entity;

public class UserContextHolder {
    private static final ThreadLocal<UserContext> userContextThreadLocal = new ThreadLocal<>();

    private static final ThreadLocal<String> userTokenThreadLocal = new ThreadLocal<>();

    public static void setUserContext(UserContext userContext) {
        userContextThreadLocal.set(userContext);
    }

    public static UserContext getUserContext() {
        return userContextThreadLocal.get();
    }

    public static void clear() {
        userContextThreadLocal.remove();
        userTokenThreadLocal.remove();
    }

    public static void setUserToken(String userToken) {
        userTokenThreadLocal.set(userToken);
    }

    public static String getUserToken() {
        return userTokenThreadLocal.get();
    }
}

