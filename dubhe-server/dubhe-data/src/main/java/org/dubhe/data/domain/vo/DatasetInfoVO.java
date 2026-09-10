package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.vo
 * @Project：mineai
 * @name：DatasetInfoVO
 * @Date：2024/6/6 17:07
 * @Filename：DatasetInfoVO
 * @Desc：数据总览展示表格用
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatasetInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private Integer annotateType;

    private String currentVersionName;

    private Integer versionFileCount;

    private Timestamp createTime;
}
