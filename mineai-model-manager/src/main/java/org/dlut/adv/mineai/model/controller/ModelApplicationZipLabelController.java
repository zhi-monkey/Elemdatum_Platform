package org.dlut.adv.mineai.model.controller;

import lombok.extern.slf4j.Slf4j;
import org.dlut.adv.mineai.core.entity.ModelApplicationZipLabel;
import org.dlut.adv.mineai.core.entity.Msg;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;
import org.dlut.adv.mineai.model.service.ModelApplicationZipLabelService;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author mingming
 * @date 2025/03/31
 */
@Slf4j
@RestController
@RequestMapping("/modelApplicationZipLabel")
public class ModelApplicationZipLabelController {
    @Autowired
    private ModelApplicationZipLabelService modelApplicationZipLabelService;
    @Resource
    private DubheDataFeign dubheDataFeign;
    @Resource
    private DubheUtils dubheUtils;

    @GetMapping
    public Msg<List<LabelDTO>> findByModelApplicationId(@RequestParam Long modelApplicationId) {
        List<Long> labelIds = modelApplicationZipLabelService.getBoundLabelIds(modelApplicationId);
        if (labelIds == null || labelIds.isEmpty()) {
            return new Msg<>(MsgCode.SUCCEED, null);
        }
        return new Msg<>(MsgCode.SUCCEED, dubheDataFeign.findLabelByIds(dubheUtils.getAuthorization(), labelIds).getData());
    }

}
