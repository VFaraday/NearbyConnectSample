package com.vfaraday.nearbyconnectsample.messages;

import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;
import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.R;
import com.vfaraday.nearbyconnectsample.databinding.ActivityMessageBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessageActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MessageActivity";
    ActivityMessageBinding layout;

    private static final String KEY_UUID = "key_uuid";

    /**
     * Creates a UUID and saves it to {@link android.content.SharedPreferences}. The UUID is added to the published
     * message to avoid it being undelivered due to de-duplication. See {@link DeviceMessage} for
     * details.
     */
    private static String getUUID(SharedPreferences sharedPreferences) {
        String uuid = sharedPreferences.getString(KEY_UUID, "");
        if (TextUtils.isEmpty(uuid)) {
            uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(KEY_UUID, uuid).apply();
        }
        return uuid;
    }

    private GoogleApiClient mGoogleApiClient;
    private Message mMessage;
    private MessageListener mMessageListener;

    private ArrayAdapter<String> mNearbyDeviceAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layout = DataBindingUtil.setContentView(this, R.layout.activity_message);

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                // Called when a new message is found.
                mNearbyDeviceAdapter.add(new String(message.getContent()));
            }

            @Override
            public void onLost(Message message) {
                // Called when a message is no longer detectable nearby.
                mNearbyDeviceAdapter.remove(new String(message.getContent()));
            }
        };

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(layout.edtTxtSmsText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);

        layout.subscribeSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // If GoogleApiClient is connected, perform sub actions in response to user action.
            // If it isn't connected, do nothing, and perform sub actions when it connects (see
            // onConnected()).
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                if (isChecked) {
                    subscribe();
                } else {
                    unSubscribe();
                }
            }
        });

        /*layout.publishSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // If GoogleApiClient is connected, perform pub actions in response to user action.
            // If it isn't connected, do nothing, and perform pub actions when it connects (see
            // onConnected()).
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                if (isChecked) {
                    publish();
                } else {
                    unPublish();
                }
            }
        });*/

        final List<String> nearbyDevicesArrayList = new ArrayList<>();
        mNearbyDeviceAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                nearbyDevicesArrayList);
        if (layout.nearbyDevicesListView != null) {
            layout.nearbyDevicesListView.setAdapter(mNearbyDeviceAdapter);
        }

        RxView.clicks(layout.btnSendSms)
                .subscribe(v -> {
                    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                        mMessage = new Message(String.valueOf(layout.edtTxtSmsText.getText()).getBytes());
                        publish();
                        layout.edtTxtSmsText.setText("");
                    }
                });
        buildGoogleApiClient();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected");
        // We use the Switch buttons in the UI to track whether we were previously doing pub/sub (
        // switch buttons retain state on orientation change). Since the GoogleApiClient disconnects
        // when the activity is destroyed, foreground pubs/subs do not survive device rotation. Once
        // this activity is re-created and GoogleApiClient connects, we check the UI and pub/sub
        /*if (layout.publishSwitch.isChecked()) {
            publish();
        }*/
        if (layout.subscribeSwitch.isChecked()) {
            subscribe();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        logAndShowSnackbar("Connection suspended. Error code: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //layout.publishSwitch.setEnabled(false);
        layout.subscribeSwitch.setEnabled(false);
        logAndShowSnackbar("Exception while connecting to Google Play services: " +
                connectionResult.getErrorMessage());
    }

    /**
     * Builds {@link GoogleApiClient}, enabling automatic lifecycle management using
     * {@link GoogleApiClient.Builder#enableAutoManage(FragmentActivity,
     * int, GoogleApiClient.OnConnectionFailedListener)}.I.e., GoogleApiClient connects in
     * {@link AppCompatActivity#onStart}, or if onStart() has already happened, it connects
     * immediately, and disconnects automatically in {@link AppCompatActivity#onStop}.
     */
    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .build();
    }

    private void subscribe() {
        Log.i(TAG, "Subscribing");
        mNearbyDeviceAdapter.clear();
        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.DEFAULT)
                .setCallback(new SubscribeCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer subscribing");
                        runOnUiThread(() -> layout.subscribeSwitch.setChecked(false));
                    }
                }).build();

        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                .setResultCallback(status -> {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Subscribed successfully.");
                            logAndShowSnackbar("Subscribed successfully. = " + status);
                        } else {
                            logAndShowSnackbar("Could not subscribe, status = " + status);
                            layout.subscribeSwitch.setChecked(false);
                        }
                    });

    }

    /**
     * Publishes a message to nearby devices and updates the UI if the publication either fails or
     * TTLs.
     */
    private void publish() {
        Log.i(TAG, "Publishing");
        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(Strategy.DEFAULT)
                .setCallback(new PublishCallback() {
                    @Override
                    public void onExpired() {
                        super.onExpired();
                        Log.i(TAG, "No longer publishing");
                        runOnUiThread(() -> {
                            Log.i(TAG, "No longer publishing");
                            //layout.publishSwitch.setChecked(false);
                        });
                    }
                })
                .build();

        Nearby.Messages.publish(mGoogleApiClient, mMessage, options)
                .setResultCallback(status -> {
                    if (status.isSuccess()) {
                        Log.i(TAG, "Published successfully.");
                        logAndShowSnackbar("Published successfully. = " + status);
                    } else {
                        logAndShowSnackbar("Could not publish, status = " + status);
                        //layout.publishSwitch.setChecked(false);
                    }
                });
    }

    /**
     * Stops subscribing to messages from nearby devices.
     */
    private void unSubscribe() {
        logAndShowSnackbar("UnSubscribing");
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

    /**
     * Stops publishing message to nearby devices.
     */
    private void unPublish() {
        logAndShowSnackbar("UnPublishing");
        Nearby.Messages.unpublish(mGoogleApiClient, mMessage);
    }

    /**
     * Logs a message and shows a {@link Snackbar} using {@code text};
     *
     * @param text The text used in the Log message and the SnackBar.
     */
    private void logAndShowSnackbar(final String text) {
        Log.w(TAG, text);
        if (layout.messageContainer != null) {
            Snackbar.make(layout.messageContainer, text, Snackbar.LENGTH_LONG).show();
        }
    }
}
