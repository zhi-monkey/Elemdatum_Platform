package org.dlut.adv.mineai.model.openfeign;

import org.dlut.adv.mineai.core.entity.Msg;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * @author mingming
 * @date 2024/09/29
 */
@Component
@FeignClient(value = "mineai-package-manager")
public interface PackageFeign {

    @RequestMapping(value = "package/startPacking", method = RequestMethod.GET)
    String startPacking(@RequestParam("appZipPath") String appZipPath, @RequestParam("authCode") String authCode);

    @PostMapping("package/alterAndPack")
    String startAlterAndPack(
            @RequestParam("appZipPath") String appZipPath,
            @RequestParam("alterFilePath") String alterFilePath,
            @RequestParam("localFileName") String localFileName,
            @RequestParam("classesUrl") String classesUrl,
            @RequestParam("authCode") String authCode
    );

    @GetMapping("package/checkTaskStatus")
    Msg<String> checkTaskStatus(@RequestParam("taskId") String taskId);

    @GetMapping("package/getTaskDetail")
    Msg<Object> getTaskDetail(@RequestParam("taskId") String taskId);

    @GetMapping(value = "package/getPackedFile")
    byte[] getPackedFile(@RequestParam("taskId") String taskId);
}
