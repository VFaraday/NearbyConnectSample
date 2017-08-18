package com.vfaraday.nearbyconnectsample.chatconnection;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;
import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.P2PStarConnectionActivity;
import com.vfaraday.nearbyconnectsample.R;
import com.vfaraday.nearbyconnectsample.databinding.ActivityChatConnectionBinding;

import java.util.ArrayList;
import java.util.Collections;

public class ChatConnectionActivity extends P2PStarConnectionActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    private ArrayList<Message> messagesList = new ArrayList<>();

    private ActivityChatConnectionBinding layout;

    private Message mMessage;
    private ChatMessageAdapter mChatMessageAdapter;

    private GoogleApiClient mGoogleApiClient;

    private static final String SERVICE_ID =
            "com.vfaraday.nerbyconectionsample.SERVICE_ID";

    private String mName = BluetoothAdapter.getDefaultAdapter().getName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.activity_chat_connection);

        Intent service = new Intent(this, ForegroundService.class);
        startService(service);

        mMessage = new Message();
        mMessage.setNickname(mName);

        mChatMessageAdapter = new ChatMessageAdapter(this, messagesList);
        layout.reyclerviewMessageList.setLayoutManager(new LinearLayoutManager(this));
        layout.reyclerviewMessageList.setAdapter(mChatMessageAdapter);

        mGoogleApiClient = getGoogleApiClient();

        RxView.clicks(layout.btnSendMessage)
                .subscribe(v -> {
                    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                        if (String.valueOf(layout.edittextChatbox.getText()).equals("")) {
                            return;
                        }
                        long date = System.currentTimeMillis();
                        mMessage.setMessage(String.valueOf(layout.edittextChatbox.getText()));
                        mMessage.setCreateAt(date);
                        mMessage.setSender(true);
                        send(Payload.fromBytes(Message.newNearbyMessage(mMessage)));
                        layout.edittextChatbox.setText("");
                        layout.reyclerviewMessageList
                                .scrollToPosition(mChatMessageAdapter.getItemCount() - 1);
                    }

                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        getGoogleApiClient().disconnect();
        logW(String.valueOf(getGoogleApiClient().isConnected()));
        logW("OnStop");
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!getGoogleApiClient().isConnected()) {
            getGoogleApiClient().connect();
            logW("connect");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        if (payload.getType() == Payload.Type.BYTES) {
            UtilsConnection.saveFoundMessages(this, payload.asBytes(), true);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(key, UtilsConnection.KEY_CACHED_MESSAGES)) {
            messagesList.clear();
            messagesList.addAll(UtilsConnection.getCachedMessages(this));
            Collections.sort(messagesList, (message, t1) ->
                    message.getCreateAt() > t1.getCreateAt() ? 1 : -1);
            mChatMessageAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startAdvertising(Strategy.P2P_CLUSTER);
        startDiscovered(Strategy.P2P_CLUSTER);
    }

    @Override
    public void onConnectionSuspended(int reason) {
        super.onConnectionSuspended(reason);
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
    }

    @Override
    protected void onEndpointDisconnected(P2PStarConnectionActivity.Endpoint endpoint) {
        // to our initial state (discovering).
        logW("we lost all our endpoints");

    }

    protected void onConnectionFailed(P2PStarConnectionActivity.Endpoint endpoint) {
    }

    @Override
    protected String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    protected String getName() {
        return mName;
    }
}
