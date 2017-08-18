package com.vfaraday.nearbyconnectsample.chatnearby;

import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
      GoogleApiClient.OnConnectionFailedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "NearbyBLe";

    private static final int REQUEST_CODE_REQUIRED_PERMISSION = 1;

    private static final String KEY_SUBSCRIBE = "subscribed";

    private GoogleApiClient mGoogleApiClient;
    private Message mMessage;

    private ChatMessageActivityBinding layout;

    private List<UserMessage> mFoundMessageList;
    private ChatListAdapter mChatListAdapter;
    private UserMessage mUserMessage;

    private boolean mSubscribed = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.chat_message_activity);
        mFoundMessageList = new ArrayList<>();
        mUserMessage = new UserMessage();
        mUserMessage.setNickname(BluetoothAdapter.getDefaultAdapter().getName());
        final List<UserMessage> cachedMessages = Utils.getCachedMessages(this);
        if (cachedMessages != null) {
            mFoundMessageList.addAll(cachedMessages);
            Collections.sort(mFoundMessageList, (message, t1) ->
                    message.getCreateAt() > t1.getCreateAt() ? 1 : -1);
        }

        mChatListAdapter = new ChatListAdapter(this, mFoundMessageList);
        layout.reyclerviewMessageList.setLayoutManager(new LinearLayoutManager(this));
        layout.reyclerviewMessageList.setAdapter(mChatListAdapter);

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
                        if (String.valueOf(layout.edittextChatbox.getText()).equals("")) {
                            return;
                        }
                        long date = System.currentTimeMillis();
                        mUserMessage.setMessage(String.valueOf(layout.edittextChatbox.getText()));
                        mUserMessage.setCreateAt(date);
                        mUserMessage.setSender(true);
                        mMessage = UserMessage.newNearbyMessage(mUserMessage);
                        publish();
                        layout.edittextChatbox.setText("");
                        layout.reyclerviewMessageList
                                .scrollToPosition(mChatListAdapter.getItemCount() - 1);
                    }
                });
        layout.reyclerviewMessageList.scrollToPosition(mChatListAdapter.getItemCount() - 1);
    }

    protected void onStart() {
        super.onStart();

        keyboardIsShowListener();

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_item:
                Utils.clearCache(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SUBSCRIBE, mSubscribed);
    }

    private boolean havePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_REQUIRED_PERMISSION);
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

    /**
     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
     * int, GoogleApiClient.OnConnectionFailedListener)}. I.e., GoogleApiClient connects in
     * {@link AppCompatActivity#onStart} -- or if onStart() has already happened -- it connects
     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
     */
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
                .setStrategy(Strategy.DEFAULT)
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
                        Utils.saveFoundMessages(this, mMessage, true);
                    } else {
                        Log.e(TAG, "Could not publish, status = " + status);
                        Toast.makeText(this, "Send message failed, try again", Toast.LENGTH_LONG).show();
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
            Collections.sort(mFoundMessageList, (message, t1) ->
                    message.getCreateAt() > t1.getCreateAt() ? 1 : -1);
            mChatListAdapter.notifyDataSetChanged();
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
        Log.e(TAG, "Connection failed. Error code: " + connectionResult);
    }

    private void keyboardIsShowListener() {
        layout.getRoot().getRootView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                layout.getRoot().getRootView().getWindowVisibleDisplayFrame(r);
                int screenHeight = layout.activityChat.getRootView().getHeight();

                // r.bottom is the position above soft keypad or device button.
                // if keypad is shown, the r.bottom is smaller than that before.
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) { // 0.15 ratio is perhaps enough to determine keypad height.
                    // keyboard is opened
                    layout.reyclerviewMessageList.scrollToPosition(mChatListAdapter.getItemCount() - 1);
                    layout.getRoot().getRootView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }
}
