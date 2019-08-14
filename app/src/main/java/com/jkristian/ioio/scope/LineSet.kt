package com.jkristian.ioio.scope

import android.view.View

import java.util.ArrayList

/**
 * Converts a sequence of samples to a sequence of lines. Not thread safe.
 */
internal class LineSet(chart: View) {

    val width: Int
    val height: Int
    private val now = System.nanoTime()
    private val startTime: Long

    init {
        width = chart.width
        height = chart.height
        startTime = now - width * SEC / SCALE
    }

    fun toPoints(samples: SampleSet): FloatArray {
        val lines = toLines(samples.getRecent(startTime))
        val points = FloatArray(lines.size * 4)
        var p = 0
        for (line in lines) {
            points[p++] = line.fromX.toFloat()
            points[p++] = line.fromY.toFloat()
            points[p++] = line.toX.toFloat()
            points[p++] = line.toY.toFloat()
        }
        return points
    }

    private fun toLines(samples: Collection<Sample>): Collection<Line> {
        val lines = ArrayList<Line>(if (samples == null) 1 else samples.size + 1)
        if (!samples.isEmpty()) {
            var last: Sample? = null
            for (sample in samples) {
                if (last != null) {
                    addLineFrom(lines, last, sample.time)
                }
                last = sample
            }
            addLineFrom(lines, last!!, now)
        }
        return lines
    }

    private fun addLineFrom(lines: MutableCollection<Line>, start: Sample, endTime: Long) {
        if (start.time <= endTime) {
            val fromX = Math.max(0, ((start.time - startTime) * SCALE / SEC).toInt())
            val toX = Math.min(width - 1, ((endTime - startTime) * SCALE / SEC).toInt())
            val y = HALF_STROKE + Math.round((1.0f - start.value) * (height - 1 - HALF_STROKE))
            lines.add(Line(fromX, y, toX, y))
        }
    }

    companion object {

        val STROKE = 4
        private val HALF_STROKE = 2
        private val SCALE = 16 // pixels/second
        private val SEC = 1000000000L
    }
}