package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.entity.RtspCaptureExecution;

public interface RtspCaptureExecutionService extends IService<RtspCaptureExecution> {

    Page<RtspCaptureExecution> page(Page<RtspCaptureExecution> page, RtspCaptureExecution query);

    RtspCaptureExecution detail(Long id);

    boolean create(RtspCaptureExecution execution);

    boolean update(Long id, RtspCaptureExecution execution);

    /**
     * 删除执行记录（仅软删除记录本身，不删除数据集）
     */
    boolean delete(Long id);

    /**
     * 删除执行记录，并同时删除关联的数据集
     */
    boolean deleteWithDataset(Long id);

    /**
     * 手动取消采集子任务（仅排队中/运行中可取消）
     */
    boolean cancel(Long id);
}
