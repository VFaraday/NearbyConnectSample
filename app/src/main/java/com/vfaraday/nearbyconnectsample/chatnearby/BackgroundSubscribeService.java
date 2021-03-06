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
import com.vfaraday.nearbyconnectsample.R;

import java.util.List;

/**
 * While subscribed in the background, this service shows a persistent notification with the
 * current set of messages from nearby beacons. Nearby launches this service when a message is
 * found or lost, and this service updates the notification, then stops itself.
*/
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
    public void onDestroy() {

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            Nearby.Messages.handleIntent(intent, new MessageListener() {

                @Override
                public void onFound(Message message) {
                    Utils.saveFoundMessages(getApplicationContext(), message, false);
                    updateNotification();
                }

                @Override
                public void onLost(Message message) {
                    Utils.removeLostMessage(getApplicationContext(), message);
                    updateNotification();
                }
            });
        }
    }

    private void updateNotification() {
        List<UserMessage> messages = Utils.getCachedMessages(getApplicationContext());
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pi);

        manager.notify(MESSAGES_NOTIFICATION_ID, nBuilder.build());
    }

    private String getContentTitle(List<UserMessage> messages) {
        switch (messages.size()) {
            case 0:
                return "Scanning";
            case 1:
                return "One messages";
            default:
                return "messages: " + messages.size();
        }
    }

    private String getContentText(List<UserMessage> messages) {
        String newLine = System.getProperty("line.separator");
        if (messages.size() < NUM_MESSAGES_NOTIFICATION) {
            return TextUtils.join(newLine, messages);
        }
        return messages.get(messages.size()-1).getMessage();
    }
}
