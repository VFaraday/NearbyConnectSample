package com.vfaraday.nearbyconnectsample.chatnearby;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
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

    static List<String> getCachedMessages(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        String cachedMessagesJson = sharedPreferences.getString(KEY_CACHED_MESSAGES, "");
        if (TextUtils.isEmpty(cachedMessagesJson)) {
            return Collections.emptyList();
        } else {
            Type type = new TypeToken<String>() {}.getType();
            return new Gson().fromJson(cachedMessagesJson, type);
        }
    }

    static void saveFoundMessages(Context context, Message message) {
        ArrayList<String> cachedMessages = new ArrayList<>(getCachedMessages(context));
        Set<String> cachedMessagesSet = new HashSet<>(cachedMessages);
        String messageString = new String(message.getContent());
        if (!cachedMessagesSet.contains(messageString)) {
            cachedMessages.add(0, new String(message.getContent()));
            getSharedPreference(context)
                    .edit()
                    .putString(KEY_CACHED_MESSAGES, new Gson().toJson(cachedMessages))
                    .apply();
        }
    }

    static void removeLostMessage(Context context, Message message) {
        ArrayList<String> cachedMessages = new ArrayList<>(getCachedMessages(context));
        cachedMessages.remove(new String(message.getContent()));
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
