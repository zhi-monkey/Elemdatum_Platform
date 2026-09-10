package org.dlut.adv.mineai.packagem.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.packagem.audit.PackageAuditLogHelper;
import org.dlut.adv.mineai.packagem.api.MsgCode;
import org.dlut.adv.mineai.packagem.entity.PackedFileResult;
import org.dlut.adv.mineai.packagem.entity.Task;
import org.dlut.adv.mineai.packagem.service.TaskService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * @author mingming
 * @date 2024/09/29
 */
@Slf4j
@RestController
@RequestMapping("/package")
@RequiredArgsConstructor
public class PackingController {
    private final TaskService taskService;
    private final PackageAuditLogHelper packageAuditLogHelper;
    @GetMapping("/startPacking")
    public String startPacking(@RequestParam("appZipPath") String appZipPath, @RequestParam("authCode") String authCode) {
        return taskService.startPackaging(appZipPath, authCode);
    }

    @PostMapping("/alterAndPack")
    public String alterAndPack(
            @RequestParam("appZipPath") String appZipPath,
            @RequestParam("alterFilePath") String alterFilePath,
            @RequestParam("localFileName") String localFileName,
            @RequestParam("classesUrl") String classesUrl,
            @RequestParam("authCode") String authCode
    ) {
        return taskService.startAlterPackaging(appZipPath, alterFilePath, localFileName, classesUrl, authCode);
    }

    @GetMapping("/checkTaskStatus")
    public Msg<String> checkTaskStatus(@RequestParam("taskId") String taskId) {
        return new Msg<>(MsgCode.SUCCEED, taskService.checkTaskStatus(taskId));
    }

    @GetMapping("/getTaskDetail")
    public Msg<Task> getTaskDetail(@RequestParam("taskId") String taskId) {
        Task task = taskService.getTaskDetail(taskId);
        if (task == null) {
            Msg<Task> msg = new Msg<>(MsgCode.FAILED);
            msg.setText("任务不存在");
            return msg;
        }
        return new Msg<>(MsgCode.SUCCEED, task);
    }

    @GetMapping("/getPackedFile")
    public ResponseEntity<byte[]> getPackedFile(@RequestParam("taskId") String taskId, HttpServletRequest request) {
        PackedFileResult result = taskService.getPackedFile(taskId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename(result.getFileName(), StandardCharsets.UTF_8)
                        .build()
        );

        packageAuditLogHelper.saveDownloadAuditLog("package_download", request);
        return new ResponseEntity<>(result.getData(), headers, HttpStatus.OK);
    }
}
