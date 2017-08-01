package com.vfaraday.nearbyconnectsample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class ConnectionActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "NearbySample";

    /**
     * These permission are required before connecting to Nearby Connection.
     */
    private static final String [] REQUIRED_PERMISSIONS =
            new String[] {
              Manifest.permission.BLUETOOTH,
              Manifest.permission.BLUETOOTH_ADMIN,
              Manifest.permission.ACCESS_WIFI_STATE,
              Manifest.permission.CHANGE_WIFI_STATE,
              Manifest.permission.ACCESS_COARSE_LOCATION
            };

    private static final int REQUEST_CODE_REQUIRED_PERMISSION = 1;

    /** The Connection strategy we'll use for Nearby Connection */
    private static final Strategy STAR_STRATEGY = Strategy.P2P_STAR;

    /** The devices we've discovered near us */
    private final Map<String, Endpoint> mDiscoveredEndpoints = new HashMap<>();

    /** The devices we have pending connections to */
    private final Map<String, Endpoint> mPendingConnections = new HashMap<>();

    /** The devices we are currently connected to. For advertisers, this may be large. For
     * discovers, there will be only one entry in this map
     */
    private final Map<String, Endpoint> mEstablishConnections = new HashMap<>();

    /** We'll talk to Nearby Connection through the GoogleApiClient */
    private GoogleApiClient mGoogleApiClient;

    /**
     *  True if we are asking a discovered device to connect to us. While we ask, we cannot ask
     *  another device
     */
    private boolean mIsConnecting = false;

    /** True if we are discovering */
    private boolean mIsDiscovering = false;

    /** True if we are advertising*/
    private boolean mIsAdertising = false;

    /** Callbacks for connections to other devices */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    logD(String.format(
                            "OnConnectionInitiated(endpointId = %s, endpointName = %s)",
                            endpointId, connectionInfo.getEndpointName()));
                    Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mPendingConnections.put(endpointId, endpoint);
                    ConnectionActivity.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    logD(String.format("onConnectionResponse(endpoint = %s, result = %s)",
                            endpointId, result));

                    mIsConnecting = false;

                    if (!result.getStatus().isSuccess()) {
                        logW(String.format("Connection failed. Recieved status %s.",
                                ConnectionActivity.toString(result.getStatus())));
                        onConnectionFailed(mPendingConnections.remove(endpointId));
                        return;
                    }
                    connectedToEndpoint(mEstablishConnections.get(endpointId));
                }

                @Override
                public void onDisconnected(String endpointId) {
                    if (!mEstablishConnections.containsKey(endpointId)) {
                        logW("Unexpected disconnection from endpoint " + endpointId);
                        return;
                    }
                    disconnectedFromEndpoint(mEstablishConnections.get(endpointId));
                }
            };

    private final PayloadCallback mPayloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    logD(String.format("onPayloadReceived(endpoindId = %s, payload = %s)",
                            endpointId, payload));
                    onReceive(mEstablishConnections.get(endpointId), payload);
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    logD(String.format(
                            "onPayloadTransferUpdate(endpointId = %s, update = %s)", endpointId, update
                    ));
                }
            };

    private void createGoogleClientApi() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Nearby.CONNECTIONS_API)
                    .build();
        }
    }

    private void resetState() {
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishConnections.clear();
        mIsConnecting = false;
        mIsAdertising = false;
        mIsDiscovering = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (hasPermissions(this, REQUIRED_PERMISSIONS)) {
            createGoogleClientApi();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSION);
            }
        }
    }

    /** connected to Nearby Connections */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        logD("onConnected");
    }

    /** temporarily disconnected from Nearby Connections */
    @CallSuper
    @Override
    public void onConnectionSuspended(int reason) {
        logW(String.format("onConnectionSuspended (reason = %s", reason));
        resetState();
    }

    /** unable to connect to Nearby Connections */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        logW(String.format("onConnectionFailed(%s)",
                ConnectionActivity.toString(new Status(connectionResult.getErrorCode()))));
    }

    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSION) {
            for (int grantResult: grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Missing Permissions", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            recreate();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    protected void startAdvertising() {
        mIsAdertising = true;
        Nearby.Connections.startAdvertising(
                mGoogleApiClient,
                getName(),
                getServiceId(),
                mConnectionLifecycleCallback,
                new AdvertisingOptions(STAR_STRATEGY))
                .setResultCallback(
                        new ResultCallback<Connections.StartAdvertisingResult>() {
                            @Override
                            public void onResult(@NonNull Connections.StartAdvertisingResult result) {
                                if (result.getStatus().isSuccess()) {
                                    logD("Now advertising endpoint " + result.getLocalEndpointName());
                                    onAdvertisingStarted();
                                } else {
                                    mIsAdertising = false;
                                    logW(String.format("Advertising failed. Status %s",
                                            ConnectionActivity.toString(result.getStatus())));
                                    onAdvertisingFailed();
                                }
                            }
                        });
    }

    protected void stopAdvertising() {
        mIsAdertising = false;
        Nearby.Connections.stopAdvertising(mGoogleApiClient);
    }

    protected void onAdvertisingFailed() {
    }

    protected void onAdvertisingStarted() {
    }

    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {}

    protected void acceptConnection(final Endpoint endpoint) {
        Nearby.Connections.acceptConnection(mGoogleApiClient, endpoint.getId(), mPayloadCallback)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (!status.isSuccess()) {
                            logW(String.format("acceptConnection failed %s", ConnectionActivity.toString(status)));
                        }
                    }
                });
    }

    protected void rejectConnection(Endpoint endpoint) {
        Nearby.Connections.rejectConnection(mGoogleApiClient, endpoint.getId())
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    logW(String.format("rejectConnection failed %s", ConnectionActivity.toString(status)));
                                }
                            }
                        }
                );
    }

    protected void startDiscovered() {
        mIsDiscovering = true;
        mDiscoveredEndpoints.clear();
        Nearby.Connections.startDiscovery(
                mGoogleApiClient,
                getServiceId(),
                new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                        logD(String.format(
                                "onEndpointFound(endpointId = %s, serviceId = %s, endpointName = %s",
                                endpointId, info.getServiceId(), info.getEndpointName()));

                        if (getServiceId().equals(info.getServiceId())) {
                            Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                            mDiscoveredEndpoints.put(endpointId, endpoint);
                            onEndpointDiscovered(endpoint);
                        }
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        logD(String.format("onEndpointLost(endpointId = %s)", endpointId));
                    }
                },
                new DiscoveryOptions(STAR_STRATEGY))
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (status.isSuccess()) {
                                    onDiscoveryStarted();
                                } else {
                                    mIsDiscovering = false;
                                    logW(String.format("Discovering failed. Status %s",
                                            ConnectionActivity.toString(status)));
                                    onDiscoveryFailed();
                                }
                            }
                        });
    }

    protected void stopDiscovering() {
        mIsDiscovering = false;
        Nearby.Connections.stopDiscovery(mGoogleApiClient);
    }

    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    protected void onDiscoveryStarted() {}

    protected void onDiscoveryFailed() {}

    protected void onEndpointDiscovered(Endpoint endpoint) {}

    protected void disconnect(Endpoint endpoint) {
        Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.getId());
        mEstablishConnections.remove(endpoint.getId());
    }

    protected void disconnectFromAllEndpoints() {
        for (Endpoint endpoint: mEstablishConnections.values()) {
            Nearby.Connections.disconnectFromEndpoint(mGoogleApiClient, endpoint.getId());
        }
        mEstablishConnections.clear();
    }

    protected void connectToEndpoint(final Endpoint endpoint) {
        if (mIsConnecting) {
            logW("Already connecting this endpoint " + endpoint);
            return;
        }

        logD("Sending a connection request to endpoint " + endpoint);
        mIsConnecting = true;

        Nearby.Connections.requestConnection(
                mGoogleApiClient, getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    logW(String.format(
                                            "requestConnection failed. %s", ConnectionActivity.toString(status)));
                                    mIsConnecting = false;
                                    onConnectionFailed(endpoint);
                                }
                            }
                        });
    }

    protected boolean isConnecting() {
        return mIsConnecting;
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        logD(String.format("connectedTo Endpoint(endpoint = %s)", endpoint));
        mEstablishConnections.put(endpoint.getId(), endpoint);
        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        logD(String.format("conntectedToEndpoint(endpoint = %s)", endpoint));
        mEstablishConnections.remove(endpoint.getId());
        onEndpointDisconnected(endpoint);
    }

    protected void onConnectionFailed(Endpoint endpoint) {}

    protected void onEndpointConnected(Endpoint endpoint) {}

    protected void onEndpointDisconnected(Endpoint endpoint) {}

    protected Set<Endpoint> getConnectedEndpoints() {
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.addAll(mEstablishConnections.values());
        return endpoints;
    }

    protected Set<Endpoint> getDiscoveredEndpoints() {
        Set<Endpoint> endpoints = new HashSet<>();
        endpoints.addAll(mDiscoveredEndpoints.values());
        return endpoints;
    }

    protected void send(Payload payload) {
        send(payload, mEstablishConnections.keySet());
    }

    private void send(Payload payload, Set<String> endpoints) {
        Nearby.Connections.sendPayload(mGoogleApiClient, new ArrayList<String>(endpoints), payload)
                .setResultCallback(
                        new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                if (!status.isSuccess()) {
                                    logW(String.format(
                                            "sendPayload failed. %s",
                                            ConnectionActivity.toString(status)
                                    ));
                                }
                            }
                        }
                );
    }

    protected void onReceive(Endpoint endpoint, Payload payload) {}

    protected abstract String getServiceId();

    protected abstract String getName();

    protected String[] getRequiredPermissions() {
        return REQUIRED_PERMISSIONS;
    }

    protected static class Endpoint {
        @NonNull private final String id;
        @NonNull private final String name;

        private Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null && obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint {id=%s, name=%s}", id, name);
        }
    }

    public static boolean hasPermissions(Context context, String... permisions) {
        for (String permission : permisions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                ? status.getStatusMessage()
                : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }

    @CallSuper
    protected void logD(String msg) {
        Log.d(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg) {
        Log.w(TAG, msg);
    }
}
