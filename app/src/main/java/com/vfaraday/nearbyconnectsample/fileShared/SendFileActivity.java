package com.vfaraday.nearbyconnectsample.fileShared;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.P2PStarConnectionActivity;
import com.vfaraday.nearbyconnectsample.R;
import com.vfaraday.nearbyconnectsample.databinding.ActivitySendfileBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;

public class SendFileActivity extends P2PStarConnectionActivity{

    private final SimpleArrayMap<Long, Payload> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();

    private static final int READ_REQUEST_CODE = 42;

    private File payloadFile;

    private ActivitySendfileBinding layout;

    private boolean changeIntent = false;

    private static final String SERVICE_ID =
            "com.vfaraday.nerbyconectionsample.SERVICE_ID";

    private String mName = BluetoothAdapter.getDefaultAdapter().getName();

    private static final long ADVERTISING_DURATION = 30000;

    private State mState = State.UNKNOWN;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Runnable mDiscoverRunnable = () -> setState(State.DISCOVERING);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.activity_sendfile);

        RxView.clicks(layout.btnAdvertise)
                .subscribe(v -> {
                    setState(State.ADVERTISING);
                    postDelayed(mDiscoverRunnable, ADVERTISING_DURATION);
                });

        RxView.clicks(layout.fabSendfile)
                .subscribe(v -> showImageChooser());

        RxView.clicks(layout.btnOpenFile)
                .subscribe(v -> {
                    changeIntent = true;
                    if (payloadFile != null) {
                        try {
                            Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
                            String extension = android.webkit.MimeTypeMap
                                    .getFileExtensionFromUrl(Uri.fromFile(payloadFile.getAbsoluteFile()).toString());
                            String mimeType = android.webkit.MimeTypeMap
                                    .getSingleton().getMimeTypeFromExtension(extension);
                            myIntent.setDataAndType(Uri.fromFile(payloadFile),mimeType);
                            startActivity(myIntent);
                        }
                        catch (Exception e) {
                            e.getMessage();
                        }
                    } else {
                        Toast.makeText(getBaseContext(), "No File to open", Toast.LENGTH_LONG).show();
                    }
                });

        Intent service = new Intent(this, ForegroundService.class);
        startService(service);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!getGoogleApiClient().isConnected()) {
            getGoogleApiClient().connect();
            logW("connect");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!changeIntent) {
            getGoogleApiClient().disconnect();
            logW(String.valueOf(getGoogleApiClient().isConnected()));
        }
        logW("OnStop");
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
            layout.state.setText(String.format("Current state: %s", state));
        }
    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.BYTES) {
            String payloadFilenameMessage = null;
            try {
                //noinspection ConstantConditions
                payloadFilenameMessage = new String(payload.asBytes(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            addPayloadFileName(payloadFilenameMessage);
        } else if (payload.getType() == Payload.Type.FILE) {
            incomingPayloads.put(payload.getId(), payload);
        }
    }

    private void addPayloadFileName(String payloadFileNameMassege) {
        int colonIndex = payloadFileNameMassege.indexOf(':');
        String payloadId = payloadFileNameMassege.substring(0, colonIndex);
        String fileName = payloadFileNameMassege.substring(colonIndex +1);
        filePayloadFilenames.put(Long.valueOf(payloadId), fileName);
    }

    @Override
    protected void onTransferUpdate(Long payloadId, PayloadTransferUpdate update) {
        if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
            if (!incomingPayloads.isEmpty()) {
                Payload payload = incomingPayloads.remove(payloadId);
                if (payload.getType() == Payload.Type.FILE) {
                    // Retrieve the filename that was received in a bytes payload.
                    //noinspection ConstantConditions
                    String newFilename = "some.jpg";

                    //noinspection ConstantConditions
                    payloadFile = payload.asFile().asJavaFile();
                    logW(newFilename);
                    File renameTo = new File(payloadFile.getParentFile(), newFilename);
                    boolean p = payloadFile.renameTo(renameTo);
                    payloadFile = renameTo;
                    logW(String.valueOf(p));
                    layout.pathFile.setVisibility(View.VISIBLE);
                    layout.btnOpenFile.setVisibility(View.VISIBLE);
                    layout.pathFile.setText(payloadFile.getAbsolutePath());
                }
            }
        }
    }

    private void showImageChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        changeIntent = false;
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                // The URI of the file selected by the user.
                Uri uri = resultData.getData();

                // Open the ParcelFileDescriptor for this URI with read access.
                ParcelFileDescriptor pfd = null;
                try {
                    pfd = getContentResolver().openFileDescriptor(uri, "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                assert pfd != null;
                Payload filePayload = Payload.fromFile(pfd);
                // Construct a simple message mapping the ID of the file payload to the desired filename.
                String payloadFilenameMessage = filePayload.getId() + ":" + uri.getLastPathSegment();

                // Send this message as a bytes payload.
                try {
                    send(Payload.fromBytes(payloadFilenameMessage.getBytes("UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                // Finally, send the file payload.
                logW(String.valueOf(ismGoogleConnect()));
                send(filePayload);
            }
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
    public void onEndpointDiscovered(P2PStarConnectionActivity.Endpoint endpoint) {
        logW("FOUNT ADVISER");
        connectToEndpoint(endpoint);
    }

    @Override
    public void onConnectionInitiated(P2PStarConnectionActivity.Endpoint endpoint, ConnectionInfo connectionInfo) {
        logW("A connection to another device has been initiated");
        acceptConnection(endpoint);
    }

    @Override
    public void onEndpointConnected(P2PStarConnectionActivity.Endpoint endpoint) {
        logW("CONNECTED");
        setState(State.CONNECTED);
    }

    @Override
    protected void onEndpointDisconnected(P2PStarConnectionActivity.Endpoint endpoint) {
        // If we lost all our endpoints, then we should reset the state of our app and go back
        // to our initial state (discovering).
        logW("we lost all our endpoints");
        if (getConnectedEndpoints().isEmpty()) {
            setState(State.DISCOVERING);
        }
    }

    protected void onConnectionFailed(P2PStarConnectionActivity.Endpoint endpoint) {
        if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
            connectToEndpoint(endpoint);
        }
    }

    private void setState(State state) {
        if (mState == state) {
            return;
        }
        mState = state;
        updateTextView(state);
        onStateChange(mState);
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
