package org.dlut.adv.mineai.model.controller;
import org.dlut.adv.mineai.core.utils.FileUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author oyjp
 * @create 2023/11/9 19:07
 */
@RestController
@RequestMapping("/test")
public class TestController {


    @RequestMapping("divideFolder")
    public boolean divideFolder(double divideSize, String folderSrc, String trgA, String trgB){
        return FileUtil.folderDivide(divideSize, folderSrc, trgA, trgB);
    }
}
