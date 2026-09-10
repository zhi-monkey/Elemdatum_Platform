package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.entity.DatasetOperationEvent;

import java.util.Date;


public interface DatasetOperationEventService extends IService<DatasetOperationEvent> {
    Page<DatasetOperationEvent> pageByDatasetId(Long datasetId, Page<DatasetOperationEvent> page);
    //boolean safeInsert(DatasetOperationEvent event);
    void safeInsertAsync(DatasetOperationEvent event);
    void createAndInsertEvent(Long datasetId, Date eventSubmittedTime, String eventDetailInfo, Integer eventType, Integer operationType);
}

//使用例
//eventService.createAndInsertEvent(856L, new Date(),"开启了数据增强",DatasetOperationEvent.EventType.INFO, DatasetOperationEvent.OperationType.DATA_AUGMENTATION);
