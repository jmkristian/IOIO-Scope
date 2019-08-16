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
    override fun toString(): String {
        return samples.toString()
    }

    @Synchronized
    fun getRecent(startTime: Long): List<Sample> {
        while (samples.size > 1 && (startTime - getSecond(samples).time >= 0)) {
            samples.removeFirst()
        }
        return ArrayList(samples)
    }

    @Synchronized
    fun getLast(count: Int): List<Sample> {
        val into = ArrayList<Sample>(count)
        var iter = samples.descendingIterator()
        var limit = count
        while (iter.hasNext() && limit-- > 0) {
            into.add(0, iter.next())
        }
        return into
    }

    private fun <T> getSecond(from: Iterable<T>): T {
        val i = from.iterator()
        i.next()
        return i.next()
    }
}