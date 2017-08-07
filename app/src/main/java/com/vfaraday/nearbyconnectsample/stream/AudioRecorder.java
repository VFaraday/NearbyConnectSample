package com.vfaraday.nearbyconnectsample.stream;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class AudioRecorder {

    private static final String TAG = "record";
    private final OutputStream mOutputStream;

    private volatile boolean mAlive;

    private Thread mThread;

    public AudioRecorder(ParcelFileDescriptor fileDescriptor) {
        mOutputStream = new ParcelFileDescriptor.AutoCloseOutputStream(fileDescriptor);
    }

    public boolean isRecording() {
        return mAlive;
    }

    public void start() {
        if (isRecording()) {
            Log.w(TAG, "Already runnig");
            return;
        }

        mAlive = true;
        mThread = new Thread(() -> {
           Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

            Buffer buffer = new Buffer();
            AudioRecord record = new AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_8BIT,
                    buffer.size);

            if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "Failed to start recording");
                mAlive = false;
                return;
            }

            record.startRecording();

            try {
                while (isRecording()) {
                    int len = record.read(buffer.data, 0 , buffer.size);
                    if (len >= 0 && len <= buffer.size) {
                        mOutputStream.write(buffer.data, 0, len);
                        mOutputStream.flush();
                    } else {
                        Log.w(TAG, "Unexpected length returned: " + len);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Exeption with recording ", e);
            } finally {
                stopInternal();
                try {
                    record.stop();
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed to stop recording ", e);
                }
                record.release();
            }
        });

        mThread.start();
    }

    private void stopInternal() {
        mAlive = false;
        try {
            mOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close output ", e);
        }
    }

    public void stop() {
        stopInternal();
        try {
            mThread.join();
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupting while joining AudoiRecorder thread ", e);
            Thread.currentThread().interrupt();
        }
    }

    private static class Buffer extends AudioBuffer {

        @Override
        protected boolean validSize(int size) {
            return size != AudioTrack.ERROR && size != AudioTrack.ERROR_BAD_VALUE;
        }

        @Override
        protected int getMinBufferSize(int samplerRate) {
            return AudioTrack.getMinBufferSize(
                    samplerRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_8BIT);
        }
    }
}
