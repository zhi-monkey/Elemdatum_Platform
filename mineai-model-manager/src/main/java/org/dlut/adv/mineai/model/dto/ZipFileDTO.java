package org.dlut.adv.mineai.model.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Setter
@Getter
@Builder
public class ZipFileDTO {
    /**
     * 每个文件的文件名称
     */
    private List<String> fileNms;
    /**
     * 每个文件的流
     */
    private List<ByteArrayOutputStream> streams;
    /**
     * 定义的压缩文件的名称
     */
    private String zipFileNm;
}


