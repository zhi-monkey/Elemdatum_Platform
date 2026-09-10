package org.dubhe.data.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PcDatasetUploadUrlVO {
    private Long datasetId;
    private String name;
    private String bucket;
    private String objectKey;
    private String uploadUrl;
}
