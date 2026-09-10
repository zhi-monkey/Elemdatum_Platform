package org.dubhe.data.service.task;

import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.dao.DatasetDatasetGroupMapper;
import org.dubhe.data.dao.DatasetGroupMapper;
import org.dubhe.data.domain.entity.DatasetGroup;
import org.dubhe.data.service.impl.DatasetVersionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatasetAnnotatedImageCountScheduler {

    private static final Logger log = LoggerFactory.getLogger(DatasetAnnotatedImageCountScheduler.class);

    private final DatasetGroupMapper datasetGroupMapper;
    private final DatasetDatasetGroupMapper datasetDatasetGroupMapper;
    private final DatasetVersionServiceImpl datasetVersionServiceImpl;
    private final RedisUtils redisUtils;

    public DatasetAnnotatedImageCountScheduler(DatasetGroupMapper datasetGroupMapper,
                                              DatasetDatasetGroupMapper datasetDatasetGroupMapper,
                                              DatasetVersionServiceImpl datasetVersionServiceImpl,
                                              RedisUtils redisUtils) {
        this.datasetGroupMapper = datasetGroupMapper;
        this.datasetDatasetGroupMapper = datasetDatasetGroupMapper;
        this.datasetVersionServiceImpl = datasetVersionServiceImpl;
        this.redisUtils = redisUtils;
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void refreshAnnotatedImageCount() {
        long total = 0L;
        try {
            List<DatasetGroup> groups = datasetGroupMapper.getAllDatasetGroup();
            if (groups == null || groups.isEmpty()) {
                redisUtils.set(Constant.DATASET_ANNOTATED_IMAGE_COUNT_KEY, 0L);
                return;
            }

            for (DatasetGroup group : groups) {
                if (group == null || group.getId() == null) {
                    continue;
                }
                List<Long> datasetIds = datasetDatasetGroupMapper.getDatasetsByDatasetGroupId(group.getId());
                if (datasetIds == null || datasetIds.isEmpty()) {
                    continue;
                }
                for (Long datasetId : datasetIds) {
                    Integer cnt = datasetVersionServiceImpl.calculateDatasetTotalFilesForSystem(datasetId);
                    if (cnt != null) {
                        total += cnt.longValue();
                    }
                }
            }

            redisUtils.set(Constant.DATASET_ANNOTATED_IMAGE_COUNT_KEY, total);
        } catch (Exception e) {
            log.warn("refreshAnnotatedImageCount failed: {}", e.getMessage());
        }
    }
}
