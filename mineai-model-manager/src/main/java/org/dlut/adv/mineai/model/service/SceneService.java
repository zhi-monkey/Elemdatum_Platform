package org.dlut.adv.mineai.model.service;

import org.dlut.adv.mineai.core.entity.Scene;
import org.dlut.adv.mineai.model.domain.vo.SceneApplicationTaskVO;
import org.dlut.adv.mineai.model.domain.vo.SceneWithApplicationsVO;
import org.dlut.adv.mineai.model.repository.ModelApplicationRepo;
import org.dlut.adv.mineai.model.repository.SceneRepo;
import javax.annotation.Resource;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import javax.persistence.criteria.Predicate;
/**
 * @author wangwenjiao
 */
@Service
public class SceneService {

    @Resource
    private ModelApplicationRepo modelApplicationRepo;

    @Resource
    private SceneRepo sceneRepo;

    // 查询所有数据
    public List<Scene> findAll() {
        return sceneRepo.findAll();
    }

    public List<Scene> findAllNotDelete() {
        return sceneRepo.findAllNotDelete();
    }

    public List<SceneWithApplicationsVO> findAllActiveWithApplications() {
        List<Scene> scenes = sceneRepo.findAllNotDelete();
        Map<Long, SceneWithApplicationsVO> sceneMap = new LinkedHashMap<>();
        if (scenes != null) {
            for (Scene scene : scenes) {
                if (scene == null || scene.getId() == null) {
                    continue;
                }
                SceneWithApplicationsVO vo = new SceneWithApplicationsVO();
                vo.setSceneId(scene.getId());
                vo.setSceneName(scene.getName());
                vo.setApplications(new ArrayList<>());
                sceneMap.put(scene.getId(), vo);
            }
        }

        List<Object[]> rows = modelApplicationRepo.findSceneApplicationTaskAndChip();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row == null || row.length < 3) {
                    continue;
                }
                Long sceneId = row[0] == null ? null : ((Number) row[0]).longValue();
                String applicationName = row[1] == null ? null : row[1].toString();
                String chipType = row[2] == null ? null : row[2].toString();

                if (sceneId == null || !sceneMap.containsKey(sceneId)) {
                    continue;
                }

                if (StringUtils.isBlank(applicationName)) {
                    continue;
                }

                SceneWithApplicationsVO sceneVO = sceneMap.get(sceneId);
                boolean exists = false;
                for (SceneApplicationTaskVO app : sceneVO.getApplications()) {
                    if (applicationName.equals(app.getApplicationTaskName())
                            && ((chipType == null && app.getDevice() == null) || (chipType != null && chipType.equals(app.getDevice())))) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    continue;
                }

                SceneApplicationTaskVO app = new SceneApplicationTaskVO();
                app.setApplicationTaskName(applicationName);
                app.setDevice(chipType);
                sceneVO.getApplications().add(app);
            }
        }

        return new ArrayList<>(sceneMap.values());
    }

    //动态分页查询
    public Page<Scene> dynamicFindScenePage(Pageable pageable, Scene scene) {
        return sceneRepo.findAll(new Specification<Scene>() {
            @Override
            public Predicate toPredicate(Root<Scene> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();

                // 条件1: 未被删除的记录
                predicates.add(cb.notEqual(root.get("isDelete"), 1));


               // 条件2: 根据 id 查询（这里假设您只查询精确匹配的 id）
                if (scene.getId() != null) {
                    predicates.add(cb.equal(root.get("id"), scene.getId()));
                }

                // 条件3: 根据 name 模糊查询
                if (StringUtils.isNotBlank(scene.getName())) {
                    predicates.add(cb.like(root.get("name"), "%" + scene.getName() + "%"));
                }

                // 条件4: 根据 inscription 模糊查询
                if (StringUtils.isNotBlank(scene.getInstruction())) {
                    predicates.add(cb.like(root.get("instruction"), "%" + scene.getInstruction() + "%"));
                }

                query.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 模糊查询
     */
    public Page<Scene> getVagueScenePage(Pageable pageable, String vagueInfo) {
        return sceneRepo.findAll(new Specification<Scene>() {
            @Override
            public Predicate toPredicate(Root<Scene> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
                List<Predicate> predicates = new ArrayList<>();
                if (org.apache.commons.lang.StringUtils.isNotEmpty(vagueInfo)) {
                    predicates.add(cb.notEqual(root.<Integer>get("isDelete"), 1));

                    predicates.add(cb.or(cb.like(root.<String>get("name"), "%" + vagueInfo + "%"), cb.like(root.<String>get("instruction"), "%" + vagueInfo + "%")));
                }
                cq.where(predicates.toArray(new Predicate[predicates.size()]));
                return null;
            }
        }, pageable);
    }

    /**
     * 检查是否可以编辑场景
     */
    public boolean isSceneEditable(Scene scene) {
        // 场景已经绑定
        if (modelApplicationRepo.countBySceneId(scene.getId()) != 0)
            return false;
        return true;
    }
    /**
     * 检查是否可以删除场景
     */
    public boolean isSceneDeletable(Scene scene) {
        // 场景已经绑定
        if (modelApplicationRepo.countBySceneId(scene.getId()) != 0)
            return false;
        return true;
    }
    public List<Scene> getModel() {
        return sceneRepo.findScenesByIsDelete(Scene.NOT_DELETE);
    }

    /**
     * 根据Id查找scene
     */
    public Scene findModelById(long id) {
        return sceneRepo.findSceneBySceneId(id);
    }

    public List<Scene> isSceneNameExist(String sceneName) {
        return sceneRepo.findBySceneNameAndNotDeleted(sceneName);
    }

    // 添加或更新场景
    // 删了原来库里的唯一索引了，否则同名已删除场景会报错，其它方法都未用到唯一索引
    public Scene saveOrUpdate(Scene scene) {
        // 场景已经绑定
        if (modelApplicationRepo.countBySceneId(scene.getId()) != 0)
            throw new RuntimeException("Scene is binded, id: " + scene.getId());
        return sceneRepo.save(scene);
    }

    // 硬删除：根据ID删除场景
//    public void deleteById(Long id) {
//        sceneRepo.deleteById(id);
//    }

    // 软删除：将 isDelete 设置为 1
    public void deleteById(Long id) {
        Scene scene = sceneRepo.findById(id).orElseThrow(() -> new RuntimeException("Scene not found"));
        if (modelApplicationRepo.countBySceneId(id) != 0)
            throw new RuntimeException("Scene is binded, id: " + id);
        scene.setIsDelete(Scene.DELETE);  // 设置 isDelete 为 1
        scene.setName(scene.getName()+"(已删除)"+new Date().getTime());
        sceneRepo.save(scene);  // 保存更新后的场景
    }


    public List<Scene> findSceneListByIds(List<Long> ids) {
        return (List<Scene>) sceneRepo.findAllById(ids);
    }
}
