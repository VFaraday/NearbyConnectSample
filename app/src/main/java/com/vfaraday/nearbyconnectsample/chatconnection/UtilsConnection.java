package com.vfaraday.nearbyconnectsample.chatconnection;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class UtilsConnection {
    static final String KEY_CACHED_MESSAGES = "connection-cached-messages";

    /**
     * Fetches message userMessage stored in {@link SharedPreferences}.
     *
     * @param context The context.
     * @return  A list (possibly empty) containing message UserMessage.
     */
    static List<Message> getCachedMessages(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        String cachedMessagesJson = sharedPreferences.getString(KEY_CACHED_MESSAGES, "");
        if (TextUtils.isEmpty(cachedMessagesJson)) {
            return Collections.emptyList();
        } else {
            Type type = new TypeToken<List<Message>>() {}.getType();
            return new Gson().fromJson(cachedMessagesJson, type);
        }
    }

    /**
     * Saves a message UserMessage to {@link SharedPreferences}.
     *
     * @param context The context.
     * @param message The Message whose payload (as string) is saved to SharedPreferences.
     * @param sender The boolean
     */
    static void saveFoundMessages(Context context, byte[] message, boolean sender) {
        ArrayList<Message> cachedMessages = new ArrayList<>(getCachedMessages(context));
        Set<Message> cachedMessagesSet = new HashSet<>(cachedMessages);
        Message userMessage = Message.fromNearbyMessage(message);
        if (!cachedMessagesSet.contains(userMessage)) {
            if (!sender) userMessage.setSender(false);
            cachedMessages.add(0, userMessage);
            getSharedPreference(context)
                    .edit()
                    .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                    .apply();
        }
    }

    /**
     * Removes a userMessage from {@link SharedPreferences}.
     * @param context The context.
     * @param message The Message whose payload (as string) is removed from SharedPreferences.
     */
    static void removeLostMessage(Context context, byte[] message) {
        ArrayList<Message> cachedMessages = new ArrayList<>(getCachedMessages(context));
        cachedMessages.remove(Message.fromNearbyMessage(message));
        getSharedPreference(context)
                .edit()
                .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                .apply();
    }

    static void clearCache(Context context) {
        ArrayList<Message> cachedMessages = new ArrayList<>();
        getSharedPreference(context)
                .edit()
                .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                .apply();
    }

    /**
     * Gets the SharedPReferences object that is used for persisting data in this application.
     *
     * @param context The context.
     * @return The single {@link SharedPreferences} instance that can be used to retrieve and modify
     *         values.
     */
    static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(
                context.getApplicationContext().getPackageName(),
                Context.MODE_PRIVATE);
    }
}
