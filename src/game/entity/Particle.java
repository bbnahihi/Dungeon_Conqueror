package game.entity;

import game.core.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;

public class Particle extends Entity {
    
    GamePanel gp;

    Color color;
    int size;
    double xVel, yVel;
    int life;
    int maxLife;

    public Particle(GamePanel gp, int x, int y, Color color, int size, double xVel, double yVel, int life) {
        this.gp = gp;

        this.x = x;
        this.y = y;
        this.color = color;
        this.size = size;
        this.xVel = xVel;
        this.yVel = yVel;
        this.life = life;
        this.maxLife = life;
    }

    public void update() {
        x += xVel;
        y += yVel;
        life--;
    }

    public boolean isExpired() {
        return life <= 0;
    }

    public void draw(Graphics2D g2) {
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        float opacity = (float) life / maxLife;
        if (opacity < 0) opacity = 0;
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2.setColor(color);
        g2.fillRect(screenX, screenY, size, size);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}
