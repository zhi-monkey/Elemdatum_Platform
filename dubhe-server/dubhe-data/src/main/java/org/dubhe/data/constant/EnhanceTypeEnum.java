/**
 * Copyright 2020 Tianshu AI Platform. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =============================================================
 */

package org.dubhe.data.constant;

/**
 * @description 增强算法枚举
 * @date 2020-06-30
 */
public enum EnhanceTypeEnum {

    /**
     * 去雾
     */
    DEFOG(1, "去雾"),
    /**
     * 增雾
     */
    INCREASE_FOG(2, "增雾"),
    /**
     * 对比度增强
     */
    CONTRAST_ENHANCE(3, "对比度增强"),
    /**
     * 直方图均衡化
     */
    HISTOGRAM_AVERAGE(4, "直方图均衡化"),

    /**
     * 筛选+去重
     */
    CLEAN(5, "清洗"),

    /**
     * 裁剪
     */
    CROP(6, "裁剪"),

    /**
     * 压缩
     */
    COMPRESS(7, "压缩");

    private Integer key;
    private String value;

    EnhanceTypeEnum(Integer key, String value) {
        this.key = key;
        this.value = value;
    }

}
