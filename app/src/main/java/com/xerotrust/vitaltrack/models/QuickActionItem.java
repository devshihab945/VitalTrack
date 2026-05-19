package com.xerotrust.vitaltrack.models;

public class QuickActionItem {
    private final String title;
    private final int iconRes;
    private final int iconBackgroundRes;
    private final Class<?> targetActivity;

    public QuickActionItem(String title, int iconRes, int iconBackgroundRes, Class<?> targetActivity) {
        this.title = title;
        this.iconRes = iconRes;
        this.iconBackgroundRes = iconBackgroundRes;
        this.targetActivity = targetActivity;
    }

    public String getTitle() {
        return title;
    }

    public int getIconRes() {
        return iconRes;
    }

    public int getIconBackgroundRes() {
        return iconBackgroundRes;
    }

    public Class<?> getTargetActivity() {
        return targetActivity;
    }
}