package game.input;

import game.core.GamePanel;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {
    
    public boolean upPressed, downPressed, leftPressed, rightPressed, spacePressed;
    GamePanel gp; 

    public KeyHandler(GamePanel gp) {
        this.gp = gp;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        
        int code = e.getKeyCode();

        if (gp.gameState == gp.storyState) {
            if (code == KeyEvent.VK_ENTER) {
                gp.advanceStory();
            }
            return;
        }

        // Title menu.
        if (gp.gameState == gp.titleState) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                gp.ui.commandNum--;
                if (gp.ui.commandNum < 0) gp.ui.commandNum = 3;
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                gp.ui.commandNum++;
                if (gp.ui.commandNum > 3) gp.ui.commandNum = 0;
            }
            if (gp.ui.commandNum == 1 && (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT)) {
                gp.cycleDifficultyBack();
            }
            if (gp.ui.commandNum == 1 && (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT)) {
                gp.cycleDifficulty();
            }
            if (code == KeyEvent.VK_ENTER) {
                if (gp.ui.commandNum == 0) {
                    gp.gameState = gp.characterState;
                }
                if (gp.ui.commandNum == 1) {
                    gp.cycleDifficulty();
                }
                if (gp.ui.commandNum == 2) {
                    gp.previousState = gp.titleState;
                    gp.gameState = gp.optionsState;
                    gp.ui.commandNum = 0;
                }
                if (gp.ui.commandNum == 3) {
                    System.exit(0);
                }
            }
        }
        else if (gp.gameState == gp.characterState) {
            if (code == KeyEvent.VK_ESCAPE) {
                gp.gameState = gp.titleState;
                gp.ui.commandNum = 0;
            }
            if (code == KeyEvent.VK_1 || code == KeyEvent.VK_2) {
                gp.startRunWithClass(code == KeyEvent.VK_1 ? 0 : 1);
            }
        }
        else if (gp.gameState == gp.optionsState) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                gp.ui.commandNum--;
                if (gp.ui.commandNum < 0) gp.ui.commandNum = 2;
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                gp.ui.commandNum++;
                if (gp.ui.commandNum > 2) gp.ui.commandNum = 0;
            }
            
            // Left/right adjust the selected volume.
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                if (gp.ui.commandNum == 0 && gp.musicVolume > 0) {
                    gp.musicVolume--;
                    gp.music.setVolume(gp.getVolumeDecibels(gp.musicVolume)); 
                }
                if (gp.ui.commandNum == 1 && gp.seVolume > 0) {
                    gp.seVolume--;
                }
            }
            if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
                if (gp.ui.commandNum == 0 && gp.musicVolume < 5) {
                    gp.musicVolume++;
                    gp.music.setVolume(gp.getVolumeDecibels(gp.musicVolume)); 
                }
                if (gp.ui.commandNum == 1 && gp.seVolume < 5) {
                    gp.seVolume++;
                }
            }
            
            if (code == KeyEvent.VK_ENTER && gp.ui.commandNum == 2) {
                gp.gameState = gp.previousState;
                gp.ui.commandNum = (gp.previousState == gp.pauseState) ? 2 : 0; 
            }
            if (code == KeyEvent.VK_ESCAPE) {
                gp.gameState = gp.previousState;
                gp.ui.commandNum = (gp.previousState == gp.pauseState) ? 2 : 0;
            }
        }
        
        // Gameplay controls.
        else if (gp.gameState == gp.playState) {
            if (code == KeyEvent.VK_W) upPressed = true;
            if (code == KeyEvent.VK_A) leftPressed = true;
            if (code == KeyEvent.VK_S) downPressed = true;
            if (code == KeyEvent.VK_D) rightPressed = true;
            
            if (code == KeyEvent.VK_SPACE) spacePressed = true;

            if (code == KeyEvent.VK_F8) {
                gp.reloadCurrentNormalMapProps();
            }
            
            if (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE) { 
                gp.pauseMusic();
                gp.gameState = gp.pauseState;
                gp.ui.commandNum = 0;
            }
        }
        
        // Pause menu.
        else if (gp.gameState == gp.pauseState) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                gp.ui.commandNum--;
                if (gp.ui.commandNum < 0) gp.ui.commandNum = 4;
            }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                gp.ui.commandNum++;
                if (gp.ui.commandNum > 4) gp.ui.commandNum = 0;
            }
            if (code == KeyEvent.VK_ENTER) {
                if (gp.ui.commandNum == 0) {
                    gp.gameState = gp.playState;
                    gp.resumeMusic();
                }
                if (gp.ui.commandNum == 1) {
                    int selectedClass = gp.player.classType;
                    gp.resetGame();
                    gp.player.setupClass(selectedClass);
                    gp.gameState = gp.playState;
                    gp.ui.commandNum = 0;
                    gp.transitionToNewMap(gp.currentLevel);
                }
                if (gp.ui.commandNum == 2) {
                    gp.previousState = gp.pauseState;
                    gp.gameState = gp.optionsState;
                    gp.ui.commandNum = 0;
                    return;
                }
                if (gp.ui.commandNum == 3) {
                    gp.gameState = gp.titleState;
                    gp.ui.commandNum = 0;
                    gp.resetGame();
                    gp.playMusic(6);
                }
                if (gp.ui.commandNum == 4) {
                    System.exit(0);
                }
            }
            if (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE) {
                gp.gameState = gp.playState; 
                gp.resumeMusic();
            }
        }
        
        // Game over screen.
        else if (gp.gameState == gp.gameOverState) {
            if (code == KeyEvent.VK_R) { 
                gp.resetGame();
                gp.gameState = gp.titleState;
                gp.ui.commandNum = 0;
                gp.playMusic(6);
            }
        }

        // Victory screen.
        else if (gp.gameState == gp.gameWinState) {
            if (code == KeyEvent.VK_ENTER) { 
                gp.resetGame();
                gp.gameState = gp.titleState;
                gp.ui.commandNum = 0;
                gp.playMusic(6);
            }
        }
        
        // Upgrade choice screen.
        else if (gp.gameState == gp.upgradeState) {
            if (code == KeyEvent.VK_1 || code == KeyEvent.VK_2 || code == KeyEvent.VK_3) {
                int choice = 0;
                if (code == KeyEvent.VK_1) choice = 0;
                if (code == KeyEvent.VK_2) choice = 1;
                if (code == KeyEvent.VK_3) choice = 2;

                gp.selectUpgrade(choice);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) upPressed = false;
        if (code == KeyEvent.VK_A) leftPressed = false;
        if (code == KeyEvent.VK_S) downPressed = false;
        if (code == KeyEvent.VK_D) rightPressed = false;
        if (code == KeyEvent.VK_SPACE) spacePressed = false;
    }
}
