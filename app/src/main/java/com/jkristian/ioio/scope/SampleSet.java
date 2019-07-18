package com.jkristian.ioio.scope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Contains a sequence of samples. Thread safe.
 */
class SampleSet {

    private final Deque<Sample> samples = new ArrayDeque<>();

    synchronized void clear() {
        samples.clear();
    }

    synchronized boolean isEmpty() {
        return samples.isEmpty();
    }

    synchronized void add(Sample sample) {
        samples.add(sample);
    }

    synchronized List<Sample> getRecent(long startTime) {
        while (samples.size() > 1 && getSecond(samples).time < startTime) {
            samples.removeFirst();
        }
        return new ArrayList<>(samples);
    }

    private static <T> T getSecond(Iterable<T> from) {
        Iterator<T> i = from.iterator();
        i.next();
        return i.next();
    }
}