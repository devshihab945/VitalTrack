package com.xerotrust.vitaltrack.models;

public class MedicineTime {
    private int id;
    private int planId;
    private int hour;   // 0-23
    private int minute; // 0-59

    public MedicineTime() {
    }

    public MedicineTime(int id, int planId, int hour, int minute) {
        this.id = id;
        this.planId = planId;
        this.hour = hour;
        this.minute = minute;
    }

    public int getId() {
        return id;
    }

    public int getPlanId() {
        return planId;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String hhmm() {
        int h12 = hour % 12;
        if (h12 == 0) h12 = 12;
        String amPm = hour < 12 ? "AM" : "PM";
        return String.format(java.util.Locale.getDefault(), "%d:%02d %s", h12, minute, amPm);
    }
}