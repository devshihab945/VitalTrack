package com.xerotrust.vitaltrack.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.textfield.TextInputEditText;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;


public class ProfileActivity extends AppCompatActivity {

    private TextView tvAvatar, tvProfileName;
    private TextView tvNameVal, tvAgeVal, tvHeightVal, tvWeightVal, tvBloodVal;
    private TextInputEditText etName, etAge, etHeight, etWeight, etBlood;
    private CardView cardView, cardEdit;
    private Button btnEdit, btnSave, btnCancel;
    private AppPrefs prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_profile);

        prefs = new AppPrefs(this);

        tvAvatar = findViewById(R.id.tv_avatar);
        tvProfileName = findViewById(R.id.tv_profile_name);
        tvNameVal = findViewById(R.id.tv_name_val);
        tvAgeVal = findViewById(R.id.tv_age_val);
        tvHeightVal = findViewById(R.id.tv_height_val);
        tvWeightVal = findViewById(R.id.tv_weight_val);
        tvBloodVal = findViewById(R.id.tv_blood_val);

        etName = findViewById(R.id.et_name);
        etAge = findViewById(R.id.et_age);
        etHeight = findViewById(R.id.et_height);
        etWeight = findViewById(R.id.et_weight);
        etBlood = findViewById(R.id.et_blood);

        cardView = findViewById(R.id.card_view);
        cardEdit = findViewById(R.id.card_edit);
        btnEdit = findViewById(R.id.btn_edit);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        loadProfile();

        btnEdit.setOnClickListener(v -> switchToEditMode());
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> switchToViewMode());
    }

    private void loadProfile() {
        String name = prefs.getName();
        if (name.isEmpty()) name = "User";

        String initials = getInitials(name);
        tvAvatar.setText(initials);
        tvProfileName.setText(name);
        tvNameVal.setText(name.isEmpty() ? "--" : name);
        tvAgeVal.setText(prefs.getAge() > 0 ? prefs.getAge() + " years" : "--");
        tvHeightVal.setText(prefs.getHeight() > 0 ? prefs.getHeight() + " cm" : "--");
        tvWeightVal.setText(prefs.getWeight() > 0 ? prefs.getWeight() + " kg" : "--");
        tvBloodVal.setText(prefs.getBloodType().isEmpty() ? "--" : prefs.getBloodType());
    }

    private void switchToEditMode() {
        cardView.setVisibility(View.GONE);
        cardEdit.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);

        etName.setText(prefs.getName());
        etAge.setText(prefs.getAge() > 0 ? String.valueOf(prefs.getAge()) : "");
        etHeight.setText(prefs.getHeight() > 0 ? String.valueOf(prefs.getHeight()) : "");
        etWeight.setText(prefs.getWeight() > 0 ? String.valueOf(prefs.getWeight()) : "");
        etBlood.setText(prefs.getBloodType());
    }

    private void switchToViewMode() {
        cardView.setVisibility(View.VISIBLE);
        cardEdit.setVisibility(View.GONE);
        btnEdit.setVisibility(View.VISIBLE);
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        String ageStr = etAge.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String weightStr = etWeight.getText().toString().trim();
        String blood = etBlood.getText().toString().trim();

        if (name.isEmpty()) {
            etName.setError("Name required");
            return;
        }

        prefs.setName(name);
        if (!ageStr.isEmpty()) prefs.setAge(Integer.parseInt(ageStr));
        if (!heightStr.isEmpty()) prefs.setHeight(Float.parseFloat(heightStr));
        if (!weightStr.isEmpty()) prefs.setWeight(Float.parseFloat(weightStr));
        if (!blood.isEmpty()) prefs.setBloodType(blood);

        loadProfile();
        switchToViewMode();
        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
    }

    private String getInitials(String name) {
        String[] parts = name.trim().split(" ");
        if (parts.length >= 2) {
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        }
        return parts.length > 0 && !parts[0].isEmpty()
                ? String.valueOf(parts[0].charAt(0)).toUpperCase() : "U";
    }
}
