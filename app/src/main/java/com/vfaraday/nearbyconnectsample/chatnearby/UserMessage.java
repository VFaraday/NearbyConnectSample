package com.vfaraday.nearbyconnectsample.chatnearby;

import com.google.android.gms.nearby.messages.Message;
import com.google.gson.Gson;

public class UserMessage {

    private final static Gson gson = new Gson();

    private String message;
    private String createAt;
    private String nickname;
    private boolean sender;

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

    public boolean isSender() {
        return sender;
    }

    public void setSender(boolean sender) {
        this.sender = sender;
    }

    public static Message newNearbyMessage(UserMessage message) {
        return new Message(gson.toJson(message).getBytes());
    }

    public static UserMessage fromNearbyMessage(Message message) {
        String nearbyMessageString = new String(message.getContent()).trim();
        return gson.fromJson(
                (new String(nearbyMessageString.getBytes())), UserMessage.class);
    }


}
