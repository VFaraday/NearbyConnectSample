package com.vfaraday.nearbyconnectsample.fileShared;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.P2PStarConnectionActivity;
import com.vfaraday.nearbyconnectsample.R;
import com.vfaraday.nearbyconnectsample.adapter.RecyclerAdapter;
import com.vfaraday.nearbyconnectsample.databinding.P2pStarActivityMainBinding;

import java.io.FileNotFoundException;
import java.util.HashSet;

/**
 * <p>{@link State#UNKNOWN}: We cannot do anything. We are waiting for the GoogleApiClient to
 * connect</p>
 *
 * <p>{@link State#DISCOVERING}: Our default state (after we've connected). We constantly
 * listen for a device to advertise near us. </p>
 *
 * <p>{@link State#ADVERTISING}: We advertise our device so that others nearby can
 * discover us.</p>
 *
 * <p>{@link State#CONNECTED}: We've connected to another device. </p>
 * */
public class MainActivityP2PStar extends P2PStarConnectionActivity {

    /**
     * The state of app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop
     */
    private State mState = State.UNKNOWN;

    /**
     * This service id lets us find other nearby devices that are interested in the same thing.
     * Our sample does exactly one thing, so we hardcode the ID.
     */
    private static final String SERVICE_ID =
            "com.vfaraday.nerbyconectionsample.SERVICE_ID";

    private static final String ENDPOINT_ID_EXTRA = "endpointId";
    private static final int READ_REQUEST_CODE = 42;


    private final String mName = BluetoothAdapter.getDefaultAdapter().getName();

    RecyclerAdapter recyclerAdapter;
    P2pStarActivityMainBinding layout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.p2p_star_activity_main);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerAdapter = new RecyclerAdapter(new HashSet<>());
        layout.listDiscovers.setLayoutManager(mLayoutManager);
        layout.listDiscovers.setAdapter(recyclerAdapter);

        RxView.clicks(layout.btnStartAdvertise)
                .subscribe(v -> {
                    if (isDiscovering()) {
                        stopDiscovering();
                    }
                    startAdvertising();
                    mState = State.ADVERTISING;
                    layout.tvState.setText(String.format("State : %s", mState.toString()));
                });

        RxView.clicks(layout.btnStopAdvertise)
                .subscribe(v -> {
                    stopAdvertising();
                    mState = State.UNKNOWN;
                    layout.tvState.setText(String.format("State : %s", mState.toString()));
                });


        RxView.clicks(layout.btnStartDiscover)
                .subscribe(v -> {
                    if (isAdvertising()) {
                        stopAdvertising();
                    }
                    startDiscovered();
                    mState = State.DISCOVERING;
                    layout.tvState.setText(String.format("State : %s", mState.toString()));
                });

        RxView.clicks(layout.btnStopDiscover)
                .subscribe(v -> {
                    stopDiscovering();
                    mState = State.UNKNOWN;
                    layout.tvState.setText(String.format("State : %s", mState.toString()));
                });
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        new AlertDialog.Builder(this)
                .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                .setMessage("Confirm if the code " + connectionInfo.getAuthenticationToken() +
                        " is also displayed on the other device")
                .setPositiveButton("Accept", (dialogInterface, i) -> {
                    // The user confirmed, so we can accept the connection.
                    acceptConnection(endpoint);
                })
                .setNeutralButton("Cancel", (dialogInterface, i) -> {
                    // The user canceled, so we should reject the connection.
                    rejectConnection(endpoint);
                })
                .show();
        recyclerAdapter.notifyDataSetChanged();
    }

    private void onStateChanged(State oldState, State newState) {

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
                    startDiscovered();
                }
                disconnectFromAllEndpoints();
                stopAdvertising();
                break;
        }

        switch (oldState) {

        }
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        recyclerAdapter.setEndpoints(getDiscoveredEndpoints());
        recyclerAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.STREAM) {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isAdvertising() || isDiscovering()) {
            stopAdvertising();
            stopDiscovering();
        }
    }

    private void showImage(String endpointId) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(ENDPOINT_ID_EXTRA, endpointId);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {

                String endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA);
                Uri uri = resultData.getData();
                ParcelFileDescriptor pfd;
                try {
                    pfd = getContentResolver().openFileDescriptor(uri, "r");
                    Payload payload = Payload.fromFile(pfd);
                    send(payload);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /** {@see P2PStarConnectionActivity#getServiceId()} */
    @Override
    protected String getServiceId() {
        return SERVICE_ID;
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device
     */
    @Override
    protected String getName() {
        return mName;
    }

    /** State thar the UI goes through. */
    private enum State {
        UNKNOWN,
        DISCOVERING,
        ADVERTISING,
        CONNECTED
    }
}