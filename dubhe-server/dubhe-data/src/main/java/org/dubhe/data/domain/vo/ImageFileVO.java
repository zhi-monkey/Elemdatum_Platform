package org.dubhe.data.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class ImageFileVO {
    private Long id;

    private String name;

    private Integer status;

    private String url;

    //文件大小
    private String fileSize;

    //文件格式
    private String fileType;
}
