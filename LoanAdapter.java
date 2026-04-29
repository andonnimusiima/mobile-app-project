package com.example.ohms.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ohms.R;
import com.example.ohms.databinding.ItemLoanBinding;
import com.example.ohms.model.Loan;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class LoanAdapter extends RecyclerView.Adapter<LoanAdapter.LoanViewHolder> {
    private List<Loan> loans = new ArrayList<>();
    private final boolean isAdmin;
    private final OnLoanActionListener listener;

    public interface OnLoanActionListener {
        void onApprove(Loan loan);
        void onReject(Loan loan);
    }

    public LoanAdapter(boolean isAdmin, OnLoanActionListener listener) {
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void setLoans(List<Loan> loans) {
        this.loans = loans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LoanViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoanBinding binding = ItemLoanBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new LoanViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull LoanViewHolder holder, int position) {
        Loan loan = loans.get(position);
        holder.binding.tvLoanId.setText("ID: " + (loan.getId().length() > 8 ? loan.getId().substring(0, 8) : loan.getId()));
        holder.binding.tvLoanAmount.setText(String.format(Locale.getDefault(), "Principal: UGX %,.0f", loan.getAmount()));
        holder.binding.tvLoanStatus.setText(loan.getStatus());
        
        // Detailed Tracking
        holder.binding.tvTotalDue.setText(String.format(Locale.getDefault(), "UGX %,.0f", loan.getTotalRepayment()));
        holder.binding.tvPaidAmount.setText(String.format(Locale.getDefault(), "UGX %,.0f", loan.getPaidAmount()));
        holder.binding.tvRemainingBalance.setText(String.format(Locale.getDefault(), "UGX %,.0f", loan.getRemainingBalance()));

        // Days Remaining Logic
        if (loan.getApprovedDate() > 0 && !"PAID".equals(loan.getStatus())) {
            long currentTime = System.currentTimeMillis();
            long diffInMillis = currentTime - loan.getApprovedDate();
            long daysPassed = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            long daysRemaining = 30 - daysPassed;
            
            holder.binding.tvDaysRemaining.setVisibility(View.VISIBLE);
            if (daysRemaining < 0) {
                holder.binding.tvDaysRemaining.setText("Overdue by " + Math.abs(daysRemaining) + " days");
                holder.binding.tvDaysRemaining.setTextColor(holder.itemView.getContext().getColor(R.color.error));
            } else {
                holder.binding.tvDaysRemaining.setText("Days Remaining: " + daysRemaining);
                holder.binding.tvDaysRemaining.setTextColor(holder.itemView.getContext().getColor(R.color.secondary));
            }
        } else {
            holder.binding.tvDaysRemaining.setVisibility(View.GONE);
        }

        // Display Loan Type
        String type = loan.getLoanType();
        holder.binding.tvLoanType.setText(type != null ? type : "General Loan");

        // Office Charges & Net Received
        holder.binding.tvOfficeCharges.setText(String.format(Locale.getDefault(), "Charges: UGX %,.0f", loan.getOfficeCharges()));
        holder.binding.tvNetReceived.setText(String.format(Locale.getDefault(), "Net Recv: UGX %,.0f", loan.getNetAmountReceived()));

        String status = loan.getStatus() != null ? loan.getStatus() : "PENDING";
        
        // Reset visibilities
        holder.binding.loanProgress.setVisibility(View.GONE);
        holder.binding.tvRepaymentDaily.setVisibility(View.GONE);
        holder.binding.userActions.setVisibility(View.GONE);
        holder.binding.adminActions.setVisibility(View.GONE);
        holder.binding.adminDivider.setVisibility(View.GONE);

        switch (status) {
            case "APPROVED":
                holder.binding.tvLoanStatus.setBackgroundResource(R.drawable.bg_status_approved);
                holder.binding.loanProgress.setVisibility(View.VISIBLE);
                holder.binding.tvRepaymentDaily.setVisibility(View.VISIBLE);
                
                int progress = (int) ((loan.getPaidAmount() / loan.getTotalRepayment()) * 100);
                holder.binding.loanProgress.setProgress(progress);
                
                double daily = loan.getTotalRepayment() / 30;
                holder.binding.tvRepaymentDaily.setText(String.format(Locale.getDefault(), "Daily Due: UGX %,.0f", daily));
                
                if (!isAdmin && loan.getRemainingBalance() > 0.01) {
                    holder.binding.userActions.setVisibility(View.VISIBLE);
                    holder.binding.btnRepay.setOnClickListener(v -> {
                        if (holder.itemView.getContext() instanceof DashboardActivity) {
                            ((DashboardActivity) holder.itemView.getContext()).onRepayClick(loan);
                        }
                    });
                    holder.binding.btnSchedule.setOnClickListener(v -> showScheduleDialog(holder.itemView.getContext(), loan));
                }
                break;
                
            case "PAID":
                holder.binding.tvLoanStatus.setBackgroundResource(R.drawable.bg_status_approved);
                holder.binding.tvLoanStatus.setText("FULLY PAID");
                holder.binding.loanProgress.setVisibility(View.VISIBLE);
                holder.binding.loanProgress.setProgress(100);
                break;
                
            case "REJECTED":
                holder.binding.tvLoanStatus.setBackgroundResource(R.drawable.bg_status_rejected);
                break;
                
            default: // PENDING
                holder.binding.tvLoanStatus.setBackgroundResource(R.drawable.bg_status_pending);
                if (isAdmin) {
                    holder.binding.adminDivider.setVisibility(View.VISIBLE);
                    holder.binding.adminActions.setVisibility(View.VISIBLE);
                    holder.binding.btnApprove.setOnClickListener(v -> listener.onApprove(loan));
                    holder.binding.btnReject.setOnClickListener(v -> listener.onReject(loan));
                }
                break;
        }
    }

    private void showScheduleDialog(android.content.Context context, Loan loan) {
        double total = loan.getTotalRepayment();
        double daily = total / 30;
        
        StringBuilder schedule = new StringBuilder();
        schedule.append("Total Repayment: UGX ").append(String.format(Locale.getDefault(), "%,.0f", total)).append("\n\n");
        schedule.append("Daily Installments (30 Days):\n");
        
        for (int i = 1; i <= 30; i++) {
            schedule.append("Day ").append(i).append(": UGX ").append(String.format(Locale.getDefault(), "%,.0f", daily)).append("\n");
        }

        new AlertDialog.Builder(context)
                .setTitle("Repayment Schedule")
                .setMessage(schedule.toString())
                .setPositiveButton("CLOSE", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return loans.size();
    }

    static class LoanViewHolder extends RecyclerView.ViewHolder {
        final ItemLoanBinding binding;
        LoanViewHolder(ItemLoanBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
