package com.vfaraday.nearbyconnectsample.stream;


import android.util.Log;

public abstract class AudioBuffer {
    private static final int [] POSSIBLE_SAMPLE_RATES =
            new int[] {8000, 11025, 16000, 22050, 44100, 48000};

    final int size;
    final int sampleRate;
    final byte[] data;

    protected AudioBuffer() {
        int size = -1;
        int samplerRate = -1;

        for (int rate : POSSIBLE_SAMPLE_RATES) {
            samplerRate = rate;
            size = getMinBufferSize(samplerRate);
            if (validSize(size)) {
                break;
            }
        }

        if (!validSize(size)) {
            size = 1024;
        }
        Log.w("sda", String.valueOf(samplerRate));
        this.size = size;
        this.sampleRate = 8000;
        data = new byte[size];
    }

    protected abstract boolean validSize(int size);

    protected abstract int getMinBufferSize(int samplerRate);

}
