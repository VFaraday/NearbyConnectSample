package com.vfaraday.nearbyconnectsample.chatnearby;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import java.util.List;

public class BackgroundSubscribeService extends IntentService {

    private static final int MESSAGES_NOTIFICATION_ID = 1;
    private static final int NUM_MESSAGES_NOTIFICATION = 5;

    public BackgroundSubscribeService() {
        super("BackgroundSubscribeService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        updateNotification();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            Nearby.Messages.handleIntent(intent, new MessageListener() {

                @Override
                public void onFound(Message message) {
                    Utils.saveFoundMessages(getApplicationContext(), message);
                    updateNotification();
                }

                @Override
                public void onLost(Message message) {
                    super.onLost(message);
                    Utils.removeLostMessage(getApplicationContext(), message);
                    updateNotification();
                }
            });
        }
    }

    private void updateNotification() {
        List<String> messages = Utils.getCachedMessages(getApplicationContext());
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent launchIntent = new Intent(getApplicationContext(), ChatActivity.class);
        launchIntent.setAction(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String contentTitle = getContentTitle(messages);
        String contentText = getContentText(messages);

        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setOngoing(true)
                .setContentIntent(pi);

        manager.notify(MESSAGES_NOTIFICATION_ID, nBuilder.build());
    }

    private String getContentTitle(List<String> messages) {
        switch (messages.size()) {
            case 0:
                return "Scanning";
            case 1:
                return "One messages";
            default:
                return "e";
        }
    }

    private String getContentText(List<String> messages) {
        String newLine = System.getProperty("line.separator");
        if (messages.size() < NUM_MESSAGES_NOTIFICATION) {
            return TextUtils.join(newLine, messages);
        }
        return TextUtils.join(newLine, messages.subList(0, NUM_MESSAGES_NOTIFICATION)) + newLine + "";
    }
}
