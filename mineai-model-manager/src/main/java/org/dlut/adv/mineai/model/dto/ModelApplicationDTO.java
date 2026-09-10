package org.dlut.adv.mineai.model.dto;

import lombok.Data;
import org.dlut.adv.mineai.core.entity.Device;
import org.dlut.adv.mineai.core.entity.Model;
import org.dlut.adv.mineai.core.entity.Scene;

import javax.persistence.Id;
import java.util.List;

/**
 * @author mingming
 */
@Data
public class ModelApplicationDTO {
    @Id
    private Long id;

    private String applicationTaskName;

    private String description;

    private Long applicationNameId;

    private Long publisherId;

    private Device device;

    private List<Scene> applicableScene;

    private Model model;
}
