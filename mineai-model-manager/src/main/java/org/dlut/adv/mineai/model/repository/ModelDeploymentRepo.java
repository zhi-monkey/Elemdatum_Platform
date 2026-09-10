package org.dlut.adv.mineai.model.repository;


import org.dlut.adv.mineai.core.entity.Controller;
import org.dlut.adv.mineai.core.entity.Deployment;
import org.dlut.adv.mineai.core.entity.ModelVersion;
import org.dlut.adv.mineai.core.entity.Monitor;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * @author Gaozixuan
 */
public interface ModelDeploymentRepo extends PagingAndSortingRepository<Deployment, Long>, JpaSpecificationExecutor<Deployment> {

    /**
     * 查找全部部署信息
     */
    List<Deployment> findAll();

    /**
     * 根据id查找部署信息
     *
     * @param id id
     * @return Deployment
     */
    Deployment findDeploymentById(long id);

    List<Deployment> findDeploymentsByStatus(int status);

    /**
     * 根据三个关键属性来查找部署信息
     *
     * @param modelVersion modelVersion
     * @param controller   controller
     * @param monitor      monitor
     * @return Deployment
     */
    Deployment findDeploymentByModelVersionAndControllerAndMonitor(ModelVersion modelVersion, Controller controller, Monitor monitor);

    /**
     * 根据modelVersion查找部署信息
     *
     * @param modelVersion 算法镜像
     * @return List<Deployment>
     */
    List<Deployment> findDeploymentsByModelVersion(ModelVersion modelVersion);

    /**
     * 根据Monitor查找部署信息
     *
     * @param monitor 摄像头
     * @return List<Deployment>
     */
    List<Deployment> findDeploymentsByMonitor(Monitor monitor);

    /**
     * 根据Controller查找部署信息
     *
     * @param controller 控制器节点
     * @return List<Deployment>
     */
    List<Deployment> findDeploymentsByController(Controller controller);


}
