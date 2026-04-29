package com.example.ohms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ohms.databinding.ActivityRegisterBinding;
import com.example.ohms.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private AuthViewModel authViewModel;
    private int titleClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        observeAuth();
    }

    private void setupListeners() {
        // Hidden "Easter Egg" to show Admin Code field: Tap "Create Account" title 5 times
        binding.tvRegTitle.setOnClickListener(v -> {
            titleClickCount++;
            if (titleClickCount >= 5) {
                binding.tilAdminCode.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Admin mode enabled", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnFinalRegister.setOnClickListener(v -> {
            String name = binding.etRegName.getText().toString().trim();
            String email = binding.etRegEmail.getText().toString().trim();
            String phone = binding.etRegPhone.getText().toString().trim();
            String address = binding.etRegAddress.getText().toString().trim();
            String nin = binding.etRegNIN.getText().toString().trim();
            String password = binding.etRegPassword.getText().toString().trim();
            String adminCode = binding.etAdminCode.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || nin.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show();
                return;
            }

            if (phone.length() < 10) {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check for admin secret code
            String role = "BENEFICIARY";
            if ("OHMS-ADMIN-2024".equals(adminCode)) {
                role = "ADMIN";
            } else if (!adminCode.isEmpty()) {
                Toast.makeText(this, "Invalid Admin Code", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(name, email, password, phone, address, nin, role);
        });

        binding.tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void observeAuth() {
        authViewModel.getErrorLiveData().observe(this, error -> {
            if (error != null) {
                if ("REGISTERED_LOCALLY".equals(error)) {
                    authViewModel.clearError();
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    
                    SharedPreferences prefs = getSharedPreferences("OHMS_PREFS", MODE_PRIVATE);
                    prefs.edit().putString("CURRENT_USER_EMAIL", binding.etRegEmail.getText().toString()).apply();
                    
                    // Check if it's admin or user to redirect correctly
                    authViewModel.getUserByEmail(binding.etRegEmail.getText().toString()).observe(this, user -> {
                        if (user != null) {
                            Intent intent;
                            if ("ADMIN".equals(user.getRole())) {
                                intent = new Intent(this, AdminDashboardActivity.class);
                            } else {
                                intent = new Intent(this, DashboardActivity.class);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    });
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    authViewModel.clearError();
                }
            }
        });
    }
}
