package org.dlut.adv.mineai.model.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;


/**
 * @author oyjp
 */
@RequestMapping("storage")
@RestController
public class StorageController {


    @Value("${datasetRootPath}")
    private String datasetRootPath;


    @RequestMapping(value = "getFileStreamNew", method = RequestMethod.GET)
    public boolean getFileStreamNew(String datasetName, String fileName, HttpServletResponse response) {
        //接受相对路径作为参数
        //TODO:修改为大文件分片，断点续传逻辑
        File f = new File(new File(datasetRootPath, datasetName), fileName);
        if (!f.exists()) {
            return false;
        }
        try {
            InputStream inputStream = Files.newInputStream(f.toPath());
            FileCopyUtils.copy(inputStream, response.getOutputStream());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
