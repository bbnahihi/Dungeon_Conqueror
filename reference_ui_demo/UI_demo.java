package game.ui;

import game.core.GamePanel;
import game.entity.Monster;
import game.system.StatsTracker;
import game.system.Upgrade;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class UI {

    GamePanel gp;

    Font titleFont;
    Font headingFont;
    Font buttonFont;
    Font smallFont;
    Font hudFont;

    private final Color panelFill = new Color(12, 14, 22, 220);
    private final Color panelBorder = new Color(230, 190, 90, 200);
    private final Color buttonFill = new Color(18, 22, 32, 200);
    private final Color buttonHover = new Color(55, 48, 38, 230);
    private final Color buttonSelected = new Color(95, 72, 32, 240);
    private final Color textLight = new Color(245, 240, 228);
    private final Color gold = new Color(238, 196, 78);
    private final Color goldDim = new Color(180, 145, 55);

    public BufferedImage menuBackground;
    public int commandNum = 0;
    public int levelClearCounter = 0;

    public UI(GamePanel gp) {
        this.gp = gp;
        titleFont = new Font("SansSerif", Font.BOLD, 48);
        headingFont = new Font("SansSerif", Font.BOLD, 34);
        buttonFont = new Font("SansSerif", Font.BOLD, 22);
        smallFont = new Font("SansSerif", Font.BOLD, 15);
        hudFont = new Font("SansSerif", Font.BOLD, 17);

        try (InputStream is = getClass().getResourceAsStream("/res/menu_bg.png")) {
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
            g2.setPaint(new GradientPaint(0, 0, new Color(28, 32, 52), 0, gp.screenHeight, new Color(8, 10, 18)));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
        if (overlayAlpha > 0) {
            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
    }

    private void drawVignette(Graphics2D g2) {
        int cx = gp.screenWidth / 2;
        int cy = gp.screenHeight / 2;
        int r = Math.max(gp.screenWidth, gp.screenHeight);
        g2.setPaint(new GradientPaint(cx, cy, new Color(0, 0, 0, 0), cx, cy + r / 2, new Color(0, 0, 0, 140)));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setPaint(new GradientPaint(cx, cy - r / 3, new Color(0, 0, 0, 100), cx, cy, new Color(0, 0, 0, 0)));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight / 2);
    }

    private void drawPanel(Graphics2D g2, int x, int y, int width, int height) {
        g2.setColor(panelFill);
        g2.fillRoundRect(x, y, width, height, 20, 20);
        g2.setColor(panelBorder);
        g2.drawRoundRect(x, y, width - 1, height - 1, 20, 20);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.drawRoundRect(x + 2, y + 2, width - 5, height / 3, 16, 16);
    }

    private void drawShadowedText(Graphics2D g2, String text, int centerX, int baselineY,
                                  Font font, Color fill, Color shadow) {
        g2.setFont(font);
        int w = g2.getFontMetrics().stringWidth(text);
        int x = centerX - w / 2;
        if (shadow != null) {
            g2.setColor(shadow);
            g2.drawString(text, x + 2, baselineY + 2);
        }
        g2.setColor(fill);
        g2.drawString(text, x, baselineY);
    }

    private void drawButton(Graphics2D g2, Rectangle bounds, String text, boolean selected, Color accent) {
        boolean hover = isMouseOver(bounds);
        boolean active = selected || hover;

        if (active && accent != null) {
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 45));
            g2.fillRoundRect(bounds.x - 4, bounds.y - 4, bounds.width + 8, bounds.height + 8, 16, 16);
        }

        Color fill = selected ? buttonSelected : (hover ? buttonHover : buttonFill);
        g2.setColor(fill);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);

        Color border = active ? (accent != null ? accent : gold) : new Color(200, 200, 210, 120);
        g2.setColor(border);
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 14, 14);

        if (selected) {
            g2.setColor(gold);
            g2.drawString("▶", bounds.x + 14, bounds.y + bounds.height / 2 + 7);
        }

        int textY = bounds.y + (bounds.height + g2.getFontMetrics(buttonFont).getAscent()
                - g2.getFontMetrics(buttonFont).getDescent()) / 2 - 2;
        drawShadowedText(g2, text, bounds.x + bounds.width / 2 + (selected ? 10 : 0), textY,
                buttonFont, active ? Color.WHITE : textLight, new Color(0, 0, 0, 160));
    }

    private boolean isMouseOver(Rectangle bounds) {
        return gp.mouseH != null && bounds.contains(gp.mouseH.mouseX, gp.mouseH.mouseY);
    }

    private Rectangle centeredRowBounds(int baselineY, int width, int height) {
        return new Rectangle(gp.screenWidth / 2 - width / 2, baselineY - height + 10, width, height);
    }

    public Rectangle getTitleMenuBounds(int index) {
        int[] yPositions = {362, 418, 474, 530};
        if (index < 0 || index >= yPositions.length) return new Rectangle();
        return centeredRowBounds(yPositions[index], 480, 44);
    }

    /** Title menu: selected row = blue pill; others = plain dimmed text (reference layout). */
    private void drawTitleMenuItem(Graphics2D g2, String text, int baselineY, int index) {
        boolean selected = commandNum == index;
        boolean hover = isMouseOver(getTitleMenuBounds(index));
        int centerX = gp.screenWidth / 2;

        g2.setFont(buttonFont);
        var fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);

        if (selected) {
            int padX = 28;
            int padY = 8;
            int chevronW = 18;
            int boxW = textW + padX * 2 + chevronW;
            int boxH = fm.getHeight() + padY * 2;
            int boxX = centerX - boxW / 2;
            int boxY = baselineY - fm.getAscent() - padY;

            Color fill = new Color(52, 88, 138, 235);
            Color outer = new Color(38, 68, 108);
            Color inner = new Color(130, 185, 245);

            g2.setColor(fill);
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 14, 14);
            g2.setColor(outer);
            g2.drawRoundRect(boxX, boxY, boxW - 1, boxH - 1, 14, 14);
            g2.setColor(inner);
            g2.drawRoundRect(boxX + 2, boxY + 2, boxW - 5, boxH - 5, 12, 12);

            g2.setColor(new Color(100, 170, 255));
            g2.drawString(">", boxX + 16, baselineY);

            drawShadowedText(g2, text, centerX + 6, baselineY, buttonFont, Color.WHITE, Color.BLACK);
        } else {
            Color dim = hover ? new Color(215, 220, 230) : new Color(178, 184, 196);
            drawShadowedText(g2, text, centerX, baselineY, buttonFont, dim, Color.BLACK);
        }
    }

    public Rectangle getCharacterChoiceBounds(int index) {
        if (index == 0) return new Rectangle(98, 288, 280, 118);
        if (index == 1) return new Rectangle(390, 288, 280, 118);
        return new Rectangle();
    }

    private static final int OPTIONS_PANEL_Y = 92;
    private static final int OPTIONS_PANEL_H = 400;

    public Rectangle getOptionsRowBounds(int index) {
        int panelW = 540;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int padding = 40;
        int rowX = panelX + padding;
        int rowW = panelW - padding * 2;
        if (index == 0) return new Rectangle(rowX, OPTIONS_PANEL_Y + 138, rowW, 64);
        if (index == 1) return new Rectangle(rowX, OPTIONS_PANEL_Y + 228, rowW, 64);
        if (index == 2) return centeredRowBounds(OPTIONS_PANEL_Y + 332, 210, 46);
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
        int boxWidth = 520;
        int boxHeight = 84;
        int boxX = gp.screenWidth / 2 - boxWidth / 2;
        int boxY = 178 + (index * 118);
        return new Rectangle(boxX, boxY, boxWidth, boxHeight);
    }

    public Rectangle getGameOverRestartBounds() {
        return centeredRowBounds(gp.screenHeight - 72, 480, 48);
    }

    public Rectangle getGameWinMainMenuBounds() {
        return centeredRowBounds(288, 410, 52);
    }

    public void draw(Graphics2D g2) {
        applyRendering(g2);
        if (gp.gameState == gp.playState || gp.gameState == gp.pauseState) {
            drawHud(g2);
        }
        // Full-screen menus are drawn from GamePanel.paintComponent.
    }

    private void drawHud(Graphics2D g2) {
        int x = 12;
        int y = 12;
        int w = 276;
        int h = 102;
        int inset = 14;

        drawHudBlock(g2, x, y, w, h);

        g2.setFont(hudFont);
        g2.setColor(new Color(200, 220, 255));
        g2.drawString("LEVEL " + gp.currentLevel, x + inset, y + 26);

        String difficulty = gp.difficulty.getDisplayName();
        g2.setFont(smallFont);
        g2.setColor(goldDim);
        int diffW = g2.getFontMetrics().stringWidth(difficulty);
        g2.drawString(difficulty, x + w - inset - diffW, y + 26);

        int barX = x + inset;
        int barW = w - inset * 2;
        int hpBarY = y + 36;
        float hpRatio = gp.player.maxHp <= 0 ? 0 : (float) gp.player.hp / gp.player.maxHp;
        Color hpEnd = hpRatio > 0.35f ? new Color(220, 55, 65) : new Color(255, 140, 45);
        drawGradientBar(g2, barX, hpBarY, barW, 18, hpRatio, new Color(30, 32, 42), hpEnd, "", 0, 0);

        g2.setFont(smallFont);
        g2.setColor(textLight);
        g2.drawString("HP " + gp.player.hp + "/" + gp.player.maxHp, barX, hpBarY + 34);

        int skillBarY = y + h - 24;
        float skillRatio = gp.player.skillCooldown == 0 ? 1f
                : 1f - (float) gp.player.skillCooldown / gp.player.skillMaxCooldown;
        Color skillEnd = gp.player.skillCooldown == 0 ? new Color(70, 200, 255) : new Color(255, 165, 50);
        int skillBarW = 128;
        drawGradientBar(g2, barX, skillBarY, skillBarW, 12, skillRatio,
                new Color(30, 32, 42), skillEnd, "", 0, 0);

        String skillLabel = gp.player.skillCooldown == 0 ? "SKILL READY" : "SKILL";
        g2.setFont(smallFont);
        g2.setColor(textLight);
        int skillTextW = g2.getFontMetrics().stringWidth(skillLabel);
        g2.drawString(skillLabel, x + w - inset - skillTextW, skillBarY + 11);

        int scoreX = gp.screenWidth - 220;
        int scoreY = 12;
        int scoreW = 208;
        int scoreH = 78;
        drawHudBlock(g2, scoreX, scoreY, scoreW, scoreH);

        g2.setFont(smallFont);
        g2.setColor(textLight);
        g2.drawString("SCORE", scoreX + inset, scoreY + 28);
        g2.setFont(hudFont);
        g2.setColor(gold);
        g2.drawString(String.valueOf(gp.score), scoreX + inset + 72, scoreY + 28);

        g2.setFont(smallFont);
        g2.setColor(textLight);
        g2.drawString("BEST", scoreX + inset, scoreY + 56);
        g2.setFont(hudFont);
        g2.setColor(new Color(180, 200, 255));
        g2.drawString(String.valueOf(gp.bestScore), scoreX + inset + 72, scoreY + 56);

        drawBossHp(g2);
        drawLevelClearText(g2);
    }

    private void drawHudBlock(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(12, 16, 24, 115));
        g2.fillRoundRect(x, y, w, h, 14, 14);
        g2.setColor(new Color(90, 110, 150, 90));
        g2.drawRoundRect(x, y, w - 1, h - 1, 14, 14);
    }

    private void drawGradientBar(Graphics2D g2, int x, int y, int w, int h, float ratio,
                                 Color track, Color fillEnd, String label, int labelX, int labelY) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        g2.setColor(track);
        g2.fillRoundRect(x, y, w, h, 8, 8);
        int fillW = Math.max(0, (int) (w * ratio));
        if (fillW > 0) {
            g2.setPaint(new GradientPaint(x, y, fillEnd.brighter(), x + fillW, y, fillEnd));
            g2.fillRoundRect(x, y, fillW, h, 8, 8);
        }
        g2.setColor(new Color(255, 255, 255, 90));
        g2.drawRoundRect(x, y, w - 1, h - 1, 8, 8);
        g2.setFont(smallFont);
        g2.setColor(textLight);
        g2.drawString(label, labelX, labelY);
    }

    private void drawBossHp(Graphics2D g2) {
        for (Monster m : gp.monsterList) {
            if (m.type != 3) continue;
            int barW = 440;
            int barH = 22;
            int barX = gp.screenWidth / 2 - barW / 2;
            int barY = gp.screenHeight - 62;
            drawShadowedText(g2, "DUNGEON OVERLORD", gp.screenWidth / 2, barY - 12,
                    new Font("SansSerif", Font.BOLD, 20), new Color(255, 130, 90), Color.BLACK);
            float ratio = m.maxHp <= 0 ? 0 : (float) m.hp / m.maxHp;
            drawGradientBar(g2, barX, barY, barW, barH, ratio,
                    new Color(40, 18, 22), new Color(200, 40, 50), "", 0, 0);
            g2.setColor(new Color(255, 200, 120, 80));
            g2.drawRoundRect(barX - 2, barY - 2, barW + 3, barH + 3, 12, 12);
            break;
        }
    }

    private void drawLevelClearText(Graphics2D g2) {
        if (!gp.monsterList.isEmpty()) {
            levelClearCounter = 0;
            return;
        }
        levelClearCounter++;
        if (levelClearCounter >= 180) return;

        int alpha = 255;
        if (levelClearCounter > 120) {
            alpha = (int) (255 * (1f - (levelClearCounter - 120) / 60f));
            alpha = Math.max(0, alpha);
        }
        float scale = 1f + (float) Math.sin(levelClearCounter * 0.12) * 0.04f;
        Font font = new Font("SansSerif", Font.BOLD, (int) (38 * scale));
        drawShadowedText(g2, "LEVEL CLEAR", gp.screenWidth / 2, gp.screenHeight / 2 - 48, font,
                new Color(255, 220, 120, alpha), new Color(0, 0, 0, alpha / 2));
    }

    public void drawTitleScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 0);

        String[] labels = {
                "START GAME",
                "DIFFICULTY: " + gp.difficulty.getDisplayName(),
                "OPTIONS",
                "QUIT GAME"
        };
        int[] yPositions = {362, 418, 474, 530};
        for (int i = 0; i < labels.length; i++) {
            drawTitleMenuItem(g2, labels[i], yPositions[i], i);
        }
    }

    public void drawCharacterScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 120);
        drawVignette(g2);
        drawPanel(g2, 54, 168, 660, 360);
        drawShadowedText(g2, "CHOOSE YOUR CLASS", gp.screenWidth / 2, 228, headingFont, gold, Color.BLACK);

        drawClassCard(g2, getCharacterChoiceBounds(0), "RANGER",
                "Ranged bow attacks", new Color(80, 200, 120), isMouseOver(getCharacterChoiceBounds(0)));
        drawClassCard(g2, getCharacterChoiceBounds(1), "SWORDSMAN",
                "Melee cone slashes", new Color(255, 160, 90), isMouseOver(getCharacterChoiceBounds(1)));

        drawShadowedText(g2, "Press 1 or 2  •  ESC to go back",
                gp.screenWidth / 2, 548, smallFont, textLight, null);
    }

    private void drawClassCard(Graphics2D g2, Rectangle bounds, String title, String subtitle,
                               Color accent, boolean hover) {
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hover ? 55 : 30));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 16, 16);
        g2.setColor(hover ? accent.brighter() : accent);
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 16, 16);

        g2.setFont(buttonFont);
        g2.setColor(Color.WHITE);
        g2.drawString(title, bounds.x + 20, bounds.y + 42);
        g2.setFont(smallFont);
        g2.setColor(textLight);
        g2.drawString(subtitle, bounds.x + 20, bounds.y + 68);
        g2.setColor(accent);
        g2.drawString(hover ? "CLICK TO SELECT" : "KEY " + (bounds.x < 300 ? "1" : "2"),
                bounds.x + 20, bounds.y + 98);
    }

    public void drawOptionsScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 130);
        drawVignette(g2);
        int panelW = 540;
        int panelH = OPTIONS_PANEL_H;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int panelY = OPTIONS_PANEL_Y;
        drawPanel(g2, panelX, panelY, panelW, panelH);
        drawShadowedText(g2, "AUDIO OPTIONS", gp.screenWidth / 2, panelY + 63, headingFont, gold, Color.BLACK);

        drawVolumeOption(g2, getOptionsRowBounds(0), getMusicVolumeBounds(), "Music", gp.musicVolume, commandNum == 0);
        drawVolumeOption(g2, getOptionsRowBounds(1), getSfxVolumeBounds(), "Effects", gp.seVolume, commandNum == 1);
        drawButton(g2, getOptionsRowBounds(2), "BACK", commandNum == 2, goldDim);

        drawShadowedText(g2, "← → adjust volume  •  click bars",
                gp.screenWidth / 2, panelY + panelH + 36, smallFont, new Color(160, 170, 190), null);
    }

    public void drawPauseScreen(Graphics2D g2) {
        applyRendering(g2);
        g2.setColor(new Color(0, 0, 0, 165));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        drawPanel(g2, 144, 118, 480, 408);
        drawShadowedText(g2, "PAUSED", gp.screenWidth / 2, 178, headingFont, gold, Color.BLACK);

        Color[] accents = {
                new Color(100, 220, 140), new Color(255, 200, 80),
                new Color(160, 140, 255), new Color(120, 180, 255), new Color(255, 90, 90)
        };
        String[] labels = {"Resume", "Restart", "Options", "Main Menu", "Quit"};
        for (int i = 0; i < labels.length; i++) {
            drawButton(g2, getPauseMenuBounds(i), labels[i], commandNum == i, accents[i]);
        }
    }

    public void drawUpgradeScreen(Graphics2D g2) {
        applyRendering(g2);
        g2.setColor(new Color(0, 0, 0, 195));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        drawPanel(g2, 94, 58, 580, 468);
        drawShadowedText(g2, "CHOOSE AN UPGRADE", gp.screenWidth / 2, 118, headingFont, gold, Color.BLACK);
        drawShadowedText(g2, "Press 1, 2, or 3", gp.screenWidth / 2, 148, smallFont, textLight, null);

        for (int i = 0; i < 3; i++) {
            Upgrade upgrade = gp.upgradeManager.getChoice(i);
            if (upgrade != null) {
                drawUpgradeCard(g2, i, upgrade);
            }
        }
    }

    private void drawUpgradeCard(Graphics2D g2, int index, Upgrade upgrade) {
        Rectangle bounds = getUpgradeChoiceBounds(index);
        boolean hover = isMouseOver(bounds);
        Color accent = upgrade.getColor();

        g2.setColor(hover ? buttonHover : buttonFill);
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 14, 14);
        g2.setColor(hover ? accent.brighter() : accent);
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 14, 14);

        g2.setColor(accent);
        g2.fillRoundRect(bounds.x + 10, bounds.y + 10, 56, bounds.height - 20, 10, 10);
        drawShadowedText(g2, String.valueOf(index + 1), bounds.x + 38, bounds.y + 50,
                headingFont, Color.BLACK, null);

        g2.setFont(buttonFont);
        g2.setColor(accent.brighter());
        g2.drawString(upgrade.getName(), bounds.x + 82, bounds.y + 34);
        g2.setFont(smallFont);
        g2.setColor(textLight);
        g2.drawString(upgrade.getDescription(), bounds.x + 82, bounds.y + 58);
    }

    public void drawStoryScreen(Graphics2D g2) {
        applyRendering(g2);
        drawMenuBackground(g2, 140);
        drawVignette(g2);
        int panelW = 620;
        int panelH = 452;
        int panelX = gp.screenWidth / 2 - panelW / 2;
        int panelY = 62;
        drawPanel(g2, panelX, panelY, panelW, panelH);
        drawShadowedText(g2, gp.storyManager.getTitle(), gp.screenWidth / 2, panelY + 58, headingFont, gold, Color.BLACK);

        Font storyFont = new Font("SansSerif", Font.PLAIN, 18);
        int textX = panelX + 42;
        int textY = panelY + 104;
        for (String line : gp.storyManager.getLines()) {
            textY = drawWrappedStoryLine(g2, line, textX, textY, panelW - 84, 24, storyFont);
        }
        drawShadowedText(g2, "Press ENTER or click to continue",
                gp.screenWidth / 2, panelY + panelH - 34, smallFont, textLight, null);
    }

    private int drawWrappedStoryLine(Graphics2D g2, String text, int x, int y, int maxWidth,
                                     int lineHeight, Font font) {
        if (text.isEmpty()) return y + lineHeight / 2;
        g2.setFont(font);
        g2.setColor(textLight);
        String[] words = text.split(" ");
        String line = "";
        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (g2.getFontMetrics().stringWidth(test) > maxWidth && !line.isEmpty()) {
                g2.drawString(line, x, y);
                y += lineHeight;
                line = word;
            } else {
                line = test;
            }
        }
        if (!line.isEmpty()) g2.drawString(line, x, y);
        return y + lineHeight + 4;
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new Color(4, 6, 10, 200));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
    }

    private void drawGoldScreenBorder(Graphics2D g2) {
        int margin = 18;
        int inner = margin + 5;
        Color outer = new Color(175, 140, 50);
        Color innerLine = new Color(240, 208, 128);
        g2.setColor(outer);
        g2.drawRect(margin, margin, gp.screenWidth - margin * 2 - 1, gp.screenHeight - margin * 2 - 1);
        g2.setColor(innerLine);
        g2.drawRect(inner, inner, gp.screenWidth - inner * 2 - 1, gp.screenHeight - inner * 2 - 1);
    }

    public void drawGameOverScreen(Graphics2D g2) {
        applyRendering(g2);
        drawGameOverOverlay(g2);
        drawGoldScreenBorder(g2);

        Font gameOverFont = new Font("SansSerif", Font.BOLD, 54);
        Font scoreFont = new Font("SansSerif", Font.BOLD, 24);
        Font restartFont = new Font("SansSerif", Font.BOLD, 30);
        Color scoreGold = new Color(240, 208, 128);

        int cx = gp.screenWidth / 2;
        drawShadowedText(g2, "GAME OVER", cx, 108, gameOverFont, scoreGold, Color.BLACK);

        String scoreLine = "SCORE: " + gp.score + "   |   BEST: " + gp.bestScore;
        drawShadowedText(g2, scoreLine, cx, 158, scoreFont, scoreGold, Color.BLACK);

        drawRunStats(g2, 248);

        Color restartColor = isMouseOver(getGameOverRestartBounds())
                ? Color.WHITE : new Color(210, 214, 222);
        drawShadowedText(g2, "PRESS R TO RESTART", cx, gp.screenHeight - 58, restartFont, restartColor, Color.BLACK);
    }

    public void drawGameWinScreen(Graphics2D g2) {
        applyRendering(g2);
        g2.setColor(new Color(0, 0, 0, 155));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        drawPanel(g2, 114, 82, 540, 400);
        drawShadowedText(g2, "VICTORY!", gp.screenWidth / 2, 148, titleFont, gold, Color.BLACK);
        drawShadowedText(g2, "FINAL SCORE: " + gp.score + "   |   BEST: " + gp.bestScore,
                gp.screenWidth / 2, 198, buttonFont, gold, Color.BLACK);
        drawButton(g2, getGameWinMainMenuBounds(), "PRESS ENTER FOR MAIN MENU",
                isMouseOver(getGameWinMainMenuBounds()), gold);
        drawRunStats(g2, 286);
    }

    private void drawRunStats(Graphics2D g2, int startY) {
        StatsTracker stats = gp.statsTracker;
        Font statFont = new Font("SansSerif", Font.BOLD, 18);
        g2.setFont(statFont);
        g2.setColor(Color.WHITE);
        int leftX = gp.screenWidth / 2 - 248;
        int rightX = gp.screenWidth / 2 + 32;
        int rowGap = 28;
        g2.drawString("Enemies Defeated: " + stats.getEnemiesKilled(), leftX, startY);
        g2.drawString("Items Collected: " + stats.getItemsCollected(), rightX, startY);
        g2.drawString("Damage Taken: " + stats.getDamageTaken(), leftX, startY + rowGap);
        g2.drawString("Upgrades Chosen: " + stats.getUpgradesChosen(), rightX, startY + rowGap);
        g2.drawString("Level Reached: " + stats.getLevelReached(), leftX, startY + rowGap * 2);
        g2.drawString("Survival Time: " + stats.getSurvivalTimeText(), rightX, startY + rowGap * 2);
        g2.drawString("Difficulty: " + gp.difficulty.getDisplayName(), leftX, startY + rowGap * 3);
    }

    private void drawVolumeOption(Graphics2D g2, Rectangle rowBounds, Rectangle barBounds,
                                  String label, int volume, boolean selected) {
        boolean hover = isMouseOver(rowBounds) || isMouseOver(barBounds);
        g2.setColor(selected ? buttonSelected : (hover ? buttonHover : buttonFill));
        g2.fillRoundRect(rowBounds.x, rowBounds.y, rowBounds.width, rowBounds.height, 12, 12);
        g2.setColor(selected || hover ? gold : new Color(200, 200, 210, 120));
        g2.drawRoundRect(rowBounds.x, rowBounds.y, rowBounds.width - 1, rowBounds.height - 1, 12, 12);
        g2.setFont(buttonFont);
        g2.setColor(textLight);
        g2.drawString(label, rowBounds.x + 22, rowBounds.y + 43);
        drawVolumeBar(g2, barBounds, volume);
    }

    private void drawVolumeBar(Graphics2D g2, Rectangle bounds, int volume) {
        g2.setColor(new Color(20, 22, 32, 220));
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);
        int volumeWidth = 30 * volume;
        if (volumeWidth > 0) {
            g2.setPaint(new GradientPaint(bounds.x, bounds.y, gold.brighter(), bounds.x + volumeWidth, bounds.y, gold));
            g2.fillRoundRect(bounds.x, bounds.y, volumeWidth, bounds.height, 10, 10);
        }
        g2.setColor(new Color(255, 255, 255, 140));
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 10, 10);
        for (int i = 1; i < 5; i++) {
            int tickX = bounds.x + i * 30;
            g2.drawLine(tickX, bounds.y + 4, tickX, bounds.y + bounds.height - 4);
        }
    }
}
