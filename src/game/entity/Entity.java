package game.entity;

import java.awt.Rectangle;
import java.awt.image.BufferedImage; 

public class Entity {
    public int x, y;      
    public int speed;     
    public Rectangle solidArea; 
    public boolean collisionOn = false;
    public boolean isGhost = false;
    public BufferedImage image;
    public BufferedImage image1, image2;
    
    public int spriteCounter = 0;
    public int spriteNum = 1;
}
