package com.xerotrust.vitaltrack.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class WeeklyStepsChartView extends View {

    public interface OnDayClickListener {
        void onDayClicked(int dayIndex);
    }

    // ─── Paints ───────────────────────────────────────────────────────────────
    private final Paint barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barWalkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barRunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stepsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ─── Data ─────────────────────────────────────────────────────────────────
    private int[] walkSteps = new int[7];
    private int[] runSteps = new int[7];
    private int[] goals = new int[7];
    private String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
    private String currentMode = "walk"; // "walk" or "run"

    private int selectedDay = 6; // default today
    private OnDayClickListener clickListener;

    // ─── Geometry ─────────────────────────────────────────────────────────────
    private final float[] barCenterX = new float[7];
    private float barWidth;
    private float chartTop, chartBottom, chartLeft, chartRight;

    // ─── Colors ───────────────────────────────────────────────────────────────
    private static final int COLOR_WALK_TOP = 0xFF34D399; // emerald-400
    private static final int COLOR_WALK_BOTTOM = 0xFF059669; // emerald-600
    private static final int COLOR_RUN_TOP = 0xFF60A5FA; // blue-400
    private static final int COLOR_RUN_BOTTOM = 0xFF1565C0; // blue-700
    private static final int COLOR_BAR_BG = 0xFFF1F5F9;
    private static final int COLOR_SELECTED_BG = 0xFFE8F5E9;
    private static final int COLOR_GOAL_LINE = 0xFFEF4444;
    private static final int COLOR_LABEL = 0xFF9CA3AF;
    private static final int COLOR_TODAY_LABEL = 0xFF1565C0;
    private static final int COLOR_STEPS_TEXT = 0xFF374151;

    public WeeklyStepsChartView(Context context) {
        super(context);
        init();
    }

    public WeeklyStepsChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeeklyStepsChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barBgPaint.setColor(COLOR_BAR_BG);
        barBgPaint.setStyle(Paint.Style.FILL);

        // Walk & run paints — shader set in onDraw after geometry known
        barWalkPaint.setStyle(Paint.Style.FILL);
        barRunPaint.setStyle(Paint.Style.FILL);

        // Glow under active bars
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setAlpha(40);

        selectedPaint.setColor(COLOR_SELECTED_BG);
        selectedPaint.setStyle(Paint.Style.FILL);

        goalPaint.setColor(COLOR_GOAL_LINE);
        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(dp(1.5f));
        goalPaint.setAlpha(160);

        labelPaint.setColor(COLOR_LABEL);
        labelPaint.setTextSize(sp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        todayLabelPaint.setColor(COLOR_TODAY_LABEL);
        todayLabelPaint.setTextSize(sp(10));
        todayLabelPaint.setTextAlign(Paint.Align.CENTER);
        todayLabelPaint.setFakeBoldText(true);

        stepsPaint.setColor(COLOR_STEPS_TEXT);
        stepsPaint.setTextSize(sp(8));
        stepsPaint.setTextAlign(Paint.Align.CENTER);
    }

    // ─── Public setters ───────────────────────────────────────────────────────

    public void setCurrentMode(String mode) {
        this.currentMode = (mode != null) ? mode : "walk";
        invalidate();
    }

    public void setDayLabels(String[] labels) {
        if (labels != null && labels.length == 7) dayLabels = labels;
        invalidate();
    }

    public void setWeeklyData(int[] walkArr, int[] runArr, int[] goalArr) {
        if (walkArr != null && walkArr.length == 7) this.walkSteps = walkArr.clone();
        if (runArr != null && runArr.length == 7) this.runSteps = runArr.clone();
        if (goalArr != null && goalArr.length == 7) this.goals = goalArr.clone();
        invalidate();
    }

    /** Backward-compat: called from StepCounterActivity with walk-only history */
    public void setWeeklySteps(int[] stepsMonToSun) {
        if (stepsMonToSun == null || stepsMonToSun.length != 7) return;
        setWeeklyData(stepsMonToSun, new int[7], new int[7]);
    }

    public void setOnDayClickListener(OnDayClickListener l) {
        clickListener = l;
    }

    // ─── Geometry ─────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        float padL = dp(6), padR = dp(6), padT = dp(16), padB = dp(30);
        chartLeft = padL;
        chartRight = w - padR;
        chartTop = padT;
        chartBottom = h - padB;

        float slotW = (chartRight - chartLeft) / 7f;
        barWidth = slotW * 0.48f;

        for (int i = 0; i < 7; i++) {
            barCenterX[i] = chartLeft + slotW * i + slotW / 2f;
        }
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float chartH = chartBottom - chartTop;
        float slotW = (chartRight - chartLeft) / 7f;
        float radius = dp(7);

        // Find max for scaling — only count active mode steps
        boolean isWalkMode = "walk".equals(currentMode);
        int maxVal = 1;
        for (int i = 0; i < 7; i++) {
            int modeSteps = isWalkMode ? walkSteps[i] : runSteps[i];
            maxVal = Math.max(maxVal, modeSteps);
            maxVal = Math.max(maxVal, goals[i]);
        }
        maxVal = (int) (maxVal * 1.1f); // 10% headroom so bars don't touch top

        for (int i = 0; i < 7; i++) {
            float cx = barCenterX[i];
            float left = cx - barWidth / 2f;
            float right = cx + barWidth / 2f;

            // ── Selected column highlight ──────────────────────────────
            if (i == selectedDay) {
                RectF hlRect = new RectF(cx - slotW / 2f + dp(3), chartTop - dp(10), cx + slotW / 2f - dp(3), chartBottom + dp(20));
                canvas.drawRoundRect(hlRect, dp(10), dp(10), selectedPaint);
            }

            // ── Background track ──────────────────────────────────────
            canvas.drawRoundRect(left, chartTop, right, chartBottom, radius, radius, barBgPaint);

            int modeSteps = isWalkMode ? walkSteps[i] : runSteps[i];
            float modeH = (modeSteps / (float) maxVal) * chartH;

            // ── Active mode bar ───────────────────────────────────────
            if (modeSteps > 0) {
                float top = chartBottom - modeH;
                float drawTop = Math.min(top, chartBottom - dp(4));

                if (isWalkMode) {
                    barWalkPaint.setShader(new LinearGradient(0, drawTop, 0, chartBottom, COLOR_WALK_TOP, COLOR_WALK_BOTTOM, Shader.TileMode.CLAMP));
                    RectF rect = new RectF(left, drawTop, right, chartBottom);
                    canvas.drawRoundRect(rect, radius, radius, barWalkPaint);
                    glowPaint.setColor(COLOR_WALK_BOTTOM & 0x00FFFFFF | 0x20000000);
                } else {
                    barRunPaint.setShader(new LinearGradient(0, drawTop, 0, chartBottom, COLOR_RUN_TOP, COLOR_RUN_BOTTOM, Shader.TileMode.CLAMP));
                    RectF rect = new RectF(left, drawTop, right, chartBottom);
                    canvas.drawRoundRect(rect, radius, radius, barRunPaint);
                    glowPaint.setColor(COLOR_RUN_BOTTOM & 0x00FFFFFF | 0x20000000);
                }
                canvas.drawRoundRect(new RectF(left - dp(2), chartBottom - dp(6), right + dp(2), chartBottom + dp(3)), dp(4), dp(4), glowPaint);
            }

            // ── Step count label above bar ────────────────────────────
            if (modeSteps > 0 && i == selectedDay) {
                String label = modeSteps >= 1000 ? String.format("%.1fk", modeSteps / 1000f) : String.valueOf(modeSteps);
                float labelY = chartBottom - modeH - dp(5);
                if (labelY > chartTop + dp(12)) {
                    canvas.drawText(label, cx, labelY, stepsPaint);
                }
            }

            // ── Goal line ─────────────────────────────────────────────
            if (goals[i] > 0) {
                float goalY = chartBottom - (goals[i] / (float) maxVal) * chartH;
                if (goalY >= chartTop && goalY <= chartBottom) {
                    canvas.drawLine(left - dp(2), goalY, right + dp(2), goalY, goalPaint);
                }
            }

            // ── Day label ─────────────────────────────────────────────
            Paint lp = (i == 6) ? todayLabelPaint : labelPaint;
            canvas.drawText(dayLabels[i], cx, chartBottom + dp(20), lp);

            // ── Today dot indicator ───────────────────────────────────
            if (i == 6) {
                Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                dotPaint.setColor(COLOR_TODAY_LABEL);
                canvas.drawCircle(cx, chartBottom + dp(26), dp(2f), dotPaint);
            }
        }
    }

    // ─── Touch ────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && clickListener != null) {
            float x = event.getX();
            float slotW = (chartRight - chartLeft) / 7f;
            int idx = (int) ((x - chartLeft) / slotW);
            if (idx >= 0 && idx < 7) {
                selectedDay = idx;
                invalidate();
                clickListener.onDayClicked(idx);
            }
        }
        return true;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private float sp(float v) {
        return v * getResources().getDisplayMetrics().scaledDensity;
    }
}