package models.actorModels.wordgame.wordtable;

public class SelectTable {
    private long id;
    private String name;

    public SelectTable(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public SelectTable() {
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
