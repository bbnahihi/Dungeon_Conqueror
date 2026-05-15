import java.awt.Rectangle;
import java.awt.image.BufferedImage; 

public class Entity {
    public int x, y;      
    public int speed;     
    public Rectangle solidArea; 
    public boolean collisionOn = false;
    public boolean isGhost = false;
    // --- SỬA LẠI KHU VỰC HÌNH ẢNH ---
    public BufferedImage image;
    public BufferedImage image1, image2; // Lưu 2 trạng thái ảnh khác nhau
    
    // --- MỚI: BỘ ĐẾM HOẠT ẢNH ---
    public int spriteCounter = 0; // Đếm số khung hình trôi qua
    public int spriteNum = 1;     // Lưu trạng thái hiện tại (đang hiện ảnh 1 hay ảnh 2)
}