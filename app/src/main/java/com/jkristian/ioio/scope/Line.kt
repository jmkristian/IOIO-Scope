package com.jkristian.ioio.scope

internal class Line(val fromX: Int, val fromY: Int, val toX: Int, val toY: Int) {

    override fun toString(): String {
        return "Line{$fromX,$fromY,$toX,$toY}"
    }
}