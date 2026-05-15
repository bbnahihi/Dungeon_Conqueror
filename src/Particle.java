import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;

public class Particle extends Entity {
    
    GamePanel gp; // MỚI: Khai báo biến GamePanel

    Color color;
    int size;
    double xVel, yVel; // Vận tốc x và y
    int life;          // Tuổi thọ của hạt (tính bằng khung hình)
    int maxLife;

    public Particle(GamePanel gp, int x, int y, Color color, int size, double xVel, double yVel, int life) {
        this.gp = gp; // MỚI: Lưu GamePanel lại để dùng cho hàm draw

        this.x = x;
        this.y = y;
        this.color = color;
        this.size = size;
        this.xVel = xVel;
        this.yVel = yVel;
        this.life = life;
        this.maxLife = life;
    }

    public void update() {
        x += xVel;
        y += yVel;
        life--; // Hạt già đi sau mỗi khung hình
    }

    public void draw(Graphics2D g2) {
        // CÔNG THỨC CUỘN CAMERA
        int screenX = x - gp.player.x + gp.player.screenX;
        int screenY = y - gp.player.y + gp.player.screenY;

        // Hiệu ứng mờ dần dựa trên tuổi thọ còn lại
        float opacity = (float) life / maxLife;
        if (opacity < 0) opacity = 0;
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2.setColor(color);
        // Vẽ hạt ở tọa độ màn hình (screenX, screenY)
        g2.fillRect(screenX, screenY, size, size);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}