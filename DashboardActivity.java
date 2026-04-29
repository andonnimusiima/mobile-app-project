package com.example.ohms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ohms.R;
import com.example.ohms.databinding.ActivityDashboardBinding;
import com.example.ohms.model.Loan;
import com.example.ohms.model.Transaction;
import com.example.ohms.model.User;
import com.example.ohms.repository.AppRepository;
import com.example.ohms.service.MomoService;
import com.example.ohms.viewmodel.AuthViewModel;
import com.example.ohms.viewmodel.LoanViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity implements LoanAdapter.OnLoanActionListener, TransactionAdapter.OnTransactionClickListener {
    private ActivityDashboardBinding binding;
    private AuthViewModel authViewModel;
    private LoanViewModel loanViewModel;
    private LoanAdapter loanAdapter;
    private TransactionAdapter fullHistoryAdapter;
    private TransactionAdapter dailyRepaymentAdapter;
    private User currentUser;
    private AppRepository repository;
    private Loan selectedLoanForRepay;
    private final double INTEREST_RATE = 5.0;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
    private List<Loan> userLoans = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        loanViewModel = new ViewModelProvider(this).get(LoanViewModel.class);
        repository = new AppRepository(getApplication());

        binding.swipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary);
        binding.swipeRefresh.setOnRefreshListener(this::refreshData);

        SharedPreferences prefs = getSharedPreferences("OHMS_PREFS", MODE_PRIVATE);
        String email = prefs.getString("CURRENT_USER_EMAIL", null);

        if (email == null) {
            navigateToMarketing();
            return;
        }

        authViewModel.getUserByEmail(email).observe(this, user -> {
            if (user != null) {
                currentUser = user;
                setupUI();
            } else {
                navigateToMarketing();
            }
        });

        setupLogic();
        setupBottomNavigation();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.layoutDashboard.getVisibility() != View.VISIBLE) {
                    showDashboard();
                } else {
                    navigateToMarketing();
                }
            }
        });
    }

    private void setupBottomNavigation() {
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
                startActivity(new Intent(this, LoginActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupLogic() {
        binding.chipLoans.setOnClickListener(v -> {
            binding.layoutLoansSection.setVisibility(View.VISIBLE);
            binding.rvTransactions.setVisibility(View.GONE);
        });
        binding.chipTransactions.setOnClickListener(v -> {
            binding.layoutLoansSection.setVisibility(View.GONE);
            binding.rvTransactions.setVisibility(View.VISIBLE);
        });

        binding.fabApply.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoanApplicationActivity.class);
            startActivity(intent);
        });
        
        binding.btnBackFromRepay.setOnClickListener(v -> showDashboard());
        binding.btnConfirmPay.setOnClickListener(v -> initiateMoMoRepayment());
        binding.btnPrintReceipt.setOnClickListener(v -> generatePdfReceipt());

        binding.cardEmergency.setOnClickListener(v -> openApplicationWithLoanType("Emergency Loan"));
        binding.cardBodaBoda.setOnClickListener(v -> openApplicationWithLoanType("BodaBoda Loan"));
        binding.cardSchoolFees.setOnClickListener(v -> openApplicationWithLoanType("School Fees Loan"));
        binding.cardBusinessBoost.setOnClickListener(v -> openApplicationWithLoanType("Business Boost Loan"));

        binding.cardTopUp.setOnClickListener(v -> {
            double totalOutstanding = 0;
            for (Loan l : userLoans) {
                if (!"PAID".equals(l.getStatus()) && !"REJECTED".equals(l.getStatus())) {
                    totalOutstanding += l.getRemainingBalance();
                }
            }
            if (totalOutstanding < 100000) {
                openApplicationWithLoanType("Loan Top-Up");
            } else {
                Toast.makeText(this, "Top-Up only allowed for outstanding balance < 100,000 UGX", Toast.LENGTH_LONG).show();
            }
        });

        binding.cardQuickCash.setOnClickListener(v -> {
            boolean hasOldLoan = false;
            for (Loan l : userLoans) {
                if ("PAID".equals(l.getStatus())) {
                    hasOldLoan = true;
                    break;
                }
            }
            if (hasOldLoan) {
                openApplicationWithLoanType("Quick Cash");
            } else {
                Toast.makeText(this, "Quick Cash only available for old customers with paid loans", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openApplicationWithLoanType(String type) {
        Intent intent = new Intent(this, LoanApplicationActivity.class);
        intent.putExtra("SELECTED_LOAN_TYPE", type);
        startActivity(intent);
    }

    private void refreshData() {
        if (currentUser != null) {
            binding.swipeRefresh.setRefreshing(true);
            setupUI();
            binding.swipeRefresh.postDelayed(() -> binding.swipeRefresh.setRefreshing(false), 1000);
        } else {
            binding.swipeRefresh.setRefreshing(false);
        }
    }

    private void navigateToMarketing() {
        SharedPreferences prefs = getSharedPreferences("OHMS_PREFS", MODE_PRIVATE);
        prefs.edit().remove("CURRENT_USER_EMAIL").apply();
        Intent intent = new Intent(this, MarketingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupUI() {
        binding.tvWelcome.setText("Welcome, " + currentUser.getName());
        binding.tvRole.setText(currentUser.getRole());

        boolean isAdmin = "ADMIN".equals(currentUser.getRole());
        
        loanAdapter = new LoanAdapter(isAdmin, this);
        binding.rvLoans.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLoans.setAdapter(loanAdapter);

        fullHistoryAdapter = new TransactionAdapter(this);
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTransactions.setAdapter(fullHistoryAdapter);

        dailyRepaymentAdapter = new TransactionAdapter(this);
        binding.rvDailyRepayments.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDailyRepayments.setAdapter(dailyRepaymentAdapter);

        if (isAdmin) {
            loanViewModel.getAllLoans().observe(this, loans -> loanAdapter.setLoans(loans));
            binding.fabApply.setVisibility(View.GONE);
            binding.chipGroup.setVisibility(View.GONE);
            binding.tvDailyHistoryHeader.setVisibility(View.GONE);
            binding.rvDailyRepayments.setVisibility(View.GONE);
            binding.layoutLoanTypes.setVisibility(View.GONE);
        } else {
            binding.layoutLoanTypes.setVisibility(View.VISIBLE);
            loanViewModel.getUserLoans(currentUser.getId()).observe(this, loans -> {
                userLoans = loans;
                loanAdapter.setLoans(loans);
            });
            repository.getUserTransactions(currentUser.getId()).observe(this, transactions -> {
                if (transactions != null) {
                    fullHistoryAdapter.setTransactions(transactions);
                    
                    List<Transaction> daily = new ArrayList<>();
                    long now = System.currentTimeMillis();
                    long oneDayAgo = now - (24 * 60 * 60 * 1000);
                    
                    for (Transaction t : transactions) {
                        if ("REPAYMENT".equals(t.getType()) && t.getTimestamp() >= oneDayAgo) {
                            daily.add(t);
                        }
                    }
                    dailyRepaymentAdapter.setTransactions(daily);
                    
                    if (daily.isEmpty()) {
                        binding.tvDailyHistoryHeader.setVisibility(View.GONE);
                        binding.rvDailyRepayments.setVisibility(View.GONE);
                    } else {
                        binding.tvDailyHistoryHeader.setVisibility(View.VISIBLE);
                        binding.rvDailyRepayments.setVisibility(View.VISIBLE);
                    }
                }
            });
            binding.fabApply.setVisibility(View.VISIBLE);
            binding.chipGroup.setVisibility(View.VISIBLE);
        }
    }

    private void showDashboard() {
        binding.swipeRefresh.setEnabled(true);
        binding.layoutDashboard.setVisibility(View.VISIBLE);
        binding.layoutRepayments.setVisibility(View.GONE);
        if (currentUser != null && !"ADMIN".equals(currentUser.getRole())) {
            binding.fabApply.setVisibility(View.VISIBLE);
        }
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("OHMS Financial");
    }

    private void showRepaymentForm(Loan loan) {
        selectedLoanForRepay = loan;
        binding.swipeRefresh.setEnabled(false);
        binding.layoutDashboard.setVisibility(View.GONE);
        binding.layoutRepayments.setVisibility(View.VISIBLE);
        binding.fabApply.setVisibility(View.GONE);
        
        binding.cardReceipt.setVisibility(View.GONE);
        binding.cardProcessing.setVisibility(View.GONE);
        binding.cardPaymentForm.setVisibility(View.VISIBLE);
        binding.etRepayAmount.setText(String.format(Locale.getDefault(), "%.0f", loan.getRemainingBalance()));
        binding.etMoMoPhone.setText(currentUser.getPhoneNumber() != null ? currentUser.getPhoneNumber() : "");
        
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Make Repayment");
    }

    private void initiateMoMoRepayment() {
        String amountStr = binding.etRepayAmount.getText().toString();
        String phone = binding.etMoMoPhone.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty() || phone.length() < 10) {
            Toast.makeText(this, "Please enter valid MoMo number", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount <= 0 || amount > selectedLoanForRepay.getRemainingBalance() + 1.0) {
            Toast.makeText(this, "Invalid Amount", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.cardPaymentForm.setVisibility(View.GONE);
        binding.cardProcessing.setVisibility(View.VISIBLE);

        MomoService.initiateCollection(phone, amount, new MomoService.MomoCallback() {
            @Override
            public void onSuccess(String transactionId) {
                processFinalRepayment(amount);
            }

            @Override
            public void onFailure(String errorMessage) {
                binding.cardProcessing.setVisibility(View.GONE);
                binding.cardPaymentForm.setVisibility(View.VISIBLE);
                Toast.makeText(DashboardActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void processFinalRepayment(double amount) {
        selectedLoanForRepay.setPaidAmount(selectedLoanForRepay.getPaidAmount() + amount);
        if (selectedLoanForRepay.getRemainingBalance() <= 1.0) {
            selectedLoanForRepay.setStatus("PAID");
        }
        loanViewModel.updateLoan(selectedLoanForRepay);

        Transaction transaction = new Transaction(currentUser.getId(), selectedLoanForRepay.getId(), amount, "REPAYMENT");
        repository.insertTransaction(transaction);

        binding.cardProcessing.setVisibility(View.GONE);
        binding.cardReceipt.setVisibility(View.VISIBLE);
        binding.tvReceiptId.setText(transaction.getId().substring(0, 8).toUpperCase());
        binding.tvReceiptDate.setText(dateFormat.format(new Date(transaction.getTimestamp())));
        binding.tvReceiptAmount.setText(String.format(Locale.getDefault(), "UGX %,.0f", amount));

        Toast.makeText(this, "Payment Collected to Admin Account " + MomoService.ADMIN_MOMO_NUMBER, Toast.LENGTH_LONG).show();
    }

    private void generatePdfReceipt() {
        PdfDocument document = new PdfDocument();
        View view = binding.receiptContent;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(view.getWidth(), view.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        view.draw(canvas);
        document.finishPage(page);

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Receipt_" + System.currentTimeMillis() + ".pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Receipt saved: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving receipt", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            refreshData();
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
            navigateToMarketing();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
    }

    @Override
    public void onApprove(Loan loan) {
        loanViewModel.approveLoan(loan);
        Toast.makeText(this, "Loan Approved", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReject(Loan loan) {
        loanViewModel.rejectLoan(loan);
        Toast.makeText(this, "Loan Rejected", Toast.LENGTH_SHORT).show();
    }

    public void onRepayClick(Loan loan) {
        showRepaymentForm(loan);
    }
}
