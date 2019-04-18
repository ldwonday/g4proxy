package com.virjar.g4proxy.android;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.virjar.g4proxy.R;

public class MainActivity extends AppCompatActivity {


    public LocationManager locationManager;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.text);
        String clientKey = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        textView.setText("clientId: " + clientKey);
        startService(new Intent(this, HttpProxyService.class));

    }

}
