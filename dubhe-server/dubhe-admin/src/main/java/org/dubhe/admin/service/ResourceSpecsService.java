
package org.dubhe.admin.service;

import org.dubhe.admin.domain.dto.*;
import org.dubhe.biz.base.vo.QueryResourceSpecsVO;
import org.dubhe.biz.base.dto.QueryResourceSpecsDTO;

import java.util.List;
import java.util.Map;

/**
 * @description CPU, GPU, 内存等资源规格管理
 * @date 2021-05-27
 */
public interface ResourceSpecsService {

    /**
     *  查询资源规格
     * @param resourceSpecsQueryDTO 查询资源规格请求实体
     * @return List<ResourceSpecs> resourceSpecs 资源规格列表
     */
    Map<String, Object> getResourceSpecs(ResourceSpecsQueryDTO resourceSpecsQueryDTO);

    /**
     *  新增资源规格
     * @param resourceSpecsCreateDTO  新增资源规格实体
     * @return List<Long> 新增资源规格id
     */
    List<Long> create(ResourceSpecsCreateDTO resourceSpecsCreateDTO);

    /**
     *  修改资源规格
     * @param resourceSpecsUpdateDTO  修改资源规格实体
     * @return List<Long> 修改资源规格id
     */
    List<Long> update(ResourceSpecsUpdateDTO resourceSpecsUpdateDTO);

    /**
     *  资源规格删除
     * @param resourceSpecsDeleteDTO 资源规格删除id集合
     */
    void delete(ResourceSpecsDeleteDTO resourceSpecsDeleteDTO);

    /**
     * 查询资源规格
     * @param queryResourceSpecsDTO 查询资源规格请求实体
     * @return QueryResourceSpecsVO 资源规格返回结果实体类
     */
    QueryResourceSpecsVO queryResourceSpecs(QueryResourceSpecsDTO queryResourceSpecsDTO);

    /**
     * 查询资源规格
     * @param id 资源规格id
     * @return QueryResourceSpecsVO 资源规格返回结果实体类
     */
    QueryResourceSpecsVO queryTadlResourceSpecs(Long id);
}