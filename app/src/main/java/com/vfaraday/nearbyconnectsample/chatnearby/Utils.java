package com.vfaraday.nearbyconnectsample.chatnearby;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.android.gms.nearby.messages.Message;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

    static final String KEY_CACHED_MESSAGES = "cached-messages";

    static List<UserMessage> getCachedMessages(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        String cachedMessagesJson = sharedPreferences.getString(KEY_CACHED_MESSAGES, "");
        if (TextUtils.isEmpty(cachedMessagesJson)) {
            return Collections.emptyList();
        } else {
            Type type = new TypeToken<UserMessage>() {}.getType();
            return new Gson().fromJson(cachedMessagesJson, type);
        }
    }

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

    static void removeLostMessage(Context context, Message message) {
        ArrayList<UserMessage> cachedMessages = new ArrayList<>(getCachedMessages(context));
        cachedMessages.remove(UserMessage.fromNearbyMessage(message));
        getSharedPreference(context)
                .edit()
                .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                .apply();
    }

    static SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(
                context.getApplicationContext().getPackageName(),
                Context.MODE_PRIVATE);
    }
}
