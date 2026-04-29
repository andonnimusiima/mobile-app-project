package com.example.ohms.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ohms.R;
import com.example.ohms.databinding.ActivityFeaturesBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FeaturesActivity extends AppCompatActivity {
    private ActivityFeaturesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeaturesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_features);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MarketingActivity.class));
                return true;
            } else if (id == R.id.nav_features) {
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            }
            return false;
        });
    }
}
