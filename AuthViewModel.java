package com.example.ohms.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ohms.db.AppDatabase;
import com.example.ohms.db.UserDao;
import com.example.ohms.model.User;
import com.example.ohms.repository.AppRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.UUID;

public class AuthViewModel extends AndroidViewModel {
    private final AppRepository repository;
    private final UserDao userDao;
    private final FirebaseAuth auth;
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<String> errorLiveData;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = new AppRepository(application);
        userDao = AppDatabase.getDatabase(application).userDao();
        auth = FirebaseAuth.getInstance();
        userLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();

        if (auth.getCurrentUser() != null) {
            userLiveData.setValue(auth.getCurrentUser());
        }
    }

    public LiveData<User> getUserByIdentifier(String identifier) {
        if (identifier == null) return new MutableLiveData<>(null);
        
        if (identifier.contains("@")) {
            return repository.getUserByEmail(identifier);
        } else {
            return repository.getUserByPhone(identifier);
        }
    }

    public LiveData<User> getUserByEmail(String email) {
        return repository.getUserByEmail(email);
    }

    public void clearError() {
        errorLiveData.setValue(null);
    }

    public void login(String identifier, String password) {
        Log.d("AuthViewModel", "Logging in: " + identifier);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            User user;
            if (identifier.contains("@")) {
                user = userDao.getUserByEmailSync(identifier);
            } else {
                user = userDao.getUserByPhoneSync(identifier);
            }

            if (user != null) {
                errorLiveData.postValue("LOGGED_IN_LOCALLY");
            } else {
                errorLiveData.postValue("Account not found. Please register first.");
            }
        });
    }

    public void register(String name, String email, String password, String phoneNumber, String address, String nin, String role) {
        String uid = UUID.randomUUID().toString();
        User newUser = new User(uid, name, email, role, phoneNumber, address, nin);
        
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                repository.insertUser(newUser);
                errorLiveData.postValue("REGISTERED_LOCALLY");
            } catch (Exception e) {
                errorLiveData.postValue("Local registration failed: " + e.getMessage());
            }
        });
    }

    public void logout() {
        auth.signOut();
        userLiveData.setValue(null);
        errorLiveData.setValue(null);
    }

    public LiveData<FirebaseUser> getUserLiveData() { return userLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }
}
