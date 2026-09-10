package org.dlut.adv.mineai.core.entity;

public class RequestInfoContextHolder {

    // 使用 ThreadLocal 来存储每个线程独立的 RequestInfoContext
    private static final ThreadLocal<RequestInfoContext> requestInfoContextThreadLocal = new ThreadLocal<>();

    // 设置 RequestInfoContext
    public static void setRequestInfoContext(RequestInfoContext requestInfoContext) {
        requestInfoContextThreadLocal.set(requestInfoContext);
    }

    // 获取当前线程的 RequestInfoContext
    public static RequestInfoContext getRequestInfoContext() {
        return requestInfoContextThreadLocal.get();
    }

    // 清理 ThreadLocal 中的内容
    public static void clear() {
        requestInfoContextThreadLocal.remove();
    }
}

