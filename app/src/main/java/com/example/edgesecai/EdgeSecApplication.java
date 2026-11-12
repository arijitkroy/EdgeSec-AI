package com.example.edgesecai;

import android.app.Application;

import com.example.edgesecai.storage.SecurityRepository;

public class EdgeSecApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize On-Device Security Core
        SecurityRepository.getInstance(this);
    }
}
