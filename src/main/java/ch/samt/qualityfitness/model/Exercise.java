package ch.samt.qualityfitness.model;

public class Exercise {

    private final int id;
    private final String name;
    private final String description;
    private final String type;

    public Exercise(int id, String name, String description, String type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getType() { return type; }
}