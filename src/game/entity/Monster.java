package game.entity;

import game.core.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Monster extends Entity {
    
    GamePanel gp;
    public int hp; 
    public int maxHp;
    public int type; // 1 = Cận chiến, 2 = Bắn xa
    public int shootCooldown = 0; 
    
    private boolean lookLeft = false; // Dùng để lật ảnh

    public boolean isElite = false;
    // ĐÃ THÊM: Biến kiểm soát trạng thái Khựng lại khi bị chém
    public int stunCounter = 0;
    // THÊM 3 BIẾN NÀY VÀO TRÊN CÙNG CLASS MONSTER
    public int escapeCounter = 0; // Bộ đếm thời gian bỏ chạy
    public int escapeDirX = 0;    // Hướng chạy trục X
    public int escapeDirY = 0;    // Hướng chạy trục Y
    public BossState currentState;
    

    public interface BossState {
    void enter(Monster boss);  // Chạy 1 lần khi bắt đầu vào trạng thái
    void update(Monster boss); // Chạy 60 lần/giây
    void exit(Monster boss);   // Chạy 1 lần trước khi chuyển sang trạng thái khác
    }
    public Monster(GamePanel gp, int startX, int startY, int type) {
        this.gp = gp;
        this.x = startX; 
        this.y = startY; 
        this.type = type;

        // CHỈ SỐ VÀ HITBOX DỰA THEO LOẠI QUÁI
        if (type == 1) {
            this.speed = 3;
            this.maxHp = 3;
            this.solidArea = new Rectangle(8, 8, 32, 32); // HITBOX THON GỌN (Dư 8px mỗi viền)
        } else if (type == 2) {
            this.speed = 2; 
            this.maxHp = 2; 
            this.solidArea = new Rectangle(8, 8, 32, 32);   
        } else if (type == 3) { // TRÙM CUỐI (To gấp đôi nên Hitbox cũng phải căn chỉnh)
            this.speed = 4;
            this.maxHp = 500; 
            this.solidArea = new Rectangle(16, 16, 64, 64); // Ô gạch 96x96, Hitbox 64x64
        }
        if (type == 3 && currentState != null) {
        currentState.update(this);
}       
        this.hp = this.maxHp; 
        
        // TUYỆT ĐỐI KHÔNG KHAI BÁO LẠI solidArea BẰNG TILESIZE Ở ĐÂY NỮA NHÉ!
        getMonsterImage();
    }

    public void getMonsterImage() {
        try {
            if (type == 1) {
                image1 = ImageIO.read(getClass().getResourceAsStream("/res/monster_1_1.png"));
                image2 = ImageIO.read(getClass().getResourceAsStream("/res/monster_1_2.png"));
            }
            if (type == 2) {
                image1 = ImageIO.read(getClass().getResourceAsStream("/res/monster_2_1.png"));
                image2 = ImageIO.read(getClass().getResourceAsStream("/res/monster_2_2.png"));
            }
            else if (type == 3) {
                // Giả sử ảnh Boss đặt trong /res/boss/ và tên là boss1.png, boss2.png
                image1 = ImageIO.read(getClass().getResourceAsStream("/res/boss/boss1.png"));
                image2 = ImageIO.read(getClass().getResourceAsStream("/res/boss/boss2.png"));
                
                // Nếu Boss không có ảnh bắn riêng, ta dùng ảnh di chuyển làm ảnh bắn
                
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        
        // ==========================================
        // 0. KIỂM TRA CHOÁNG (HIT-STUN) TỪ ĐÒN CHÉM
        // ==========================================
        if (stunCounter > 0) {
            stunCounter--;
            return; // Đang bị choáng thì đứng im, không di chuyển, không bắn súng
        }

        // ==========================================
        // TÍNH TOÁN KHOẢNG CÁCH TỚI NGƯỜI CHƠI
        // ==========================================
        int playerCenterX = gp.player.x + 24;
        int playerCenterY = gp.player.y + 24;
        
        // Boss to gấp đôi nên bán kính cũng to hơn quái thường
        int size = (type == 3) ? gp.tileSize * 2 : gp.tileSize; 
        int monsterCenterX = x + (size / 2);
        int monsterCenterY = y + (size / 2);
        
        double distance = Math.sqrt(Math.pow(playerCenterX - monsterCenterX, 2) + Math.pow(playerCenterY - monsterCenterY, 2));

        // ==========================================
        // AI V12: A* 8-HƯỚNG (ĐÁNH BAY KẸT GÓC VÀ ĐI SONG SONG)
        // ==========================================
        if (distance > size / 2) {
            int oldX = x;
            int oldY = y;

            int mCenterX = x + solidArea.x + solidArea.width / 2;
            int mCenterY = y + solidArea.y + solidArea.height / 2;
            int pCenterX = gp.player.x + gp.player.solidArea.x + gp.player.solidArea.width / 2;
            int pCenterY = gp.player.y + gp.player.solidArea.y + gp.player.solidArea.height / 2;

            if (pCenterX - mCenterX < 0) lookLeft = true;
            else if (pCenterX - mCenterX > 0) lookLeft = false;

            int startCol = mCenterX / gp.tileSize;
            int startRow = mCenterY / gp.tileSize;
            int goalCol = pCenterX / gp.tileSize;
            int goalRow = pCenterY / gp.tileSize;

            if (startCol < 0) startCol = 0; if (startCol >= gp.maxWorldCol) startCol = gp.maxWorldCol - 1;
            if (startRow < 0) startRow = 0; if (startRow >= gp.maxWorldRow) startRow = gp.maxWorldRow - 1;
            if (goalCol < 0) goalCol = 0; if (goalCol >= gp.maxWorldCol) goalCol = gp.maxWorldCol - 1;
            if (goalRow < 0) goalRow = 0; if (goalRow >= gp.maxWorldRow) goalRow = gp.maxWorldRow - 1;

            gp.pFinder.setNodes(startCol, startRow, goalCol, goalRow);
            gp.pFinder.setNodeSolid(goalCol, goalRow, false); 

            boolean pathFound = gp.pFinder.search();
            int targetX = pCenterX;
            int targetY = pCenterY;

            if (pathFound == true && gp.pFinder.pathList.size() > 0) {
                int step = 0;
                // Nội suy: Nếu đang đứng trên ô đầu tiên, nhắm tới ô thứ 2 để duy trì gia tốc mượt mà
                if (gp.pFinder.pathList.size() > 1 && gp.pFinder.pathList.get(0).col == startCol && gp.pFinder.pathList.get(0).row == startRow) {
                    step = 1;
                }
                targetX = gp.pFinder.pathList.get(step).col * gp.tileSize + gp.tileSize / 2;
                targetY = gp.pFinder.pathList.get(step).row * gp.tileSize + gp.tileSize / 2;
            }

            int moveX = targetX - mCenterX;
            int moveY = targetY - mCenterY;

            // 1. DI CHUYỂN TRỤC X
            if (Math.abs(moveX) > 0) {
                int currentSpeed = (Math.abs(moveX) < speed) ? Math.abs(moveX) : speed;
                x += (moveX > 0) ? currentSpeed : -currentSpeed;
                collisionOn = false; gp.cChecker.checkTile(this);
                if (collisionOn) x = oldX; 
            }

            // 2. DI CHUYỂN TRỤC Y
            if (Math.abs(moveY) > 0) {
                int currentSpeed = (Math.abs(moveY) < speed) ? Math.abs(moveY) : speed;
                y += (moveY > 0) ? currentSpeed : -currentSpeed;
                collisionOn = false; gp.cChecker.checkTile(this);
                if (collisionOn) y = oldY;
            }
        }     
        // --- 2. LOGIC HOẠT ẢNH BƯỚC ĐI ---
        spriteCounter++;
        if (spriteCounter > 15) {
            if (spriteNum == 1) {
                spriteNum = 2;
            } else if (spriteNum == 2) {
                spriteNum = 1;
            }
            spriteCounter = 0; 
        }

        // --- 3. AI BẮN SÚNG (Dành cho quái Type 2) ---
        if (type == 2) {
            if (shootCooldown == 0) {
                int startX = x + gp.tileSize / 2;
                int startY = y + gp.tileSize / 2;
                // Nhắm thẳng vào tọa độ thực của người chơi
                int targetX = gp.player.x + gp.tileSize / 2;
                int targetY = gp.player.y + gp.tileSize / 2;
                
                Bullet b = new Bullet(gp, startX, startY, targetX, targetY, false, 1);
                gp.bulletList.add(b);
                
                // Quái bắn xa bắn nhanh dần theo level.
                shootCooldown = gp.difficulty.applyRangedCooldown(Math.max(35, 75 - gp.currentLevel * 4)); 
            }
            if (shootCooldown > 0) shootCooldown--;
        }
        
        // --- 4. SIÊU AI TRÙM CUỐI (Type 3) ---
        else if (type == 3) {
            // ==========================================
            // CƠ CHẾ PHASE 2 (NỔI ĐIÊN)
            // ==========================================
            
            // Kiểm tra máu: Nổi điên khi máu <= 50%
            boolean isEnraged = (this.hp <= this.maxHp / 2);

            // 1. BUFF TỐC ĐỘ DI CHUYỂN
            this.speed = gp.difficulty.getBossSpeed(isEnraged); // Boss gây áp lực mạnh hơn ở cả 2 phase

            // 2. LOGIC XẢ ĐẠN (BULLET HELL)
            if (shootCooldown == 0) {
                int startX = x + gp.tileSize;
                int startY = y + gp.tileSize;
                
                if (isEnraged) {
                    // --- PHASE 2: BÃO ĐẠN ---
                    // Tỉ lệ 40% mỗi khung hình Boss sẽ đẻ ra một con quái cận chiến (Type 1) bảo vệ mình
                    if (Math.random() < gp.difficulty.applyBossMinionChance(0.50)) {
                        Monster minion = new Monster(gp, x + gp.tileSize, y + gp.tileSize, 1);
                        gp.difficulty.applyMonsterStats(minion);
                        if (minion.isElite == false) minion.hp = minion.maxHp;
                        gp.monsterList.add(minion);
                    }
                    
                    // Tỏa 12 hướng, sát thương 2 máu/viên (Đã sửa vòng lặp lên 12 cho khớp chia góc 360 độ)
                    for(int i = 0; i < 12; i++) {
                        double angle = Math.PI / 6 * i; // Chia 360 độ cho 12
                        int tX = (int) (startX + Math.cos(angle) * 100);
                        int tY = (int) (startY + Math.sin(angle) * 100);
                        
                        // Chú ý số 2 ở cuối: Đạn Boss giờ trừ 2 máu!
                        Bullet b = new Bullet(gp, startX, startY, tX, tY, false, 3);
                        gp.bulletList.add(b);
                    }
                    shootCooldown = gp.difficulty.getBossShootCooldown(true); // Nhả đạn nhanh hơn ở phase 2
                    
                } else {
                    // --- PHASE 1: CHILL CHILL ---
                    // Tỏa 8 hướng, sát thương 1 máu/viên
                    for(int i = 0; i < 8; i++) {
                        double angle = Math.PI / 4 * i; 
                        int tX = (int) (startX + Math.cos(angle) * 100);
                        int tY = (int) (startY + Math.sin(angle) * 100);
                        
                        Bullet b = new Bullet(gp, startX, startY, tX, tY, false, 1);
                        gp.bulletList.add(b);
                    }
                    shootCooldown = gp.difficulty.getBossShootCooldown(false); // Phase 1 cũng nhanh hơn một chút
                }
            }
            if (shootCooldown > 0) shootCooldown--;
        }
    }

    public void draw(Graphics2D g2) {
        // --- CÔNG THỨC CUỘN CAMERA ---
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        // CHỌN ẢNH VÀ LẬT HƯỚNG
        BufferedImage imageToDraw = null;
        if (spriteNum == 1) imageToDraw = image1;
        if (spriteNum == 2) imageToDraw = image2;

        // Lấy kích thước thật của thực thể (Type 3 là Boss to gấp đôi)
        int size = (type == 3) ? gp.tileSize * 2 : gp.tileSize;

        int drawX = screenX;
        int drawY = screenY;
        int drawWidth = size;
        int drawHeight = size;

        // Xử lý lật mặt
        if (lookLeft == true) {
            drawX = screenX + drawWidth; // Cộng thêm width thực tế
            drawWidth = -drawWidth; 
        }

        // CHỈ VẼ NẾU QUÁI VẬT NẰM LỌT TRONG MÀN HÌNH (Tối ưu game chạy mượt hơn)
        // ĐÃ FIX: Dùng biến 'size' thay vì cứng 'gp.tileSize' để tính toán Hitbox camera
        if (x + size > gp.player.x - gp.player.screenX &&
            x - size < gp.player.x + gp.player.screenX &&
            y + size > gp.player.y - gp.player.screenY &&
            y - size < gp.player.y + gp.player.screenY) {
            
            if (imageToDraw != null) {
                g2.drawImage(imageToDraw, drawX, drawY, drawWidth, drawHeight, null);
                // --- HIỆU ỨNG HÀO QUANG ĐỎ CHO ELITE (TẠM THỜI) ---
            if (isElite == true) {
                g2.setColor(new Color(255, 0, 0, 100)); // Màu đỏ, độ mờ (Alpha) là 100
                // Vẽ một vòng oval dưới chân con quái
                g2.fillOval(screenX, screenY + gp.tileSize / 2, gp.tileSize, gp.tileSize / 2);
            }
            } else {
                // ĐÃ FIX: Vẽ hình hộp dự phòng với đúng kích thước 'size' và thêm màu cho Boss
                if (type == 1) g2.setColor(Color.RED);
                else if (type == 2) g2.setColor(Color.MAGENTA);
                else if (type == 3) g2.setColor(Color.ORANGE); 
                
                g2.fillRect(screenX, screenY, size, size); 
            }
        }
    }

    // Hitbox vẫn dùng tọa độ Thế giới để tính toán đạn bắn chuẩn xác
    public Rectangle getBounds() {
        int size = (type == 3) ? gp.tileSize * 2 : gp.tileSize;
        return new Rectangle(x, y, size, size);
    }

    // ==========================================
    // HÀM BIẾN DỊ (DÀNH CHO QUÁI ELITE)
    // ==========================================
    public void transformToElite() {
        this.isElite = true;
        this.speed = 5;        // Elite nhanh hơn người chơi một chút
        this.maxHp += 5;       // Trâu hơn
        this.hp = this.maxHp;  // Hồi đầy máu
        
        // --- TẢI LẠI SPRITE RIÊNG CHO ELITE ---
        // (Bạn nhớ sửa lại đường dẫn thư mục và tên file ảnh cho đúng với game của bạn nhé)
        try {
            // Ví dụ bạn dùng 2 ảnh up1, up2 để làm animation:
            // up1 = ImageIO.read(getClass().getResourceAsStream("/monster/elite_down_1.png"));
            // up2 = ImageIO.read(getClass().getResourceAsStream("/monster/elite_down_2.png"));
            // ... (Làm tương tự cho các hướng khác nếu có)
            
        } catch (Exception e) {
            System.out.println("Error: Elite image file not found!");
            e.printStackTrace();
        }
    }
    public void changeState(BossState newState) {
    if (currentState != null) currentState.exit(this);
    currentState = newState;
    currentState.enter(this);
}
}
