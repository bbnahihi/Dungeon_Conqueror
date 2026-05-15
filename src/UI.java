import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;

public class UI {
    
    GamePanel gp;
    
    // Khai báo Font chữ để dùng chung
    Font arial_30;
    Font arial_60B;
    
    // Biến cho màn hình Menu
    public BufferedImage titleBg;
    public int titleBlinkCounter = 0;
    public int commandNum = 0; // Con trỏ Menu
    public BufferedImage menuBackground;
    public int levelClearCounter = 0; // Bộ đếm thời gian hiển thị chữ "Qua màn"
    public UI(GamePanel gp) {
        this.gp = gp;
        
        arial_30 = new Font("Arial", Font.BOLD, 30);
        arial_60B = new Font("Arial", Font.BOLD, 60);

        // Tải ảnh nền menu nếu có. Nếu thiếu file, game vẫn chạy với nền đen dự phòng.
        try {
            InputStream is = getClass().getResourceAsStream("/res/menu_bg.png");
            if (is != null) {
                menuBackground = ImageIO.read(is);
            }
        } catch (Exception e) {
            menuBackground = null;
        }
    }
    
///
    public void draw(Graphics2D g2) {

        // ==========================================
        // 1. GIAO DIỆN TRONG TRẬN (HUD)
        // Bọc trong lệnh if để chỉ hiện khi Đang chơi hoặc Tạm dừng
        // ==========================================
        if (gp.gameState == gp.playState || gp.gameState == gp.pauseState) {
            
            // Vẽ chữ Màn chơi
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            g2.setColor(Color.WHITE);
            g2.drawString("MÀN CHƠI: " + gp.currentLevel, 20, 30);
            
            // Vẽ điểm số
            g2.drawString("ĐIỂM: " + gp.score, 520, 30);
            g2.drawString("KỶ LỤC: " + gp.bestScore, 520, 60);
            
            // Vẽ chữ HP
            g2.drawString("HP: ", 20, 65);
            
            // Vẽ khung nền thanh máu (Màu xám)
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(60, 45, 150, 20); 
            
            // Tính toán chiều dài thanh máu hiện tại
            double oneScale = 150.0 / gp.player.maxHp; 
            double hpBarWidth = oneScale * gp.player.hp;
            
            // Vẽ thanh máu (Màu đỏ)
            g2.setColor(Color.RED);
            if (hpBarWidth > 0) {
                g2.fillRect(60, 45, (int)hpBarWidth, 20); 
            }

            // Vẽ viền trắng cho thanh máu (Gom lại gần khối HP cho gọn)
            g2.setColor(Color.WHITE);
            g2.drawRect(60, 45, 150, 20);

            // Thanh Hồi Chiêu (Bên dưới thanh máu)
            g2.setColor(Color.WHITE);
            g2.drawString("SKILL (SPACE): ", 20, 100);
            
            // Khung nền hồi chiêu
            g2.setColor(Color.DARK_GRAY);
            g2.fillRect(220, 85, 100, 15); 
            
            // Tính toán độ dài thanh hồi chiêu (Xanh lơ = Sẵn sàng, Vàng = Đang hồi)
            if (gp.player.skillCooldown == 0) {
                g2.setColor(Color.CYAN);
                g2.fillRect(220, 85, 100, 15);
                g2.drawString("SẴN SÀNG!", 330, 100);
            } else {
                g2.setColor(Color.ORANGE);
                double cdScale = 100.0 / gp.player.skillMaxCooldown;
                double currentCd = 100 - (cdScale * gp.player.skillCooldown);
                g2.fillRect(220, 85, (int)currentCd, 15);
            }

            // ==========================================
            // VẼ THANH MÁU TRÙM CUỐI (TỰ ĐỘNG HIỆN KHI CÓ BOSS)
            // ==========================================
            for (int i = 0; i < gp.monsterList.size(); i++) {
                Monster m = gp.monsterList.get(i);
                
                if (m.type == 3) { // Nếu tìm thấy Trùm Cuối
                    
                    // 1. Vẽ Tên Boss
                    g2.setFont(new Font("Arial", Font.BOLD, 24));
                    g2.setColor(Color.WHITE);
                    String bossName = "Anh Độ Mixi";
                    int textLen = (int) g2.getFontMetrics().getStringBounds(bossName, g2).getWidth();
                    int textX = gp.screenWidth / 2 - textLen / 2;
                    int textY = gp.screenHeight - 75; // Nằm sát dưới đáy màn hình
                    g2.drawString(bossName, textX, textY);

                    // 2. Kích thước thanh máu Boss (Rộng 400px)
                    int barWidth = 400;
                    int barHeight = 20;
                    int barX = gp.screenWidth / 2 - barWidth / 2;
                    int barY = gp.screenHeight - 60;

                    // 3. Khung nền (Xám đen)
                    g2.setColor(new Color(50, 50, 50));
                    g2.fillRect(barX, barY, barWidth, barHeight);

                    // 4. Lõi máu (Đỏ)
                    g2.setColor(Color.RED);
                    double scale = (double) barWidth / m.maxHp;
                    double hpBarValue = scale * m.hp;
                    if (hpBarValue > 0) {
                        g2.fillRect(barX, barY, (int) hpBarValue, barHeight);
                    }

                    // 5. Viền kim loại (Trắng)
                    g2.setColor(Color.WHITE);
                    g2.drawRect(barX, barY, barWidth, barHeight);
                    
                    break; // Chỉ có 1 Boss nên vẽ xong là thoát vòng lặp
                }
            }
            // ==========================================
            // THÔNG BÁO "QUA MÀN" (TỐI GIẢN & MỜ DẦN)
            // ==========================================
            if (gp.monsterList.isEmpty()) {
                levelClearCounter++;
                
                // Hiển thị tối đa trong 3 giây (180 khung hình ở 60FPS)
                if (levelClearCounter < 180) { 
                    g2.setFont(new Font("Arial", Font.ITALIC, 28)); // Nhỏ nhắn, in nghiêng
                    String text = "Qua màn";
                    int textLen = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
                    int textX = gp.screenWidth / 2 - textLen / 2;
                    int textY = gp.screenHeight / 2 - 50; 
                    
                    // Logic Fade-out: Bắt đầu mờ dần đi trong 1 giây cuối (Khung hình 120 -> 180)
                    int alpha = 255;
                    if (levelClearCounter > 120) {
                        alpha = (int) (255 * (1.0f - (levelClearCounter - 120) / 60.0f));
                        if (alpha < 0) alpha = 0;
                    }
                    
                    // Đổ màu trắng tinh khiết kèm theo độ mờ alpha
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.drawString(text, textX, textY);
                }
            } else {
                // Tự động reset bộ đếm về 0 khi sang ải mới (có quái vật)
                levelClearCounter = 0; 
            }

        } // <--- ĐÂY LÀ DẤU NGOẶC KẾT THÚC KHỐI HUD (gp.gameState == gp.playState ...)

        // ==========================================
        // 2. CÁC MÀN HÌNH MENU CHUYÊN DỤNG 
        // (Vẽ sau cùng để đè lên mọi thứ khác)
        // ==========================================
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
        else if (gp.gameState == gp.pauseState) {
            drawPauseScreen(g2); // Lớp phủ Tạm dừng sẽ đè mờ lên cả HUD
        }
        // Nếu bạn có các màn hình Thắng/Thua thì gọi luôn ở đây:
        // else if (gp.gameState == gp.gameOverState) drawGameOverScreen(g2);
        // else if (gp.gameState == gp.gameWinState) drawGameWinScreen(g2);
    }
    

    // ==========================================
    // 4. VẼ MÀN HÌNH GAME OVER
    // ==========================================
    public void drawGameOverScreen(Graphics2D g2) {
        // Phủ lớp đen mờ
        g2.setColor(new Color(0, 0, 0, 150)); 
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // ĐÃ FIX: Khai báo Font trực tiếp với cỡ 60
        g2.setFont(new Font("Arial", Font.BOLD, 60));
        g2.setColor(Color.RED);
        String text = "GAME OVER";
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        int x = gp.screenWidth / 2 - length / 2;
        int y = gp.screenHeight / 2;
        
        // Bóng đổ
        g2.setColor(Color.BLACK);
        g2.drawString(text, x + 3, y + 3);
        
        // Chữ chính
        g2.setColor(Color.RED);
        g2.drawString(text, x, y);

        // ĐÃ FIX: Khai báo Font trực tiếp với cỡ 30
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        g2.setColor(Color.WHITE);
        text = "NHẤN R ĐỂ CHƠI LẠI";
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 100;
        
        g2.drawString(text, x, y);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(Color.YELLOW);
        text = "ĐIỂM: " + gp.score + "   |   KỶ LỤC: " + gp.bestScore;
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 150;
        g2.drawString(text, x, y);
    }
    // ==========================================
    // 5. VẼ MÀN HÌNH CHIẾN THẮNG
    // ==========================================
    public void drawGameWinScreen(Graphics2D g2) {
        // Phủ một lớp đen mờ lên toàn màn hình
        g2.setColor(new Color(0, 0, 0, 150)); 
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);

        // Vẽ dòng chữ CHÚC MỪNG CHIẾN THẮNG
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 45F));
        String text = "CHÚC MỪNG CHIẾN THẮNG!";
        int length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        int x = gp.screenWidth / 2 - length / 2;
        int y = gp.screenHeight / 2 - 50; // Đẩy lên cao một chút
        
        // Vẽ bóng đổ đen
        g2.setColor(Color.BLACK);
        g2.drawString(text, x + 3, y + 3);
        
        // Vẽ chữ chính màu Vàng (Gold)
        g2.setColor(Color.YELLOW); 
        g2.drawString(text, x, y);

        // Vẽ dòng chữ hướng dẫn
        g2.setFont(arial_30);
        g2.setColor(Color.WHITE);
        text = "NHẤN ENTER ĐỂ VỀ MENU";
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 50;
        
        g2.drawString(text, x, y);

        g2.setFont(new Font("Arial", Font.BOLD, 24));
        g2.setColor(Color.YELLOW);
        text = "ĐIỂM CUỐI: " + gp.score + "   |   KỶ LỤC: " + gp.bestScore;
        length = (int) g2.getFontMetrics().getStringBounds(text, g2).getWidth();
        x = gp.screenWidth / 2 - length / 2;
        y = gp.screenHeight / 2 + 100;
        g2.drawString(text, x, y);
    }

    // ==========================================
    // 6. VẼ MÀN HÌNH CHỌN NÂNG CẤP SAU MỖI 3 MÀN
    // ==========================================
    public void drawUpgradeScreen(Graphics2D g2) {
        // Phủ bóng tối
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        
        // Vẽ Tiêu đề
        g2.setFont(new Font("Arial", Font.BOLD, 40));
        g2.setColor(Color.YELLOW);
        String title = "PHẦN THƯỞNG NÂNG CẤP (PHÍM 1, 2, 3)";
        int titleX = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(title, g2).getWidth()/2;
        g2.drawString(title, titleX, 100);

        // Vẽ 3 Hộp lựa chọn
        g2.setFont(new Font("Arial", Font.BOLD, 22));
        
        for (int i = 0; i < 3; i++) {
            int boxWidth = 500;
            int boxHeight = 80;
            int boxX = gp.screenWidth/2 - boxWidth/2;
            int boxY = 180 + (i * 120); // Xếp dọc 3 khung
            
            // Vẽ hộp mờ
            g2.setColor(new Color(50, 50, 50, 200));
            g2.fillRect(boxX, boxY, boxWidth, boxHeight);
            
            // Vẽ viền ngoài
            g2.setColor(Color.WHITE);
            g2.drawRect(boxX, boxY, boxWidth, boxHeight);
            
            // Lấy Tên Nâng cấp dựa trên ID
            String text = "PHÍM " + (i+1) + ": ";
            int buffID = gp.availableUpgrades[i];
            
            if (buffID == 1) { text += "Trái Tim Thép (+2 Máu Tối Đa)"; g2.setColor(Color.GREEN); }
            else if (buffID == 2) { text += "Giày Hermes (+1 Tốc Độ Chạy)"; g2.setColor(Color.CYAN); }
            else if (buffID == 3) { text += "Thiền Định (-2s Hồi Chiêu Cuối)"; g2.setColor(Color.MAGENTA); }
            else if (buffID == 4) { text += "Đạn Xuyên Giáp (+1 Sát Thương Súng)"; g2.setColor(Color.ORANGE); }
            else if (buffID == 5) { text += "Cò Súng Nhạy (Tăng Tốc Độ Bắn)"; g2.setColor(Color.YELLOW); }
            else if (buffID == 6) { text += "Lưỡi Kiếm Huyết (+2 Sát Thương Chém)"; g2.setColor(Color.RED); }
            else if (buffID == 7) { text += "Cổ Tay Linh Hoạt (Tăng Tốc Độ Chém)"; g2.setColor(Color.ORANGE); }
            // THÊM TÊN 6 BUFF MỚI:
            else if (buffID == 8) { text += "Lọ Thuốc Thần (Hồi 50% Máu)"; g2.setColor(Color.PINK); }
            else if (buffID == 9) { text += "Bước Chân Gió (+2 Tốc Độ)"; g2.setColor(Color.CYAN); }
            else if (buffID == 10) { text += "Súng Nòng Đôi (Bắn 2 Viên Tỏa Mở)"; g2.setColor(Color.WHITE); }
            else if (buffID == 11) { text += "Bão Đạn (Chiêu Cuối 36 Viên)"; g2.setColor(Color.YELLOW); }
            else if (buffID == 12) { text += "Gươm Khổng Lồ (Tăng Tầm Chém)"; g2.setColor(Color.LIGHT_GRAY); }
            else if (buffID == 13) { text += "Chém Toàn Phong (Góc Chém 180 Độ)"; g2.setColor(Color.ORANGE); }
            
            // Căn giữa chữ trong hộp
            int textX = boxX + boxWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
            g2.drawString(text, textX, boxY + 48);
        }
    }

    // ==========================================
    // 1. MÀN HÌNH CHÍNH (TITLE SCREEN)
    // ==========================================
    public void drawTitleScreen(Graphics2D g2) {
        if (menuBackground != null) {
            // Vẽ ảnh tràn kích thước màn hình
            g2.drawImage(menuBackground, 0, 0, gp.screenWidth, gp.screenHeight, null);
        } else {
            // Phương án dự phòng: Nếu quên bỏ file ảnh vào thư mục thì nó tự tô đen lại để tránh lỗi
            g2.setColor(Color.BLACK); 
            g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        }
        g2.setFont(new Font("Arial", Font.BOLD, 60)); g2.setColor(Color.WHITE);
        // String text = "DUNGEON CONQUEROR";
        // int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        // g2.drawString(text, x, 200);

        // Menu
        g2.setFont(new Font("Arial", Font.BOLD, 30));
        String text = "BẮT ĐẦU CHƠI";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 400);
        if (commandNum == 0) g2.drawString(">", x - 30, 400); // Vẽ con trỏ

        text = "CÀI ĐẶT (OPTIONS)";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 470);
        if (commandNum == 1) g2.drawString(">", x - 30, 470);

        text = "THOÁT GAME";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 540);
        if (commandNum == 2) g2.drawString(">", x - 30, 540);
    }

    // ==========================================
    // 2. MÀN HÌNH CHỌN NHÂN VẬT
    // ==========================================
    public void drawCharacterScreen(Graphics2D g2) {
        g2.setColor(Color.BLACK); g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setFont(new Font("Arial", Font.BOLD, 50)); g2.setColor(Color.YELLOW);
        String text = "CHỌN CLASS NHÂN VẬT";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 150);

        g2.setFont(new Font("Arial", Font.BOLD, 30)); g2.setColor(Color.WHITE);
        text = "1. XẠ THỦ (Súng viễn trình)";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 300);

        text = "2. KIẾM SĨ (Cận chiến vung nón)";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 400);

        g2.setFont(new Font("Arial", Font.ITALIC, 20)); g2.setColor(Color.GRAY);
        text = "(Nhấn Phím 1 hoặc 2 để chọn, nhấn ESC để quay lại)";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 550);
    }

    // ==========================================
    // 3. MÀN HÌNH CÀI ĐẶT (TÙY CHỈNH ÂM THANH)
    // ==========================================
    public void drawOptionsScreen(Graphics2D g2) {
        g2.setColor(Color.BLACK); g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight);
        g2.setFont(new Font("Arial", Font.BOLD, 50)); g2.setColor(Color.WHITE);
        String text = "CÀI ĐẶT ÂM THANH";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 150);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        // Nhạc nền
        text = "Nhạc Nền: "; x = 300;
        g2.drawString(text, x, 300);
        if (commandNum == 0) g2.drawString(">", x - 30, 300);
        g2.drawRect(x + 200, 275, 150, 30); // Khung thanh âm lượng
        int volumeWidth = 30 * gp.musicVolume; 
        g2.fillRect(x + 200, 275, volumeWidth, 30); // Thanh chạy

        // Hiệu ứng
        text = "Hiệu Ứng (SFX): "; 
        g2.drawString(text, x, 400);
        if (commandNum == 1) g2.drawString(">", x - 30, 400);
        g2.drawRect(x + 230, 375, 150, 30);
        volumeWidth = 30 * gp.seVolume; 
        g2.fillRect(x + 230, 375, volumeWidth, 30);

        // Quay lại
        text = "QUAY LẠI"; 
        g2.drawString(text, x, 500);
        if (commandNum == 2) g2.drawString(">", x - 30, 500);
        
        g2.setFont(new Font("Arial", Font.ITALIC, 20)); g2.setColor(Color.GRAY);
        g2.drawString("(Dùng Mũi tên Trái/Phải để chỉnh)", x, 580);
    }

    // ==========================================
    // 4. MÀN HÌNH TẠM DỪNG (PAUSE)
    // ==========================================
    public void drawPauseScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200)); g2.fillRect(0, 0, gp.screenWidth, gp.screenHeight); // Phủ mờ
        
        g2.setFont(new Font("Arial", Font.BOLD, 50)); g2.setColor(Color.WHITE);
        String text = "TẠM DỪNG";
        int x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 200);

        g2.setFont(new Font("Arial", Font.BOLD, 30));
        
        // 0. TIẾP TỤC (Tọa độ Y = 350)
        text = "TIẾP TỤC";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 350);
        if (commandNum == 0) g2.drawString(">", x - 30, 350);

        // 1. CÀI ĐẶT ÂM THANH (Tọa độ Y = 420)
        text = "CÀI ĐẶT";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 420);
        if (commandNum == 1) g2.drawString(">", x - 30, 420);

        // 2. VỀ TRANG CHỦ (Tọa độ Y = 490)
        text = "VỀ TRANG CHỦ";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 490);
        if (commandNum == 2) g2.drawString(">", x - 30, 490);

        // 3. THOÁT GAME (Tọa độ Y = 560)
        text = "THOÁT GAME";
        x = gp.screenWidth/2 - (int)g2.getFontMetrics().getStringBounds(text, g2).getWidth()/2;
        g2.drawString(text, x, 560);
        if (commandNum == 3) g2.drawString(">", x - 30, 560);
    }
}