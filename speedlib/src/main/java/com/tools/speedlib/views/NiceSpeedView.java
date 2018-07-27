package com.tools.speedlib.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;

import com.tools.speedlib.R;
import com.tools.speedlib.views.base.Speedometer;
import com.tools.speedlib.views.base.SpeedometerDefault;
import com.tools.speedlib.views.components.Indicators.ImageIndicator;

/**
 * this Library build By Anas Altair
 * see it on <a href="https://github.com/anastr/SpeedView">GitHub</a>
 */
public class NiceSpeedView extends Speedometer {

    private Path markPath = new Path();
    private Paint speedometerPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            speedometer2Paint = new Paint(Paint.ANTI_ALIAS_FLAG),
            pointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            pointerBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG),
            markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF speedometerRect = new RectF();

    private int speedometerColor = Color.parseColor("#00bafe"), pointerColor = Color.WHITE;
    private Drawable imageSpeedometer;

    public NiceSpeedView(Context context) {
        this(context, null);
    }

    public NiceSpeedView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NiceSpeedView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        initAttributeSet(context, attrs);
    }

    @Override
    protected void defaultValues() {
        super.setTextColor(Color.BLACK);
        super.setSpeedTextColor(Color.BLACK);
        super.setUnitTextColor(Color.BLACK);
        super.setSpeedTextSize(dpTOpx(20f));
        super.setUnitTextSize(dpTOpx(20f));
        super.setSpeedTextTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        super.setUnitUnderSpeedText(false);
    }

    @Override
    protected SpeedometerDefault getSpeedometerDefault() {
        SpeedometerDefault speedometerDefault = new SpeedometerDefault();
        //设置指针样式
        speedometerDefault.indicator = new ImageIndicator(getContext(), R.drawable.icon_speed_pointer);
        //设置背景颜色
        speedometerDefault.backgroundCircleColor = getResources().getColor(R.color.gray);
        //设置指示条宽度
        speedometerDefault.speedometerWidth = dpTOpx(1f);
        speedometerDefault.highSpeedColor = getResources().getColor(R.color.blue);
        setImageSpeedometer(R.drawable.img_speed_bg);
        return speedometerDefault;
    }

    /**
     * set background speedometer image, Preferably be square.
     *
     * @param imageResource image id.
     * @see #setImageSpeedometer(Drawable)
     */
    public void setImageSpeedometer(int imageResource) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setImageSpeedometer(getContext().getDrawable(imageResource));
        } else {
            setImageSpeedometer(getContext().getResources().getDrawable(imageResource));
        }
    }

    /**
     * set background speedometer image, Preferably be square.
     *
     * @param imageSpeedometer image drawable.
     * @see #setImageSpeedometer(int)
     */
    public void setImageSpeedometer(Drawable imageSpeedometer) {
        this.imageSpeedometer = imageSpeedometer;
        updateBackgroundBitmap();
    }

    private void init() {
        speedometerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        speedometerPaint.setStrokeCap(Paint.Cap.ROUND);
        speedometer2Paint.setStyle(Paint.Style.STROKE);
        speedometer2Paint.setStrokeCap(Paint.Cap.ROUND);
        markPaint.setStyle(Paint.Style.STROKE);
        markPaint.setStrokeCap(Paint.Cap.ROUND);
        markPaint.setStrokeWidth(dpTOpx(2));
        circlePaint.setColor(Color.WHITE);
    }

    private void initAttributeSet(Context context, AttributeSet attrs) {
        if (attrs == null) {
            initAttributeValue();
            return;
        }
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.PointerSpeedView, 0, 0);

        speedometerColor = a.getColor(R.styleable.PointerSpeedView_sv_speedometerColor, speedometerColor);
        pointerColor = a.getColor(R.styleable.PointerSpeedView_sv_pointerColor, pointerColor);
        circlePaint.setColor(a.getColor(R.styleable.PointerSpeedView_sv_centerCircleColor, circlePaint.getColor()));
        a.recycle();
        initAttributeValue();
    }

    private void initAttributeValue() {
        pointerPaint.setColor(pointerColor);
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        float risk = 0;
        speedometerRect.set(risk, risk, getSize() - risk, getSize() - risk);

        updateRadial();
        updateBackgroundBitmap();
    }

    private void initDraw() {
        speedometer2Paint.setColor(getResources().getColor(R.color.gray));
        speedometer2Paint.setStrokeWidth(getSpeedometerWidth());
        speedometerPaint.setStrokeWidth(getSpeedometerWidth());
        speedometerPaint.setColor(getResources().getColor(R.color.blue));
        markPaint.setColor(getMarkColor());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        initDraw();

        canvas.drawArc(speedometerRect, getStartDegree(), getEndDegree() - getStartDegree(), false, speedometer2Paint);

        canvas.drawArc(speedometerRect, getStartDegree()
                , (getEndDegree() - getStartDegree()) * getOffsetSpeed(), true, speedometerPaint);

        if (imageSpeedometer != null) {
            imageSpeedometer.setBounds((int) getViewLeft() + getPadding(), (int) getViewTop() + getPadding()
                    , (int) getViewRight() - getPadding(), (int) getViewBottom() - getPadding());
            imageSpeedometer.draw(canvas);
        }

        drawIndicator(canvas);
    }

    @Override
    protected void updateBackgroundBitmap() {
        Canvas c = createBackgroundBitmapCanvas();
        if (speedometerPaint == null) {
            return;
        }
        initDraw();

        markPath.reset();
        markPath.moveTo(getSize() * .5f, getSpeedometerWidth() + dpTOpx(8) + dpTOpx(4) + getPadding());
        markPath.lineTo(getSize() * .5f, getSpeedometerWidth() + dpTOpx(8) + dpTOpx(4) + getPadding() + getSize() / 60);

        if (imageSpeedometer != null) {
            imageSpeedometer.setBounds((int) getViewLeft() + getPadding(), (int) getViewTop() + getPadding()
                    , (int) getViewRight() - getPadding(), (int) getViewBottom() - getPadding());
            imageSpeedometer.draw(c);
        }
    }

    private SweepGradient updateSweep() {
        int grayColor = Color.argb(255, Color.red(speedometerColor), Color.green(speedometerColor), Color.blue(speedometerColor));
        int startColor = Color.argb(150, Color.red(speedometerColor), Color.green(speedometerColor), Color.blue(speedometerColor));
        int color2 = Color.argb(220, Color.red(speedometerColor), Color.green(speedometerColor), Color.blue(speedometerColor));
        int color3 = Color.argb(70, Color.red(speedometerColor), Color.green(speedometerColor), Color.blue(speedometerColor));
        int endColor = Color.argb(15, Color.red(speedometerColor), Color.green(speedometerColor), Color.blue(speedometerColor));
        float position = getOffsetSpeed() * (getEndDegree() - getStartDegree()) / 360f;
        SweepGradient sweepGradient = new SweepGradient(getSize() * .5f, getSize() * .5f
                , new int[]{grayColor, grayColor, grayColor, grayColor, grayColor, grayColor}
                , new float[]{0f, position * .5f, position, position, .99f, 1f});
        Matrix matrix = new Matrix();
        matrix.postRotate(getStartDegree(), getSize() * .5f, getSize() * .5f);
        sweepGradient.setLocalMatrix(matrix);
        return sweepGradient;
    }

    private void updateRadial() {
        int centerColor = Color.argb(160, Color.red(pointerColor), Color.green(pointerColor), Color.blue(pointerColor));
        int edgeColor = Color.argb(10, Color.red(pointerColor), Color.green(pointerColor), Color.blue(pointerColor));
        RadialGradient pointerGradient = new RadialGradient(getSize() * .5f, getSpeedometerWidth() * .5f + dpTOpx(8) + getPadding()
                , getSpeedometerWidth() * .5f + dpTOpx(8), new int[]{centerColor, edgeColor}
                , new float[]{.4f, 1f}, Shader.TileMode.CLAMP);
        pointerBackPaint.setShader(pointerGradient);
    }

    public int getSpeedometerColor() {
        return speedometerColor;
    }

    public void setSpeedometerColor(int speedometerColor) {
        this.speedometerColor = speedometerColor;
        invalidate();
    }

    public int getPointerColor() {
        return pointerColor;
    }

    public void setPointerColor(int pointerColor) {
        this.pointerColor = pointerColor;
        pointerPaint.setColor(pointerColor);
        updateRadial();
        invalidate();
    }

    public int getCenterCircleColor() {
        return circlePaint.getColor();
    }

    /**
     * change the color of the center circle (if exist),
     * <b>this option is not available for all Speedometers</b>.
     *
     * @param centerCircleColor new color.
     */
    public void setCenterCircleColor(int centerCircleColor) {
        circlePaint.setColor(centerCircleColor);
        if (!isAttachedToWindow()) {
            return;
        }
        invalidate();
    }

    /**
     * this Speedometer doesn't use this method.
     *
     * @return {@code Color.TRANSPARENT} always.
     */
    @Deprecated
    @Override
    public int getLowSpeedColor() {
        return Color.TRANSPARENT;
    }

    /**
     * this Speedometer doesn't use this method.
     *
     * @param lowSpeedColor nothing.
     */
    @Deprecated
    @Override
    public void setLowSpeedColor(int lowSpeedColor) {
    }

    /**
     * this Speedometer doesn't use this method.
     *
     * @return {@code Color.TRANSPARENT} always.
     */
    @Deprecated
    @Override
    public int getMediumSpeedColor() {
        return Color.TRANSPARENT;
    }

    /**
     * this Speedometer doesn't use this method.
     *
     * @param mediumSpeedColor nothing.
     */
    @Deprecated
    @Override
    public void setMediumSpeedColor(int mediumSpeedColor) {
    }

    /**
     * this Speedometer doesn't use this method.
     *
     * @return {@code Color.TRANSPARENT} always.
     */
    @Deprecated
    @Override
    public int getHighSpeedColor() {
        return Color.TRANSPARENT;
    }

    /**
     * this Speedometer doesn't use this method.
     *
     * @param highSpeedColor nothing.
     */
    @Deprecated
    @Override
    public void setHighSpeedColor(int highSpeedColor) {
    }
}
