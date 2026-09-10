package org.dlut.adv.mineai.model.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class SceneWithApplicationsVO {

    private Long sceneId;

    private String sceneName;

    private List<SceneApplicationTaskVO> applications;
}
