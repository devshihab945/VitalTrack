package com.xerotrust.vitaltrack.models;

public class MedicineScheduleItem {
    private int id;
    private int planId;
    private String label;   // e.g., After Breakfast
    private String dosage;  // e.g., 1.0 / 500mg

    public MedicineScheduleItem() {
    }

    public MedicineScheduleItem(int id, int planId, String label, String dosage) {
        this.id = id;
        this.planId = planId;
        this.label = label;
        this.dosage = dosage;
    }

    public int getId() {
        return id;
    }

    public int getPlanId() {
        return planId;
    }

    public String getLabel() {
        return label;
    }

    public String getDosage() {
        return dosage;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }
}