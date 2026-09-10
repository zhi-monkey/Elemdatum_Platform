package org.dubhe.data.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.dto
 * @Project：mineai
 * @name：MP4RecordDTO
 * @Date：2023/12/17 10:16
 * @Filename：MP4RecordDTO
 */

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(value = "Record mp4 dto", description = "zl录制的视频文件")
public class MP4RecordDTO implements Serializable {

    private final static long serialVersionUID = 1L;

    private String mediaServerId;

    private String app;

    @JsonProperty(value = "file_name")
    private String fileName;

    @JsonProperty(value = "file_path")
    private String filePath;

    @JsonProperty(value = "file_size")
    private Integer fileSize;

    private String folder;

    @JsonProperty(value = "start_time")
    private Integer startTime;

    private String stream;

    @JsonProperty(value = "time_len")
    private Float timeLen;

    private String url;

    private String vhost;


}
