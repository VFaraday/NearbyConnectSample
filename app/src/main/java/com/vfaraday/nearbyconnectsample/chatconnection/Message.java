package com.vfaraday.nearbyconnectsample.chatconnection;

import com.google.gson.Gson;
import com.vfaraday.nearbyconnectsample.chatnearby.UserMessage;


class Message {

    private final static Gson gson = new Gson();

    private String message;
    private long createAt;
    private String nickname;
    private boolean sender;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getCreateAt() {
        return createAt;
    }

    public void setCreateAt(long createAt) {
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

    public static byte [] newNearbyMessage(Message message) {
        return gson.toJson(message).getBytes();
    }

    public static Message fromNearbyMessage(byte[] message) {
        return gson.fromJson((new String(message)), Message.class);
    }
}
