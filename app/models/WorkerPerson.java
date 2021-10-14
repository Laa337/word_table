package model;

import javax.persistence.Entity;

@Entity
public class WorkerPerson extends Person {
    private String job;

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }
}
