package org.dlut.adv.mineai.core.constant;

public class AuditLogConstants {
    // 请求状态常量
    public static final class RequestStatus {
        private RequestStatus() {}

        /** 请求正常 */
        public static final Integer NORMAL = 0;
        /** 请求异常 */
        public static final Integer EXCEPTION = 1;
    }

    // 操作类型常量
    public static final class OperationType {
        private OperationType() {}

        /** 新增 */
        public static final int ADD = 0;
        /** 删除 */
        public static final int DELETE = 1;
        /** 修改 */
        public static final int UPDATE = 2;
        /** 查询 */
        public static final int QUERY = 3;
        public static final int DOWNLOAD = 4;
    }

}


