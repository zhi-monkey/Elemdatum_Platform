package org.dubhe.data.service.impl;

import org.dubhe.data.dao.PcDatasetMapper;
import org.dubhe.data.domain.entity.PcDataset;
import org.dubhe.data.service.PcDatasetUploadStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PcDatasetUploadStatusServiceImpl implements PcDatasetUploadStatusService {
    private final PcDatasetMapper datasetMapper;

    public PcDatasetUploadStatusServiceImpl(PcDatasetMapper datasetMapper) {
        this.datasetMapper = datasetMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long datasetId, String error) {
        datasetMapper.updateById(new PcDataset().setId(datasetId)
                .setStatus(1004).setUploadStatus("FAILED").setUploadError(error));
    }
}
