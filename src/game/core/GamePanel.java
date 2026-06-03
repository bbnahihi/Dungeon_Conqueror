package game.core;

import game.entity.Bullet;
import game.entity.FloatingText;
import game.entity.Item;
import game.entity.Monster;
import game.entity.Particle;
import game.entity.Player;
import game.input.KeyHandler;
import game.input.MouseHandler;
import game.system.Difficulty;
import game.system.Sound;
import game.system.StatsTracker;
import game.system.StoryManager;
import game.system.StoryManager.StoryAction;
import game.system.StoryManager.StoryMoment;
import game.system.UpgradeManager;
import game.tile.TileManager;
import game.ui.UI;

import javax.swing.JPanel;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.io.*;
public class GamePanel extends JPanel implements Runnable {
    
    public final int tileSize = 48; 
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;  
    public final int screenHeight = tileSize * maxScreenRow; 
    public final int maxWorldCol = 30;
    public final int maxWorldRow = 30;

    // Game states.
    public int gameState;
    public final int titleState = 0;
    public final int playState = 1;
    public final int pauseState = 2;
    public final int gameOverState = 3;
    public final int gameWinState = 4;
    public final int upgradeState = 5;
    public final int storyState = 8;

    Thread gameThread;
    KeyHandler keyH = new KeyHandler(this);
    public MouseHandler mouseH = new MouseHandler(this); 
    
    public Player player = new Player(this, keyH, mouseH);
    public UpgradeManager upgradeManager = new UpgradeManager(this);
    public TileManager tileM = new TileManager(this);
    public ArrayList<Bullet> bulletList = new ArrayList<>();
    public ArrayList<Monster> monsterList = new ArrayList<>();
    public ArrayList<Item> itemList = new ArrayList<>();
    public CollisionChecker cChecker = new CollisionChecker(this);
    public PathFinder pFinder = new PathFinder(this);
    public UI ui = new UI(this);
    public int currentLevel = 1;
    public int score = 0;
    public int bestScore = 0;
    public StatsTracker statsTracker = new StatsTracker();
    public StoryManager storyManager = new StoryManager();
    private final String saveFileName = "save.dat";
    private int pendingStoryClassType = 0;
    private BufferedImage bossArenaImage;
    private BufferedImage portalSpriteSheet;
    private BufferedImage[] portalFrames;
    private int portalFrameIndex = 0;
    private int portalFrameCounter = 0;
    private static final int PORTAL_FRAME_COUNT = 8;
    private static final int PORTAL_FRAME_DELAY = 6;
    private static final boolean DEBUG_BOSS_COVER_HITBOX = false;
    private ArrayList<BossStageDecoration> bossStageDecorations = new ArrayList<>();
    private ArrayList<BossArenaCover> bossArenaCovers = new ArrayList<>();
    private BufferedImage brokenPedestalCoverImage;
    private BufferedImage rubblePileCoverImage;
    private BufferedImage purpleCrystalCoverImage;
    private BufferedImage brokenPillarCoverImage;

    public ArrayList<Particle> particleList = new ArrayList<>();
    public Sound music = new Sound();
    public Sound se = new Sound(); // Sound effects.

    // Map themes.
    public final int THEME_FOREST = 0;
    public final int THEME_DUNGEON = 1;
    public final int THEME_DESERT = 2;
    
    public int currentTheme = THEME_FOREST;
    public Difficulty difficulty = Difficulty.NORMAL;
    public int previousState; // Used when leaving the options screen.

    public final int characterState = 6;
    public final int optionsState = 7;

    // Volume levels use 0-5.
    public int musicVolume = 3;
    public int seVolume = 3;

    // Damage text shown above enemies.
    public java.util.ArrayList<FloatingText> floatingTextList = new java.util.ArrayList<>();

    public GamePanel() {
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK); 
        this.setDoubleBuffered(true);    
        this.addKeyListener(keyH);
        this.addMouseMotionListener(mouseH);
        this.addMouseListener(mouseH);
        this.setFocusable(true); 

        gameState = titleState;
        loadBossArenaImage();
        loadPortalImage();
        loadBossStageDecorations();
        loadBossArenaCoverImages();
        createBossArenaCovers();
        loadBestScore();

        playMusic(6);
    }

    private static class BossStageDecoration {
        BufferedImage image;
        int worldX;
        int worldY;
        int drawWidth;
        int drawHeight;

        BossStageDecoration(BufferedImage image, int worldX, int worldY, int drawWidth, int drawHeight) {
            this.image = image;
            this.worldX = worldX;
            this.worldY = worldY;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
        }
    }

    private static class BossArenaCover {
        BufferedImage image;
        int worldX;
        int worldY;
        int drawWidth;
        int drawHeight;
        Rectangle hitbox;

        BossArenaCover(BufferedImage image, int worldX, int worldY, int drawWidth, int drawHeight, Rectangle hitbox) {
            this.image = image;
            this.worldX = worldX;
            this.worldY = worldY;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.hitbox = hitbox;
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start(); 
    }

    private void loadBossArenaImage() {
        try (InputStream is = getClass().getResourceAsStream("/res/boss_stage/boss_arena.png")) {
            if (is != null) {
                bossArenaImage = ImageIO.read(is);
            }
        } catch (IOException e) {
            bossArenaImage = null;
        }
    }

    private void loadPortalImage() {
        try (InputStream is = getClass().getResourceAsStream("/res/portal/portal_vortex_8frame.png")) {
            if (is != null) {
                portalSpriteSheet = ImageIO.read(is);
                if (portalSpriteSheet != null) {
                    slicePortalSpriteSheet();
                }
            } else {
                System.out.println("Warning: could not load portal sprite sheet /res/portal/portal_vortex_8frame.png");
            }
        } catch (IOException e) {
            System.out.println("Warning: could not load portal sprite sheet /res/portal/portal_vortex_8frame.png");
            portalSpriteSheet = null;
        }

        if (portalFrames == null || portalFrames.length == 0) {
            loadPortalFallbackFrames();
        }
    }

    private void slicePortalSpriteSheet() {
        if (portalSpriteSheet.getWidth() % PORTAL_FRAME_COUNT != 0) {
            System.out.println("Warning: portal sprite sheet width is not divisible by 8");
        }

        int frameWidth = portalSpriteSheet.getWidth() / PORTAL_FRAME_COUNT;
        int frameHeight = portalSpriteSheet.getHeight();
        if (frameWidth <= 0 || frameHeight <= 0) {
            portalFrames = null;
            return;
        }

        portalFrames = new BufferedImage[PORTAL_FRAME_COUNT];
        for (int i = 0; i < PORTAL_FRAME_COUNT; i++) {
            portalFrames[i] = portalSpriteSheet.getSubimage(i * frameWidth, 0, frameWidth, frameHeight);
        }
        portalFrameIndex = 0;
        portalFrameCounter = 0;
    }

    private void loadPortalFallbackFrames() {
        BufferedImage[] frames = new BufferedImage[PORTAL_FRAME_COUNT];

        for (int i = 0; i < frames.length; i++) {
            String path = "/res/portal/portal_vortex_frame_" + (i + 1) + ".png";
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) {
                    System.out.println("Warning: could not load portal frame " + path);
                    portalFrames = null;
                    return;
                }
                frames[i] = ImageIO.read(is);
                if (frames[i] == null) {
                    System.out.println("Warning: could not read portal frame " + path);
                    portalFrames = null;
                    return;
                }
            } catch (IOException e) {
                System.out.println("Warning: could not load portal frame " + path);
                portalFrames = null;
                return;
            }
        }

        portalFrames = frames;
        portalFrameIndex = 0;
        portalFrameCounter = 0;
    }

    private void loadBossStageDecorations() {
        bossStageDecorations.clear();

        addBossStageDecoration("12_void_portal_gate.png", 13, 3, 4, 4);
        addBossStageDecoration("06_hanging_banner.png", 5, 2, 2, 3);
        addBossStageDecoration("06_hanging_banner.png", 23, 2, 2, 3);
        addBossStageDecoration("07_chain_fence.png", 9, 3, 3, 1);
        addBossStageDecoration("07_chain_fence.png", 18, 3, 3, 1);
        addBossStageDecoration("07_chain_fence.png", 11, 27, 4, 1);
        addBossStageDecoration("04_purple_crystal_cluster.png", 2, 13, 2, 2);
        addBossStageDecoration("04_purple_crystal_cluster.png", 26, 14, 2, 2);
        addBossStageDecoration("05_void_brazier.png", 4, 4, 1, 2);
        addBossStageDecoration("05_void_brazier.png", 25, 4, 1, 2);
        addBossStageDecoration("05_void_brazier.png", 5, 25, 1, 2);
        addBossStageDecoration("05_void_brazier.png", 24, 25, 1, 2);
        addBossStageDecoration("01_broken_pillar_damaged.png", 3, 6, 2, 3);
        addBossStageDecoration("02_stone_column.png", 25, 6, 2, 3);
        addBossStageDecoration("03_broken_pedestal.png", 4, 23, 2, 2);
        addBossStageDecoration("08_rubble_pile.png", 3, 25, 2, 1);
        addBossStageDecoration("08_rubble_pile.png", 25, 23, 2, 1);
    }

    private void addBossStageDecoration(String fileName, int tileX, int tileY, int tileW, int tileH) {
        BufferedImage image = loadBossStageDecorationImage(fileName);
        if (image == null) return;

        bossStageDecorations.add(new BossStageDecoration(
                image,
                tileX * tileSize,
                tileY * tileSize,
                tileW * tileSize,
                tileH * tileSize));
    }

    private BufferedImage loadBossStageDecorationImage(String fileName) {
        String path = "/res/boss_stage/decor/" + fileName;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("Warning: could not load boss stage decoration " + path);
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            System.out.println("Warning: could not load boss stage decoration " + path);
            return null;
        }
    }

    private void loadBossArenaCoverImages() {
        brokenPedestalCoverImage = loadBossArenaCoverImage("03_broken_pedestal.png");
        rubblePileCoverImage = loadBossArenaCoverImage("08_rubble_pile.png");
        purpleCrystalCoverImage = loadBossArenaCoverImage("04_purple_crystal_cluster.png");
        brokenPillarCoverImage = loadBossArenaCoverImage("01_broken_pillar_damaged.png");
    }

    private BufferedImage loadBossArenaCoverImage(String fileName) {
        String path = "/res/boss_stage/cover/" + fileName;
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("Warning: could not load boss arena cover " + path);
                return null;
            }
            return ImageIO.read(is);
        } catch (IOException e) {
            System.out.println("Warning: could not load boss arena cover " + path);
            return null;
        }
    }

    private void createBossArenaCovers() {
        bossArenaCovers.clear();

        addBossArenaCover(brokenPedestalCoverImage, 7, 13, 96, 64, 4, 16, 88, 44);
        addBossArenaCover(brokenPedestalCoverImage, 21, 13, 96, 64, 4, 16, 88, 44);
        addBossArenaCover(rubblePileCoverImage, 8, 21, 80, 56, 4, 12, 72, 40);
        addBossArenaCover(rubblePileCoverImage, 20, 21, 80, 56, 4, 12, 72, 40);
        addBossArenaCover(purpleCrystalCoverImage, 4, 17, 72, 80, 8, 28, 56, 48);
        addBossArenaCover(purpleCrystalCoverImage, 25, 17, 72, 80, 8, 28, 56, 48);
    }

    private void addBossArenaCover(BufferedImage image, int tileX, int tileY, int drawWidth, int drawHeight,
                                   int hitboxOffsetX, int hitboxOffsetY, int hitboxWidth, int hitboxHeight) {
        if (image == null) return;

        int worldX = tileX * tileSize;
        int worldY = tileY * tileSize;
        Rectangle hitbox = new Rectangle(worldX + hitboxOffsetX, worldY + hitboxOffsetY,
                hitboxWidth, hitboxHeight);
        bossArenaCovers.add(new BossArenaCover(image, worldX, worldY, drawWidth, drawHeight, hitbox));
    }

    @Override
    public void run() {
        while (gameThread != null) {
            update();    
            repaint();   
            try { Thread.sleep(16); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public void update() {
        // Only update gameplay while playing.
        if (gameState == playState) {
            player.update(); 

            // Update dropped items.
            for (int i = 0; i < itemList.size(); i++) {
                Item item = itemList.get(i);
                item.update();

                if (item.alive == false) {
                    itemList.remove(i);
                    i--;
                }
            }

            // Update monsters and contact damage.
            for (int i = 0; i < monsterList.size(); i++) {
                Monster m = monsterList.get(i);
                m.update();
                if (m.shouldSpawnBossDeathParticles()) {
                    generateBossDeathPulseParticles(m);
                }

                if (m.isBossDying() == false && m.getBounds().intersects(player.getBounds()) && player.invincible == false) {
                    player.hp--; 
                    statsTracker.recordDamageTaken(1);
                    player.invincible = true; 
                    
                    if (player.hp <= 0) {
                        statsTracker.endRun();
                        gameState = gameOverState; 
                        stopMusic(); 
                        playSE(5);   
                    }
                }
            }

            // Update bullets.
            for (int i = 0; i < bulletList.size(); i++) {
                Bullet b = bulletList.get(i);
                b.update();

                // Remove dead bullets and bullets outside the map.
                if (b.alive == false || b.x < 0 || b.x > maxWorldCol * tileSize || b.y < 0 || b.y > maxWorldRow * tileSize) {
                    bulletList.remove(i);
                    i--; 
                    continue; 
                }

                // Enemy bullets damage the player here.
                if (b.isPlayerBullet == false) {
                    if (b.getBounds().intersects(player.getBounds()) && player.invincible == false) {
                        player.hp -= b.damage;
                        statsTracker.recordDamageTaken(b.damage);
                        player.invincible = true; 
                        
                        bulletList.remove(i);
                        i--;
                        
                        if (player.hp <= 0) {
                            statsTracker.endRun();
                            gameState = gameOverState; 
                            stopMusic(); 
                            playSE(5);   
                        }
                    }
                }
            }

            // Remove monsters after their HP reaches zero.
            for (int i = 0; i < monsterList.size(); i++) {
                Monster defeatedMonster = monsterList.get(i);

                if (defeatedMonster.hp <= 0) {
                    if (defeatedMonster.type == 3) {
                        if (defeatedMonster.isBossDying() == false) {
                            defeatedMonster.startBossDeathSequence();
                            generateBossDeathParticles(defeatedMonster);
                            continue;
                        }

                        if (defeatedMonster.isBossDeathFinished() == false) {
                            continue;
                        }

                        handleMonsterDefeated(defeatedMonster);
                        monsterList.remove(i);
                        i--;
                        continue;
                    }

                    if (defeatedMonster.type == 3) {
                    }

                    handleMonsterDefeated(defeatedMonster);
                    monsterList.remove(i);
                    i--;
                }
            }

            // Update particles and floating damage text.
            for (int i = 0; i < floatingTextList.size(); i++) {
                if (floatingTextList.get(i) != null) {
                    floatingTextList.get(i).update();
                    if (floatingTextList.get(i).isExpired()) {
                        floatingTextList.remove(i);
                        i--;
                    }
                }
            }
            
            for (int i = 0; i < particleList.size(); i++) {
                Particle p = particleList.get(i);
                p.update();
                if (p.isExpired()) {
                    particleList.remove(i);
                    i--;
                }
            }

            // Enter the portal after all monsters are gone.
            if (monsterList.isEmpty()) {
                updatePortalAnimation();

                Rectangle doorWorldHitbox = new Rectangle(20 * tileSize, 20 * tileSize, tileSize * 2, tileSize);
                
                if (player.getBounds().intersects(doorWorldHitbox)) {
                    if (currentLevel < 10) { 
                        nextLevel();
                    } else {
                        beginEndingStory();
                    }
                }
            } 
            
        }
    }

    private void updatePortalAnimation() {
        if (portalFrames == null || portalFrames.length == 0) return;

        portalFrameCounter++;
        if (portalFrameCounter >= PORTAL_FRAME_DELAY) {
            portalFrameCounter = 0;
            portalFrameIndex = (portalFrameIndex + 1) % portalFrames.length;
        }
    }

    private BufferedImage getCurrentPortalFrame() {
        if (portalFrames == null || portalFrames.length == 0) return null;
        if (portalFrameIndex < 0 || portalFrameIndex >= portalFrames.length) {
            portalFrameIndex = 0;
        }
        return portalFrames[portalFrameIndex];
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (gameState == titleState) {
            ui.drawTitleScreen(g2);
        } else if (gameState == storyState) {
            ui.drawStoryScreen(g2);
        } else {
            if (currentLevel == 10 && bossArenaImage != null) {
                drawBossArenaBackground(g2);
            } else {
                tileM.draw(g2);
            }
            if (currentLevel == 10) {
                drawBossStageDecorations(g2);
                drawBossArenaCovers(g2);
            }
            
           // Portal appears after the room is cleared.
        if (monsterList.isEmpty()) {
            int doorWorldX = 20 * tileSize;
            int doorWorldY = 20 * tileSize;

            int doorScreenX = doorWorldX - player.x + player.screenX;
            int doorScreenY = doorWorldY - player.y + player.screenY;

            BufferedImage portalFrame = getCurrentPortalFrame();
            if (portalFrame != null) {
                int portalDrawWidth = tileSize * 2;
                int portalDrawHeight = tileSize * 2;
                int portalDrawWorldX = 20 * tileSize;
                int portalDrawWorldY = 20 * tileSize - tileSize;
                int portalScreenX = portalDrawWorldX - player.x + player.screenX;
                int portalScreenY = portalDrawWorldY - player.y + player.screenY;
                g2.drawImage(portalFrame, portalScreenX, portalScreenY, portalDrawWidth, portalDrawHeight, null);
            } else {
                g2.setColor(Color.CYAN);
                g2.fillRect(doorScreenX, doorScreenY, tileSize * 2, tileSize);

                g2.setColor(Color.WHITE);
                String msg = (currentLevel < 10) ? "PORTAL" : "VICTORY";
                g2.drawString(msg, doorScreenX, doorScreenY - 10);
            }
        }

            for (int i = 0; i < itemList.size(); i++) {
                itemList.get(i).draw(g2);
            }

            player.draw(g2); 
        if (player.classType == 1 && player.isMeleeAttacking) {
                
            if (player.meleeHitbox.width > tileSize * 2) {
                int screenHitX = player.meleeHitbox.x - player.x + player.screenX;
                int screenHitY = player.meleeHitbox.y - player.y + player.screenY;
                    
                g2.setColor(new Color(255, 215, 0, 150)); 
                g2.fillOval(screenHitX, screenHitY, player.meleeHitbox.width, player.meleeHitbox.height);
                    
                g2.setColor(Color.ORANGE);
                g2.drawOval(screenHitX, screenHitY, player.meleeHitbox.width, player.meleeHitbox.height);
            } 
        }

            for (int i = 0; i < monsterList.size(); i++) {
                monsterList.get(i).draw(g2);
            }

            for (int i = 0; i < bulletList.size(); i++) {
                bulletList.get(i).draw(g2);
            }

            drawBossCoverDebugHitboxes(g2);
            
            for (int i = 0; i < floatingTextList.size(); i++) {
                if (floatingTextList.get(i) != null) {
                floatingTextList.get(i).draw(g2);
                }
            }
            ui.draw(g2);

            // Menu overlays are drawn above the world.
            if (gameState == pauseState) {
                ui.drawPauseScreen(g2);
            }
            if (gameState == gameOverState) {
                ui.drawGameOverScreen(g2);
            }
            else if (gameState == gameWinState) {
            ui.drawGameWinScreen(g2);
            }
            else if (gameState == upgradeState) {
                ui.drawUpgradeScreen(g2);
            }
            for (int i = 0; i < particleList.size(); i++) {
                particleList.get(i).draw(g2);
                }   
        }
    
        g2.dispose(); 
    }

    private void drawBossArenaBackground(Graphics2D g2) {
        int worldScreenX = -player.x + player.screenX;
        int worldScreenY = -player.y + player.screenY;
        g2.drawImage(bossArenaImage, worldScreenX, worldScreenY,
                maxWorldCol * tileSize, maxWorldRow * tileSize, null);
    }

    private void drawBossStageDecorations(Graphics2D g2) {
        for (int i = 0; i < bossStageDecorations.size(); i++) {
            BossStageDecoration decoration = bossStageDecorations.get(i);
            drawWorldDecoration(g2, decoration.image, decoration.worldX, decoration.worldY,
                    decoration.drawWidth, decoration.drawHeight);
        }
    }

    private void drawBossArenaCovers(Graphics2D g2) {
        if (isLevel10BossArenaCoverActive() == false) return;

        for (int i = 0; i < bossArenaCovers.size(); i++) {
            BossArenaCover cover = bossArenaCovers.get(i);
            drawWorldDecoration(g2, cover.image, cover.worldX, cover.worldY, cover.drawWidth, cover.drawHeight);
        }
    }

    public boolean collidesWithBossArenaCover(Rectangle worldHitbox) {
        if (isLevel10BossArenaCoverActive() == false || worldHitbox == null) return false;

        for (int i = 0; i < bossArenaCovers.size(); i++) {
            BossArenaCover cover = bossArenaCovers.get(i);
            if (cover.hitbox != null && worldHitbox.intersects(cover.hitbox)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLevel10BossArenaCoverActive() {
        return currentLevel == 10 && bossArenaCovers.isEmpty() == false;
    }

    private void drawBossCoverDebugHitboxes(Graphics2D g2) {
        if (DEBUG_BOSS_COVER_HITBOX == false || currentLevel != 10) return;

        g2.setColor(Color.RED);
        for (int i = 0; i < bossArenaCovers.size(); i++) {
            BossArenaCover cover = bossArenaCovers.get(i);
            drawWorldRect(g2, cover.hitbox);
        }

        g2.setColor(Color.GREEN);
        drawEntitySolidArea(g2, player);

        g2.setColor(Color.ORANGE);
        for (int i = 0; i < monsterList.size(); i++) {
            drawEntitySolidArea(g2, monsterList.get(i));
        }

        g2.setColor(Color.CYAN);
        for (int i = 0; i < bulletList.size(); i++) {
            drawWorldRect(g2, bulletList.get(i).getBounds());
        }
    }

    private void drawEntitySolidArea(Graphics2D g2, game.entity.Entity entity) {
        if (entity == null || entity.solidArea == null) return;

        Rectangle worldHitbox = new Rectangle(entity.x + entity.solidArea.x, entity.y + entity.solidArea.y,
                entity.solidArea.width, entity.solidArea.height);
        drawWorldRect(g2, worldHitbox);
    }

    private void drawWorldRect(Graphics2D g2, Rectangle worldRect) {
        if (worldRect == null) return;

        int screenX = worldRect.x - player.x + player.screenX;
        int screenY = worldRect.y - player.y + player.screenY;
        g2.drawRect(screenX, screenY, worldRect.width, worldRect.height);
    }

    private void drawWorldDecoration(Graphics2D g2, BufferedImage image, int worldX, int worldY,
                                     int drawWidth, int drawHeight) {
        int screenX = worldX - player.x + player.screenX;
        int screenY = worldY - player.y + player.screenY;
        g2.drawImage(image, screenX, screenY, drawWidth, drawHeight, null);
    }

    public void spawnMonsters(int level) {
        // Level 10 only spawns the final boss.
        if (level == 10) {
            int bossWorldX = (maxWorldCol / 2) * tileSize;
            int bossWorldY = (maxWorldRow / 4) * tileSize;
            
            Monster boss = new Monster(this, bossWorldX, bossWorldY, 3);
            difficulty.applyBossStats(boss);
            monsterList.add(boss);
            return;
        }

        // Enemy count and types scale with level.
        int monsterCount = difficulty.applyEnemyCount(10 + (level * 4)); 
        int spawned = 0;
        
        int attempts = 0; 
        
        // Stop trying if the map has no good spawn spots.
        while (spawned < monsterCount && attempts < 800) {
            attempts++; 
            
            int col = (int)(Math.random() * (maxWorldCol - 2)) + 1;
            int row = (int)(Math.random() * (maxWorldRow - 2)) + 1;

            if (tileM.mapTileNum[col][row] == 0) {
                int distance = Math.abs(col - 15) + Math.abs(row - 15);
                
                if (distance > 8) { 
                    int worldX = col * tileSize;
                    int worldY = row * tileSize;
                    // Later levels add more ranged monsters.
                    double meleeRate = Math.max(0.35, 0.65 - level * 0.03);
                    int type = (Math.random() < meleeRate) ? 1 : 2; 
                    
                    Monster m = new Monster(this, worldX, worldY, type);
                    
                    // Elite monsters start appearing from level 3.
                    double eliteChance = Math.min(0.25, 0.08 + level * 0.015);
                    if (level >= 3 && type == 1 && Math.random() < eliteChance) {
                        m.transformToElite(); 
                    }

                    // Extra scaling keeps item drops from making later levels too easy.
                    m.maxHp += Math.max(0, level / 2);
                    if (level >= 6) m.speed += 1;
                    if (type == 2 && level >= 4) m.maxHp += 1;

                    if (currentTheme == THEME_DUNGEON) m.maxHp += 2; 
                    else if (currentTheme == THEME_DESERT) m.speed += 1; 
                    
                    difficulty.applyMonsterStats(m);
                    
                    if (m.isElite == false) m.hp = m.maxHp; 
                    
                    monsterList.add(m);
                    spawned++; 
                }
            }
        }
    }

    public void nextLevel() {
        // Move away from the portal before loading the next level.
        player.x = tileSize * 15;
        player.y = tileSize * 15;
        player.isMeleeAttacking = false; 

        currentLevel++;
        statsTracker.setLevelReached(currentLevel);

        // Show upgrades after every 3 cleared levels.
        if ((currentLevel - 1) % 3 == 0 && currentLevel <= 10) {
            upgradeManager.rollUpgrades();
            gameState = upgradeState; 
        } else {
            transitionToLevelWithStory(currentLevel);
        }
    }

    public void selectUpgrade(int choiceIndex) {
        if (upgradeManager.applySelectedUpgrade(choiceIndex)) {
            statsTracker.recordUpgradeChosen();
        }
        transitionToLevelWithStory(currentLevel);
    }

    public void cycleDifficulty() {
        difficulty = difficulty.next();
    }

    public void cycleDifficultyBack() {
        difficulty = difficulty.previous();
    }

    public boolean isMenuLikeState() {
        return gameState == titleState
                || gameState == characterState
                || gameState == optionsState
                || gameState == pauseState
                || gameState == upgradeState
                || gameState == storyState
                || gameState == gameOverState
                || gameState == gameWinState;
    }

    public void handleUIClick(int x, int y) {
        if (gameState == titleState) {
            handleTitleClick(x, y);
        } else if (gameState == characterState) {
            handleCharacterClick(x, y);
        } else if (gameState == optionsState) {
            handleOptionsClick(x, y);
        } else if (gameState == pauseState) {
            handlePauseClick(x, y);
        } else if (gameState == upgradeState) {
            handleUpgradeClick(x, y);
        } else if (gameState == storyState) {
            advanceStory();
        } else if (gameState == gameOverState) {
            handleGameOverClick(x, y);
        } else if (gameState == gameWinState) {
            handleGameWinClick(x, y);
        }
    }

    private void handleTitleClick(int x, int y) {
        for (int i = 0; i < 4; i++) {
            if (ui.getTitleMenuBounds(i).contains(x, y)) {
                ui.commandNum = i;
                if (i == 0) {
                    gameState = characterState;
                } else if (i == 1) {
                    cycleDifficulty();
                } else if (i == 2) {
                    previousState = titleState;
                    gameState = optionsState;
                    ui.commandNum = 0;
                } else if (i == 3) {
                    System.exit(0);
                }
                return;
            }
        }
    }

    private void handleCharacterClick(int x, int y) {
        for (int i = 0; i < 2; i++) {
            if (ui.getCharacterChoiceBounds(i).contains(x, y)) {
                startRunWithClass(i);
                return;
            }
        }
    }

    private void handleOptionsClick(int x, int y) {
        Rectangle musicBounds = ui.getMusicVolumeBounds();
        Rectangle sfxBounds = ui.getSfxVolumeBounds();

        if (musicBounds.contains(x, y)) {
            ui.commandNum = 0;
            setMusicVolumeLevel(volumeLevelFromClick(x, musicBounds));
            return;
        }
        if (sfxBounds.contains(x, y)) {
            ui.commandNum = 1;
            setSeVolumeLevel(volumeLevelFromClick(x, sfxBounds));
            return;
        }

        if (ui.getOptionsRowBounds(0).contains(x, y)) {
            ui.commandNum = 0;
            setMusicVolumeLevel((musicVolume + 1) % 6);
            return;
        }
        if (ui.getOptionsRowBounds(1).contains(x, y)) {
            ui.commandNum = 1;
            setSeVolumeLevel((seVolume + 1) % 6);
            return;
        }
        if (ui.getOptionsRowBounds(2).contains(x, y)) {
            returnFromOptions();
        }
    }

    private void handlePauseClick(int x, int y) {
        for (int i = 0; i < 5; i++) {
            if (ui.getPauseMenuBounds(i).contains(x, y)) {
                ui.commandNum = i;
                if (i == 0) {
                    resumePausedGame();
                } else if (i == 1) {
                    restartCurrentRun();
                } else if (i == 2) {
                    previousState = pauseState;
                    gameState = optionsState;
                    ui.commandNum = 0;
                } else if (i == 3) {
                    returnToTitleMenu();
                } else if (i == 4) {
                    System.exit(0);
                }
                return;
            }
        }
    }

    private void handleUpgradeClick(int x, int y) {
        for (int i = 0; i < 3; i++) {
            if (ui.getUpgradeChoiceBounds(i).contains(x, y)) {
                selectUpgrade(i);
                return;
            }
        }
    }

    private void handleGameOverClick(int x, int y) {
        if (ui.getGameOverRestartBounds().contains(x, y)) {
            returnToTitleMenu();
        }
    }

    private void handleGameWinClick(int x, int y) {
        if (ui.getGameWinMainMenuBounds().contains(x, y)) {
            returnToTitleMenu();
        }
    }

    public void startRunWithClass(int classType) {
        pendingStoryClassType = classType;
        storyManager.resetRunFlags();
        storyManager.begin(StoryMoment.INTRO, StoryAction.START_RUN);
        gameState = storyState;
    }

    private void startRunAfterIntro() {
        gameState = playState;
        currentLevel = 1;
        player.setDefaultValues();
        particleList.clear();
        player.setupClass(pendingStoryClassType);
        transitionToNewMap(currentLevel);
    }

    public void advanceStory() {
        StoryAction action = storyManager.finishCurrentStory();

        if (action == StoryAction.START_RUN) {
            startRunAfterIntro();
        } else if (action == StoryAction.START_BOSS) {
            gameState = playState;
            transitionToNewMap(10);
        } else if (action == StoryAction.SHOW_WIN) {
            gameState = gameWinState;
        }
    }

    private void transitionToLevelWithStory(int level) {
        if (level == 10 && storyManager.shouldShowPreBossStory()) {
            storyManager.begin(StoryMoment.PRE_BOSS, StoryAction.START_BOSS);
            gameState = storyState;
        } else {
            gameState = playState;
            transitionToNewMap(level);
        }
    }

    private void beginEndingStory() {
        statsTracker.endRun();
        storyManager.begin(StoryMoment.ENDING, StoryAction.SHOW_WIN);
        gameState = storyState;
        stopMusic();
        playSE(7);
    }

    private void resumePausedGame() {
        gameState = playState;
        resumeMusic();
    }

    private void restartCurrentRun() {
        int selectedClass = player.classType;
        resetGame();
        player.setupClass(selectedClass);
        gameState = playState;
        ui.commandNum = 0;
        transitionToNewMap(currentLevel);
    }

    private void returnToTitleMenu() {
        gameState = titleState;
        ui.commandNum = 0;
        resetGame();
        playMusic(6);
    }

    private void returnFromOptions() {
        gameState = previousState;
        ui.commandNum = (previousState == pauseState) ? 2 : 0;
    }

    private int volumeLevelFromClick(int x, Rectangle bounds) {
        int relativeX = Math.max(0, Math.min(bounds.width, x - bounds.x));
        return Math.max(0, Math.min(5, (int) Math.ceil(relativeX / 30.0)));
    }

    private void setMusicVolumeLevel(int volumeLevel) {
        musicVolume = Math.max(0, Math.min(5, volumeLevel));
        music.setVolume(getVolumeDecibels(musicVolume));
    }

    private void setSeVolumeLevel(int volumeLevel) {
        seVolume = Math.max(0, Math.min(5, volumeLevel));
    }
    // Convert menu volume level to decibels.
    public float getVolumeDecibels(int volumeLevel) {
        switch(volumeLevel) {
            case 0: return -80.0f; // Mute.
            case 1: return -20.0f; // Very low.
            case 2: return -12.0f; // Low.
            case 3: return -5.0f;  // Default.
            case 4: return 1.0f;   // High.
            case 5: return 6.0f;   // Very high.
            default: return -5.0f;
        }
    }

    public void playMusic(int i) {
        music.stop();
        music.setFile(i);
        // Apply the current menu volume.
        music.setVolume(getVolumeDecibels(musicVolume)); 
        music.play();
        music.loop(); 
    }

    public void stopMusic() {
        music.stop();
    }
    public void pauseMusic() {
        music.pause();
    }
    public void resumeMusic() {
        music.resume();
    }

    public void playSE(int i) {
        se.setFile(i); 
        
        float currentVol = getVolumeDecibels(seVolume);
        
        // Keep loud effects lower than the music setting.
        if (seVolume > 0) {
            if (i == 1) { 
                currentVol -= 15.0f;
            } else if (i == 2) {
                currentVol -= 5.0f;
            }
        }
        
        se.setVolume(currentVol);
        se.play(); 
    }
    // Small burst effect when enemies die.
    public void generateParticles(int worldX, int worldY) {
        for (int k = 0; k < 20; k++) {
            double xVel = (Math.random() - 0.5) * 6;
            double yVel = (Math.random() - 0.5) * 6;
            
            Particle p = new Particle(this, worldX + tileSize/2, worldY + tileSize/2, Color.RED, 6, xVel, yVel, 40);
            particleList.add(p);
        }
    }

    public void generateBossDeathParticles(Monster boss) {
        int centerX = boss.x + tileSize;
        int centerY = boss.y + tileSize;
        Color[] colors = {
                new Color(160, 40, 220),
                new Color(220, 40, 210),
                new Color(95, 30, 180),
                new Color(255, 90, 230)
        };

        for (int k = 0; k < 90; k++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 1.5 + Math.random() * 6.0;
            double xVel = Math.cos(angle) * speed;
            double yVel = Math.sin(angle) * speed;
            int size = 4 + (int)(Math.random() * 7);
            int life = 55 + (int)(Math.random() * 45);
            Color color = colors[(int)(Math.random() * colors.length)];

            particleList.add(new Particle(this, centerX, centerY, color, size, xVel, yVel, life));
        }
    }

    public void generateBossDeathPulseParticles(Monster boss) {
        int centerX = boss.x + tileSize;
        int centerY = boss.y + tileSize;
        Color[] colors = {
                new Color(90, 20, 120),
                new Color(150, 30, 190),
                new Color(210, 45, 210),
                new Color(35, 20, 55)
        };

        for (int k = 0; k < 16; k++) {
            double angle = Math.random() * Math.PI * 2;
            double speed = 0.8 + Math.random() * 3.8;
            double xVel = Math.cos(angle) * speed;
            double yVel = Math.sin(angle) * speed;
            int size = 4 + (int)(Math.random() * 5);
            int life = 35 + (int)(Math.random() * 28);
            Color color = colors[(int)(Math.random() * colors.length)];

            particleList.add(new Particle(this, centerX, centerY, color, size, xVel, yVel, life));
        }
    }

    // Called when starting a new level or entering the portal.
    public void transitionToNewMap(int level) {
        if (level == 1) {
            statsTracker.startRun(level);
        }
        statsTracker.setLevelReached(level);
        
        bulletList.clear(); 
        monsterList.clear(); 
        itemList.clear(); 

        // Level groups use fixed themes.
        if (level >= 1 && level <= 3) {
            currentTheme = THEME_FOREST;
        } else if (level >= 4 && level <= 6) {
            currentTheme = THEME_DUNGEON;
        } else if (level >= 7 && level <= 9) {
            currentTheme = THEME_DESERT;
        } else if (level >= 10) {
            currentTheme = THEME_DUNGEON;
        }

        // Reload tile images before reading the new map.
        tileM.getTileInfo(); 
        tileM.loadMap(level); 

        // No auto-heal between levels; hearts are the healing source.

        if (level == 10) {
            player.x = (maxWorldCol / 2) * tileSize;
            player.y = (maxWorldRow - 4) * tileSize;
        } else {
            player.x = tileSize * 15;
            player.y = tileSize * 15;
        }
        player.isMeleeAttacking = false; 
        player.invincible = true;
        player.invincibleCounter = 0;
        
        spawnMonsters(level);
        
        if (level == 1) {
            playMusic(0);
        } else if (level == 10) {
            playMusic(8);
        }
    }

    public void handleMonsterDefeated(Monster defeatedMonster) {
        if (defeatedMonster.type == 3) {
        }
        statsTracker.recordEnemyKilled();
        addScore(getMonsterScore(defeatedMonster));
        if (defeatedMonster.type == 3) return;

        generateParticles(defeatedMonster.x, defeatedMonster.y);
        spawnItemDrop(defeatedMonster);
    }

    public void spawnItemDrop(Monster defeatedMonster) {
        if (defeatedMonster == null) return;

        if (defeatedMonster.type == 3) {
            return;
        }

        // Normal monsters drop less often than elites.
        double dropChance = difficulty.applyItemDropChance(defeatedMonster.isElite ? 0.50 : 0.20);
        if (Math.random() > dropChance) return;

        int itemType = rollItemType();
        int dropX = defeatedMonster.x + tileSize / 2 - 14;
        int dropY = defeatedMonster.y + tileSize / 2 - 14;
        itemList.add(new Item(this, dropX, dropY, itemType));
    }

    public int rollItemType() {
        double r = Math.random();

        if (r < 0.50) return Item.TYPE_COIN;    // +50 score.
        if (r < 0.65) return Item.TYPE_HEART;   // +1 HP.
        if (r < 0.90) return Item.TYPE_ENERGY;  // Shorter skill cooldown.
        return Item.TYPE_SHIELD;                // Short invincibility.
    }

    public int getMonsterScore(Monster m) {
        if (m.type == 3) return 1000; // Boss.
        if (m.isElite) return 100;    // Elite.
        if (m.type == 2) return 40;   // Ranged.
        return 25;                    // Melee.
    }

    public void addScore(int amount) {
        score += amount;
        if (score > bestScore) {
            bestScore = score;
            saveBestScore();
        }
    }

    public void loadBestScore() {
        File saveFile = new File(saveFileName);
        if (!saveFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(saveFile))) {
            String line = br.readLine();
            if (line != null) {
                bestScore = Integer.parseInt(line.trim());
            }
        } catch (Exception e) {
            bestScore = 0;
        }
    }

    public void saveBestScore() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(saveFileName))) {
            pw.println(bestScore);
        } catch (IOException e) {
            System.out.println("Could not save high score: " + e.getMessage());
        }
    }

    // Reset the current run.
    public void resetGame() {
        currentLevel = 1;
        score = 0;
        statsTracker.reset();
        storyManager.resetRunFlags();
        player.setDefaultValues();
        monsterList.clear();
        bulletList.clear();
        itemList.clear();
        particleList.clear();
        floatingTextList.clear();
        ui.levelClearCounter = 0;
        
        player.x = tileSize * 15; 
        player.y = tileSize * 15;
    }
}
