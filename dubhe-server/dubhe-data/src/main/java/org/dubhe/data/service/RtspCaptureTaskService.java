package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.entity.RtspCaptureTask;

public interface RtspCaptureTaskService extends IService<RtspCaptureTask> {

    Page<RtspCaptureTask> page(Page<RtspCaptureTask> page, RtspCaptureTask query);

    RtspCaptureTask detail(Long id);

    /**
     * 创建回流任务
     * 若 datasetGroupId 为空但 datasetGroupName 不为空，则自动创建数据集组并回填 ID
     *
     * @param task               任务实体（包含 datasetGroupId 或 datasetGroupName）
     * @param datasetGroupName   新建数据集组时使用的名称（task.datasetGroupId 为空时生效）
     */
    boolean create(RtspCaptureTask task, String datasetGroupName);

    boolean update(Long id, RtspCaptureTask task);

    boolean delete(Long id);

    /**
     * 开始采集：在数据集组下自动创建数据集，并生成执行记录
     */
    boolean startCapture(Long id);

    boolean startCapture(Long id, boolean delRaw);
}
