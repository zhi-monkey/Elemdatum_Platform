package org.dubhe.data.service;

public interface PcDatasetUploadStatusService {
    void markFailed(Long datasetId, String error);
}
