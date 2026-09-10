package org.dlut.adv.mineai.model.service;

import org.apache.commons.lang.StringUtils;
import org.dlut.adv.mineai.core.entity.MineService;
import org.dlut.adv.mineai.core.utils.FieldRename;
import org.dlut.adv.mineai.model.repository.ModelMineServiceRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ModelMineServiceService {
    @Resource
    ModelMineServiceRepo modelMineServiceRepo;

    FieldRename fieldRename = new FieldRename();

    public boolean save(MineService mineService) {
        return modelMineServiceRepo.save(mineService) != null;
    }

    /**
     * 获得所有未删除的服务list
     *
     * @return
     */
    public List<MineService> getMineServiceList() {
        return modelMineServiceRepo.getMineServiceList();
    }

    /**
     * 根据服务Id查询拿到对应的服务
     *
     * @param mineServiceId
     * @return
     */
    public MineService getMineServiceById(Long mineServiceId) {
        Optional<MineService> optional = modelMineServiceRepo.findById(mineServiceId);
        return optional.orElse(null);
    }


    public MineService findMineServiceByName(String name) {
        return modelMineServiceRepo.findMineServiceByName(name);
    }

    public List<String> getNamesOfMineService() {
        return modelMineServiceRepo.getNamesOfMineService();
    }

    public MineService findMineServiceById(long mineServiceId) {
        return modelMineServiceRepo.findMineServiceById(mineServiceId);
    }

    /**
     * @param mineService
     * @description 判断是否存在
     */
    public boolean isMineServiceExist(MineService mineService) {
        if (StringUtils.isBlank(mineService.getName())) {
            return false;
        }
        return modelMineServiceRepo.findMineServiceByName(mineService.getName()) != null;
    }

    /**
     * 模糊查询
     */
    public Page<MineService> getVagueMineServicePage(Pageable pageable, String vagueInfo) {
        return modelMineServiceRepo.findAll(new Specification<MineService>() {
                                           @Override
                                           public Predicate toPredicate(Root<MineService> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                                               List<Predicate> predicates = new ArrayList<>();
                                               if (StringUtils.isNotEmpty(vagueInfo)) {
                                                   predicates.add(cb.or(cb.like(root.<String>get("name"), "%" + vagueInfo + "%"), cb.like(root.<String>get("mineServiceName"), "%" + vagueInfo + "%"), cb.like(root.<String>get("port").as(String.class), "%" + String.valueOf(vagueInfo) + "%")));
                                               }
                                               cq.where(predicates.toArray(new Predicate[predicates.size()]));
                                               return null;
                                           }
                                       },
                pageable);
    }

    /**
     * 用于动态分页查找
     */
    public Page<MineService> dynamicFindMineServicePage(Pageable pageable, MineService mineService) {
        return modelMineServiceRepo.findAll(new Specification<MineService>() {
            @Override
            public Predicate toPredicate(Root<MineService> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();


                Boolean nameAndPortAndMineServiceName = (StringUtils.isBlank(mineService.getName()))
                        && (StringUtils.isBlank(mineService.getMineServiceName()))
                        && (mineService.getPort() == 0);
                if (!nameAndPortAndMineServiceName) {
                    if (StringUtils.isNotBlank(mineService.getName())) {
                        predicates.add(cb.like(root.<String>get("name"), "%" + mineService.getName() + "%"));
                    }
                    if (StringUtils.isNotBlank(mineService.getMineServiceName())) {
                        predicates.add(cb.like(root.<String>get("mineServiceName"), "%" + mineService.getMineServiceName() + "%"));
                    }
                    if (mineService.getPort() != 0) {
                        predicates.add(cb.like(root.<String>get("port").as(String.class), "%" + String.valueOf(mineService.getPort()) + "%"));
                    }
                }

                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 删除服务
     */
    public boolean deleteMineService(MineService mineService) {
        MineService origin = modelMineServiceRepo.findById(mineService.getId()).orElse(null);
        modelMineServiceRepo.unbindControllerByMineServiceId(origin.getId());
        modelMineServiceRepo.save(origin);
        modelMineServiceRepo.delete(mineService);
        return true;
    }

    /**
     * 更新
     *
     * @param mineService
     * @return
     */
    public boolean updateMineService(MineService mineService) {
        if (StringUtils.isBlank(mineService.getName())) {
            return false;   //输入信息为空
        }
        MineService originMineService = modelMineServiceRepo.findMineServiceById(mineService.getId());
        if (originMineService == null) {
            return false;
        }
        MineService sameNameMineService = modelMineServiceRepo.findMineServiceByName(mineService.getName());
        if (sameNameMineService != null && sameNameMineService.getId() != mineService.getId()) {
            return false;   // name重复
        } else {
            mineService.setId(originMineService.getId());
            mineService.setModel(originMineService.getModel());
            return modelMineServiceRepo.save(mineService) != null;
        }
    }

    /**
     * 查询当前设备已绑定的服务id
     */
    public List<Long> findMineServiceIdByControllerId(long controllerId) {
        return modelMineServiceRepo.findMineServiceIdByControllerId(controllerId);
    }

    /**
     * 查询当前设备绑定的服务list
     */
    public List<MineService> findMineServiceByControllerId(long controllerId) {
        return modelMineServiceRepo.findMineServiceByControllerId(controllerId);
    }


    public long findMineServiceIdByModelVersionId(long modelVersionId) {
        MineService mineService = modelMineServiceRepo.findMineServiceByModelId(modelVersionId);
        return mineService.getId();
    }
    public MineService findMineServiceByModelVersionId(long modelVersionId) {
        return modelMineServiceRepo.findMineServiceByModelId(modelVersionId);
    }


}
