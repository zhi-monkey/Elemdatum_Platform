package org.dlut.adv.mineai.model.service;

import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.*;
import org.dlut.adv.mineai.core.vo.DatasetVersionVO;
import org.dlut.adv.mineai.model.client.DubheDataFeign;
import org.dlut.adv.mineai.model.domain.dto.LabelDTO;
import org.dlut.adv.mineai.model.repository.ApplicationNameRepo;
import org.dlut.adv.mineai.model.repository.DeviceRepository;
import org.dlut.adv.mineai.model.repository.ModelApplicationRepo;
import org.dlut.adv.mineai.model.repository.ModelGenerationRepo;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ModelApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ModelApplicationService.class);

    @Resource
    private ModelApplicationRepo modelApplicationRepo;
    @Autowired
    private ApplicationNameRepo applicationNameRepo;
    @Autowired
    private DeviceRepository deviceRepository;
    @Autowired
    private ModelGenerationRepo modelGenerationRepo;
    @Autowired
    private DubheDataFeign dubheDataFeign;
    @Autowired
    private DubheUtils dubheUtils;
    @Autowired
    private ModelApplicationZipLabelService modelApplicationZipLabelService;
    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${minio.annotation}")
    private String annotation;

    //    public Page<ModelApplication> findAll(Pageable pageable, String applicationTaskName) {
//        return modelApplicationRepo.findAll((Specification<ModelApplication>) (root, cq, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//            predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelApplication.NOT_DELETED));
//            if (StringUtils.isNotEmpty(applicationTaskName)) {
//                predicates.add(cb.or(cb.like(root.get("applicationTaskName"), "%" + applicationTaskName + "%"), cb.like(root.get("description"), "%" + applicationTaskName + "%")));
//            }
//            cq.where(predicates.toArray(new Predicate[predicates.size()]));
//            return null;
//        }, pageable);
//    }
    public Page<ModelApplication> dynamicFindApplicationPage(Pageable pageable, ModelApplication modelApplication) {
        return modelApplicationRepo.findAll((Specification<ModelApplication>) (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 保留未删除的数据
            //predicates.add(cb.equal(root.get("isDelete"), ModelApplication.NOT_DELETED));

            // 精确查找 applicationTaskName
            if (StringUtils.isNotEmpty(modelApplication.getApplicationTaskName())) {
                predicates.add(cb.equal(root.get("applicationTaskName"), modelApplication.getApplicationTaskName()));
            }
            String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
            if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                predicates.add(cb.equal(root.get("creatorId"), UserContextHolder.getUserContext().getId()));
            }
            cq.where(predicates.toArray(new Predicate[0]));
            return null;
        }, pageable);
    }

    public Page<ModelApplication> findSubApplicationTaskPage(Pageable pageable, ModelApplication modelApplication, String modelName) {
        return modelApplicationRepo.findAll((Specification<ModelApplication>) (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Long modelId = applicationNameRepo.findIdByApplicationName(modelName);
            predicates.add(cb.and(
                    cb.equal(root.get("applicationName"), modelId),
                    cb.equal(root.get("isReleased"), "已发布")
            ));
            cq.where(predicates.toArray(new Predicate[0]));
            return null;
        }, pageable);
    }

    public Page<ModelApplication> findAllApplicationPage(Pageable pageable) {
        return modelApplicationRepo.findAll((Specification<ModelApplication>) (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isDelete"), ModelApplication.NOT_DELETED));

            cq.where(predicates.toArray(new Predicate[0]));
            return null;
        }, pageable);
    }

    public Page<ModelApplication> findVagueApplicationPage(Pageable pageable, ModelApplication modelApplication) {
        return modelApplicationRepo.findAll((Specification<ModelApplication>) (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 保留未删除的数据
            predicates.add(cb.equal(root.get("isDelete"), ModelApplication.NOT_DELETED));

            // 模糊查询 applicationTaskName 和 description
            if (StringUtils.isNotEmpty(modelApplication.getApplicationTaskName())) {
                predicates.add(cb.like(root.get("applicationTaskName"), "%" + modelApplication.getApplicationTaskName() + "%"));
            }
//            if (StringUtils.isNotEmpty(modelApplication.getDescription())) {
//                predicates.add(cb.like(root.get("description"), "%" + modelApplication.getDescription() + "%"));
//            }
            // 模糊查询 Model 中的 modelName 字段
            if (modelApplication.getModel() != null && StringUtils.isNotEmpty(modelApplication.getModel().getModelName())) {
                Join<ModelApplication, Model> modelJoin = root.join("model", JoinType.LEFT);
                predicates.add(cb.like(modelJoin.get("modelName"), "%" + modelApplication.getModel().getModelName() + "%"));
            }

            // 模糊查询 Scene 中的 name 字段
            if (modelApplication.getApplicableScene() != null && !modelApplication.getApplicableScene().isEmpty()) {
                Join<ModelApplication, Scene> sceneJoin = root.join("applicableScene", JoinType.LEFT);
                String sceneName = modelApplication.getApplicableScene().get(0).getName();
                if (StringUtils.isNotEmpty(sceneName)) {
                    predicates.add(cb.like(sceneJoin.get("name"), "%" + sceneName + "%"));
                }
            }

            // 模糊查询 Device 中的 deviceName 和 firmwareVersion 字段
            if (modelApplication.getDevice() != null) {
                Join<ModelApplication, Device> deviceJoin = root.join("device", JoinType.LEFT);
                String deviceName = modelApplication.getDevice().getDeviceName();
                String firmwareVersion = modelApplication.getDevice().getFirmwareVersion();

                boolean hasDeviceName = StringUtils.isNotEmpty(deviceName);
                boolean hasFirmwareVersion = StringUtils.isNotEmpty(firmwareVersion);

                if (hasDeviceName && hasFirmwareVersion) {
                    // 名称和版本都存在，使用 AND
                    Predicate deviceNamePredicate = cb.like(deviceJoin.get("deviceName"), "%" + deviceName);
                    Predicate firmwareVersionPredicate = cb.like(deviceJoin.get("firmwareVersion"), firmwareVersion + "%");
                    predicates.add(cb.and(deviceNamePredicate, firmwareVersionPredicate));
                } else if (hasDeviceName) {
                    // 只有 deviceName 有值，做 OR 查询 deviceName 或 firmwareVersion 包含该条件
                    Predicate deviceNamePredicate = cb.like(deviceJoin.get("deviceName"), "%" + deviceName + "%");
                    Predicate firmwareVersionPredicate = cb.like(deviceJoin.get("firmwareVersion"), "%" + deviceName + "%");
                    predicates.add(cb.or(deviceNamePredicate, firmwareVersionPredicate));
                }
                // 如果两个字段都为空，不添加条件
            }
            String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
            if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                predicates.add(cb.equal(root.get("creatorId"), UserContextHolder.getUserContext().getId()));
            }

            cq.where(predicates.toArray(new Predicate[0]));
            return null;
        }, pageable);
    }


//    public Page<ModelApplication> findVagueApplicationPage(Pageable pageable, ModelApplication modelApplication) {
//        return modelApplicationRepo.findAll((Specification<ModelApplication>) (root, cq, cb) -> {
//            List<Predicate> predicates = new ArrayList<>();
//
//            // 保留未删除的数据
//            //predicates.add(cb.equal(root.get("isDelete"), ModelApplication.NOT_DELETED));
//
//            // 模糊查找 applicationTaskName 或 description
//            if (StringUtils.isNotEmpty(modelApplication.getApplicationTaskName())) {
//                predicates.add(cb.or(
//                        cb.like(root.get("applicationTaskName"), "%" + modelApplication.getApplicationTaskName() + "%")));
//            }
//            if (StringUtils.isNotEmpty(modelApplication.getDescription())) {
//                predicates.add(cb.or(
//                        cb.like(root.get("description"), "%" + modelApplication.getDescription() + "%")));
//            }
//
//
//
//            cq.where(predicates.toArray(new Predicate[0]));
//            return null;
//        }, pageable);
//    }

    // 添加或更新应用
    public ModelApplication saveOrUpdate(ModelApplication modelApplication) {
        return modelApplicationRepo.save(modelApplication);
    }

    // 根据ID删除应用
    public void deleteById(Long id) {
        modelApplicationRepo.softDelete(id);
    }

    public void bindReleasedById(Long id) {
        modelApplicationRepo.bindReleasedById(id);
    }

    public void unbindReleasedById(Long id) {
        modelApplicationRepo.unbindReleasedById(id);
    }

    @Transactional
    public void removeBoundScene(Long id) {
        modelApplicationRepo.removeBoundScene(id);
    }

    public void removeBoundDevice(Long id) {
        modelApplicationRepo.removeBoundDevice(id);
    }

    public void removeBoundModel(Long id) {
        modelApplicationRepo.removeBoundModel(id);
    }

    public ModelApplication findModelApplicationById(Long modelApplicationId) {
        Optional<ModelApplication> modelApplication = modelApplicationRepo.findById(modelApplicationId);
        return modelApplication.orElse(null);
    }

    /**
     * 获取classes在minio中的路径
     * @param modelApplicationId 应用任务ID
     * @return {@link String }
     */
    public String getClassesFileUrlByModelApplicationId(Long modelApplicationId) {
        ModelApplication modelApplication = findModelApplicationById(modelApplicationId);
        if (modelApplication == null) {
            throw new RuntimeException("应用任务不存在");
        }
        Model model = modelApplication.getModel();
        if (model == null) {
            throw new RuntimeException("应用任务没有绑算法");
        }
        ModelGeneration modelGeneration = modelGenerationRepo.findById(model.getGenerationId()).orElse(null);
        if (modelGeneration == null) {
            throw new RuntimeException("没找到ModelGeneration, id为" + model.getGenerationId());
        }
        DatasetVersionVO data = dubheDataFeign.selectDatasetVersionById(dubheUtils.getAuthorization(), modelGeneration.getTrainDataset()).getData();
        return "/" + bucketName + "/dataset/" + data.getDatasetId() + "/versionFile/" + data.getVersionName() + "/YOLO/annotations/classes.txt";
    }

    public List<String> getReleasedApplicationName() {
        return modelApplicationRepo.getReleasedApplicationName();
    }

    public ModelApplication getUniqueModelApplication(String applicationName, String deviceAndFirmwareName) {
        ApplicationName applicationNameByName = applicationNameRepo.findApplicationByApplicationName(applicationName);
        String deviceName = StringUtils.substringBeforeLast(deviceAndFirmwareName, "-");
        String firmwareVersion = StringUtils.substringAfterLast(deviceAndFirmwareName, "-");
        Device deviceByDeviceName = deviceRepository.findDeviceByDeviceNameAndFirmwareVersion(deviceName, firmwareVersion);
        return modelApplicationRepo.getUniqueModelApplication(applicationNameByName.getId(), deviceByDeviceName.getId());
    }

    public List<Device> getDeviceByBoundApplicationName(String applicationName) {
        ApplicationName applicationNameByApplicationName = applicationNameRepo.findApplicationByApplicationName(applicationName);
        List<ModelApplication> modelApplicationsByApplicationNameId = modelApplicationRepo.findModelApplicationsByapplicationNameId(applicationNameByApplicationName.getId());
        return modelApplicationsByApplicationNameId.stream().map((ModelApplication::getDevice)).collect(Collectors.toList());
    }

    public List<ApplicationName> findApplicationNameByBoundDeviceFirmware(String deviceName, String firmwareVersion) {
        Device deviceByDeviceNameAndFirmwareVersion = deviceRepository.findDeviceByDeviceNameAndFirmwareVersion(deviceName, firmwareVersion);
        List<ModelApplication> modelApplicationsByDeviceAndFirmwareName = modelApplicationRepo.findModelApplicationsByDeviceId(deviceByDeviceNameAndFirmwareVersion.getId());
        if (!modelApplicationsByDeviceAndFirmwareName.isEmpty()) {
            return modelApplicationsByDeviceAndFirmwareName.stream().map(ModelApplication::getApplicationName).collect(Collectors.toList());
        }
        return null;
    }

    public String getSplitSizeByApplicationNameAndDevice(String applicationName, String deviceFirmware) {
        ApplicationName applicationByApplicationName = applicationNameRepo.findApplicationByApplicationName(applicationName);
        Device deviceByDeviceName = deviceRepository.findDeviceByDeviceName(deviceFirmware.split("-")[0]);
        return modelApplicationRepo.getSplitSizeByApplicationNameAndDevice(applicationByApplicationName.getId(), deviceByDeviceName.getId());
    }

    public boolean checkIfApplicationExists(String deviceName, String firmwareVersion, String applicationName) {
        Specification<ModelApplication> spec = (root, query, cb) -> {
            Join<ModelApplication, Device> deviceJoin = root.join("device", JoinType.INNER);
            Join<ModelApplication, ApplicationName> appNameJoin = root.join("applicationName", JoinType.INNER);

            return cb.and(
                    cb.equal(deviceJoin.get("deviceName"), deviceName),
                    cb.equal(deviceJoin.get("firmwareVersion"), firmwareVersion),
                    cb.equal(appNameJoin.get("applicationName"), applicationName), // ApplicationName 实体里字段名要改成真实的
                    cb.equal(root.get("isReleased"), "已发布"),
                    cb.equal(root.get("isDelete"), ModelApplication.NOT_DELETED)
            );
        };

        return modelApplicationRepo.count(spec) > 0;
    }

    public List<Map<String, Object>> getBoundLabels(Long modelApplicationId) {
        List<Long> labelIds = modelApplicationZipLabelService.getBoundLabelIds(modelApplicationId);
        if (labelIds == null || labelIds.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            List<LabelDTO> labels = dubheDataFeign.findLabelByIds(dubheUtils.getAuthorization(), labelIds).getData();
            if (labels == null) return new ArrayList<>();

            return labels.stream()
                    .map(label -> {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", label.getId());
                        map.put("name", label.getName());
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取应用绑定标签失败：modelApplicationId={}", modelApplicationId, e);
            return new ArrayList<>();
        }
    }

    public long countAllApplications() {
        Specification<ModelApplication> spec = (root, query, cb) -> cb.equal(root.get("isDelete"), ModelApplication.NOT_DELETED);
        return modelApplicationRepo.count(spec);
    }

    public long countApplicationsByReleaseStatus(String releaseStatus) {
        Specification<ModelApplication> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("isDelete"), ModelApplication.NOT_DELETED),
                cb.equal(root.get("isReleased"), releaseStatus)
        );
        return modelApplicationRepo.count(spec);
    }

    public Map<String, Long> countApplicationsReleasedAndUnreleased() {
        Map<String, Long> result = new HashMap<>();
        long releasedCount = countApplicationsByReleaseStatus("已发布");
        long unreleasedCount = countApplicationsByReleaseStatus("未发布");
        result.put("releasedCount", releasedCount);
        result.put("unreleasedCount", unreleasedCount);
        return result;
    }

    public long countDistinctApplicationNamesByReleaseStatus(String releaseStatus) {
        return modelApplicationRepo.countDistinctApplicationNameIdsByReleaseStatus(releaseStatus);
    }

    public Map<String, Long> countDistinctApplicationNamesReleasedAndUnreleased() {
        Map<String, Long> result = new HashMap<>();
        long releasedCount = countDistinctApplicationNamesByReleaseStatus("已发布");
        long unreleasedCount = countDistinctApplicationNamesByReleaseStatus("未发布");
        result.put("releasedCount", releasedCount);
        result.put("unreleasedCount", unreleasedCount);
        return result;
    }
}