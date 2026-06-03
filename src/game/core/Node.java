package game.core;

public class Node {
    Node parent; // Previous step in the path.
    public int col, row;
    public int gCost, hCost, fCost; // A* costs.
    boolean solid, open, checked;

    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }
}
