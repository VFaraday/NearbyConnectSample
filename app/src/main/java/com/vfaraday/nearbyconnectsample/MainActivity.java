package com.vfaraday.nearbyconnectsample;

import android.os.Bundle;
import android.support.annotation.Nullable;


public class MainActivity extends ConnectionActivity {

    private static final String SERVIVE_ID =
            "com.vfaraday.nerbyconectionsample.SERVICE_ID";

    private final String mName = "VFARADY";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected String getServiceId() {
        return SERVIVE_ID;
    }

    @Override
    protected String getName() {
        return mName;
    }
}
