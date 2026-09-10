package org.dubhe.data.service;

import org.dubhe.data.domain.dto.MultiDatasetCreateDTO;
import org.dubhe.data.domain.dto.MultiRecordImportAssetsDTO;
import org.dubhe.data.domain.dto.MultiRecordImportClaimDTO;
import org.dubhe.data.domain.dto.MultiRecordImportFailDTO;
import org.dubhe.data.domain.dto.MultiRecordImportFinishDTO;
import org.dubhe.data.domain.dto.MultiRecordImportProgressDTO;
import org.dubhe.data.domain.dto.MultiRecordUploadUrlDTO;
import org.dubhe.data.domain.entity.MultiDataset;
import org.dubhe.data.domain.entity.MultiRecordImportTask;
import org.dubhe.data.domain.vo.MultiRecordUploadUrlVO;

public interface MultiDatasetService {
    MultiDataset create(MultiDatasetCreateDTO dto);

    MultiRecordUploadUrlVO createRecordUploadUrl(Long datasetId, MultiRecordUploadUrlDTO dto);

    MultiRecordImportTask commitRecord(Long datasetId, Long taskId);

    MultiRecordImportTask getImportTask(Long datasetId, Long taskId);

    MultiRecordImportTask claimImportTask(MultiRecordImportClaimDTO dto);

    void reportProgress(Long taskId, MultiRecordImportProgressDTO dto);

    void saveAssets(Long taskId, MultiRecordImportAssetsDTO dto);

    void finishImport(Long taskId, MultiRecordImportFinishDTO dto);

    void failImport(Long taskId, MultiRecordImportFailDTO dto);

    void reconcileImportTasks();
}
