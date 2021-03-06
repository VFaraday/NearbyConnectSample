package com.vfaraday.nearbyconnectsample.chatconnection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.vfaraday.nearbyconnectsample.R;

import java.text.SimpleDateFormat;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private Context mContext;
    private List<Message> mMessageList;

    public ChatMessageAdapter(Context context, List<Message> messageList) {
        mContext = context;
        mMessageList = messageList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new ChatMessageAdapter.SensMessageHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ChatMessageAdapter.ReceivedMessageHolder(view);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        Message userMessage = mMessageList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ChatMessageAdapter.ReceivedMessageHolder) holder).bind(userMessage);
                break;
            case VIEW_TYPE_MESSAGE_SENT:
                ((ChatMessageAdapter.SensMessageHolder) holder).bind(userMessage);
                break;
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mMessageList.get(position).isSender()) {
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
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

        void bind(Message message) {
            messageText.setText(message.getMessage());
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf =
                    new SimpleDateFormat("h:mm a");
            String dateString = sdf.format(message.getCreateAt());
            timeText.setText(dateString);
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

        void bind(Message message) {
            messageText.setText(message.getMessage());
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf =
                    new SimpleDateFormat("h:mm a");
            String dateString = sdf.format(message.getCreateAt());
            timeText.setText(dateString);
        }
    }
}
