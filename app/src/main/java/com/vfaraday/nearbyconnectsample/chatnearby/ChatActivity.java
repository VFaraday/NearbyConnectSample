package com.vfaraday.nearbyconnectsample.chatnearby;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessagesOptions;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.NearbyPermissions;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.R;
import com.vfaraday.nearbyconnectsample.databinding.ChatMessageActivityBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
      GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "NearbyBLe";

    private static final int PERMISSION_REQUEST_CODE = 1111;

    private static final String KEY_SUBSCRIBE = "subscribed";

    private GoogleApiClient mGoogleApiClient;
    private Message mMessage;

    private ChatMessageActivityBinding layout;

    private List<String> mFoundMessageList;
    private ChatListAdapter mChatListAdapter;
    private UserMessage mUserMessage;

    private boolean mSubscribed;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.chat_message_activity);

        mUserMessage = new UserMessage();
        mUserMessage.setNickname(BluetoothAdapter.getDefaultAdapter().getName());
        final List<String> cachedMessages = Utils.getCachedMessages(this);
        if (cachedMessages != null) {
            mFoundMessageList.addAll(cachedMessages);
        }

        //mChatListAdapter = new ChatListAdapter(this, mFoundMessageList);


        if (havePermission()) {
            buildGoogleApiClient();
        } else {
            requestPermission();
        }

        if (savedInstanceState != null) {
            mSubscribed = savedInstanceState.getBoolean(KEY_SUBSCRIBE, false);
        }

        RxView.clicks(layout.buttonChatboxSend)
                .subscribe(v -> {
                    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                        long date = System.currentTimeMillis();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf =
                                new SimpleDateFormat("MMM MM dd, yyyy h:mm a");
                        String dateString = sdf.format(date);
                        mUserMessage.setMessage(String.valueOf(layout.edittextChatbox.getText()));
                        mUserMessage.setCreateAt(dateString);
                        mMessage = new Message(getByteMessage(mUserMessage));
                        publish();
                        layout.edittextChatbox.setText("");
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        getSharedPreferences(getApplicationContext().getPackageName(), Context.MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(this);

    }

    private byte [] getByteMessage(UserMessage message) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] byteMessage = {};
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(message);
            out.flush();
            byteMessage = bos.toByteArray();
        } catch (IOException e) {
            e.getMessage();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                ex.getMessage();
            }
        }
        return byteMessage;
    }

    private boolean havePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
    }

    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Nearby.MESSAGES_API, new MessagesOptions.Builder()
                    .setPermissions(NearbyPermissions.BLE)
                    .build())
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .build();
        }
    }

    private void subscribe() {
        if (mSubscribed) {
            Log.i(TAG, "Already Subscribe");
            return;
        }

        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .build();

        Nearby.Messages.subscribe(mGoogleApiClient, getPendingIntent(), options)
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Subscribe success");
                        startService(getBackgroundSubscribeService());
                    } else {
                        Log.e(TAG, "Operation failed. Error - " +
                                NearbyMessagesStatusCodes.getStatusCodeString(
                                        status.getStatusCode()
                                ));
                    }
                });
    }

    private void publish() {
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(Strategy.BLE_ONLY)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");
                        runOnUiThread(() -> Log.i(TAG, "No longer publishing"));
                    }
                })
                .build();

        Nearby.Messages.publish(mGoogleApiClient, mMessage, options)
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Published successfully.");
                    } else {
                        Log.e(TAG, "Could not publish, status = " + status);
                    }
                });
    }

    private PendingIntent getPendingIntent() {
        return PendingIntent.getService(this, 0,
                getBackgroundSubscribeService(), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private Intent getBackgroundSubscribeService() {
        return new Intent(this, BackgroundSubscribeService.class);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (TextUtils.equals(key, Utils.KEY_CACHED_MESSAGES)) {
            mFoundMessageList.clear();
            mFoundMessageList.addAll(Utils.getCachedMessages(this));

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        // Nearby.Messages.subscribe(...) requires a connected GoogleApiClient. For that reason,
        // we subscribe only once we have confirmation that GoogleApiClient is connected.
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
