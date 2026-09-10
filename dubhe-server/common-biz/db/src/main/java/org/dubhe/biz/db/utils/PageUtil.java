

package org.dubhe.biz.db.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.*;
import java.util.function.Function;

/**
 * @description  分页工具
 * @date 2020-03-13
 */
public class PageUtil extends cn.hutool.core.util.PageUtil {

    /**
     * List 分页
     */
    public static List toPage(int page, int size, List list) {
        int fromIndex = page * size;
        int toIndex = page * size + size;
        if (fromIndex > list.size()) {
            return new ArrayList();
        } else if (toIndex >= list.size()) {
            return list.subList(fromIndex, list.size());
        } else {
            return list.subList(fromIndex, toIndex);
        }
    }

    /**
     * Page 数据处理，预防redis反序列化报错
     */
    public static Map<String, Object> toPage(IPage page) {
        return toPage(page, page.getRecords());
    }

    /**
     * 自定义分页
     */
    public static Map<String, Object> toPage(IPage page, Function<? super List, ? extends List> function) {
        return toPage(page, function.apply(page.getRecords()));
    }

    /**
     * 自定义分页
     */
    public static Map<String, Object> toPage(IPage page, Collection data) {
        Map<String, Object> map = new LinkedHashMap<>(2);
        map.put("result", data);
        map.put("page", buildPagination(page));
        return map;
    }

    private static Map<String, Object> buildPagination(IPage page) {
        Map<String, Object> map = new HashMap<>(2);
        map.put("current", page.getCurrent());
        map.put("size", page.getSize());
        map.put("total", page.getTotal());
        return map;
    }

}
