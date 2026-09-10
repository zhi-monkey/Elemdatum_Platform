package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.PublishedHyperParams;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @package: org.dlut.adv.mineai.model.repository
 * @author: chystart
 * @create: 2024-10-23 19:24
 * @description: 发布超参实体信息
 **/
public interface PublishHyperParamsRepo extends PagingAndSortingRepository<PublishedHyperParams, Long>, JpaSpecificationExecutor<PublishedHyperParams> {

}
