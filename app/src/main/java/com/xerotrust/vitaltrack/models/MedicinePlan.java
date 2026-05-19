package com.xerotrust.vitaltrack.models;

import java.util.ArrayList;
import java.util.List;

public class MedicinePlan {

    public static final String FREQ_EVERYDAY = "EVERYDAY";
    public static final String FREQ_EVERY_X_DAYS = "EVERY_X_DAYS";
    public static final String FREQ_DAY_OF_WEEK = "DAY_OF_WEEK";
    public static final String FREQ_DAY_OF_MONTH = "DAY_OF_MONTH";
    public static final String FREQ_X_ENABLE_Y_DISABLE = "X_ENABLE_Y_DISABLE";

    private int id;
    private String name;
    private String frequencyType;   // one of FREQ_*
    private String frequencyParam;  // string payload (e.g. "3" or "MON,WED,FRI" or "1,15,30" or "2,1")
    private boolean enabled;
    private String vibrationMode;   // "normal" or "alarm"
    private boolean remind60;
    private boolean remind10;

    // loaded children (optional)
    private List<MedicineScheduleItem> scheduleItems = new ArrayList<>();
    private List<MedicineTime> times = new ArrayList<>();

    // transient UI fields – computed by fragment, read by adapter
    private String computedNextDose = "";
    private String computedMeta     = "";

    public MedicinePlan() {
    }

    public MedicinePlan(int id, String name, String frequencyType, String frequencyParam, boolean enabled, String vibrationMode, boolean remind60, boolean remind10) {
        this.id = id;
        this.name = name;
        this.frequencyType = frequencyType;
        this.frequencyParam = frequencyParam;
        this.enabled = enabled;
        this.vibrationMode = vibrationMode;
        this.remind60 = remind60;
        this.remind10 = remind10;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFrequencyType() {
        return frequencyType;
    }

    public String getFrequencyParam() {
        return frequencyParam;
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

    public List<MedicineScheduleItem> getScheduleItems() {
        return scheduleItems;
    }

    public List<MedicineTime> getTimes() {
        return times;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFrequencyType(String frequencyType) {
        this.frequencyType = frequencyType;
    }

    public void setFrequencyParam(String frequencyParam) {
        this.frequencyParam = frequencyParam;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setVibrationMode(String vibrationMode) {
        this.vibrationMode = vibrationMode;
    }

    public void setRemind60(boolean remind60) {
        this.remind60 = remind60;
    }

    public void setRemind10(boolean remind10) {
        this.remind10 = remind10;
    }

    public void setScheduleItems(List<MedicineScheduleItem> scheduleItems) {
        this.scheduleItems = (scheduleItems != null) ? scheduleItems : new ArrayList<>();
    }

    public void setTimes(List<MedicineTime> times) {
        this.times = (times != null) ? times : new ArrayList<>();
    }

    public String getComputedNextDose() {
        return computedNextDose != null ? computedNextDose : "";
    }

    public void setComputedNextDose(String v) {
        this.computedNextDose = v != null ? v : "";
    }

    public String getComputedMeta() {
        return computedMeta != null ? computedMeta : "";
    }

    public void setComputedMeta(String v) {
        this.computedMeta = v != null ? v : "";
    }
}