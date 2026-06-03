package game.entity;

import game.core.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Monster extends Entity {
    
    GamePanel gp;
    public int hp; 
    public int maxHp;
    public int type; // 1 = melee, 2 = ranged
    public int shootCooldown = 0; 
    
    private boolean lookLeft = false;

    public boolean isElite = false;
    public int stunCounter = 0;
    public int escapeCounter = 0;
    public int escapeDirX = 0;
    public int escapeDirY = 0;
    public BossState currentState;
    

    public interface BossState {
    void enter(Monster boss);
    void update(Monster boss);
    void exit(Monster boss);
    }
    public Monster(GamePanel gp, int startX, int startY, int type) {
        this.gp = gp;
        this.x = startX; 
        this.y = startY; 
        this.type = type;

        // Stats and hitboxes depend on monster type.
        if (type == 1) {
            this.speed = 3;
            this.maxHp = 3;
            this.solidArea = new Rectangle(8, 8, 32, 32);
        } else if (type == 2) {
            this.speed = 2; 
            this.maxHp = 2; 
            this.solidArea = new Rectangle(8, 8, 32, 32);   
        } else if (type == 3) {
            this.speed = 4;
            this.maxHp = 500; 
            this.solidArea = new Rectangle(16, 16, 64, 64);
        }
        if (type == 3 && currentState != null) {
        currentState.update(this);
}       
        this.hp = this.maxHp; 
        
        getMonsterImage();
    }

    public void getMonsterImage() {
        try {
            if (type == 1) {
                image1 = ImageIO.read(getClass().getResourceAsStream("/res/monster_1_1.png"));
                image2 = ImageIO.read(getClass().getResourceAsStream("/res/monster_1_2.png"));
            }
            if (type == 2) {
                image1 = ImageIO.read(getClass().getResourceAsStream("/res/monster_2_1.png"));
                image2 = ImageIO.read(getClass().getResourceAsStream("/res/monster_2_2.png"));
            }
            else if (type == 3) {
                image1 = ImageIO.read(getClass().getResourceAsStream("/res/boss/boss1.png"));
                image2 = ImageIO.read(getClass().getResourceAsStream("/res/boss/boss2.png"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        
        // Hit stun from melee attacks.
        if (stunCounter > 0) {
            stunCounter--;
            return;
        }

        int playerCenterX = gp.player.x + 24;
        int playerCenterY = gp.player.y + 24;
        
        int size = (type == 3) ? gp.tileSize * 2 : gp.tileSize; 
        int monsterCenterX = x + (size / 2);
        int monsterCenterY = y + (size / 2);
        
        double distance = Math.sqrt(Math.pow(playerCenterX - monsterCenterX, 2) + Math.pow(playerCenterY - monsterCenterY, 2));

        // 8-way A* movement.
        if (distance > size / 2) {
            int oldX = x;
            int oldY = y;

            int mCenterX = x + solidArea.x + solidArea.width / 2;
            int mCenterY = y + solidArea.y + solidArea.height / 2;
            int pCenterX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width / 2;
            int pCenterY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;

            if (pCenterX - mCenterX < 0) lookLeft = true;
            else if (pCenterX - mCenterX > 0) lookLeft = false;

            int startCol = mCenterX / gp.tileSize;
            int startRow = mCenterY / gp.tileSize;
            int goalCol = pCenterX / gp.tileSize;
            int goalRow = pCenterY / gp.tileSize;

            if (startCol < 0) startCol = 0; if (startCol >= gp.maxWorldCol) startCol = gp.maxWorldCol - 1;
            if (startRow < 0) startRow = 0; if (startRow >= gp.maxWorldRow) startRow = gp.maxWorldRow - 1;
            if (goalCol < 0) goalCol = 0; if (goalCol >= gp.maxWorldCol) goalCol = gp.maxWorldCol - 1;
            if (goalRow < 0) goalRow = 0; if (goalRow >= gp.maxWorldRow) goalRow = gp.maxWorldRow - 1;

            gp.pFinder.setNodes(startCol, startRow, goalCol, goalRow);
            gp.pFinder.setNodeSolid(goalCol, goalRow, false); 

            boolean pathFound = gp.pFinder.search();
            int targetX = pCenterX;
            int targetY = pCenterY;

            if (pathFound == true && gp.pFinder.pathList.size() > 0) {
                int step = 0;
                // Skip the current tile so movement does not stutter.
                if (gp.pFinder.pathList.size() > 1 && gp.pFinder.pathList.get(0).col == startCol && gp.pFinder.pathList.get(0).row == startRow) {
                    step = 1;
                }
                targetX = gp.pFinder.pathList.get(step).col * gp.tileSize + gp.tileSize / 2;
                targetY = gp.pFinder.pathList.get(step).row * gp.tileSize + gp.tileSize / 2;
            }

            int moveX = targetX - mCenterX;
            int moveY = targetY - mCenterY;

            if (Math.abs(moveX) > 0) {
                int currentSpeed = (Math.abs(moveX) < speed) ? Math.abs(moveX) : speed;
                x += (moveX > 0) ? currentSpeed : -currentSpeed;
                collisionOn = false; gp.cChecker.checkTile(this);
                if (collisionOn) x = oldX; 
            }

            if (Math.abs(moveY) > 0) {
                int currentSpeed = (Math.abs(moveY) < speed) ? Math.abs(moveY) : speed;
                y += (moveY > 0) ? currentSpeed : -currentSpeed;
                collisionOn = false; gp.cChecker.checkTile(this);
                if (collisionOn) y = oldY;
            }
        }     
        spriteCounter++;
        if (spriteCounter > 15) {
            if (spriteNum == 1) {
                spriteNum = 2;
            } else if (spriteNum == 2) {
                spriteNum = 1;
            }
            spriteCounter = 0; 
        }

        // Ranged monsters shoot at the player.
        if (type == 2) {
            if (shootCooldown == 0) {
                int startX = x + gp.tileSize / 2;
                int startY = y + gp.tileSize / 2;
                int targetX = gp.player.x + gp.tileSize / 2;
                int targetY = gp.player.y + gp.tileSize / 2;
                
                Bullet b = new Bullet(gp, startX, startY, targetX, targetY, false, 1);
                gp.bulletList.add(b);
                
                // Ranged monsters shoot faster on later levels.
                shootCooldown = gp.difficulty.applyRangedCooldown(Math.max(35, 75 - gp.currentLevel * 4)); 
            }
            if (shootCooldown > 0) shootCooldown--;
        }
        
        // Final boss phase logic.
        else if (type == 3) {
            // Phase 2 starts at half HP.
            boolean isEnraged = (this.hp <= this.maxHp / 2);

            this.speed = gp.difficulty.getBossSpeed(isEnraged);

            if (shootCooldown == 0) {
                int startX = x + gp.tileSize;
                int startY = y + gp.tileSize;
                
                if (isEnraged) {
                    // Phase 2 can summon minions.
                    if (Math.random() < gp.difficulty.applyBossMinionChance(0.50)) {
                        Monster minion = new Monster(gp, x + gp.tileSize, y + gp.tileSize, 1);
                        gp.difficulty.applyMonsterStats(minion);
                        if (minion.isElite == false) minion.hp = minion.maxHp;
                        gp.monsterList.add(minion);
                    }
                    
                    // Phase 2 fires in 12 directions.
                    for(int i = 0; i < 12; i++) {
                        double angle = Math.PI / 6 * i;
                        int tX = (int) (startX + Math.cos(angle) * 100);
                        int tY = (int) (startY + Math.sin(angle) * 100);
                        
                        Bullet b = new Bullet(gp, startX, startY, tX, tY, false, 3);
                        gp.bulletList.add(b);
                    }
                    shootCooldown = gp.difficulty.getBossShootCooldown(true);
                    
                } else {
                    // Phase 1 uses a smaller 8-way shot.
                    for(int i = 0; i < 8; i++) {
                        double angle = Math.PI / 4 * i; 
                        int tX = (int) (startX + Math.cos(angle) * 100);
                        int tY = (int) (startY + Math.sin(angle) * 100);
                        
                        Bullet b = new Bullet(gp, startX, startY, tX, tY, false, 1);
                        gp.bulletList.add(b);
                    }
                    shootCooldown = gp.difficulty.getBossShootCooldown(false);
                }
            }
            if (shootCooldown > 0) shootCooldown--;
        }
    }

    public void draw(Graphics2D g2) {
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        BufferedImage imageToDraw = null;
        if (spriteNum == 1) imageToDraw = image1;
        if (spriteNum == 2) imageToDraw = image2;

        int size = (type == 3) ? gp.tileSize * 2 : gp.tileSize;

        int drawX = screenX;
        int drawY = screenY;
        int drawWidth = size;
        int drawHeight = size;

        if (lookLeft == true) {
            drawX = screenX + drawWidth;
            drawWidth = -drawWidth; 
        }

        // Skip monsters outside the camera view.
        if (x + size > gp.player.x - gp.player.screenX &&
            x - size < gp.player.x + gp.player.screenX &&
            y + size > gp.player.y - gp.player.screenY &&
            y - size < gp.player.y + gp.player.screenY) {
            
            if (imageToDraw != null) {
                g2.drawImage(imageToDraw, drawX, drawY, drawWidth, drawHeight, null);
            if (isElite == true) {
                g2.setColor(new Color(255, 0, 0, 100));
                g2.fillOval(screenX, screenY + gp.tileSize / 2, gp.tileSize, gp.tileSize / 2);
            }
            } else {
                if (type == 1) g2.setColor(Color.RED);
                else if (type == 2) g2.setColor(Color.MAGENTA);
                else if (type == 3) g2.setColor(Color.ORANGE); 
                
                g2.fillRect(screenX, screenY, size, size); 
            }
        }
    }

    // Hitbox uses world coordinates for bullet checks.
    public Rectangle getBounds() {
        int size = (type == 3) ? gp.tileSize * 2 : gp.tileSize;
        return new Rectangle(x, y, size, size);
    }

    // Elite monsters are faster and tougher.
    public void transformToElite() {
        this.isElite = true;
        this.speed = 5;        // Slightly faster than the player.
        this.maxHp += 5;       // More HP.
        this.hp = this.maxHp;  // Heal after transforming.
        
        try {
            
        } catch (Exception e) {
            System.out.println("Error: Elite image file not found!");
            e.printStackTrace();
        }
    }
    public void changeState(BossState newState) {
    if (currentState != null) currentState.exit(this);
    currentState = newState;
    currentState.enter(this);
}
}
