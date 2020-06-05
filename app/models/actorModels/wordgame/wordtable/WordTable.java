package models.actorModels.wordgame.wordtable;

import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class WordTable {

    @Id
    @GeneratedValue
    private long id;

    @Column(columnDefinition="TEXT")
    private String jsonTable;

    @CreationTimestamp
    private LocalDateTime createdDate;

    private String name;
    private String topic;

    @Transient
    private boolean error = false;

    @Transient
    private String errorMesage;

    public WordTable() {
    }

    public WordTable(String table) {
        this.jsonTable = table;
    }

    public WordTable(boolean error, String errorMesage) {
        this.error = error;
        this.errorMesage = errorMesage;
    }

    public long getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public boolean isError() {
        return error;
    }

    public String getJsonTable() {
        return jsonTable;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public String getName() {
        return name;
    }

    public String getErrorMesage() {
        return errorMesage;
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
