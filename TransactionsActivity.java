package com.example.ohms.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ohms.databinding.ActivityTransactionsBinding;
import com.example.ohms.model.Transaction;
import com.example.ohms.repository.AppRepository;
import com.google.firebase.auth.FirebaseAuth;

public class TransactionsActivity extends AppCompatActivity implements TransactionAdapter.OnTransactionClickListener {
    private ActivityTransactionsBinding binding;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTransactionsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new TransactionAdapter(this);
        binding.rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvTransactions.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            // Reusing a ViewModel or repository to fetch transactions
            AppRepository repository = new AppRepository(getApplication());
            repository.getUserTransactions(uid).observe(this, transactions -> {
                adapter.setTransactions(transactions);
            });
        }
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        // Handle transaction click if necessary
    }
}
