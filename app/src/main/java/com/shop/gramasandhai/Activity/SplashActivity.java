package com.shop.gramasandhai.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.shop.gramasandhai.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
//        ImageView gifImageView = findViewById(R.id.gifImageView);
//        Glide.with(this)
//                .asGif()
//                .load(R.drawable.logo) // Replace with your GIF resource
//                .transition(DrawableTransitionOptions.withCrossFade())
//                .into(gifImageView);

        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

            Intent intent;
            if (isLoggedIn) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 3000);
    }
}