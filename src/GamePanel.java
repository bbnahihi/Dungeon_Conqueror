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
    // --- THÊM PHẦN NÀY DƯỚI CÁC THÔNG SỐ MÀN HÌNH ---
    // THÔNG SỐ THẾ GIỚI MỞ
    public final int maxWorldCol = 30; // Bản đồ rộng 50 ô
    public final int maxWorldRow = 30; // Bản đồ cao 50 ô

    // THIẾT LẬP TRẠNG THÁI GAME
    public int gameState;
    public final int titleState = 0;
    public final int playState = 1;
    public final int pauseState = 2;
    public final int gameOverState = 3;
    public final int gameWinState = 4;
    public final int upgradeState = 5; // Trạng thái chọn nâng cấp

    Thread gameThread;
    KeyHandler keyH = new KeyHandler(this);
    public MouseHandler mouseH = new MouseHandler(this); 
    
    public Player player = new Player(this, keyH, mouseH);
    public UpgradeManager upgradeManager = new UpgradeManager(this);
    public TileManager tileM = new TileManager(this);
    // DANH SÁCH ĐẠN VÀ QUÁI VẬT
    public ArrayList<Bullet> bulletList = new ArrayList<>();
    public ArrayList<Monster> monsterList = new ArrayList<>();
    public ArrayList<Item> itemList = new ArrayList<>();
    // QUẢN LÝ BẢN ĐỒ VÀ VA CHẠM
    public CollisionChecker cChecker = new CollisionChecker(this); // THÊM DÒNG NÀY
    public PathFinder pFinder = new PathFinder(this);
    public UI ui = new UI(this);
    public int currentLevel = 1;
    public int score = 0;
    public int bestScore = 0;
    public StatsTracker statsTracker = new StatsTracker();
    private final String saveFileName = "save.dat";

    // Thêm vào phần khai báo biến
    public ArrayList<Particle> particleList = new ArrayList<>();
    // HỆ THỐNG ÂM THANH
    public Sound music = new Sound();
    public Sound se = new Sound(); // se = Sound Effect
    // Các ID của Hệ Sinh Thái
    public final int THEME_FOREST = 0;
    public final int THEME_DUNGEON = 1;
    public final int THEME_DESERT = 2;
    
    public int currentTheme = THEME_FOREST; // Mặc định là Rừng
    public int previousState; // Ghi nhớ xem Options được gọi từ Menu hay Pause

    // Thêm 2 state mới
    public final int characterState = 6; // Màn hình chọn nhân vật
    public final int optionsState = 7;   // Màn hình cài đặt

    // Thêm biến lưu trữ Âm lượng (Mức từ 0 đến 5, mặc định là 3)
    public int musicVolume = 3;
    public int seVolume = 3;

    // Danh sách lưu trữ chữ số sát thương
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
        // Chỉ cho phép mọi thứ chuyển động khi đang ở trạng thái PLAY
        if (gameState == playState) {
            player.update(); 

            // ==========================================
            // 0. CẬP NHẬT ITEM RƠI TRÊN MAP
            // ==========================================
            for (int i = 0; i < itemList.size(); i++) {
                Item item = itemList.get(i);
                item.update();

                if (item.alive == false) {
                    itemList.remove(i);
                    i--;
                }
            }

            // ==========================================
            // 1. CẬP NHẬT QUÁI VẬT VÀ VA CHẠM VỚI NGƯỜI
            // ==========================================
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

            // ==========================================
            // 2. CẬP NHẬT ĐẠN (SIÊU TỐI ƯU)
            // ==========================================
            for (int i = 0; i < bulletList.size(); i++) {
                Bullet b = bulletList.get(i);
                b.update(); // Bullet tự lo việc xét chạm tường và bắn quái (Xuyên thấu/Crit) ở trong này rồi!

                // Xóa đạn nếu ĐÃ CHẾT (chạm tường/hết lực xuyên) HOẶC bay ra ngoài màn hình
                if (b.alive == false || b.x < 0 || b.x > maxWorldCol * tileSize || b.y < 0 || b.y > maxWorldRow * tileSize) {
                    bulletList.remove(i);
                    i--; 
                    continue; 
                }

                // CHỈ CẦN XỬ LÝ LỖI ĐẠN QUÁI BẮN TRÚNG NGƯỜI CHƠI TẠI ĐÂY
                if (b.isPlayerBullet == false) {
                    if (b.getBounds().intersects(player.getBounds()) && player.invincible == false) {
                        player.hp -= b.damage;
                        statsTracker.recordDamageTaken(b.damage);
                        player.invincible = true; 
                        
                        bulletList.remove(i); // Đạn trúng người thì nổ/biến mất
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

            // ==========================================
            // 3. QUÉT DỌN CHIẾN TRƯỜNG (XÓA QUÁI CHẾT)
            // ==========================================
            // Vì Bullet.java chỉ trừ máu (để xử lý xuyên thấu), ta phải dọn xác quái ở đây
            for (int i = 0; i < monsterList.size(); i++) {
                Monster defeatedMonster = monsterList.get(i);
                if (defeatedMonster.hp <= 0) {
                    handleMonsterDefeated(defeatedMonster);
                    monsterList.remove(i);
                    i--;
                }
            }

            // ==========================================
            // 4. CẬP NHẬT HIỆU ỨNG (HẠT VÀ CHỮ SÁT THƯƠNG)
            // ==========================================
            for (int i = 0; i < floatingTextList.size(); i++) {
                if (floatingTextList.get(i) != null) {
                    floatingTextList.get(i).update();
                    if (floatingTextList.get(i).life <= 0) {
                        floatingTextList.remove(i);
                        i--;
                    }
                }
            }
            
            for (int i = 0; i < particleList.size(); i++) {
                Particle p = particleList.get(i);
                p.update();
                if (p.life <= 0) {
                    particleList.remove(i);
                    i--;
                }
            }

            // ==========================================
            // 5. LOGIC CHUYỂN MÀN / CHIẾN THẮNG
            // ==========================================
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
            
        } // <--- Kết thúc khối if (gameState == playState)
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // VẼ DỰA TRÊN TRẠNG THÁI HIỆN TẠI
        if (gameState == titleState) {
            ui.drawTitleScreen(g2);
        } else {
            // ĐANG CHƠI THÌ VẼ BẢN ĐỒ, NGƯỜI, QUÁI
            tileM.draw(g2); 
            
           // VẼ CỬA (Chỉ xuất hiện khi hết quái)
        if (monsterList.isEmpty()) {
            // 1. Xác định tọa độ Thế giới của cửa (Ví dụ: ô 20, 20)
            int doorWorldX = 20 * tileSize;
            int doorWorldY = 20 * tileSize;

            // 2. Tính toán tọa độ hiển thị trên Màn hình
            int doorScreenX = doorWorldX - player.x + player.screenX;
            int doorScreenY = doorWorldY - player.y + player.screenY;

            // 3. Vẽ bằng tọa độ Screen
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
        // 4. VẼ HIỆU ỨNG CỦA KIẾM SĨ
        if (player.classType == 1 && player.isMeleeAttacking) {
                
            // KIỂM TRA CHIÊU CUỐI (Giữ nguyên)
            if (player.meleeHitbox.width > tileSize * 2) {
                int screenHitX = player.meleeHitbox.x - player.x + player.screenX;
                int screenHitY = player.meleeHitbox.y - player.y + player.screenY;
                    
                g2.setColor(new Color(255, 215, 0, 150)); 
                g2.fillOval(screenHitX, screenHitY, player.meleeHitbox.width, player.meleeHitbox.height);
                    
                g2.setColor(Color.ORANGE);
                g2.drawOval(screenHitX, screenHitY, player.meleeHitbox.width, player.meleeHitbox.height);
            } 
            // CHÉM THƯỜNG: CHỈ HIỆN LƯỠI KIẾM QUÉT QUA
            
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

            // NẾU TẠM DỪNG HOẶC CHẾT, VẼ MỘT LỚP PHỦ ĐEN LÊN TRÊN CÙNG
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
        // ==========================================
        // NẾU LÀ MÀN 10: CHỈ SINH RA TRÙM CUỐI
        // ==========================================
        if (level == 10) {
            // Sinh Boss ở tọa độ chính giữa nửa trên bản đồ
            int bossWorldX = (maxWorldCol / 2) * tileSize;
            int bossWorldY = (maxWorldRow / 4) * tileSize;
            
            Monster boss = new Monster(this, bossWorldX, bossWorldY, 3);
            monsterList.add(boss);
            return; // Dừng hàm tại đây, KHÔNG sinh bầy quái nhỏ nữa!
        }
        // TĂNG ĐỘ KHÓ THEO LEVEL
        // Sau khi có item rơi, cần tăng áp lực bằng cách tăng số quái mạnh hơn.
        int monsterCount = 10 + (level * 4); 
        int spawned = 0;
        
        // ĐÃ FIX: Lắp thêm bộ đếm an toàn
        int attempts = 0; 
        
        // Cổng bảo vệ: Thử tìm chỗ đứng tối đa 800 lần, nếu không tìm được thì bỏ qua luôn!
        while (spawned < monsterCount && attempts < 800) {
            attempts++; 
            
            int col = (int)(Math.random() * (maxWorldCol - 2)) + 1;
            int row = (int)(Math.random() * (maxWorldRow - 2)) + 1;

            if (tileM.mapTileNum[col][row] == 0) {
                int distance = Math.abs(col - 15) + Math.abs(row - 15);
                
                if (distance > 8) { 
                    int worldX = col * tileSize;
                    int worldY = row * tileSize;
                    // Càng về sau càng có nhiều quái bắn xa hơn.
                    double meleeRate = Math.max(0.35, 0.65 - level * 0.03);
                    int type = (Math.random() < meleeRate) ? 1 : 2; 
                    
                    Monster m = new Monster(this, worldX, worldY, type);
                    
                    // Cơ chế Elite: xuất hiện sớm hơn và tăng dần theo level.
                    double eliteChance = Math.min(0.25, 0.08 + level * 0.015);
                    if (level >= 3 && type == 1 && Math.random() < eliteChance) {
                        m.transformToElite(); 
                    }

                    // Scaling cơ bản để item không làm game quá dễ.
                    m.maxHp += Math.max(0, level / 2);
                    if (level >= 6) m.speed += 1;
                    if (type == 2 && level >= 4) m.maxHp += 1;

                    if (currentTheme == THEME_DUNGEON) m.maxHp += 2; 
                    else if (currentTheme == THEME_DESERT) m.speed += 1; 
                    
                    if (m.isElite == false) m.hp = m.maxHp; 
                    
                    monsterList.add(m);
                    spawned++; 
                }
            }
        }
    }

    public void nextLevel() {
        // Dời nhân vật ra khỏi cửa ngay lập tức
        player.x = tileSize * 15;
        player.y = tileSize * 15;
        player.isMeleeAttacking = false; 

        // Tăng level
        currentLevel++;
        statsTracker.setLevelReached(currentLevel);

        // Nếu lên màn 4 hoặc 7 (tức là vừa qua 3 màn) thì văng ra bảng chọn nâng cấp
        if ((currentLevel - 1) % 3 == 0 && currentLevel <= 10) {
            upgradeManager.rollUpgrades();
            gameState = upgradeState; 
        } else {
            // Không còn dùng Random Theme nữa, giao phó hết cho hàm mới
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
    // ==========================================
    // HÀM CHUYỂN ĐỔI MỨC ÂM LƯỢNG (0-5) SANG DECIBEL
    // ==========================================
    public float getVolumeDecibels(int volumeLevel) {
        switch(volumeLevel) {
            case 0: return -80.0f; // Mute (Im lặng tuyệt đối)
            case 1: return -20.0f; // Rất nhỏ
            case 2: return -12.0f; // Nhỏ
            case 3: return -5.0f;  // Vừa phải (Mặc định)
            case 4: return 1.0f;   // To
            case 5: return 6.0f;   // Rất to
            default: return -5.0f;
        }
    }

    public void playMusic(int i) {
        music.stop();
        music.setFile(i);
        // Lấy mức âm lượng hiện tại từ Menu và áp dụng ngay
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
        
        // Lấy mức âm lượng hiệu ứng từ Menu
        float currentVol = getVolumeDecibels(seVolume);
        
        // Nếu menu đã tắt âm (Mức 0), thì không cần trừ thêm, ngược lại thì giảm nhẹ cho các âm quá ồn
        if (seVolume > 0) {
            if (i == 1) { 
                currentVol -= 15.0f; // Tiếng súng nghe đanh, giảm 15dB so với nền
            } else if (i == 2) {
                currentVol -= 5.0f;  // Tiếng kiếm chém giảm 5dB cho đỡ đinh tai
            }
        }
        
        se.setVolume(currentVol);
        se.play(); 
    }
    // ==========================================
    // HÀM TẠO HIỆU ỨNG HẠT (DÙNG CHUNG CHO CẢ SÚNG VÀ DAO)
    // ==========================================
    public void generateParticles(int worldX, int worldY) {
        // Tạo 20 hạt khi quái chết
        for (int k = 0; k < 20; k++) {
            // Tốc độ và hướng ngẫu nhiên
            double xVel = (Math.random() - 0.5) * 6; // Bay trái/phải
            double yVel = (Math.random() - 0.5) * 6; // Bay lên/xuống
            
            // Sinh hạt tại tâm con quái, màu đỏ, size 6, sống 40 khung hình
            Particle p = new Particle(this, worldX + tileSize/2, worldY + tileSize/2, Color.RED, 6, xVel, yVel, 40);
            particleList.add(p);
        }
    }
// Hàm này được gọi khi nhân vật đi vào Cổng Dịch Chuyển hoặc qua Ải
    // 1. ĐÃ SỬA CỬA NGÕ: Nhận vào int targetTheme và int level
    public void transitionToNewMap(int level) {
        if (level == 1) {
            statsTracker.startRun(level);
        }
        statsTracker.setLevelReached(level);
        
        // Dọn dẹp chiến trường cũ
        bulletList.clear(); 
        monsterList.clear(); 
        itemList.clear(); 

        // ==========================================
        // CẬP NHẬT THEME THEO ĐÚNG THIẾT KẾ MÀN CHƠI
        // ==========================================
        if (level >= 1 && level <= 3) {
            currentTheme = THEME_FOREST; // Màn 1,2,3 là Rừng
        } else if (level >= 4 && level <= 6) {
            currentTheme = THEME_DUNGEON; // Màn 4,5,6 là Ngục
        } else if (level >= 7 && level <= 9) {
            currentTheme = THEME_DESERT; // Màn 7,8,9 là Sa mạc
        } else if (level >= 10) {
            currentTheme = THEME_DUNGEON; // Đấu trường Boss dùng nền Ngục Tối cho u ám!
        }

        // Tải ảnh gạch và kết cấu ma trận bản đồ
        tileM.getTileInfo(); 
        tileM.loadMap(level); 

        // Không hồi máu tự động khi qua màn nữa.
        // Người chơi cần nhặt item HEART rơi từ quái để hồi máu.

        // Reset nhân vật về điểm xuất phát
        player.x = tileSize * 15; 
        player.y = tileSize * 15;
        player.isMeleeAttacking = false; 
        player.invincible = true;
        player.invincibleCounter = 0;
        
        spawnMonsters(level);
        
        // ĐỔI NHẠC
        if (level == 1) {
            playMusic(0); // Nhạc vô trận
        } else if (level == 10) {
            playMusic(8); // Nhạc đánh Boss
        }
    }
    // ==========================================
    // HỆ THỐNG RƠI ITEM
    // ==========================================
    public void handleMonsterDefeated(Monster defeatedMonster) {
        statsTracker.recordEnemyKilled();
        addScore(getMonsterScore(defeatedMonster));
        generateParticles(defeatedMonster.x, defeatedMonster.y);
        spawnItemDrop(defeatedMonster);
    }

    public void spawnItemDrop(Monster defeatedMonster) {
        if (defeatedMonster == null) return;

        // Boss vẫn rơi item, nhưng không còn rơi quá nhiều máu để tránh làm game quá dễ.
        if (defeatedMonster.type == 3) {
            int[] bossDrops = { Item.TYPE_HEART, Item.TYPE_ENERGY, Item.TYPE_COIN };
            for (int i = 0; i < bossDrops.length; i++) {
                int offsetX = (i - 1) * 32;
                itemList.add(new Item(this, defeatedMonster.x + tileSize - 14 + offsetX, defeatedMonster.y + tileSize - 14, bossDrops[i]));
            }
            return;
        }

        // Giảm tỉ lệ rơi item: quái thường 20%, elite 50%.
        double dropChance = defeatedMonster.isElite ? 0.50 : 0.20;
        if (Math.random() > dropChance) return;

        int itemType = rollItemType();
        int dropX = defeatedMonster.x + tileSize / 2 - 14;
        int dropY = defeatedMonster.y + tileSize / 2 - 14;
        itemList.add(new Item(this, dropX, dropY, itemType));
    }

    public int rollItemType() {
        double r = Math.random();

        if (r < 0.50) return Item.TYPE_COIN;    // +50 điểm
        if (r < 0.65) return Item.TYPE_HEART;   // +1 máu
        if (r < 0.90) return Item.TYPE_ENERGY;  // Giảm hồi chiêu skill ít hơn
        return Item.TYPE_SHIELD;                // Bất tử ngắn hạn hơn
    }

    // ==========================================
    // HỆ THỐNG ĐIỂM VÀ KỶ LỤC
    // ==========================================
    public int getMonsterScore(Monster m) {
        if (m.type == 3) return 1000; // Boss
        if (m.isElite) return 100;    // Quái tinh anh
        if (m.type == 2) return 40;   // Quái bắn xa
        return 25;                    // Quái cận chiến
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

    // ==========================================
    // HÀM RESET GAME (DÙNG KHI CHẾT, CHIẾN THẮNG HOẶC VỀ MENU)
    // ==========================================
    public void resetGame() {
        currentLevel = 1;
        score = 0;
        statsTracker.reset();
        player.setDefaultValues(); // Đưa máu, tốc độ, nâng cấp về mặc định
        monsterList.clear();
        bulletList.clear();
        itemList.clear();
        particleList.clear();
        floatingTextList.clear();
        ui.levelClearCounter = 0;
        
        // Đưa nhân vật về điểm xuất phát
        player.x = tileSize * 15; 
        player.y = tileSize * 15;
    }
}
