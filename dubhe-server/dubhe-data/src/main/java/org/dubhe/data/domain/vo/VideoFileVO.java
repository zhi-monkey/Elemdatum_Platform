package org.dubhe.data.domain.vo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
public class VideoFileVO {

    private Long id;

    private String name;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private String url;

    //文件大小
    private String fileSize;

    //文件格式
    private String fileType;

}