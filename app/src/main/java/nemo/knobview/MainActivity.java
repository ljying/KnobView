package nemo.knobview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.text.DateFormat;

import nemo.knobview.ui.KnobView;

public class MainActivity extends AppCompatActivity {

    private KnobView knob;

    private TextView totalTimeView;

    private TextView startTimeView;

    private TextView endTimeView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        totalTimeView = findViewById(R.id.totalTime);
        endTimeView = findViewById(R.id.endTime);
        startTimeView = findViewById(R.id.startTime);
        knob = findViewById(R.id.knob);


        knob.setListener(new KnobView.Listener() {
            @Override
            public void hideTime() {
                findViewById(R.id.ll_content).setVisibility(View.INVISIBLE);
            }

            /**
             * 计划任务停止
             */
            @Override
            public void onCancel() {
            }

            /**
             * 计划任务
             * @param startTime
             * @param endTime
             */
            @Override
            public void onSchedule(long startTime, long endTime) {
                long nowTime = System.currentTimeMillis();
                long doTime = Math.abs(startTime - nowTime);
            }

            @Override
            public void onStart(long startTime, long endTime) {
            }

            @Override
            public void showTime() {
                findViewById(R.id.ll_content).setVisibility(View.VISIBLE);
            }

            @Override
            public void updateTime(KnobView.State state, long seconds, long minutes, long hours, long start, long end) {
                String totalTimeText;
                String startTimeText;
                String endTimeText;
                DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());
                String labelHours =getResources().getString(R.string.knob_hours);
                String labelMinutes = getResources().getString(R.string.knob_minutes);
                if (state == KnobView.State.Untouched) {
                    totalTimeText = null;
                    startTimeText = null;
                    endTimeText = null;
                } else {
                    String strHours = hours != 0 ? hours + " " + labelHours + " " : "";
                    totalTimeText =getString(R.string.total_time, strHours, minutes, labelMinutes);
                    startTimeText = timeFormat.format(start);
                    endTimeText = timeFormat.format(end);
                    if (state == KnobView.State.Moved) {
                        endTimeText = null;
                    }
                }
                totalTimeView.setText(totalTimeText);
                startTimeView.setText(startTimeText);
                endTimeView.setText(endTimeText);
            }
        });
    }
}
