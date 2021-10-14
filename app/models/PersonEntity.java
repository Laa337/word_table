package model;

import javax.persistence.Entity;

@Entity
public class PersonEntity extends MainEntity{
    private Integer age;

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}
