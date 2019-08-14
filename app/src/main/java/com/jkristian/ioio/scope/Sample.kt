package com.jkristian.ioio.scope

internal class Sample(val time: Long, val value: Float) {

    override fun toString(): String {
        return "Sample{$time,$value}"
    }
}