package com.jkristian.ioio.scope;

import android.view.View;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Converts a sequence of samples to a sequence of lines. Not thread safe.
 */
class LineSet {

    static final int STROKE = 4;
    private static final int HALF_STROKE = 2;
    private static final int SCALE = 16; // pixels/second
    private static final long SEC = 1000000000L;

    final int width;
    final int height;
    private final long now = System.nanoTime();
    private final long startTime;

    LineSet(View chart) {
        width = chart.getWidth();
        height = chart.getHeight();
        startTime = now - (width * SEC / SCALE);
    }

    float[] toPoints(SampleSet samples) {
        Collection<Line> lines = toLines(samples.getRecent(startTime));
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

    private Collection<Line> toLines(Collection<Sample> samples) {
        Collection<Line> lines = new ArrayList<Line>(samples == null ? 1 : samples.size() + 1);
        if (!samples.isEmpty()) {
            Sample last = null;
            for (Sample sample : samples) {
                if (last != null) {
                    addLineFrom(lines, last, sample.time);
                }
                last = sample;
            }
            addLineFrom(lines, last, now);
        }
        return lines;
    }

    private void addLineFrom(Collection<Line> lines, Sample start, long endTime) {
        if (start.time <= endTime) {
            int fromX = Math.max(0, (int) ((start.time - startTime) * SCALE / SEC));
            int toX = Math.min(width - 1, (int) ((endTime - startTime) * SCALE / SEC));
            int y = HALF_STROKE + Math.round((1.0f - start.value) * (height - 1 - HALF_STROKE));
            lines.add(new Line(fromX, y, toX, y));
        }
    }
}