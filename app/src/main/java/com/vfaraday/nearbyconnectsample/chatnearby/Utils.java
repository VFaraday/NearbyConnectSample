package com.vfaraday.nearbyconnectsample.chatnearby;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.android.gms.nearby.messages.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

    static final String KEY_CACHED_MESSAGES = "cached-messages";

    /**
     * Fetches message userMessage stored in {@link SharedPreferences}.
     *
     * @param context The context.
     * @return  A list (possibly empty) containing message UserMessage.
     */
    static List<UserMessage> getCachedMessages(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        String cachedMessagesJson = sharedPreferences.getString(KEY_CACHED_MESSAGES, "");
        if (TextUtils.isEmpty(cachedMessagesJson)) {
            return Collections.emptyList();
        } else {
            Type type = new TypeToken<List<UserMessage>>() {}.getType();
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
    static void saveFoundMessages(Context context, Message message, boolean sender) {
        ArrayList<UserMessage> cachedMessages = new ArrayList<>(getCachedMessages(context));
        Set<UserMessage> cachedMessagesSet = new HashSet<>(cachedMessages);
        UserMessage userMessage = UserMessage.fromNearbyMessage(message);
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
    static void removeLostMessage(Context context, Message message) {
        ArrayList<UserMessage> cachedMessages = new ArrayList<>(getCachedMessages(context));
        cachedMessages.remove(UserMessage.fromNearbyMessage(message));
        getSharedPreference(context)
                .edit()
                .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                .apply();
    }

    static void clearCache(Context context) {
        ArrayList<UserMessage> cachedMessages = new ArrayList<>();
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
