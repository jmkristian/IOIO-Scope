package com.jkristian.ioio.scope;

import java.util.Collection;

class AddSample implements Runnable {

    private final Collection<Sample> into;
    private final Sample item;

    public AddSample(Collection<Sample> into, Sample item) {
        this.into = into;
        this.item = item;
    }

    @Override
    public void run() {
        into.add(item);
    }
}