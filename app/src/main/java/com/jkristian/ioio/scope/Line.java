package com.jkristian.ioio.scope;

class Line {
    final int fromX;
    final int fromY;
    final int toX;
    final int toY;

    public Line(int fromX, int fromY, int toX, int toY) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    @Override
    public String toString() {
        return "Line{" + fromX + "," + fromY + "," + toX + "," + toY + "}";
    }
}