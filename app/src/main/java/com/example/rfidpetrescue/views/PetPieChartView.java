package com.example.rfidpetrescue.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

public class PetPieChartView extends View {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF rectF = new RectF();

    private float treatingPercent = 0f;
    private float adoptedPercent = 0f;
    private float readyPercent = 0f;
    private float waitPercent = 0f;
    private float interviewPercent = 0f;

    private float animProgress = 0f;

    // Mã màu chính xác từ thiết kế
    private final int COLOR_TREATING = Color.parseColor("#17118E");   // Xanh đậm
    private final int COLOR_ADOPTED = Color.parseColor("#5752BC");    // Xanh trung bình
    private final int COLOR_READY = Color.parseColor("#C4BAEE");      // Tím nhạt
    private final int COLOR_WAIT = Color.parseColor("#E6E2F5");       // Tím rất nhạt
    private final int COLOR_INTERVIEW = Color.parseColor("#D1D1D1");  // Xám

    public PetPieChartView(Context context) {
        super(context);
    }

    public PetPieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(float treating, float adopted, float ready, float wait, float interview) {
        this.treatingPercent = treating;
        this.adoptedPercent = adopted;
        this.readyPercent = ready;
        this.waitPercent = wait;
        this.interviewPercent = interview;
        startChartAnimation();
    }

    public void startChartAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1200); // 1.2 giây
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float height = getHeight();

        // Tâm biểu đồ khớp với tâm nút tròn đen (25dp từ lề trái của view)
        float centerX = dpToPx(25);
        float centerY = height / 2f;

        // Bán kính và độ dày vòng donut
        float radius = dpToPx(85);
        float strokeWidth = dpToPx(55);

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStrokeCap(Paint.Cap.BUTT);

        float totalWeight = treatingPercent + adoptedPercent + readyPercent + waitPercent + interviewPercent;
        if (totalWeight <= 0) return;

        // --- ĐÃ SỬA GÓC TẠI ĐÂY ---
        // Bắt đầu từ thẳng đứng bên trên (-90 độ) và quét vừa đủ nửa hình tròn (180 độ)
        float startAngle = -90f;
        float totalSweep = 180f * animProgress;

        startAngle = drawSegment(canvas, startAngle, (treatingPercent / totalWeight) * totalSweep, COLOR_TREATING);
        startAngle = drawSegment(canvas, startAngle, (adoptedPercent / totalWeight) * totalSweep, COLOR_ADOPTED);
        startAngle = drawSegment(canvas, startAngle, (readyPercent / totalWeight) * totalSweep, COLOR_READY);
        startAngle = drawSegment(canvas, startAngle, (waitPercent / totalWeight) * totalSweep, COLOR_WAIT);
        drawSegment(canvas, startAngle, (interviewPercent / totalWeight) * totalSweep, COLOR_INTERVIEW);
    }

    private float drawSegment(Canvas canvas, float startAngle, float sweepAngle, int color) {
        if (sweepAngle <= 0) return startAngle;
        paint.setColor(color);
        canvas.drawArc(rectF, startAngle, sweepAngle, false, paint);
        return startAngle + sweepAngle;
    }
}