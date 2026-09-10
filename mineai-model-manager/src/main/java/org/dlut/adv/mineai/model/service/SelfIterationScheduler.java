package org.dlut.adv.mineai.model.service;

import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.SelfIterationJob;
import org.dlut.adv.mineai.core.entity.SelfIterationTask;
import org.dlut.adv.mineai.model.repository.SelfIterationJobRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 自迭代训练定时任务调度器
 * <p>定期扫描进行中的任务，兜底推进状态，防止因回调丢失导致流程卡住</p>
 */
@Component
@Slf4j
public class SelfIterationScheduler {

    @Autowired
    private SelfIterationJobRepo jobRepo;

    @Autowired
    private SelfIterationService selfIterationService;

    /**
     * 每30秒扫描一次进行中的任务，兜底推进状态
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void scanAndProgressStuckJobs() {
        try {
            List<SelfIterationJob> runningJobs = jobRepo.findByStatusIn(Arrays.asList(
                    SelfIterationJob.STATUS_COLLECTING,
                    SelfIterationJob.STATUS_AUTO_LABELING,
                    SelfIterationJob.STATUS_TRAINING,
                    SelfIterationJob.STATUS_CONVERTING,
                    SelfIterationJob.STATUS_PACKAGING
            ));

            if (runningJobs.isEmpty()) {
                return;
            }

            // 过滤：父任务必须是 RUNNING 状态
            List<SelfIterationJob> validJobs = runningJobs.stream()
                    .filter(job -> {
                        SelfIterationTask task = job.getSelfIterationTask();
                        return task != null && task.getStatus() == SelfIterationTask.STATUS_RUNNING;
                    })
                    .collect(Collectors.toList());

            if (validJobs.isEmpty()) {
                return;
            }

            log.debug("定时扫描：发现 {} 个进行中的任务", validJobs.size());

            for (SelfIterationJob job : validJobs) {
                try {
                    selfIterationService.refreshModelIterationProgress(job.getId());
                } catch (Exception e) {
                    log.warn("定时扫描推进失败：jobId={}, status={}", job.getId(), job.getStatus(), e);
                }
            }
        } catch (Exception e) {
            log.error("定时扫描任务执行失败", e);
        }
    }
}
