package com.jkristian.ioio.scope

import java.util.ArrayDeque
import java.util.ArrayList

/**
 * Contains a sequence of samples. Thread safe.
 */
internal class SampleSet {

    private val samples = ArrayDeque<Sample>()

    val isEmpty: Boolean
        @Synchronized get() = samples.isEmpty()

    @Synchronized
    fun clear() {
        samples.clear()
    }

    @Synchronized
    fun add(sample: Sample) {
        samples.add(sample)
    }

    @Synchronized
    fun getRecent(startTime: Long): List<Sample> {
        while (samples.size > 1 && getSecond(samples).time < startTime) {
            samples.removeFirst()
        }
        return ArrayList(samples)
    }

    private fun <T> getSecond(from: Iterable<T>): T {
        val i = from.iterator()
        i.next()
        return i.next()
    }
}