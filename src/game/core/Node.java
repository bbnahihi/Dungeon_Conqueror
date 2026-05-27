package game.core;

public class Node {
    Node parent; // Ghi nhớ bước đi trước đó
    public int col, row;
    public int gCost, hCost, fCost; // Các chỉ số tính toán quãng đường
    boolean solid, open, checked;

    public Node(int col, int row) {
        this.col = col;
        this.row = row;
    }
}
