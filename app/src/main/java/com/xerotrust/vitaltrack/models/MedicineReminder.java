package com.xerotrust.vitaltrack.models;

public class MedicineReminder {
    private int id, hour, minute;
    private String name, dosage, time, vibrationMode;
    private boolean enabled;

    private boolean remind60 = true;
    private boolean remind10 = true;

    public MedicineReminder(int id, String name, String dosage, String time, int hour, int minute, boolean enabled, String vibrationMode, boolean remind60, boolean remind10) {
        this.id = id;
        this.name = name;
        this.dosage = dosage;
        this.time = time;
        this.hour = hour;
        this.minute = minute;
        this.enabled = enabled;
        this.vibrationMode = vibrationMode != null ? vibrationMode : "normal";
        this.remind60 = remind60;
        this.remind10 = remind10;
    }

    public MedicineReminder(String name, String dosage, String time, int hour, int minute) {
        this(-1, name, dosage, time, hour, minute, true, "normal", true, true);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDosage() {
        return dosage;
    }

    public String getTime() {
        return time;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getVibrationMode() {
        return vibrationMode;
    }

    public boolean isRemind60() {
        return remind60;
    }

    public boolean isRemind10() {
        return remind10;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVibrationMode(String vibrationMode) {
        this.vibrationMode = vibrationMode;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setRemind60(boolean remind60) {
        this.remind60 = remind60;
    }

    public void setRemind10(boolean remind10) {
        this.remind10 = remind10;
    }
}