package game.tile;

import game.core.GamePanel;

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
        
        mapTileNum = new int[gp.maxWorldCol][gp.maxWorldRow];
        
        getTileInfo();
        loadMap(1);
    }

    // Load tile images for the current theme.
    public void getTileInfo() {
        try {
            tile[0] = new Tile();
            tile[0].collision = false;

            tile[1] = new Tile();
            tile[1].collision = true;

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
            System.out.println("WARNING: Map image file not found!");
            e.printStackTrace();
        }
    }

    public boolean isCollisionTile(int tileId) {
        if (tileId < 0 || tileId >= tile.length) {
            return true;
        }
        if (tile[tileId] == null) {
            return true;
        }
        return tile[tileId].collision;
    }

    public boolean isWalkableTile(int tileId) {
        return isCollisionTile(tileId) == false;
    }

    // Load the map for this level.
    public void loadMap(int level) {
        try {
            String mapPath = "/res/maps/level" + level + ".txt";
            InputStream is = getClass().getResourceAsStream(mapPath);
            
            // Fall back to level 1 if this level map is missing.
            if (is == null) {
                System.out.println("WARNING: Map for level " + level + " not found -> using level 1!");
                is = getClass().getResourceAsStream("/res/maps/level1.txt"); 
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int col = 0;
            int row = 0;

            while (col < gp.maxWorldCol && row < gp.maxWorldRow) {
                String line = br.readLine();
                
                if (line == null) break;

                String numbers[] = line.split(" ");

                while (col < gp.maxWorldCol) {
                    int num = Integer.parseInt(numbers[col]);
                    
                    mapTileNum[col][row] = num;
                    col++;
                }
                
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
                
                if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null) {
                    if (tile[tileNum].image != null) {
                        g2.drawImage(tile[tileNum].image, screenX, screenY, gp.tileSize, gp.tileSize, null);
                    } else {
                        // Fallback colors if a tile image is missing.
                        if (tileNum == 1) {
                            g2.setColor(Color.GRAY);
                        } else {
                            g2.setColor(new Color(40, 40, 40));
                        }
                        g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
                        
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
