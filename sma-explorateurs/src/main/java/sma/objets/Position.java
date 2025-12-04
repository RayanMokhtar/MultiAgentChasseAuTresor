package sma.objets;

import java.util.Objects;

/**
 * Représente une position (x, y) sur la carte
 */
public class Position {
    private volatile int x;
    private volatile int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public synchronized int getX() {
        return x;
    }

    public synchronized int getY() {
        return y;
    }

    public synchronized void setX(int x) {
        this.x = x;
    }

    public synchronized void setY(int y) {
        this.y = y;
    }

    public synchronized void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public double distanceTo(Position other) {
        if (other == null) return Double.MAX_VALUE;
        return Math.sqrt(Math.pow(this.x - other.getX(), 2) + Math.pow(this.y - other.getY(), 2));
    }

    public int distanceManhattan(Position other) {
        if (other == null) return Integer.MAX_VALUE;
        return Math.abs(this.x - other.getX()) + Math.abs(this.y - other.getY());
    }

    public synchronized Position copy() {
        return new Position(this.x, this.y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Position position = (Position) obj;
        return x == position.getX() && y == position.getY();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
