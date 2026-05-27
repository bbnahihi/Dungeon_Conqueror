package game.input;

import game.core.GamePanel;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

// Bắt buộc phải implements cả 2 giao diện này
public class MouseHandler implements MouseListener, MouseMotionListener {

    public boolean pressed;
    public int mouseX, mouseY; // Tọa độ chuột trên màn hình
    GamePanel gp;

    public MouseHandler(GamePanel gp) {
        this.gp = gp;
    }

    // ==========================================
    // CÁC HÀM CỦA MOUSE LISTENER (Lắng nghe Click)
    // ==========================================
    @Override
    public void mousePressed(MouseEvent e) {
        // e.getButton() == MouseEvent.BUTTON1 nghĩa là CHỈ NHẬN CHUỘT TRÁI
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseX = e.getX();
            mouseY = e.getY();
            pressed = true;

            if (gp != null) {
                gp.requestFocus(); 
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            mouseX = e.getX();
            mouseY = e.getY();
            pressed = false;

            if (gp != null && gp.isMenuLikeState()) {
                gp.handleUIClick(mouseX, mouseY);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // KHÔNG DÙNG HÀM NÀY ĐỂ LÀM GAME HÀNH ĐỘNG
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // ==========================================
    // CÁC HÀM CỦA MOUSE MOTION LISTENER (Lắng nghe Di chuyển)
    // ==========================================
    @Override
    public void mouseDragged(MouseEvent e) {
        // Khi vừa nhấn đè chuột vừa kéo đi
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Khi di chuyển chuột bình thường
        mouseX = e.getX();
        mouseY = e.getY();
    }
}
