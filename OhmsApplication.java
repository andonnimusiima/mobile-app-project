package com.example.ohms;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class OhmsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            // Try to initialize Firebase. 
            // If google-services.json is missing and plugin is disabled, 
            // we provide dummy options to prevent the crash on startup.
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId("1:1234567890:android:dummy") // Placeholder
                        .setApiKey("dummy_api_key")
                        .setProjectId("dummy-project")
                        .build();
                FirebaseApp.initializeApp(this, options);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
