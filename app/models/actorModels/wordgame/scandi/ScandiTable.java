package models.actorModels.wordgame.scandi;

import models.actorModels.wordgame.wordtable.SelectTable;

import javax.persistence.*;

@Entity
public class ScandiTable {
    @Id
    @GeneratedValue
    private long id;

    private String name;
    private String topic;

    @Column(columnDefinition="TEXT")
    private String jsonTable;

    @Transient
    private boolean error = false;
    @Transient
    private String errorMesage;

    public ScandiTable() {
    }

    public ScandiTable(String jsonTable) {
        this.jsonTable = jsonTable;
    }

    public ScandiTable(boolean error, String errorMesage  ) {
        this.error = error;
        this.errorMesage = errorMesage;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public String getErrorMesage() {
        return errorMesage;
    }

    public long getId() {
        return id;
    }

    public String getJsonTable() {
        return jsonTable;
    }

    public void setJsonTable(String jsonTable) {
        this.jsonTable = jsonTable;
    }

    public boolean isError() {
        return error;
    }

    public SelectTable getMessage() {
        if(error) {
            return new SelectTable(-1, errorMesage);
        }
        return new SelectTable(id, name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

}
