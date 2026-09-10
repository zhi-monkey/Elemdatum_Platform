package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.Controller;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelControllerRepo extends PagingAndSortingRepository<Controller, Long>, JpaSpecificationExecutor<Controller> {

 List<Controller> findControllersByStatusAndStatusUsingAndArchitecture(int status , int statusUsing, String architecture);

 Controller findControllerById(long id);
}
