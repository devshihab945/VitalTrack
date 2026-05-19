package com.xerotrust.vitaltrack.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.annotations.SerializedName;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public class ProfileFragment extends Fragment {

    private TextView tvAvatar, tvProfileName, tvNameVal, tvAgeVal, tvHeightVal, tvWeightVal, tvBloodVal;
    private ImageView ivAvatarImage;
    private TextInputEditText etName, etAge, etWeight, etBlood;
    private TextInputEditText etHeightCm;
    private TextInputEditText etHeightFeet, etHeightInch;
    private TextInputLayout tilHeightCm;
    private View layoutHeightFeet;
    private TextView btnUnitCm, btnUnitFeet;

    private CardView cardView, cardEdit;
    private CardView btnEdit, btnSave, btnCancel;
    private CardView btnUnsubscribe;          // ← NEW

    private AppPrefs prefs;
    private boolean isUnitCm = false;

    // Unsubscribe API
    private Retrofit retrofit;
    private ApiService apiService;

    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = new AppPrefs(requireContext());

        // ── Bind views ───────────────────────────────────────────────
        tvAvatar      = view.findViewById(R.id.tv_avatar);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvNameVal     = view.findViewById(R.id.tv_name_val);
        tvAgeVal      = view.findViewById(R.id.tv_age_val);
        tvHeightVal   = view.findViewById(R.id.tv_height_val);
        tvWeightVal   = view.findViewById(R.id.tv_weight_val);
        tvBloodVal    = view.findViewById(R.id.tv_blood_val);
        ivAvatarImage = view.findViewById(R.id.iv_avatar_image);

        etName        = view.findViewById(R.id.et_name);
        etAge         = view.findViewById(R.id.et_age);
        etWeight      = view.findViewById(R.id.et_weight);
        etBlood       = view.findViewById(R.id.et_blood);
        etHeightCm    = view.findViewById(R.id.et_height_cm);
        etHeightFeet  = view.findViewById(R.id.et_height_feet);
        etHeightInch  = view.findViewById(R.id.et_height_inch);
        tilHeightCm   = view.findViewById(R.id.til_height_cm);
        layoutHeightFeet = view.findViewById(R.id.layout_height_feet);
        btnUnitCm     = view.findViewById(R.id.btn_unit_cm);
        btnUnitFeet   = view.findViewById(R.id.btn_unit_feet);

        cardView      = view.findViewById(R.id.card_view);
        cardEdit      = view.findViewById(R.id.card_edit);
        btnEdit       = view.findViewById(R.id.btn_edit);
        btnSave       = view.findViewById(R.id.btn_save);
        btnCancel     = view.findViewById(R.id.btn_cancel);

        // ── Retrofit setup ───────────────────────────────────────────
        retrofit = new Retrofit.Builder()
                .baseUrl("https://lisenar.xyz/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // ── Unit toggle ──────────────────────────────────────────────
        btnUnitCm.setOnClickListener(v -> setHeightUnit(true));
        btnUnitFeet.setOnClickListener(v -> setHeightUnit(false));

        // ── Photo picker ─────────────────────────────────────────────
        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) handlePickedImage(uri);
                });

        view.findViewById(R.id.btn_change_avatar).setOnClickListener(v ->
                photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        loadProfile();

        btnEdit.setOnClickListener(v -> switchToEditMode());
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> switchToViewMode());

    }

    // ─────────────────────────────────────────────────────────────────────────
    // Height unit toggle
    // ─────────────────────────────────────────────────────────────────────────

    private void setHeightUnit(boolean cm) {
        isUnitCm = cm;
        btnUnitCm.setBackgroundResource(cm ? R.drawable.rounded_button : android.R.color.transparent);
        btnUnitCm.setTextColor(cm ? 0xFFFFFFFF : requireContext().getColor(R.color.text_secondary));
        btnUnitFeet.setBackgroundResource(!cm ? R.drawable.rounded_button : android.R.color.transparent);
        btnUnitFeet.setTextColor(!cm ? 0xFFFFFFFF : requireContext().getColor(R.color.text_secondary));
        tilHeightCm.setVisibility(cm ? View.VISIBLE : View.GONE);
        layoutHeightFeet.setVisibility(cm ? View.GONE : View.VISIBLE);
    }

    private float resolveHeightCm() {
        if (isUnitCm) {
            String s = etHeightCm.getText() != null ? etHeightCm.getText().toString().trim() : "";
            return s.isEmpty() ? 0 : Float.parseFloat(s);
        } else {
            String fStr = etHeightFeet.getText() != null ? etHeightFeet.getText().toString().trim() : "0";
            String iStr = etHeightInch.getText() != null ? etHeightInch.getText().toString().trim() : "0";
            float feet   = fStr.isEmpty() ? 0 : Float.parseFloat(fStr);
            float inches = iStr.isEmpty() ? 0 : Float.parseFloat(iStr);
            return (feet * 12f + inches) * 2.54f;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Height display helper
    // ─────────────────────────────────────────────────────────────────────────

    private String formatHeight(float cm) {
        if (cm <= 0) return "--";
        float totalInches = cm / 2.54f;
        int feet          = (int) (totalInches / 12);
        int inches        = Math.round(totalInches - feet * 12);
        return feet + " ft " + inches + " in  |  " + Math.round(cm) + " cm";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Image helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void handlePickedImage(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap original = BitmapFactory.decodeStream(is);
            Bitmap resized  = Bitmap.createScaledBitmap(original, 300, 300, true);
            ivAvatarImage.setImageBitmap(resized);
            ivAvatarImage.setVisibility(View.VISIBLE);
            tvAvatar.setVisibility(View.GONE);
            prefs.setProfilePicBase64(bitmapToBase64(resized));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "ছবি লোড করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show();
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
            tvAvatar.setVisibility(View.GONE);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Profile CRUD
    // ─────────────────────────────────────────────────────────────────────────

    private void loadProfile() {
        String name = prefs.getName();
        if (name.isEmpty()) name = "User";

        tvProfileName.setText(name);
        tvNameVal.setText(name);
        tvAgeVal.setText(prefs.getAge() > 0 ? prefs.getAge() + " বছর" : "--");
        tvHeightVal.setText(formatHeight(prefs.getHeight()));
        tvWeightVal.setText(prefs.getWeight() > 0 ? prefs.getWeight() + " kg" : "--");
        tvBloodVal.setText(prefs.getBloodType().isEmpty() ? "--" : prefs.getBloodType());

        String pic = prefs.getProfilePicBase64();
        if (pic != null && !pic.isEmpty()) {
            loadBase64IntoImageView(pic);
        } else {
            ivAvatarImage.setVisibility(View.GONE);
            tvAvatar.setVisibility(View.VISIBLE);
            tvAvatar.setText(getInitials(name));
        }

    }

    private void switchToEditMode() {
        cardView.setVisibility(View.GONE);
        cardEdit.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.GONE);
        btnUnsubscribe.setVisibility(View.GONE);   // hide unsubscribe in edit mode

        etName.setText(prefs.getName());
        etAge.setText(prefs.getAge() > 0 ? String.valueOf(prefs.getAge()) : "");
        etWeight.setText(prefs.getWeight() > 0 ? String.valueOf(prefs.getWeight()) : "");
        etBlood.setText(prefs.getBloodType());

        setHeightUnit(false);
        float savedCm = prefs.getHeight();
        if (savedCm > 0) {
            float totalInches = savedCm / 2.54f;
            int feet   = (int) (totalInches / 12);
            float inch = totalInches - (feet * 12);
            etHeightFeet.setText(String.valueOf(feet));
            etHeightInch.setText(String.format(java.util.Locale.getDefault(), "%.1f", inch));
        }
    }

    private void switchToViewMode() {
        cardView.setVisibility(View.VISIBLE);
        cardEdit.setVisibility(View.GONE);
        btnEdit.setVisibility(View.VISIBLE);
        btnUnsubscribe.setVisibility(View.VISIBLE);  // show again
    }

    private void saveProfile() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        if (name.isEmpty()) { etName.setError("নাম আবশ্যক"); return; }

        if (!isUnitCm) {
            String fStr = etHeightFeet.getText() != null ? etHeightFeet.getText().toString().trim() : "";
            if (fStr.isEmpty()) { etHeightFeet.setError("ফুট লিখুন"); return; }
        }

        prefs.setName(name);
        String age    = etAge.getText() != null ? etAge.getText().toString().trim() : "";
        String weight = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
        String blood  = etBlood.getText() != null ? etBlood.getText().toString().trim() : "";

        if (!age.isEmpty())    prefs.setAge(Integer.parseInt(age));
        if (!weight.isEmpty()) prefs.setWeight(Float.parseFloat(weight));
        if (!blood.isEmpty())  prefs.setBloodType(blood);

        float h = resolveHeightCm();
        if (h > 0) prefs.setHeight(h);

        loadProfile();
        switchToViewMode();
        Toast.makeText(requireContext(), "প্রোফাইল আপডেট হয়েছে!", Toast.LENGTH_SHORT).show();
    }

    private String getInitials(String name) {
        String[] parts = name.trim().split(" ");
        if (parts.length >= 2)
            return String.valueOf(parts[0].charAt(0)).toUpperCase()
                    + String.valueOf(parts[1].charAt(0)).toUpperCase();
        return (parts.length > 0 && !parts[0].isEmpty())
                ? String.valueOf(parts[0].charAt(0)).toUpperCase() : "U";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Retrofit
    // ─────────────────────────────────────────────────────────────────────────

    interface ApiService {
        @FormUrlEncoded
        @POST("unsubscription")
        Call<UnsubResponse> unsubscription(
                @Field("mobile") String mobile,
                @Field("app_id") String appId
        );
    }

    public static class UnsubResponse {
        @SerializedName("statusCode")   private String statusCode;
        @SerializedName("statusDetail") private String statusDetail;
        @SerializedName("subscriberId") private String subscriberId;

        public String getStatusCode()   { return statusCode; }
        public String getStatusDetail() { return statusDetail; }
        public String getSubscriberId() { return subscriberId; }
    }
}
