package com.jkristian.ioio.scope

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.annotation.ColorInt
import java.util.*

private const val TAG = "Chart"

/**
 * Displays a sequence of samples. Not thread safe.
 */
internal class Chart(private val view: ImageView) {

    private val width: Int = view.width
    private val height: Int = view.height
    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private var samples: SampleSet? = null
    private var now: Long = 0
    private var startTime: Long = 0

    fun setSamples(samples: SampleSet?) {
        this.samples = samples
        now = System.nanoTime()
        startTime = now - (width * SEC / SCALE)
    }

    fun draw(@ColorInt color: Int) {
        val canvas = Canvas(bitmap)
        canvas.drawARGB(0xFF, 0, 0, 0)
        val data = samples
        if (width > 0 && height > 0 && data != null && !data.isEmpty) {
            // Log.v(TAG, "draw samples " + data);
            val paint = Paint()
            paint.color = color
            paint.strokeWidth = STROKE.toFloat()
            paint.strokeCap = Paint.Cap.BUTT
            val points = toPoints(data)
            // Log.v(TAG, "drawLines in " + width + " " + deltas(points));
            canvas.drawLines(points, paint)
        }
    }

    fun show() {
        view.setImageDrawable(BitmapDrawable(view.resources, bitmap))
    }

    private fun toPoints(samples: SampleSet): FloatArray {
        // Log.v(TAG, String.format("getRecent(%f sec)", (now - startTime).toDouble() / SEC))
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
        if (samples == null || samples.isEmpty()) {
            return Collections.emptyList()
        }
        val lines = ArrayList<Line>(samples.size + 1)
        var last: Sample? = null
        for (sample in samples) {
            if (last != null) {
                addLineFrom(lines, last, sample.time)
            }
            last = sample
        }
        addLineFrom(lines, last!!, now)
        return lines
    }

    private fun addLineFrom(into: MutableCollection<Line>, start: Sample, endTime: Long) {
        if (endTime - start.time >= 0) {
            val fromX = Math.max(0, ((start.time - startTime) * SCALE / SEC).toInt())
            val toX = Math.min(width - 1, ((endTime - startTime) * SCALE / SEC).toInt())
            val y = HALF_STROKE + Math.round((1.0f - start.value) * (height - 1 - HALF_STROKE))
            into.add(Line(fromX, y, toX, y))
        }
    }

    companion object {
        val SEC = 1000000000L // nanoseconds per second
        private val SCALE = 16 // pixels/second
        private val STROKE = 4
        private val HALF_STROKE = 2

        private fun deltas(from: FloatArray): Collection<Float> {
            val into = ArrayList<Float>(from.size / 4)
            if (!from.isEmpty()) {
                var last = from[0]
                into.add(last)
                for (f in 2..from.size - 1 step 2) {
                    into.add(from[f] - last)
                    last = from[f]
                }
            }
            return into
        }
    }
}