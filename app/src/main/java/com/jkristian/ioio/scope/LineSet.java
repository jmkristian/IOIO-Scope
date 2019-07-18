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
    private final long startTime;
    private final Collection<Sample> samples;
    private final long now = System.nanoTime();
    private Collection<Line> lines = null;

    LineSet(View chart, Deque<Sample> samples) {
        width = chart.getWidth();
        height = chart.getHeight();
        startTime = now - (width * SEC / SCALE);
        this.samples = recent(samples);
    }

    boolean isEmpty() {
        return samples.isEmpty();
    }

    float[] toPoints() {
        if (lines == null) {
            computeLines();
        }
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

    private void computeLines() {
        lines = new ArrayList<Line>(samples.size() + 1);
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

    private void addLineFrom(Sample start, long endTime) {
        if (start.time <= endTime) {
            int lastX = Math.max(0, (int) ((start.time - startTime) * SCALE / SEC));
            int x = Math.min(width - 1, (int) ((endTime - startTime) * SCALE / SEC));
            int y = HALF_STROKE + Math.round((1.0f - start.value) * (height - 1 - HALF_STROKE));
            lines.add(new Line(lastX, y, x, y));
        }
    }

    private Collection<Sample> recent(Deque<Sample> from) {
        while (from.size() > 1 && getSecond(from).time < startTime) {
            from.removeFirst();
        }
        return new ArrayList<>(from);
    }

    private static <T> T getSecond(Iterable<T> from) {
        Iterator<T> i = from.iterator();
        i.next();
        return i.next();
    }
}