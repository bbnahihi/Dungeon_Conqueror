package game.entity;

import game.core.GamePanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Item {

    public static final int TYPE_HEART = 0;
    public static final int TYPE_COIN = 1;
    public static final int TYPE_ENERGY = 2;
    public static final int TYPE_SHIELD = 3;

    GamePanel gp;
    public int x;
    public int y;
    public int type;
    public boolean alive = true;

    private final int size = 28;
    private int animationCounter = 0;
    private Rectangle solidArea = new Rectangle(0, 0, size, size);

    public Item(GamePanel gp, int x, int y, int type) {
        this.gp = gp;
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public Rectangle getBounds() {
        return new Rectangle(x + solidArea.x, y + solidArea.y, solidArea.width, solidArea.height);
    }

    public void update() {
        animationCounter++;

        if (getBounds().intersects(gp.player.getBounds())) {
            applyEffect();
            gp.statsTracker.recordItemCollected();
            alive = false;
        }
    }

    private void applyEffect() {
        if (type == TYPE_HEART) {
            int oldHp = gp.player.hp;
            gp.player.hp += 1;
            if (gp.player.hp > gp.player.maxHp) {
                gp.player.hp = gp.player.maxHp;
            }

            if (gp.player.hp > oldHp) {
                gp.floatingTextList.add(new FloatingText(gp, x, y, "+" + (gp.player.hp - oldHp) + " HP", Color.PINK));
            } else {
                gp.floatingTextList.add(new FloatingText(gp, x, y, "HP FULL", Color.PINK));
            }
        }
        else if (type == TYPE_COIN) {
            gp.addScore(50);
            gp.floatingTextList.add(new FloatingText(gp, x, y, "+50", Color.YELLOW));
        }
        else if (type == TYPE_ENERGY) {
            gp.player.skillCooldown -= 120;
            if (gp.player.skillCooldown < 0) {
                gp.player.skillCooldown = 0;
            }
            gp.floatingTextList.add(new FloatingText(gp, x, y, "SKILL +", Color.CYAN));
        }
        else if (type == TYPE_SHIELD) {
            gp.player.invincible = true;
            gp.player.invincibleCounter = -60; // Khoảng 2 giây bất tử ở 60 FPS
            gp.floatingTextList.add(new FloatingText(gp, x, y, "SHIELD", Color.GREEN));
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        int bob = (int) (Math.sin(animationCounter / 10.0) * 4);
        int drawY = screenY + bob;

        Color itemColor;
        String label;

        if (type == TYPE_HEART) {
            itemColor = Color.PINK;
            label = "+";
        }
        else if (type == TYPE_COIN) {
            itemColor = Color.YELLOW;
            label = "$";
        }
        else if (type == TYPE_ENERGY) {
            itemColor = Color.CYAN;
            label = "E";
        }
        else {
            itemColor = Color.GREEN;
            label = "S";
        }

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillOval(screenX + 2, drawY + size - 4, size - 4, 8);

        g2.setColor(itemColor);
        g2.fillOval(screenX, drawY, size, size);

        g2.setColor(Color.WHITE);
        g2.drawOval(screenX, drawY, size, size);
        g2.setFont(new Font("Arial", Font.BOLD, 16));

        int textWidth = (int) g2.getFontMetrics().getStringBounds(label, g2).getWidth();
        g2.drawString(label, screenX + size / 2 - textWidth / 2, drawY + 20);
    }
}
