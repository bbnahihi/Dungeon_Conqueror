package game.ui;

import game.core.GamePanel;
import game.entity.Monster;
import game.system.StatsTracker;
import game.system.Upgrade;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class UI {
    
    GamePanel gp;
    
    Font arial_30;
    Font arial_60B;
    
    public BufferedImage titleBg;
    public int titleBlinkCounter = 0;
    public int commandNum = 0;
    public BufferedImage menuBackground;
    public int levelClearCounter = 0;
    public UI(GamePanel gp) {
        this.gp = gp;
        
        arial_30 = new Font("Arial", Font.BOLD, 30);
        arial_60B = new Font("Arial", Font.BOLD, 60);

        // Menu background is optional.
        try {
            InputStream is = getClass().getResourceAsStream("/res/menu_bg.png");
            if (is != null) {
                menuBackground = ImageIO.read(is);
            }
        } catch (Exception e) {
            menuBackground = null;
        }
    }

    private Rectangle centeredRowBounds(int baselineY, int width, int height) {
        return new Rectangle(gp.screenWidth / 2 - width / 2, baselineY - height + 10, width, height);
    }

    public Rectangle getTitleMenuBounds(int index) {
        int[] yPositions = {350, 420, 490, 560};
        if (index < 0 || index >= yPositions.length) return new Rectangle();
        return centeredRowBounds(yPositions[index], 430, 54);
    }

    public Rectangle getCharacterChoiceBounds(int index) {
        int[] yPositions = {300, 400};
        if (index < 0 || index >= yPositions.length) return new Rectangle();
        return centeredRowBounds(yPositions[index], 620, 60);
    }

    public Rectangle getOptionsRowBounds(int index) {
        if (index == 0) return new Rectangle(260, 255, 450, 70);
        if (index == 1) return new Rectangle(260, 355, 450, 70);
        if (index == 2) return new Rectangle(260, 465, 200, 55);
        return new Rectangle();
    }

    public Rectangle getMusicVolumeBounds() {
        return new Rectangle(500, 275, 150, 30);
    }

    public Rectangle getSfxVolumeBounds() {
        return new Rectangle(530, 375, 150, 30);
    }

    public Rectangle getPauseMenuBounds(int index) {
        int[] yPositions = {300, 360, 420, 480, 540};
        if (index < 0 || index >= yPositions.length) return new Rectangle();
        return centeredRowBounds(yPositions[index], 360, 52);
    }

    public Rectangle getUpgradeChoiceBounds(int index) {
        if (index < 0 || index >= 3) return new Rectangle();
        int boxWidth = 500;
        int boxHeight = 80;
        int boxX = gp.screenWidth / 2 - boxWidth / 2;
        int boxY = 180 + (index * 120);
        return new Rectangle(boxX, boxY, boxWidth, boxHeight);
    }

    public Rectangle getGameOverRestartBounds() {
        return centeredRowBounds(gp.screenHeight / 2 + 100, 430, 56);
    }

    public Rectangle getGameWinMainMenuBounds() {
        return centeredRowBounds(gp.screenHeight / 2 + 50, 520, 56);
    }
    
    public void draw(Graphics2D g2) {

        // Draw HUD while gameplay is visible.
        if (gp.gameState == gp.playState || gp.gameState == gp.pauseState) {
            
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            g2.drawString("LEVEL: " + gp.currentLevel, 20, 30);
            
            g2.drawString("SCORE: " + gp.score, 520, 30);
            g2.drawString("BEST: " + gp.bestScore, 520, 60);
            
            g2.drawString("HP: ", 20, 65);
            
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(60, 45, 150, 20); 
            
            double oneScale = 150.0 / gp.player.maxHp; 
            double hpBarWidth = oneScale * gp.player.hp;
            
            g2.setColor(Color.RED);
            if (hpBarWidth > 0) {
                g2.fillRect(60, 45, (int)hpBarWidth, 20); 
            }

            g2.setColor(Color.WHITE);
            g2.drawRect(60, 45, 150, 20);

            g2.setColor(Color.WHITE);
            g2.drawString("SKILL (SPACE): ", 20, 100);
            
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(220, 85, 100, 15); 
            
            if (gp.player.skillCooldown == 0) {
                g2.setColor(Color.CYAN);
                g2.fillRect(220, 85, 100, 15);
                g2.drawString("READY!", 330, 100);
            } else {
                g2.setColor(Color.ORANGE);
                double cdScale = 100.0 / gp.player.skillMaxCooldown;
                double currentCd = 100 - (cdScale * gp.player.skillCooldown);
                g2.fillRect(220, 85, (int)currentCd, 15);
            }

            // Boss HP bar appears only when the boss exists.
            for (int i = 0; i < gp.monsterList.size(); i++) {
                Monster m = gp.monsterList.get(i);
                
                if (m.type == 3) {
                    
                    g2.setFont(new Font("Arial", Font.BOLD, 24));
                    g2.setColor(Color.WHITE);
                    String bossName = "Dungeon Overlord";
                    int textLen = (int) g2.getFontMetrics().getStringBounds(bossName, g2).getWidth();
                    int textX = gp.screenWidth / 2 - textLen / 2;
                    int textY = gp.screenHeight - 75;
                    g2.drawString(bossName, textX, textY);

                    int barWidth = 400;
                    int barHeight = 20;
                    int barX = gp.screenWidth / 2 - barWidth / 2;
                    int barY = gp.screenHeight - 60;

                    g2.setColor(new Color(50, 50, 50));
                    g2.fillRect(barX, barY, barWidth, barHeight);

                    g2.setColor(Color.RED);
                    double scale = (double) barWidth / m.maxHp;
                    double hpBarValue = scale * m.hp;
                    if (hpBarValue > 0) {
                        g2.fillRect(barX, barY, (int) hpBarValue, barHeight);
                    }

                    g2.setColor(Color.WHITE);
                    g2.drawRect(barX, barY, barWidth, barHeight);
                    
                    break;
                }
            }

            // Short fade message after a room is cleared.
            if (gp.monsterList.isEmpty()) {
                levelClearCounter++;
                
                if (levelClearCounter < 180) { 
                    g2.setFont(new Font("Arial", Font.ITALIC, 28));
                    String text = "Level Clear";
                    int textLen = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
                    int textX = gp.screenWidth / 2 - textLen / 2;
                    int textY = gp.screenHeight / 2 - 50; 
                    
                    int alpha = 255;
                    if (levelClearCounter > 120) {
                        alpha = (int) (255 * (1.0f - (levelClearCounter - 120) / 60.0f));
                        if (alpha < 0) alpha = 0;
                    }
                    
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.drawString(text, textX, textY);
                }
            } else {
                levelClearCounter = 0; 
            }

        }

        // Menus are drawn last.
        if (gp.gameState == gp.titleState) {
            drawTitleScreen(g2);
        }
        else if (gp.gameState == gp.characterState) {
            drawCharacterScreen(g2);
        }
        else if (gp.gameState == gp.optionsState) {
            drawOptionsScreen(g2);
        }
        else if (gp.gameState == gp.upgradeState) {
            drawUpgradeScreen(g2);
        }
    }
    

    public void drawGameOverScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150)); 
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        g2.setFont(new Font("Arial", Font.BOLD, 60));
        g2.setColor(Color.RED);
        String text = "GAME OVER";
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        int x = gp.screenWidth / 2 - length / 2;
        int y = gp.screenHeight / 2;
        
        g2.setColor(Color.BLACK);
        g2.drawString(text, x + 3, y + 3);
        
        g2.setColor(Color.RED);
        g2.drawString(text, x, y);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.setColor(Color.WHITE);
        text = "PRESS R TO RESTART";
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 100;
        
        g2.drawString(text, x, y);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(Color.YELLOW);
        text = "SCORE: " + gp.score + "   |   BEST: " + gp.bestScore;
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 150;
        g2.drawString(text, x, y);

        drawRunStats(g2, y + 35);
    }
    public void drawGameWinScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150)); 
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 45F));
        String text = "VICTORY!";
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        int x = gp.screenWidth / 2 - length / 2;
        int y = gp.screenHeight / 2 - 50;
        
        g2.setColor(Color.BLACK);
        g2.drawString(text, x + 3, y + 3);
        
        g2.setColor(Color.YELLOW); 
        g2.drawString(text, x, y);

        g2.setFont(arial_30);
        g2.setColor(Color.WHITE);
        text = "PRESS ENTER FOR MAIN MENU";
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 50;
        
        g2.drawString(text, x, y);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(Color.YELLOW);
        text = "FINAL SCORE: " + gp.score + "   |   BEST: " + gp.bestScore;
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 100;
        g2.drawString(text, x, y);

        drawRunStats(g2, y + 35);
    }

    private void drawRunStats(Graphics2D g2, int startY) {
        StatsTracker stats = gp.statsTracker;
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.setColor(Color.WHITE);

        int leftX = 95;
        int rightX = 425;
        int rowGap = 24;

        g2.drawString("Enemies Defeated: " + stats.getEnemiesKilled(), leftX, startY);
        g2.drawString("Items Collected: " + stats.getItemsCollected(), rightX, startY);
        g2.drawString("Damage Taken: " + stats.getDamageTaken(), leftX, startY + rowGap);
        g2.drawString("Upgrades Chosen: " + stats.getUpgradesChosen(), rightX, startY + rowGap);
        g2.drawString("Level Reached: " + stats.getLevelReached(), leftX, startY + rowGap * 2);
        g2.drawString("Survival Time: " + stats.getSurvivalTimeText(), rightX, startY + rowGap * 2);
        g2.drawString("Difficulty: " + gp.difficulty.getDisplayName(), leftX, startY + rowGap * 3);
    }

    public void drawUpgradeScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.setColor(Color.YELLOW);
        String title = "CHOOSE AN UPGRADE (1, 2, 3)";
        int titleX = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(title, g2).getWidth()/2;
        g2.drawString(title, titleX, 100);

        g2.setFont(new Font("Arial", Font.BOLD, 22));
        
        for (int i = 0; i < 3; i++) {
            int boxWidth = 500;
            int boxHeight = 80;
            int boxX = gp.screenWidth/2 - boxWidth/2;
            int boxY = 180 + (i * 120);
            
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRect(boxX, boxY, boxWidth, boxHeight);
            
            g2.setColor(Color.WHITE);
            g2.drawRect(boxX, boxY, boxWidth, boxHeight);
            
            Upgrade upgrade = gp.upgradeManager.getChoice(i);
            if (upgrade == null) continue;

            String text = "KEY " + (i+1) + ": " + upgrade.getName() + " (" + upgrade.getDescription() + ")";
            g2.setColor(upgrade.getColor());
            
            int textX = boxX + boxWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
            g2.drawString(text, textX, boxY + 48);
        }
    }

    public void drawTitleScreen(Graphics2D g2) {
        if (menuBackground != null) {
            g2.drawImage(menuBackground, 0, 0, gp.screenWidth, gp.screenHeight, null);
        } else {
            // Fallback if the menu image is missing.
            g2.setColor(Color.BLACK); 
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
        g2.setFont(new Font("Arial", Font.BOLD, 60)); g2.setColor(Color.WHITE);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        String text = "START GAME";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 350);
        if (commandNum == 0) g2.drawString(">", x - 30, 350);

        text = "DIFFICULTY: " + gp.difficulty.getDisplayName();
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 420);
        if (commandNum == 1) g2.drawString(">", x - 30, 420);

        text = "OPTIONS";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 490);
        if (commandNum == 2) g2.drawString(">", x - 30, 490);

        text = "QUIT GAME";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 560);
        if (commandNum == 3) g2.drawString(">", x - 30, 560);
    }

    public void drawCharacterScreen(Graphics2D g2) {
        g2.setColor(Color.BLACK); g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setFont(new Font("Arial", Font.BOLD, 50)); g2.setColor(Color.YELLOW);
        String text = "CHOOSE CLASS";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 150);

        g2.setFont(new Font("Arial", Font.BOLD, 30)); g2.setColor(Color.WHITE);
        text = "1. RANGER (Ranged attacks)";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 300);

        text = "2. SWORDSMAN (Melee cone attacks)";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 400);

        g2.setFont(new Font("Arial", Font.ITALIC, 20)); g2.setColor(Color.GRAY);
        text = "(Press 1 or 2 to choose, press ESC to go back)";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 550);
    }

    public void drawOptionsScreen(Graphics2D g2) {
        g2.setColor(Color.BLACK); g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setFont(new Font("Arial", Font.BOLD, 50)); g2.setColor(Color.WHITE);
        String text = "AUDIO OPTIONS";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 150);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        text = "Music: "; x = 300;
        g2.drawString(text, x, 300);
        if (commandNum == 0) g2.drawString(">", x - 30, 300);
        g2.drawRect(x + 200, 275, 150, 30);
        int volumeWidth = 30 * gp.musicVolume; 
        g2.fillRect(x + 200, 275, volumeWidth, 30);

        text = "Effects (SFX): "; 
        g2.drawString(text, x, 400);
        if (commandNum == 1) g2.drawString(">", x - 30, 400);
        g2.drawRect(x + 230, 375, 150, 30);
        volumeWidth = 30 * gp.seVolume; 
        g2.fillRect(x + 230, 375, volumeWidth, 30);

        text = "BACK"; 
        g2.drawString(text, x, 500);
        if (commandNum == 2) g2.drawString(">", x - 30, 500);
        
        g2.setFont(new Font("Arial", Font.ITALIC, 20)); g2.setColor(Color.GRAY);
        g2.drawString("(Use Left/Right arrows to adjust)", x, 580);
    }

    public void drawPauseScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200)); g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        
        g2.setFont(new Font("Arial", Font.BOLD, 50)); g2.setColor(Color.WHITE);
        String text = "PAUSED";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 170);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        
        text = "Resume";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 300);
        if (commandNum == 0) g2.drawString(">", x - 30, 300);

        text = "Restart";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 360);
        if (commandNum == 1) g2.drawString(">", x - 30, 360);

        text = "Options";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 420);
        if (commandNum == 2) g2.drawString(">", x - 30, 420);

        text = "Main Menu";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 480);
        if (commandNum == 3) g2.drawString(">", x - 30, 480);

        text = "Quit";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 540);
        if (commandNum == 4) g2.drawString(">", x - 30, 540);
    }
}
