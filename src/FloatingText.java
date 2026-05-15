import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

public class FloatingText {
    GamePanel gp;
    int worldX, worldY;
    String text;
    Color color;
    
    int life;
    int maxLife = 40; // Chữ sẽ tồn tại trong 40 khung hình (~0.6 giây)

    public FloatingText(GamePanel gp, int worldX, int worldY, String text, Color color) {
        this.gp = gp;
        // Thêm một chút ngẫu nhiên để các số không bị đè cứng lên nhau
        this.worldX = worldX + (int)(Math.random() * 20 - 10);
        this.worldY = worldY + (int)(Math.random() * 20 - 10);
        this.text = text;
        this.color = color;
        this.life = maxLife;
    }

    public void update() {
        worldY -= 1; // Bay từ từ lên trên
        life--;      // Trừ dần thời gian tồn tại
    }

    public void draw(Graphics2D g2) {
        // Chuyển đổi tọa độ Thế Giới (World) sang tọa độ Màn Hình (Screen)
        int screenX = worldX - gp.player.x + gp.player.screenX;
        int screenY = worldY - gp.player.y + gp.player.screenY;

        // Tính toán độ mờ dần (Fade out)
        int alpha = (int) (255 * ((double) life / maxLife));
        if (alpha < 0) alpha = 0;

        g2.setFont(new Font("Arial", Font.BOLD, 20));
        
        // 1. Vẽ bóng đổ (Màu đen mờ dần, thụt xuống 2px)
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.drawString(text, screenX + 2, screenY + 2);

        // 2. Vẽ số sát thương chính
        g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
        g2.drawString(text, screenX, screenY);
    }
}