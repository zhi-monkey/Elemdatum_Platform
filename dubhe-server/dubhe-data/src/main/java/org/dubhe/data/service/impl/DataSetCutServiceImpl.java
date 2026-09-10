package org.dubhe.data.service.impl;

import org.dubhe.biz.file.utils.MinioUtil;
import org.dubhe.data.domain.dto.CutRuleDTO;
import org.dubhe.data.service.DataSetCutService;
import org.dubhe.data.service.DatasetVersionFileService;
import org.dubhe.data.service.DatasetVersionService;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;

/**
 * @package: org.dubhe.data.service.impl
 * @author: chystart
 * @create: 2024-04-06 16:24
 * @description: 数据集切分实现类
 **/
@Service
public class DataSetCutServiceImpl implements DataSetCutService {

    @Resource
    private MinioUtil minioUtil;

    @Resource
    private DatasetVersionService datasetVersionService ;

    @Override
    public void cutCocoAnnotationDataSet(Long dataSetId, CutRuleDTO cutRuleDTO) {
        System.out.println(dataSetId + "------------" + cutRuleDTO);
        List<String> strings = datasetVersionService.getDatasetVersionNameList(dataSetId);
        strings.stream().forEach(s -> System.out.println("s = " + s));
    }
}
