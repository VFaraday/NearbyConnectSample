package com.vfaraday.nearbyconnectsample.stream;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.view.MotionEvent;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.P2PStarConnectionActivity;
import com.vfaraday.nearbyconnectsample.R;
import com.vfaraday.nearbyconnectsample.databinding.ActivityStreamBinding;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class StreamActivity extends P2PStarConnectionActivity {

    private ActivityStreamBinding layout;

    private static final String SERVICE_ID =
            "com.vfaraday.nerbyconectionsample.SERVICE_ID";

    private String mName = BluetoothAdapter.getDefaultAdapter().getName();

    private static final long ADVERTISING_DURATION = 30000;

    private State mState = State.UNKNOWN;

    @Nullable private AudioRecorder mAudioRecorder;

    private final Set<AudioPlayer> mAudioPlayers = new HashSet<>();

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mDiscoverRunnable = () -> setState(State.DISCOVERING);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.activity_stream);

        RxView.clicks(layout.btnAdvertise)
                .subscribe(v -> {
                    setState(State.ADVERTISING);
                    postDelayed(mDiscoverRunnable, ADVERTISING_DURATION);
                });

        RxView.touches(layout.fabVoice)
                .subscribe(v -> {
                    if (v.getAction() == MotionEvent.ACTION_DOWN) {
                        startRecording();
                    } else if (v.getAction() == MotionEvent.ACTION_UP) {
                        stopRecording();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    }

    @Override
    protected void onStop() {
        super.onStop();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 50, 0);
        setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);

        if (isRecording()) {
            stopRecording();
        }
        if (isPlaying()) {
            stopPlaying();
        }
    }

    protected void onStateChange(State newState) {
        switch (newState) {
            case DISCOVERING:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                disconnectFromAllEndpoints();
                startDiscovered();
                break;
            case ADVERTISING:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpoints();
                startAdvertising();
                break;
            case CONNECTED:
                if (isDiscovering()) {
                    stopDiscovering();
                } else if (isAdvertising()) {
                    removeCallbacks(mDiscoverRunnable);
                }
                break;
        }
    }

    private void updateTextView(State state) {
        if (layout != null) {
            layout.tvState.setText(String.format("Current state: %s", state));
        }
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.STREAM) {
            @SuppressWarnings("ConstantConditions") AudioPlayer player =
                    new AudioPlayer(payload.asStream().asInputStream()) {
                        @WorkerThread
                        @Override
                        protected void onFinish() {
                            final AudioPlayer audioPlayer = this;
                            post(() -> mAudioPlayers.remove(audioPlayer));
                        }
                    };
            mAudioPlayers.add(player);
            player.start();
            logW("START PLAYING");
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        super.onConnected(bundle);
        setState(State.DISCOVERING);
    }

    @Override
    public void onConnectionSuspended(int reason) {
        super.onConnectionSuspended(reason);
        setState(State.UNKNOWN);
    }

    @Override
    public void onEndpointDiscovered(Endpoint endpoint) {
        logW("FOUNT ADVISER");
        connectToEndpoint(endpoint);
    }

    @Override
    public void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        logW("A connection to another device has been initiated");
        acceptConnection(endpoint);
    }

    @Override
    public void onEndpointConnected(Endpoint endpoint) {
        logW("CONNECTED");
        setState(State.CONNECTED);
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        // If we lost all our endpoints, then we should reset the state of our app and go back
        // to our initial state (discovering).
        logW("we lost all our endpoints");
        if (getConnectedEndpoints().isEmpty()) {
            setState(State.DISCOVERING);
        }
    }

    protected void onConnectionFailed(Endpoint endpoint) {
        if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
            connectToEndpoint(endpoint);
        }
    }

    private void stopPlaying() {
        mAudioPlayers.forEach(AudioPlayer::stop);
        mAudioPlayers.clear();
    }

    private boolean isPlaying() {
        return !mAudioPlayers.isEmpty();
    }

    private void startRecording() {
        if (getDiscoveredEndpoints().isEmpty()) {
            logW("DISCOVER EMPTY");
            return;
        }
        try {
            ParcelFileDescriptor [] fileDescriptors = ParcelFileDescriptor.createPipe();
            send(Payload.fromStream(fileDescriptors[0]));

            mAudioRecorder = new AudioRecorder(fileDescriptors[1]);
            mAudioRecorder.start();
            logW("START RECORD");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        if (mAudioRecorder != null) {
            mAudioRecorder.stop();
            mAudioRecorder = null;
        }
    }

    private boolean isRecording() {
        return mAudioRecorder != null && mAudioRecorder.isRecording();
    }

    private void setState(State state) {
        if (mState == state) {
            return;
        }

        mState = state;
        updateTextView(state);
        onStateChange(mState);
    }

    protected void post(Runnable r) {
        mHandler.post(r);
    }

    protected void postDelayed(Runnable r, long duration) {
        mHandler.postDelayed(r, duration);
    }

    protected void removeCallbacks(Runnable r) {
        mHandler.removeCallbacks(r);
    }

    @Override
    protected String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    protected String getName() {
        return mName;
    }

    public State getState() {
        return mState;
    }

    private enum State {
        UNKNOWN,
        DISCOVERING,
        ADVERTISING,
        CONNECTED
    }
}
