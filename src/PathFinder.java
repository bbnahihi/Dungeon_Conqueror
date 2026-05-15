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

        // Quét bản đồ xem ô nào là tường
        for (int col = 0; col < gp.maxWorldCol; col++) {
            for (int row = 0; row < gp.maxWorldRow; row++) {
                int tileNum = gp.tileM.mapTileNum[col][row];
                if (gp.tileM.tile[tileNum] != null && gp.tileM.tile[tileNum].collision == true) {
                    node[col][row].solid = true;
                }
            }
        }
    }

    public boolean search() {
        while (goalReached == false && step < 2000) {
            int col = currentNode.col;
            int row = currentNode.row;
            currentNode.checked = true;
            openList.remove(currentNode);

            // ==========================================
            // NÂNG CẤP: TÌM ĐƯỜNG 8 HƯỚNG (DIAGONAL A*)
            // ==========================================
            boolean up = false, down = false, left = false, right = false;
            
            // 1. Quét 4 hướng cơ bản (Lên, Xuống, Trái, Phải) và kiểm tra Tường
            if (row - 1 >= 0) { openNode(node[col][row - 1]); up = !node[col][row - 1].solid; }
            if (row + 1 < gp.maxWorldRow) { openNode(node[col][row + 1]); down = !node[col][row + 1].solid; }
            if (col - 1 >= 0) { openNode(node[col - 1][row]); left = !node[col - 1][row].solid; }
            if (col + 1 < gp.maxWorldCol) { openNode(node[col + 1][row]); right = !node[col + 1][row].solid; }

            // 2. Quét 4 hướng chéo (CHỈ CHO PHÉP ĐI CHÉO NẾU KHÔNG BỊ TƯỜNG KẸT GÓC)
            if (up && left) openNode(node[col - 1][row - 1]);
            if (up && right) openNode(node[col + 1][row - 1]);
            if (down && left) openNode(node[col - 1][row + 1]);
            if (down && right) openNode(node[col + 1][row + 1]);

            int bestNodeIndex = 0;
            int bestNodefCost = 9999;
            
            // Tìm ô có chi phí (khoảng cách) ngắn nhất để đi tiếp
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

            if (openList.size() == 0) break; // Bí đường hoàn toàn
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
            node.gCost = currentNode.gCost + 1;
            node.hCost = Math.abs(node.col - goalNode.col) + Math.abs(node.row - goalNode.row);
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