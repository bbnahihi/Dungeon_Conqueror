import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.awt.Color;
import javax.imageio.ImageIO;

public class TileManager {
    GamePanel gp;
    public Tile[] tile;
    public int mapTileNum[][];

    public TileManager(GamePanel gp) {
        this.gp = gp;
        tile = new Tile[10]; 
        
        // MẢNG HIỆN TẠI LÀ 50x50
        mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];
        
        getTileInfo(); // Bây giờ hàm này sẽ tự động đọc biến gp.currentTheme để lấy ảnh
        loadMap(1);
    }

    // ==========================================
    // ĐÃ NÂNG CẤP: TỰ ĐỘNG NẠP ẢNH THEO CHỦ ĐỀ
    // ==========================================
    public void getTileInfo() {
        try {
            // Khởi tạo 2 loại ô cơ bản
            tile[0] = new Tile();
            tile[0].collision = false; // Sàn nhà

            tile[1] = new Tile();
            tile[1].collision = true;  // Bức tường (Vật lý cản đường)

            // ==========================================
            // LOGIC XOAY VÒNG THEME (HỆ SINH THÁI)
            // ==========================================
            if (gp.currentTheme == gp.THEME_FOREST) {
                tile[0].image = ImageIO.read(getClass().getResourceAsStream("/res/maps/forest_floor.png"));
                tile[1].image = ImageIO.read(getClass().getResourceAsStream("/res/maps/forest_wall.png"));
            } 
            else if (gp.currentTheme == gp.THEME_DUNGEON) {
                tile[0].image = ImageIO.read(getClass().getResourceAsStream("/res/maps/dungeon_floor.png"));
                tile[1].image = ImageIO.read(getClass().getResourceAsStream("/res/maps/dungeon_wall.png"));
            } 
            else if (gp.currentTheme == gp.THEME_DESERT) {
                tile[0].image = ImageIO.read(getClass().getResourceAsStream("/res/maps/desert_floor.png"));
                tile[1].image = ImageIO.read(getClass().getResourceAsStream("/res/maps/desert_wall.png"));
            }
            
        } catch (Exception e) {
            System.out.println("CẢNH BÁO: Chưa tìm thấy file ảnh bản đồ!");
            e.printStackTrace();
        }
    }

    // ==========================================
    // LOGIC TẠO MAP VÀ VẼ MAP GIỮ NGUYÊN 100%
    // ==========================================
    public void loadMap(int level) {
        try {
            // Tự động tìm file theo level. Ví dụ: qua màn 2 sẽ tìm level2.txt
            String mapPath = "/res/maps/level" + level + ".txt";
            InputStream is = getClass().getResourceAsStream(mapPath);
            
            // LỚP BẢO VỆ: Nếu bạn chưa kịp tạo level2.txt, level3.txt...
            // Nó sẽ tự động load lại level1.txt để game không bị Crash.
            if (is == null) {
                System.out.println("CẢNH BÁO: Chưa làm map cho level " + level + " -> Dùng tạm map 1!");
                is = getClass().getResourceAsStream("/res/maps/level1.txt"); 
            }

            // Mở file ra đọc từng dòng
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int col = 0;
            int row = 0;

            while (col < gp.maxWorldCol && row < gp.maxWorldRow) {
                // Đọc 1 dòng (ví dụ: "1 0 0 1 1...")
                String line = br.readLine();
                
                if (line == null) break;

                // Tách các con số ra dựa vào khoảng trắng
                String numbers[] = line.split(" ");

                while (col < gp.maxWorldCol) {
                    // Ép kiểu chữ thành số nguyên
                    int num = Integer.parseInt(numbers[col]);
                    
                    // Ghi vào ma trận bộ nhớ của game
                    mapTileNum[col][row] = num;
                    col++;
                }
                
                // Hết 1 hàng (đạt 30 cột) thì xuống hàng tiếp theo
                if (col == gp.maxWorldCol) {
                    col = 0;
                    row++;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void draw(Graphics2D g2) {
        int worldCol = 0;
        int worldRow = 0;

        while (worldCol < gp.maxWorldCol && worldRow < gp.maxWorldRow) {

            int tileNum = mapTileNum[worldCol][worldRow];

            int worldX = worldCol * gp.tileSize;
            int worldY = worldRow * gp.tileSize;
            int screenX = worldX - gp.player.x + gp.player.screenX;
            int screenY = worldY - gp.player.y + gp.player.screenY;

            if (worldX + gp.tileSize > gp.player.x - gp.player.screenX &&
                worldX - gp.tileSize < gp.player.x + gp.player.screenX &&
                worldY + gp.tileSize > gp.player.y - gp.player.screenY &&
                worldY - gp.tileSize < gp.player.y + gp.player.screenY) {
                
                if (tile[tileNum] != null) {
                    // ==========================================
                    // KÍNH X-QUANG: NẾU CÓ ẢNH THÌ VẼ ẢNH, KHÔNG CÓ THÌ ĐỔ MÀU!
                    // ==========================================
                    if (tile[tileNum].image != null) {
                        g2.drawImage(tile[tileNum].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
                    } else {
                        // Vẽ màu tạm thời (Xám = Tường, Xanh đen = Sàn)
                        if (tileNum == 1) {
                            g2.setColor(Color.GRAY); // Tường
                        } else {
                            g2.setColor(new Color(40, 40, 40)); // Sàn nhà
                        }
                        g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
                        
                        // Vẽ thêm cái viền mờ mờ cho dễ nhìn từng ô gạch
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawRect(screenX, screenY, gp.tileSize, gp.tileSize);
                    }
                }
            }

            worldCol++;
            if (worldCol == gp.maxWorldCol) {
                worldCol = 0;
                worldRow++;
            }
        }
    }
}