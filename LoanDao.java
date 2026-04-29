package com.example.ohms.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ohms.model.Loan;

import java.util.List;

@Dao
public interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLoan(Loan loan);

    @Update
    void updateLoan(Loan loan);

    @Query("SELECT * FROM loans WHERE userId = :userId ORDER BY requestDate DESC")
    LiveData<List<Loan>> getLoansByUserId(String userId);

    @Query("SELECT * FROM loans ORDER BY requestDate DESC")
    LiveData<List<Loan>> getAllLoans();

    @Query("SELECT * FROM loans WHERE id = :loanId")
    LiveData<Loan> getLoanById(String loanId);
}
