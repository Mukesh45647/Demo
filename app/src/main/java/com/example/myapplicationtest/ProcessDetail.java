package com.example.myapplicationtest;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

public class ProcessDetail {

    private long mTimestamp;
    private Drawable mIcon;
    private String mApplicationName;
    private String mPackageName;

    public ProcessDetail(@NonNull String packageName,
                         @NonNull String applicationName,
                         @NonNull Drawable icon,
                         long lastUsedTimestamp) {
        mPackageName = packageName;
        mApplicationName = applicationName;
        mIcon = icon;
        mTimestamp = lastUsedTimestamp;
    }

    public String getApplicationName() {
        return mApplicationName;
    }

    public long getLastUsedTimestamp() {
        return mTimestamp;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public String getPackageName() {
        return mPackageName;
    }
}
