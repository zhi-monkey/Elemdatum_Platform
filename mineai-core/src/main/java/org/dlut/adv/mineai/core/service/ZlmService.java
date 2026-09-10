package org.dlut.adv.mineai.core.service;

import org.dlut.adv.mineai.core.entity.Monitor;
import org.dlut.adv.mineai.core.entity.SubsystemMonitor;
import org.dlut.adv.mineai.core.repository.ZlmRepo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 注意不要和功能模块命名冲突
 *
 * @author dean
 */
@Service
public class ZlmService {
    @Resource
    ZlmRepo zlmRepo;

    public List<Monitor> getPushStreamList() {
        return zlmRepo.findMonitorByIsPushStreamAndStatusUsing(Monitor.PUSH_STREAM, Monitor.ON);
    }

    public List<SubsystemMonitor> getSubsystemMonitorList() {
        return zlmRepo.getSubsystemMonitors();
    }

    public List<Monitor> getRecordStreamList() {
        return zlmRepo.findMonitorByStatusUsingAndDatasetIdIsNotNullAndIsRecord(Monitor.ON, Monitor.RECORD);
    }

    public Monitor findMonitorById(long id) {
        return zlmRepo.findMonitorById(id);
    }
}