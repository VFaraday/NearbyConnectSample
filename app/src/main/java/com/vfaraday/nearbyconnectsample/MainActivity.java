package com.vfaraday.nearbyconnectsample;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.jakewharton.rxbinding2.view.RxView;
import com.vfaraday.nearbyconnectsample.chatconnection.ChatConnectionActivity;
import com.vfaraday.nearbyconnectsample.chatnearby.ChatActivity;
import com.vfaraday.nearbyconnectsample.databinding.ActivityMainBinding;
import com.vfaraday.nearbyconnectsample.fileshared.SendFileActivity;
import com.vfaraday.nearbyconnectsample.messages.MessageActivity;
import com.vfaraday.nearbyconnectsample.stream.StreamActivity;


public class MainActivity extends AppCompatActivity {

    ActivityMainBinding layout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = DataBindingUtil.setContentView(this, R.layout.activity_main);

        RxView.clicks(layout.btnNearbyMessage)
                .subscribe(v -> {
                    Intent intent = new Intent(this, MessageActivity.class);
                    startActivity(intent);
                });

        RxView.clicks(layout.btnNearbyFileShared)
                .subscribe(v -> {
                    Intent intent = new Intent(this, SendFileActivity.class);
                    startActivity(intent);
                });

        RxView.clicks(layout.btnNearbyStream)
                .subscribe(v -> {
                    Intent intent = new Intent(this, StreamActivity.class);
                    startActivity(intent);
                });

        RxView.clicks(layout.btnChat)
                .subscribe(v -> {
                    Intent intent = new Intent(this, ChatActivity.class);
                    startActivity(intent);
                });

        RxView.clicks(layout.btnChatConnection)
                .subscribe(v -> {
                    Intent intent = new Intent(this, ChatConnectionActivity.class);
                    startActivity(intent);
                });
    }
}
