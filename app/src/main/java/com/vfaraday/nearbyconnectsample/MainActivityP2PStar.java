package com.vfaraday.nearbyconnectsample;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.databinding.P2pStarActivityMainBinding;


public class MainActivityP2PStar extends P2PStarConnectionActivity {

    private static final String SERVIVE_ID =
            "com.vfaraday.nerbyconectionsample.SERVICE_ID";

    private final String mName = "VFARADY";

    P2pStarActivityMainBinding layout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.p2p_star_activity_main);

        RxView.clicks(layout.btnStartAdvertise)
                .subscribe(v -> startAdvertising());

        RxView.clicks(layout.btnStopAdvertise)
                .subscribe(v -> stopAdvertising());

        RxView.clicks(layout.btnStartDiscover)
                .subscribe(v -> startDiscovered());

        RxView.clicks(layout.btnStopDiscover)
                .subscribe(v -> stopDiscovering());
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
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected String getServiceId() {
        return SERVIVE_ID;
    }

    @Override
    protected String getName() {
        return mName;
    }
}
