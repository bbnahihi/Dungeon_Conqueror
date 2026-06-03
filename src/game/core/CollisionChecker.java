package game.core;

import game.entity.Entity;

import java.awt.Rectangle;

public class CollisionChecker {
    
    GamePanel gp;

    public CollisionChecker(GamePanel gp) {
        this.gp = gp;
    }

    public void checkTile(Entity entity) {
        if (entity.solidArea == null) {
            return;
        }

        // Check the four corners of the entity hitbox.
        int entityLeftWorldX = entity.x + entity.solidArea.x;
        int entityRightWorldX = entity.x + entity.solidArea.x + entity.solidArea.width;
        int entityTopWorldY = entity.y + entity.solidArea.y;
        int entityBottomWorldY = entity.y + entity.solidArea.y + entity.solidArea.height;

        int entityLeftCol = entityLeftWorldX / gp.tileSize;
        int entityRightCol = entityRightWorldX / gp.tileSize;
        int entityTopRow = entityTopWorldY / gp.tileSize;
        int entityBottomRow = entityBottomWorldY / gp.tileSize;

        if (entityLeftCol < 0) entityLeftCol = 0;
        if (entityRightCol >= gp.maxWorldCol) entityRightCol = gp.maxWorldCol - 1;
        if (entityTopRow < 0) entityTopRow = 0;
        if (entityBottomRow >= gp.maxWorldRow) entityBottomRow = gp.maxWorldRow - 1;

        int tileNum1 = gp.tileM.mapTileNum[entityLeftCol][entityTopRow];    
        int tileNum2 = gp.tileM.mapTileNum[entityRightCol][entityTopRow];   
        int tileNum3 = gp.tileM.mapTileNum[entityLeftCol][entityBottomRow]; 
        int tileNum4 = gp.tileM.mapTileNum[entityRightCol][entityBottomRow];

        if (tileNum1 == 1 || tileNum2 == 1 || tileNum3 == 1 || tileNum4 == 1) {
            entity.collisionOn = true;
        }
    }

    public boolean checkWallCollision(int targetX, int targetY, Rectangle hitbox) {
        
        if (hitbox == null) return false;

        int leftX = targetX + hitbox.x;
        int rightX = targetX + hitbox.x + hitbox.width;
        int topY = targetY + hitbox.y;
        int bottomY = targetY + hitbox.y + hitbox.height;

        int leftCol = leftX / gp.tileSize;
        int rightCol = rightX / gp.tileSize;
        int topRow = topY / gp.tileSize;
        int bottomRow = bottomY / gp.tileSize;

        if (leftCol < 0 || rightCol >= gp.maxWorldCol || topRow < 0 || bottomRow >= gp.maxWorldRow) {
            return true; 
        }

        int tileNum1 = gp.tileM.mapTileNum[leftCol][topRow];
        int tileNum2 = gp.tileM.mapTileNum[rightCol][topRow];
        int tileNum3 = gp.tileM.mapTileNum[leftCol][bottomRow];
        int tileNum4 = gp.tileM.mapTileNum[rightCol][bottomRow];

        // Missing tiles should not crash collision checks.
        if ((gp.tileM.tile[tileNum1] != null && gp.tileM.tile[tileNum1].collision) || 
            (gp.tileM.tile[tileNum2] != null && gp.tileM.tile[tileNum2].collision) || 
            (gp.tileM.tile[tileNum3] != null && gp.tileM.tile[tileNum3].collision) || 
            (gp.tileM.tile[tileNum4] != null && gp.tileM.tile[tileNum4].collision)) {
            return true; 
        }

        return false; 
    }
}
