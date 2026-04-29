package com.example.ohms.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.ohms.model.Loan;
import com.example.ohms.repository.AppRepository;

import java.util.List;

public class LoanViewModel extends AndroidViewModel {
    private final AppRepository repository;

    public LoanViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
    }

    public void applyForLoan(Loan loan) {
        repository.applyForLoan(loan);
    }

    public LiveData<List<Loan>> getUserLoans(String userId) {
        return repository.getUserLoans(userId);
    }

    public LiveData<List<Loan>> getAllLoans() {
        return repository.getAllLoans();
    }

    public LiveData<Loan> getLoanById(String loanId) {
        return repository.getLoan(loanId);
    }

    public void updateLoan(Loan loan) {
        repository.updateLoan(loan);
    }

    public void approveLoan(Loan loan) {
        loan.setStatus("APPROVED");
        repository.updateLoanStatus(loan);
    }

    public void rejectLoan(Loan loan) {
        loan.setStatus("REJECTED");
        repository.updateLoanStatus(loan);
    }
}
