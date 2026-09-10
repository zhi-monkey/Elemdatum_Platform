package org.dubhe.data.domain.dto;

import lombok.Data;

@Data
public class VideoStatisticsDTO {
    private int totalVideos;
    private int unExtractedVideos;
    private int extractedVideos;
}

