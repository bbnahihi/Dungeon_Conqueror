package game.ui;

import game.core.GamePanel;
import game.entity.Monster;
import game.system.StatsTracker;
import game.system.Upgrade;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class UI {
    
    GamePanel gp;
    
    Font arial_30;
    Font arial_60B;
    Font titleFont;
    Font headingFont;
    Font buttonFont;
    Font smallFont;
    Font hudFont;

    private final Color panelColor = new Color(10, 12, 18, 215);
    private final Color panelBorderColor = new Color(230, 190, 90, 170);
    private final Color buttonColor = new Color(18, 22, 30, 190);
    private final Color buttonHoverColor = new Color(70, 55, 35, 220);
    private final Color buttonSelectedColor = new Color(105, 76, 34, 225);
    private final Color textColor = new Color(245, 240, 220);
    private final Color goldColor = new Color(238, 190, 78);
    
    public BufferedImage titleBg;
    public int titleBlinkCounter = 0;
    public int commandNum = 0;
    public BufferedImage menuBackground;
    public int levelClearCounter = 0;
    public UI(GamePanel gp) {
        this.gp = gp;
        
        arial_30 = new Font("Arial", Font.BOLD, 30);
        arial_60B = new Font("Arial", Font.BOLD, 60);
        titleFont = new Font("Arial", Font.BOLD, 52);
        headingFont = new Font("Arial", Font.BOLD, 36);
        buttonFont = new Font("Arial", Font.BOLD, 24);
        smallFont = new Font("Arial", Font.BOLD, 16);
        hudFont = new Font("Arial", Font.BOLD, 18);

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

    private void applyRendering(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawMenuBackground(Graphics2D g2, int overlayAlpha) {
        if (menuBackground != null) {
            g2.drawImage(menuBackground, 0, 0, gp.screenWidth, gp.screenHeight, null);
        } else {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }

        if (overlayAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
    }

    private void drawPanel(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(panelColor);
        g2.fillRoundRect(x, y, width, height, 18, 18);
        g2.setColor(panelBorderColor);
        g2.drawRoundRect(x, y, width, height, 18, 18);
    }

    private void drawButton(Graphics2D g2, Rectangle bounds, String text, boolean selected) {
        boolean hover = isMouseOver(bounds);
        Color fill = selected ? buttonSelectedColor : (hover ? buttonHoverColor : buttonColor);

        g2.setColor(fill);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);

        g2.setColor(selected || hover ? goldColor : new Color(210, 210, 210, 190));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 12, 12);

        g2.setFont(buttonFont);
        int textY = bounds.y + (bounds.height - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
        drawCenteredText(g2, text, bounds.x + bounds.width / 2, textY, buttonFont, selected || hover ? Color.WHITE : textColor);
    }

    private void drawCenteredText(Graphics2D g2, String text, int centerX, int baselineY, Font font, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        int textWidth = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        g2.drawString(text, centerX - textWidth / 2, baselineY);
    }

    private void drawUpgradeCard(Graphics2D g2, int index, Upgrade upgrade) {
        Rectangle bounds = getUpgradeChoiceBounds(index);
        boolean hover = isMouseOver(bounds);

        g2.setColor(hover ? buttonHoverColor : buttonColor);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);
        g2.setColor(hover ? goldColor : new Color(220, 220, 220, 170));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

        g2.setColor(upgrade.getColor());
        g2.fillRoundRect(bounds.x + 10, bounds.y + 10, 58, bounds.height - 20, 10, 10);

        drawCenteredText(g2, "KEY " + (index + 1), bounds.x + 39, bounds.y + 47, smallFont, Color.BLACK);

        g2.setFont(buttonFont);
        g2.setColor(upgrade.getColor());
        g2.drawString(upgrade.getName(), bounds.x + 88, bounds.y + 32);

        g2.setFont(smallFont);
        g2.setColor(textColor);
        g2.drawString(upgrade.getDescription(), bounds.x + 88, bounds.y + 58);
    }

    private boolean isMouseOver(Rectangle bounds) {
        return gp.mouseH != null && bounds.contains(gp.mouseH.mouseX, gp.mouseH.mouseY);
    }

    private Rectangle centeredRowBounds(int baselineY, int width, int height) {
        return new Rectangle(gp.screenWidth / 2 - width / 2, baselineY - height + 10, width, height);
    }

    public Rectangle getTitleMenuBounds(int index) {
        int[] yPositions = {316, 374, 432, 490};
        if (index < 0 || index >= yPositions.length) return new Rectangle();
        return centeredRowBounds(yPositions[index], 404, 50);
    }

    public Rectangle getCharacterChoiceBounds(int index) {
        int[] yPositions = {300, 400};
        if (index < 0 || index >= yPositions.length) return new Rectangle();
        return centeredRowBounds(yPositions[index], 620, 60);
    }

    public Rectangle getOptionsRowBounds(int index) {
        int panelW = 540;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int padding = 40;
        int rowX = panelX + padding;
        int rowW = panelW - padding * 2;

        if (index == 0) return new Rectangle(rowX, 230, rowW, 64);
        if (index == 1) return new Rectangle(rowX, 320, rowW, 64);
        if (index == 2) return new Rectangle(gp.screenWidth / 2 - 105, 420, 210, 50);
        return new Rectangle();
    }

    public Rectangle getMusicVolumeBounds() {
        Rectangle row = getOptionsRowBounds(0);
        return new Rectangle(row.x + row.width - 180, row.y + 17, 150, 30);
    }

    public Rectangle getSfxVolumeBounds() {
        Rectangle row = getOptionsRowBounds(1);
        return new Rectangle(row.x + row.width - 180, row.y + 17, 150, 30);
    }

    public Rectangle getPauseMenuBounds(int index) {
        int[] yPositions = {244, 302, 360, 418, 476};
        if (index < 0 || index >= yPositions.length) return new Rectangle();
        return centeredRowBounds(yPositions[index], 400, 50);
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
        return centeredRowBounds(288, 410, 52);
    }

    public Rectangle getGameWinMainMenuBounds() {
        return centeredRowBounds(288, 410, 52);
    }
    
    public void draw(Graphics2D g2) {
        applyRendering(g2);

        if (gp.gameState == gp.playState || gp.gameState == gp.pauseState) {
            drawHud(g2);
        }

        if (gp.gameState == gp.titleState) {
            drawTitleScreen(g2);
        }
        else if (gp.gameState == gp.characterState) {
            drawCharacterScreen(g2);
        }
        else if (gp.gameState == gp.optionsState) {
            drawOptionsScreen(g2);
        }
    }

    private void drawHud(Graphics2D g2) {
        int leftX = 16;
        int topY = 14;
        int leftW = 360;
        int leftH = 100;
        int rightX = gp.screenWidth - 240;
        int rightW = 224;
        int rightH = 82;

        drawSmallPanel(g2, leftX, topY, leftW, leftH);
        drawSmallPanel(g2, rightX, topY, rightW, rightH);

        g2.setFont(hudFont);
        g2.setColor(textColor);
        g2.drawString("LEVEL " + gp.currentLevel, leftX + 16, topY + 26);
        g2.drawString("DIFFICULTY: " + gp.difficulty.getDisplayName(), leftX + 132, topY + 26);

        g2.drawString("HP", leftX + 16, topY + 58);
        double hpScale = 170.0 / gp.player.maxHp;
        drawBar(g2, leftX + 58, topY + 43, 170, 18, (int)(hpScale * gp.player.hp), Color.RED);

        g2.drawString("SKILL", leftX + 16, topY + 86);
        int skillWidth = 120;
        int currentSkillWidth = skillWidth;
        Color skillColor = Color.CYAN;
        if (gp.player.skillCooldown > 0) {
            double cdScale = skillWidth / (double) gp.player.skillMaxCooldown;
            currentSkillWidth = skillWidth - (int)(cdScale * gp.player.skillCooldown);
            skillColor = Color.ORANGE;
        }
        drawBar(g2, leftX + 88, topY + 72, skillWidth, 16, currentSkillWidth, skillColor);
        if (gp.player.skillCooldown == 0) {
            g2.setColor(Color.CYAN);
            g2.drawString("READY", leftX + 220, topY + 86);
        }

        g2.setColor(textColor);
        g2.drawString("SCORE", rightX + 16, topY + 30);
        g2.setColor(goldColor);
        g2.drawString(String.valueOf(gp.score), rightX + 104, topY + 30);

        g2.setColor(textColor);
        g2.drawString("BEST", rightX + 16, topY + 60);
        g2.setColor(goldColor);
        g2.drawString(String.valueOf(gp.bestScore), rightX + 104, topY + 60);

        drawBossHp(g2);
        drawLevelClearText(g2);
    }

    private void drawSmallPanel(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(new Color(0, 0, 0, 145));
        g2.fillRoundRect(x, y, width, height, 12, 12);
        g2.setColor(new Color(255, 255, 255, 70));
        g2.drawRoundRect(x, y, width, height, 12, 12);
    }

    private void drawBar(Graphics2D g2, int x, int y, int width, int height, int valueWidth, Color fillColor) {
        int clampedWidth = Math.max(0, Math.min(width, valueWidth));
        g2.setColor(new Color(35, 35, 35, 220));
        g2.fillRoundRect(x, y, width, height, 8, 8);
        if (clampedWidth > 0) {
            g2.setColor(fillColor);
            g2.fillRoundRect(x, y, clampedWidth, height, 8, 8);
        }
        g2.setColor(new Color(255, 255, 255, 150));
        g2.drawRoundRect(x, y, width, height, 8, 8);
    }

    private void drawBossHp(Graphics2D g2) {
        for (int i = 0; i < gp.monsterList.size(); i++) {
            Monster m = gp.monsterList.get(i);
            if (m.type == 3) {
                String bossName = "Dungeon Overlord";
                int barWidth = 420;
                int barHeight = 20;
                int barX = gp.screenWidth / 2 - barWidth / 2;
                int barY = gp.screenHeight - 58;

                drawCenteredText(g2, bossName, gp.screenWidth / 2, barY - 10, smallFont, textColor);
                int hpWidth = (int)((double) barWidth / m.maxHp * m.hp);
                drawBar(g2, barX, barY, barWidth, barHeight, hpWidth, Color.RED);
                break;
            }
        }
    }

    private void drawLevelClearText(Graphics2D g2) {
        if (gp.monsterList.isEmpty()) {
            levelClearCounter++;

            if (levelClearCounter < 180) {
                int alpha = 255;
                if (levelClearCounter > 120) {
                    alpha = (int) (255 * (1.0f - (levelClearCounter - 120) / 60.0f));
                    if (alpha < 0) alpha = 0;
                }

                drawCenteredText(g2, "Level Clear", gp.screenWidth / 2, gp.screenHeight / 2 - 50,
                        new Font("Arial", Font.ITALIC, 28), new Color(255, 255, 255, alpha));
            }
        } else {
            levelClearCounter = 0;
        }
    }
    

    public void drawGameOverScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 155);

        int panelW = 540;
        int panelH = 420;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int panelY = 76;
        drawPanel(g2, panelX, panelY, panelW, panelH);

        drawCenteredText(g2, "GAME OVER", gp.screenWidth / 2, panelY + 74, arial_60B, Color.RED);
        drawCenteredText(g2, "SCORE: " + gp.score + "   |   BEST: " + gp.bestScore,
                gp.screenWidth / 2, panelY + 120, buttonFont, goldColor);

        drawButton(g2, getGameOverRestartBounds(), "PRESS R TO RESTART", isMouseOver(getGameOverRestartBounds()));
        drawRunStats(g2, panelY + 296);
    }

    public void drawGameWinScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 145);

        int panelW = 540;
        int panelH = 400;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int panelY = 82;
        drawPanel(g2, panelX, panelY, panelW, panelH);

        drawCenteredText(g2, "VICTORY!", gp.screenWidth / 2, panelY + 74, titleFont, goldColor);
        drawCenteredText(g2, "FINAL SCORE: " + gp.score + "   |   BEST: " + gp.bestScore,
                gp.screenWidth / 2, panelY + 120, buttonFont, goldColor);

        drawButton(g2, getGameWinMainMenuBounds(), "PRESS ENTER FOR MAIN MENU", isMouseOver(getGameWinMainMenuBounds()));
        drawRunStats(g2, panelY + 286);
    }

    private void drawRunStats(Graphics2D g2, int startY) {
        StatsTracker stats = gp.statsTracker;
        g2.setFont(smallFont);
        g2.setColor(textColor);

        int leftX = gp.screenWidth / 2 - 260;
        int rightX = gp.screenWidth / 2 + 20;
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
        applyRendering(g2);
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        
        int panelW = 560;
        int panelH = 450;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int panelY = 66;
        drawPanel(g2, panelX, panelY, panelW, panelH);
        
        drawCenteredText(g2, "CHOOSE AN UPGRADE", gp.screenWidth / 2, 122, headingFont, goldColor);
        drawCenteredText(g2, "Press 1, 2, or 3", gp.screenWidth / 2, 150, smallFont, textColor);
        
        for (int i = 0; i < 3; i++) {
            Upgrade upgrade = gp.upgradeManager.getChoice(i);
            if (upgrade == null) continue;
            drawUpgradeCard(g2, i, upgrade);
        }
    }

    public void drawTitleScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 55);

        drawPanel(g2, 164, 252, 440, 272);

        String[] labels = {
                "START GAME",
                "DIFFICULTY: " + gp.difficulty.getDisplayName(),
                "OPTIONS",
                "QUIT GAME"
        };

        for (int i = 0; i < labels.length; i++) {
            drawButton(g2, getTitleMenuBounds(i), labels[i], commandNum == i);
        }
    }

    public void drawCharacterScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 135);

        drawPanel(g2, 64, 178, 640, 380);
        drawCenteredText(g2, "CHOOSE CLASS", gp.screenWidth / 2, 245, headingFont, goldColor);

        drawButton(g2, getCharacterChoiceBounds(0), "1. RANGER   -   Ranged attacks", isMouseOver(getCharacterChoiceBounds(0)));
        drawButton(g2, getCharacterChoiceBounds(1), "2. SWORDSMAN   -   Melee cone attacks", isMouseOver(getCharacterChoiceBounds(1)));

        drawCenteredText(g2, "Press 1 or 2 to choose, ESC to go back", gp.screenWidth / 2, 525, smallFont, textColor);
    }

    public void drawOptionsScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 145);

        int panelW = 540;
        int panelH = 410;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int panelY = 92;
        drawPanel(g2, panelX, panelY, panelW, panelH);
        drawCenteredText(g2, "AUDIO OPTIONS", gp.screenWidth / 2, 155, headingFont, textColor);

        drawVolumeOption(g2, getOptionsRowBounds(0), getMusicVolumeBounds(), "Music", gp.musicVolume, commandNum == 0);
        drawVolumeOption(g2, getOptionsRowBounds(1), getSfxVolumeBounds(), "Effects", gp.seVolume, commandNum == 1);
        drawButton(g2, getOptionsRowBounds(2), "BACK", commandNum == 2);

        drawCenteredText(g2, "Use Left/Right arrows or click the bar", gp.screenWidth / 2, panelY + panelH - 24, smallFont, textColor);
    }

    public void drawPauseScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 145);

        drawPanel(g2, 154, 132, 460, 392);
        drawCenteredText(g2, "PAUSED", gp.screenWidth / 2, 190, headingFont, textColor);

        String[] labels = {"Resume", "Restart", "Options", "Main Menu", "Quit"};
        for (int i = 0; i < labels.length; i++) {
            drawButton(g2, getPauseMenuBounds(i), labels[i], commandNum == i);
        }
    }

    private void drawVolumeOption(Graphics2D g2, Rectangle rowBounds, Rectangle barBounds, String label, int volume, boolean selected) {
        boolean hover = isMouseOver(rowBounds) || isMouseOver(barBounds);
        g2.setColor(selected ? buttonSelectedColor : (hover ? buttonHoverColor : buttonColor));
        g2.fillRoundRect(rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height, 12, 12);
        g2.setColor(selected || hover ? goldColor : new Color(210, 210, 210, 170));
        g2.drawRoundRect(rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height, 12, 12);

        g2.setFont(buttonFont);
        g2.setColor(textColor);
        g2.drawString(label, rowBounds.x + 22, rowBounds.y + 43);

        drawVolumeBar(g2, barBounds, volume);
    }

    private void drawVolumeBar(Graphics2D g2, Rectangle bounds, int volume) {
        g2.setColor(new Color(0, 0, 0, 155));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

        int volumeWidth = 30 * volume;
        if (volumeWidth > 0) {
            g2.setColor(goldColor);
            g2.fillRoundRect(bounds.x, bounds.y, volumeWidth, bounds.height, 10, 10);
        }

        g2.setColor(new Color(255, 255, 255, 165));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

        for (int i = 1; i < 5; i++) {
            int tickX = bounds.x + i * 30;
            g2.drawLine(tickX, bounds.y + 4, tickX, bounds.y + bounds.height - 4);
        }
    }
}
