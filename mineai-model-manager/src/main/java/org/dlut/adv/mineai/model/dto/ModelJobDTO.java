package org.dlut.adv.mineai.model.dto;

import lombok.Data;
import org.dlut.adv.mineai.core.entity.ModelJob;

@Data
public class ModelJobDTO {
    private ModelJob modelJob;
    private String userName;// 用户名
    private Boolean isDelete;

    public ModelJobDTO(ModelJob modelJob, String userName,Boolean isDelete) {
        this.modelJob = modelJob;
        this.userName = userName;
        this.isDelete = isDelete;
    }
}
