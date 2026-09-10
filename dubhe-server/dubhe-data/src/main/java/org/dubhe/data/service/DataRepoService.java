package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.dto.DataRepoCreateDTO;
import org.dubhe.data.domain.dto.DataRepoQueryDTO;
import org.dubhe.data.domain.dto.DataRepoUpdateDTO;
import org.dubhe.data.domain.entity.DataRepo;
import org.dubhe.data.domain.vo.DataRepoVO;

import java.util.List;

public interface DataRepoService extends IService<DataRepo> {

    Page<DataRepoVO> listDataRepos(Page<DataRepo> page, DataRepoQueryDTO queryDTO);

    DataRepoVO getDataRepoById(Long id);

    boolean createDataRepo(DataRepoCreateDTO createDTO);

    boolean updateDataRepo(DataRepoUpdateDTO updateDTO, Long id);

    void deleteDataRepo(Long id);

    List<DataRepoVO> queryAllDataRepos();

    List<DataRepoVO> queryAllDataReposNotNull();

    boolean isNameExist(String departmentName);
}