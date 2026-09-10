package org.dubhe.data.domain.vo;

import org.dubhe.biz.db.base.BaseConvert;
import org.dubhe.data.domain.entity.DataTeamSubtask;
import org.dubhe.data.domain.entity.DataTeamTask;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.vo
 * @Project：mineai
 * @name：DataTeamConvert
 * @Date：2024/3/11 16:59
 * @Filename：DataTeamConvert
 * @Desc：团队转换
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface DataTeamSubtaskConvert extends BaseConvert<DataTeamSubtaskVO, DataTeamSubtask> {

}
