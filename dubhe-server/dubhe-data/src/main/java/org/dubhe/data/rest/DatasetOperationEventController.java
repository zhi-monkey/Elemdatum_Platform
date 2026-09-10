package org.dubhe.data.rest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.dubhe.biz.base.constant.Permissions;
import org.dubhe.biz.base.vo.DataResponseBody;
import org.dubhe.data.constant.Constant;
import org.dubhe.data.domain.entity.DatasetOperationEvent;
import org.dubhe.data.service.DatasetOperationEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@Api(tags = "数据处理：数据集操作事件")
@RestController
@RequestMapping(Constant.MODULE_URL_PREFIX + "/datasets/dataset-operations")
public class DatasetOperationEventController {

    @Autowired
    private DatasetOperationEventService eventService;

    @ApiOperation(value = "分页查询操作记录")
    @GetMapping
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<Page<DatasetOperationEvent>> page(Page<DatasetOperationEvent> page,Long datasetId) {
        return new DataResponseBody<>(eventService.pageByDatasetId(datasetId,page));
    }


    @ApiOperation(value = "操作记录详情")
    @GetMapping("/{id}")
    @PreAuthorize(Permissions.DATA)
    public DataResponseBody<DatasetOperationEvent> detail(@PathVariable Long id) {
        return new DataResponseBody<>(eventService.getById(id));
    }
}



//CREATE TABLE dataset_operation_event (
//        id BIGINT PRIMARY KEY AUTO_INCREMENT,
//        dataset_id BIGINT NOT NULL,
//        event_submitted_time DATETIME NOT NULL,
//        event_detail_info VARCHAR(1000),
//event_type INT NOT NULL COMMENT '0:INFO, 1:ERROR',
//operation_type INT NOT NULL COMMENT '0:DATA_AUGMENTATION, 1:VIDEO_FRAME_EXTRACTION, 2:AUTO_LABELING, 3:DATA_CLEANING, 4:DATA_EXPORT',
//
//INDEX idx_dataset (dataset_id),
//INDEX idx_submitted_time (event_submitted_time)
//) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

