package com.xerotrust.vitaltrack.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.xerotrust.vitaltrack.R;

public class VibrationDialogFragment extends DialogFragment {

    public interface OnVibrationSelectedListener {
        void onVibrationSelected(String mode); // "normal" or "alarm"
    }

    private static final String ARG_MEDICINE_NAME = "medicine_name";
    private OnVibrationSelectedListener listener;
    private String selectedMode = "normal";

    private LinearLayout optionNormal, optionAlarm;
    private View checkNormal, checkAlarm;
    private Button btnSave;

    public static VibrationDialogFragment newInstance(String medicineName) {
        VibrationDialogFragment f = new VibrationDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MEDICINE_NAME, medicineName);
        f.setArguments(args);
        return f;
    }

    public void setOnVibrationSelectedListener(OnVibrationSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.VibrationDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_vibration_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String medicineName = getArguments() != null ? getArguments().getString(ARG_MEDICINE_NAME, "") : "";

        optionNormal = view.findViewById(R.id.option_normal);
        optionAlarm = view.findViewById(R.id.option_alarm);
        checkNormal = view.findViewById(R.id.check_normal);
        checkAlarm = view.findViewById(R.id.check_alarm);
        btnSave = view.findViewById(R.id.btn_save_vibration);
        ImageButton playNormal = view.findViewById(R.id.btn_play_normal);
        ImageButton playAlarm = view.findViewById(R.id.btn_play_alarm);

        // Default: normal selected
        selectMode("normal");

        optionNormal.setOnClickListener(v -> selectMode("normal"));
        optionAlarm.setOnClickListener(v -> selectMode("alarm"));

        playNormal.setOnClickListener(v -> vibrate("normal"));
        playAlarm.setOnClickListener(v -> vibrate("alarm"));

        btnSave.setOnClickListener(v -> {
            if (listener != null) listener.onVibrationSelected(selectedMode);
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.88);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void selectMode(String mode) {
        selectedMode = mode;
        boolean isNormal = mode.equals("normal");

        optionNormal.setSelected(isNormal);
        optionAlarm.setSelected(!isNormal);

        // Teal border when selected, dark otherwise
        optionNormal.setBackgroundResource(isNormal ? R.drawable.bg_vibration_option_selected : R.drawable.bg_vibration_option);
        optionAlarm.setBackgroundResource(!isNormal ? R.drawable.bg_vibration_option_selected : R.drawable.bg_vibration_option);

        checkNormal.setAlpha(isNormal ? 1f : 0.3f);
        checkAlarm.setAlpha(!isNormal ? 1f : 0.3f);
    }

    private void vibrate(String mode) {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = mode.equals("alarm") ? new long[]{0, 500, 200, 500, 200, 500} : new long[]{0, 300, 200, 300};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }
}