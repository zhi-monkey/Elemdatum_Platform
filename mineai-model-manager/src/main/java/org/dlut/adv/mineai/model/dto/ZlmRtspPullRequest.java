package org.dlut.adv.mineai.model.dto;

import lombok.Data;

@Data
public class ZlmRtspPullRequest {
    private String rtspUrl;
    private String stream;
    private String app;
    private Boolean closeOld;
}
