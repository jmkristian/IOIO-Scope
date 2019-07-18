package com.jkristian.ioio.scope;

class Sample {

    final long time;
    final float value;

    public Sample(long time, float value) {
        this.time = time;
        this.value = value;
    }

    @Override
    public String toString() {
        return "Sample{" + time + "," + value + "}";
    }
}