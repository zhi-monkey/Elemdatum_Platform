package org.dubhe.data.service.task;

import org.dubhe.data.service.MultiDatasetService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MultiRecordImportTaskScheduler {
    private final MultiDatasetService multiDatasetService;

    public MultiRecordImportTaskScheduler(MultiDatasetService multiDatasetService) {
        this.multiDatasetService = multiDatasetService;
    }

    @Scheduled(fixedDelayString = "${multi.record-import.reconcile-delay-ms:60000}")
    public void reconcile() {
        multiDatasetService.reconcileImportTasks();
    }
}
