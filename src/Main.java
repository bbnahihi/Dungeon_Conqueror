import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame();
        
        // Cài đặt cơ bản cho cửa sổ
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        // Tiêu đề cửa sổ đã được thiết lập sẵn thông tin của bạn
        window.setTitle("Dungeon Conquerer"); 
        
        // Thêm màn hình game vào cửa sổ
        GamePanel gamePanel = new GamePanel();
        window.add(gamePanel);
        
        // Hiển thị cửa sổ
        window.pack();
        window.setLocationRelativeTo(null); // Hiển thị ở chính giữa màn hình
        window.setVisible(true);
        
        // Khởi động vòng lặp game
        gamePanel.startGameThread();
    }
}   