package game.system;

import game.core.GamePanel;

import java.awt.Color;

public abstract class Upgrade {
    private final int id;
    private final String name;
    private final String description;
    private final Color color;

    public Upgrade(int id, String name, String description, Color color) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Color getColor() {
        return color;
    }

    public abstract void apply(GamePanel gp);
}
