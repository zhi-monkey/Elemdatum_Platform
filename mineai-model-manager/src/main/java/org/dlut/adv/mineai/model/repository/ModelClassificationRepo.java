package org.dlut.adv.mineai.model.repository;

import org.dlut.adv.mineai.core.entity.ModelClassification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModelClassificationRepo extends JpaRepository<ModelClassification, Long> {

}
