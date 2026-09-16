package ch.samt.qualityfitness.model;

public class Template {
    private final int id;
    private final String name;

    public Template(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
}