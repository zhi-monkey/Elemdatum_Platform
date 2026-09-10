package org.dlut.adv.mineai.model.harbor;

import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/harbor")
public class HarborController {
    @Resource
    private HarborService harborService;
    /**
     * 本地镜像上传接口
     * */
    @RequestMapping("/imageUpload")
    public Msg<Map<String, String>> uploadImage(MultipartFile file, String imageName, String tag) {
        try {
            Map<String, String> envMap = harborService.uploadImage(file, imageName, tag);
            if (envMap == null) {
                return new Msg<>(MsgCode.FAILED);
            } else {
                return new Msg<>(MsgCode.SUCCEED, envMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return new Msg<>(MsgCode.FAILED);
        }
    }
    /**
     * Url镜像上传接口
     * */
    @RequestMapping("/urlImageUpload")
    public Msg<Map<String, String>> urlUploadImage(String srcImageName, String imageName, String tag){
        Map<String, String> result = harborService.urlUploadImage(srcImageName, imageName, tag);
        // result只用来判断是否成功上传
        if(result.isEmpty() || result.containsKey("ERROR_MESSAGE")){
            return new Msg<>(MsgCode.FAILED, result);
        } else {
            return new Msg<>(MsgCode.SUCCEED, result);
        }
    }

    @RequestMapping("/getImageEnvFromSource")
    public Msg<Map<String, String>> getImageEnvFromSource(String srcImageName) {
        Map<String, String> envMap = harborService.getImageEnvFromSource(srcImageName);
        // envMap为空表示获取环境变量失败
        if (envMap.isEmpty() || envMap.containsKey("ERROR_MESSAGE")) {
            return new Msg<>(MsgCode.FAILED);
        } else {
            return new Msg<>(MsgCode.SUCCEED, envMap);
        }
    }
}
