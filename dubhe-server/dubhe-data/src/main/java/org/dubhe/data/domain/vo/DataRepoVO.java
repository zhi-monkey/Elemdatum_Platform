package org.dubhe.data.domain.vo;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DataRepoVO {
    private Long id;
    private String name;
    private String remark;
    private String uri;
    private Timestamp createTime;
    private Long userId;
}