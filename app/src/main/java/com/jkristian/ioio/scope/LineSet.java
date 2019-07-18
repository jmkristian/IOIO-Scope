package com.jkristian.ioio.scope;

import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

/**
 * Converts a sequence of samples to a sequence of lines.
 */
class LineSet {

    static final int STROKE = 4;
    private static final int HALF_STROKE = 2;
    private static final int SCALE = 16; // pixels/second
    private static final long SEC = 1000000000L;

    final int width;
    final int height;
    final long startTime;
    private final Collection<Line> lines = new ArrayList<>();
    private final long now = System.nanoTime();

    LineSet(View chart) {
        width = chart.getWidth();
        height = chart.getHeight();
        startTime = now - (width * SEC / SCALE);
    }

    boolean isEmpty() {
        return lines.isEmpty();
    }

    void addAll(Collection<Sample> samples) {
        if (samples.isEmpty()) {
            return;
        }
        Sample last = null;
        for (Sample sample : samples) {
            if (last != null) {
                addLineFrom(last, sample.time);
            }
            last = sample;
        }
        addLineFrom(last, now);
    }

    float[] toPoints() {
        float[] points = new float[lines.size() * 4];
        int p = 0;
        for (Line line : lines) {
            points[p++] = line.fromX;
            points[p++] = line.fromY;
            points[p++] = line.toX;
            points[p++] = line.toY;
        }
        return points;
    }

    private void addLineFrom(Sample start, long endTime) {
        if (start.time <= endTime) {
            int lastX = Math.max(0, (int) ((start.time - startTime) * SCALE / SEC));
            int x = Math.min(width - 1, (int) ((endTime - startTime) * SCALE / SEC));
            int y = HALF_STROKE + Math.round((1.0f - start.value) * (height - 1 - HALF_STROKE));
            lines.add(new Line(lastX, y, x, y));
        }
    }
}