package com.xerotrust.vitaltrack.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.xerotrust.vitaltrack.R;

public class PremiumSnackbar {

    private static final int DISPLAY_DURATION_MS = 2800;
    private static final int ANIM_DURATION_MS = 400;

    public static void show(View anchorView, String message, SnackbarType type) {
        Context ctx = anchorView.getContext();

        int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
        int sideMargin = dpToPx(ctx, 16);
        int popupWidth = screenWidth - (sideMargin * 2);

        // ── Root linear layout (this is what PopupWindow sizes to) ────────────
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(buildCardBackground(ctx, type));
        root.setElevation(dpToPx(ctx, 20));

        // Inner row
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dpToPx(ctx, 14);
        int padV = dpToPx(ctx, 12);
        row.setPadding(padH, padV, padH, padV);

        // ── Left accent bar ───────────────────────────────────────────────────
        GradientDrawable accentBar = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{lighten(baseColor(type)), baseColor(type)});
        accentBar.setCornerRadius(dpToPx(ctx, 3));
        View accentView = new View(ctx);
        accentView.setBackground(accentBar);
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dpToPx(ctx, 3), dpToPx(ctx, 36));
        accentLp.setMarginEnd(dpToPx(ctx, 12));
        row.addView(accentView, accentLp);

        // ── Icon circle ───────────────────────────────────────────────────────
        FrameLayout iconCircle = new FrameLayout(ctx);
        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(blendWithAlpha(baseColor(type), 0x44));
        circleBg.setStroke(dpToPx(ctx, 1), blendWithAlpha(lighten(baseColor(type)), 0xAA));
        iconCircle.setBackground(circleBg);

        TextView tvIcon = new TextView(ctx);
        tvIcon.setText(iconFor(type));
        tvIcon.setTextSize(14);
        tvIcon.setGravity(Gravity.CENTER);
        tvIcon.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        tvIcon.setTextColor(Color.WHITE);
        FrameLayout.LayoutParams iconTextLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        iconTextLp.gravity = Gravity.CENTER;
        iconCircle.addView(tvIcon, iconTextLp);

        int circleSize = dpToPx(ctx, 32);
        LinearLayout.LayoutParams circleLp = new LinearLayout.LayoutParams(circleSize, circleSize);
        circleLp.setMarginEnd(dpToPx(ctx, 12));
        row.addView(iconCircle, circleLp);

        // ── Message ───────────────────────────────────────────────────────────
        TextView tvMsg = new TextView(ctx);
        tvMsg.setText(message);
        tvMsg.setTextColor(Color.WHITE);
        tvMsg.setTextSize(13.5f);
        tvMsg.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvMsg.setLetterSpacing(0.01f);
        tvMsg.setMaxLines(3);
        LinearLayout.LayoutParams msgLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(tvMsg, msgLp);

        root.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Shimmer line at very top ──────────────────────────────────────────
        GradientDrawable shimmer = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{Color.TRANSPARENT, blendWithAlpha(lighten(baseColor(type)), 0x88), blendWithAlpha(Color.WHITE, 0x55), blendWithAlpha(lighten(baseColor(type)), 0x88), Color.TRANSPARENT});
        View shimmerView = new View(ctx);
        shimmerView.setBackground(shimmer);
        root.addView(shimmerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(ctx, 1)));

        // ── PopupWindow ───────────────────────────────────────────────────────
        root.measure(View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int popupHeight = root.getMeasuredHeight();

        PopupWindow popup = new PopupWindow(root, popupWidth, popupHeight, false);
        popup.setElevation(dpToPx(ctx, 20));
        popup.setClippingEnabled(false);
        popup.setBackgroundDrawable(null); // prevent default white bg

        popup.showAtLocation(anchorView, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, dpToPx(ctx, 52));

        // ── Animate IN ────────────────────────────────────────────────────────
        root.setAlpha(0f);
        root.setTranslationY(-dpToPx(ctx, 48));

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(root, "alpha", 0f, 1f);
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(root, "translationY", -dpToPx(ctx, 48), 0f);
        fadeIn.setDuration(ANIM_DURATION_MS);
        slideIn.setDuration(ANIM_DURATION_MS);
        fadeIn.setInterpolator(new DecelerateInterpolator(2.2f));
        slideIn.setInterpolator(new DecelerateInterpolator(2.2f));
        fadeIn.start();
        slideIn.start();

        // ── Animate OUT ───────────────────────────────────────────────────────
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(root, "alpha", 1f, 0f);
            ObjectAnimator slideOut = ObjectAnimator.ofFloat(root, "translationY", 0f, -dpToPx(ctx, 48));
            fadeOut.setDuration(ANIM_DURATION_MS);
            slideOut.setDuration(ANIM_DURATION_MS);
            fadeOut.setInterpolator(new AccelerateInterpolator(2.2f));
            slideOut.setInterpolator(new AccelerateInterpolator(2.2f));

            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    try {
                        popup.dismiss();
                    } catch (Exception ignored) {
                    }
                }
            });

            fadeOut.start();
            slideOut.start();
        }, DISPLAY_DURATION_MS);
    }

    // ── Drawables ─────────────────────────────────────────────────────────────

    private static android.graphics.drawable.Drawable buildCardBackground(Context ctx, SnackbarType type) {

        android.graphics.drawable.Drawable base = ContextCompat.getDrawable(ctx, R.drawable.bg_premium_snackbar);

        GradientDrawable tint = new GradientDrawable();
        tint.setShape(GradientDrawable.RECTANGLE);
        tint.setCornerRadius(dpToPx(ctx, 18));
        tint.setColor(blendWithAlpha(baseColor(type), 0x40));

        return new LayerDrawable(new android.graphics.drawable.Drawable[]{base, tint});
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int baseColor(SnackbarType type) {
        switch (type) {
            case SUCCESS:
                return Color.parseColor("#16A34A");
            case ERROR:
                return Color.parseColor("#DC2626");
            case WARNING:
                return Color.parseColor("#D97706");
            case INFO:
            default:
                return Color.parseColor("#2563EB");
        }
    }

    private static String iconFor(SnackbarType type) {
        switch (type) {
            case SUCCESS:
                return "✓";
            case ERROR:
                return "✕";
            case WARNING:
                return "⚠";
            case INFO:
            default:
                return "ℹ";
        }
    }

    private static int blendWithAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static int lighten(int color) {
        int r = Math.min(255, (int) (Color.red(color) * 1.5f + 70));
        int g = Math.min(255, (int) (Color.green(color) * 1.5f + 70));
        int b = Math.min(255, (int) (Color.blue(color) * 1.5f + 70));
        return Color.rgb(r, g, b);
    }

    private static int dpToPx(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }
}