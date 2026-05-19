package com.xerotrust.vitaltrack.models;

public class WaterAlarm {
    private int id;
    private int hour;
    private int minute;
    private String label;       // user comment / label
    private String alertType;   // "vibrate" or "ring"
    private boolean enabled;

    public WaterAlarm() {}

    public WaterAlarm(int id, int hour, int minute, String label, String alertType, boolean enabled) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.label = label;
        this.alertType = alertType;
        this.enabled = enabled;
    }

    public WaterAlarm(int hour, int minute, String label, String alertType) {
        this(-1, hour, minute, label, alertType, true);
    }

    public int getId()        { return id; }
    public int getHour()      { return hour; }
    public int getMinute()    { return minute; }
    public String getLabel()  { return label; }
    public String getAlertType() { return alertType; }
    public boolean isEnabled()   { return enabled; }

    public void setId(int id)             { this.id = id; }
    public void setHour(int hour)         { this.hour = hour; }
    public void setMinute(int minute)     { this.minute = minute; }
    public void setLabel(String label)    { this.label = label; }
    public void setAlertType(String t)    { this.alertType = t; }
    public void setEnabled(boolean e)     { this.enabled = e; }

    /** HH:MM string for display */
    public String hhmm() {
        int h12 = hour == 0 ? 12 : hour > 12 ? hour - 12 : hour;
        String ampm = hour < 12 ? "AM" : "PM";
        return String.format("%02d:%02d %s", h12, minute, ampm);
    }
}
