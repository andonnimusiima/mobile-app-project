package com.example.ohms.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Entity(tableName = "loans")
public class Loan {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private double amount;
    private double interestRate;
    private int durationMonths; // in months
    private String status; // "PENDING", "APPROVED", "REJECTED", "PAID"
    private long requestDate;
    private double paidAmount;
    private long approvedDate;
    private String loanType;

    // Additional Application Form Fields
    private String applicantFullName;
    private String applicantAddress;
    private String applicantPhone;
    private String applicantNIN; // National ID Number
    private String guarantorName;
    private String guarantorPhone;
    private boolean isElectronicallySigned;

    public Loan() {
        this.id = UUID.randomUUID().toString();
        this.requestDate = System.currentTimeMillis();
        this.status = "PENDING";
        this.paidAmount = 0.0;
    }

    @Ignore
    public Loan(String userId, double amount, double interestRate, int durationMonths) {
        this();
        this.userId = userId;
        this.amount = amount;
        this.interestRate = interestRate;
        this.durationMonths = durationMonths;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getInterestRate() { return interestRate; }
    public void setInterestRate(double interestRate) { this.interestRate = interestRate; }

    public int getDurationMonths() { return durationMonths; }
    public void setDurationMonths(int durationMonths) { this.durationMonths = durationMonths; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getRequestDate() { return requestDate; }
    public void setRequestDate(long requestDate) { this.requestDate = requestDate; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public long getApprovedDate() { return approvedDate; }
    public void setApprovedDate(long approvedDate) { this.approvedDate = approvedDate; }

    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }

    public String getApplicantFullName() { return applicantFullName; }
    public void setApplicantFullName(String applicantFullName) { this.applicantFullName = applicantFullName; }

    public String getApplicantAddress() { return applicantAddress; }
    public void setApplicantAddress(String applicantAddress) { this.applicantAddress = applicantAddress; }

    public String getApplicantPhone() { return applicantPhone; }
    public void setApplicantPhone(String applicantPhone) { this.applicantPhone = applicantPhone; }

    public String getApplicantNIN() { return applicantNIN; }
    public void setApplicantNIN(String applicantNIN) { this.applicantNIN = applicantNIN; }

    public String getGuarantorName() { return guarantorName; }
    public void setGuarantorName(String guarantorName) { this.guarantorName = guarantorName; }

    public String getGuarantorPhone() { return guarantorPhone; }
    public void setGuarantorPhone(String guarantorPhone) { this.guarantorPhone = guarantorPhone; }

    public boolean isElectronicallySigned() { return isElectronicallySigned; }
    public void setElectronicallySigned(boolean electronicallySigned) { isElectronicallySigned = electronicallySigned; }

    public double getOfficeCharges() {
        if (amount >= 100000 && amount <= 250000) return 10000.0;
        if (amount > 250000 && amount <= 450000) return 15000.0;
        if (amount >= 500000 && amount <= 600000) return 20000.0;
        if (amount > 600000 && amount <= 700000) return 25000.0;
        if (amount >= 800000 && amount <= 900000) return 30000.0;
        if (amount >= 1000000) return 40000.0;
        return 0;
    }

    public double getNetAmountReceived() {
        return amount - getOfficeCharges();
    }

    public double getTotalRepayment() {
        double baseTotal = amount + (amount * interestRate / 100);
        return baseTotal + calculateLateFees() + calculateRenewalInterest();
    }

    public double getRemainingBalance() {
        return getTotalRepayment() - paidAmount;
    }

    public double calculateLateFees() {
        if (approvedDate == 0 || "PAID".equals(status)) return 0;
        
        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - approvedDate;
        long daysSinceApproved = TimeUnit.MILLISECONDS.toDays(diffInMillis);
        
        if (daysSinceApproved > 2) {
            return daysSinceApproved * 5000.0;
        }
        return 0;
    }

    public double calculateRenewalInterest() {
        if (approvedDate == 0 || "PAID".equals(status)) return 0;

        long currentTime = System.currentTimeMillis();
        long diffInMillis = currentTime - approvedDate;
        long daysSinceApproved = TimeUnit.MILLISECONDS.toDays(diffInMillis);

        if (daysSinceApproved > 30) {
            double baseTotal = amount + (amount * interestRate / 100);
            double remainingAt30Days = baseTotal - paidAmount;
            if (remainingAt30Days > 0) {
                return remainingAt30Days * 0.10; // 10% Renewal Interest
            }
        }
        return 0;
    }
}
