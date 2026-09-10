package org.dlut.adv.mineai.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * 取消发布前的占用检查结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModelApplicationUnbindCheckVO {

    /**
     * 是否允许取消发布
     */
    private boolean canUnbind;

    /**
     * 阻塞取消发布的生产任务列表
     */
    @Builder.Default
    private List<ModelGenerationBriefVO> blockingGenerations = Collections.emptyList();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModelGenerationBriefVO {
        private Long id;
        private String name;
        private Integer status;
    }
}

