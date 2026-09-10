package org.dubhe.data.domain.vo;

import org.dubhe.biz.db.base.BaseConvert;
import org.dubhe.data.domain.entity.DataTeam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
public interface DataTeamConvert extends BaseConvert<DataTeamVO, DataTeam> {
    @Override
    @Mapping(target = "canModify", source = "canModify")
    DataTeamVO toDto(DataTeam entity);
}
