package com.vfaraday.nearbyconnectsample.chatconnection;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.vfaraday.nearbyconnectsample.P2PStarConnectionActivity;

class ForegroundService extends Service{


    @Override
    public void onCreate() {
        Log.w("NearbySample", "onCreate: ");
        P2PStarConnectionActivity.getGoogleApiClient().connect();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("NearbySample", "onStartCommand: " + P2PStarConnectionActivity.getGoogleApiClient().isConnected());
        return START_STICKY;

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
