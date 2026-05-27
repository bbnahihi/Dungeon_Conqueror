package game.entity;

import game.core.GamePanel;
import game.input.KeyHandler;
import game.input.MouseHandler;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.AlphaComposite; 
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO; 

public class Player extends Entity {
    
    GamePanel gp;
    KeyHandler keyH;
    MouseHandler mouseH; 
    
    int shootCooldown = 0; 
    public double meleeAttackAngle = 0;
    
    public int maxHp = 8; 
    public int hp = maxHp; 
    
    public boolean invincible = false;
    public int invincibleCounter = 0;

    public int classType = 0; // 0: Pháp sư, 1: Kiếm sĩ
    public int gunDamage; 
    public int meleeDamage; 

    // THÔNG SỐ TẤN CÔNG
    public boolean isMeleeAttacking = false;
    public int meleeAttackCounter = 0;
    public Rectangle meleeHitbox = new Rectangle(0, 0, 0, 0); 
    
    // ĐÃ THÊM: BIẾN KIỂM SOÁT HOẠT ẢNH CHO CUNG THỦ
    public boolean isShooting = false;
    public int shootAttackCounter = 0;

    public int skillCooldown = 0; 
    public int skillMaxCooldown = 600; 

    
    // ==========================================
    // KHAI BÁO BIẾN HỆ THỐNG PHÓNG TO (ĐÃ BỔ SUNG)
    // ==========================================
    public double scaleFactor = 5.0; // Hệ số phóng to
    public int drawWidth, drawHeight, offset; // Các biến dùng để vẽ

    public final int screenX;
    public final int screenY;
    public int attackCooldown; 

    public boolean doubleShot = false;     
    public int ultiBulletCount = 24;       
    public int meleeRangeBonus = 0;        
    public double meleeAngleBonus = 0;     

    public BufferedImage[] idleFrames;
    public BufferedImage[] walkFrames;
    public BufferedImage[] attackFrames;
    public int spriteNum = 0;
    public int animationState = 0;
    public BufferedImage[] slashVFX;
    public BufferedImage arrowImage;
    
    public Player(GamePanel gp, KeyHandler keyH, MouseHandler mouseH) {
        this.gp = gp;
        this.keyH = keyH;
        this.mouseH = mouseH; 
        
        screenX = gp.screenWidth / 2 - (gp.tileSize / 2);
        screenY = gp.screenHeight / 2 - (gp.tileSize / 2);

        setDefaultValues();
    }

    // ==========================================
    // HÀM TẢI TRỌN BỘ HOẠT ẢNH DỰA TRÊN LỚP NHÂN VẬT
    // ==========================================
    public void getPlayerImage() {
        
        try {
            BufferedImage idleSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Idle.png"));
            BufferedImage walkSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Walk.png"));
            BufferedImage attackSheet = null;

            if (classType == 0) { // CUNG THỦ
                attackSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Attack03.png"));
                attackFrames = new BufferedImage[9]; // Ảnh Attack03 của cung thủ có 9 frames
                // ==========================================
                // NẠP ẢNH MŨI TÊN (Tên file là arrow.png)
                // ==========================================
                try {
                    java.io.InputStream arrowStream = getClass().getResourceAsStream("/res/player/Arrow.png");
                    if (arrowStream == null) arrowStream = getClass().getResourceAsStream("/res/player/arrow.png");
                    if (arrowStream != null) arrowImage = ImageIO.read(arrowStream);
                } catch (Exception e) {
                    arrowImage = null;
                }
            } 
            else if (classType == 1) { // KIẾM SĨ
                attackSheet = ImageIO.read(getClass().getResourceAsStream("/res/player/Soldier-Attack01.png"));
                attackFrames = new BufferedImage[6]; // Ảnh Attack01 của kiếm sĩ có 6 frames
            }
            
            idleFrames = new BufferedImage[6]; 
            walkFrames = new BufferedImage[8]; 

            // ĐÃ FIX: CHIA CẮT THEO SỐ LƯỢNG FRAME CHÍNH XÁC CỦA TỪNG FILE
            int idleFw = idleSheet.getWidth() / 6; 
            for (int i = 0; i < 6; i++) idleFrames[i] = idleSheet.getSubimage(i * idleFw, 0, idleFw, idleSheet.getHeight());

            int walkFw = walkSheet.getWidth() / 8;
            for (int i = 0; i < 8; i++) walkFrames[i] = walkSheet.getSubimage(i * walkFw, 0, walkFw, walkSheet.getHeight());

            int attackFw = attackSheet.getWidth() / attackFrames.length;
            for (int i = 0; i < attackFrames.length; i++) attackFrames[i] = attackSheet.getSubimage(i * attackFw, 0, attackFw, attackSheet.getHeight());
            
            slashVFX = new BufferedImage[10];
            for (int i = 0; i < 10; i++) {
                // %02d nghĩa là định dạng số có 2 chữ số (01, 02... 10)
                String fileName = String.format("/res/player/slash5-animation_%02d.png", i + 1);
                slashVFX[i] = ImageIO.read(getClass().getResourceAsStream(fileName));
            }
        } catch (Exception e) {
            System.out.println("ERROR: Could not load or slice the sprite sheet!");
            e.printStackTrace();
        }
    }

    public void setDefaultValues() {
        x = gp.tileSize * 15; 
        y = gp.tileSize * 15;
        speed = 4;
        solidArea = new Rectangle(8, 16, 32, 32); 
        
        setupClass(0); 
    }

    public void setupClass(int type) {
        this.classType = type;
        
        doubleShot = false;
        ultiBulletCount = 24;
        meleeRangeBonus = 0;
        meleeAngleBonus = 0;
        
        if (maxHp <= 0) {
            maxHp = 10; 
        }
        
        if (type == 0) {
            gunDamage = 1;
            // ĐÃ SỬA: Giảm cooldown để tốc độ bắn nhanh như chớp
            attackCooldown = 25; 
        } 
        else if (type == 1) {
            meleeDamage = 2;
            // ĐÃ SỬA: Giảm cooldown để vung kiếm nhanh hơn
            attackCooldown = 35; 
        }
        
        hp = maxHp; 
        getPlayerImage();
    }

    public void update() {
        if (invincible == true) {
            invincibleCounter++;
            if (invincibleCounter > 60) { 
                invincible = false;
                invincibleCounter = 0;
            }
        }

        if (shootCooldown > 0) shootCooldown--; 
        if (skillCooldown > 0) skillCooldown--;
 
        if (isMeleeAttacking == true) {
            int shieldSize = gp.tileSize * 3; 
            Rectangle shieldArea = new Rectangle(x + 24 - shieldSize/2, y + 24 - shieldSize/2, shieldSize, shieldSize);

            for (int i = 0; i < gp.bulletList.size(); i++) {
                Bullet b = gp.bulletList.get(i);
                if (b.isPlayerBullet == false && shieldArea.intersects(b.getBounds())) {
                    gp.playSE(2); 
                    gp.bulletList.remove(i); 
                    i--; 
                }
            }

            meleeAttackCounter++;
            
            // ==========================================
            // ĐÃ FIX: Đồng bộ thời gian chém với Cooldown
            // ==========================================
            if (meleeAttackCounter >= attackCooldown) { 
                isMeleeAttacking = false;
                meleeAttackCounter = 0;
                meleeHitbox.setBounds(0, 0, 0, 0); 
            }
        }
        
        if (isShooting == true) {
            shootAttackCounter++;
            if (shootAttackCounter >= attackCooldown) { // Khi chạy hết thời gian hồi chiêu
                isShooting = false;
                shootAttackCounter = 0;
            }
        }

        boolean isMoving = false;
        
        if (keyH.upPressed || keyH.downPressed || keyH.leftPressed || keyH.rightPressed) {
            isMoving = true;
        }

        if (isMoving == true) {
            int oldX = x;
            int oldY = y;

            if (keyH.leftPressed) x -= speed;
            else if (keyH.rightPressed) x += speed;

            collisionOn = false;
            gp.cChecker.checkTile(this);
            if (collisionOn == true) x = oldX; 

            if (keyH.upPressed) y -= speed;
            else if (keyH.downPressed) y += speed;

            collisionOn = false;
            gp.cChecker.checkTile(this);
            if (collisionOn == true) y = oldY; 
            
            if (animationState != 1) {
                animationState = 1; 
                spriteNum = 0; 
            }
        } else {
            if (animationState != 0) {
                animationState = 0; 
                spriteNum = 0; 
            }
        }

        spriteCounter++;
        if (spriteCounter > 5) { 
            if (animationState == 0) { 
                spriteNum++;
                if (idleFrames != null && spriteNum >= idleFrames.length) spriteNum = 0; 
            }
            else if (animationState == 1) { 
                spriteNum++;
                if (walkFrames != null && spriteNum >= walkFrames.length) spriteNum = 0; 
            }
            spriteCounter = 0; 
        }

        // ==========================================
        // TÍNH TOÁN KÍCH THƯỚC VẼ 
        // ==========================================
        drawWidth = (int) (gp.tileSize * scaleFactor);
        drawHeight = (int) (gp.tileSize * scaleFactor);
        offset = (drawWidth - gp.tileSize) / 2;

        int targetWorldX = gp.mouseH.mouseX + x - gp.player.screenX; 
        int targetWorldY = gp.mouseH.mouseY + y - gp.player.screenY;

        if (gp.mouseH.pressed == true) {
            if (shootCooldown == 0) { 
                
                // --- XẠ THỦ (Bắn xa) ---
                if (classType == 0 && isShooting == false) {
                    isShooting = true;
                    if (doubleShot == true) {
                        double angle = Math.atan2(targetWorldY - (y + 24), targetWorldX - (x + 24));
                        int dist = 1000;
                        int tx1 = (int) (x + 24 + Math.cos(angle - 0.2) * dist);
                        int ty1 = (int) (y + 24 + Math.sin(angle - 0.2) * dist);
                        int tx2 = (int) (x + 24 + Math.cos(angle + 0.2) * dist);
                        int ty2 = (int) (y + 24 + Math.sin(angle + 0.2) * dist);
                        
                        gp.bulletList.add(new Bullet(gp, x + 24, y + 24, tx1, ty1, true, gunDamage));
                        gp.bulletList.add(new Bullet(gp, x + 24, y + 24, tx2, ty2, true, gunDamage));
                    } else {
                        gp.bulletList.add(new Bullet(gp, x + 24, y + 24, targetWorldX, targetWorldY, true, gunDamage));
                    }
                    gp.playSE(1); 
                    shootCooldown = Math.max(15, attackCooldown);
                }
                
                // --- KIẾM SĨ (Chém cận chiến) ---
                else if (classType == 1 && isMeleeAttacking == false) {
                    isMeleeAttacking = true;
                    gp.playSE(2); 
                    
                    int attackRange = (int) (gp.tileSize * 0.5 * scaleFactor) + meleeRangeBonus; 
                    double coneAngle = Math.PI / 2 + meleeAngleBonus;  
                    
                    meleeAttackAngle = Math.atan2(targetWorldY - (y + 24), targetWorldX - (x + 24));
                    
                    for (int i = 0; i < gp.monsterList.size(); i++) {
                        Monster m = gp.monsterList.get(i);
                        
                        Rectangle mBounds = m.getBounds();
                        int mCenterX = mBounds.x + (mBounds.width / 2);
                        int mCenterY = mBounds.y + (mBounds.height / 2);
                        int monsterRadius = mBounds.width / 2; 
                        
                        double distanceToCenter = Math.sqrt(Math.pow(mCenterX - (x + 24), 2) + Math.pow(mCenterY - (y + 24), 2));
                        double distanceToEdge = distanceToCenter - monsterRadius;

                        double angleToMonster = Math.atan2(mCenterY - (y + 24), mCenterX - (x + 24));
                        double angleDifference = angleToMonster - meleeAttackAngle;
                        
                        while (angleDifference <= -Math.PI) angleDifference += Math.PI * 2;
                        while (angleDifference > Math.PI) angleDifference -= Math.PI * 2;

                        // ==========================================
                        // ĐÃ NÂNG CẤP: XÓA ĐIỂM MÙ & ĐẨY LÙI
                        // ==========================================
                        boolean isHit = false;
                        
                        // A. Nếu quái đè sát vào người -> Trúng 100%
                        if (distanceToCenter <= monsterRadius) {
                            isHit = true; 
                        } 
                        // B. Va chạm bình thường trong tầm vung kiếm
                        else if (distanceToEdge <= attackRange && Math.abs(angleDifference) <= coneAngle / 2.0) {
                            isHit = true; 
                        }

                        // XỬ LÝ SÁT THƯƠNG VÀ HIỆU ỨNG
                        if (isHit) {
                            m.hp -= meleeDamage; 
                            gp.floatingTextList.add(new FloatingText(gp, m.x, m.y, "-" + meleeDamage, Color.YELLOW));
                            
                            // // KNOCKBACK: Đẩy lùi quái
                            // int knockbackForce = 20; 
                            // m.x += (int)(Math.cos(meleeAttackAngle) * knockbackForce);
                            // m.y += (int)(Math.sin(meleeAttackAngle) * knockbackForce);

                            // STUN: Làm choáng quái trong nửa giây
                            m.stunCounter = 15;

                            if (m.hp <= 0) {
                                gp.playSE(2); 
                                gp.handleMonsterDefeated(m);
                                gp.monsterList.remove(i);
                                i--; 
                            }
                        }
                    }
                    shootCooldown = Math.max(20, attackCooldown);
                }
            } 
        } 

        // CHIÊU CUỐI (SKILL)
        if (keyH.spacePressed == true && skillCooldown == 0) {
            keyH.spacePressed = false; 

            if (classType == 0) { 
                int ultiBullets = ultiBulletCount; 
                int ultiDamage = gunDamage * 3; 

                for (int i = 0; i < ultiBullets; i++) {
                    double angle = (Math.PI * 2) / ultiBullets * i; 
                    int tX = (int) (x + 24 + Math.cos(angle) * 100);
                    int tY = (int) (y + 24 + Math.sin(angle) * 100);
                    gp.bulletList.add(new Bullet(gp, x + 24, y + 24, tX, tY, true, ultiDamage));
                }
                gp.playSE(3);
            }
            else if (classType == 1) { 
                isMeleeAttacking = true;
                meleeAttackCounter = 0; 
                gp.playSE(4);
                
                int ultimateSize = gp.tileSize * 8;
                meleeHitbox.setBounds(x + 24 - ultimateSize / 2, y + 24 - ultimateSize / 2, ultimateSize, ultimateSize);
                
                for (int i = 0; i < gp.monsterList.size(); i++) {
                    Monster m = gp.monsterList.get(i);
                    if (meleeHitbox.intersects(m.getBounds())) {
                        m.hp -= 15; 
                        gp.floatingTextList.add(new FloatingText(gp, m.x, m.y, "-15", Color.YELLOW));
                        if (m.hp <= 0) {
                            gp.playSE(2); 
                            gp.handleMonsterDefeated(m);
                            gp.monsterList.remove(i);
                            i--;
                        }
                    }
                }
            }
            skillCooldown = skillMaxCooldown; 
        }
    }
    
    public void draw(Graphics2D g2) {
        if (invincible == true) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); 
        }

        BufferedImage image = null;

        try {
            // Ưu tiên 1: ĐANG CHÉM KIẾM (Kiếm Sĩ)
            if (classType == 1 && isMeleeAttacking == true) {
                
                double progress = (double) meleeAttackCounter / attackCooldown;
                int attackFrameIndex = 0; 
                
                // Thao túng nhịp độ (Non-linear animation)
                if (progress < 0.15) {
                    attackFrameIndex = 0;
                } else if (progress < 0.60) {
                    double swingProgress = (progress - 0.15) / 0.45;
                    attackFrameIndex = 1 + (int)(swingProgress * (attackFrames.length - 2));
                } else {
                    attackFrameIndex = attackFrames.length - 1; 
                }

                if (attackFrameIndex >= attackFrames.length) {
                    attackFrameIndex = attackFrames.length - 1;
                }
                
                image = attackFrames[attackFrameIndex];
            } 
            // Ưu tiên 2: ĐANG BẮN CUNG (Cung Thủ)
            else if (classType == 0 && isShooting == true) {
                double progress = (double) shootAttackCounter / attackCooldown;
                int attackFrameIndex = (int) (progress * attackFrames.length); 
                if (attackFrameIndex >= attackFrames.length) attackFrameIndex = attackFrames.length - 1;
                image = attackFrames[attackFrameIndex];
            } 
            // Ưu tiên 3: ĐANG CHẠY HOẶC ĐỨNG YÊN (Áp dụng chung)
            else {
                if (animationState == 0) { // ĐỨNG YÊN
                    if (idleFrames != null && spriteNum < idleFrames.length) {
                        image = idleFrames[spriteNum];
                    }
                } else if (animationState == 1) { // ĐANG CHẠY
                    if (walkFrames != null && spriteNum < walkFrames.length) {
                        image = walkFrames[spriteNum];
                    }
                }
            }
        } catch (Exception e) {
            spriteNum = 0; 
        }

        //TỰ ĐỘNG CĂN TÂM & CO GIÃN THEO TỶ LỆ GỐC
        int drawX = gp.player.screenX; 
        int drawY = gp.player.screenY; 
        int currentDrawWidth = gp.tileSize; 
        int currentDrawHeight = gp.tileSize;

        if (image != null) {
            // Cố định chiều cao theo scaleFactor
            currentDrawHeight = (int) (gp.tileSize * scaleFactor); 
            
            // Tính chiều rộng linh hoạt theo tỷ lệ thật của ảnh
            double ratio = (double) image.getWidth() / image.getHeight();
            currentDrawWidth = (int) (currentDrawHeight * ratio);

            // Tự động tính toán bù trừ để căn giữa ảnh vào Hitbox
            int offsetX = (currentDrawWidth - gp.tileSize) / 2;
            int offsetY = (currentDrawHeight - gp.tileSize) / 2;
            
            drawX = gp.player.screenX - offsetX;
            drawY = gp.player.screenY - offsetY;
        }

        int playerCenterX = gp.player.screenX + gp.tileSize / 2;
        int playerCenterY = gp.player.screenY + gp.tileSize / 2;

        // Logic Lật ảnh theo hướng chuột
        if (gp.mouseH.mouseX < playerCenterX) { 
            drawX = drawX + currentDrawWidth; 
            currentDrawWidth = -currentDrawWidth; 
        }

        // Vẽ nhân vật
        if (image != null) {
            g2.drawImage(image, drawX, drawY, currentDrawWidth, currentDrawHeight, null);
        } else {
            g2.setColor(Color.WHITE);
            g2.fillRect(gp.player.screenX, gp.player.screenY, gp.tileSize, gp.tileSize); 
        }

        if (invincible == true) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }

        g2.setColor(Color.RED);
        g2.drawLine(playerCenterX, playerCenterY, gp.mouseH.mouseX, gp.mouseH.mouseY);

        // VẼ HIỆU ỨNG VFX KẾT HỢP (GIÓ CHUẨN HITBOX, LỬA KHỔNG LỒ)
        if (classType == 1 && isMeleeAttacking == true) {
            
            double progress = (double) meleeAttackCounter / attackCooldown;
            int vfxIndex = (int) (progress * slashVFX.length);
            if (vfxIndex >= slashVFX.length) vfxIndex = slashVFX.length - 1;
            BufferedImage currentSlash = slashVFX[vfxIndex];

            java.awt.geom.AffineTransform oldTransform = g2.getTransform(); 
            g2.rotate(meleeAttackAngle, playerCenterX, playerCenterY);

            // ==========================================
            // 1. KHAI BÁO 2 TẦM ĐÁNH KHÁC NHAU
            // ==========================================
            // A. Tầm sát thương thật (Đảm bảo hệ số ở đây khớp 100% với hàm update)
            int attackRange = (int) (gp.tileSize * 0.5 * scaleFactor) + meleeRangeBonus; 
            
            // B. Tầm nhìn của hình ảnh Lửa (Hệ số to hơn để lửa phình ra)
            int visualRange = (int) (gp.tileSize * 0.8 * scaleFactor) + meleeRangeBonus; 
            
            // Góc chém chung
            double totalAngleRadian = Math.PI / 2 + meleeAngleBonus;
            int totalAngleDeg = (int) Math.toDegrees(totalAngleRadian);

            // ----------------------------------------------------
            // 2. VẼ VỆT GIÓ MỜ (Chỉ vẽ bằng attackRange)
            // ----------------------------------------------------
            int windAlpha = 150 - (150 * meleeAttackCounter / attackCooldown);
            if (windAlpha < 0) windAlpha = 0;
            g2.setColor(new Color(255, 200, 100, windAlpha)); 
            
            int startAngle = -totalAngleDeg / 2; 
            
            // Vệt gió báo hiệu đúng giới hạn mất máu của quái!
            g2.fillArc(playerCenterX - attackRange, playerCenterY - attackRange, 
                       attackRange * 2, attackRange * 2, 
                       startAngle, totalAngleDeg); 

            // ----------------------------------------------------
            // 3. VẼ SPRITE LỬA (Vẽ to ra bằng visualRange)
            // ----------------------------------------------------
            if (currentSlash != null) {
                
                // Toán học bóp méo lửa dựa trên visualRange (không liên quan tới Hitbox nữa)
                int vfxHeight = (int) (visualRange * totalAngleRadian * 1.2); 
                int vfxWidth = (int) (visualRange * 1.2); 
                
                int vfxX = playerCenterX - (int)(visualRange * 0.2); 
                int vfxY = playerCenterY - (vfxHeight / 2); 
                
                g2.drawImage(currentSlash, vfxX, vfxY, vfxWidth, vfxHeight, null);
            }

            // Trả lại màn hình
            g2.setTransform(oldTransform); 
        }
        
    }

    public Rectangle getBounds() {
        return new Rectangle(x + solidArea.x, y + solidArea.y, solidArea.width, solidArea.height);
    }
}   
