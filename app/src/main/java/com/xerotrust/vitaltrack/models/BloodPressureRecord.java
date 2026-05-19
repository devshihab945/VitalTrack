package com.xerotrust.vitaltrack.models;

public class BloodPressureRecord {
    private int id, systolic, diastolic, pulse;
    private String date, note;

    public BloodPressureRecord(int id, int systolic, int diastolic, int pulse, String date, String note) {
        this.id = id; this.systolic = systolic; this.diastolic = diastolic;
        this.pulse = pulse; this.date = date; this.note = note;
    }
    public BloodPressureRecord(int systolic, int diastolic, int pulse, String date, String note) {
        this(-1, systolic, diastolic, pulse, date, note);
    }
    public int getId() { return id; }
    public int getSystolic() { return systolic; }
    public int getDiastolic() { return diastolic; }
    public int getPulse() { return pulse; }
    public String getDate() { return date; }
    public String getNote() { return note; }
    public void setSystolic(int v) { systolic = v; }
    public void setDiastolic(int v) { diastolic = v; }
    public void setPulse(int v) { pulse = v; }
    public void setNote(String v) { note = v; }
    public String getDisplayBP() { return systolic + "/" + diastolic; }
    public String getCategory() {
        if (systolic < 120 && diastolic < 80) return "Normal";
        if (systolic < 130 && diastolic < 80) return "Elevated";
        if (systolic < 140 || diastolic < 90) return "High Stage 1";
        if (systolic >= 140 || diastolic >= 90) return "High Stage 2";
        return "Unknown";
    }
}
