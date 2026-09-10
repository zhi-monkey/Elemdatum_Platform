package org.dubhe.data.domain.dto;

import lombok.Data;
import org.dubhe.biz.db.annotation.Query;
import org.dubhe.biz.db.base.PageQueryBase;

import java.io.Serializable;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.data.domain.dto
 * @Project：mineai
 * @name：DataTeamQueryDTO
 * @Date：2024/3/11 16:21
 * @Filename：DataTeamQueryDTO
 * @Desc：
 */

@Data
public class DataTeamTaskQueryDTO  extends PageQueryBase implements Serializable  {

    private static final long serialVersionUID = 1L;

    @Query(propName = "name")
    private String name;

    @Query(propName = "deleted", type = Query.Type.EQ)
    private Boolean deleted = false;


}
