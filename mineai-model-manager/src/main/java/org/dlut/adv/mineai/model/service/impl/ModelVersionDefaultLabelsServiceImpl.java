package org.dlut.adv.mineai.model.service.impl;

import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.GuidedAndLabelsVO;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;
import org.dlut.adv.mineai.model.domain.entity.ModelVersionDefaultLabels;
import org.dlut.adv.mineai.model.repository.ModelVersionDefaultLabelsRepo;
import org.dlut.adv.mineai.model.service.ModelVersionDefaultLabelsService;
import org.dlut.adv.mineai.model.service.ModelVersionService;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author mingming
 * @date 2025/06/11
 */
@Service
public class ModelVersionDefaultLabelsServiceImpl implements ModelVersionDefaultLabelsService {
    @Resource
    private DubheDataFeign dubheDataFeign;

    @Resource
    private DubheUtils dubheUtils;

    private final ModelVersionDefaultLabelsRepo modelVersionDefaultLabelsRepo;
    @Autowired
    private ModelVersionService modelVersionService;

    public ModelVersionDefaultLabelsServiceImpl(ModelVersionDefaultLabelsRepo modelVersionDefaultLabelsRepo) {
        this.modelVersionDefaultLabelsRepo = modelVersionDefaultLabelsRepo;
    }

    @Autowired
    private EntityManager entityManager;

    @Transactional
    @Override
    public void insertRelations(Long modelVersionId, String[] labelNames) {
        // JDBC批处理优化
        try (Session session = entityManager.unwrap(Session.class)) {
            session.doWork(connection -> {
                try (PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO model_version_default_labels (model_version_id, label_name) VALUES (?, ?)"
                )) {
                    for (String labelName : labelNames) {
                        ps.setLong(1, modelVersionId);
                        ps.setString(2, labelName);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            });
        }
    }

    /**
     * @param imageUrl 镜像URL
     * @return {@link List }<{@link String }> 返回值未labelName的数组
     */
    @Override
    public List<String> getLabelsByImageUrl(String imageUrl) {
        ModelVersion modelVersion = modelVersionService.findModelVersionByUrl(imageUrl);
        if (modelVersion == null) {
            return Collections.emptyList();
        }
        return modelVersionDefaultLabelsRepo.findRelationsByModelVersionId(modelVersion.getId())
                .stream()
                .map(ModelVersionDefaultLabels::getLabelName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getLabelsByImageUrlAndDatasetId(String imageUrl, Long datasetId) {
        // 1. 获取镜像对应的标签
        ModelVersion modelVersion = modelVersionService.findModelVersionByUrl(imageUrl);
        if (modelVersion == null) {
            return Collections.emptyList();
        }
        List<String> imageLabels = modelVersionDefaultLabelsRepo.findRelationsByModelVersionId(modelVersion.getId())
                .stream()
                .map(ModelVersionDefaultLabels::getLabelName)
                .filter(Objects::nonNull)  // 过滤掉null值
                .collect(Collectors.toList());
        if (imageLabels.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 获取数据集对应的所有标签
        GuidedAndLabelsVO guidedAndLabels = dubheDataFeign.guidedAndLabels(dubheUtils.getAuthorization(), datasetId).getData();
        if (guidedAndLabels == null || guidedAndLabels.getLabels() == null) {
            return Collections.emptyList();
        }
        List<String> datasetLabels = guidedAndLabels.getLabels().stream()
                .map(LabelDTO::getName)  // 使用LabelDTO的name字段
                .filter(Objects::nonNull)  // 过滤掉null值
                .collect(Collectors.toList());
        if (datasetLabels.isEmpty()) {
            return Collections.emptyList();
        }
        // 3. 取交集
        List<String> intersection = new ArrayList<>(imageLabels);
        intersection.retainAll(datasetLabels);

        return intersection;
    }
}
