package game.core;

import java.awt.Rectangle;
import java.util.ArrayList;

public class PathFinder {
    GamePanel gp;
    public Node[][] node;
    ArrayList<Node> openList = new ArrayList<>();
    public ArrayList<Node> pathList = new ArrayList<>();
    Node startNode, goalNode, currentNode;
    boolean goalReached = false;
    int step = 0;

    public PathFinder(GamePanel gp) {
        this.gp = gp;
        instantiateNodes();
    }

    public void instantiateNodes() {
        node = new Node[gp.maxWorldCol][gp.maxWorldRow];
        for (int col = 0; col < gp.maxWorldCol; col++) {
            for (int row = 0; row < gp.maxWorldRow; row++) {
                node[col][row] = new Node(col, row);
            }
        }
    }

    public void resetNodes() {
        for (int col = 0; col < gp.maxWorldCol; col++) {
            for (int row = 0; row < gp.maxWorldRow; row++) {
                node[col][row].open = false;
                node[col][row].checked = false;
                node[col][row].solid = false;
            }
        }
        openList.clear();
        pathList.clear();
        goalReached = false;
        step = 0;
    }

    public void setNodes(int startCol, int startRow, int goalCol, int goalRow) {
        resetNodes();
        startNode = node[startCol][startRow];
        currentNode = startNode;
        goalNode = node[goalCol][goalRow];
        openList.add(currentNode);
        boolean useImageMapCollision = gp.isNormalBackgroundMapActive();

        // Mark wall tiles as blocked.
        for (int col = 0; col < gp.maxWorldCol; col++) {
            for (int row = 0; row < gp.maxWorldRow; row++) {
                Rectangle tileWorldArea = new Rectangle(col * gp.tileSize, row * gp.tileSize, gp.tileSize, gp.tileSize);

                if (useImageMapCollision) {
                    if (gp.collidesWithMapCollision(tileWorldArea, false)) {
                        node[col][row].solid = true;
                    }
                } else {
                    int tileNum = gp.tileM.mapTileNum[col][row];
                    if (gp.tileM.tile[tileNum] != null && gp.tileM.tile[tileNum].collision == true) {
                        node[col][row].solid = true;
                    }

                    if (gp.collidesWithNormalMapObstacle(tileWorldArea, false)) {
                        node[col][row].solid = true;
                    }
                }
            }
        }
    }

    public void setNodeSolid(int col, int row, boolean solid) {
        node[col][row].solid = solid;
    }

    public boolean search() {
        while (goalReached == false && step < 2000) {
            int col = currentNode.col;
            int row = currentNode.row;
            currentNode.checked = true;
            openList.remove(currentNode);

            // 8-way A* search.
            boolean up = false, down = false, left = false, right = false;
            
            // Check straight neighbors first.
            if (row - 1 >= 0) { openNode(node[col][row - 1]); up = !node[col][row - 1].solid; }
            if (row + 1 < gp.maxWorldRow) { openNode(node[col][row + 1]); down = !node[col][row + 1].solid; }
            if (col - 1 >= 0) { openNode(node[col - 1][row]); left = !node[col - 1][row].solid; }
            if (col + 1 < gp.maxWorldCol) { openNode(node[col + 1][row]); right = !node[col + 1][row].solid; }

            // Diagonal moves are allowed only when corners are not blocked.
            if (up && left) openNode(node[col - 1][row - 1]);
            if (up && right) openNode(node[col + 1][row - 1]);
            if (down && left) openNode(node[col - 1][row + 1]);
            if (down && right) openNode(node[col + 1][row + 1]);

            int bestNodeIndex = 0;
            int bestNodefCost = 9999;
            
            // Pick the cheapest open node.
            for (int i = 0; i < openList.size(); i++) {
                if (openList.get(i).fCost < bestNodefCost) {
                    bestNodeIndex = i;
                    bestNodefCost = openList.get(i).fCost;
                } else if (openList.get(i).fCost == bestNodefCost) {
                    if (openList.get(i).gCost > openList.get(bestNodeIndex).gCost) {
                        bestNodeIndex = i;
                    }
                }
            }

            if (openList.size() == 0) break;
            currentNode = openList.get(bestNodeIndex);
            
            if (currentNode == goalNode) {
                goalReached = true;
                trackThePath();
            }
            step++;
        }
        return goalReached;
    }

    private void openNode(Node node) {
    if (node.open == false && node.checked == false && node.solid == false) {
        node.open = true;
        node.parent = currentNode;
        
        // Diagonal movement costs a little more than straight movement.
        if (node.col != currentNode.col && node.row != currentNode.row) {
            node.gCost = currentNode.gCost + 14;
        } else {
            node.gCost = currentNode.gCost + 10;
        }
        
        // Octile distance works well for 8-way movement.
        int dx = Math.abs(node.col - goalNode.col);
        int dy = Math.abs(node.row - goalNode.row);
        node.hCost = 10 * (dx + dy) + (14 - 2 * 10) * Math.min(dx, dy);
        
        node.fCost = node.gCost + node.hCost;
        openList.add(node);
    }
}

    private void trackThePath() {
        Node current = goalNode;
        while (current != startNode) {
            pathList.add(0, current);
            current = current.parent;
        }
    }
}
