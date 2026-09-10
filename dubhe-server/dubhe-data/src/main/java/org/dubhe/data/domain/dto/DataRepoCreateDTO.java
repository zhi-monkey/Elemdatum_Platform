package org.dubhe.data.domain.dto;

import lombok.Data;
import org.dubhe.data.domain.entity.DataRepo;

@Data
public class DataRepoCreateDTO {
    private String name;
    private String remark;

    public DataRepo toDataRepo() {
        DataRepo dataRepo = new DataRepo();
        dataRepo.setName(this.name);
        dataRepo.setRemark(this.remark);
        // 根据需要设置其他属性
        return dataRepo;
    }
}
