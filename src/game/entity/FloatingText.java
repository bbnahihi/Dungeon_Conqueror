package game.entity;

import game.core.GamePanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class FloatingText {
    GamePanel gp;
    int worldX, worldY;
    String text;
    Color color;
    
    int life;
    int maxLife = 40;

    public FloatingText(GamePanel gp, int worldX, int worldY, String text, Color color) {
        this.gp = gp;
        // Small offset keeps numbers from stacking exactly.
        this.worldX = worldX + (int)(Math.random() * 20 - 10);
        this.worldY = worldY + (int)(Math.random() * 20 - 10);
        this.text = text;
        this.color = color;
        this.life = maxLife;
    }

    public void update() {
        worldY -= 1;
        life--;
    }

    public boolean isExpired() {
        return life <= 0;
    }

    public void draw(Graphics2D g2) {
        int screenX = worldX - gp.player.x + gp.player.screenX;
        int screenY = worldY - gp.player.y + gp.player.screenY;

        int alpha = (int) (255 * ((double) life / maxLife));
        if (alpha < 0) alpha = 0;

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.drawString(text, screenX + 2, screenY + 2);

        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2.drawString(text, screenX, screenY);
    }
}
