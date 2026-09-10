package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.data.dao.DatasetOperationEventMapper;
import org.dubhe.data.domain.entity.DatasetOperationEvent;
import org.dubhe.data.service.DatasetOperationEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class DatasetOperationEventServiceImpl
        extends ServiceImpl<DatasetOperationEventMapper, DatasetOperationEvent>
        implements DatasetOperationEventService {

    private Logger logger = LoggerFactory.getLogger(DatasetOperationEventService.class);

    @Override
    public Page<DatasetOperationEvent> pageByDatasetId(Long datasetId, Page<DatasetOperationEvent> page) {
        return this.lambdaQuery()
                .eq(DatasetOperationEvent::getDatasetId, datasetId)
                .orderByDesc(DatasetOperationEvent::getEventSubmittedTime)
                .page(page);
    }

//    // 明确排除deleted字段
//    public boolean safeInsert(DatasetOperationEvent event) {
//        return this.getBaseMapper().insert(
//                new DatasetOperationEvent()
//                        .setDatasetId(event.getDatasetId())
//                        .setEventSubmittedTime(event.getEventSubmittedTime())
//                        .setEventDetailInfo(event.getEventDetailInfo())
//                        .setEventType(event.getEventType())
//                        .setOperationType(event.getOperationType())
//        ) > 0;
//    }


    @Async
    public void safeInsertAsync(DatasetOperationEvent event) {
        try {
            this.getBaseMapper().insert(
                    new DatasetOperationEvent()
                            .setDatasetId(event.getDatasetId())
                            .setEventSubmittedTime(event.getEventSubmittedTime())
                            .setEventDetailInfo(event.getEventDetailInfo())
                            .setEventType(event.getEventType())
                            .setOperationType(event.getOperationType())
            );
        } catch (Exception e) {
            logger.error("Error occurred while inserting dataset operation event: ", e);
        }
    }

    /**
     * 构造DatasetOperationEvent实体并调用异步插入方法
     *
     * @param datasetId 数据集ID，表示此次操作事件对应的数据集
     * @param eventSubmittedTime 事件提交时间，表示事件发生的具体时间
     * @param eventDetailInfo 事件详情，描述事件的具体内容，例如操作步骤或错误信息
     * @param eventType 事件类型，取值应为DatasetOperationEvent.EventType中的常量
     *                  例如：INFO（0），ERROR（1）
     * @param operationType 操作类型，取值应为DatasetOperationEvent.OperationType中的常量
     *                      请不要使用magic number，请使用实体类中定义好的常量。例如 DatasetOperationEvent.OperationType.AUTO_LABELING
     *                      例如：DATA_AUGMENTATION（0），VIDEO_FRAME_EXTRACTION（1），AUTO_LABELING（2），DATA_CLEANING（3），DATA_EXPORT（4）
     *                  用来描述操作事件的具体类型
     */
    public void createAndInsertEvent(Long datasetId, Date eventSubmittedTime, String eventDetailInfo, Integer eventType, Integer operationType) {
        // 构建DatasetOperationEvent对象
        DatasetOperationEvent event = new DatasetOperationEvent()
                .setDatasetId(datasetId)
                .setEventSubmittedTime(eventSubmittedTime)
                .setEventDetailInfo(eventDetailInfo)
                .setEventType(eventType)
                .setOperationType(operationType);

        // 调用异步插入方法
        this.safeInsertAsync(event);
    }
}

