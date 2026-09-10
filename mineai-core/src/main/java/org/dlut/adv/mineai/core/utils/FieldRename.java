package org.dlut.adv.mineai.core.utils;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lin Wang
 */
public class FieldRename {
    /**
     * 获取实体类中所有的唯一属性，除了id
     *
     * @param clazz 实体类
     * @return 唯一属性列表
     */
    public List<String> getUniqueFieldListExceptId(Class<?> clazz) {
        List<String> uniqueFieldList = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.unique() && !field.getName().equals("id")) {
                uniqueFieldList.add(field.getName());
            }
        }
        return uniqueFieldList;
    }

    /**
     * 对删除的实体重命名
     *
     * @param entityName     实体名字
     * @param entityNameList 实体名字列表
     * @return newEntityName 新实体名字
     */
    public String renameProperty(String entityName, List<String> entityNameList) {
        String newEntityName = entityName + "(已删除)";
        int i = 1;
        for (String name : entityNameList) {
            if (name.startsWith(entityName + "(已删除)")) {
                newEntityName = entityName + "(已删除)(" + i + ")";
                i++;
            }
        }
        System.out.println("---------------------------------");
        System.out.println("newEntityName = " + newEntityName);
        return newEntityName;
    }

}
