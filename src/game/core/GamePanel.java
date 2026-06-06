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
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    private static final int FOREST_LEVEL = 1;
    private static final int ICE_LEVEL = 2;
    private static final int DESERT_LEVEL = 3;
    private static final int LAST_NORMAL_LEVEL = 3;
    private static final int BOSS_LEVEL = 10;
    private static final int NO_PENDING_LEVEL = -1;
    private static final int MUSIC_LEVEL1_FOREST = 0;
    private static final int MUSIC_LOBBY = 6;
    private static final int MUSIC_BOSS = 8;
    private static final int MUSIC_LEVEL2_ICE = 9;
    private static final int MUSIC_LEVEL3_DESERT = 10;
    public int currentLevel = FOREST_LEVEL;
    public int score = 0;
    public int bestScore = 0;
    public StatsTracker statsTracker = new StatsTracker();
    public StoryManager storyManager = new StoryManager();
    private final String saveFileName = "save.dat";
    private int pendingStoryClassType = 0;
    private int pendingLevelAfterUpgrade = NO_PENDING_LEVEL;
    private boolean levelClearHandled = false;
    private boolean bossFightStarted = false;
    private boolean mapTransitionInProgress = false;
    private boolean normalStageEnemiesSpawned = false;
    private BufferedImage bossArenaImage;
    private BufferedImage virtualScreen;
    private int screenShakeTimer = 0;
    private int screenShakeDuration = 0;
    private int screenShakeMagnitude = 0;
    private int screenShakeOffsetX = 0;
    private int screenShakeOffsetY = 0;
    private int damageFlashTimer = 0;
    private static final int DAMAGE_FLASH_DURATION = 18;
    private static final int DAMAGE_EDGE_THICKNESS = 48;
    private static final float DAMAGE_EDGE_MAX_ALPHA = 0.42f;
    private int bossPhaseIntroTimer = 0;
    private int bossPhaseFlashTimer = 0;
    private static final int BOSS_PHASE_FLASH_DURATION = 42;
    private Random screenShakeRandom = new Random();
    private BufferedImage portalSpriteSheet;
    private BufferedImage[] portalFrames;
    private int portalFrameIndex = 0;
    private int portalFrameCounter = 0;
    private static final int PORTAL_FRAME_COUNT = 8;
    private static final int PORTAL_FRAME_DELAY = 6;
    private static final boolean DEBUG_BOSS_COVER_HITBOX = false;
    private static final boolean DEBUG_NORMAL_MAP_OBSTACLES = false;
    private static final boolean DEBUG_RESOURCE_LOGS = false;
    private BufferedImage forestBackgroundImage;
    private BufferedImage iceBackgroundImage;
    private BufferedImage desertBackgroundImage;
    private ArrayList<BossStageDecoration> bossStageDecorations = new ArrayList<>();
    private ArrayList<BossArenaCover> bossArenaCovers = new ArrayList<>();
    private ArrayList<NormalMapObstacle> normalMapObstacles = new ArrayList<>();
    private ArrayList<MapProp> normalMapProps = new ArrayList<>();
    private ArrayList<WalkableArea> normalMapWalkableAreas = new ArrayList<>();
    private Map<String, MapPropDefinition> propDefinitions = new HashMap<>();
    private Set<String> warnedNormalObstacleFallbacks = new HashSet<>();
    private Set<String> warnedMapPropIssues = new HashSet<>();
    private Set<String> warnedWalkableAreaIssues = new HashSet<>();
    private String loadedPropDefinitionPath = null;
    private String loadedWalkableAreaPath = null;
    private boolean bossPropLayerActive = false;
    private boolean bossCoverFallbackActive = false;
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
    private int currentMusicTrack = -1;

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
        loadNormalMapBackgrounds();
        loadPortalImage();
        loadBossStageDecorations();
        loadBossArenaCoverImages();
        createBossArenaCovers();
        loadBestScore();

        playLobbyMusic();
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

    private static class NormalMapObstacle {
        String id;
        Rectangle worldHitbox;
        boolean blocksMovement;
        boolean blocksProjectiles;
        String source;

        NormalMapObstacle(String id, Rectangle worldHitbox, boolean blocksMovement, boolean blocksProjectiles) {
            this(id, worldHitbox, blocksMovement, blocksProjectiles, "obstacle");
        }

        NormalMapObstacle(String id, Rectangle worldHitbox, boolean blocksMovement, boolean blocksProjectiles,
                          String source) {
            this.id = id;
            this.worldHitbox = worldHitbox;
            this.blocksMovement = blocksMovement;
            this.blocksProjectiles = blocksProjectiles;
            this.source = source;
        }
    }

    private static class WalkableArea {
        String id;
        Rectangle worldArea;

        WalkableArea(String id, Rectangle worldArea) {
            this.id = id;
            this.worldArea = worldArea;
        }
    }

    private static class MapPropDefinition {
        String type;
        String imagePath;
        BufferedImage image;
        double scale;
        String collisionMode;
        double shrinkX;
        double shrinkY;
        boolean blocksMovement;
        boolean blocksProjectiles;

        MapPropDefinition(String type, String imagePath, BufferedImage image, double scale, String collisionMode,
                          double shrinkX, double shrinkY, boolean blocksMovement, boolean blocksProjectiles) {
            this.type = type;
            this.imagePath = imagePath;
            this.image = image;
            this.scale = scale;
            this.collisionMode = collisionMode;
            this.shrinkX = shrinkX;
            this.shrinkY = shrinkY;
            this.blocksMovement = blocksMovement;
            this.blocksProjectiles = blocksProjectiles;
        }
    }

    private static class MapProp {
        String id;
        String type;
        BufferedImage image;
        int worldX;
        int worldY;
        int imageWidth;
        int imageHeight;
        int drawWidth;
        int drawHeight;
        double scale;
        Rectangle collisionRect;

        MapProp(String id, String type, BufferedImage image, int worldX, int worldY,
                int imageWidth, int imageHeight, int drawWidth, int drawHeight,
                double scale, Rectangle collisionRect) {
            this.id = id;
            this.type = type;
            this.image = image;
            this.worldX = worldX;
            this.worldY = worldY;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
            this.drawWidth = drawWidth;
            this.drawHeight = drawHeight;
            this.scale = scale;
            this.collisionRect = collisionRect;
        }
    }

    private void ensureVirtualScreen() {
        if (virtualScreen == null) {
            virtualScreen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public double getRenderScale() {
        double scaleX = getWidth() / (double) screenWidth;
        double scaleY = getHeight() / (double) screenHeight;
        return Math.min(scaleX, scaleY);
    }

    public int getRenderOffsetX() {
        int drawW = (int)Math.round(screenWidth * getRenderScale());
        return (getWidth() - drawW) / 2;
    }

    public int getRenderOffsetY() {
        int drawH = (int)Math.round(screenHeight * getRenderScale());
        return (getHeight() - drawH) / 2;
    }

    public int toVirtualX(int realMouseX) {
        double scale = getRenderScale();
        if (scale <= 0) return realMouseX;
        return (int)Math.round((realMouseX - getRenderOffsetX()) / scale);
    }

    public int toVirtualY(int realMouseY) {
        double scale = getRenderScale();
        if (scale <= 0) return realMouseY;
        return (int)Math.round((realMouseY - getRenderOffsetY()) / scale);
    }

    public boolean isInsideVirtualScreen(int realMouseX, int realMouseY) {
        int virtualX = toVirtualX(realMouseX);
        int virtualY = toVirtualY(realMouseY);
        return virtualX >= 0 && virtualY >= 0 && virtualX < screenWidth && virtualY < screenHeight;
    }

    private void triggerPlayerDamageFeedback(int damage) {
        int safeDamage = Math.max(1, damage);
        int magnitude = safeDamage >= 2 ? 8 : 5;
        int duration = safeDamage >= 2 ? 14 : 10;

        startScreenShake(magnitude, duration);
        damageFlashTimer = DAMAGE_FLASH_DURATION;
    }

    private void triggerBossDeathFeedback() {
        startScreenShake(16, 95);
    }

    public void triggerBossPhaseTwoFeedback(Monster boss) {
        if (boss == null) return;

        bossPhaseIntroTimer = 45;
        bossPhaseFlashTimer = BOSS_PHASE_FLASH_DURATION;

        startScreenShake(13, 38);
        generateBossPhaseTwoBurstParticles(boss);
    }

    private void startScreenShake(int magnitude, int duration) {
        screenShakeMagnitude = Math.max(screenShakeMagnitude, magnitude);
        screenShakeDuration = Math.max(screenShakeDuration, duration);
        screenShakeTimer = Math.max(screenShakeTimer, duration);
    }

    private void updateDamageFeedbackEffects() {
        if (screenShakeTimer > 0) {
            double progress = screenShakeTimer / (double)Math.max(1, screenShakeDuration);
            int currentMagnitude = Math.max(1, (int)Math.round(screenShakeMagnitude * progress));

            screenShakeOffsetX = screenShakeRandom.nextInt(currentMagnitude * 2 + 1) - currentMagnitude;
            screenShakeOffsetY = screenShakeRandom.nextInt(currentMagnitude * 2 + 1) - currentMagnitude;

            screenShakeTimer--;

            if (screenShakeTimer <= 0) {
                screenShakeOffsetX = 0;
                screenShakeOffsetY = 0;
                screenShakeMagnitude = 0;
                screenShakeDuration = 0;
            }
        } else {
            screenShakeOffsetX = 0;
            screenShakeOffsetY = 0;
        }

        if (damageFlashTimer > 0) {
            damageFlashTimer--;
        }
    }

    private void updateBossPhaseTwoPresentation() {
        if (bossPhaseIntroTimer > 0) bossPhaseIntroTimer--;
        if (bossPhaseFlashTimer > 0) bossPhaseFlashTimer--;
    }

    private void drawDamageFlash(Graphics2D g2) {
        if (damageFlashTimer <= 0) return;

        float alpha = damageFlashTimer / (float)DAMAGE_FLASH_DURATION;
        alpha = Math.min(0.15f, alpha * 0.15f);

        Composite oldComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(Color.RED);
        g2.fillRect(0, 0, screenWidth, screenHeight);
        g2.setComposite(oldComposite);
    }

    private void drawDamageEdgeVignette(Graphics2D g2) {
        if (damageFlashTimer <= 0) return;

        float progress = damageFlashTimer / (float)DAMAGE_FLASH_DURATION;
        float alpha = Math.min(DAMAGE_EDGE_MAX_ALPHA, progress * DAMAGE_EDGE_MAX_ALPHA);

        Composite oldComposite = g2.getComposite();

        int layers = 4;
        for (int i = 0; i < layers; i++) {
            float layerAlpha = alpha * (1.0f - i * 0.18f);
            if (layerAlpha <= 0f) continue;

            int thickness = Math.max(4, DAMAGE_EDGE_THICKNESS - i * 10);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layerAlpha));
            g2.setColor(Color.RED);

            g2.fillRect(0, 0, screenWidth, thickness);
            g2.fillRect(0, screenHeight - thickness, screenWidth, thickness);
            g2.fillRect(0, 0, thickness, screenHeight);
            g2.fillRect(screenWidth - thickness, 0, thickness, screenHeight);
        }

        g2.setComposite(oldComposite);
    }

    private void drawBossPhaseTwoAura(Graphics2D g2) {
        if (isBossLevel(currentLevel) == false) return;

        Composite oldComposite = g2.getComposite();
        for (int i = 0; i < monsterList.size(); i++) {
            Monster boss = monsterList.get(i);
            if (boss.type != 3 || boss.hp <= 0 || boss.maxHp <= 0 || boss.isBossDying()) continue;
            if (boss.hp > boss.maxHp / 2) continue;

            int bossSize = tileSize * 2;
            int centerX = boss.x + bossSize / 2 - player.x + player.screenX;
            int centerY = boss.y + bossSize / 2 - player.y + player.screenY;
            float introAlpha = bossPhaseIntroTimer > 0 ? bossPhaseIntroTimer / 45f : 0f;
            int introRadiusBonus = bossPhaseIntroTimer > 0 ? (45 - bossPhaseIntroTimer) * 2 : 0;
            int outerRadius = tileSize * 3 + introRadiusBonus;
            int innerRadius = tileSize * 2;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.13f + introAlpha * 0.14f));
            g2.setColor(new Color(116, 28, 170));
            g2.fillOval(centerX - outerRadius / 2, centerY - outerRadius / 2, outerRadius, outerRadius);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f + introAlpha * 0.12f));
            g2.setColor(new Color(190, 35, 120));
            g2.fillOval(centerX - innerRadius / 2, centerY - innerRadius / 2, innerRadius, innerRadius);
        }
        g2.setComposite(oldComposite);
    }

    private void drawBossPhaseTwoOverlay(Graphics2D g2) {
        if (isBossLevel(currentLevel) == false) return;
        if (bossPhaseFlashTimer <= 0) return;

        Composite oldComposite = g2.getComposite();

        float progress = bossPhaseFlashTimer / (float)BOSS_PHASE_FLASH_DURATION;
        float alpha = Math.min(0.28f, progress * 0.28f);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(55, 8, 80));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.45f));
        g2.setColor(new Color(160, 20, 95));
        g2.fillRect(0, 0, screenWidth, screenHeight);

        g2.setComposite(oldComposite);
    }

    private void resetDamageFeedbackEffects() {
        screenShakeTimer = 0;
        screenShakeDuration = 0;
        screenShakeMagnitude = 0;
        screenShakeOffsetX = 0;
        screenShakeOffsetY = 0;
        damageFlashTimer = 0;
    }

    private void resetBossPhaseTwoPresentation() {
        bossPhaseIntroTimer = 0;
        bossPhaseFlashTimer = 0;
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start(); 
    }

    private void loadBossArenaImage() {
        bossArenaImage = loadNormalMapBackground("/res/maps/backgrounds/boss_base.png");
        if (bossArenaImage == null) {
            bossArenaImage = loadNormalMapBackground("/res/boss_stage/boss_arena.png");
        }
    }

    private void loadNormalMapBackgrounds() {
        forestBackgroundImage = loadNormalMapBackground("/res/maps/backgrounds/forest_base.png");
        iceBackgroundImage = loadNormalMapBackground("/res/maps/backgrounds/ice_base.png");
        desertBackgroundImage = loadNormalMapBackground("/res/maps/backgrounds/desert_base.png");
    }

    private BufferedImage loadNormalMapBackground(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                System.out.println("Warning: could not load normal map background " + path);
                return null;
            }

            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                System.out.println("Warning: could not read normal map background " + path);
            }
            return image;
        } catch (IOException e) {
            System.out.println("Warning: could not load normal map background " + path);
            return null;
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
                if (DEBUG_RESOURCE_LOGS) {
                    System.out.println("Warning: could not load portal sprite sheet /res/portal/portal_vortex_8frame.png");
                }
            }
        } catch (IOException e) {
            if (DEBUG_RESOURCE_LOGS) {
                System.out.println("Warning: could not load portal sprite sheet /res/portal/portal_vortex_8frame.png");
            }
            portalSpriteSheet = null;
        }

        if (portalFrames == null || portalFrames.length == 0) {
            loadPortalFallbackFrames();
        }
    }

    private void slicePortalSpriteSheet() {
        if (portalSpriteSheet.getWidth() % PORTAL_FRAME_COUNT != 0) {
            if (DEBUG_RESOURCE_LOGS) {
                System.out.println("Warning: portal sprite sheet width is not divisible by 8");
            }
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
                    if (DEBUG_RESOURCE_LOGS) {
                        System.out.println("Warning: could not load portal frame " + path);
                    }
                    portalFrames = null;
                    return;
                }
                frames[i] = ImageIO.read(is);
                if (frames[i] == null) {
                    if (DEBUG_RESOURCE_LOGS) {
                        System.out.println("Warning: could not read portal frame " + path);
                    }
                    portalFrames = null;
                    return;
                }
            } catch (IOException e) {
                if (DEBUG_RESOURCE_LOGS) {
                    System.out.println("Warning: could not load portal frame " + path);
                }
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
        if (gameState == playState) {
            updateDamageFeedbackEffects();
            updateBossPhaseTwoPresentation();
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
                    startScreenShake(7, 12);
                }

                if (m.isBossDying() == false && m.getBounds().intersects(player.getBounds()) && player.invincible == false) {
                    player.hp--; 
                    triggerPlayerDamageFeedback(1);
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
                    if (player.tryParryBullet(b)) {
                        bulletList.remove(i);
                        i--;
                        continue;
                    }

                    if (b.getBounds().intersects(player.getBounds()) && player.invincible == false) {
                        player.hp -= b.damage;
                        triggerPlayerDamageFeedback(b.damage);
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
                            triggerBossDeathFeedback();
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

            // Enter the portal after the current stage is truly cleared.
            if (shouldShowClearPortal()) {
                updatePortalAnimation();

                if (player.getBounds().intersects(getClearPortalHitbox())) {
                    handleClearPortalReached();
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
        ensureVirtualScreen();

        Graphics2D virtualGraphics = virtualScreen.createGraphics();
        try {
            virtualGraphics.setColor(Color.BLACK);
            virtualGraphics.fillRect(0, 0, screenWidth, screenHeight);
            drawGameToVirtualScreen(virtualGraphics);
        } finally {
            virtualGraphics.dispose();
        }

        Graphics2D g2 = (Graphics2D) g;
        drawScaledVirtualScreen(g2);
    }

    private void drawGameToVirtualScreen(Graphics2D g2) {

        if (gameState == titleState) {
            ui.drawTitleScreen(g2);
        } else if (gameState == storyState) {
            ui.drawStoryScreen(g2);
        } else {
            Graphics2D worldG = (Graphics2D)g2.create();
            worldG.translate(screenShakeOffsetX, screenShakeOffsetY);
            try {
                if (isBossLevel(currentLevel) && bossArenaImage != null) {
                    drawBossArenaBackground(worldG);
                } else if (isNormalMapLevel()) {
                    drawNormalMapBackground(worldG);
                } else {
                    tileM.draw(worldG);
                }
                if (isBossLevel(currentLevel)) {
                    if (bossPropLayerActive) {
                        drawNormalMapProps(worldG);
                    } else {
                        drawBossStageDecorations(worldG);
                        drawBossArenaCovers(worldG);
                    }
                    drawNormalMapObstacleDebug(worldG);
                } else {
                    drawNormalMapProps(worldG);
                    drawNormalMapObstacleDebug(worldG);
                }

                if (shouldShowClearPortal()) {
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
                        worldG.drawImage(portalFrame, portalScreenX, portalScreenY, portalDrawWidth, portalDrawHeight, null);
                    } else {
                        worldG.setColor(new Color(20, 190, 235, 165));
                        worldG.fillOval(doorScreenX, doorScreenY - tileSize / 2, tileSize * 2, tileSize * 2);
                        worldG.setColor(new Color(225, 255, 255, 190));
                        worldG.drawOval(doorScreenX, doorScreenY - tileSize / 2, tileSize * 2, tileSize * 2);
                    }
                }

                for (int i = 0; i < itemList.size(); i++) {
                    itemList.get(i).draw(worldG);
                }

                player.draw(worldG);

                drawBossPhaseTwoAura(worldG);

                for (int i = 0; i < monsterList.size(); i++) {
                    monsterList.get(i).draw(worldG);
                }

                for (int i = 0; i < bulletList.size(); i++) {
                    bulletList.get(i).draw(worldG);
                }

                drawBossCoverDebugHitboxes(worldG);

                for (int i = 0; i < floatingTextList.size(); i++) {
                    if (floatingTextList.get(i) != null) {
                        floatingTextList.get(i).draw(worldG);
                    }
                }
            } finally {
                worldG.dispose();
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

            drawBossPhaseTwoOverlay(g2);
            drawDamageFlash(g2);
            drawDamageEdgeVignette(g2);
        }
    
    }

    private void drawScaledVirtualScreen(Graphics2D g2) {
        int panelW = getWidth();
        int panelH = getHeight();
        double scale = getRenderScale();

        int drawW = (int)Math.round(screenWidth * scale);
        int drawH = (int)Math.round(screenHeight * scale);
        int drawX = (panelW - drawW) / 2;
        int drawY = (panelH - drawH) / 2;

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, panelW, panelH);

        Object oldInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(virtualScreen, drawX, drawY, drawW, drawH, null);
        if (oldInterpolation != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
        }
    }

    private void drawBossArenaBackground(Graphics2D g2) {
        int worldScreenX = -player.x + player.screenX;
        int worldScreenY = -player.y + player.screenY;
        g2.drawImage(bossArenaImage, worldScreenX, worldScreenY,
                maxWorldCol * tileSize, maxWorldRow * tileSize, null);
    }

    private boolean isNormalStageLevel(int level) {
        return level >= FOREST_LEVEL && level <= LAST_NORMAL_LEVEL;
    }

    private boolean isBossLevel(int level) {
        return level == BOSS_LEVEL;
    }

    private int getNextLevelAfterClear(int level) {
        if (level == FOREST_LEVEL) return ICE_LEVEL;
        if (level == ICE_LEVEL) return DESERT_LEVEL;
        if (level == DESERT_LEVEL) return BOSS_LEVEL;
        return NO_PENDING_LEVEL;
    }

    private boolean shouldGrantUpgradeAfterLevel(int level) {
        return isNormalStageLevel(level);
    }

    private int getNormalStageMonsterCount(int level) {
        if (level == FOREST_LEVEL) return 12;
        if (level == ICE_LEVEL) return 15;
        if (level == DESERT_LEVEL) return 18;
        return 0;
    }

    private double getMeleeSpawnRate(int level) {
        if (level == FOREST_LEVEL) return 0.70;
        if (level == ICE_LEVEL) return 0.60;
        if (level == DESERT_LEVEL) return 0.52;
        return 0.60;
    }

    private boolean shouldShowClearPortal() {
        if (mapTransitionInProgress) return false;

        if (isNormalStageLevel(currentLevel)) {
            return normalStageEnemiesSpawned && monsterList.isEmpty();
        }

        if (isBossLevel(currentLevel)) {
            return bossFightStarted && monsterList.isEmpty();
        }

        return false;
    }

    private Rectangle getClearPortalHitbox() {
        return new Rectangle(20 * tileSize, 20 * tileSize, tileSize * 2, tileSize);
    }

    private void handleClearPortalReached() {
        if (gameState != playState || mapTransitionInProgress || levelClearHandled) return;

        if (isBossLevel(currentLevel)) {
            if (bossFightStarted == false || monsterList.isEmpty() == false) return;

            levelClearHandled = true;
            beginEndingStory();
            return;
        }

        if (isNormalStageLevel(currentLevel)) {
            if (normalStageEnemiesSpawned == false || monsterList.isEmpty() == false) return;
            handleNormalStageCleared();
        }
    }

    private void handleNormalStageCleared() {
        if (levelClearHandled) return;

        int nextLevel = getNextLevelAfterClear(currentLevel);
        if (nextLevel == NO_PENDING_LEVEL) return;

        levelClearHandled = true;
        pendingLevelAfterUpgrade = nextLevel;
        upgradeManager.rollUpgrades();
        stopCurrentMusicForUpgrade();
        gameState = upgradeState;
    }

    private boolean isNormalMapLevel() {
        return isNormalMapLevel(currentLevel);
    }

    private boolean isNormalMapLevel(int level) {
        return isNormalStageLevel(level) || isBossLevel(level);
    }

    public boolean isNormalBackgroundMapActive() {
        return isNormalMapLevel() && getCurrentNormalMapBackground() != null;
    }

    private void drawNormalMapBackground(Graphics2D g2) {
        BufferedImage background = getCurrentNormalMapBackground();
        if (background == null) {
            tileM.draw(g2);
            return;
        }

        int worldScreenX = -player.x + player.screenX;
        int worldScreenY = -player.y + player.screenY;
        g2.drawImage(background, worldScreenX, worldScreenY,
                maxWorldCol * tileSize, maxWorldRow * tileSize, null);
    }

    private BufferedImage getCurrentNormalMapBackground() {
        if (currentLevel == FOREST_LEVEL) {
            return forestBackgroundImage;
        }
        if (currentLevel == ICE_LEVEL) {
            return iceBackgroundImage;
        }
        if (currentLevel == DESERT_LEVEL) {
            return desertBackgroundImage;
        }
        if (isBossLevel(currentLevel)) {
            return bossArenaImage;
        }
        return null;
    }

    private void drawNormalMapProps(Graphics2D g2) {
        if (isNormalMapLevel() == false || normalMapProps.isEmpty()) return;

        RenderingHints oldHints = g2.getRenderingHints();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        for (int i = 0; i < normalMapProps.size(); i++) {
            MapProp prop = normalMapProps.get(i);
            if (prop == null || prop.image == null) continue;

            int screenX = prop.worldX - player.x + player.screenX;
            int screenY = prop.worldY - player.y + player.screenY;
            g2.drawImage(prop.image, screenX, screenY, prop.drawWidth, prop.drawHeight, null);
        }

        g2.setRenderingHints(oldHints);
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
        return isBossLevel(currentLevel) && bossCoverFallbackActive && bossArenaCovers.isEmpty() == false;
    }

    private void createNormalMapObstacles(int level) {
        normalMapObstacles.clear();
        normalMapProps.clear();
        normalMapWalkableAreas.clear();
        loadedWalkableAreaPath = null;
        bossPropLayerActive = false;
        bossCoverFallbackActive = false;
        if (isNormalMapLevel(level) == false) return;

        boolean walkableAreasLoaded = loadNormalMapWalkableAreas(level);

        if (isBossLevel(level)) {
            boolean propsLoaded = loadNormalMapPropsForLevel(level);
            if (walkableAreasLoaded && propsLoaded) {
                bossPropLayerActive = true;
                loadBossBoundaryObstacles();
                return;
            }

            normalMapObstacles.clear();
            normalMapProps.clear();
            loadFallbackNormalMapObstacles(level);
            return;
        }

        if (loadNormalMapPropsForLevel(level)) {
            return;
        }

        loadFallbackNormalMapObstacles(level);
    }

    public void reloadCurrentMapData() {
        if (isNormalMapLevel() == false) {
            System.out.println("No map data to reload for level " + currentLevel);
            return;
        }

        createNormalMapObstacles(currentLevel);
        repaint();

        String definitionInfo = (loadedPropDefinitionPath == null) ? "fallback obstacles" : loadedPropDefinitionPath;
        String walkableInfo = (loadedWalkableAreaPath == null) ? "no walkable whitelist" : loadedWalkableAreaPath;
        System.out.println("Reloaded map data for level " + currentLevel + " using "
                + definitionInfo + " and " + walkableInfo);
    }

    private boolean loadNormalMapPropsForLevel(int level) {
        if (loadMapPropDefinitions() == false) {
            warnMapPropIssue("prop-definitions", "Warning: normal map prop definitions unavailable; using fallback obstacles.");
            return false;
        }

        String objectPath = getNormalMapObjectFilePath(level);
        if (objectPath == null) return false;

        if (loadNormalMapObjectsFromFile(objectPath) == false) {
            return false;
        }

        return normalMapProps.isEmpty() == false;
    }

    private void loadFallbackNormalMapObstacles(int level) {
        loadedPropDefinitionPath = null;

        if (isBossLevel(level)) {
            createBossCoverFallbackNormalMapObstacles();
            bossCoverFallbackActive = bossArenaCovers.isEmpty() == false;
            return;
        }

        String obstaclePath = getNormalMapObstacleFilePath(level);
        if (obstaclePath != null && loadNormalMapObstaclesFromFile(obstaclePath)) {
            return;
        }

        createFallbackNormalMapObstacles(level);
    }

    private String getNormalMapObjectFilePath(int level) {
        if (level == FOREST_LEVEL) {
            return "/res/maps/objects/forest_1_objects.txt";
        }
        if (level == ICE_LEVEL) {
            return "/res/maps/objects/ice_1_objects.txt";
        }
        if (level == DESERT_LEVEL) {
            return "/res/maps/objects/desert_1_objects.txt";
        }
        if (isBossLevel(level)) {
            return "/res/maps/objects/boss_1_objects.txt";
        }
        return null;
    }

    private boolean loadNormalMapWalkableAreas(int level) {
        String[] walkablePaths = getNormalMapWalkableAreaPaths(level);
        if (walkablePaths == null) return false;

        for (int i = 0; i < walkablePaths.length; i++) {
            if (loadNormalMapWalkableAreasFromFile(walkablePaths[i])) {
                loadedWalkableAreaPath = walkablePaths[i];
                return true;
            }
        }

        warnWalkableAreaIssue("level-" + level,
                "Warning: no usable walkable area file for level " + level
                        + "; background walkable whitelist disabled.");
        return false;
    }

    private String[] getNormalMapWalkableAreaPaths(int level) {
        if (level == FOREST_LEVEL) {
            return new String[] {"/res/maps/walkable/forest_1_walkable_areas.txt"};
        }
        if (level == ICE_LEVEL) {
            return new String[] {"/res/maps/walkable/ice_1_walkable_areas.txt"};
        }
        if (level == DESERT_LEVEL) {
            return new String[] {"/res/maps/walkable/desert_1_walkable_areas.txt"};
        }
        if (isBossLevel(level)) {
            return new String[] {"/res/maps/walkable/boss_1_walkable_areas.txt"};
        }
        return null;
    }

    private boolean loadNormalMapWalkableAreasFromFile(String path) {
        ArrayList<WalkableArea> loadedAreas = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return false;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int lineNumber = 0;

                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    String trimmed = line.trim();
                    if (trimmed.length() == 0 || trimmed.startsWith("#")) continue;

                    WalkableArea area = parseWalkableAreaLine(path, lineNumber, trimmed);
                    if (area != null) {
                        loadedAreas.add(area);
                    }
                }
            }
        } catch (IOException e) {
            warnWalkableAreaIssue(path, "Warning: could not read walkable area file " + path);
            return false;
        }

        if (loadedAreas.isEmpty()) {
            warnWalkableAreaIssue(path, "Warning: walkable area file has no usable areas " + path);
            return false;
        }

        normalMapWalkableAreas.addAll(loadedAreas);
        return true;
    }

    private WalkableArea parseWalkableAreaLine(String path, int lineNumber, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 5) {
            System.out.println("Warning: invalid walkable area line " + path + ":" + lineNumber + " -> " + line);
            return null;
        }

        try {
            String id = parts[0];
            int worldX = Integer.parseInt(parts[1]);
            int worldY = Integer.parseInt(parts[2]);
            int width = Integer.parseInt(parts[3]);
            int height = Integer.parseInt(parts[4]);

            if (id.length() == 0 || width <= 0 || height <= 0) {
                System.out.println("Warning: invalid walkable area values " + path + ":" + lineNumber + " -> " + line);
                return null;
            }

            return new WalkableArea(id, new Rectangle(worldX, worldY, width, height));
        } catch (NumberFormatException e) {
            System.out.println("Warning: invalid walkable area number " + path + ":" + lineNumber + " -> " + line);
            return null;
        }
    }

    private void warnWalkableAreaIssue(String key, String message) {
        if (warnedWalkableAreaIssues.add(key)) {
            System.out.println(message);
        }
    }

    private boolean loadBossBoundaryObstacles() {
        return loadNormalMapObstaclesFromFile("/res/maps/obstacles/boss_1_boundaries.txt", "boundary");
    }

    private boolean loadMapPropDefinitions() {
        propDefinitions.clear();
        loadedPropDefinitionPath = null;

        String[] definitionPaths = {
                "/res/maps/props/prop_definition.txt",
                "/res/maps/props/prop_definitions.txt",
                "/res/maps/props/props_definitions.txt"
        };

        for (int i = 0; i < definitionPaths.length; i++) {
            if (loadMapPropDefinitionsFromFile(definitionPaths[i])) {
                loadedPropDefinitionPath = definitionPaths[i];
                return true;
            }
        }

        return false;
    }

    private boolean loadMapPropDefinitionsFromFile(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                return false;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int lineNumber = 0;

                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    String trimmed = line.trim();
                    if (trimmed.length() == 0 || trimmed.startsWith("#")) continue;

                    MapPropDefinition definition = parseMapPropDefinitionLine(path, lineNumber, trimmed);
                    if (definition != null) {
                        propDefinitions.put(definition.type, definition);
                    }
                }
            }
        } catch (IOException e) {
            warnMapPropIssue(path, "Warning: could not read prop definition file " + path);
            return false;
        }

        if (propDefinitions.isEmpty()) {
            warnMapPropIssue(path, "Warning: prop definition file has no usable definitions " + path);
            return false;
        }
        return true;
    }

    private MapPropDefinition parseMapPropDefinitionLine(String path, int lineNumber, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 7 && parts.length != 8) {
            System.out.println("Warning: invalid prop definition line " + path + ":" + lineNumber + " -> " + line);
            return null;
        }

        String type = parts[0];
        String imagePath = parts[1];
        int fieldOffset = isNumericToken(parts[2]) ? 1 : 0;

        if ((fieldOffset == 1 && parts.length != 8) || (fieldOffset == 0 && parts.length != 7)) {
            System.out.println("Warning: invalid prop definition line " + path + ":" + lineNumber + " -> " + line);
            return null;
        }

        String collisionMode = parts[2 + fieldOffset].toLowerCase();

        if (type.length() == 0 || ("none".equals(collisionMode) == false && "bbox".equals(collisionMode) == false)) {
            System.out.println("Warning: invalid prop definition values " + path + ":" + lineNumber + " -> " + line);
            return null;
        }

        try {
            double scale = (fieldOffset == 1) ? Double.parseDouble(parts[2]) : 1.0;
            double shrinkX = Double.parseDouble(parts[3 + fieldOffset]);
            double shrinkY = Double.parseDouble(parts[4 + fieldOffset]);
            Boolean blocksMovement = parseObstacleBoolean(parts[5 + fieldOffset]);
            Boolean blocksProjectiles = parseObstacleBoolean(parts[6 + fieldOffset]);

            if (scale <= 0.0 || shrinkX < 0.0 || shrinkX >= 1.0 || shrinkY < 0.0 || shrinkY >= 1.0
                    || blocksMovement == null || blocksProjectiles == null) {
                System.out.println("Warning: invalid prop definition values " + path + ":" + lineNumber + " -> " + line);
                return null;
            }

            BufferedImage image = loadMapPropImage(imagePath, path, lineNumber);
            if (image == null) return null;

            return new MapPropDefinition(type, imagePath, image, scale, collisionMode,
                    shrinkX, shrinkY, blocksMovement.booleanValue(), blocksProjectiles.booleanValue());
        } catch (NumberFormatException e) {
            System.out.println("Warning: invalid prop definition number " + path + ":" + lineNumber + " -> " + line);
            return null;
        }
    }

    private boolean isNumericToken(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private BufferedImage loadMapPropImage(String imagePath, String definitionPath, int lineNumber) {
        try (InputStream is = getClass().getResourceAsStream(imagePath)) {
            if (is == null) {
                warnMapPropIssue(imagePath, "Warning: missing prop image " + imagePath + " referenced by "
                        + definitionPath + ":" + lineNumber);
                return null;
            }

            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                warnMapPropIssue(imagePath, "Warning: could not read prop image " + imagePath + " referenced by "
                        + definitionPath + ":" + lineNumber);
            }
            return image;
        } catch (IOException e) {
            warnMapPropIssue(imagePath, "Warning: could not load prop image " + imagePath + " referenced by "
                    + definitionPath + ":" + lineNumber);
            return null;
        }
    }

    private boolean loadNormalMapObjectsFromFile(String path) {
        int validPropCount = 0;

        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                warnMapPropIssue(path, "Warning: missing normal map object file " + path + "; using fallback obstacles.");
                return false;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int lineNumber = 0;

                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    String trimmed = line.trim();
                    if (trimmed.length() == 0 || trimmed.startsWith("#")) continue;

                    if (parseNormalMapObjectLine(path, lineNumber, trimmed)) {
                        validPropCount++;
                    }
                }
            }
        } catch (IOException e) {
            warnMapPropIssue(path, "Warning: could not read normal map object file " + path + "; using fallback obstacles.");
            return false;
        }

        if (validPropCount == 0) {
            warnMapPropIssue(path, "Warning: normal map object file has no usable props " + path + "; using fallback obstacles.");
            return false;
        }
        return true;
    }

    private boolean parseNormalMapObjectLine(String path, int lineNumber, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3) {
            System.out.println("Warning: invalid map object line " + path + ":" + lineNumber + " -> " + line);
            return false;
        }

        MapPropDefinition definition = propDefinitions.get(parts[0]);
        if (definition == null) {
            System.out.println("Warning: unknown map prop type " + path + ":" + lineNumber + " -> " + parts[0]);
            return false;
        }
        if (isBossObjectFile(path) && isBlockingPropDefinition(definition) == false) {
            System.out.println("Warning: boss object prop must be blocking cover "
                    + path + ":" + lineNumber + " -> " + parts[0]);
            return false;
        }

        try {
            int worldX = Integer.parseInt(parts[1]);
            int worldY = Integer.parseInt(parts[2]);
            addNormalMapProp(definition, worldX, worldY);
            return true;
        } catch (NumberFormatException e) {
            System.out.println("Warning: invalid map object coordinates " + path + ":" + lineNumber + " -> " + line);
            return false;
        }
    }

    private boolean isBossObjectFile(String path) {
        return "/res/maps/objects/boss_1_objects.txt".equals(path);
    }

    private boolean isBlockingPropDefinition(MapPropDefinition definition) {
        return definition != null
                && "bbox".equals(definition.collisionMode)
                && definition.blocksMovement
                && definition.blocksProjectiles;
    }

    private void addNormalMapProp(MapPropDefinition definition, int worldX, int worldY) {
        int imageWidth = definition.image.getWidth();
        int imageHeight = definition.image.getHeight();
        int drawWidth = Math.max(1, (int)Math.round(imageWidth * definition.scale));
        int drawHeight = Math.max(1, (int)Math.round(imageHeight * definition.scale));
        String propId = definition.type + "#" + (normalMapProps.size() + 1);
        Rectangle collisionRect = null;

        if ("bbox".equals(definition.collisionMode)) {
            collisionRect = createMapPropCollisionRect(worldX, worldY, drawWidth, drawHeight,
                    definition.shrinkX, definition.shrinkY, definition.type);
            normalMapObstacles.add(new NormalMapObstacle(propId, collisionRect,
                    definition.blocksMovement, definition.blocksProjectiles, "prop"));
        }

        normalMapProps.add(new MapProp(propId, definition.type, definition.image,
                worldX, worldY, imageWidth, imageHeight, drawWidth, drawHeight,
                definition.scale, collisionRect));
    }

    private Rectangle createMapPropCollisionRect(int worldX, int worldY, int drawWidth, int drawHeight,
                                                 double shrinkX, double shrinkY, String propType) {
        int collisionW = Math.max(1, (int)Math.round(drawWidth * (1.0 - shrinkX)));
        int collisionH = Math.max(1, (int)Math.round(drawHeight * (1.0 - shrinkY)));
        int collisionX = worldX + (drawWidth - collisionW) / 2;
        int collisionY;

        if (usesBottomAnchoredCollision(propType, drawWidth, drawHeight)) {
            int bottomInset = Math.max(2, (int)Math.round(drawHeight * 0.08));
            collisionY = worldY + drawHeight - collisionH - bottomInset;
            if (collisionY < worldY) collisionY = worldY;
        } else {
            collisionY = worldY + (drawHeight - collisionH) / 2;
        }

        return new Rectangle(collisionX, collisionY, collisionW, collisionH);
    }

    private boolean usesBottomAnchoredCollision(String propType, int drawWidth, int drawHeight) {
        if (propType == null || propType.startsWith("boss_") == false) return false;
        return drawHeight > drawWidth * 1.1;
    }

    private void warnMapPropIssue(String key, String message) {
        if (warnedMapPropIssues.add(key)) {
            System.out.println(message);
        }
    }

    private String getNormalMapObstacleFilePath(int level) {
        if (level == FOREST_LEVEL) {
            return "/res/maps/obstacles/forest_1_obstacles.txt";
        }
        if (level == ICE_LEVEL) {
            return "/res/maps/obstacles/ice_1_obstacles.txt";
        }
        if (level == DESERT_LEVEL) {
            return "/res/maps/obstacles/desert_1_obstacles.txt";
        }
        return null;
    }

    private boolean loadNormalMapObstaclesFromFile(String path) {
        return loadNormalMapObstaclesFromFile(path, "file");
    }

    private boolean loadNormalMapObstaclesFromFile(String path, String source) {
        ArrayList<NormalMapObstacle> loadedObstacles = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                warnNormalObstacleFallback(path, "missing obstacle file");
                return false;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                int lineNumber = 0;

                while ((line = br.readLine()) != null) {
                    lineNumber++;
                    String trimmed = line.trim();
                    if (trimmed.length() == 0 || trimmed.startsWith("#")) continue;

                    NormalMapObstacle obstacle = parseNormalMapObstacleLine(path, lineNumber, trimmed);
                    if (obstacle != null) {
                        obstacle.source = source;
                        loadedObstacles.add(obstacle);
                    }
                }
            }
        } catch (IOException e) {
            warnNormalObstacleFallback(path, "could not read obstacle file");
            return false;
        }

        if (loadedObstacles.isEmpty()) {
            warnNormalObstacleFallback(path, "no valid obstacles found");
            return false;
        }

        normalMapObstacles.addAll(loadedObstacles);
        return true;
    }

    private NormalMapObstacle parseNormalMapObstacleLine(String path, int lineNumber, String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 7) {
            System.out.println("Warning: invalid obstacle line " + path + ":" + lineNumber + " -> " + line);
            return null;
        }

        try {
            String id = parts[0];
            int worldX = Integer.parseInt(parts[1]);
            int worldY = Integer.parseInt(parts[2]);
            int width = Integer.parseInt(parts[3]);
            int height = Integer.parseInt(parts[4]);
            Boolean blocksMovement = parseObstacleBoolean(parts[5]);
            Boolean blocksProjectiles = parseObstacleBoolean(parts[6]);

            if (id.length() == 0 || width <= 0 || height <= 0 || blocksMovement == null || blocksProjectiles == null) {
                System.out.println("Warning: invalid obstacle values " + path + ":" + lineNumber + " -> " + line);
                return null;
            }

            return new NormalMapObstacle(id, new Rectangle(worldX, worldY, width, height),
                    blocksMovement.booleanValue(), blocksProjectiles.booleanValue());
        } catch (NumberFormatException e) {
            System.out.println("Warning: invalid obstacle number " + path + ":" + lineNumber + " -> " + line);
            return null;
        }
    }

    private Boolean parseObstacleBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;
        return null;
    }

    private void warnNormalObstacleFallback(String path, String reason) {
        if (warnedNormalObstacleFallbacks.add(path)) {
            System.out.println("Warning: " + reason + " " + path + "; using fallback normal map obstacles.");
        }
    }

    private void createFallbackNormalMapObstacles(int level) {
        int variation = (level - 1) % 3;
        addNormalMapObstacleForVariation("A", 5, 7, 4, 3, variation);
        addNormalMapObstacleForVariation("B", 21, 7, 4, 3, variation);
        addNormalMapObstacleForVariation("C", 4, 15, 4, 2, variation);
        addNormalMapObstacleForVariation("D", 22, 15, 4, 2, variation);
        addNormalMapObstacleForVariation("E", 7, 22, 4, 3, variation);
        addNormalMapObstacleForVariation("F", 19, 22, 4, 3, variation);
        addNormalMapObstacleForVariation("G", 12, 4, 6, 2, variation);
        addNormalMapObstacleForVariation("H", 12, 25, 6, 2, variation);
        addNormalMapObstacleForVariation("I", 2, 11, 2, 6, variation);
        addNormalMapObstacleForVariation("J", 26, 11, 2, 6, variation);
    }

    private void createBossCoverFallbackNormalMapObstacles() {
        for (int i = 0; i < bossArenaCovers.size(); i++) {
            BossArenaCover cover = bossArenaCovers.get(i);
            if (cover == null || cover.hitbox == null) continue;

            normalMapObstacles.add(new NormalMapObstacle(
                    "boss_cover_fallback_" + (i + 1),
                    new Rectangle(cover.hitbox),
                    true,
                    true,
                    "boss_fallback"));
        }
    }

    private void addNormalMapObstacleForVariation(String id, int tileX, int tileY, int tileW, int tileH, int variation) {
        int adjustedTileX = tileX;
        int adjustedTileY = tileY;
        String adjustedId = id;

        if (variation == 1) {
            adjustedTileX = maxWorldCol - tileX - tileW;
            adjustedId = id + "m";
        } else if (variation == 2) {
            if ("C".equals(id)) {
                adjustedTileY += 1;
                adjustedId = id + "s";
            } else if ("F".equals(id)) {
                adjustedTileX -= 1;
                adjustedId = id + "s";
            }
        }

        addNormalMapObstacle(adjustedId, adjustedTileX, adjustedTileY, tileW, tileH, true, true);
    }

    private void addNormalMapObstacle(String id, int tileX, int tileY, int tileW, int tileH,
                                      boolean blocksMovement, boolean blocksProjectiles) {
        int worldX = tileX * tileSize;
        int worldY = tileY * tileSize;
        int fullWidth = tileW * tileSize;
        int fullHeight = tileH * tileSize;
        int insetX = Math.max(4, (int)Math.round(fullWidth * 0.08));
        int insetY = Math.max(4, (int)Math.round(fullHeight * 0.08));

        Rectangle hitbox = new Rectangle(worldX + insetX, worldY + insetY,
                Math.max(1, fullWidth - insetX * 2),
                Math.max(1, fullHeight - insetY * 2));
        normalMapObstacles.add(new NormalMapObstacle(id, hitbox, blocksMovement, blocksProjectiles, "fallback"));
    }

    public boolean collidesWithNormalMapObstacle(Rectangle worldArea, boolean projectile) {
        if (isNormalMapLevel() == false || worldArea == null || normalMapObstacles.isEmpty()) return false;

        for (int i = 0; i < normalMapObstacles.size(); i++) {
            NormalMapObstacle obstacle = normalMapObstacles.get(i);
            if (obstacle == null || obstacle.worldHitbox == null) continue;
            if (projectile == true && obstacle.blocksProjectiles == false) continue;
            if (projectile == false && obstacle.blocksMovement == false) continue;

            if (worldArea.intersects(obstacle.worldHitbox)) {
                return true;
            }
        }

        return false;
    }

    public boolean collidesWithMapCollision(Rectangle worldArea, boolean projectile) {
        if (worldArea == null) return false;

        if (isNormalBackgroundMapActive()) {
            if (collidesWithWorldBounds(worldArea)) {
                return true;
            }
            if (collidesWithWalkableAreaBoundary(worldArea, projectile)) {
                return true;
            }
            if (collidesWithBossArenaCover(worldArea)) {
                return true;
            }
            return collidesWithNormalMapObstacle(worldArea, projectile);
        }

        return false;
    }

    private boolean collidesWithWorldBounds(Rectangle worldArea) {
        int worldWidth = maxWorldCol * tileSize;
        int worldHeight = maxWorldRow * tileSize;
        return worldArea.x < 0
                || worldArea.y < 0
                || worldArea.x + worldArea.width >= worldWidth
                || worldArea.y + worldArea.height >= worldHeight;
    }

    public boolean collidesWithWalkableAreaBoundary(Rectangle worldArea, boolean projectile) {
        if (isNormalBackgroundMapActive() == false || worldArea == null || normalMapWalkableAreas.isEmpty()) {
            return false;
        }

        if (projectile) {
            int centerX = worldArea.x + worldArea.width / 2;
            int centerY = worldArea.y + worldArea.height / 2;
            return isPointInsideNormalMapWalkableArea(centerX, centerY) == false;
        }

        return isWorldHitboxInsideNormalMapWalkableArea(worldArea) == false;
    }

    private boolean isWorldHitboxInsideNormalMapWalkableArea(Rectangle worldArea) {
        int centerX = worldArea.x + worldArea.width / 2;
        int centerY = worldArea.y + worldArea.height / 2;
        int insetX = Math.max(1, Math.min(6, worldArea.width / 4));
        int insetY = Math.max(1, Math.min(6, worldArea.height / 4));
        int left = worldArea.x + insetX;
        int right = worldArea.x + worldArea.width - 1 - insetX;
        int top = worldArea.y + insetY;
        int bottom = worldArea.y + worldArea.height - 1 - insetY;

        if (right < left) {
            left = centerX;
            right = centerX;
        }
        if (bottom < top) {
            top = centerY;
            bottom = centerY;
        }

        return isPointInsideNormalMapWalkableArea(centerX, centerY)
                && isPointInsideNormalMapWalkableArea(left, top)
                && isPointInsideNormalMapWalkableArea(right, top)
                && isPointInsideNormalMapWalkableArea(left, bottom)
                && isPointInsideNormalMapWalkableArea(right, bottom);
    }

    private boolean isPointInsideNormalMapWalkableArea(int worldX, int worldY) {
        for (int i = 0; i < normalMapWalkableAreas.size(); i++) {
            WalkableArea area = normalMapWalkableAreas.get(i);
            if (area != null && area.worldArea != null && area.worldArea.contains(worldX, worldY)) {
                return true;
            }
        }
        return false;
    }

    private void drawBossCoverDebugHitboxes(Graphics2D g2) {
        if (DEBUG_BOSS_COVER_HITBOX == false || isLevel10BossArenaCoverActive() == false) return;

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

    private void drawNormalMapObstacleDebug(Graphics2D g2) {
        if (DEBUG_NORMAL_MAP_OBSTACLES == false || isNormalMapLevel() == false) return;

        Composite oldComposite = g2.getComposite();

        drawActiveTileCollisionDebug(g2);
        drawWorldBoundsDebug(g2);
        drawWalkableAreaDebug(g2);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
        g2.setColor(new Color(255, 210, 0));
        for (int i = 0; i < normalMapProps.size(); i++) {
            MapProp prop = normalMapProps.get(i);
            if (prop == null || prop.image == null) continue;

            int screenX = prop.worldX - player.x + player.screenX;
            int screenY = prop.worldY - player.y + player.screenY;
            g2.fillRect(screenX, screenY, prop.drawWidth, prop.drawHeight);
        }

        for (int i = 0; i < normalMapObstacles.size(); i++) {
            NormalMapObstacle obstacle = normalMapObstacles.get(i);
            if (obstacle == null || obstacle.worldHitbox == null) continue;

            int screenX = obstacle.worldHitbox.x - player.x + player.screenX;
            int screenY = obstacle.worldHitbox.y - player.y + player.screenY;
            Color obstacleColor = getNormalMapDebugColor(obstacle);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g2.setColor(obstacleColor);
            g2.fillRect(screenX, screenY, obstacle.worldHitbox.width, obstacle.worldHitbox.height);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
            g2.setColor(Color.WHITE);
            g2.drawString(getNormalMapDebugLabel(obstacle.id) + getNormalMapDebugFlags(obstacle),
                    screenX + 4, screenY + 14);
        }

        g2.setComposite(oldComposite);
    }

    private Color getNormalMapDebugColor(NormalMapObstacle obstacle) {
        if (obstacle == null) return new Color(255, 90, 180);
        if ("prop".equals(obstacle.source)) return new Color(20, 220, 100);
        if ("boundary".equals(obstacle.source)) return new Color(255, 75, 75);
        if ("boss_fallback".equals(obstacle.source)) return new Color(255, 90, 180);
        return new Color(255, 150, 40);
    }

    private void drawWalkableAreaDebug(Graphics2D g2) {
        if (normalMapWalkableAreas.isEmpty()) return;

        for (int i = 0; i < normalMapWalkableAreas.size(); i++) {
            WalkableArea area = normalMapWalkableAreas.get(i);
            if (area == null || area.worldArea == null) continue;

            int screenX = area.worldArea.x - player.x + player.screenX;
            int screenY = area.worldArea.y - player.y + player.screenY;

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            g2.setColor(new Color(0, 210, 255));
            g2.fillRect(screenX, screenY, area.worldArea.width, area.worldArea.height);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.80f));
            g2.setColor(new Color(95, 235, 255));
            g2.drawRect(screenX, screenY, area.worldArea.width, area.worldArea.height);
            g2.drawString(area.id, screenX + 4, screenY + 14);
        }
    }

    private void drawActiveTileCollisionDebug(Graphics2D g2) {
        if (isNormalBackgroundMapActive()) return;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
        g2.setColor(new Color(255, 80, 40));
        for (int col = 0; col < maxWorldCol; col++) {
            for (int row = 0; row < maxWorldRow; row++) {
                int tileNum = tileM.mapTileNum[col][row];
                if (tileM.isCollisionTile(tileNum) == false) continue;

                int screenX = col * tileSize - player.x + player.screenX;
                int screenY = row * tileSize - player.y + player.screenY;
                g2.fillRect(screenX, screenY, tileSize, tileSize);
            }
        }
    }

    private void drawWorldBoundsDebug(Graphics2D g2) {
        int screenX = -player.x + player.screenX;
        int screenY = -player.y + player.screenY;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
        g2.setColor(new Color(255, 70, 70));
        g2.drawRect(screenX, screenY, maxWorldCol * tileSize, maxWorldRow * tileSize);
        g2.drawString("world_bounds", screenX + 6, screenY + 16);
    }

    private String getNormalMapDebugLabel(String obstacleId) {
        for (int i = 0; i < normalMapProps.size(); i++) {
            MapProp prop = normalMapProps.get(i);
            if (prop != null && obstacleId.equals(prop.id)) {
                return prop.type + " x" + String.format("%.2f", prop.scale);
            }
        }
        return obstacleId;
    }

    private String getNormalMapDebugFlags(NormalMapObstacle obstacle) {
        if (obstacle == null) return "";
        String movement = obstacle.blocksMovement ? "M" : "-";
        String projectile = obstacle.blocksProjectiles ? "P" : "-";
        return " [" + movement + projectile + "]";
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

    public int spawnMonsters(int level) {
        // The boss stage only spawns the final boss.
        if (isBossLevel(level)) {
            int bossWorldX = (maxWorldCol / 2) * tileSize;
            int bossWorldY = (maxWorldRow / 4) * tileSize;
            
            Monster boss = new Monster(this, bossWorldX, bossWorldY, 3);
            difficulty.applyBossStats(boss);
            monsterList.add(boss);
            bossFightStarted = true;
            return 1;
        }
        if (isNormalStageLevel(level) == false) return 0;

        // Enemy count and types scale with level.
        int monsterCount = difficulty.applyEnemyCount(getNormalStageMonsterCount(level));
        int spawned = 0;
        
        int attempts = 0; 
        boolean useImageMapCollision = isNormalBackgroundMapActive();
        
        // Stop trying if the map has no good spawn spots.
        while (spawned < monsterCount && attempts < 800) {
            attempts++; 
            
            int col = (int)(Math.random() * (maxWorldCol - 2)) + 1;
            int row = (int)(Math.random() * (maxWorldRow - 2)) + 1;

            if (useImageMapCollision || tileM.mapTileNum[col][row] == 0) {
                int distance = Math.abs(col - 15) + Math.abs(row - 15);
                
                if (distance > 8) { 
                    int worldX = col * tileSize;
                    int worldY = row * tileSize;
                    Rectangle spawnHitbox = new Rectangle(worldX + 8, worldY + 8, 32, 32);
                    if (useImageMapCollision && collidesWithMapCollision(spawnHitbox, false)) {
                        continue;
                    }
                    if (useImageMapCollision == false && collidesWithNormalMapObstacle(spawnHitbox, false)) {
                        continue;
                    }

                    // Later levels add more ranged monsters.
                    double meleeRate = getMeleeSpawnRate(level);
                    int type = (Math.random() < meleeRate) ? 1 : 2; 
                    
                    Monster m = new Monster(this, worldX, worldY, type);
                    
                    // Elite monsters start appearing from level 3.
                    double eliteChance = Math.min(0.25, 0.08 + level * 0.015);
                    if (level == DESERT_LEVEL && type == 1 && Math.random() < eliteChance) {
                        m.transformToElite(); 
                    }

                    // Extra scaling keeps item drops from making later levels too easy.
                    m.maxHp += Math.max(0, level / 2);
                    if (level == DESERT_LEVEL) m.speed += 1;
                    
                    difficulty.applyMonsterStats(m);
                    
                    if (m.isElite == false) m.hp = m.maxHp; 
                    
                    monsterList.add(m);
                    spawned++; 
                }
            }
        }
        return spawned;
    }

    public void nextLevel() {
        player.resetSwordCombatState();

        if (gameState != playState || isNormalStageLevel(currentLevel) == false || levelClearHandled) return;

        handleNormalStageCleared();
    }

    public void selectUpgrade(int choiceIndex) {
        if (pendingLevelAfterUpgrade == NO_PENDING_LEVEL) {
            return;
        }

        if (upgradeManager.applySelectedUpgrade(choiceIndex) == false) {
            return;
        }

        statsTracker.recordUpgradeChosen();
        int nextLevel = pendingLevelAfterUpgrade;
        pendingLevelAfterUpgrade = NO_PENDING_LEVEL;

        if (nextLevel == BOSS_LEVEL) {
            transitionToLevelWithStory(nextLevel);
        } else {
            transitionToNewMap(nextLevel);
            gameState = playState;
        }
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
        currentLevel = FOREST_LEVEL;
        pendingLevelAfterUpgrade = NO_PENDING_LEVEL;
        levelClearHandled = false;
        bossFightStarted = false;
        mapTransitionInProgress = false;
        normalStageEnemiesSpawned = false;
        resetDamageFeedbackEffects();
        resetBossPhaseTwoPresentation();
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
            transitionToNewMap(BOSS_LEVEL);
        } else if (action == StoryAction.SHOW_WIN) {
            gameState = gameWinState;
        }
    }

    private void transitionToLevelWithStory(int level) {
        if (isBossLevel(level) && storyManager.shouldShowPreBossStory()) {
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
        playLobbyMusic();
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
        currentMusicTrack = i;
        // Apply the current menu volume.
        music.setVolume(getVolumeDecibels(musicVolume)); 
        music.play();
        music.loop(); 
    }

    public void stopMusic() {
        music.stop();
        currentMusicTrack = -1;
    }

    private void playMusicIfChanged(int index) {
        if (currentMusicTrack == index) return;
        playMusic(index);
    }

    private void playLobbyMusic() {
        playMusicIfChanged(MUSIC_LOBBY);
    }

    private void playMusicForLevel(int level) {
        if (level == FOREST_LEVEL) {
            playMusicIfChanged(MUSIC_LEVEL1_FOREST);
        } else if (level == ICE_LEVEL) {
            playMusicIfChanged(MUSIC_LEVEL2_ICE);
        } else if (level == DESERT_LEVEL) {
            playMusicIfChanged(MUSIC_LEVEL3_DESERT);
        } else if (isBossLevel(level)) {
            playMusicIfChanged(MUSIC_BOSS);
        } else {
            stopMusic();
        }
    }

    private void stopCurrentMusicForUpgrade() {
        stopMusic();
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

    private void generateBossPhaseTwoBurstParticles(Monster boss) {
        int centerX = boss.x + tileSize;
        int centerY = boss.y + tileSize;
        Color[] colors = {
                new Color(115, 25, 190),
                new Color(210, 45, 210),
                new Color(165, 20, 95),
                new Color(55, 15, 90),
                new Color(245, 120, 220)
        };

        for (int k = 0; k < 78; k++) {
            double angle = (Math.PI * 2.0 * k / 78.0) + (Math.random() - 0.5) * 0.42;
            double speed = 1.4 + Math.random() * 6.2;
            if (k % 5 == 0) speed *= 0.45;

            double xVel = Math.cos(angle) * speed;
            double yVel = Math.sin(angle) * speed;
            int size = 3 + (int)(Math.random() * 7);
            int life = 38 + (int)(Math.random() * 42);
            Color color = colors[(int)(Math.random() * colors.length)];

            particleList.add(new Particle(this, centerX, centerY, color, size, xVel, yVel, life));
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

    // Map transitions are guarded so progression cannot run while a level is half-loaded.
    public void transitionToNewMap(int level) {
        mapTransitionInProgress = true;
        try {
            currentLevel = level;
            pendingLevelAfterUpgrade = NO_PENDING_LEVEL;
            levelClearHandled = false;
            bossFightStarted = false;
            normalStageEnemiesSpawned = false;
            resetDamageFeedbackEffects();
            resetBossPhaseTwoPresentation();
            if (level == FOREST_LEVEL) {
                statsTracker.startRun(level);
            }
            statsTracker.setLevelReached(level);

            bulletList.clear();
            monsterList.clear();
            itemList.clear();

            // Shortened stages use fixed themes.
            if (level == FOREST_LEVEL) {
                currentTheme = THEME_FOREST;
            } else if (level == ICE_LEVEL) {
                currentTheme = THEME_DUNGEON;
            } else if (level == DESERT_LEVEL) {
                currentTheme = THEME_DESERT;
            } else if (isBossLevel(level)) {
                currentTheme = THEME_DUNGEON;
            }

            // Reload tile images before reading the new map.
            tileM.getTileInfo();
            tileM.loadMap(level);
            createNormalMapObstacles(level);

            // No auto-heal between levels; hearts are the healing source.

            if (isBossLevel(level)) {
                player.x = (maxWorldCol / 2) * tileSize;
                player.y = (maxWorldRow - 4) * tileSize;
            } else {
                player.x = tileSize * 15;
                player.y = tileSize * 15;
            }
            player.resetSwordCombatState();
            player.invincible = true;
            player.invincibleCounter = 0;

            int spawnedCount = spawnMonsters(level);
            if (isNormalStageLevel(level)) {
                normalStageEnemiesSpawned = spawnedCount > 0;
                if (normalStageEnemiesSpawned == false) {
                    System.out.println("Warning: no monsters spawned for level " + level);
                }
            }

            playMusicForLevel(level);
        } finally {
            mapTransitionInProgress = false;
        }
    }

    public void handleMonsterDefeated(Monster defeatedMonster) {
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
        currentLevel = FOREST_LEVEL;
        pendingLevelAfterUpgrade = NO_PENDING_LEVEL;
        levelClearHandled = false;
        bossFightStarted = false;
        mapTransitionInProgress = false;
        normalStageEnemiesSpawned = false;
        resetDamageFeedbackEffects();
        resetBossPhaseTwoPresentation();
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
