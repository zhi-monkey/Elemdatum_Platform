package org.dubhe.data.domain.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.dubhe.biz.db.annotation.Query;
import org.dubhe.biz.db.base.PageQueryBase;
import org.dubhe.data.domain.entity.DataRepo;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
public class DataRepoQueryDTO extends PageQueryBase{

    @Query(type = Query.Type.LIKE)
    private String name;

    private Boolean deleted;

    private List<Long> createTime;

    @ApiModelProperty(required = false, hidden = true)
    @Query(type = Query.Type.BETWEEN, propName = "create_time")
    private List<Timestamp> createTimeSearch;

    public void timeConvert() {
        if (!CollectionUtils.isEmpty(this.createTime)) {
            createTimeSearch = new ArrayList<>(createTime.size());
            createTime.forEach(aLong -> createTimeSearch.add(new Timestamp(aLong)));
        }
    }

}
