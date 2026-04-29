package com.example.ohms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ohms.R;
import com.example.ohms.databinding.ActivityMarketingBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MarketingActivity extends AppCompatActivity {
    private ActivityMarketingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences("OHMS_PREFS", MODE_PRIVATE);
        String userEmail = prefs.getString("CURRENT_USER_EMAIL", null);
        
        // We only redirect if we're not explicitly coming back to marketing from nav
        if (userEmail != null && !getIntent().getBooleanExtra("FROM_NAV", false)) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        binding = ActivityMarketingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Lottie animation is handled automatically via XML attributes (autoPlay, loop, url)
        // You can also change the animation dynamically here:
        // binding.lottieAnimation.setAnimationFromUrl("https://...");

        binding.btnContinue.setOnClickListener(v -> {
            startActivity(new Intent(MarketingActivity.this, LoginActivity.class));
        });

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_features) {
                startActivity(new Intent(this, FeaturesActivity.class));
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            }
            return false;
        });
    }
}
