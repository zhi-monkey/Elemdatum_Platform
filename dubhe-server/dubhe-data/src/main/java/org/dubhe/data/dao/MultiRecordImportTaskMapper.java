package org.dubhe.data.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.dubhe.data.domain.entity.MultiRecordImportTask;

import java.sql.Timestamp;

public interface MultiRecordImportTaskMapper extends BaseMapper<MultiRecordImportTask> {
    @Update("UPDATE multi_import_task SET status = 'RUNNING', worker_id = #{workerId}, " +
            "attempt_count = attempt_count + 1, heartbeat_at = #{now}, started_at = COALESCE(started_at, #{now}), " +
            "error_message = NULL WHERE id = #{taskId} AND deleted = 0 AND status IN ('QUEUED', 'RETRY')")
    int claim(@Param("taskId") Long taskId, @Param("workerId") String workerId, @Param("now") Timestamp now);

    @Update("UPDATE multi_import_task SET heartbeat_at = #{now}, scanned_messages = GREATEST(scanned_messages, #{scannedMessages}), " +
            "processed_images = GREATEST(processed_images, #{processedImages}), " +
            "processed_pointclouds = GREATEST(processed_pointclouds, #{processedPointclouds}) " +
            "WHERE id = #{taskId} AND worker_id = #{workerId} AND status = 'RUNNING' AND deleted = 0")
    int heartbeat(@Param("taskId") Long taskId, @Param("workerId") String workerId,
                  @Param("scannedMessages") Long scannedMessages, @Param("processedImages") Long processedImages,
                  @Param("processedPointclouds") Long processedPointclouds, @Param("now") Timestamp now);

    @Update("UPDATE multi_import_task SET status = 'RETRY', worker_id = NULL, error_message = 'Worker heartbeat timed out' " +
            "WHERE status = 'RUNNING' AND deleted = 0 AND heartbeat_at < #{deadline}")
    int retryStale(@Param("deadline") Timestamp deadline);
}
