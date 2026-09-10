

package org.dubhe.biz.base.functional;

/**
 * @description 字符串格式化函数式接口
 * @date 2021-02-03
 */
@FunctionalInterface
public interface StringFormat {
    /**
     * 格式化对象为字符串
     * @param value 被格式对象
     * @return
     */
    String format(Object value);
}
