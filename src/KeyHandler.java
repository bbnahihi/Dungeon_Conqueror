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
        
        int code = e.getKeyCode(); // Khai báo biến code nhận tín hiệu phím

        // ==========================================
        // 1. TRẠNG THÁI MENU (CHỌN CLASS)
        // ==========================================
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
                    gp.gameState = gp.characterState; // Vào màn chọn nhân vật
                }
                if (gp.ui.commandNum == 1) {
                    gp.cycleDifficulty();
                }
                if (gp.ui.commandNum == 2) {
                    gp.previousState = gp.titleState; // Ghi nhớ Options được mở từ menu chính
                    gp.gameState = gp.optionsState; // Vào cài đặt
                    gp.ui.commandNum = 0; // Reset con trỏ cho màn cài đặt
                }
                if (gp.ui.commandNum == 3) {
                    System.exit(0); // Thoát game
                }
            }
        }
        else if (gp.gameState == gp.characterState) {
            if (code == KeyEvent.VK_ESCAPE) {
                gp.gameState = gp.titleState; // Bấm ESC để quay lại
                gp.ui.commandNum = 0;
            }
            if (code == KeyEvent.VK_1 || code == KeyEvent.VK_2) {
                gp.gameState = gp.playState;
                
                // ĐÃ FIX BUG: Bắt đầu từ Màn 1, thay vì Màn 10!
                gp.currentLevel = 1;     
                
                gp.player.setDefaultValues(); 
                gp.particleList.clear(); 
                
                if (code == KeyEvent.VK_1) gp.player.setupClass(0); 
                if (code == KeyEvent.VK_2) gp.player.setupClass(1); 
                
                // ĐÃ FIX: Chỉ gọi hàm chuyển map bằng level
                gp.transitionToNewMap(gp.currentLevel); 
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
            
            // Dùng nút Trái/Phải để chỉnh âm lượng
            // Dùng nút Trái/Phải để chỉnh âm lượng
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                if (gp.ui.commandNum == 0 && gp.musicVolume > 0) {
                    gp.musicVolume--;
                    // Cập nhật âm lượng nhạc nền đang phát ngay lập tức
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
            
            // Thoát
            // TÌM CHỖ NÀY VÀ SỬA LẠI:
            // Thoát
            if (code == KeyEvent.VK_ENTER && gp.ui.commandNum == 2) {
                gp.gameState = gp.previousState; // <--- TRẢ VỀ ĐÚNG MÀN HÌNH TRƯỚC ĐÓ
                // Chỉnh lại con trỏ để UX mượt hơn
                gp.ui.commandNum = (gp.previousState == gp.pauseState) ? 2 : 0; 
            }
            if (code == KeyEvent.VK_ESCAPE) {
                gp.gameState = gp.previousState; // <--- TRẢ VỀ ĐÚNG MÀN HÌNH TRƯỚC ĐÓ
                gp.ui.commandNum = (gp.previousState == gp.pauseState) ? 2 : 0;
            }
        }
        
        // ==========================================
        // 2. TRẠNG THÁI ĐANG CHƠI (PLAY STATE)
        // ==========================================
        else if (gp.gameState == gp.playState) {
            if (code == KeyEvent.VK_W) upPressed = true;
            if (code == KeyEvent.VK_A) leftPressed = true;
            if (code == KeyEvent.VK_S) downPressed = true;
            if (code == KeyEvent.VK_D) rightPressed = true;
            
            // Đưa phím SPACE vào đúng trạng thái đang chơi
            if (code == KeyEvent.VK_SPACE) spacePressed = true;
            
            if (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE) { 
                gp.pauseMusic();
                gp.gameState = gp.pauseState; // Nhấn P để tạm dừng
                gp.ui.commandNum = 0;
            }
        }
        
        // ==========================================
        // TRẠNG THÁI TẠM DỪNG (PAUSE STATE)
        // ==========================================
        else if (gp.gameState == gp.pauseState) {
            // Chú ý: Đổi điều kiện lên 3 vì chúng ta có 4 menu (0, 1, 2, 3)
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
                    gp.gameState = gp.playState; // Tiếp tục
                    gp.resumeMusic(); // <--- CHẠY TIẾP NHẠC
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
                    gp.gameState = gp.titleState; // Về trang chủ
                    gp.ui.commandNum = 0;
                    gp.resetGame(); // Hủy hết quái và đạn cũ
                    gp.playMusic(6); // Mở nhạc menu
                }
                if (gp.ui.commandNum == 4) {
                    System.exit(0); // Thoát luôn
                }
            }
            if (code == KeyEvent.VK_P || code == KeyEvent.VK_ESCAPE) {
                gp.gameState = gp.playState; 
                gp.resumeMusic(); // Phím tắt để tiếp tục cũng phải bật lại nhạc
            }
        }
        
        // ==========================================
        // 4. TRẠNG THÁI GAME OVER
        // ==========================================
        else if (gp.gameState == gp.gameOverState) {
            if (code == KeyEvent.VK_R) { 
                gp.resetGame();
                gp.gameState = gp.titleState;
                gp.ui.commandNum = 0;
                gp.playMusic(6); // Bật lại nhạc Menu (ID 6)
            }
        }

        // ==========================================
        // 5. TRẠNG THÁI CHIẾN THẮNG
        // ==========================================
        else if (gp.gameState == gp.gameWinState) {
            if (code == KeyEvent.VK_ENTER) { 
                gp.resetGame();
                gp.gameState = gp.titleState;
                gp.ui.commandNum = 0;
                gp.playMusic(6); // Bật lại nhạc Menu
            }
        }
        
        // ==========================================
        // 6. TRẠNG THÁI CHỌN NÂNG CẤP (UPGRADE STATE)
        // ==========================================
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
