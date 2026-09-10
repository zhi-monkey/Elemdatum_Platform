package org.dubhe.data.domain.dto;

import lombok.Data;

@Data
public class HttpCameraSyncResult {
    private Integer addedCount;
    private Integer updatedCount;
    private Integer offlineCount;
    private Integer totalCount;
}
