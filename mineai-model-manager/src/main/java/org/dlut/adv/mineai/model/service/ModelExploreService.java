package org.dlut.adv.mineai.model.service;


import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.common.aliasing.qual.Unique;
import org.dlut.adv.mineai.core.entity.ModelExplore;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.core.entity.UserContextHolder;
import org.dlut.adv.mineai.core.utils.FieldRename;
import org.dlut.adv.mineai.model.api.MsgCode;
import org.dlut.adv.mineai.model.client.DubheUserFeign;
import org.dlut.adv.mineai.model.repository.ModelExploreRepo;
import org.dlut.adv.mineai.model.repository.ModelRepo;
import org.dlut.adv.mineai.model.utils.DubheUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class ModelExploreService {

    @Resource
    ModelExploreRepo modelExploreRepo;

    @Resource
    DubheUserFeign dubheUserFeign;

    @Resource
    private DubheUtils dubheUtils;

    FieldRename fieldRename = new FieldRename();

    /**
     * 用于动态分页查找
     */
    public Page<ModelExplore> dynamicFindModelPage(Pageable pageable, ModelExplore modelExplore) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.notEqual(root.<Integer>get("isDelete"), 1));
                //筛选已发布的算法
                predicates.add(cb.equal(root.get("modelStatus"), 2));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelExplore.getModelName())
                        && StringUtils.isBlank(modelExplore.getDescription())
                        && modelExplore.getMonitorType() == 0;
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelExplore.getModelName())) {
                        predicates.add(cb.like(root.<String>get("modelName"), "%" + modelExplore.getModelName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(modelExplore.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + modelExplore.getDescription() + "%"));
                    }
                    if (modelExplore.getMonitorType() != 0) {
                        predicates.add(cb.equal(root.get("monitorType"), modelExplore.getMonitorType()));
                    }
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    /**
     * 用于动态分页查找
     */
    public Page<ModelExplore> dynamicFindUnReleasedModelPage(Pageable pageable, ModelExplore modelExplore, String userName) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelExplore.NOT_DELETE));
                //筛选已发布的算法
                predicates.add(cb.equal(root.get("modelStatus"), ModelExplore.UNRELEASED));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelExplore.getModelName())
                        && StringUtils.isBlank(modelExplore.getDescription())
                        && StringUtils.isBlank(modelExplore.getModelNickName())
                        && modelExplore.getMonitorType() == 0
                        && StringUtils.isBlank(userName);
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelExplore.getModelName())) {
                        predicates.add(cb.like(root.<String>get("modelName"), "%" + modelExplore.getModelName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(modelExplore.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + modelExplore.getDescription() + "%"));
                    }
                    //根据算法别称查询
                    if (StringUtils.isNotBlank(modelExplore.getModelNickName())) {
                        predicates.add(cb.like(root.<String>get("modelNickName"), "%" + modelExplore.getModelNickName() + "%"));
                    }
                    if (modelExplore.getMonitorType() != 0) {
                        predicates.add(cb.equal(root.get("monitorType"), modelExplore.getMonitorType()));
                    }
                    //根据用户名查询
                    if (StringUtils.isNotBlank(userName)) {
                        List<Long> ids = dubheUserFeign.findIdsByUsernameLike(dubheUtils.getAuthorization(), userName).getData();
                        if (ids != null && !ids.isEmpty()) {
                            predicates.add(root.get("creatorId").in(ids));
                        } else {
                            // 如果没查到任何用户，返回空结果（避免查询所有记录）
                            predicates.add(cb.equal(root.get("creatorId"), -1L));
                        }
                    }
                }
                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    predicates.add(cb.equal(root.get("creatorId"), UserContextHolder.getUserContext().getId()));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    /**
     * 用于动态分页查找
     */
    public Page<ModelExplore> dynamicFindReleasedModelPage(Pageable pageable, ModelExplore modelExplore) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelExplore.NOT_DELETE));
                //筛选已发布的算法
                predicates.add(cb.equal(root.get("modelStatus"), ModelExplore.RELEASED));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(modelExplore.getModelName())
                        && StringUtils.isBlank(modelExplore.getDescription())
                        && modelExplore.getMonitorType() == 0;
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(modelExplore.getModelName())) {
                        predicates.add(cb.like(root.<String>get("modelName"), "%" + modelExplore.getModelName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(modelExplore.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + modelExplore.getDescription() + "%"));
                    }
                    if (modelExplore.getMonitorType() != 0) {
                        predicates.add(cb.equal(root.get("monitorType"), modelExplore.getMonitorType()));
                    }
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 用于动态分页查找 处于审核状态的算法
     */
    public Page<ModelExplore> dynamicFindUnderExamineModelPage(Pageable pageable, ModelExplore model) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                //未被删除保留
                predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelExplore.NOT_DELETE));
                //筛选已发布的算法
                predicates.add(cb.notEqual(root.<Integer>get("releaseStatus"), ModelExplore.NOT_UNDER_EXAMINE));
                //根据modelName查询
                Boolean PageableIsBlank = StringUtils.isBlank(model.getModelName())
                        && StringUtils.isBlank(model.getDescription())
                        && model.getMonitorType() == 0;
                if (!PageableIsBlank) {
                    if (StringUtils.isNotBlank(model.getModelName())) {
                        predicates.add(cb.like(root.<String>get("modelName"), "%" + model.getModelName() + "%"));
                    }
                    //根据描述查询
                    if (StringUtils.isNotBlank(model.getDescription())) {
                        predicates.add(cb.like(root.<String>get("description"), "%" + model.getDescription() + "%"));
                    }
                    if (model.getMonitorType() != 0) {
                        predicates.add(cb.equal(root.get("monitorType"), model.getMonitorType()));
                    }
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 模糊查询
     */
    public Page<ModelExplore> getVagueModelPage(Pageable pageable, String vagueInfo) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.notEqual(root.<Integer>get("isDelete"), 1));

                    predicates.add(cb.or(cb.like(root.<String>get("modelName"), "%" + vagueInfo + "%"), cb.like(root.<String>get("description"), "%" + vagueInfo + "%"), cb.like(root.<String>get("modelEnglishName"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    public Page<ModelExplore> getVagueUnReleasedModelPage(Pageable pageable, String vagueInfo) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.<Integer>get("modelStatus"), ModelExplore.UNRELEASED));
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelExplore.NOT_DELETE));

                    predicates.add(cb.or(
                            cb.like(root.<String>get("modelName"), "%" + vagueInfo + "%"),
                            cb.like(root.<String>get("modelNickName"), "%" + vagueInfo + "%"),
                            cb.like(root.<String>get("description"), "%" + vagueInfo + "%")));
                }

                String roleName = UserContextHolder.getUserContext().getRoles().get(0).getName();
                if (!("管理员".equals(roleName) || "管理人员".equals(roleName))) {
                    predicates.add(cb.equal(root.get("creatorId"), UserContextHolder.getUserContext().getId()));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 处于审核状态的算法的模糊查询
     *
     * @param pageable
     * @param vagueInfo
     * @return
     */
    public Page<ModelExplore> getVagueUnderExamineModelPage(Pageable pageable, String vagueInfo) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.notEqual(root.<Integer>get("releaseStatus"), ModelExplore.NOT_UNDER_EXAMINE));
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelExplore.NOT_DELETE));

                    predicates.add(cb.or(cb.like(root.<String>get("modelName"), "%" + vagueInfo + "%"), cb.like(root.<String>get("description"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    public Page<ModelExplore> getVagueReleasedModelPage(Pageable pageable, String vagueInfo) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.<Integer>get("modelStatus"), ModelExplore.RELEASED));
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.equal(root.<Integer>get("isDelete"), ModelExplore.NOT_DELETE));

                    predicates.add(cb.or(cb.like(root.<String>get("modelName"), "%" + vagueInfo + "%"), cb.like(root.<String>get("description"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    public Page<ModelExplore> getVagueModelPageWithoutEnglishName(Pageable pageable, String vagueInfo) {
        return modelExploreRepo.findAll(new Specification<ModelExplore>() {
            @Override
            public Predicate toPredicate(Root<ModelExplore> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.notEqual(root.<Integer>get("isDelete"), 1));

                    predicates.add(cb.or(cb.like(root.<String>get("modelName"), "%" + vagueInfo + "%"), cb.like(root.<String>get("description"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    public List<ModelExplore> getExplores() {
        return modelExploreRepo.findModelExploresByIsDeleteAndModelStatus(ModelExplore.NOT_DELETE, ModelExplore.RELEASED);
    }


    public void saveModel(ModelExplore model) {

        modelExploreRepo.save(model);
    }

    /**
     * @param model
     * @description 判断modelName是否存在
     */
    public boolean isModelExploreExist(ModelExplore model) {
        if (StringUtils.isBlank(model.getModelName())) {
            return false;
        }
        return modelExploreRepo.findModelExploreByModelName(model.getModelName()) != null;
    }

    public boolean isModelExploreNameExistWithOwnerID(ModelExplore model) {
        if (StringUtils.isBlank(model.getModelName())) {
            return false;
        }
        return !modelExploreRepo.findModelExploreByModelNameAndCreatorId(model.getModelName(),model.getCreatorId()).isEmpty();
    }


    /**
     * @param model
     * @description 判断Description是否存在
     */
    public boolean isModelExploreDescriptionExist(ModelExplore model) {
        if (StringUtils.isBlank(model.getDescription())) {
            return false;
        }
        return modelExploreRepo.findModelExploreByDescription(model.getDescription()) != null;
    }


    /**
     * 更新算法
     *
     * @param modelExplore
     * @return
     */
    public MsgCode updateModelExplore(ModelExplore modelExplore) {
        // 判断模型名称   模型描述是否为空
        if (StringUtils.isBlank(modelExplore.getModelName())) {
            return MsgCode.MODEL_NAME_EMPTY;
        }
        if (StringUtils.isBlank(modelExplore.getDescription())) {
            return MsgCode.MODEL_DESCRIPTION_EMPTY;
        }
        // 设置非空约束的update默认值
        modelExplore.setSubsystem(ModelExplore.CENTRAL_PLATFORM);
        modelExploreRepo.save(modelExplore);
        return MsgCode.SUCCEED;
    }

    /**
     * 根据 算法 id 查询 ModelExplore
     *
     * @param id
     * @return
     */
    public ModelExplore findModelExploreById(long id) {
        return modelExploreRepo.findModelExploreById(id);
    }

    /**
     * 根据算法名称查询算法
     */
    public ModelExplore findModelExploreByName(String name) {
        return modelExploreRepo.findModelExploreByModelName(name);
    }

    public ModelExplore getModelExploreById(Long modelId) {
        Optional<ModelExplore> optional = modelExploreRepo.findById(modelId);
        return optional.orElse(null);
    }

    /**
     * 软删除model,重命名删除的modelName
     *
     * @param modelId
     * @return isDelete
     */
    public boolean deleteModel(Long modelId) {
        ModelExplore model = modelExploreRepo.findById(modelId).orElse(null);
        if (model != null) {
            // 查找monitor中除id外所有@Unique的属性

            List<String> uniqueFieldListExceptId = fieldRename.getUniqueFieldListExceptId(model.getClass());
            for (String uniqueFiled : uniqueFieldListExceptId) {
                try {
                    // 获取unique字段的值
                    String uniqueValue = (String) PropertyUtils.getProperty(model, uniqueFiled);
                    // 根据 uniqueFiled 调用对应repo查询方法
                    Method method = ModelRepo.class.getMethod("findModelListBy" + uniqueFiled.substring(0, 1).toUpperCase() + uniqueFiled.substring(1) + "Like", String.class, int.class);

                    //   Method method = ModelRepo.class.getMethod("findModelBy" + uniqueFiled.substring(0, 1).toUpperCase() + uniqueFiled.substring(1) + "LikeAndIsDelete", String.class, int.class);
                    List<ModelExplore> modelListByUniqueFiled = (List<ModelExplore>) method.invoke(modelExploreRepo, uniqueValue + "%", ModelExplore.DELETE);
                    // 获取monitorProperty的值List
                    List<String> modelFiledList = new ArrayList<>();
                    for (ModelExplore model1 : modelListByUniqueFiled) {
                        modelFiledList.add((String) PropertyUtils.getProperty(model1, uniqueFiled));
                    }
                    // 重命名unique字段的值
                    String newName = fieldRename.renameProperty(uniqueValue, modelFiledList);
                    PropertyUtils.setProperty(model, uniqueFiled, newName);
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
            model.setIsDelete(ModelExplore.DELETE);
            modelExploreRepo.save(model);
            return true;
        }
        return false;
    }

    private List<Field> getUniqueFields(Object object) {
        List<Field> uniqueFields = new ArrayList<>();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Unique.class)) {
                uniqueFields.add(field);
            }
        }
        return uniqueFields;
    }

    public List<ModelExplore> getModelExplore() {
        return modelExploreRepo.findModelExploresByIsDeleteAndModelStatus(ModelExplore.NOT_DELETE, ModelExplore.RELEASED);
    }


    /**
     * 获取所有的未发布 且 未删除的算法
     *
     * @return List<ModelExplore>
     */
    public List<ModelExplore> findAll() {
        return modelExploreRepo.findAll();
    }


    public List<ModelVersion> findtrainModelVersionByModelExplore() {
        return modelExploreRepo.findtrainModelVersionByModelExplore(ModelExplore.NOT_DELETE);
    }

    public List<ModelVersion> finddeployModelVersionByModelExplore() {
        return modelExploreRepo.finddeployModelVersionByModelExplore(ModelExplore.NOT_DELETE);
    }

    public List<ModelVersion> findconvertModelVersionByModelExplore() {
        return modelExploreRepo.findconvertModelVersionByModelExplore(ModelExplore.NOT_DELETE);
    }


    public List<ModelVersion> AllNoDeleteModelVersion() {
        List<ModelVersion> trainVersions = findtrainModelVersionByModelExplore();
        List<ModelVersion> deployVersions = finddeployModelVersionByModelExplore();
        List<ModelVersion> convertVersions = findconvertModelVersionByModelExplore();
        Set<ModelVersion> uniqueVersions = new HashSet<>();
        uniqueVersions.addAll(trainVersions);
        uniqueVersions.addAll(deployVersions);
        uniqueVersions.addAll(convertVersions);
        return new ArrayList<>(uniqueVersions);
    }

    public void deleteModelExplore(Long modelExploreId) {
        // 查询实体
        ModelExplore modelExplore = modelExploreRepo.findById(modelExploreId)
                .orElseThrow(() -> new IllegalArgumentException("无效的ID"));

        // 生成时间戳格式：yyyyMMddHHmmss
        String timestamp = new SimpleDateFormat("（已删除-yyyyMMddHHmmss）").format(new Date());

        // 追加后缀（处理null值）
        if (modelExplore.getModelName() != null) {
            modelExplore.setModelName(modelExplore.getModelName() + timestamp);
        } else {
            modelExplore.setModelName("已删除对象" + timestamp);
        }

        if (modelExplore.getModelNickName() != null) {
            modelExplore.setModelNickName(modelExplore.getModelNickName() + timestamp);
        } else {
            modelExplore.setModelNickName("已删除对象" + timestamp);
        }

        // 软删除并更新
        modelExplore.setIsDelete(1);
        modelExploreRepo.save(modelExplore); // 触发JPA更新
    }
}
