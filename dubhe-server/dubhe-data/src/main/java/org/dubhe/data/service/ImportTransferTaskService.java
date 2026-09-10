package org.dubhe.data.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.dubhe.data.domain.dto.ImportTransferTaskCreateDTO;
import org.dubhe.data.domain.dto.ImportTransferTaskProgressDTO;
import org.dubhe.data.domain.vo.ImportTransferTaskVO;

import java.util.List;

public interface ImportTransferTaskService {
    Long create(ImportTransferTaskCreateDTO dto);

    IPage<ImportTransferTaskVO> list(long current, long size, String status, String datasetType);

    ImportTransferTaskVO get(Long id);

    void progress(Long id, ImportTransferTaskProgressDTO dto);

    void cancel(Long id);

    void retry(Long id);

    void delete(List<Long> ids);

    void complete(Long id, String message);

    void fail(Long id, String message);
}
