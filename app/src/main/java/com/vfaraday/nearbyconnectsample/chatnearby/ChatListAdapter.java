package com.vfaraday.nearbyconnectsample.chatnearby;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vfaraday.nearbyconnectsample.R;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private Context mContext;
    private List<UserMessage> mMessageList;

    public ChatListAdapter(Context context, List<UserMessage> messageList) {
        mContext = context;
        mMessageList = messageList;
    }

    public void add(UserMessage message) {
        mMessageList.add(message);
    }

    public void remove(UserMessage message) {
        mMessageList.remove(message);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {

        TextView messageText, timeText, nameText;
        ImageView profileImage;

        public ReceivedMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
            nameText = itemView.findViewById(R.id.text_message_name);

            profileImage = itemView.findViewById(R.id.image_message_profile);
        }

        void bind(UserMessage message) {
            messageText.setText(message.getMessage());
            timeText.setText((int) message.getCreateAt());
            nameText.setText(message.getNickname());
        }
    }

    private class SensMessageHolder extends RecyclerView.ViewHolder {

        TextView messageText, timeText;

        public SensMessageHolder(View itemView) {
            super(itemView);

            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }

        void bind(UserMessage message) {
            messageText.setText(message.getMessage());
            timeText.setText((int) message.getCreateAt());
        }
    }
}
