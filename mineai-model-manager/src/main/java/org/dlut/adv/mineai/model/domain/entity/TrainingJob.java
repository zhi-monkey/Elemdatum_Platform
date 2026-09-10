package org.dlut.adv.mineai.model.domain.entity;

public class TrainingJob {
    private String jobName;
    private String namespace;

    public TrainingJob(String jobName, String namespace) {
        this.jobName = jobName;
        this.namespace = namespace;
    }

    public String getJobName() {
        return jobName;
    }

    public String getNamespace() {
        return namespace;
    }
}
