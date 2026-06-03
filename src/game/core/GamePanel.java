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
import game.system.UpgradeManager;
import game.tile.TileManager;
import game.ui.UI;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
    private final String saveFileName = "save.dat";

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
        loadBestScore();

        playMusic(6);
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start(); 
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

                if (m.getBounds().intersects(player.getBounds()) && player.invincible == false) {
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
                Rectangle doorWorldHitbox = new Rectangle(20 * tileSize, 20 * tileSize, tileSize * 2, tileSize);
                
                if (player.getBounds().intersects(doorWorldHitbox)) {
                    if (currentLevel < 10) { 
                        nextLevel();
                    } else {
                        statsTracker.endRun();
                        gameState = gameWinState; 
                        stopMusic();
                        playSE(7);
                    }
                }
            } 
            
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        if (gameState == titleState) {
            ui.drawTitleScreen(g2);
        } else {
            tileM.draw(g2); 
            
           // Portal appears after the room is cleared.
        if (monsterList.isEmpty()) {
            int doorWorldX = 20 * tileSize;
            int doorWorldY = 20 * tileSize;

            int doorScreenX = doorWorldX - player.x + player.screenX;
            int doorScreenY = doorWorldY - player.y + player.screenY;

            g2.setColor(Color.CYAN); 
            g2.fillRect(doorScreenX, doorScreenY, tileSize * 2, tileSize);
            
            g2.setColor(Color.WHITE);
            String msg = (currentLevel < 10) ? "PORTAL" : "VICTORY";
            g2.drawString(msg, doorScreenX, doorScreenY - 10);
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
            transitionToNewMap(currentLevel);
        }
    }

    public void selectUpgrade(int choiceIndex) {
        if (upgradeManager.applySelectedUpgrade(choiceIndex)) {
            statsTracker.recordUpgradeChosen();
        }
        gameState = playState;
        transitionToNewMap(currentLevel);
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

    private void startRunWithClass(int classType) {
        gameState = playState;
        currentLevel = 1;
        player.setDefaultValues();
        particleList.clear();
        player.setupClass(classType);
        transitionToNewMap(currentLevel);
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

        player.x = tileSize * 15; 
        player.y = tileSize * 15;
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
        statsTracker.recordEnemyKilled();
        addScore(getMonsterScore(defeatedMonster));
        generateParticles(defeatedMonster.x, defeatedMonster.y);
        spawnItemDrop(defeatedMonster);
    }

    public void spawnItemDrop(Monster defeatedMonster) {
        if (defeatedMonster == null) return;

        // Boss drops a small fixed reward set.
        if (defeatedMonster.type == 3) {
            int[] bossDrops = { Item.TYPE_HEART, Item.TYPE_ENERGY, Item.TYPE_COIN };
            for (int i = 0; i < bossDrops.length; i++) {
                int offsetX = (i - 1) * 32;
                itemList.add(new Item(this, defeatedMonster.x + tileSize - 14 + offsetX, defeatedMonster.y + tileSize - 14, bossDrops[i]));
            }
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
