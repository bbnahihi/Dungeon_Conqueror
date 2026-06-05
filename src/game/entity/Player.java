package game.entity;

import game.core.GamePanel;
import game.input.KeyHandler;
import game.input.MouseHandler;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.AlphaComposite; 
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO; 

public class Player extends Entity {
    
    GamePanel gp;
    KeyHandler keyH;
    MouseHandler mouseH; 
    
    int shootCooldown = 0; 
    public double meleeAttackAngle = 0;
    
    public int maxHp = 8; 
    public int hp = maxHp; 
    
    public boolean invincible = false;
    public int invincibleCounter = 0;

    public int classType = 0; // 0 = ranger, 1 = swordsman
    public int gunDamage; 
    public int meleeDamage; 

    public boolean isMeleeAttacking = false;
    public int meleeAttackCounter = 0;
    public Rectangle meleeHitbox = new Rectangle(0, 0, 0, 0); 
    private Set<Monster> hitMonstersThisSwing = new HashSet<>();
    private Set<Bullet> parriedBulletsThisSwing = new HashSet<>();
    
    public boolean isShooting = false;
    public int shootAttackCounter = 0;

    public int skillCooldown = 0; 
    public int skillMaxCooldown = 600; 

    
    public double scaleFactor = 5.0;
    public int drawWidth, drawHeight, offset;

    public final int screenX;
    public final int screenY;
    public int attackCooldown; 

    public boolean doubleShot = false;     
    public int ultiBulletCount = 24;       
    public int meleeRangeBonus = 0;        
    public int meleeWidthBonus = 0;
    public boolean swordReflectBullets = false;
    public double meleeAngleBonus = 0;     

    public boolean isBladeRushActive = false;
    private int bladeRushCounter = 0;
    private double bladeRushAngle = 0;
    private Set<Monster> hitMonstersThisBladeRush = new HashSet<>();
    private Set<Bullet> parriedBulletsThisBladeRush = new HashSet<>();

    private static final boolean DEBUG_SWORD_HITBOX = false;
    private static final int SWORD_BASE_REACH = 80;
    private static final int SWORD_BASE_WIDTH = 42;
    private static final int SWORD_SLASH_FRAME_COUNT = 6;
    private static final int SWORD_SLASH_FRAME_WIDTH = 96;
    private static final int SWORD_SLASH_FRAME_HEIGHT = 64;
    private static final int SWORD_IMAGE_WIDTH = 128;
    private static final int SWORD_IMAGE_HEIGHT = 32;
    private static final int SWORD_IMAGE_PIVOT_X = 24;
    private static final int SWORD_IMAGE_PIVOT_Y = 16;
    private static final double[] SWORD_FRAME_LOCAL_ANGLE_DEGREES = {-70.0, -35.0, -10.0, 0.0, 35.0, 45.0};
    private static final int BLADE_RUSH_DURATION = 14;
    private static final int BLADE_RUSH_SPEED = 11;
    private static final int BLADE_RUSH_DAMAGE = 10;
    private static final int BLADE_RUSH_WIDTH = 56;
    private static final int BLADE_RUSH_STUN = 30;
    private static final int BLADE_RUSH_AFTERIMAGE_INTERVAL = 2;
    private static final int BLADE_RUSH_AFTERIMAGE_LIFE = 12;
    private static final int BLADE_RUSH_AFTERIMAGE_MAX = 10;

    private static class DashAfterimage {
        int worldX;
        int worldY;
        BufferedImage image;
        boolean facingLeft;
        int life;
        int maxLife;
    }

    private java.util.ArrayList<DashAfterimage> bladeRushAfterimages = new java.util.ArrayList<>();

    public BufferedImage[] idleFrames;
    public BufferedImage[] walkFrames;
    public BufferedImage[] attackFrames;
    public int spriteNum = 0;
    public int animationState = 0;
    public BufferedImage[] slashVFX;
    private BufferedImage swordsmanSwordImage;
    private BufferedImage swordsmanSlashSheet;
    private BufferedImage[] swordsmanSlashFrames;
    private boolean warnedSwordsmanSwordAssetMissing = false;
    private boolean warnedSwordsmanSlashAssetMissing = false;
    public BufferedImage arrowImage;
    
    public Player(GamePanel gp, KeyHandler keyH, MouseHandler mouseH) {
        this.gp = gp;
        this.keyH = keyH;
        this.mouseH = mouseH; 
        
        screenX = gp.screenWidth / 2 - (gp.tileSize / 2);
        screenY = gp.screenHeight / 2 - (gp.tileSize / 2);

        setDefaultValues();
    }

    // Load sprites for the selected class.
    public void getPlayerImage() {
        
        try {
            BufferedImage idleSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Idle.png"));
            BufferedImage walkSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Walk.png"));
            BufferedImage attackSheet = null;

            if (classType == 0) {
                attackSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Attack03.png"));
                attackFrames = new BufferedImage[9];

                // Try both arrow file names.
                try {
                    java.io.InputStream arrowStream = getClass().getResourceAsStream("/res/player/Arrow.png");
                    if (arrowStream == null) arrowStream = getClass().getResourceAsStream("/res/player/arrow.png");
                    if (arrowStream != null) arrowImage = ImageIO.read(arrowStream);
                } catch (Exception e) {
                    arrowImage = null;
                }
            } 
            else if (classType == 1) {
                attackSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Attack01.png"));
                attackFrames = new BufferedImage[6];
                loadSwordsmanMeleeAssets();
            }
            
            idleFrames = new BufferedImage[6]; 
            walkFrames = new BufferedImage[8]; 

            // Slice each sheet by its frame count.
            int idleFw = idleSheet.getWidth() / 6; 
            for (int i = 0; i < 6; i++) idleFrames[i] = idleSheet.getSubimage(i * idleFw, 0, idleFw, idleSheet.getHeight());

            int walkFw = walkSheet.getWidth() / 8;
            for (int i = 0; i < 8; i++) walkFrames[i] = walkSheet.getSubimage(i * walkFw, 0, walkFw, walkSheet.getHeight());

            int attackFw = attackSheet.getWidth() / attackFrames.length;
            for (int i = 0; i < attackFrames.length; i++) attackFrames[i] = attackSheet.getSubimage(i * attackFw, 0, attackFw, attackSheet.getHeight());
            
            slashVFX = new BufferedImage[10];
            for (int i = 0; i < 10; i++) {
                String fileName = String.format("/res/player/slash5-animation_%02d.png", i + 1);
                slashVFX[i] = ImageIO.read(getClass().getResourceAsStream(fileName));
            }
        } catch (Exception e) {
            System.out.println("ERROR: Could not load or slice the sprite sheet!");
            e.printStackTrace();
        }
    }

    private void loadSwordsmanMeleeAssets() {
        swordsmanSwordImage = null;
        swordsmanSlashSheet = null;
        swordsmanSlashFrames = null;

        try (java.io.InputStream swordStream = getClass().getResourceAsStream("/res/player/swordsman_sword.png")) {
            if (swordStream == null) {
                warnSwordsmanSwordAsset("Warning: missing /res/player/swordsman_sword.png; using code-drawn sword fallback.");
            } else {
                swordsmanSwordImage = ImageIO.read(swordStream);
                if (swordsmanSwordImage == null) {
                    warnSwordsmanSwordAsset("Warning: could not read /res/player/swordsman_sword.png; using code-drawn sword fallback.");
                }
            }
        } catch (Exception e) {
            warnSwordsmanSwordAsset("Warning: could not load /res/player/swordsman_sword.png; using code-drawn sword fallback.");
            swordsmanSwordImage = null;
        }

        try (java.io.InputStream slashStream = getClass().getResourceAsStream("/res/player/swordsman_slash_sheet.png")) {
            if (slashStream == null) {
                warnSwordsmanSlashAsset("Warning: missing /res/player/swordsman_slash_sheet.png; using code-drawn slash fallback.");
            } else {
                swordsmanSlashSheet = ImageIO.read(slashStream);
                if (swordsmanSlashSheet == null) {
                    warnSwordsmanSlashAsset("Warning: could not read /res/player/swordsman_slash_sheet.png; using code-drawn slash fallback.");
                } else {
                    sliceSwordsmanSlashSheet();
                }
            }
        } catch (Exception e) {
            warnSwordsmanSlashAsset("Warning: could not load /res/player/swordsman_slash_sheet.png; using code-drawn slash fallback.");
            swordsmanSlashSheet = null;
            swordsmanSlashFrames = null;
        }
    }

    private void sliceSwordsmanSlashSheet() {
        int requiredWidth = SWORD_SLASH_FRAME_WIDTH * SWORD_SLASH_FRAME_COUNT;
        if (swordsmanSlashSheet.getWidth() < requiredWidth || swordsmanSlashSheet.getHeight() < SWORD_SLASH_FRAME_HEIGHT) {
            warnSwordsmanSlashAsset("Warning: /res/player/swordsman_slash_sheet.png is too small; using code-drawn slash fallback.");
            swordsmanSlashFrames = null;
            return;
        }

        swordsmanSlashFrames = new BufferedImage[SWORD_SLASH_FRAME_COUNT];
        for (int i = 0; i < SWORD_SLASH_FRAME_COUNT; i++) {
            swordsmanSlashFrames[i] = swordsmanSlashSheet.getSubimage(
                    i * SWORD_SLASH_FRAME_WIDTH,
                    0,
                    SWORD_SLASH_FRAME_WIDTH,
                    SWORD_SLASH_FRAME_HEIGHT);
        }
    }

    private void warnSwordsmanSwordAsset(String message) {
        if (warnedSwordsmanSwordAssetMissing == false) {
            System.out.println(message);
            warnedSwordsmanSwordAssetMissing = true;
        }
    }

    private void warnSwordsmanSlashAsset(String message) {
        if (warnedSwordsmanSlashAssetMissing == false) {
            System.out.println(message);
            warnedSwordsmanSlashAssetMissing = true;
        }
    }

    public void setDefaultValues() {
        x = gp.tileSize * 15; 
        y = gp.tileSize * 15;
        maxHp = 8;
        hp = maxHp;
        speed = 4;
        solidArea = new Rectangle(8, 16, 32, 32); 
        
        setupClass(0); 
    }

    public void setupClass(int type) {
        this.classType = type;
        
        doubleShot = false;
        ultiBulletCount = 24;
        meleeRangeBonus = 0;
        meleeWidthBonus = 0;
        swordReflectBullets = false;
        meleeAngleBonus = 0;
        resetSwordCombatState();
        
        if (type == 0) {
            maxHp = 8;
            speed = 4;
            gunDamage = 1;
            attackCooldown = 25; 
        } 
        else if (type == 1) {
            maxHp = 11;
            speed = 5;
            meleeDamage = 3;
            attackCooldown = 25;
        }
        
        hp = maxHp; 
        getPlayerImage();
    }

    public void resetSwordCombatState() {
        resetNormalMeleeAttack();
        resetBladeRush();
    }

    private void resetNormalMeleeAttack() {
        isMeleeAttacking = false;
        meleeAttackCounter = 0;
        meleeHitbox.setBounds(0, 0, 0, 0);
        hitMonstersThisSwing.clear();
        parriedBulletsThisSwing.clear();
    }

    private void resetBladeRush() {
        isBladeRushActive = false;
        bladeRushCounter = 0;
        hitMonstersThisBladeRush.clear();
        parriedBulletsThisBladeRush.clear();
        bladeRushAfterimages.clear();
    }

    private int getMeleeAttackFrameIndex() {
        int frameCount = 6;
        if (attackFrames != null && attackFrames.length > 0) {
            frameCount = attackFrames.length;
        }

        double progress = (double) meleeAttackCounter / Math.max(1, attackCooldown);
        int attackFrameIndex;

        if (progress < 0.15) {
            attackFrameIndex = 0;
        } else if (progress < 0.60) {
            double swingProgress = (progress - 0.15) / 0.45;
            attackFrameIndex = 1 + (int)(swingProgress * (frameCount - 2));
        } else {
            attackFrameIndex = frameCount - 1;
        }

        if (attackFrameIndex < 0) attackFrameIndex = 0;
        if (attackFrameIndex >= frameCount) attackFrameIndex = frameCount - 1;
        return attackFrameIndex;
    }

    private boolean isMeleeAttackActiveFrame() {
        int frameIndex = getMeleeAttackFrameIndex();
        return frameIndex == 3 || frameIndex == 4;
    }

    public boolean isSwordParryActive() {
        if (classType != 1 || isMeleeAttacking == false) return false;

        int frameIndex = getMeleeAttackFrameIndex();
        return frameIndex >= 2 && frameIndex <= 5;
    }

    private int getSwordReach() {
        return SWORD_BASE_REACH + meleeRangeBonus;
    }

    private int getSwordWidth() {
        return SWORD_BASE_WIDTH + meleeWidthBonus;
    }

    private double getSwordAttackAngle() {
        if (isBladeRushActive == true) {
            return bladeRushAngle;
        }

        int frameIndex = getMeleeAttackFrameIndex();
        if (frameIndex < 0) frameIndex = 0;
        if (frameIndex >= SWORD_FRAME_LOCAL_ANGLE_DEGREES.length) frameIndex = SWORD_FRAME_LOCAL_ANGLE_DEGREES.length - 1;

        return meleeAttackAngle + Math.toRadians(SWORD_FRAME_LOCAL_ANGLE_DEGREES[frameIndex]);
    }

    private int getSwordPivotWorldX() {
        double angle = getSwordAttackAngle();
        double forwardX = Math.cos(angle);
        double forwardY = Math.sin(angle);
        double sideX = -forwardY;

        return x + gp.tileSize / 2 + (int)Math.round(forwardX * 12 + sideX * 4);
    }

    private int getSwordPivotWorldY() {
        double angle = getSwordAttackAngle();
        double forwardX = Math.cos(angle);
        double forwardY = Math.sin(angle);
        double sideY = forwardX;

        return y + gp.tileSize / 2 + (int)Math.round(forwardY * 12 + sideY * 4);
    }

    private boolean swordCapsuleIntersects(Rectangle bounds, int reach, int width) {
        return swordCapsuleIntersects(bounds, reach, width, getSwordAttackAngle());
    }

    private boolean swordCapsuleIntersects(Rectangle bounds, int reach, int width, double angle) {
        return capsuleIntersectsFromStart(bounds, angle, getSwordPivotWorldX(), getSwordPivotWorldY(), reach, width);
    }

    private boolean bladeRushCapsuleIntersects(Rectangle bounds) {
        return capsuleIntersects(bounds, bladeRushAngle, -BLADE_RUSH_SPEED, gp.tileSize + BLADE_RUSH_SPEED, BLADE_RUSH_WIDTH);
    }

    private boolean capsuleIntersects(Rectangle bounds, double angle, double startDistance, double endDistance, int width) {
        int centerX = x + gp.tileSize / 2;
        int centerY = y + gp.tileSize / 2;
        double dirX = Math.cos(angle);
        double dirY = Math.sin(angle);

        double startX = centerX + dirX * startDistance;
        double startY = centerY + dirY * startDistance;
        double endX = centerX + dirX * endDistance;
        double endY = centerY + dirY * endDistance;

        double boundsCenterX = bounds.x + bounds.width / 2.0;
        double boundsCenterY = bounds.y + bounds.height / 2.0;
        double boundsRadius = Math.max(bounds.width, bounds.height) / 2.0;
        double allowedDistance = width / 2.0 + boundsRadius;

        return distancePointToSegment(boundsCenterX, boundsCenterY, startX, startY, endX, endY) <= allowedDistance;
    }

    private boolean capsuleIntersectsFromStart(Rectangle bounds, double angle, double startX, double startY, int reach, int width) {
        double dirX = Math.cos(angle);
        double dirY = Math.sin(angle);

        double endX = startX + dirX * reach;
        double endY = startY + dirY * reach;

        double boundsCenterX = bounds.x + bounds.width / 2.0;
        double boundsCenterY = bounds.y + bounds.height / 2.0;
        double boundsRadius = Math.max(bounds.width, bounds.height) / 2.0;
        double allowedDistance = width / 2.0 + boundsRadius;

        return distancePointToSegment(boundsCenterX, boundsCenterY, startX, startY, endX, endY) <= allowedDistance;
    }

    private double distancePointToSegment(double px, double py, double x1, double y1, double x2, double y2) {
        double segX = x2 - x1;
        double segY = y2 - y1;
        double lenSq = segX * segX + segY * segY;

        if (lenSq == 0) {
            double dx = px - x1;
            double dy = py - y1;
            return Math.sqrt(dx * dx + dy * dy);
        }

        double t = ((px - x1) * segX + (py - y1) * segY) / lenSq;
        if (t < 0) t = 0;
        if (t > 1) t = 1;

        double closestX = x1 + t * segX;
        double closestY = y1 + t * segY;
        double dx = px - closestX;
        double dy = py - closestY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void applySwordSlashHits() {
        int reach = getSwordReach();
        int width = getSwordWidth();

        for (int i = 0; i < gp.monsterList.size(); i++) {
            Monster m = gp.monsterList.get(i);
            if (m.hp <= 0 || m.isBossDying() || hitMonstersThisSwing.contains(m)) continue;

            if (swordCapsuleIntersects(m.getBounds(), reach, width)) {
                hitMonstersThisSwing.add(m);
                m.hp -= meleeDamage;
                gp.floatingTextList.add(new FloatingText(gp, m.x, m.y, "-" + meleeDamage, Color.YELLOW));
                m.stunCounter = 20;
            }
        }
    }

    private void parrySwordBullets() {
        for (int i = 0; i < gp.bulletList.size(); i++) {
            Bullet b = gp.bulletList.get(i);
            if (tryParryBullet(b)) {
                gp.bulletList.remove(i);
                i--;
            }
        }
    }

    public boolean tryParryBullet(Bullet b) {
        if (b == null || b.alive == false || b.isPlayerBullet || classType != 1) return false;

        if (isBladeRushActive) {
            if (parriedBulletsThisBladeRush.contains(b)) return false;

            if (bladeRushCapsuleIntersects(b.getBounds())) {
                parriedBulletsThisBladeRush.add(b);
                finishParriedBullet(b, bladeRushAngle);
                return true;
            }
        }

        if (isSwordParryActive()) {
            if (parriedBulletsThisSwing.contains(b)) return false;

            int parryReach = getSwordReach() + 32;
            int parryWidth = getSwordWidth() + 36;
            if (swordCapsuleIntersects(b.getBounds(), parryReach, parryWidth)) {
                parriedBulletsThisSwing.add(b);
                finishParriedBullet(b, meleeAttackAngle);
                return true;
            }
        }

        return false;
    }

    private void finishParriedBullet(Bullet b, double reflectAngle) {
        Rectangle bulletBounds = b.getBounds();
        int bulletCenterX = bulletBounds.x + bulletBounds.width / 2;
        int bulletCenterY = bulletBounds.y + bulletBounds.height / 2;

        b.alive = false;
        gp.playSE(2);

        if (swordReflectBullets) {
            int targetX = bulletCenterX + (int)(Math.cos(reflectAngle) * 1000);
            int targetY = bulletCenterY + (int)(Math.sin(reflectAngle) * 1000);
            int reflectedDamage = Math.max(1, meleeDamage);
            gp.bulletList.add(new Bullet(gp, bulletCenterX, bulletCenterY, targetX, targetY, true, reflectedDamage));
        }
    }

    private void spawnBladeRushAfterimage() {
        BufferedImage image = getCurrentPlayerDrawImage();
        if (image == null) return;

        DashAfterimage afterimage = new DashAfterimage();
        afterimage.worldX = x;
        afterimage.worldY = y;
        afterimage.image = image;
        afterimage.facingLeft = isFacingLeft();
        afterimage.life = BLADE_RUSH_AFTERIMAGE_LIFE;
        afterimage.maxLife = BLADE_RUSH_AFTERIMAGE_LIFE;
        bladeRushAfterimages.add(afterimage);

        while (bladeRushAfterimages.size() > BLADE_RUSH_AFTERIMAGE_MAX) {
            bladeRushAfterimages.remove(0);
        }
    }

    private void updateBladeRushAfterimages() {
        for (int i = 0; i < bladeRushAfterimages.size(); i++) {
            DashAfterimage afterimage = bladeRushAfterimages.get(i);
            afterimage.life--;
            if (afterimage.life <= 0) {
                bladeRushAfterimages.remove(i);
                i--;
            }
        }
    }

    private boolean isFacingLeft() {
        int playerCenterX = gp.player.screenX + gp.tileSize / 2;
        return gp.mouseH.mouseX < playerCenterX;
    }

    private void startBladeRush(int targetWorldX, int targetWorldY) {
        resetNormalMeleeAttack();
        isBladeRushActive = true;
        bladeRushCounter = 0;
        bladeRushAngle = Math.atan2(targetWorldY - (y + gp.tileSize / 2), targetWorldX - (x + gp.tileSize / 2));
        meleeAttackAngle = bladeRushAngle;
        hitMonstersThisBladeRush.clear();
        parriedBulletsThisBladeRush.clear();
        invincible = true;
        invincibleCounter = 0;
        gp.playSE(4);
    }

    private void updateBladeRush() {
        if (isBladeRushActive == false) return;

        invincible = true;
        invincibleCounter = 0;
        bladeRushCounter++;

        if (bladeRushCounter % BLADE_RUSH_AFTERIMAGE_INTERVAL == 0) {
            spawnBladeRushAfterimage();
        }

        int oldX = x;
        int oldY = y;
        x += (int)Math.round(Math.cos(bladeRushAngle) * BLADE_RUSH_SPEED);
        y += (int)Math.round(Math.sin(bladeRushAngle) * BLADE_RUSH_SPEED);

        collisionOn = false;
        gp.cChecker.checkTile(this);
        if (collisionOn == true) {
            x = oldX;
            y = oldY;
            collisionOn = false;
            endBladeRush();
            return;
        }

        applyBladeRushHits();
        parryBladeRushBullets();

        if (bladeRushCounter >= BLADE_RUSH_DURATION) {
            endBladeRush();
        }
    }

    private void endBladeRush() {
        isBladeRushActive = false;
        bladeRushCounter = 0;
        hitMonstersThisBladeRush.clear();
        parriedBulletsThisBladeRush.clear();
        invincible = true;
        invincibleCounter = 50;
    }

    private void applyBladeRushHits() {
        for (int i = 0; i < gp.monsterList.size(); i++) {
            Monster m = gp.monsterList.get(i);
            if (m.hp <= 0 || m.isBossDying() || hitMonstersThisBladeRush.contains(m)) continue;

            if (bladeRushCapsuleIntersects(m.getBounds())) {
                hitMonstersThisBladeRush.add(m);
                m.hp -= BLADE_RUSH_DAMAGE;
                gp.floatingTextList.add(new FloatingText(gp, m.x, m.y, "-" + BLADE_RUSH_DAMAGE, Color.YELLOW));
                m.stunCounter = BLADE_RUSH_STUN;
            }
        }
    }

    private void parryBladeRushBullets() {
        for (int i = 0; i < gp.bulletList.size(); i++) {
            Bullet b = gp.bulletList.get(i);
            if (tryParryBullet(b)) {
                gp.bulletList.remove(i);
                i--;
            }
        }
    }

    public void update() {
        if (invincible == true) {
            invincibleCounter++;
            if (invincibleCounter > 60) { 
                invincible = false;
                invincibleCounter = 0;
            }
        }

        if (shootCooldown > 0) shootCooldown--; 
        if (skillCooldown > 0) skillCooldown--;

        updateBladeRushAfterimages();
        updateBladeRush();
 
        if (isMeleeAttacking == true) {
            if (isMeleeAttackActiveFrame()) {
                applySwordSlashHits();
            }
            if (isSwordParryActive()) {
                parrySwordBullets();
            }

            meleeAttackCounter++;
            
            if (meleeAttackCounter >= attackCooldown) { 
                resetNormalMeleeAttack();
            }
        }
        
        if (isShooting == true) {
            shootAttackCounter++;
            if (shootAttackCounter >= attackCooldown) {
                isShooting = false;
                shootAttackCounter = 0;
            }
        }

        boolean isMoving = false;
        
        if (isBladeRushActive == false && (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed)) {
            isMoving = true;
        }

        if (isMoving == true) {
            int oldX = x;
            int oldY = y;

            if (keyH.leftPressed) x -= speed;
            else if (keyH.rightPressed) x += speed;

            collisionOn = false;
            gp.cChecker.checkTile(this);
            if (collisionOn == true) x = oldX; 

            if (keyH.upPressed) y -= speed;
            else if (keyH.downPressed) y += speed;

            collisionOn = false;
            gp.cChecker.checkTile(this);
            if (collisionOn == true) y = oldY; 
            
            if (animationState != 1) {
                animationState = 1; 
                spriteNum = 0; 
            }
        } else {
            if (animationState != 0) {
                animationState = 0; 
                spriteNum = 0; 
            }
        }

        spriteCounter++;
        if (spriteCounter > 5) { 
            if (animationState == 0) { 
                spriteNum++;
                if (idleFrames != null && spriteNum >= idleFrames.length) spriteNum = 0; 
            }
            else if (animationState == 1) { 
                spriteNum++;
                if (walkFrames != null && spriteNum >= walkFrames.length) spriteNum = 0; 
            }
            spriteCounter = 0; 
        }

        drawWidth = (int) (gp.tileSize * scaleFactor);
        drawHeight = (int) (gp.tileSize * scaleFactor);
        offset = (drawWidth - gp.tileSize) / 2;

        int targetWorldX = gp.mouseH.mouseX + x - gp.player.screenX; 
        int targetWorldY = gp.mouseH.mouseY + y - gp.player.screenY;

        if (gp.mouseH.pressed == true) {
            if (shootCooldown == 0) { 
                
                // Ranger attack.
                if (classType == 0 && isShooting == false) {
                    isShooting = true;
                    if (doubleShot == true) {
                        double angle = Math.atan2(targetWorldY - (y + 24), targetWorldX - (x + 24));
                        int dist = 1000;
                        int tx1 = (int) (x + 24 + Math.cos(angle - 0.2) * dist);
                        int ty1 = (int) (y + 24 + Math.sin(angle - 0.2) * dist);
                        int tx2 = (int) (x + 24 + Math.cos(angle + 0.2) * dist);
                        int ty2 = (int) (y + 24 + Math.sin(angle + 0.2) * dist);
                        
                        gp.bulletList.add(new Bullet(gp, x + 24, y + 24, tx1, ty1, true, gunDamage));
                        gp.bulletList.add(new Bullet(gp, x + 24, y + 24, tx2, ty2, true, gunDamage));
                    } else {
                        gp.bulletList.add(new Bullet(gp, x + 24, y + 24, targetWorldX, targetWorldY, true, gunDamage));
                    }
                    gp.playSE(1); 
                    shootCooldown = Math.max(15, attackCooldown);
                }
                
                // Swordsman attack.
                else if (classType == 1 && isMeleeAttacking == false && isBladeRushActive == false) {
                    isMeleeAttacking = true;
                    meleeAttackCounter = 0;
                    meleeAttackAngle = Math.atan2(targetWorldY - (y + gp.tileSize / 2), targetWorldX - (x + gp.tileSize / 2));
                    meleeHitbox.setBounds(0, 0, 0, 0);
                    hitMonstersThisSwing.clear();
                    parriedBulletsThisSwing.clear();
                    gp.playSE(2); 
                    shootCooldown = Math.max(18, attackCooldown);
                }
            } 
        } 

        // Ultimate skill.
        if (keyH.spacePressed == true && skillCooldown == 0) {
            keyH.spacePressed = false; 

            if (classType == 0) { 
                int ultiBullets = ultiBulletCount; 
                int ultiDamage = gunDamage * 3; 

                for (int i = 0; i < ultiBullets; i++) {
                    double angle = (Math.PI * 2) / ultiBullets * i; 
                    int tX = (int) (x + 24 + Math.cos(angle) * 100);
                    int tY = (int) (y + 24 + Math.sin(angle) * 100);
                    gp.bulletList.add(new Bullet(gp, x + 24, y + 24, tX, tY, true, ultiDamage));
                }
                gp.playSE(3);
            }
            else if (classType == 1) { 
                startBladeRush(targetWorldX, targetWorldY);
            }
            skillCooldown = skillMaxCooldown; 
        }
    }
    
    private BufferedImage getCurrentBodyDrawImage() {
        BufferedImage image = null;

        if (animationState == 0) {
            if (idleFrames != null && idleFrames.length > 0) {
                int frameIndex = spriteNum;
                if (frameIndex < 0 || frameIndex >= idleFrames.length) frameIndex = 0;
                image = idleFrames[frameIndex];
            }
        } else if (animationState == 1) {
            if (walkFrames != null && walkFrames.length > 0) {
                int frameIndex = spriteNum;
                if (frameIndex < 0 || frameIndex >= walkFrames.length) frameIndex = 0;
                image = walkFrames[frameIndex];
            }
        }

        if (image == null && idleFrames != null && idleFrames.length > 0) {
            image = idleFrames[0];
        }

        return image;
    }

    private BufferedImage getCurrentPlayerDrawImage() {
        BufferedImage image = null;

        try {
            // During melee, the visible sword is drawn as a separate layer.
            // A future body-only attack sheet can replace this branch without reintroducing baked slash frames.
            if (classType == 1 && isMeleeAttacking == true) {
                image = getCurrentBodyDrawImage();
            } 
            // Then ranged attack animation.
            else if (classType == 0 && isShooting == true) {
                double progress = (double) shootAttackCounter / attackCooldown;
                int attackFrameIndex = (int) (progress * attackFrames.length); 
                if (attackFrameIndex >= attackFrames.length) attackFrameIndex = attackFrames.length - 1;
                image = attackFrames[attackFrameIndex];
            } 
            else {
                image = getCurrentBodyDrawImage();
            }
        } catch (Exception e) {
            spriteNum = 0; 
        }

        return image;
    }

    private void drawBladeRushAfterimages(Graphics2D g2) {
        if (bladeRushAfterimages.isEmpty()) return;

        Composite oldComposite = g2.getComposite();

        for (int i = 0; i < bladeRushAfterimages.size(); i++) {
            DashAfterimage afterimage = bladeRushAfterimages.get(i);
            if (afterimage.image == null || afterimage.maxLife <= 0) continue;

            float alpha = 0.45f * ((float)afterimage.life / afterimage.maxLife);
            if (alpha <= 0) continue;

            int afterimageScreenX = afterimage.worldX - x + gp.player.screenX;
            int afterimageScreenY = afterimage.worldY - y + gp.player.screenY;
            int afterimageDrawHeight = (int)(gp.tileSize * scaleFactor);
            double ratio = (double)afterimage.image.getWidth() / afterimage.image.getHeight();
            int afterimageDrawWidth = (int)(afterimageDrawHeight * ratio);
            int offsetX = (afterimageDrawWidth - gp.tileSize) / 2;
            int offsetY = (afterimageDrawHeight - gp.tileSize) / 2;
            int drawX = afterimageScreenX - offsetX;
            int drawY = afterimageScreenY - offsetY;

            if (afterimage.facingLeft) {
                drawX += afterimageDrawWidth;
                afterimageDrawWidth = -afterimageDrawWidth;
            }

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(afterimage.image, drawX, drawY, afterimageDrawWidth, afterimageDrawHeight, null);
        }

        g2.setComposite(oldComposite);
    }

    private void drawBladeRushStreak(Graphics2D g2, int playerCenterX, int playerCenterY) {
        if (isBladeRushActive == false) return;

        java.awt.geom.AffineTransform oldTransform = g2.getTransform();
        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        float progress = Math.min(1f, (float)bladeRushCounter / Math.max(1, BLADE_RUSH_DURATION));
        float alpha = Math.max(0.14f, 0.50f * (1f - progress * 0.55f));

        g2.rotate(bladeRushAngle, playerCenterX, playerCenterY);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(170, 235, 255));
        g2.drawLine(playerCenterX - 72, playerCenterY, playerCenterX + 38, playerCenterY);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.70f, alpha + 0.15f)));
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 250, 255));
        g2.drawLine(playerCenterX - 58, playerCenterY - 4, playerCenterX + 30, playerCenterY - 4);

        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
        g2.setTransform(oldTransform);
    }

    private int getSwordPivotScreenX() {
        return getSwordPivotWorldX() - x + gp.player.screenX;
    }

    private int getSwordPivotScreenY() {
        return getSwordPivotWorldY() - y + gp.player.screenY;
    }

    private int getSwordVisualHeight() {
        int bonusWidth = Math.max(0, getSwordWidth() - SWORD_BASE_WIDTH);
        return Math.max(22, (int)Math.round(24 + getSwordWidth() * 0.18 + bonusWidth * 0.15));
    }

    private void drawSwordsmanWeapon(Graphics2D g2, boolean drawSlashEffect, boolean drawDebug) {
        int pivotScreenX = getSwordPivotScreenX();
        int pivotScreenY = getSwordPivotScreenY();
        double swordAngle = getSwordAttackAngle();

        drawSwordsmanSword(g2, pivotScreenX, pivotScreenY, swordAngle);

        if (drawSlashEffect == true) {
            drawSwordsmanSlash(g2, pivotScreenX, pivotScreenY, swordAngle);
        }

        if (drawDebug == true && DEBUG_SWORD_HITBOX == true) {
            drawSwordHitboxDebug(g2, pivotScreenX, pivotScreenY, swordAngle);
        }
    }

    private void drawSwordsmanSword(Graphics2D g2, int pivotScreenX, int pivotScreenY, double swordAngle) {
        java.awt.geom.AffineTransform oldTransform = g2.getTransform();
        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        g2.rotate(swordAngle, pivotScreenX, pivotScreenY);

        if (swordsmanSwordImage != null) {
            int reach = getSwordReach();
            double scaleX = reach / (double)(SWORD_IMAGE_WIDTH - SWORD_IMAGE_PIVOT_X);
            int drawWidth = Math.max(1, (int)Math.round(SWORD_IMAGE_WIDTH * scaleX));
            int drawHeight = Math.max(1, getSwordVisualHeight());
            double imageScaleX = drawWidth / (double)SWORD_IMAGE_WIDTH;
            double imageScaleY = drawHeight / (double)SWORD_IMAGE_HEIGHT;
            int scaledPivotX = (int)Math.round(SWORD_IMAGE_PIVOT_X * imageScaleX);
            int scaledPivotY = (int)Math.round(SWORD_IMAGE_PIVOT_Y * imageScaleY);

            g2.drawImage(swordsmanSwordImage,
                    pivotScreenX - scaledPivotX,
                    pivotScreenY - scaledPivotY,
                    drawWidth,
                    drawHeight,
                    null);
        } else {
            drawFallbackSword(g2, pivotScreenX, pivotScreenY);
        }

        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
        g2.setTransform(oldTransform);
    }

    private void drawFallbackSword(Graphics2D g2, int pivotScreenX, int pivotScreenY) {
        int reach = getSwordReach();
        float swordStroke = Math.max(6f, getSwordVisualHeight() * 0.42f);

        g2.setStroke(new BasicStroke(swordStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(210, 225, 235));
        g2.drawLine(pivotScreenX, pivotScreenY, pivotScreenX + reach, pivotScreenY);

        g2.setStroke(new BasicStroke(Math.max(2f, swordStroke * 0.22f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(250, 250, 255));
        g2.drawLine(pivotScreenX + 10, pivotScreenY - 2, pivotScreenX + reach, pivotScreenY - 2);
    }

    private void drawSwordsmanSlash(Graphics2D g2, int pivotScreenX, int pivotScreenY, double swordAngle) {
        java.awt.geom.AffineTransform oldTransform = g2.getTransform();
        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        g2.rotate(swordAngle, pivotScreenX, pivotScreenY);

        BufferedImage slashFrame = getCurrentSwordsmanSlashFrame();
        if (slashFrame != null) {
            int slashWidth = Math.max(48, getSwordReach() + 8);
            int slashHeight = Math.max(36, getSwordWidth() + 12);
            int slashX = pivotScreenX;
            int slashY = pivotScreenY - slashHeight / 2;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
            g2.drawImage(slashFrame, slashX, slashY, slashWidth, slashHeight, null);
        } else {
            drawFallbackSlash(g2, pivotScreenX, pivotScreenY);
        }

        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
        g2.setTransform(oldTransform);
    }

    private BufferedImage getCurrentSwordsmanSlashFrame() {
        if (swordsmanSlashFrames == null || swordsmanSlashFrames.length == 0) return null;

        int frameIndex = getMeleeAttackFrameIndex();
        if (frameIndex < 0) frameIndex = 0;
        if (frameIndex >= swordsmanSlashFrames.length) frameIndex = swordsmanSlashFrames.length - 1;
        return swordsmanSlashFrames[frameIndex];
    }

    private void drawFallbackSlash(Graphics2D g2, int pivotScreenX, int pivotScreenY) {
        int reach = getSwordReach();
        int width = getSwordWidth();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.36f));
        g2.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(125, 235, 255));
        g2.drawLine(pivotScreenX, pivotScreenY, pivotScreenX + reach, pivotScreenY);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
        g2.setStroke(new BasicStroke(Math.max(3, width / 5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(245, 255, 255));
        g2.drawLine(pivotScreenX + Math.max(8, reach / 5), pivotScreenY, pivotScreenX + reach, pivotScreenY);
    }

    private void drawSwordHitboxDebug(Graphics2D g2, int pivotScreenX, int pivotScreenY, double swordAngle) {
        java.awt.geom.AffineTransform oldTransform = g2.getTransform();
        Composite oldComposite = g2.getComposite();
        Stroke oldStroke = g2.getStroke();

        int reach = getSwordReach();
        int width = getSwordWidth();
        int radius = width / 2;

        g2.rotate(swordAngle, pivotScreenX, pivotScreenY);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f));

        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0, 255, 255));
        g2.drawLine(pivotScreenX, pivotScreenY - radius, pivotScreenX + reach, pivotScreenY - radius);
        g2.drawLine(pivotScreenX, pivotScreenY + radius, pivotScreenX + reach, pivotScreenY + radius);
        g2.drawOval(pivotScreenX - radius, pivotScreenY - radius, width, width);
        g2.drawOval(pivotScreenX + reach - radius, pivotScreenY - radius, width, width);

        g2.setColor(new Color(255, 80, 80));
        g2.drawLine(pivotScreenX, pivotScreenY, pivotScreenX + reach, pivotScreenY);
        g2.fillOval(pivotScreenX - 4, pivotScreenY - 4, 8, 8);

        g2.setStroke(oldStroke);
        g2.setComposite(oldComposite);
        g2.setTransform(oldTransform);
    }

    public void draw(Graphics2D g2) {
        if (invincible == true) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        }

        BufferedImage image = getCurrentPlayerDrawImage();

        // Keep the enlarged sprite centered on the hitbox.
        int drawX = gp.player.screenX; 
        int drawY = gp.player.screenY; 
        int currentDrawWidth = gp.tileSize; 
        int currentDrawHeight = gp.tileSize;

        if (image != null) {
            currentDrawHeight = (int) (gp.tileSize * scaleFactor); 
            
            double ratio = (double) image.getWidth() / image.getHeight();
            currentDrawWidth = (int) (currentDrawHeight * ratio);

            int offsetX = (currentDrawWidth - gp.tileSize) / 2;
            int offsetY = (currentDrawHeight - gp.tileSize) / 2;
            
            drawX = gp.player.screenX - offsetX;
            drawY = gp.player.screenY - offsetY;
        }

        int playerCenterX = gp.player.screenX + gp.tileSize / 2;
        int playerCenterY = gp.player.screenY + gp.tileSize / 2;

        drawBladeRushAfterimages(g2);
        drawBladeRushStreak(g2, playerCenterX, playerCenterY);

        // Face the mouse cursor.
        if (gp.mouseH.mouseX < playerCenterX) { 
            drawX = drawX + currentDrawWidth; 
            currentDrawWidth = -currentDrawWidth; 
        }

        if (image != null) {
            g2.drawImage(image, drawX, drawY, currentDrawWidth, currentDrawHeight, null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(gp.player.screenX, gp.player.screenY, gp.tileSize, gp.tileSize); 
        }

        if (invincible == true) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        if (classType == 1) {
            if (isMeleeAttacking == true) {
                drawSwordsmanWeapon(g2, isMeleeAttackActiveFrame(), true);
            } else if (isBladeRushActive == true) {
                drawSwordsmanWeapon(g2, false, false);
            }
        }

    }

    public Rectangle getBounds() {
        return new Rectangle(x + solidArea.x, y + solidArea.y, solidArea.width, solidArea.height);
    }
}   
