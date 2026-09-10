package org.dlut.adv.mineai.packagem.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
@RequestMapping("/applicationManager")
public class TestController {

    @GetMapping("/test")
    public String testEndpoint() {
        try {
            // 执行Linux命令
            String command = "ls -l";
            Process process = Runtime.getRuntime().exec(command);

            // 读取命令输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 等待命令执行完成
            int exitCode = process.waitFor();

            // 根据退出码判断是否执行成功
            if (exitCode == 0) {
                return "Command executed successfully:\n" + output.toString();
            } else {
                return "Command execution failed with exit code " + exitCode;
            }
        } catch (IOException | InterruptedException e) {
            return "Error executing command: " + e.getMessage();
        }
    }
}
