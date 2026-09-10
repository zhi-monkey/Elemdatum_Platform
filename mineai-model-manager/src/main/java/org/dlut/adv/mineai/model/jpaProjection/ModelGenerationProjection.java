package org.dlut.adv.mineai.model.jpaProjection;

public interface ModelGenerationProjection {
    Long getId();
    String getName();
    String getDescription();
    Long getDatasetId();
    String getDatasetName();
    Boolean getIsGuided();
    default Long getDatasetVersionId() { return null; }
}
