package org.dubhe.data.util;

import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * 文件计数工具类
 */
@Component
public class FileCountUtil {

    private static final String ANNO_FILE_COUNT = "annoFileCount.text";

    /**
     * 从文件中读取指定数据集和版本的图片数量
     *
     * @param basePath 基础路径
     * @param datasetId 数据集ID
     * @param versionName 版本名称
     * @return 图片数量
     */
    public Integer readImageCountFromFile(String basePath, Long datasetId, String versionName) {
        try {
            // 构造文件路径 Z:/dubhe-storage/cz-dev/dataset/957/versionFile/V0003/annotation/annoFileCount.text
            String filePath = Paths.get(basePath, datasetId.toString(), "versionFile", versionName, "annotation", ANNO_FILE_COUNT)
                    .toString();

            File file = new File(filePath);
            if (!file.exists()) {
                LogUtil.warn(LogEnum.BIZ_DATASET, "Count file not found: {}", filePath);
                return 0;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return Integer.parseInt(line.trim());
                }
            }
        } catch (IOException | NumberFormatException e) {
            LogUtil.error(LogEnum.BIZ_DATASET, "Failed to read image count from file for datasetId: {}, version: {}",
                    datasetId, versionName, e);
        }

        return 0;
    }
}
