package org.dubhe.task.data;

import lombok.extern.slf4j.Slf4j;
import org.dubhe.data.service.impl.DatasetServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @description 清理已删除数据集文件定时任务
 * @author mingming
 * @date 2025/08/17
 */
@Slf4j
@Component
public class DeletedDatasetFileClearTask {

    @Autowired
    private DatasetServiceImpl datasetServiceImpl;

    /**
     * 每天凌晨两点
     */
    // @Scheduled(initialDelay = 15000, fixedRate = Long.MAX_VALUE)
    // @Scheduled(cron = "1 * * * * ?")
    @Scheduled(cron = "0 0 2 * * ?")
    public void process() {
        log.info("deleted dataset file clear --- > start");
        datasetServiceImpl.deletedDatasetFileClear();
        log.info("deleted dataset file clear --- > end");
    }
}
