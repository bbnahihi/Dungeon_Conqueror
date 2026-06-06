package game.input;

import game.core.GamePanel;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class MouseHandler implements MouseListener, MouseMotionListener {

    public boolean pressed;
    public int mouseX, mouseY;
    GamePanel gp;

    public MouseHandler(GamePanel gp) {
        this.gp = gp;
    }

    private void updateVirtualMousePosition(MouseEvent e) {
        if (gp == null) {
            mouseX = e.getX();
            mouseY = e.getY();
            return;
        }

        mouseX = gp.toVirtualX(e.getX());
        mouseY = gp.toVirtualY(e.getY());
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            updateVirtualMousePosition(e);
            if (gp != null && gp.isInsideVirtualScreen(e.getX(), e.getY()) == false) {
                pressed = false;
                return;
            }
            pressed = true;

            if (gp != null) {
                gp.requestFocus(); 
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            updateVirtualMousePosition(e);
            pressed = false;

            if (gp != null && gp.isMenuLikeState() && gp.isInsideVirtualScreen(e.getX(), e.getY())) {
                gp.handleUIClick(mouseX, mouseY);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updateVirtualMousePosition(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateVirtualMousePosition(e);
    }
}
