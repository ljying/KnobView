package nemo.knobview.ui;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Calendar;


/**
 * Description: 表盘时间控制信息
 *
 * @author Li Jianying
 * @version 2.0
 * @since 2018/7/31
 */
public class Knob implements Parcelable, Serializable {
    public static final Creator<Knob> CREATOR = new Creator<Knob>() {
        public Knob createFromParcel(Parcel in) {
            return new Knob(in);
        }

        public Knob[] newArray(int size) {
            return new Knob[size];
        }
    };
    public static final int DEFAULT_PERIOD_MINUTES = 90;
    private static final float MIN_MOVING_PERIOD_ANGLE = 7.5f / 3;
    private static final long MIN_MOVING_PERIOD_MILLIS = 900000 / 3;
    private static final long MIN_MOVING_PERIOD_MINUTES = 15;
    private static final float MIN_PERIOD_ANGLE = 2.5f;
    private static final long MIN_PERIOD_MILLIS = 300000;
    private static final long MIN_PERIOD_MINUTES = 5;
    public static final float ONE_MINUTE_ANGLE = 0.5f;
    private static final long SNAP_NOW_MILLIS = 300000;
    private static final long SNAP_NOW_MINUTES = 5;
    private static final float SNAP_TO_NOW_ANGLE = 2.5f;
    private static final String TAG = "Knob";
    private static final long serialVersionUID = 1;
    private static Calendar tmpCalendar = Calendar.getInstance();
    private float endAngle;
    private long endTime;
    private Calendar endTimeCal = Calendar.getInstance();
    private float startAngle;
    private long startTime;
    private Calendar startTimeCal = Calendar.getInstance();

    public Knob() {
        reset();
    }

    public Knob(long startTime, long endTime) {
        setTime(startTime, endTime);
    }

    public Knob(Parcel in) {
        setTime(in.readLong(), in.readLong());
    }

    public void reset() {
        long now = System.currentTimeMillis();
        setTime(now, 5400000 + now);
    }

    public void setTime(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.startTimeCal.setTimeInMillis(startTime);
        this.endTimeCal.setTimeInMillis(endTime);
        this.startAngle = timeToAngle(startTime);
        this.endAngle = timeToAngle(endTime);
    }

    public void setStartTime(long time) {
        this.startTime = time;
        this.startTimeCal.setTimeInMillis(this.startTime);
        this.startAngle = timeToAngle(this.startTime);
    }

    public boolean moved() {
        return this.startAngle != this.endAngle;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public void setStartAngle(float newAngle) {
        this.startAngle = newAngle;
    }

    public void setEndAngle(float newAngle) {
        this.endAngle = newAngle;
    }

    public void adjustTimeFromAngles() {
        long now = System.currentTimeMillis();
        float adjustedStartAngle = (this.startAngle + 90.0f) % 360.0f;
        int startHours = (int) (adjustedStartAngle / 30.0f);
        int startMinutes = (int) ((2.0f * adjustedStartAngle) % 60.0f);
        Calendar startCal = Calendar.getInstance();
        startCal.set(Calendar.HOUR, startHours);
        startCal.set(Calendar.MINUTE, startMinutes);
        float adjustedEndAngle = (this.endAngle + 90.0f) % 360.0f;
        Calendar endCal = Calendar.getInstance();
        endCal.set(Calendar.HOUR, (int) (adjustedEndAngle / 30.0f));
        endCal.set(Calendar.MINUTE, (int) ((2.0f * adjustedEndAngle) % 60.0f));
        if (startCal.getTimeInMillis() < now && !shouldStartNow()) {
            startCal.add(Calendar.HOUR_OF_DAY, 12);
            endCal.add(Calendar.HOUR_OF_DAY, 12);
        }
        if (endCal.before(startCal)) {
            endCal.add(Calendar.HOUR_OF_DAY, 12);
        }
        this.startTimeCal = startCal;
        this.endTimeCal = endCal;
        this.startTime = this.startTimeCal.getTimeInMillis();
        this.endTime = this.endTimeCal.getTimeInMillis();
    }

    public boolean shouldStartNow() {
        long now = System.currentTimeMillis();
        return Math.abs(this.startTime - now) < 300000 || Math.abs((this.startTime - 43200000) - now) < 300000;
    }

    public boolean isSnapped() {
        return minAngularDistance(this.startAngle, timeToAngle(System.currentTimeMillis())) < 2.5f / 5;
    }

    public boolean durationTooShort() {
        return minAngularDistance(this.startAngle, this.endAngle) < 2.5f;
    }

    private float minAngularDistance(float a, float b) {
        float angle = Math.abs(a - b) % 360.0f;
        if (angle > 180.0f) {
            return 360.0f - angle;
        }
        return angle;
    }

    private boolean isAngleBefore(float a, float b, float threshold) {
        return (b - a < threshold && b - a > 0.0f) || (360.0f + b) - a < threshold;
    }

    private static float timeToAngle(long time) {
        tmpCalendar.setTimeInMillis(time);
        return calToAngle(tmpCalendar);
    }

    private static float calToAngle(Calendar cal) {
        return (float) ((((cal.get(Calendar.HOUR) * 30) + (cal.get(Calendar.MINUTE) / 2)) + 270) % 360);
    }

    public float getStartAngle() {
        return this.startAngle;
    }

    public float getEndAngle() {
        return this.endAngle;
    }

    public boolean tick() {
        if (!shouldStartNow()) {
            return false;
        }
        long now = System.currentTimeMillis();
        setTime(now, this.endTime - this.startTime < 300000 ? now + 300000 : this.endTime);
        return true;
    }

    public boolean moveStartAngle(float newStartAngle) {
        if (isAngleBefore(this.startAngle, newStartAngle, 22.5f / 3) && this.endTime - this.startTime < MIN_MOVING_PERIOD_MILLIS) {
            this.endAngle = MIN_MOVING_PERIOD_ANGLE + newStartAngle;
        }
        float nowAngle = timeToAngle(System.currentTimeMillis());
        boolean snapped = minAngularDistance(newStartAngle, nowAngle) < (2.5f / 5);
        if (snapped) {
            newStartAngle = nowAngle + (0.25f / 5);
        }
        this.startAngle = newStartAngle;
        adjustTimeFromAngles();
        return snapped;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.startTime);
        dest.writeLong(this.endTime);
    }

    public void adjustPeriod() {
        if (durationTooShort()) {
            setEndAngle(this.startAngle + 2.5f);
        }
        adjustTimeFromAngles();
    }
}
