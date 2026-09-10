package org.dubhe.data.domain.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.Data;
import org.dubhe.data.domain.entity.DataRepoFile;
import java.sql.Timestamp;

@Data
public class DataRepoFileQueryDTO {

    private String name;
    private Long datasetId;
    private Boolean deleted;
    private Integer fileType;
    private Long[] createTime;

    public QueryWrapper<DataRepoFile> toQueryWrapper() {
        QueryWrapper<DataRepoFile> queryWrapper = new QueryWrapper<>();
        if (name != null) {
            queryWrapper.like("name", this.name);
        }
        if (datasetId != null) {
            queryWrapper.eq("dataset_id", this.datasetId);
        }
        if (deleted != null) {
            queryWrapper.eq("deleted", this.deleted);
        }
        if (fileType != null) {
            queryWrapper.eq("file_type", this.fileType);
        }
        if (createTime != null && createTime.length == 2) {
            queryWrapper.between("create_time", new Timestamp(createTime[0]), new Timestamp(createTime[1]));
        }
        return queryWrapper;
    }
}
