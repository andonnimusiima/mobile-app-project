package com.example.ohms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ohms.R;
import com.example.ohms.databinding.ActivityLoginBinding;
import com.example.ohms.model.User;
import com.example.ohms.viewmodel.AuthViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnLogin.setOnClickListener(v -> {
            String identifier = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();
            if (!identifier.isEmpty() && !password.isEmpty()) {
                authViewModel.login(identifier, password);
            } else {
                Toast.makeText(this, "Please enter email/phone and password", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        authViewModel.getErrorLiveData().observe(this, error -> {
            if (error != null) {
                if ("LOGGED_IN_LOCALLY".equals(error)) {
                    authViewModel.clearError();
                    handleLocalLogin();
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    if (!error.equals("LOGGED_IN_LOCALLY")) {
                        authViewModel.clearError();
                    }
                }
            }
        });

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_account);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                Intent intent = new Intent(this, MarketingActivity.class);
                intent.putExtra("FROM_NAV", true);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_features) {
                startActivity(new Intent(this, FeaturesActivity.class));
                return true;
            } else if (id == R.id.nav_account) {
                return true;
            }
            return false;
        });
    }

    private void handleLocalLogin() {
        String identifier = binding.etEmail.getText().toString().trim();
        
        SharedPreferences prefs = getSharedPreferences("OHMS_PREFS", MODE_PRIVATE);
        prefs.edit().putString("CURRENT_USER_EMAIL", identifier).apply();
        
        authViewModel.getUserByIdentifier(identifier).observe(this, user -> {
            if (user != null) {
                Intent intent;
                if ("ADMIN".equals(user.getRole())) {
                    intent = new Intent(this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(this, DashboardActivity.class);
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
