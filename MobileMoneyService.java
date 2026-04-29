package com.example.ohms.service;

import android.os.Handler;
import android.os.Looper;

public class MobileMoneyService {

    public interface PaymentCallback {
        void onSuccess(String transactionId);
        void onFailure(String error);
    }

    /**
     * Simulates a mobile money payment (MTN/Airtel)
     */
    public void processPayment(String phoneNumber, double amount, String provider, PaymentCallback callback) {
        // Simulate network delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (phoneNumber.length() >= 10) {
                callback.onSuccess("TXN-" + System.currentTimeMillis());
            } else {
                callback.onFailure("Invalid phone number or insufficient funds");
            }
        }, 2000);
    }
}
