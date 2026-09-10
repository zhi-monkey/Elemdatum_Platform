package org.dlut.adv.mineai.model.service;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.dlut.adv.mineai.core.entity.ApplicationName;
import org.dlut.adv.mineai.core.utils.FieldRename;
import org.dlut.adv.mineai.model.repository.ApplicationNameRepo;
import org.dlut.adv.mineai.model.repository.ModelApplicationRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wangwenjiao
 */
@Service
public class ApplicationNameService {
    @Resource
    private ModelApplicationRepo modelApplicationRepo;

    @Resource
    private ApplicationNameRepo applicationNameRepo;

    FieldRename fieldRename = new FieldRename();

    // 查询所有应用数据
    public List<ApplicationName> findAll() {
        return applicationNameRepo.findAll((root, query, cb) ->
                cb.equal(root.get("isDelete"), false)
        );
    }

    //动态分页查询
    public Page<ApplicationName> dynamicFindApplicationNamePage(Pageable pageable, ApplicationName applicationName) {
        return applicationNameRepo.findAll(new Specification<ApplicationName>() {
            @Override
            public Predicate toPredicate(Root<ApplicationName> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                // 条件1: 根据 id 查询（这里假设您只查询精确匹配的 id）
                if (applicationName.getId() != null) {
                    predicates.add(cb.equal(root.get("id"), applicationName.getId()));
                }
                // 条件2: 根据 name 模糊查询
                if (StringUtils.isNotBlank(applicationName.getApplicationName())) {
                    predicates.add(cb.like(root.get("applicationName"), "%" + applicationName.getApplicationName() + "%"));
                }
                predicates.add(cb.equal(root.get("isDelete"), false));
                query.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 模糊查询
     */
    public Page<ApplicationName> getVagueApplicationNamePage(Pageable pageable, String vagueInfo) {
        return applicationNameRepo.findAll(new Specification<ApplicationName>() {
            @Override
            public Predicate toPredicate(Root<ApplicationName> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.or(cb.like(root.<String>get("applicationName"), "%" + vagueInfo + "%")));
                }
                predicates.add(cb.equal(root.get("isDelete"), false));
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }


    // 添加或更新应用
    public ApplicationName saveOrUpdate(ApplicationName applicationName) {
        if (modelApplicationRepo.existsByApplicationNameId(applicationName.getId())) {
            throw new RuntimeException("存在关联的模型应用，禁止编辑");
        }
        return applicationNameRepo.save(applicationName);
    }

    // 根据ID删除应用
    public void deleteById(Long id) {
        ApplicationName applicationNameById = applicationNameRepo.findApplicationNameById(id);

        if (applicationNameById != null) {
            // 新增关联检查（使用前一个问题添加的existsByApplicationNameId方法）
            if (modelApplicationRepo.existsByApplicationNameId(id)) {
                throw new RuntimeException("存在关联的模型应用，禁止删除");
            }
            // 标记为删除状态
            applicationNameById.setIsDelete(true);
            // 获取当前时间戳并格式化
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);

            // 获取除ID以外的所有唯一字段
            List<String> uniqueFieldListExceptId = fieldRename.getUniqueFieldListExceptId(applicationNameById.getClass());
            for (String uniqueField : uniqueFieldListExceptId) {
                try {
                    // 获取unique字段的值
                    String uniqueValue = (String) PropertyUtils.getProperty(applicationNameById, uniqueField);

                    // 根据uniqueField字段调用对应repo查询方法
                    Method method = ApplicationNameRepo.class.getMethod(
                            "findApplicationNameListBy" + uniqueField.substring(0, 1).toUpperCase() + uniqueField.substring(1) + "Like",
                            String.class, boolean.class
                    );

                    // 调用方法查询类似的字段值并过滤已删除的数据
                    List<ApplicationName> applicationListByUniqueField =
                            (List<ApplicationName>) method.invoke(applicationNameRepo, uniqueValue + "%", true);

                    // 获取已存在的字段值列表
                    List<String> existingUniqueValues = new ArrayList<>();
                    for (ApplicationName application : applicationListByUniqueField) {
                        existingUniqueValues.add((String) PropertyUtils.getProperty(application, uniqueField));
                    }

                    // 重命名字段的值以确保唯一性
                    String newUniqueValue = fieldRename.renameProperty(uniqueValue, existingUniqueValues) + "(已删除)" + timestamp;
                    PropertyUtils.setProperty(applicationNameById, uniqueField, newUniqueValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // 保存更新后的对象
            applicationNameRepo.save(applicationNameById);
        }
    }

    // 根据id称精确查询
    public ApplicationName findApplicationNameById(Long applicationId) {
        return applicationNameRepo.findById(applicationId)
                .filter(app -> !app.getIsDelete())
                .orElse(null);
    }

    // 根据应用名称精确查询
    public List<ApplicationName> findByApplicationName(String applicationName) {
        return applicationNameRepo.findByApplicationNameAndIsDelete(applicationName, false);
    }

    public List<ApplicationName> findByApplicationNameContaining(String applicationName) {
        return applicationNameRepo.findByApplicationNameContainingAndIsDelete(applicationName, false);
    }

    /**
     * 检查是否可以删除
     */
    public boolean isApplicationNameDeletable(ApplicationName applicationName) {
        if (modelApplicationRepo.existsByApplicationNameId(applicationName.getId())) {
            return false;
        }
        return true;
    }
    /**
     * 检查是否可以编辑
     */
    public boolean isApplicationNameEditable(ApplicationName applicationName) {
        if (modelApplicationRepo.existsByApplicationNameId(applicationName.getId())) {
            return false;
        }
        return true;
    }
}

