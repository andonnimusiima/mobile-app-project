package com.example.ohms.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ohms.R;
import com.example.ohms.databinding.ActivityLoanApplicationBinding;
import com.example.ohms.model.Loan;
import com.example.ohms.model.User;
import com.example.ohms.viewmodel.AuthViewModel;
import com.example.ohms.viewmodel.LoanViewModel;

import java.util.Locale;

public class LoanApplicationActivity extends AppCompatActivity {
    private ActivityLoanApplicationBinding binding;
    private LoanViewModel loanViewModel;
    private AuthViewModel authViewModel;
    private User currentUser;
    
    private final double INTEREST_RATE = 15.0;
    private final int DURATION_DAYS = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoanApplicationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Loan Application Form");
        }

        loanViewModel = new ViewModelProvider(this).get(LoanViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnSubmitApplication.setEnabled(false);

        String selectedType = getIntent().getStringExtra("SELECTED_LOAN_TYPE");
        if (selectedType != null) {
            binding.etLoanType.setText(selectedType);
        }

        SharedPreferences prefs = getSharedPreferences("OHMS_PREFS", MODE_PRIVATE);
        String email = prefs.getString("CURRENT_USER_EMAIL", null);

        if (email != null) {
            authViewModel.getUserByEmail(email).observe(this, user -> {
                if (user != null) {
                    currentUser = user;
                    binding.etFullName.setText(user.getName());
                    binding.etPhone.setText(user.getPhoneNumber());
                    validateForm();
                }
            });
        }

        setupWatchers();

        binding.btnSubmitApplication.setOnClickListener(v -> initiateMoMoDisbursement());
    }

    private void setupWatchers() {
        TextWatcher commonWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateRepayment();
                validateForm();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        binding.etFullName.addTextChangedListener(commonWatcher);
        binding.etAddress.addTextChangedListener(commonWatcher);
        binding.etPhone.addTextChangedListener(commonWatcher);
        binding.etAmount.addTextChangedListener(commonWatcher);
        binding.etGuarantor1.addTextChangedListener(commonWatcher);
        binding.etGuarantor1Phone.addTextChangedListener(commonWatcher);
        binding.etNIN.addTextChangedListener(commonWatcher);
        binding.cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> validateForm());
    }

    private void validateForm() {
        boolean isPersonalFilled = !binding.etFullName.getText().toString().isEmpty() 
                && !binding.etAddress.getText().toString().isEmpty()
                && !binding.etPhone.getText().toString().isEmpty();
        
        boolean isLoanFilled = !binding.etAmount.getText().toString().isEmpty();
        
        boolean isGuarantorFilled = !binding.etGuarantor1.getText().toString().isEmpty()
                && !binding.etGuarantor1Phone.getText().toString().isEmpty();
        
        boolean isNINFilled = !binding.etNIN.getText().toString().isEmpty() 
                && binding.etNIN.getText().toString().length() >= 8;
        
        boolean isSigned = binding.cbAgree.isChecked();
        
        binding.btnSubmitApplication.setEnabled(isPersonalFilled && isLoanFilled && isGuarantorFilled && isNINFilled && isSigned && currentUser != null);
    }

    private void calculateRepayment() {
        try {
            String amountStr = binding.etAmount.getText().toString();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                
                Loan tempLoan = new Loan();
                tempLoan.setAmount(amount);
                tempLoan.setInterestRate(INTEREST_RATE);
                
                double officeCharges = tempLoan.getOfficeCharges();
                double netReceived = tempLoan.getNetAmountReceived();
                double totalToRepay = tempLoan.getTotalRepayment();
                double dailyRepayment = totalToRepay / DURATION_DAYS;
                
                binding.tvOfficeCharges.setText(String.format(Locale.getDefault(), "Office Charges: UGX %,.0f", officeCharges));
                binding.tvNetReceived.setText(String.format(Locale.getDefault(), "Net Amount to Receive: UGX %,.0f", netReceived));
                binding.tvDailyRepayment.setText(String.format(Locale.getDefault(), "Daily Repayment: UGX %,.0f", dailyRepayment));
                binding.tvTotalRepayment.setText(String.format(Locale.getDefault(), "Total Repayment: UGX %,.0f", totalToRepay));
            } else {
                binding.tvOfficeCharges.setText("Office Charges: UGX 0");
                binding.tvNetReceived.setText("Net Amount to Receive: UGX 0");
                binding.tvDailyRepayment.setText("Daily Repayment: UGX 0");
                binding.tvTotalRepayment.setText("Total Repayment: UGX 0");
            }
        } catch (NumberFormatException ignored) {}
    }

    private void initiateMoMoDisbursement() {
        String phone = binding.etPhone.getText().toString();
        if (phone.isEmpty() || phone.length() < 10) {
            Toast.makeText(this, "Please provide a valid MoMo number for disbursement", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmitApplication.setEnabled(false);
        binding.btnSubmitApplication.setText("INITIATING MOMO DISBURSEMENT...");
        
        // Simulate MoMo Disbursement API Call
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            submitApplication();
        }, 3000);
    }

    private void submitApplication() {
        try {
            double amount = Double.parseDouble(binding.etAmount.getText().toString());
            
            Loan loan = new Loan(currentUser.getId(), amount, INTEREST_RATE, 1);
            loan.setLoanType(binding.etLoanType.getText().toString());
            loan.setApplicantFullName(binding.etFullName.getText().toString());
            loan.setApplicantAddress(binding.etAddress.getText().toString());
            loan.setApplicantPhone(binding.etPhone.getText().toString());
            loan.setApplicantNIN(binding.etNIN.getText().toString());
            loan.setGuarantorName(binding.etGuarantor1.getText().toString());
            loan.setGuarantorPhone(binding.etGuarantor1Phone.getText().toString());
            loan.setElectronicallySigned(true);
            
            loanViewModel.applyForLoan(loan);
            
            Toast.makeText(this, "Application Submitted & MoMo Transfer Initiated!", Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Error submitting application", Toast.LENGTH_SHORT).show();
            binding.btnSubmitApplication.setEnabled(true);
            binding.btnSubmitApplication.setText("SUBMIT DIGITAL APPLICATION");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
