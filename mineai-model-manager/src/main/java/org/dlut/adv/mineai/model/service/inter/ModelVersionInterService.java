package org.dlut.adv.mineai.model.service.inter;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.domain.dto.ModelVersionCreateDTO;
import org.dlut.adv.mineai.model.dto.ModelVersionDTO;

public interface ModelVersionInterService {

    /**
     * 创建镜像Url
     *
     * @return Long 镜像id
     */
    Msg<ModelVersionDTO> create(Long userId, String url, String showName, String level, String description, String use, String chipType);

    /**
     * 创建镜像Local
     *
     * @param showName
     * @param level
     * @param fileName
     * @param description
     * @param use 镜像用途. 现在修改为多项选 例如可能为 "训练,自动标注"
     * @return
     */
    Msg<ModelVersion> createLocal(Long userId, String showName, String level, String fileName, String description, String use,String chipType);

    /**
     * modelVersion唯一性检验
     *
     * @return
     */
    Boolean checkModelVersion(ModelVersionCreateDTO modelVersionCreateDTO);

//    /**
//     * 分页
//     * @param page
//     * @param criteria
//     * @return
//     */
//    IPage<ModelVersion> findModelVersionPage(Page<ModelVersion> page, ModelVersionCreateDTO criteria);

    Msg<ModelVersion> updateModelVersion(Long mdId, String url, String showName, String level, String description, String use,String chipType);


    String ModelVersionUse(Long id);
}
