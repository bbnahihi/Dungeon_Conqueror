package game.main;

import game.core.GamePanel;

import javax.swing.JFrame;
import java.awt.Dimension;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(true);
        window.setTitle("Dungeon Conqueror"); 
        
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        window.setMinimumSize(new Dimension(gamePanel.screenWidth, gamePanel.screenHeight));
        
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        
        gamePanel.startGameThread();
    }
}   
