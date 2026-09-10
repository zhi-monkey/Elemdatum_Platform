package org.dubhe.data.domain.dto;

import lombok.Data;
import org.dubhe.data.domain.entity.DataRepo;

@Data
public class DataRepoUpdateDTO {
    private String name;
    private String remark;
    private String uri;

    public DataRepo toDataRepo() {
        DataRepo dataRepo = new DataRepo();
        dataRepo.setName(this.name);
        dataRepo.setRemark(this.remark);
        dataRepo.setUri(this.uri);
        // 根据需要设置其他属性
        return dataRepo;
    }
}
