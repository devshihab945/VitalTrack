package com.xerotrust.vitaltrack.models;

public class BloodSugarRecord {
    private int id;
    private float value;
    private String date, note;

    public BloodSugarRecord(int id, float value, String date, String note) {
        this.id = id; this.value = value; this.date = date; this.note = note;
    }
    public BloodSugarRecord(float value, String date, String note) {
        this(-1, value, date, note);
    }
    public int getId() { return id; }
    public float getValue() { return value; }
    public String getDate() { return date; }
    public String getNote() { return note; }
    public void setValue(float v) { value = v; }
    public void setNote(String v) { note = v; }
    public String getStatus() {
        if (value < 70) return "Low";
        if (value <= 100) return "Normal (Fasting)";
        if (value <= 140) return "Normal (Post-meal)";
        if (value <= 200) return "Pre-diabetic";
        return "High";
    }
}
