package com.vfaraday.nearbyconnectsample.stream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Process;

import java.io.IOException;
import java.io.InputStream;

public class AudioPlayer {

    private InputStream mInputStream;

    private volatile boolean mAlive;

    private Thread mThread;

    public AudioPlayer(InputStream inputStream) {
        mInputStream = inputStream;
    }

    public boolean isPlaying() {
        return mAlive;
    }

    public void start() {
        mAlive = true;
        mThread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            Buffer buffer = new Buffer();
            @SuppressWarnings("deprecation") AudioTrack audioTrack =
                    new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            8000,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_8BIT,
                            buffer.size,
                            AudioTrack.MODE_STREAM
                    );
            audioTrack.play();

            int len;
            try {
                while (isPlaying() && (len = mInputStream.read(buffer.data))> 0) {
                    audioTrack.write(buffer.data, 0, len);
                }
            } catch (IOException e) {
                e.getMessage();
            } finally {
                stopInternal();
                audioTrack.release();
                onFinish();
            }
        });
        mThread.start();
    }

    private void stopInternal() {
        mAlive = false;
        try {
            mInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        stopInternal();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            e.getMessage();
            Thread.currentThread().interrupt();
        }
    }

    protected void onFinish() { }

    private static class Buffer extends AudioBuffer {

        @Override
        protected boolean validSize(int size) {
            return size != AudioTrack.ERROR && size != AudioTrack.ERROR_BAD_VALUE;
        }

        @Override
        protected int getMinBufferSize(int samplerRate) {
            return AudioTrack.getMinBufferSize(
                    samplerRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_8BIT);
        }
    }

}
