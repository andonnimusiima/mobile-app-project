package com.example.ohms.ui;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ohms.R;
import com.example.ohms.databinding.ActivityRepaymentBinding;
import com.example.ohms.databinding.DialogReceiptBinding;
import com.example.ohms.model.Loan;
import com.example.ohms.model.Transaction;
import com.example.ohms.repository.AppRepository;
import com.example.ohms.viewmodel.LoanViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RepaymentActivity extends AppCompatActivity {
    private ActivityRepaymentBinding binding;
    private LoanViewModel loanViewModel;
    private AppRepository repository;
    private Loan currentLoan;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRepaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loanViewModel = new ViewModelProvider(this).get(LoanViewModel.class);
        repository = new AppRepository(getApplication());

        String loanId = getIntent().getStringExtra("LOAN_ID");
        if (loanId != null) {
            loanViewModel.getLoanById(loanId).observe(this, loan -> {
                if (loan != null) {
                    currentLoan = loan;
                    binding.tvRemainingBalance.setText(String.format(Locale.getDefault(), "$%.2f", loan.getRemainingBalance()));
                }
            });
        }

        binding.btnPay.setOnClickListener(v -> initiateMobileMoneyPayment());
    }

    private void initiateMobileMoneyPayment() {
        String amountStr = binding.etAmount.getText().toString().trim();
        String phoneStr = binding.etPhone.getText().toString().trim();

        if (amountStr.isEmpty() || phoneStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double payAmount = Double.parseDouble(amountStr);
        if (payAmount <= 0 || (currentLoan != null && payAmount > currentLoan.getRemainingBalance() + 0.01)) {
            Toast.makeText(this, "Invalid payment amount", Toast.LENGTH_SHORT).show();
            return;
        }

        String provider = binding.rbMtn.isChecked() ? "MTN MoMo" : "Airtel Money";

        // Simulate Mobile Money Prompt
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirm Payment")
                .setMessage("A " + provider + " prompt has been sent to " + phoneStr + ". Please enter your PIN to authorize $" + amountStr)
                .setPositiveButton("I HAVE PAID", (d, w) -> processRepayment(payAmount))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void processRepayment(double amount) {
        if (currentLoan == null) return;

        // Update Loan
        currentLoan.setPaidAmount(currentLoan.getPaidAmount() + amount);
        if (currentLoan.getRemainingBalance() <= 0.01) {
            currentLoan.setStatus("PAID");
        }
        loanViewModel.updateLoan(currentLoan);

        // Record Transaction
        Transaction transaction = new Transaction(currentLoan.getUserId(), currentLoan.getId(), amount, "REPAYMENT");
        repository.insertTransaction(transaction);

        Toast.makeText(this, "Payment Received Successfully!", Toast.LENGTH_SHORT).show();
        showReceipt(transaction);
    }

    private void showReceipt(Transaction transaction) {
        DialogReceiptBinding receiptBinding = DialogReceiptBinding.inflate(getLayoutInflater());
        receiptBinding.tvReceiptId.setText(transaction.getId().substring(0, 8).toUpperCase());
        receiptBinding.tvReceiptDate.setText(dateFormat.format(new Date(transaction.getTimestamp())));
        receiptBinding.tvReceiptAmount.setText(String.format(Locale.getDefault(), "$%.2f", transaction.getAmount()));
        receiptBinding.tvReceiptBalance.setText(String.format(Locale.getDefault(), "$%.2f", currentLoan.getRemainingBalance()));

        AlertDialog receiptDialog = new AlertDialog.Builder(this)
                .setView(receiptBinding.getRoot())
                .setCancelable(false)
                .create();

        receiptBinding.btnClose.setOnClickListener(v -> {
            receiptDialog.dismiss();
            finish();
        });

        receiptBinding.btnPrint.setOnClickListener(v -> {
            generatePdfReceipt(receiptBinding.receiptContainer, transaction.getId().substring(0, 8));
        });

        receiptDialog.show();
    }

    private void generatePdfReceipt(View view, String id) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(view.getWidth(), view.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        view.draw(canvas);
        document.finishPage(page);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, "Receipt_" + id + ".pdf");

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "Receipt saved to Downloads", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
