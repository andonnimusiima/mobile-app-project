package com.example.ohms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ohms.R;
import com.example.ohms.databinding.ActivityAdminDashboardBinding;
import com.example.ohms.databinding.ItemLoanAdminBinding;
import com.example.ohms.model.Loan;
import com.example.ohms.model.Transaction;
import com.example.ohms.model.User;
import com.example.ohms.repository.AppRepository;
import com.example.ohms.viewmodel.AuthViewModel;
import com.example.ohms.viewmodel.LoanViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AdminDashboardActivity extends AppCompatActivity implements LoanAdapter.OnLoanActionListener, TransactionAdapter.OnTransactionClickListener, UserAdapter.OnUserClickListener {
    private ActivityAdminDashboardBinding binding;
    private LoanViewModel loanViewModel;
    private AuthViewModel authViewModel;
    private AppRepository repository;
    private AdminLoanAdapter pendingAdapter;
    private LoanAdapter allLoansAdapter;
    private TransactionAdapter transactionAdapter;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        loanViewModel = new ViewModelProvider(this).get(LoanViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        repository = new AppRepository(getApplication());
        
        setupRecyclerViews();
        setupTabs();
        observeData();

        binding.btnGenerateReport.setOnClickListener(v -> generateReport());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                navigateToMarketing();
            }
        });
    }

    private void setupRecyclerViews() {
        // Pending Loans Adapter
        pendingAdapter = new AdminLoanAdapter(new ArrayList<>(), loan -> {
            loanViewModel.approveLoan(loan);
            Toast.makeText(this, "Loan Approved", Toast.LENGTH_SHORT).show();
        }, loan -> {
            loanViewModel.rejectLoan(loan);
            Toast.makeText(this, "Loan Rejected", Toast.LENGTH_SHORT).show();
        });
        binding.rvPendingLoans.setLayoutManager(new LinearLayoutManager(this));
        binding.rvPendingLoans.setAdapter(pendingAdapter);

        // All Loans Adapter
        allLoansAdapter = new LoanAdapter(true, this);
        binding.rvAllLoans.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAllLoans.setAdapter(allLoansAdapter);

        // Transactions Adapter
        transactionAdapter = new TransactionAdapter(this);
        binding.rvAllTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAllTransactions.setAdapter(transactionAdapter);

        // Clients Adapter
        userAdapter = new UserAdapter(this);
        binding.rvClients.setLayoutManager(new LinearLayoutManager(this));
        binding.rvClients.setAdapter(userAdapter);
    }

    private void setupTabs() {
        binding.chipPending.setOnClickListener(v -> {
            binding.layoutPending.setVisibility(View.VISIBLE);
            binding.layoutAllLoans.setVisibility(View.GONE);
            binding.layoutRepayments.setVisibility(View.GONE);
            binding.layoutClients.setVisibility(View.GONE);
        });

        binding.chipActiveLoans.setOnClickListener(v -> {
            binding.layoutPending.setVisibility(View.GONE);
            binding.layoutAllLoans.setVisibility(View.VISIBLE);
            binding.layoutRepayments.setVisibility(View.GONE);
            binding.layoutClients.setVisibility(View.GONE);
        });

        binding.chipClients.setOnClickListener(v -> {
            binding.layoutPending.setVisibility(View.GONE);
            binding.layoutAllLoans.setVisibility(View.GONE);
            binding.layoutRepayments.setVisibility(View.GONE);
            binding.layoutClients.setVisibility(View.VISIBLE);
        });

        binding.chipRepayments.setOnClickListener(v -> {
            binding.layoutPending.setVisibility(View.GONE);
            binding.layoutAllLoans.setVisibility(View.GONE);
            binding.layoutRepayments.setVisibility(View.VISIBLE);
            binding.layoutClients.setVisibility(View.GONE);
        });
    }

    private void observeData() {
        // Observe Loans
        loanViewModel.getAllLoans().observe(this, allLoans -> {
            if (allLoans != null) {
                double totalDisbursed = 0;
                List<Loan> pendingLoans = new ArrayList<>();
                
                for (Loan loan : allLoans) {
                    if ("APPROVED".equals(loan.getStatus()) || "PAID".equals(loan.getStatus())) {
                        totalDisbursed += loan.getAmount();
                    }
                    if ("PENDING".equals(loan.getStatus())) {
                        pendingLoans.add(loan);
                    }
                }

                binding.tvTotalDisbursed.setText(String.format(Locale.getDefault(), "UGX %,.0f", totalDisbursed));
                binding.tvPendingCount.setText(String.valueOf(pendingLoans.size()));
                
                pendingAdapter.updateLoans(pendingLoans);
                allLoansAdapter.setLoans(allLoans);
            }
        });

        // Observe Transactions
        repository.getAllTransactions().observe(this, transactions -> {
            if (transactions != null) {
                transactionAdapter.setTransactions(transactions);
            }
        });

        // Observe Clients
        repository.getAllBeneficiaries().observe(this, users -> {
            if (users != null) {
                userAdapter.setUsers(users);
            }
        });
    }

    private void generateReport() {
        // Logic for "Who has not made a repayment" report
        loanViewModel.getAllLoans().observe(this, loans -> {
            repository.getAllTransactions().observe(this, transactions -> {
                if (loans == null || transactions == null) return;

                long oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                Set<String> usersWhoPaidToday = new HashSet<>();
                for (Transaction t : transactions) {
                    if ("REPAYMENT".equals(t.getType()) && t.getTimestamp() >= oneDayAgo) {
                        usersWhoPaidToday.add(t.getUserId());
                    }
                }

                StringBuilder report = new StringBuilder("--- NON-PAYMENT REPORT (Last 24h) ---\n\n");
                boolean foundDefaulters = false;

                for (Loan loan : loans) {
                    if ("APPROVED".equals(loan.getStatus()) && !usersWhoPaidToday.contains(loan.getUserId())) {
                        report.append("Client: ").append(loan.getApplicantFullName())
                              .append("\nPhone: ").append(loan.getApplicantPhone())
                              .append("\nLoan Balance: UGX ").append(String.format(Locale.getDefault(), "%,.0f", loan.getRemainingBalance()))
                              .append("\n------------------------\n");
                        foundDefaulters = true;
                    }
                }

                if (!foundDefaulters) report.append("All active clients have made repayments today!");

                // For simplicity, showing in a Toast or Dialog
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Daily Collection Tracking")
                        .setMessage(report.toString())
                        .setPositiveButton("OK", null)
                        .show();
            });
        });
    }

    private void navigateToMarketing() {
        SharedPreferences prefs = getSharedPreferences("OHMS_PREFS", MODE_PRIVATE);
        prefs.edit().remove("CURRENT_USER_EMAIL").apply();
        Intent intent = new Intent(this, MarketingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            navigateToMarketing();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onApprove(Loan loan) {
        loanViewModel.approveLoan(loan);
    }

    @Override
    public void onReject(Loan loan) {
        loanViewModel.rejectLoan(loan);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        // Detail view for transaction
    }

    @Override
    public void onUserClick(User user) {
        // Show only this user's loans
        loanViewModel.getUserLoans(user.getId()).observe(this, loans -> {
            allLoansAdapter.setLoans(loans);
            binding.chipActiveLoans.performClick();
            Toast.makeText(this, "Showing loans for " + user.getName(), Toast.LENGTH_SHORT).show();
        });
    }

    private static class AdminLoanAdapter extends RecyclerView.Adapter<AdminLoanAdapter.ViewHolder> {
        private final List<Loan> loans;
        private final OnLoanActionListener approveListener;
        private final OnLoanActionListener rejectListener;

        interface OnLoanActionListener {
            void onAction(Loan loan);
        }

        AdminLoanAdapter(List<Loan> loans, OnLoanActionListener approve, OnLoanActionListener reject) {
            this.loans = loans;
            this.approveListener = approve;
            this.rejectListener = reject;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemLoanAdminBinding binding = ItemLoanAdminBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Loan loan = loans.get(position);
            
            // Applicant Details
            String name = loan.getApplicantFullName() != null ? loan.getApplicantFullName() : "Unknown User";
            holder.binding.tvUserName.setText(name);
            holder.binding.tvLoanAmount.setText(String.format(Locale.getDefault(), "UGX %,.0f", loan.getAmount()));
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            holder.binding.tvDate.setText("Requested: " + sdf.format(new Date(loan.getRequestDate())));

            holder.binding.tvApplicantAddress.setText("Address: " + (loan.getApplicantAddress() != null ? loan.getApplicantAddress() : "N/A"));
            holder.binding.tvApplicantPhone.setText("Phone: " + (loan.getApplicantPhone() != null ? loan.getApplicantPhone() : "N/A"));

            // Guarantor Details
            holder.binding.tvGuarantorName.setText("Name: " + (loan.getGuarantorName() != null ? loan.getGuarantorName() : "N/A"));
            holder.binding.tvGuarantorPhone.setText("Phone: " + (loan.getGuarantorPhone() != null ? loan.getGuarantorPhone() : "N/A"));

            // Signature Status
            if (loan.isElectronicallySigned()) {
                holder.binding.tvSignatureStatus.setText("✓ Electronically Signed");
                holder.binding.tvSignatureStatus.setVisibility(View.VISIBLE);
            } else {
                holder.binding.tvSignatureStatus.setVisibility(View.GONE);
            }

            holder.binding.btnApprove.setOnClickListener(v -> approveListener.onAction(loan));
            holder.binding.btnReject.setOnClickListener(v -> rejectListener.onAction(loan));
        }

        @Override
        public int getItemCount() { return loans.size(); }

        void updateLoans(List<Loan> newLoans) {
            this.loans.clear();
            this.loans.addAll(newLoans);
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ItemLoanAdminBinding binding;
            ViewHolder(ItemLoanAdminBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
