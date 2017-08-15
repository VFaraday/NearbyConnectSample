package com.vfaraday.nearbyconnectsample.chatnearby;

import com.google.android.gms.nearby.messages.Message;
import com.google.gson.Gson;

import java.nio.charset.Charset;

public class UserMessage {

    private final static Gson gson = new Gson();

    private String message;
    private String createAt;
    private String nickname;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public static Message newNearbyMessage(UserMessage message) {
        return new Message(gson.toJson(message).getBytes(Charset.forName("UTF-8")));
    }

    public static UserMessage fromNearbyMessage(Message message) {
        String nearbyMessageString = new String(message.getContent()).trim();
        return gson.fromJson(
                (new String(nearbyMessageString.getBytes(Charset.forName("UTF-8")))),
                UserMessage.class);
    }
}
