package org.dlut.adv.mineai.model.controller;

import org.dlut.adv.mineai.model.domain.dto.FileChunkDTO;
import org.dlut.adv.mineai.model.domain.dto.FileChunkResultDTO;
import org.dlut.adv.mineai.model.response.RestApiResponse;
import org.dlut.adv.mineai.model.service.ModelUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("upload")
public class ModelUploaderController {

    @Autowired
    private ModelUploadService modelUploadService;

    /**
     * 检查分片是否存在
     *
     * @return
     */
    @GetMapping("chunk")
    public RestApiResponse<Object> checkChunkExist(FileChunkDTO chunkDTO) {
        FileChunkResultDTO fileChunkCheckDTO;
        try {
            fileChunkCheckDTO = modelUploadService.checkChunkExist(chunkDTO);
            return RestApiResponse.success(fileChunkCheckDTO);
        } catch (Exception e) {
            return RestApiResponse.error(e.getMessage());
        }
    }


    /**
     * 上传文件分片
     *
     * @param chunkDTO
     * @return
     */
    @PostMapping("chunk")
    public RestApiResponse<Object> uploadChunk(FileChunkDTO chunkDTO) {
        try {
            modelUploadService.uploadChunk(chunkDTO);
            return RestApiResponse.success(chunkDTO.getIdentifier());
        } catch (Exception e) {
            return RestApiResponse.error(e.getMessage());
        }
    }

    /**
     * 请求合并文件分片
     *
     * @param chunkDTO
     * @return
     */
    @PostMapping("merge")
    public RestApiResponse<Object> mergeChunks(@RequestBody FileChunkDTO chunkDTO) {
        try {
            boolean success = modelUploadService.mergeChunk(chunkDTO.getIdentifier(), chunkDTO.getFilename(), chunkDTO.getTotalChunks(), chunkDTO.getFilePath(), chunkDTO.getZip());
            return RestApiResponse.flag(success);
        } catch (Exception e) {
            return RestApiResponse.error(e.getMessage());
        }
    }


}
