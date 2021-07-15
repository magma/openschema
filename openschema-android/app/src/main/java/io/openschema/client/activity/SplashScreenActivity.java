package io.openschema.client.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Empty activity that loads the splash screen theme on a cold start. It will
 * redirect the user to an actual Activity once the app loads.
 */
public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, OnboardingActivity.class));
        finish();
    }
}
