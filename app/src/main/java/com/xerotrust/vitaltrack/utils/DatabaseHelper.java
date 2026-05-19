package com.xerotrust.vitaltrack.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.xerotrust.vitaltrack.models.BloodPressureRecord;
import com.xerotrust.vitaltrack.models.BloodSugarRecord;
import com.xerotrust.vitaltrack.models.MedicinePlan;
import com.xerotrust.vitaltrack.models.MedicineScheduleItem;
import com.xerotrust.vitaltrack.models.MedicineTime;
import com.xerotrust.vitaltrack.models.MedicineReminder;
import com.xerotrust.vitaltrack.models.WaterAlarm;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "health_app.db";
    // v3 = old medicine remind_60/remind_10
    // v4 = introduce medicine plans + migrate old medicine -> new tables
    // v5 = water alarm table
    private static final int DB_VERSION = 5;

    // ─── Water Alarm Table ───────────────────────────────────────────────────
    public static final String TABLE_WATER_ALARM = "water_alarm";
    public static final String WA_ID = "id";
    public static final String WA_HOUR = "hour";
    public static final String WA_MINUTE = "minute";
    public static final String WA_LABEL = "label";
    public static final String WA_ALERT_TYPE = "alert_type"; // "vibrate" | "ring"
    public static final String WA_ENABLED = "enabled";

    // ─── Blood Pressure Table ────────────────────────────────────────────────
    public static final String TABLE_BP = "blood_pressure";
    public static final String BP_ID = "id";
    public static final String BP_SYSTOLIC = "systolic";
    public static final String BP_DIASTOLIC = "diastolic";
    public static final String BP_PULSE = "pulse";
    public static final String BP_DATE = "date";
    public static final String BP_NOTE = "note";

    // ─── Blood Sugar Table ───────────────────────────────────────────────────
    public static final String TABLE_SUGAR = "blood_sugar";
    public static final String SUGAR_ID = "id";
    public static final String SUGAR_VALUE = "value";
    public static final String SUGAR_DATE = "date";
    public static final String SUGAR_NOTE = "note";

    // ─── OLD Medicine Table (kept for migration/legacy reads) ────────────────
    public static final String TABLE_MEDICINE = "medicine";
    public static final String MED_ID = "id";
    public static final String MED_NAME = "name";
    public static final String MED_DOSAGE = "dosage";
    public static final String MED_TIME = "time";
    public static final String MED_HOUR = "hour";
    public static final String MED_MINUTE = "minute";
    public static final String MED_ENABLED = "enabled";
    public static final String MED_VIBRATION = "vibration_mode";
    public static final String MED_REMIND_60 = "remind_60";
    public static final String MED_REMIND_10 = "remind_10";

    // ─── NEW Medicine Plan Tables ────────────────────────────────────────────
    public static final String TABLE_MED_PLAN = "medicine_plan";
    public static final String PLAN_ID = "id";
    public static final String PLAN_NAME = "name";
    public static final String PLAN_FREQ_TYPE = "frequency_type";
    public static final String PLAN_FREQ_PARAM = "frequency_param";
    public static final String PLAN_ENABLED = "enabled";
    public static final String PLAN_VIBRATION = "vibration_mode";
    public static final String PLAN_REMIND_60 = "remind_60";
    public static final String PLAN_REMIND_10 = "remind_10";

    public static final String TABLE_MED_SCHEDULE = "medicine_schedule_item";
    public static final String SCH_ID = "id";
    public static final String SCH_PLAN_ID = "plan_id";
    public static final String SCH_LABEL = "label";
    public static final String SCH_DOSAGE = "dosage";

    public static final String TABLE_MED_TIME = "medicine_time";
    public static final String TIME_ID = "id";
    public static final String TIME_PLAN_ID = "plan_id";
    public static final String TIME_HOUR = "hour";
    public static final String TIME_MINUTE = "minute";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // onCreate
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE " + TABLE_BP + " (" + BP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + BP_SYSTOLIC + " INTEGER, " + BP_DIASTOLIC + " INTEGER, " + BP_PULSE + " INTEGER, " + BP_DATE + " TEXT, " + BP_NOTE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_SUGAR + " (" + SUGAR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SUGAR_VALUE + " REAL, " + SUGAR_DATE + " TEXT, " + SUGAR_NOTE + " TEXT)");

        // Old medicine table (kept for migration + legacy methods)
        db.execSQL("CREATE TABLE " + TABLE_MEDICINE + " (" + MED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + MED_NAME + " TEXT NOT NULL, " + MED_DOSAGE + " TEXT, " + MED_TIME + " TEXT, " + MED_HOUR + " INTEGER, " + MED_MINUTE + " INTEGER, " + MED_ENABLED + " INTEGER DEFAULT 1, " + MED_VIBRATION + " TEXT DEFAULT 'normal', " + MED_REMIND_60 + " INTEGER DEFAULT 1, " + MED_REMIND_10 + " INTEGER DEFAULT 1" + ")");

        createMedicinePlanTables(db);
        createWaterAlarmTable(db);
    }

    private void createWaterAlarmTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WATER_ALARM + " (" + WA_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + WA_HOUR + " INTEGER NOT NULL, " + WA_MINUTE + " INTEGER NOT NULL, " + WA_LABEL + " TEXT, " + WA_ALERT_TYPE + " TEXT DEFAULT 'ring', " + WA_ENABLED + " INTEGER DEFAULT 1" + ")");
    }

    private void createMedicinePlanTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MED_PLAN + " (" + PLAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + PLAN_NAME + " TEXT NOT NULL, " + PLAN_FREQ_TYPE + " TEXT NOT NULL, " + PLAN_FREQ_PARAM + " TEXT, " + PLAN_ENABLED + " INTEGER DEFAULT 1, " + PLAN_VIBRATION + " TEXT DEFAULT 'normal', " + PLAN_REMIND_60 + " INTEGER DEFAULT 1, " + PLAN_REMIND_10 + " INTEGER DEFAULT 1" + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MED_SCHEDULE + " (" + SCH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + SCH_PLAN_ID + " INTEGER NOT NULL, " + SCH_LABEL + " TEXT NOT NULL, " + SCH_DOSAGE + " TEXT, " + "FOREIGN KEY(" + SCH_PLAN_ID + ") REFERENCES " + TABLE_MED_PLAN + "(" + PLAN_ID + ") ON DELETE CASCADE" + ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MED_TIME + " (" + TIME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + TIME_PLAN_ID + " INTEGER NOT NULL, " + TIME_HOUR + " INTEGER NOT NULL, " + TIME_MINUTE + " INTEGER NOT NULL, " + "FOREIGN KEY(" + TIME_PLAN_ID + ") REFERENCES " + TABLE_MED_PLAN + "(" + PLAN_ID + ") ON DELETE CASCADE" + ")");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // onUpgrade
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // v2: vibration
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_MEDICINE + " ADD COLUMN " + MED_VIBRATION + " TEXT DEFAULT 'normal'");
        }

        // v3: pre reminders
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_MEDICINE + " ADD COLUMN " + MED_REMIND_60 + " INTEGER DEFAULT 1");
            db.execSQL("ALTER TABLE " + TABLE_MEDICINE + " ADD COLUMN " + MED_REMIND_10 + " INTEGER DEFAULT 1");
        }

        // v4: plan tables + migration
        if (oldVersion < 4) {
            createMedicinePlanTables(db);
            migrateOldMedicineToPlans(db);
        }

        // v5: water alarm table
        if (oldVersion < 5) {
            createWaterAlarmTable(db);
        }
    }

    private void migrateOldMedicineToPlans(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_MEDICINE, null);
        if (c == null) return;

        try {
            if (!c.moveToFirst()) return;

            do {
                String name = c.getString(c.getColumnIndexOrThrow(MED_NAME));
                String dosage = c.getString(c.getColumnIndexOrThrow(MED_DOSAGE));
                int hour = c.getInt(c.getColumnIndexOrThrow(MED_HOUR));
                int minute = c.getInt(c.getColumnIndexOrThrow(MED_MINUTE));
                boolean enabled = c.getInt(c.getColumnIndexOrThrow(MED_ENABLED)) == 1;

                String vibration = "normal";
                int vibCol = c.getColumnIndex(MED_VIBRATION);
                if (vibCol != -1 && !c.isNull(vibCol)) vibration = c.getString(vibCol);

                boolean remind60 = true;
                int r60Col = c.getColumnIndex(MED_REMIND_60);
                if (r60Col != -1 && !c.isNull(r60Col)) remind60 = c.getInt(r60Col) == 1;

                boolean remind10 = true;
                int r10Col = c.getColumnIndex(MED_REMIND_10);
                if (r10Col != -1 && !c.isNull(r10Col)) remind10 = c.getInt(r10Col) == 1;

                // 1) Create plan (defaults)
                ContentValues planCV = new ContentValues();
                planCV.put(PLAN_NAME, name);
                planCV.put(PLAN_FREQ_TYPE, MedicinePlan.FREQ_EVERYDAY);
                planCV.put(PLAN_FREQ_PARAM, "");
                planCV.put(PLAN_ENABLED, enabled ? 1 : 0);
                planCV.put(PLAN_VIBRATION, vibration);
                planCV.put(PLAN_REMIND_60, remind60 ? 1 : 0);
                planCV.put(PLAN_REMIND_10, remind10 ? 1 : 0);

                long newPlanId = db.insert(TABLE_MED_PLAN, null, planCV);
                if (newPlanId <= 0) continue;

                // 2) Schedule item
                ContentValues schCV = new ContentValues();
                schCV.put(SCH_PLAN_ID, (int) newPlanId);
                schCV.put(SCH_LABEL, inferLabelFromTime(hour));
                schCV.put(SCH_DOSAGE, (dosage == null || dosage.trim().isEmpty()) ? "1.0" : dosage);
                db.insert(TABLE_MED_SCHEDULE, null, schCV);

                // 3) Time
                ContentValues timeCV = new ContentValues();
                timeCV.put(TIME_PLAN_ID, (int) newPlanId);
                timeCV.put(TIME_HOUR, hour);
                timeCV.put(TIME_MINUTE, minute);
                db.insert(TABLE_MED_TIME, null, timeCV);

            } while (c.moveToNext());

        } finally {
            c.close();
        }
    }

    private String inferLabelFromTime(int hour24) {
        if (hour24 >= 4 && hour24 <= 11) return "After Breakfast";
        if (hour24 >= 12 && hour24 <= 16) return "After Lunch";
        if (hour24 >= 17 && hour24 <= 23) return "After Dinner";
        return "Dose";
    }

    // =========================================================================
    // Blood Pressure CRUD
    // =========================================================================
    public long addBPRecord(BloodPressureRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(BP_SYSTOLIC, record.getSystolic());
        cv.put(BP_DIASTOLIC, record.getDiastolic());
        cv.put(BP_PULSE, record.getPulse());
        cv.put(BP_DATE, record.getDate());
        cv.put(BP_NOTE, record.getNote());
        long id = db.insert(TABLE_BP, null, cv);
        db.close();
        return id;
    }

    public List<BloodPressureRecord> getAllBPRecords() {
        List<BloodPressureRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_BP + " ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new BloodPressureRecord(cursor.getInt(cursor.getColumnIndexOrThrow(BP_ID)), cursor.getInt(cursor.getColumnIndexOrThrow(BP_SYSTOLIC)), cursor.getInt(cursor.getColumnIndexOrThrow(BP_DIASTOLIC)), cursor.getInt(cursor.getColumnIndexOrThrow(BP_PULSE)), cursor.getString(cursor.getColumnIndexOrThrow(BP_DATE)), cursor.getString(cursor.getColumnIndexOrThrow(BP_NOTE))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void deleteBPRecord(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BP, BP_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateBPRecord(BloodPressureRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(BP_SYSTOLIC, record.getSystolic());
        cv.put(BP_DIASTOLIC, record.getDiastolic());
        cv.put(BP_PULSE, record.getPulse());
        cv.put(BP_NOTE, record.getNote());
        db.update(TABLE_BP, cv, BP_ID + "=?", new String[]{String.valueOf(record.getId())});
        db.close();
    }

    // =========================================================================
    // Sugar CRUD
    // =========================================================================
    public long addSugarRecord(BloodSugarRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SUGAR_VALUE, record.getValue());
        cv.put(SUGAR_DATE, record.getDate());
        cv.put(SUGAR_NOTE, record.getNote());
        long id = db.insert(TABLE_SUGAR, null, cv);
        db.close();
        return id;
    }

    public List<BloodSugarRecord> getAllSugarRecords() {
        List<BloodSugarRecord> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_SUGAR + " ORDER BY id DESC", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(new BloodSugarRecord(cursor.getInt(cursor.getColumnIndexOrThrow(SUGAR_ID)), cursor.getFloat(cursor.getColumnIndexOrThrow(SUGAR_VALUE)), cursor.getString(cursor.getColumnIndexOrThrow(SUGAR_DATE)), cursor.getString(cursor.getColumnIndexOrThrow(SUGAR_NOTE))));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public void deleteSugarRecord(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_SUGAR, SUGAR_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateSugarRecord(BloodSugarRecord record) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(SUGAR_VALUE, record.getValue());
        cv.put(SUGAR_NOTE, record.getNote());
        db.update(TABLE_SUGAR, cv, SUGAR_ID + "=?", new String[]{String.valueOf(record.getId())});
        db.close();
    }

    // =========================================================================
    // Legacy MedicineReminder CRUD (kept so old code compiles if referenced)
    // =========================================================================
    public long addMedicine(MedicineReminder med) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MED_NAME, med.getName());
        cv.put(MED_DOSAGE, med.getDosage());
        cv.put(MED_TIME, med.getTime());
        cv.put(MED_HOUR, med.getHour());
        cv.put(MED_MINUTE, med.getMinute());
        cv.put(MED_ENABLED, med.isEnabled() ? 1 : 0);
        cv.put(MED_VIBRATION, med.getVibrationMode());
        cv.put(MED_REMIND_60, med.isRemind60() ? 1 : 0);
        cv.put(MED_REMIND_10, med.isRemind10() ? 1 : 0);
        long id = db.insert(TABLE_MEDICINE, null, cv);
        db.close();
        return id;
    }

    public void updateMedicine(MedicineReminder med) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MED_NAME, med.getName());
        cv.put(MED_DOSAGE, med.getDosage());
        cv.put(MED_TIME, med.getTime());
        cv.put(MED_HOUR, med.getHour());
        cv.put(MED_MINUTE, med.getMinute());
        cv.put(MED_ENABLED, med.isEnabled() ? 1 : 0);
        cv.put(MED_VIBRATION, med.getVibrationMode());
        cv.put(MED_REMIND_60, med.isRemind60() ? 1 : 0);
        cv.put(MED_REMIND_10, med.isRemind10() ? 1 : 0);
        db.update(TABLE_MEDICINE, cv, MED_ID + "=?", new String[]{String.valueOf(med.getId())});
        db.close();
    }

    public void updateMedicineEnabled(int id, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(MED_ENABLED, enabled ? 1 : 0);
        db.update(TABLE_MEDICINE, cv, MED_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteMedicine(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MEDICINE, MED_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<MedicineReminder> getAllMedicines() {
        List<MedicineReminder> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_MEDICINE + " ORDER BY " + MED_ID + " DESC", null);
        if (c.moveToFirst()) {
            do {
                MedicineReminder m = new MedicineReminder(c.getString(c.getColumnIndexOrThrow(MED_NAME)), safeStr(c, MED_DOSAGE), safeStr(c, MED_TIME), c.getInt(c.getColumnIndexOrThrow(MED_HOUR)), c.getInt(c.getColumnIndexOrThrow(MED_MINUTE)));
                m.setId(c.getInt(c.getColumnIndexOrThrow(MED_ID)));
                m.setEnabled(c.getInt(c.getColumnIndexOrThrow(MED_ENABLED)) == 1);
                m.setVibrationMode(safeStr(c, MED_VIBRATION));
                m.setRemind60(intCol(c, MED_REMIND_60, 1) == 1);
                m.setRemind10(intCol(c, MED_REMIND_10, 1) == 1);
                list.add(m);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    private String safeStr(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i == -1 || c.isNull(i)) return "";
        return c.getString(i);
    }

    private int intCol(Cursor c, String col, int def) {
        int i = c.getColumnIndex(col);
        if (i == -1 || c.isNull(i)) return def;
        return c.getInt(i);
    }

    // =========================================================================
    // NEW Medicine Plan CRUD
    // =========================================================================
    public long insertPlan(MedicinePlan plan) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(PLAN_NAME, plan.getName());
        cv.put(PLAN_FREQ_TYPE, plan.getFrequencyType());
        cv.put(PLAN_FREQ_PARAM, plan.getFrequencyParam());
        cv.put(PLAN_ENABLED, plan.isEnabled() ? 1 : 0);
        cv.put(PLAN_VIBRATION, plan.getVibrationMode());
        cv.put(PLAN_REMIND_60, plan.isRemind60() ? 1 : 0);
        cv.put(PLAN_REMIND_10, plan.isRemind10() ? 1 : 0);
        long id = db.insert(TABLE_MED_PLAN, null, cv);
        db.close();
        return id;
    }

    public void updatePlan(MedicinePlan plan) {
        if (plan == null || plan.getId() <= 0) return;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(PLAN_NAME, plan.getName());
        cv.put(PLAN_FREQ_TYPE, plan.getFrequencyType());
        cv.put(PLAN_FREQ_PARAM, plan.getFrequencyParam());
        cv.put(PLAN_ENABLED, plan.isEnabled() ? 1 : 0);
        cv.put(PLAN_VIBRATION, plan.getVibrationMode());
        cv.put(PLAN_REMIND_60, plan.isRemind60() ? 1 : 0);
        cv.put(PLAN_REMIND_10, plan.isRemind10() ? 1 : 0);
        db.update(TABLE_MED_PLAN, cv, PLAN_ID + "=?", new String[]{String.valueOf(plan.getId())});
        db.close();
    }

    public void updatePlanEnabled(int planId, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(PLAN_ENABLED, enabled ? 1 : 0);
        db.update(TABLE_MED_PLAN, cv, PLAN_ID + "=?", new String[]{String.valueOf(planId)});
        db.close();
    }

    public void deletePlan(int planId) {
        SQLiteDatabase db = getWritableDatabase();
        // delete children first (safer even if FK cascade not enabled)
        db.delete(TABLE_MED_SCHEDULE, SCH_PLAN_ID + "=?", new String[]{String.valueOf(planId)});
        db.delete(TABLE_MED_TIME, TIME_PLAN_ID + "=?", new String[]{String.valueOf(planId)});
        db.delete(TABLE_MED_PLAN, PLAN_ID + "=?", new String[]{String.valueOf(planId)});
        db.close();
    }

    public void replaceScheduleItems(int planId, List<MedicineScheduleItem> items) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MED_SCHEDULE, SCH_PLAN_ID + "=?", new String[]{String.valueOf(planId)});

        if (items != null) {
            for (MedicineScheduleItem it : items) {
                ContentValues cv = new ContentValues();
                cv.put(SCH_PLAN_ID, planId);
                cv.put(SCH_LABEL, it.getLabel());
                cv.put(SCH_DOSAGE, it.getDosage());
                db.insert(TABLE_MED_SCHEDULE, null, cv);
            }
        }

        db.close();
    }

    public void replaceTimes(int planId, List<MedicineTime> times) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_MED_TIME, TIME_PLAN_ID + "=?", new String[]{String.valueOf(planId)});

        if (times != null) {
            for (MedicineTime t : times) {
                ContentValues cv = new ContentValues();
                cv.put(TIME_PLAN_ID, planId);
                cv.put(TIME_HOUR, t.getHour());
                cv.put(TIME_MINUTE, t.getMinute());
                db.insert(TABLE_MED_TIME, null, cv);
            }
        }

        db.close();
    }

    public List<MedicinePlan> getAllPlansBasic() {
        List<MedicinePlan> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_MED_PLAN + " ORDER BY " + PLAN_ID + " DESC", null);
        if (c.moveToFirst()) {
            do {
                list.add(cursorToPlanBasic(c));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    public MedicinePlan getPlanFull(int planId) {
        SQLiteDatabase db = getReadableDatabase();

        MedicinePlan plan = null;
        Cursor c = db.query(TABLE_MED_PLAN, null, PLAN_ID + "=?", new String[]{String.valueOf(planId)}, null, null, null);
        if (c.moveToFirst()) {
            plan = cursorToPlanBasic(c);
        }
        c.close();

        if (plan == null) {
            db.close();
            return null;
        }

        List<MedicineScheduleItem> items = new ArrayList<>();
        Cursor s = db.query(TABLE_MED_SCHEDULE, null, SCH_PLAN_ID + "=?", new String[]{String.valueOf(planId)}, null, null, SCH_ID + " ASC");
        if (s.moveToFirst()) {
            do {
                items.add(new MedicineScheduleItem(s.getInt(s.getColumnIndexOrThrow(SCH_ID)), planId, s.getString(s.getColumnIndexOrThrow(SCH_LABEL)), safeStr(s, SCH_DOSAGE)));
            } while (s.moveToNext());
        }
        s.close();

        List<MedicineTime> times = new ArrayList<>();
        Cursor t = db.query(TABLE_MED_TIME, null, TIME_PLAN_ID + "=?", new String[]{String.valueOf(planId)}, null, null, TIME_ID + " ASC");
        if (t.moveToFirst()) {
            do {
                times.add(new MedicineTime(t.getInt(t.getColumnIndexOrThrow(TIME_ID)), planId, t.getInt(t.getColumnIndexOrThrow(TIME_HOUR)), t.getInt(t.getColumnIndexOrThrow(TIME_MINUTE))));
            } while (t.moveToNext());
        }
        t.close();

        plan.setScheduleItems(items);
        plan.setTimes(times);

        db.close();
        return plan;
    }

    private MedicinePlan cursorToPlanBasic(Cursor c) {
        int id = c.getInt(c.getColumnIndexOrThrow(PLAN_ID));
        String name = c.getString(c.getColumnIndexOrThrow(PLAN_NAME));
        String ft = c.getString(c.getColumnIndexOrThrow(PLAN_FREQ_TYPE));
        String fp = safeStr(c, PLAN_FREQ_PARAM);
        boolean enabled = c.getInt(c.getColumnIndexOrThrow(PLAN_ENABLED)) == 1;

        String vibration = safeStr(c, PLAN_VIBRATION);
        if (vibration.isEmpty()) vibration = "normal";

        boolean remind60 = intCol(c, PLAN_REMIND_60, 1) == 1;
        boolean remind10 = intCol(c, PLAN_REMIND_10, 1) == 1;

        return new MedicinePlan(id, name, ft, fp, enabled, vibration, remind60, remind10);
    }

    // =========================================================================
    // Water Alarm CRUD
    // =========================================================================
    public long addWaterAlarm(WaterAlarm alarm) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(WA_HOUR, alarm.getHour());
        cv.put(WA_MINUTE, alarm.getMinute());
        cv.put(WA_LABEL, alarm.getLabel());
        cv.put(WA_ALERT_TYPE, alarm.getAlertType());
        cv.put(WA_ENABLED, alarm.isEnabled() ? 1 : 0);
        long id = db.insert(TABLE_WATER_ALARM, null, cv);
        db.close();
        return id;
    }

    public void updateWaterAlarm(WaterAlarm alarm) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(WA_HOUR, alarm.getHour());
        cv.put(WA_MINUTE, alarm.getMinute());
        cv.put(WA_LABEL, alarm.getLabel());
        cv.put(WA_ALERT_TYPE, alarm.getAlertType());
        cv.put(WA_ENABLED, alarm.isEnabled() ? 1 : 0);
        db.update(TABLE_WATER_ALARM, cv, WA_ID + "=?", new String[]{String.valueOf(alarm.getId())});
        db.close();
    }

    public void updateWaterAlarmEnabled(int id, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(WA_ENABLED, enabled ? 1 : 0);
        db.update(TABLE_WATER_ALARM, cv, WA_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteWaterAlarm(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_WATER_ALARM, WA_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public java.util.List<WaterAlarm> getAllWaterAlarms() {
        java.util.List<WaterAlarm> list = new java.util.ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_WATER_ALARM + " ORDER BY " + WA_HOUR + " ASC, " + WA_MINUTE + " ASC", null);
        if (c.moveToFirst()) {
            do {
                list.add(new WaterAlarm(c.getInt(c.getColumnIndexOrThrow(WA_ID)), c.getInt(c.getColumnIndexOrThrow(WA_HOUR)), c.getInt(c.getColumnIndexOrThrow(WA_MINUTE)), safeStr(c, WA_LABEL), safeStr(c, WA_ALERT_TYPE), c.getInt(c.getColumnIndexOrThrow(WA_ENABLED)) == 1));
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }
}
