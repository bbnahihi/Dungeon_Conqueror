package game.entity;

import game.core.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

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

    private static final int BOSS_SHEET_COLUMNS = 8;
    private static final int BOSS_SHEET_ROWS = 4;
    private static final int BOSS_DRAW_SCALE_TILES = 3;
    private static final int BOSS_IDLE_FRAME_DELAY = 12;
    private static final int BOSS_ATTACK_FRAME_DELAY = 7;
    private static final int BOSS_PHASE_FRAME_DELAY = 9;
    private static final int BOSS_ATTACK_ANIMATION_TICKS = 30;
    private static final int BOSS_PHASE_ANIMATION_TICKS = 60;
    private static final int BOSS_DEATH_SEQUENCE_TICKS = 105;
    private static final int BOSS_GLOBAL_CAST_COOLDOWN = 24;
    private static final int BOSS_SHADOW_BOLT_COOLDOWN = 70;
    private static final int BOSS_RIFT_SPREAD_COOLDOWN = 150;
    private static final int BOSS_ABYSS_BARRAGE_COOLDOWN = 58;
    private static final int BOSS_RIFT_SUMMON_COOLDOWN = 210;
    private static final int BOSS_MAX_SUMMONED_MINIONS = 2;
    private static final int BOSS_ANIM_IDLE = 0;
    private static final int BOSS_ANIM_ATTACK = 1;
    private static final int BOSS_ANIM_PHASE = 2;
    private static final int BOSS_ANIM_PHASE2_IDLE = 3;
    private static final int BOSS_ANIM_PHASE2_ATTACK = 4;
    private static final int BOSS_ANIM_DEATH = 5;
    private static final int[] BOSS_PHASE1_IDLE_SEQUENCE = {0, 1, 2, 1};
    private static final int[] BOSS_PHASE1_ATTACK_SEQUENCE = {0, 1, 2, 3};
    private static final int[] BOSS_PHASE_TRANSITION_SEQUENCE = {0, 1, 2, 3, 4};
    private static final int[] BOSS_PHASE2_IDLE_SEQUENCE = {0, 1, 2, 3, 2, 1};
    private static final int[] BOSS_PHASE2_ATTACK_SEQUENCE = {2, 3, 4, 5, 4, 3};
    private static final int[] BOSS_DEATH_SEQUENCE = {4, 5, 6, 7, 6, 5};
    private BufferedImage[][] bossFrames;
    private int bossFrameWidth;
    private int bossFrameHeight;
    private int bossCastAnimationCounter = 0;
    private int bossPhaseAnimationCounter = 0;
    private int bossAnimationState = BOSS_ANIM_IDLE;
    private int bossAnimationFrameIndex = 0;
    private boolean bossPhaseAnimationStarted = false;
    private int bossShadowBoltCooldown = 35;
    private int bossRiftSpreadCooldown = 100;
    private int bossAbyssBarrageCooldown = 45;
    private int bossRiftSummonCooldown = 140;
    private int bossBarrageOffsetStep = 0;
    private boolean bossDying = false;
    private int bossDeathTimer = 0;
    

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
            this.maxHp = 300;
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
                loadDarkConquerorSheet();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDarkConquerorSheet() {
        try (InputStream is = getClass().getResourceAsStream("/res/boss/dark_conqueror_sheet.png")) {
            if (is == null) return;

            BufferedImage bossSheet = ImageIO.read(is);
            if (bossSheet == null) return;

            bossFrameWidth = bossSheet.getWidth() / BOSS_SHEET_COLUMNS;
            bossFrameHeight = bossSheet.getHeight() / BOSS_SHEET_ROWS;
            if (bossFrameWidth <= 0 || bossFrameHeight <= 0) return;

            bossFrames = new BufferedImage[BOSS_SHEET_ROWS][BOSS_SHEET_COLUMNS];
            for (int row = 0; row < BOSS_SHEET_ROWS; row++) {
                for (int col = 0; col < BOSS_SHEET_COLUMNS; col++) {
                    bossFrames[row][col] = bossSheet.getSubimage(
                            col * bossFrameWidth,
                            row * bossFrameHeight,
                            bossFrameWidth,
                            bossFrameHeight);
                }
            }
        } catch (IOException | RuntimeException e) {
            bossFrames = null;
        }
    }

    public void update() {
        if (bossDying) {
            updateBossDeathSequence();
            return;
        }
        
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
        if (type != 3 || bossFrames == null) {
            spriteCounter++;
            if (spriteCounter > 15) {
                if (spriteNum == 1) {
                    spriteNum = 2;
                } else if (spriteNum == 2) {
                    spriteNum = 1;
                }
                spriteCounter = 0;
            }
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
                shootCooldown = gp.difficulty.applyRangedCooldown(Math.max(55, 75 - gp.currentLevel * 5));
            }
            if (shootCooldown > 0) shootCooldown--;
        }
        
        // Final boss phase logic.
        else if (type == 3) {
            // Phase 2 starts at half HP.
            boolean isEnraged = (this.hp <= this.maxHp / 2);

            this.speed = gp.difficulty.getBossSpeed(isEnraged);
            updateBossSkillCooldowns();

            if (isEnraged && bossPhaseAnimationStarted == false) {
                bossPhaseAnimationStarted = true;
                bossPhaseAnimationCounter = BOSS_PHASE_ANIMATION_TICKS;
                switchBossAnimation(BOSS_ANIM_PHASE);
                gp.triggerBossPhaseTwoFeedback(this);
            }

            if (shootCooldown == 0) {
                if (isEnraged) {
                    usePhaseTwoBossSkill();
                } else {
                    usePhaseOneBossSkill();
                }
            }
            if (bossCastAnimationCounter > 0) bossCastAnimationCounter--;
            if (bossPhaseAnimationCounter > 0) bossPhaseAnimationCounter--;
            updateBossAnimation();
            if (shootCooldown > 0) shootCooldown--;
        }
    }

    private void updateBossSkillCooldowns() {
        if (bossShadowBoltCooldown > 0) bossShadowBoltCooldown--;
        if (bossRiftSpreadCooldown > 0) bossRiftSpreadCooldown--;
        if (bossAbyssBarrageCooldown > 0) bossAbyssBarrageCooldown--;
        if (bossRiftSummonCooldown > 0) bossRiftSummonCooldown--;
    }

    private void usePhaseOneBossSkill() {
        if (bossRiftSpreadCooldown == 0) {
            castRiftSpread();
            bossRiftSpreadCooldown = BOSS_RIFT_SPREAD_COOLDOWN;
            shootCooldown = BOSS_GLOBAL_CAST_COOLDOWN;
        } else if (bossShadowBoltCooldown == 0) {
            castShadowBolt();
            bossShadowBoltCooldown = BOSS_SHADOW_BOLT_COOLDOWN;
            shootCooldown = BOSS_GLOBAL_CAST_COOLDOWN;
        }
    }

    private void usePhaseTwoBossSkill() {
        if (bossAbyssBarrageCooldown == 0) {
            castAbyssBarrage();
            bossAbyssBarrageCooldown = BOSS_ABYSS_BARRAGE_COOLDOWN;
            shootCooldown = BOSS_GLOBAL_CAST_COOLDOWN;
        } else if (bossRiftSummonCooldown == 0 && countBossMinions() < BOSS_MAX_SUMMONED_MINIONS) {
            castRiftSummon();
            bossRiftSummonCooldown = BOSS_RIFT_SUMMON_COOLDOWN;
            shootCooldown = BOSS_GLOBAL_CAST_COOLDOWN;
        } else if (bossShadowBoltCooldown == 0) {
            castShadowBolt();
            bossShadowBoltCooldown = BOSS_SHADOW_BOLT_COOLDOWN - 15;
            shootCooldown = BOSS_GLOBAL_CAST_COOLDOWN;
        }
    }

    private void castShadowBolt() {
        startBossCastAnimation();

        int bulletCount = 1 + (int)(Math.random() * 3);
        double baseAngle = getAngleToPlayer();
        double spread = 0.14;

        for (int i = 0; i < bulletCount; i++) {
            double offset = (i - (bulletCount - 1) / 2.0) * spread;
            fireBossBullet(baseAngle + offset, 1);
        }
    }

    private void castRiftSpread() {
        startBossCastAnimation();

        double baseAngle = getAngleToPlayer();
        for (int i = 0; i < 5; i++) {
            double offset = (i - 2) * 0.22;
            fireBossBullet(baseAngle + offset, 1);
        }
    }

    private void castAbyssBarrage() {
        startBossCastAnimation();

        int bulletCount = 10;
        double step = Math.PI * 2 / bulletCount;
        double offset = (bossBarrageOffsetStep % 2) * step / 2.0;

        for (int i = 0; i < bulletCount; i++) {
            fireBossBullet(offset + step * i, 1);
        }
        bossBarrageOffsetStep++;
    }

    private void castRiftSummon() {
        startBossCastAnimation();

        int availableSlots = BOSS_MAX_SUMMONED_MINIONS - countBossMinions();
        int summonCount = Math.min(1, availableSlots);
        int[][] offsets = {
                {-gp.tileSize, gp.tileSize},
                {gp.tileSize * 2, gp.tileSize},
                {gp.tileSize / 2, gp.tileSize * 2}
        };

        for (int i = 0; i < summonCount; i++) {
            Monster minion = new Monster(gp, x + offsets[i][0], y + offsets[i][1], 1);
            minion.maxHp = Math.max(1, minion.maxHp - 1);
            minion.hp = minion.maxHp;
            minion.speed = Math.max(1, minion.speed - 1);
            gp.monsterList.add(minion);
        }
    }

    private int countBossMinions() {
        int count = 0;
        for (int i = 0; i < gp.monsterList.size(); i++) {
            Monster monster = gp.monsterList.get(i);
            if (monster != null && monster.type != 3) {
                count++;
            }
        }
        return count;
    }

    private double getAngleToPlayer() {
        int startX = x + gp.tileSize;
        int startY = y + gp.tileSize;
        int targetX = gp.player.x + gp.tileSize / 2;
        int targetY = gp.player.y + gp.tileSize / 2;
        return Math.atan2(targetY - startY, targetX - startX);
    }

    private void fireBossBullet(double angle, int damage) {
        int startX = x + gp.tileSize;
        int startY = y + gp.tileSize;
        int targetX = (int)(startX + Math.cos(angle) * 100);
        int targetY = (int)(startY + Math.sin(angle) * 100);
        gp.bulletList.add(new Bullet(gp, startX, startY, targetX, targetY, false, damage));
    }

    private void startBossCastAnimation() {
        bossCastAnimationCounter = BOSS_ATTACK_ANIMATION_TICKS;
        if (bossPhaseAnimationCounter == 0) switchBossAnimation(BOSS_ANIM_ATTACK);
    }

    public void startBossDeathSequence() {
        if (type != 3 || bossDying) return;

        bossDying = true;
        bossDeathTimer = 0;
        bossCastAnimationCounter = 0;
        bossPhaseAnimationCounter = 0;
        shootCooldown = 0;
        switchBossAnimation(BOSS_ANIM_DEATH);
    }

    public boolean isBossDying() {
        return bossDying;
    }

    public boolean isBossDeathFinished() {
        return bossDying && bossDeathTimer >= BOSS_DEATH_SEQUENCE_TICKS;
    }

    public boolean shouldSpawnBossDeathParticles() {
        return bossDying && bossDeathTimer > 0 && bossDeathTimer < BOSS_DEATH_SEQUENCE_TICKS && bossDeathTimer % 7 == 0;
    }

    private void updateBossDeathSequence() {
        bossDeathTimer++;
        if (bossDeathTimer == BOSS_DEATH_SEQUENCE_TICKS) {
        }
        updateBossAnimation();
    }

    public void draw(Graphics2D g2) {
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        BufferedImage imageToDraw = null;
        if (type == 3 && bossFrames != null) {
            imageToDraw = getBossAnimationFrame();
        } else {
            if (spriteNum == 1) imageToDraw = image1;
            if (spriteNum == 2) imageToDraw = image2;
        }

        int size = (type == 3) ? gp.tileSize * 2 : gp.tileSize;
        int visualSize = (type == 3 && bossFrames != null) ? gp.tileSize * BOSS_DRAW_SCALE_TILES : size;

        int drawX = screenX - (visualSize - size) / 2;
        int drawY = screenY - (visualSize - size) / 2;
        int drawWidth = visualSize;
        int drawHeight = visualSize;

        if (lookLeft == true) {
            drawX = screenX + drawWidth;
            if (type == 3 && bossFrames != null) {
                drawX = screenX + size + (visualSize - size) / 2;
            }
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

    private BufferedImage getBossAnimationFrame() {
        int row = getBossAnimationRow();
        int[] sequence = getBossAnimationSequence();
        int sequenceIndex = Math.max(0, Math.min(sequence.length - 1, bossAnimationFrameIndex));
        int col = sequence[sequenceIndex];
        return bossFrames[row][col];
    }

    private void updateBossAnimation() {
        if (bossFrames == null) return;

        int nextState = BOSS_ANIM_IDLE;
        if (bossDying) {
            nextState = BOSS_ANIM_DEATH;
        } else if (bossPhaseAnimationCounter > 0) {
            nextState = BOSS_ANIM_PHASE;
        } else if (bossPhaseAnimationStarted && bossCastAnimationCounter > 0) {
            nextState = BOSS_ANIM_PHASE2_ATTACK;
        } else if (bossPhaseAnimationStarted) {
            nextState = BOSS_ANIM_PHASE2_IDLE;
        } else if (bossCastAnimationCounter > 0) {
            nextState = BOSS_ANIM_ATTACK;
        }
        switchBossAnimation(nextState);

        spriteCounter++;
        if (spriteCounter > getBossFrameDelay()) {
            bossAnimationFrameIndex++;
            if (bossAnimationFrameIndex >= getBossAnimationSequence().length) {
                bossAnimationFrameIndex = 0;
            }
            spriteCounter = 0;
        }
    }

    private void switchBossAnimation(int nextState) {
        if (bossFrames == null || bossAnimationState == nextState) return;

        bossAnimationState = nextState;
        bossAnimationFrameIndex = 0;
        spriteCounter = 0;
    }

    private int getBossAnimationRow() {
        if (bossAnimationState == BOSS_ANIM_DEATH) return 3;
        if (bossAnimationState == BOSS_ANIM_PHASE) return 3;
        if (bossAnimationState == BOSS_ANIM_PHASE2_IDLE) return 3;
        if (bossAnimationState == BOSS_ANIM_PHASE2_ATTACK) return 3;
        if (bossAnimationState == BOSS_ANIM_ATTACK) return 2;
        return 0;
    }

    private int getBossFrameDelay() {
        if (bossAnimationState == BOSS_ANIM_DEATH) return BOSS_PHASE_FRAME_DELAY;
        if (bossAnimationState == BOSS_ANIM_PHASE) return BOSS_PHASE_FRAME_DELAY;
        if (bossAnimationState == BOSS_ANIM_PHASE2_ATTACK) return BOSS_ATTACK_FRAME_DELAY;
        if (bossAnimationState == BOSS_ANIM_PHASE2_IDLE) return BOSS_IDLE_FRAME_DELAY;
        if (bossAnimationState == BOSS_ANIM_ATTACK) return BOSS_ATTACK_FRAME_DELAY;
        return BOSS_IDLE_FRAME_DELAY;
    }

    private int[] getBossAnimationSequence() {
        if (bossAnimationState == BOSS_ANIM_DEATH) return BOSS_DEATH_SEQUENCE;
        if (bossAnimationState == BOSS_ANIM_PHASE) return BOSS_PHASE_TRANSITION_SEQUENCE;
        if (bossAnimationState == BOSS_ANIM_PHASE2_ATTACK) return BOSS_PHASE2_ATTACK_SEQUENCE;
        if (bossAnimationState == BOSS_ANIM_PHASE2_IDLE) return BOSS_PHASE2_IDLE_SEQUENCE;
        if (bossAnimationState == BOSS_ANIM_ATTACK) return BOSS_PHASE1_ATTACK_SEQUENCE;
        return BOSS_PHASE1_IDLE_SEQUENCE;
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
