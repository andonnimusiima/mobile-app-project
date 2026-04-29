package com.example.ohms.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.ohms.db.AppDatabase;
import com.example.ohms.db.LoanDao;
import com.example.ohms.db.TransactionDao;
import com.example.ohms.db.UserDao;
import com.example.ohms.model.Loan;
import com.example.ohms.model.Transaction;
import com.example.ohms.model.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class AppRepository {
    private final UserDao userDao;
    private final LoanDao loanDao;
    private final TransactionDao transactionDao;
    private FirebaseFirestore firestore;

    public AppRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        userDao = db.userDao();
        loanDao = db.loanDao();
        transactionDao = db.transactionDao();
        try {
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e("AppRepository", "Firebase not initialized: " + e.getMessage());
            firestore = null;
        }
    }

    // User operations
    public void insertUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            userDao.insertUser(user);
            if (firestore != null) {
                try {
                    firestore.collection("users").document(user.getId()).set(user);
                } catch (Exception e) {
                    // Ignore Firestore errors
                }
            }
        });
    }

    public LiveData<User> getUser(String userId) {
        return userDao.getUserById(userId);
    }

    public LiveData<List<User>> getAllBeneficiaries() {
        refreshBeneficiariesFromFirestore();
        return userDao.getAllBeneficiaries();
    }

    public LiveData<User> getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    public LiveData<User> getUserByPhone(String phone) {
        return userDao.getUserByPhone(phone);
    }

    // Loan operations
    public void applyForLoan(Loan loan) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            loanDao.insertLoan(loan);
            if (firestore != null) {
                try {
                    firestore.collection("loans").document(loan.getId()).set(loan);
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    public LiveData<List<Loan>> getUserLoans(String userId) {
        refreshLoansFromFirestore(userId);
        return loanDao.getLoansByUserId(userId);
    }

    public LiveData<List<Loan>> getAllLoans() {
        refreshAllLoansFromFirestore();
        return loanDao.getAllLoans();
    }

    public LiveData<Loan> getLoan(String loanId) {
        return loanDao.getLoanById(loanId);
    }

    public void updateLoan(Loan loan) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            loanDao.updateLoan(loan);
            if (firestore != null) {
                try {
                    firestore.collection("loans").document(loan.getId()).set(loan);
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    public void updateLoanStatus(Loan loan) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // If approved, set the approved date
            if ("APPROVED".equals(loan.getStatus()) && loan.getApprovedDate() == 0) {
                loan.setApprovedDate(System.currentTimeMillis());
            }

            loanDao.updateLoan(loan);
            if (firestore != null) {
                try {
                    firestore.collection("loans").document(loan.getId()).set(loan);
                } catch (Exception e) {
                    // Ignore
                }
            }
            
            // If approved, simulate disbursement
            if ("APPROVED".equals(loan.getStatus())) {
                Transaction transaction = new Transaction(loan.getUserId(), loan.getId(), loan.getAmount(), "DISBURSEMENT");
                insertTransaction(transaction);
            }
        });
    }

    // Transaction operations
    public void insertTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            transactionDao.insertTransaction(transaction);
            if (firestore != null) {
                try {
                    firestore.collection("transactions").document(transaction.getId()).set(transaction);
                } catch (Exception e) {
                    // Ignore
                }
            }
        });
    }

    public LiveData<List<Transaction>> getUserTransactions(String userId) {
        refreshTransactionsFromFirestore(userId);
        return transactionDao.getTransactionsByUserId(userId);
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        refreshAllTransactionsFromFirestore();
        return transactionDao.getAllTransactions();
    }

    private void refreshBeneficiariesFromFirestore() {
        if (firestore == null) return;
        try {
            firestore.collection("users")
                    .whereEqualTo("role", "BENEFICIARY")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                User user = document.toObject(User.class);
                                userDao.insertUser(user);
                            }
                        });
                    });
        } catch (Exception e) {
            // Ignore
        }
    }

    private void refreshLoansFromFirestore(String userId) {
        if (firestore == null) return;
        try {
            firestore.collection("loans")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Loan loan = document.toObject(Loan.class);
                                loanDao.insertLoan(loan);
                            }
                        });
                    });
        } catch (Exception e) {
            // Ignore
        }
    }

    private void refreshAllLoansFromFirestore() {
        if (firestore == null) return;
        try {
            firestore.collection("loans")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Loan loan = document.toObject(Loan.class);
                                loanDao.insertLoan(loan);
                            }
                        });
                    });
        } catch (Exception e) {
            // Ignore
        }
    }

    private void refreshTransactionsFromFirestore(String userId) {
        if (firestore == null) return;
        try {
            firestore.collection("transactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Transaction transaction = document.toObject(Transaction.class);
                                transactionDao.insertTransaction(transaction);
                            }
                        });
                    });
        } catch (Exception e) {
            // Ignore
        }
    }

    private void refreshAllTransactionsFromFirestore() {
        if (firestore == null) return;
        try {
            firestore.collection("transactions")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                Transaction transaction = document.toObject(Transaction.class);
                                transactionDao.insertTransaction(transaction);
                            }
                        });
                    });
        } catch (Exception e) {
            // Ignore
        }
    }
}
