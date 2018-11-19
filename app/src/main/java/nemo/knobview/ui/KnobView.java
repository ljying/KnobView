package nemo.knobview.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug.ExportedProperty;
import android.view.ViewDebug.IntToString;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.Calendar;

import nemo.knobview.R;


/**
 * Description: 表盘时间选择控件
 *
 * @author Li Jianying
 * @version 2.0
 * @since 2018/7/31
 */
public class KnobView extends View implements OnGlobalLayoutListener {

    private static final float FIVE_MINUTE_ANGLE = 2.5f;
    private static final float ONE_MINUTE_ANGLE = 0.5f;
    public static final String TAG = "KnobView2";
    private AnimatorUpdateListener animationListener = new AnimatorUpdateListener() {
        private static final long UPDATE_DELAY = 2000;
        private long nextUpdate = 0;

        public void onAnimationUpdate(ValueAnimator animation) {
            if (KnobView.this.isShown()) {
                KnobView.this.invalidate();
                if (KnobView.this.state == State.Moved || KnobView.this.state == State.Untouched) {
                    long now = System.currentTimeMillis();
                    if (now >= this.nextUpdate) {
                        if (KnobView.this.snappedToNow) {
                            KnobView.this.knob.setTime(now, now + (KnobView.this.knob.getEndTime() - KnobView.this.knob.getStartTime()));
                            KnobView.this.updateTimeLabels();
                        }
                        this.nextUpdate = UPDATE_DELAY + now;
                        return;
                    }
                    return;
                }
                return;
            }
            KnobView.this.stopAllAnimations();
        }
    };
    private int arcColor;
    private RectF arcRect;
    private Paint blueStroke;
    private ObjectAnimator buttonBlinkAnimator;
    private float buttonInnerRadius;
    private float buttonRadius;
    private Paint buttonTextPaint;
    private float buttonTextYOffset;
    private int center_x;
    private int center_y;
    private float clockHourLength;
    private float clockMinutesLength;
    private Path clockPath = new Path();
    private Context ctx;
    private float deviceFactor = 100.0f;
    private Typeface font_reg;
    private ObjectAnimator handleBlinkAnimator;
    private float handleDist;
    private ObjectAnimator handleMovementAnimator;
    private float handleRadius;
    private PointF holdingOffset;
    private Paint hourHandPaint;
    private Paint inactiveHandlePaint;
    private Knob knob = new Knob();
    private String labelKnobCancel;
    private String labelKnobSchedule;
    private String labelKnobStart;
    private Listener listener;
    private Paint minuteHandPaint;
    private Paint nowPaint;
    private boolean snappedToNow = false;
    private Paint startButtonPaint;
    private State state = State.None;
    private Vibrator vibrator;
    private Paint whiteFill;

    public interface Listener {
        void hideTime();

        void onCancel();

        void onSchedule(long j, long j2);

        void onStart(long j, long j2);

        void showTime();

        void updateTime(State state, long j, long j2, long j3, long j4, long j5);
    }

    public enum State {
        None,
        AnimatingHandle,
        Untouched,
        MovingStart,
        MovingEnd,
        Moved,
        Scheduled
    }

    public KnobView(Context context) {
        super(context);
        init(context);
    }

    public KnobView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public KnobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.ctx = context;
        if (isInEditMode()) {
            this.font_reg = Typeface.DEFAULT;
        } else {
            this.font_reg = FontType.getTypeFace(context, FontType.Type.Regular);
        }
        this.deviceFactor = (float) 100;
        this.arcColor = getResources().getColor(R.color.knob_arc);
        this.whiteFill = new Paint();
        this.whiteFill.setColor(getResources().getColor(R.color.common_bg_color));
        this.whiteFill.setStyle(Style.FILL);
        this.whiteFill.setAntiAlias(false);
        this.minuteHandPaint = new Paint(this.whiteFill);
        this.minuteHandPaint.setColor(getResources().getColor(R.color.grey_light2));
        this.minuteHandPaint.setAntiAlias(true);
        this.hourHandPaint = new Paint(this.minuteHandPaint);
        this.hourHandPaint.setColor(getResources().getColor(R.color.grey_dark));
        this.startButtonPaint = new Paint(this.whiteFill);
        this.startButtonPaint.setColor(getResources().getColor(R.color.knob_angle));
        this.nowPaint = new Paint();
        this.nowPaint.setColor(getResources().getColor(R.color.knob_hour));
        this.nowPaint.setStyle(Style.STROKE);
        this.nowPaint.setAntiAlias(true);
        this.nowPaint.setPathEffect(new DashPathEffect(new float[]{20.0f, 20.0f}, 0.0f));
        this.blueStroke = new Paint();
        this.blueStroke.setColor(getResources().getColor(R.color.knob_angle));
        this.blueStroke.setStyle(Style.STROKE);
        this.blueStroke.setTextAlign(Align.CENTER);
        this.blueStroke.setTypeface(this.font_reg);
        this.blueStroke.setAntiAlias(true);
        this.buttonTextPaint = new Paint(this.blueStroke);
        this.buttonTextPaint.setStyle(Style.FILL);
        this.inactiveHandlePaint = new Paint();
        this.inactiveHandlePaint.setColor(getResources().getColor(R.color.grey_dark));
        this.inactiveHandlePaint.setStyle(Style.STROKE);
        this.inactiveHandlePaint.setAntiAlias(true);
        this.inactiveHandlePaint.setAlpha(160);
        this.labelKnobStart = getResources().getString(R.string.knob_label_start);
        this.labelKnobCancel = getResources().getString(R.string.knob_label_cancel);
        this.labelKnobSchedule = getResources().getString(R.string.knob_label_schedule);
        try {
            this.vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception e) {
        }
        getViewTreeObserver().addOnGlobalLayoutListener(this);
        setState(State.None);
    }

    public void setColor(int color) {
        this.startButtonPaint.setColor(color);
        this.blueStroke.setColor(color);
        this.buttonTextPaint = new Paint(this.blueStroke);
        this.buttonTextPaint.setStyle(Style.FILL);
    }

    public void setButtonText(String start, String schedule, String cancel) {
        if (!TextUtils.isEmpty(start)) {
            this.labelKnobStart = start;
        }
        if (!TextUtils.isEmpty(schedule)) {
            this.labelKnobSchedule = schedule;
        }
        if (!TextUtils.isEmpty(cancel)) {
            this.labelKnobCancel = cancel;
        }
    }

    public void onResume() {
//        init(ctx);
        setColor(Color.parseColor("#2581cd"));
        setState(State.None);
//        Log.e("zangzhaori","onResume");
        long now = System.currentTimeMillis();
        if (KnobSettings.hasHandleMovementShown()) {
            setState(State.Moved);
        } else {
            setState(State.AnimatingHandle);
        }
        updateTimeLabels();
    }

    public void onDraw(Canvas canvas) {
        int i;
        canvas.drawColor(this.arcColor);
        float sweepAngle = ((this.knob.getEndAngle() - this.knob.getStartAngle()) + 360.0f) % 360.0f;
        float max = (float) Math.max(getWidth(), getHeight2());
        canvas.save();
        canvas.rotate(this.knob.getStartAngle(), (float) this.center_x, (float) this.center_y);
        canvas.drawLine((float) this.center_x, (float) this.center_y, max, (float) this.center_y, this.minuteHandPaint);
        canvas.drawCircle((float) this.center_x, (float) this.center_y, this.handleDist, this.inactiveHandlePaint);
        canvas.rotate(sweepAngle, (float) this.center_x, (float) this.center_y);
        canvas.drawLine((float) this.center_x, (float) this.center_y, max, (float) this.center_y, this.minuteHandPaint);
        if (!(isInEditMode() || this.arcRect == null)) {
            canvas.drawArc(this.arcRect, 0.0f, (360.0f - sweepAngle) % 360.0f, true, this.whiteFill);
        }
        canvas.restore();
        if (this.state == State.Moved || this.state == State.Scheduled) {
            canvas.drawCircle((float) this.center_x, (float) this.center_y, this.buttonRadius, this.startButtonPaint);
            canvas.drawCircle((float) this.center_x, (float) this.center_y, this.buttonInnerRadius, this.whiteFill);
            String text = this.state == State.Scheduled ? this.labelKnobCancel : this.knob.shouldStartNow() ? this.labelKnobStart : this.labelKnobSchedule;
            this.buttonTextPaint.setTextSize(getSpaceAsPercent(8.0f));
            if (text.contains(" ")) {
                String[] array = text.split(" ");
                for (i = 0; i < array.length; i++) {
                    if (i == 0) {
                        canvas.drawText(array[i], (float) this.center_x, ((float) this.center_y) + (this.buttonTextYOffset * -0.55f), this.buttonTextPaint);
                    } else if (i == array.length - 1) {
                        canvas.drawText(array[i], (float) this.center_x, ((float) this.center_y) + (this.buttonTextYOffset * 2.45f), this.buttonTextPaint);
                    }
                }
            } else {
                canvas.drawText(text, (float) this.center_x, ((float) this.center_y) + this.buttonTextYOffset, this.buttonTextPaint);
            }
        }
        if (this.state == State.MovingEnd || this.state == State.MovingStart) {
            canvas.save();
            for (i = 0; i < 12; i++) {
                canvas.rotate(30.0f, (float) this.center_x, (float) this.center_y);
                canvas.drawCircle(((float) this.center_x) + this.handleDist, (float) this.center_y, 1.0f, this.inactiveHandlePaint);
            }
            canvas.restore();
            long now = System.currentTimeMillis();
            canvas.save();
            canvas.rotate(getHourAngle(now), (float) this.center_x, (float) this.center_y);
            this.clockPath.reset();
            this.clockPath.moveTo((float) this.center_x, (float) this.center_y);
            this.clockPath.lineTo((float) (this.center_x + Math.max(getWidth(), getHeight2())), (float) this.center_y);
            canvas.drawPath(this.clockPath, this.nowPaint);
            canvas.drawLine((float) this.center_x, (float) this.center_y, ((float) this.center_x) + this.clockHourLength, (float) this.center_y, this.hourHandPaint);
            canvas.restore();
            canvas.save();
            canvas.rotate(getMinuteAngle(now), (float) this.center_x, (float) this.center_y);
            canvas.drawLine((float) this.center_x, (float) this.center_y, ((float) this.center_x) + this.clockMinutesLength, (float) this.center_y, this.minuteHandPaint);
            canvas.restore();
        }
        if (this.state != State.Scheduled) {
            drawHandle(canvas, this.knob.getEndAngle(), this.state != State.MovingStart, this.blueStroke);
            if (this.state != State.Untouched) {
                drawHandle(canvas, this.knob.getStartAngle(), this.state == State.MovingStart, this.state == State.MovingStart ? this.blueStroke : this.inactiveHandlePaint);
            }
        }
    }

    @SuppressLint({"ClickableViewAccessibility"})
    public boolean onTouchEvent(MotionEvent event) {
        if (this.state == State.AnimatingHandle) {
            return true;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!checkCollision(x, y, new PointF((float) this.center_x, (float) this.center_y), this.buttonRadius)) {
                    if (this.state != State.Scheduled) {
                        PointF endPos = radialToXY(this.knob.getEndAngle(), this.handleDist);
                        if (!checkCollision(x, y, endPos, this.handleRadius)) {
                            PointF startPos = radialToXY(this.knob.getStartAngle(), this.handleDist);
                            if (checkCollision(x, y, startPos, this.handleRadius)) {
                                setState(State.MovingStart);
                                this.holdingOffset = new PointF(((float) x) - startPos.x, ((float) y) - startPos.y);
                                break;
                            }
                        }
                        setState(State.MovingEnd);
                        this.holdingOffset = new PointF(((float) x) - endPos.x, ((float) y) - endPos.y);
                        break;
                    }
                }
                if (this.state != State.Scheduled) {
                    if (this.state == State.Moved) {
                        long startTime = this.knob.getStartTime();
                        long endTime = this.knob.getEndTime();
                        if (!this.knob.shouldStartNow()) {
                            if (this.listener != null) {
                                this.listener.onSchedule(startTime, endTime);
                            }
                            setState(State.Scheduled);
                            break;
                        }
                        stopAllAnimations();
                        if (this.listener != null) {
                            this.listener.onStart(startTime, endTime);
                            break;
                        }
                    }
                }
                if (this.listener != null) {
                    this.listener.onCancel();
                }
                setState(State.Moved);
                break;
            case MotionEvent.ACTION_UP:
                if (this.state == State.MovingEnd || this.state == State.MovingStart) {
                    this.knob.adjustTimeFromAngles();
                    if (this.knob.durationTooShort()) {
                        this.knob.adjustPeriod();
                        updateTimeLabels();
                    }
                    setState(State.Moved);
                    break;
                }
            case MotionEvent.ACTION_MOVE:
                if (this.state != State.MovingEnd) {
                    if (this.state == State.MovingStart) {
                        float newStartAngle = getAngle((int) (((float) x) - this.holdingOffset.x), (int) (((float) y) - this.holdingOffset.y));
                        if (this.knob.getStartAngle() != newStartAngle) {
                            this.snappedToNow = this.knob.moveStartAngle(newStartAngle);
                            updateTimeLabels();
                            if (!(this.snappedToNow || this.vibrator == null)) {
                                this.vibrator.vibrate((long) 7);
                                break;
                            }
                        }
                    }
                } else {
                    float newEndAngle = getAngle((int) (((float) x) - this.holdingOffset.x), (int) (((float) y) - this.holdingOffset.y));
                    if (this.knob.getEndAngle() != newEndAngle) {
                        this.knob.setEndAngle(newEndAngle);
                        this.knob.adjustTimeFromAngles();
                        updateTimeLabels();
                        if (this.vibrator != null) {
                            this.vibrator.vibrate((long) 7);
                            break;
                        }
                    }
                }
                break;
        }
        invalidate();
        return true;
    }

    @TargetApi(16)
    public void onGlobalLayout() {
        int boundEnd;
        Log.v(TAG, "onGlobalLayout");
        this.center_x = getWidth() / 2;
        this.center_y = getHeight2() / 2;
        float w2 = (float) (getWidth() / 2);
        float h2 = (float) (getHeight2() / 2);
        float hyp = (float) (Math.sqrt((double) ((w2 * w2) + (h2 * h2))) * 1.2d);
        this.arcRect = new RectF(((float) this.center_x) - hyp, ((float) this.center_y) - hyp, ((float) this.center_x) + hyp, ((float) this.center_y) + hyp);
        this.minuteHandPaint.setStrokeWidth(getSpaceAsPercent(1.0f));
        this.handleDist = getSpaceAsPercent(41.0f);
        this.handleRadius = getSpaceAsPercent(7.5f);
        this.blueStroke.setStrokeWidth(getSpaceAsPercent(1.2f));
        this.blueStroke.setStyle(Style.STROKE);
        this.inactiveHandlePaint.setStrokeWidth(getSpaceAsPercent(1.2f));
        this.inactiveHandlePaint.setStyle(Style.STROKE);
        this.buttonRadius = getSpaceAsPercent(24.0f);
        this.buttonInnerRadius = getSpaceAsPercent(21.0f);
        this.buttonTextPaint.setTextSize(getSpaceAsPercent(10.0f));
        Rect textBounds = new Rect();
        int boundStart = 0;
        switch (this.labelKnobStart.length()) {
            case 1:
                boundEnd = 1;
                break;
            case 2:
            case 3:
                boundEnd = 2;
                break;
            default:
                boundStart = 2;
                boundEnd = 3;
                break;
        }
        this.buttonTextPaint.getTextBounds(this.labelKnobStart, boundStart, boundEnd, textBounds);
        this.buttonTextPaint.setTypeface(FontType.getTypeFace(ctx, FontType.Type.Bold));
        this.buttonTextYOffset = (float) (textBounds.height() / 2);
        this.clockHourLength = getSpaceAsPercent(13.0f);
        this.clockMinutesLength = getSpaceAsPercent(25.0f);
        this.minuteHandPaint.setStrokeWidth(getSpaceAsPercent(0.3f));
        this.hourHandPaint.setStrokeWidth(getSpaceAsPercent(0.3f));
        Log.v(TAG, "onGlobalLayout pre state check: " + this.state);
        switch (this.state) {
            case None:
                setState(!KnobSettings.hasHandleMovementShown() ? State.AnimatingHandle : State.Moved);
                break;
            case Untouched:
                startHandleBlinkAnimation();
                break;
            case Scheduled:
                hideTimeLabels();
                updateTimeLabels();
                break;
            case Moved:
                hideTimeLabels();
                startButtonBlinkAnimation();
                break;
        }
        if (VERSION.SDK_INT < 16) {
            getViewTreeObserver().removeGlobalOnLayoutListener(this);
        } else {
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    }

    private int getHeight2() {
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (VERSION.SDK_INT < 11) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        } else if (this.ctx.getTheme().resolveAttribute(16843499, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return super.getHeight() - ((int) (((float) actionBarHeight) * 0.0f));
    }

    private void setState(State newState) {
        Log.v(TAG, "setState " + newState);
        if (newState == null) {
            newState = State.None;
        }
        this.state = newState;
        stopAllAnimations();
        switch (newState) {
            case None:
                this.knob.reset();
                this.snappedToNow = true;
                break;
            case Untouched:
                this.knob.reset();
                this.snappedToNow = true;
                startHandleBlinkAnimation();
                updateTimeLabels();
                break;
            case Scheduled:
                hideTimeLabels();
                updateTimeLabels();
                break;
            case Moved:
                hideTimeLabels();
                startButtonBlinkAnimation();
                break;
            case AnimatingHandle:
                hideTimeLabels();
                startHandleMovementAnimation();
                break;
            case MovingEnd:
            case MovingStart:
                showTimeLabels();
                updateTimeLabels();
                break;
        }
        invalidate();
    }

    private void drawHandle(Canvas canvas, float angle, boolean drawCircle, Paint paint) {
        canvas.save();
        canvas.rotate(angle, (float) this.center_x, (float) this.center_y);
        canvas.drawCircle(((float) this.center_x) + this.handleDist, (float) this.center_y, 4.0f, paint);
        if (drawCircle) {
            canvas.drawCircle(((float) this.center_x) + this.handleDist, (float) this.center_y, this.handleRadius, paint);
        }
        canvas.restore();
    }

    private PointF radialToXY(float angle, float distance) {
        double rAngle = Math.toRadians((double) angle);
        return new PointF((float) (this.center_x + ((int) (((double) distance) * Math.cos(rAngle)))), (float) (this.center_y + ((int) (((double) distance) * Math.sin(rAngle)))));
    }

    private boolean checkCollision(int x, int y, PointF target, float tolerance) {
        float dx = ((float) x) - target.x;
        float dy = ((float) y) - target.y;
        return (dx * dx) + (dy * dy) < tolerance * tolerance;
    }

    private void updateTimeLabels() {
        if (this.listener != null) {
            long seconds = (this.knob.getEndTime() - this.knob.getStartTime()) / 1000;
            this.listener.updateTime(this.state, seconds, (seconds / 60) % 60, seconds / 3600, this.knob.getStartTime(), this.knob.getEndTime());
        }
    }

    protected void showTimeLabels() {
        if (this.listener != null) {
            this.listener.showTime();
        }
    }

    protected void hideTimeLabels() {
        if (this.listener != null) {
            this.listener.hideTime();
        }
    }

    private float getHourAngle(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return (360.0f * (((float) ((cal.get(Calendar.HOUR) * 60) + cal.get(Calendar.MINUTE))) / 720.0f)) - 90.0f;
    }

    private float getMinuteAngle(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        return (((((float) cal.get(Calendar.MINUTE)) * 360.0f) / 60.0f) - 90.0f) % 360.0f;
    }

    private float getAngle(int x, int y) {
        double aTan2 = Math.atan2(y - this.center_y, x - this.center_x);
        double degrees = Math.toDegrees(aTan2);
        return (float) (degrees + 360) % 360;
    }

    private float getSpaceAsPercent(float percent) {
        return (((percent * ((float) Math.min(getWidth(), getHeight2()))) * 100.0f) / this.deviceFactor) / 100.0f;
    }

    public void setAnimHandlePosition(int pos) {
        this.knob.setEndAngle((float) pos);
    }

    public void setStartButtonPaint(int alpha) {
        this.startButtonPaint.setAlpha(alpha);
    }

    public void setHandlePaint(int alpha) {
        this.blueStroke.setAlpha(alpha);
    }

    @ExportedProperty(mapping = {@IntToString(from = 0, to = "VISIBLE"), @IntToString(from = 4, to = "INVISIBLE"), @IntToString(from = 8, to = "GONE")})
    public int getVisibility() {
        int visibility = super.getVisibility();
        if (!(visibility != 0 || this.handleMovementAnimator == null || this.handleMovementAnimator.isRunning())) {
            this.handleMovementAnimator.start();
        }
        return visibility;
    }

    protected void startHandleMovementAnimation() {
        Log.v(TAG, "startHandleMovementAnimation handleMovementAnimator: " + this.handleMovementAnimator);
        if (this.handleMovementAnimator == null) {
            this.knob.setEndAngle(this.knob.getStartAngle());
            this.handleMovementAnimator = ObjectAnimator.ofInt(this, "animHandlePosition", new int[]{(int) knob.getStartAngle(), (int) (knob.getStartAngle() + 45.0f)});
            this.handleMovementAnimator.setDuration((long) 1600);
            this.handleMovementAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            this.handleMovementAnimator.addUpdateListener(this.animationListener);
            this.handleMovementAnimator.addListener(new AnimatorListenerAdapter() {
                public void onAnimationEnd(Animator animation) {
                    KnobView.this.setState(State.Moved);
                    KnobSettings.setHandleMovementShown(true);
                }
            });
            if (isShown()) {
                this.handleMovementAnimator.start();
            }
        }
    }

    private void stopHandleMovementAnimation() {
        if (this.handleMovementAnimator != null) {
            this.handleMovementAnimator.cancel();
            this.handleMovementAnimator = null;
        }
    }

    protected void startButtonBlinkAnimation() {
        if (this.buttonBlinkAnimator == null) {
            this.buttonBlinkAnimator = ObjectAnimator.ofInt(this, "startButtonPaint", new int[]{50, 255});
            this.buttonBlinkAnimator.setDuration(1500);
            this.buttonBlinkAnimator.setRepeatCount(-1);
            this.buttonBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
            this.buttonBlinkAnimator.addUpdateListener(this.animationListener);
            this.buttonBlinkAnimator.start();
        }
    }

    protected void startHandleBlinkAnimation() {
        if (this.handleBlinkAnimator == null) {
            this.handleBlinkAnimator = ObjectAnimator.ofInt(this, "handlePaint", new int[]{50, 255});
            this.handleBlinkAnimator.setDuration(1500);
            this.handleBlinkAnimator.setRepeatCount(-1);
            this.handleBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
            this.handleBlinkAnimator.addUpdateListener(this.animationListener);
            this.handleBlinkAnimator.start();
        }
    }

    protected void stopHandleBlinkAnimation() {
        if (this.handleBlinkAnimator != null) {
            this.handleBlinkAnimator.cancel();
            this.handleBlinkAnimator = null;
            this.blueStroke.setAlpha(255);
        }
    }

    protected void stopButtonBlinkAnimation() {
        if (this.buttonBlinkAnimator != null) {
            this.buttonBlinkAnimator.cancel();
            this.buttonBlinkAnimator = null;
            this.startButtonPaint.setAlpha(255);
        }
    }

    private void stopAllAnimations() {
        stopButtonBlinkAnimation();
        stopHandleBlinkAnimation();
        stopHandleMovementAnimation();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }
}
