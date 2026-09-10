package org.dubhe.data.constant;


/**
 * @description 数据集多人标注状态
 * @date 2020-07-01
 */
public enum DatasetModuleEnum {

    /**
     * 未进行多人标注
     */
    NO_TEAM(0, "未启动"),
    /**
     * 已进行多人标注
     */
    TEAM(1, "已启动");


    private Integer status;
    private String name;
    DatasetModuleEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }

    public Integer getStatus() {
        return status;
    }

    public String getName() {
        return name;
    }


}
