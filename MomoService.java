package com.example.ohms.service;

import android.os.Handler;
import android.os.Looper;

/**
 * Service to handle Mobile Money (MoMo) Integrations.
 * In a production environment, this would interface with MTN MoMo or Airtel Money APIs.
 */
public class MomoService {
    // Fixed Admin MoMo Number for Collections and Disbursements
    public static final String ADMIN_MOMO_NUMBER = "0760010982";

    public interface MomoCallback {
        void onSuccess(String transactionId);
        void onFailure(String errorMessage);
    }

    /**
     * INITIATE COLLECTION (STK PUSH)
     * Triggers a PIN prompt on the client's phone to collect money to the admin account.
     */
    public static void initiateCollection(String clientPhone, double amount, MomoCallback callback) {
        // Simulate API call to MoMo Gateway
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Logic: Trigger STK Push on clientPhone for 'amount'
            // Target: ADMIN_MOMO_NUMBER
            if (clientPhone != null && clientPhone.length() >= 10) {
                callback.onSuccess("MOMO-COL-" + System.currentTimeMillis());
            } else {
                callback.onFailure("Invalid Client Phone Number");
            }
        }, 4000); // Simulated network delay
    }

    /**
     * INITIATE DISBURSEMENT
     * Sends money from the admin account to the client's phone number.
     */
    public static void initiateDisbursement(String clientPhone, double amount, MomoCallback callback) {
        // Simulate API call to MoMo Gateway
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Logic: Send 'amount' from ADMIN_MOMO_NUMBER to clientPhone
            if (clientPhone != null && clientPhone.length() >= 10) {
                callback.onSuccess("MOMO-DISB-" + System.currentTimeMillis());
            } else {
                callback.onFailure("Disbursement Failed: Invalid Target Number");
            }
        }, 3000);
    }
}
