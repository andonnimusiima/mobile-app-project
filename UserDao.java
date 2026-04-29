package com.example.ohms.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ohms.model.User;

import java.util.List;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<User> getUserById(String userId);

    @Query("SELECT * FROM users WHERE role = 'BENEFICIARY' ORDER BY name ASC")
    LiveData<List<User>> getAllBeneficiaries();

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    LiveData<User> getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    LiveData<User> getUserByPhone(String phone);
    
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmailSync(String email);

    @Query("SELECT * FROM users WHERE phoneNumber = :phone LIMIT 1")
    User getUserByPhoneSync(String phone);

    @Query("DELETE FROM users")
    void deleteAllUsers();
}
