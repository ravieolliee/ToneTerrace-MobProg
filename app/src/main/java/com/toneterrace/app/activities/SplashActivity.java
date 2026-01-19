package com.toneterrace.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.toneterrace.app.MainActivity;
import com.toneterrace.app.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1. Setup the delay (3 seconds = 3000 milliseconds)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserSession();
            }
        }, 3000);
    }

    private void checkUserSession() {
        // 2. Check if user is logged in via Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            // User is logged in -> Go to Home
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // User is NOT logged in -> Go to Login
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        // 3. Start the new activity
        startActivity(intent);

        // 4. Close SplashActivity so user can't go back to it by pressing "Back"
        finish();
    }
}