package com.tools.speedhelper;

import android.content.Intent;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.tools.speedlib.SpeedManager;
import com.tools.speedlib.listener.NetDelayListener;
import com.tools.speedlib.listener.SpeedListener;
import com.tools.speedlib.utils.ConverUtil;
import com.tools.speedlib.utils.FileUtil;
import com.tools.speedlib.views.PointerSpeedView;


public class MainActivity extends AppCompatActivity {
    private static final String EXTRA_RESULT = "result";
    private static final String EXTRA_DOWNSPEED = "downSpeed";
    private static final String EXTRA_DOWNRESULT = "downResult";

    private PointerSpeedView speedometer;
    private TextView tx_delay;
    private TextView tx_down;
    private TextView tx_up;
    SpeedManager speedManager;
    private Handler mHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle data = msg.getData();
            String[] result = data.getStringArray(EXTRA_RESULT);
            long downSpeed = data.getLong(EXTRA_DOWNSPEED);
            String[] downResult = data.getStringArray(EXTRA_DOWNRESULT);
            switch (msg.what) {
                case 0:
                case 1:
                    tx_down.setText(result[0]);
                    setSpeedView(downSpeed, downResult);
                    tx_up.setText(result[1]);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedometer = (PointerSpeedView) findViewById(R.id.speedometer);
        tx_delay = (TextView) findViewById(R.id.tx_delay);
        tx_down = (TextView) findViewById(R.id.tx_down);
        tx_up = (TextView) findViewById(R.id.tx_up);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start();
            }
        });
    }

    private void start() {
        speedManager = new SpeedManager.Builder()
                .setUploadSpeedUrl("http://img.zonelian.com/test-speed-upload")
                .setNetDelayListener(new NetDelayListener() {
                    @Override
                    public void result(String delay) {
                        tx_delay.setText(delay);
                    }
                })
                .setSpeedListener(new SpeedListener() {
                    @Override
                    public void speeding(long downSpeed, long upSpeed) {
                        String[] downResult = ConverUtil.fomartSpeed(downSpeed);
                        String[] upResult = ConverUtil.fomartSpeed(upSpeed);
                        String[] result = new String[2];
                        result[0] = downResult[0] + downResult[1];
                        result[1] = upResult[0] + upResult[1];
                        Message msg = Message.obtain();
                        msg.what = 0;
                        Bundle data = new Bundle();
                        data.putStringArray(EXTRA_RESULT, result);
                        data.putLong(EXTRA_DOWNSPEED, downSpeed);
                        data.putStringArray(EXTRA_DOWNRESULT, downResult);
                        msg.setData(data);
                        mHandle.sendMessage(msg);
                    }

                    @Override
                    public void finishSpeed(long finalDownSpeed, long finalUpSpeed) {
                        String[] downResult = ConverUtil.fomartSpeed(finalDownSpeed);
                        String[] upResult = ConverUtil.fomartSpeed(finalUpSpeed);
                        String[] result = new String[2];
                        result[0] = downResult[0] + downResult[1];
                        result[1] = upResult[0] + upResult[1];
                        Message msg = Message.obtain();
                        msg.what = 1;
                        Bundle data = new Bundle();
                        data.putStringArray(EXTRA_RESULT, result);
                        data.putLong(EXTRA_DOWNSPEED, finalDownSpeed);
                        data.putStringArray(EXTRA_DOWNRESULT, downResult);
                        msg.setData(data);
                        mHandle.sendMessage(msg);
                    }
                })
                .setPindCmd("59.61.92.196")
                .setSpeedCount(6)
                .setDownFile(FileUtil.getDownFile())
                .setSpeedTimeOut(15000)
                .builder();
        speedManager.startSpeed();
    }


    private void setSpeedView(long speed, String[] result) {
        if (null != result && 2 == result.length) {
            speedometer.setCurrentSpeed(result[0]);
            speedometer.setUnit(result[1]);
            speedometer.speedPercentTo(ConverUtil.getSpeedPercent(speed));
        }
    }
}
