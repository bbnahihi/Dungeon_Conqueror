package game.entity;

import game.core.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

public class Bullet extends Entity {
    
    double dx, dy; 
    GamePanel gp;
    public boolean isPlayerBullet; 
    public int damage; 
    
    public boolean alive = true; 
    double angle; 


    public Bullet(GamePanel gp, int startX, int startY, int targetX, int targetY, boolean isPlayerBullet, int damage) {
        this.gp = gp;
        this.x = startX;
        this.y = startY;
        this.speed = 15; 
        this.isPlayerBullet = isPlayerBullet; 
        this.damage = damage; 

        this.angle = Math.atan2(targetY - startY, targetX - startX);
        dx = speed * Math.cos(angle);
        dy = speed * Math.sin(angle);
        
        solidArea = new Rectangle(0, 0, 15, 15);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 15, 15);
    }

    public void update() {
        x += (int) dx;
        y += (int) dy;

        // Wall collision is checked before enemy collision.
        collisionOn = false;
        gp.cChecker.checkTile(this);
        
        if (collisionOn == true) {
            alive = false;
            return;
        }

        // Player bullets damage monsters.
        if (isPlayerBullet == true) {
            for (int i = 0; i < gp.monsterList.size(); i++) {
                Monster m = gp.monsterList.get(i);
                
                if (m != null && this.getBounds().intersects(m.getBounds())) {
                    
                    boolean isCrit = Math.random() < 0.25; 
                    int finalDamage = isCrit ? damage * 2 : damage;
                    
                    m.hp -= finalDamage; 
                    
                    Color textColor = isCrit ? Color.ORANGE : Color.WHITE;
                    String text = isCrit ? "CRIT -" + finalDamage : "-" + finalDamage;
                    gp.floatingTextList.add(new FloatingText(gp, m.x, m.y, text, textColor));
                    
                    m.stunCounter = 15;
                    
                    alive = false; 
                    break;
                }
            }
        }
        
        if (x < 0 || x > gp.maxWorldCol * gp.tileSize || y < 0 || y > gp.maxWorldRow * gp.tileSize) {
            alive = false;
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        if (isPlayerBullet && gp.player.classType == 0 && gp.player.arrowImage != null) {
            
            double arrowScale = 3; 
            int arrowWidth = (int) (gp.tileSize * arrowScale);
            int arrowHeight = (int) (gp.tileSize * arrowScale);
            
            java.awt.geom.AffineTransform oldTransform = g2.getTransform();
            g2.rotate(angle, screenX + 15/2, screenY + 15/2);
            g2.drawImage(gp.player.arrowImage, 
                         screenX + 15/2 - arrowWidth/2, 
                         screenY + 15/2 - arrowHeight/2, 
                         arrowWidth, arrowHeight, null);
            g2.setTransform(oldTransform);
            
        } else {
            if (isPlayerBullet) g2.setColor(Color.YELLOW);
            else g2.setColor(Color.MAGENTA);
            g2.fillOval(screenX, screenY, 15, 15); 
        }
    }
}
