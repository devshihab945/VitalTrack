package com.xerotrust.vitaltrack.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.utils.LanguageHelper;
import com.xerotrust.vitaltrack.utils.PremiumSnackbar;
import com.xerotrust.vitaltrack.utils.SnackbarType;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;

public class ProfileSetupActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    private TextInputEditText etName, etAge, etWeight;
    private TextInputEditText etHeight;
    private TextInputEditText etHeightFeet, etHeightInch;
    private TextInputLayout tilHeightCm;
    private View layoutHeightFeet;
    private TextView btnUnitCm, btnUnitFeet;

    private Spinner spinnerBloodType;
    private TextView tvAvatarInitials, tvProfileName;
    private ImageView ivAvatarImage;

    private boolean isUnitCm = false;

    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_setup);

        View decor = getWindow().getDecorView();
        decor.post(() -> {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary, getTheme()));
            WindowCompat.getInsetsController(getWindow(), decor).setAppearanceLightStatusBars(false);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etName = findViewById(R.id.et_full_name);
        etAge = findViewById(R.id.et_age);
        etWeight = findViewById(R.id.et_weight);
        etHeight = findViewById(R.id.et_height);
        etHeightFeet = findViewById(R.id.et_height_feet);
        etHeightInch = findViewById(R.id.et_height_inch);
        tilHeightCm = findViewById(R.id.til_height_cm);
        layoutHeightFeet = findViewById(R.id.layout_height_feet);
        btnUnitCm = findViewById(R.id.btn_unit_cm);
        btnUnitFeet = findViewById(R.id.btn_unit_feet);

        spinnerBloodType = findViewById(R.id.spinner_blood_type);
        tvAvatarInitials = findViewById(R.id.tv_avatar_initials);
        tvProfileName = findViewById(R.id.tv_profile_name);
        ivAvatarImage = findViewById(R.id.iv_avatar_image);

        boolean isBn = LanguageHelper.isBangla(this);

        // Field hints
        etName.setHint(isBn ? "আপনার নাম" : "John Doe");
        etAge.setHint(isBn ? "বয়স লিখুন" : "Enter age");
        etHeight.setHint(isBn ? "উচ্চতা সেমি তে (যেমন ১৭০)" : "Height in cm (e.g. 170)");
        etHeightFeet.setHint(isBn ? "ফুট" : "Feet");
        etHeightInch.setHint(isBn ? "ইঞ্চি" : "Inch");
        etWeight.setHint(isBn ? "৭৫" : "75");

        btnUnitCm.setOnClickListener(v -> setHeightUnit(true));
        btnUnitFeet.setOnClickListener(v -> setHeightUnit(false));

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.blood_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBloodType.setAdapter(adapter);

        etName.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            @Override
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                updateAvatar(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) handlePickedImage(uri);
        });

        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> photoPickerLauncher.launch(new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));

        AppPrefs prefs = new AppPrefs(this);
        if (prefs.isProfileDone()) {
            etName.setText(prefs.getName());
            if (prefs.getAge() > 0) etAge.setText(String.valueOf(prefs.getAge()));
            // When loading saved profile height, only populate CM field if CM mode is active
            if (prefs.getHeight() > 0) {
                if (isUnitCm) {
                    etHeight.setText(String.valueOf(prefs.getHeight()));
                } else {
                    float totalInches = prefs.getHeight() / 2.54f;
                    int feet = (int) (totalInches / 12);
                    float inches = totalInches - (feet * 12);
                    etHeightFeet.setText(String.valueOf(feet));
                    etHeightInch.setText(String.format(java.util.Locale.getDefault(), "%.1f", inches));
                }
            }
            if (prefs.getWeight() > 0) etWeight.setText(String.valueOf(prefs.getWeight()));
            String savedPic = prefs.getProfilePicBase64();
            if (savedPic != null && !savedPic.isEmpty()) loadBase64IntoImageView(savedPic);
        }

        setHeightUnit(false); // default  Feet & Inch

        findViewById(R.id.btn_save_profile).setOnClickListener(v -> saveProfile());
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
    }

    private void setHeightUnit(boolean cm) {
        isUnitCm = cm;
        btnUnitCm.setBackgroundResource(cm ? R.drawable.rounded_button : android.R.color.transparent);
        btnUnitCm.setTextColor(cm ? 0xFFFFFFFF : getColor(R.color.text_secondary));
        btnUnitFeet.setBackgroundResource(!cm ? R.drawable.rounded_button : android.R.color.transparent);
        btnUnitFeet.setTextColor(!cm ? 0xFFFFFFFF : getColor(R.color.text_secondary));
        tilHeightCm.setVisibility(cm ? View.VISIBLE : View.GONE);
        layoutHeightFeet.setVisibility(cm ? View.GONE : View.VISIBLE);
    }

    /**
     * Returns height in CM regardless of which unit the user picked
     */
    private float resolveHeightCm() {
        if (isUnitCm) {
            String s = etHeight.getText() != null ? etHeight.getText().toString().trim() : "";
            return s.isEmpty() ? 0 : Float.parseFloat(s);
        } else {
            String fStr = etHeightFeet.getText() != null ? etHeightFeet.getText().toString().trim() : "0";
            String iStr = etHeightInch.getText() != null ? etHeightInch.getText().toString().trim() : "0";
            float feet = fStr.isEmpty() ? 0 : Float.parseFloat(fStr);
            float inches = iStr.isEmpty() ? 0 : Float.parseFloat(iStr);
            return (feet * 12f + inches) * 2.54f;
        }
    }

    private void handlePickedImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(inputStream);
            Bitmap resized = Bitmap.createScaledBitmap(original, 300, 300, true);
            ivAvatarImage.setImageBitmap(resized);
            ivAvatarImage.setVisibility(View.VISIBLE);
            tvAvatarInitials.setVisibility(View.GONE);
            new AppPrefs(this).setProfilePicBase64(bitmapToBase64(resized));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
    }

    private void loadBase64IntoImageView(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            ivAvatarImage.setImageBitmap(bitmap);
            ivAvatarImage.setVisibility(View.VISIBLE);
            tvAvatarInitials.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAvatar(String name) {
        if (name.isEmpty()) return;
        String[] parts = name.trim().split(" ");
        String initials = parts.length >= 2 ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0) : String.valueOf(parts[0].charAt(0));
        tvAvatarInitials.setText(initials.toUpperCase());
        tvProfileName.setText(name);
    }

    @SuppressLint("DefaultLocale")
    private void saveProfile() {
        String name = Objects.requireNonNull(etName.getText()).toString().trim();
        String ageStr = Objects.requireNonNull(etAge.getText()).toString().trim();
        String weightStr = Objects.requireNonNull(etWeight.getText()).toString().trim();

        boolean isBn = LanguageHelper.isBangla(this);

        if (TextUtils.isEmpty(name)) {
            etName.setError(isBn ? "নাম আবশ্যক" : "Name is required");
            return;
        }

        if (!isUnitCm) {
            String fStr = etHeightFeet.getText() != null ? etHeightFeet.getText().toString().trim() : "";
            if (fStr.isEmpty()) {
                etHeightFeet.setError(isBn ? "ফুট লিখুন" : "Enter feet");
                return;
            }
        }

        float heightCm = resolveHeightCm();

        AppPrefs prefs = new AppPrefs(this);
        prefs.setName(name);
        if (!ageStr.isEmpty()) prefs.setAge(Integer.parseInt(ageStr));
        if (heightCm > 0) prefs.setHeight(heightCm);
        if (!weightStr.isEmpty()) prefs.setWeight(Float.parseFloat(weightStr));

        String bloodType = spinnerBloodType.getSelectedItem().toString();
        if (!bloodType.startsWith("Select")) prefs.setBloodType(bloodType);

        prefs.setProfileDone(true);

        PremiumSnackbar.show(
                findViewById(android.R.id.content),
                isBn ? "প্রোফাইল সংরক্ষিত হয়েছে!" : "Profile saved successfully!",
                SnackbarType.SUCCESS
        );

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1500);
    }
}