

package org.dubhe.admin.service.convert;

import org.dubhe.biz.base.dto.TeamSmallDTO;
import org.dubhe.biz.db.base.BaseConvert;
import org.dubhe.admin.domain.entity.Team;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @description 团队 转换类
 * @date 2020-06-01
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TeamSmallConvert extends BaseConvert<TeamSmallDTO, Team> {
}
