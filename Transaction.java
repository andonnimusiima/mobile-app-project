package com.example.ohms.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "transactions")
public class Transaction {
    @PrimaryKey
    @NonNull
    private String id;
    private String userId;
    private String loanId;
    private double amount;
    private String type; // "DISBURSEMENT", "REPAYMENT"
    private long timestamp;

    public Transaction() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    @Ignore
    public Transaction(String userId, String loanId, double amount, String type) {
        this();
        this.userId = userId;
        this.loanId = loanId;
        this.amount = amount;
        this.type = type;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLoanId() { return loanId; }
    public void setLoanId(String loanId) { this.loanId = loanId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
