package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MultiRecordUploadUrlVO {
    private Long taskId;
    private Long datasetId;
    private String bucket;
    private String objectKey;
    private String uploadUrl;
}
