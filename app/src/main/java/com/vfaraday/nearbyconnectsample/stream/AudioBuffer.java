package com.vfaraday.nearbyconnectsample.stream;

public abstract class AudioBuffer {

    int size;
    final int sampleRate;
    final byte[] data;

    protected AudioBuffer() {

        this.sampleRate = 8000;
        this.size = getMinBufferSize(this.sampleRate);

        if (!validSize(this.size)) {
            this.size = 1024;
        }

        data = new byte[size];
    }

    protected abstract boolean validSize(int size);

    protected abstract int getMinBufferSize(int samplerRate);

}
