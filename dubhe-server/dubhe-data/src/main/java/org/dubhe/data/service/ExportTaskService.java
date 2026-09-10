package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dubhe.data.domain.dto.ExportTaskCreateDTO;
import org.dubhe.data.domain.vo.ExportTaskVO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @description 数据导出任务服务接口
 * @date 2026-08-29
 */
public interface ExportTaskService {

    /**
     * 创建导出任务（等待确认）
     *
     * @param dto 导出任务入参
     * @return 任务号 taskId
     */
    String createTask(ExportTaskCreateDTO dto);

    /**
     * 分页查询导出任务列表
     *
     * @param current 当前页
     * @param size    每页条数
     * @return 分页结果
     */
    IPage<ExportTaskVO> listTasks(long current, long size);

    /**
     * 查询单个任务
     *
     * @param taskId 任务号
     * @return 任务结果
     */
    ExportTaskVO getTask(String taskId);

    /**
     * 确认导出任务（触发导出执行）
     *
     * @param taskId 任务号
     */
    void confirm(String taskId);

    /**
     * 取消导出任务
     *
     * @param taskId 任务号
     */
    void cancel(String taskId);

    /**
     * 批量删除导出任务
     *
     * @param ids 任务ID列表
     */
    void deleteTasks(List<Long> ids);

    /**
     * 下载导出文件（实时生成，不落 MinIO，流式返回）
     *
     * @param taskId   任务号
     * @param response 响应对象
     */
    void download(String taskId, HttpServletResponse response) throws Exception;
}
