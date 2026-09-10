package org.dubhe.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.dubhe.data.domain.dto.DataRepoFileCreateDTO;
import org.dubhe.data.domain.dto.DataRepoFileQueryDTO;
import org.dubhe.data.domain.dto.DataRepoFileUpdateDTO;
import org.dubhe.data.domain.dto.FileTypeCountDTO;
import org.dubhe.data.domain.entity.DataRepoFile;
import org.dubhe.data.domain.vo.DataRepoFileVO;

import java.util.List;

public interface DataRepoFileService extends IService<DataRepoFile> {

    Page<DataRepoFileVO> listDataRepoFiles(Page<DataRepoFile> page, DataRepoFileQueryDTO queryDTO);

    DataRepoFileVO getDataRepoFileById(Long id);

    boolean createDataRepoFile(DataRepoFileCreateDTO createDTO);

    boolean updateDataRepoFile(DataRepoFileUpdateDTO updateDTO, Long id);

    void deleteDataRepoFile(Long id);

    FileTypeCountDTO countFileTypesInDataset(Long datasetId);

    FileTypeCountDTO countAllDatasetFileNumsByType();

    boolean createDataRepoFiles(List<DataRepoFileCreateDTO> createDTOList);

    boolean deleteBatchByIds(List<Long> ids);
}