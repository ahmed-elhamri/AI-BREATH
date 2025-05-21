package com.example.ai_breath.viewmodel;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.ai_breath.MainActivity;
import com.example.ai_breath.R;

public class SplashScreenActivity extends AppCompatActivity {
    private static final int SPLASH_TIME = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        ImageView logo = findViewById(R.id.logoImage);
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        logo.startAnimation(slideIn);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
            finish();
        }, SPLASH_TIME);
    }
}