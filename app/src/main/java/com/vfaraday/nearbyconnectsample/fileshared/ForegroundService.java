package com.vfaraday.nearbyconnectsample.fileShared;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vfaraday.nearbyconnectsample.P2PStarConnectionActivity;

public class ForegroundService extends Service {

    public static boolean IS_SERVICE_RUNNING = false;

    private static final int ONGOING_NOTIFICATION_ID = 101;

    @Override
    public void onCreate() {
        Log.w("NearbySample", "onCreate: ");
        P2PStarConnectionActivity.getGoogleApiClient().connect();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        showNotification();
        Log.w("NearbySample", "onStartCommand: " + P2PStarConnectionActivity.getGoogleApiClient().isConnected());
        return START_STICKY;

    }

    private void showNotification() {
        Notification.Builder builder = new Notification.Builder(this);
        Notification myNotification;
        builder.setAutoCancel(false);
        builder.setTicker("this is ticker text");
        builder.setContentTitle("WhatsApp Notification");
        builder.setContentText("You have a new message");
        builder.setOngoing(true);
        builder.setSubText("This is subtext...");   //API level 16
        builder.setNumber(100);
        builder.build();

        myNotification = builder.getNotification();
        startForeground(ONGOING_NOTIFICATION_ID, myNotification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (P2PStarConnectionActivity.getGoogleApiClient().isConnected()
            && P2PStarConnectionActivity.getGoogleApiClient() != null) {
            P2PStarConnectionActivity.getGoogleApiClient().disconnect();
        }
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
