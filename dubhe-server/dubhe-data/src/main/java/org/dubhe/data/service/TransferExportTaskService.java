package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dubhe.data.domain.entity.TransferExportTask;
import org.dubhe.data.domain.vo.TransferExportTaskVO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface TransferExportTaskService {
    Long createDatasetVersionTask(String taskName, Long datasetId, Long datasetVersionId,
                                  String versionName, String format, Integer totalFiles);
    IPage<TransferExportTaskVO> list(long current, long size, String status, String datasetType);
    TransferExportTaskVO get(Long id);
    TransferExportTask require(Long id);
    void progress(Long id, String stage, int progress, Integer totalFiles, Integer successFiles, String message);
    void complete(Long id, String resultObjectKey, String message);
    void fail(Long id, String message);
    void cancel(Long id);
    void delete(List<Long> ids);
    void download(Long id, HttpServletResponse response) throws Exception;
}
