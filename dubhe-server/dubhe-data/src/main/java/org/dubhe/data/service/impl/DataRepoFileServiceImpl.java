package org.dubhe.data.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.dubhe.biz.base.service.UserContextService;
import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.dao.DataRepoFileMapper;
import org.dubhe.data.domain.dto.DataRepoFileCreateDTO;
import org.dubhe.data.domain.dto.DataRepoFileQueryDTO;
import org.dubhe.data.domain.dto.DataRepoFileUpdateDTO;
import org.dubhe.data.domain.dto.FileTypeCountDTO;
import org.dubhe.data.domain.entity.DataRepoFile;
import org.dubhe.data.domain.vo.DataRepoFileVO;
import org.dubhe.data.service.DataRepoFileService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataRepoFileServiceImpl extends ServiceImpl<DataRepoFileMapper, DataRepoFile> implements DataRepoFileService {

    @Autowired
    private DataRepoFileMapper dataRepoFileMapper;
    /**
     * 用户内容服务
     */
    @Autowired
    private UserContextService userContextService;


    @Autowired
    private MinioUtil minioUtil;

//    @Override
//    public Page<DataRepoFileVO> listDataRepoFiles(Page<DataRepoFile> page, DataRepoFileQueryDTO queryDTO) {
//        queryDTO.setDeleted(false);
//        QueryWrapper<DataRepoFile> queryWrapper = queryDTO.toQueryWrapper();
//        Page<DataRepoFile> dataRepoFilePage = this.page(page, queryWrapper);
//        Page<DataRepoFileVO> voPage = new Page<>();
//        BeanUtils.copyProperties(dataRepoFilePage, voPage, "records");
//        voPage.setRecords(dataRepoFilePage.getRecords().stream().map(this::convertToVO).collect(Collectors.toList()));
//        return voPage;
//    }
    @Override
    public Page<DataRepoFileVO> listDataRepoFiles(Page<DataRepoFile> page, DataRepoFileQueryDTO queryDTO) {
        queryDTO.setDeleted(false);
        QueryWrapper<DataRepoFile> queryWrapper = queryDTO.toQueryWrapper();
        Page<DataRepoFile> dataRepoFilePage = this.page(page, queryWrapper);
        Page<DataRepoFileVO> voPage = new Page<>();
        BeanUtils.copyProperties(dataRepoFilePage, voPage, "records");

        // 转换记录并查询文件大小
        List<DataRepoFileVO> voList = dataRepoFilePage.getRecords().stream().map(dataRepoFile -> {
            DataRepoFileVO vo = convertToVO(dataRepoFile);
            // 从URL中解析出bucketName和objectName
            String[] parts = vo.getUrl().split("/", 3);
            if (parts.length < 3) {
                // URL格式不正确，可以根据实际情况处理，这里简单处理为跳过
                return vo;
            }
            String bucketName = parts[0];
            String objectName = parts[1] + "/" + parts[2];

            // 调用接口查询文件大小
            try {
                Map<String, Object> fileDetails = minioUtil.getFileDetails(bucketName, objectName);
                // 格式化文件大小，转换为MB并保留两位小数
                long fileSizeInBytes = Long.parseLong(fileDetails.get("size").toString());
                double fileSizeInMB = fileSizeInBytes / (1024.0 * 1024.0);
                String formattedFileSize = String.format("%.2fMB", fileSizeInMB);
                vo.setFileSize(formattedFileSize);
            } catch (Exception e) {
                // 可以选择设置一个默认值或者留空
                vo.setFileSize("未知");
            }

            return vo;
        }).collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public DataRepoFileVO getDataRepoFileById(Long id) {
        DataRepoFile dataRepoFile = this.getById(id);
        if (dataRepoFile == null) {
            return null;
        }
        return convertToVO(dataRepoFile);
    }

    @Override
    public boolean createDataRepoFile(DataRepoFileCreateDTO createDTO) {
        DataRepoFile dataRepoFile = new DataRepoFile();
        BeanUtils.copyProperties(createDTO, dataRepoFile);
        dataRepoFile.setCreateTime(new Timestamp(new Date().getTime()));
        dataRepoFile.setOriginUserId(userContextService.getCurUserId());
        dataRepoFile.setCreateUserId(userContextService.getCurUserId());
        return this.save(dataRepoFile);
    }

    @Override
    public boolean updateDataRepoFile(DataRepoFileUpdateDTO updateDTO, Long id) {
        DataRepoFile dataRepoFile = this.getById(id);
        if (dataRepoFile == null) {
            return false;
        }
        BeanUtils.copyProperties(updateDTO, dataRepoFile);
        return this.updateById(dataRepoFile);
    }

    @Override
    public void deleteDataRepoFile(Long id) {
        DataRepoFile dataRepoFile = new DataRepoFile();
        dataRepoFile.setId(id);
        dataRepoFile.setDeleted(true);
        this.updateById(dataRepoFile);
    }

    private DataRepoFileVO convertToVO(DataRepoFile dataRepoFile) {
        DataRepoFileVO vo = new DataRepoFileVO();
        BeanUtils.copyProperties(dataRepoFile, vo);
        return vo;
    }

    @Override
    public FileTypeCountDTO countFileTypesInDataset(Long datasetId) {
        return dataRepoFileMapper.countFileTypesByDatasetId(datasetId);
    }

    @Override
    public FileTypeCountDTO countAllDatasetFileNumsByType() {
        return dataRepoFileMapper.countAllDatasetFileNumsByType();
    }

    @Override
    public boolean createDataRepoFiles(List<DataRepoFileCreateDTO> createDTOList) {
        if (createDTOList == null || createDTOList.isEmpty()) {
            return false;
        }

        List<DataRepoFile> dataRepoFiles = createDTOList.stream().map(dto -> {
            DataRepoFile dataRepoFile = new DataRepoFile()
                    .setName(dto.getName())
                    .setDatasetId(dto.getDatasetId())
                    .setUrl(dto.getUrl())
                    .setFileType(dto.getFileType())
                    .setWidth(dto.getWidth())
                    .setHeight(dto.getHeight())
                    .setOriginUserId(userContextService.getCurUserId())
                    .setCreateUserId(userContextService.getCurUserId());
            return dataRepoFile;
        }).collect(Collectors.toList());

        // 使用MyBatis Plus提供的saveBatch方法批量插入
        return this.saveBatch(dataRepoFiles);
    }


    @Override
    public boolean deleteBatchByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }

        // 创建一个DataRepoFile对象作为更新模板，设置deleted为true
        DataRepoFile dataRepoFileTemplate = new DataRepoFile();
        dataRepoFileTemplate.setDeleted(true);

        // 创建更新条件，使其仅适用于提供的ID列表
        UpdateWrapper<DataRepoFile> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids);

        // 执行更新操作
        boolean updateResult = this.update(dataRepoFileTemplate, updateWrapper);
        return updateResult;
    }
}
