package org.dlut.adv.mineai.model.service;

import org.dlut.adv.mineai.core.entity.ModelAlert;
import org.dlut.adv.mineai.model.repository.ModelAlertRepo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @package: org.dlut.adv.mineai.model.service
 * @author: chystart
 * @create: 2023-12-07 13:43
 * @description: 透明化煤流 Service
 **/
@Service
public class CoalFlowService {

    @Resource
    private ModelAlertRepo modelAlertRepo;

    /**
     * 根据算法的名称 和 子系统的前缀 查找最新的报警信息
     *
     * @param commadName 算法名称
     * @param subSystemPrefixName 子系统
     * @return
     */
    public List<ModelAlert> getModelAlert(String commadName, String subSystemPrefixName) {
        return modelAlertRepo.findModelAlertByAlgorithmNameAndSubSystemPrefixName(commadName, subSystemPrefixName);
    }

    /**
     * 针对 小时煤流 的数据量返回近 7 个小时的数据
     *
     * @return
     */
    public List<ModelAlert> getHourlyCoalFlowOvertime(String commadName) {
        return modelAlertRepo.getHourlyCoalFlowData(commadName);
    }

}
