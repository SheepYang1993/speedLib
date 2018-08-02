package com.tools.speedhelper;

import android.content.Intent;
import android.os.BaseBundle;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tools.speedlib.SpeedManager;
import com.tools.speedlib.listener.NetDelayListener;
import com.tools.speedlib.listener.SpeedListener;
import com.tools.speedlib.utils.ConverUtil;
import com.tools.speedlib.utils.FileUtil;
import com.tools.speedlib.views.NiceSpeedView;
import com.tools.speedlib.views.PointerSpeedView;

import java.lang.ref.WeakReference;


public class MainActivity extends AppCompatActivity {
    private static final String EXTRA_RESULT = "result";
    private static final String EXTRA_DOWNSPEED = "downSpeed";
    private static final String EXTRA_DOWNRESULT = "downResult";

    private NiceSpeedView speedometer;
    private TextView tx_delay;
    private TextView tx_down;
    private TextView tx_up;
    SpeedManager speedManager;
    private Handler mHandle = new MyHandler(this);

    private static class MyHandler extends Handler {

        private WeakReference<MainActivity> activityWeakReference;

        public MyHandler(MainActivity activity) {
            this.activityWeakReference = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MainActivity mainActivity = this.activityWeakReference.get();
            if (mainActivity == null) {
                return;
            }
            Bundle data = msg.getData();
            String[] result = data.getStringArray(EXTRA_RESULT);
            long downSpeed = data.getLong(EXTRA_DOWNSPEED);
            String[] downResult = data.getStringArray(EXTRA_DOWNRESULT);
            switch (msg.what) {
                case 0:
                case 1:
                    mainActivity.tx_down.setText(result[0]);
                    mainActivity.setSpeedView(downSpeed, downResult);
                    if (result[1] != null) {
                        mainActivity.tx_up.setText(result[1]);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speedometer = (NiceSpeedView) findViewById(R.id.speedometer);
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
                    public void onStart() {
//                        Log.i("SheepYang", "开始测速");
                    }

                    @Override
                    public void onFinish(long finalDownSpeed, long finalUpSpeed) {
//                        Log.i("SheepYang", "测速完成");
                    }

                    @Override
                    public void onDownloadSpeeding(long downSpeed, long upSpeed) {
//                        Log.i("SheepYang", "正在下载，下载速度:" + downSpeed);
                        String[] downResult = ConverUtil.fomartSpeed(downSpeed);
                        String[] upResult = null;
                        if (upSpeed != -1) {
                            upResult = ConverUtil.fomartSpeed(upSpeed);
                        }
                        String[] result = new String[2];
                        result[0] = downResult[0] + downResult[1];
                        if (upResult != null) {
                            result[1] = upResult[0] + upResult[1];
                        } else {
                            result[1] = null;
                        }
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
                    public void onDownloadSpeedFinish(long finalDownSpeed, long finalUpSpeed) {
//                        Log.i("SheepYang", "下载结束，下载速度:" + finalDownSpeed);
                        String[] downResult = ConverUtil.fomartSpeed(finalDownSpeed);
                        String[] upResult = null;
                        if (finalUpSpeed != -1) {
                            upResult = ConverUtil.fomartSpeed(finalUpSpeed);
                        }
                        String[] result = new String[2];
                        result[0] = downResult[0] + downResult[1];
                        if (upResult != null) {
                            result[1] = upResult[0] + upResult[1];
                        } else {
                            result[1] = null;
                        }
                        Message msg = Message.obtain();
                        msg.what = 1;
                        Bundle data = new Bundle();
                        data.putStringArray(EXTRA_RESULT, result);
                        data.putLong(EXTRA_DOWNSPEED, finalDownSpeed);
                        data.putStringArray(EXTRA_DOWNRESULT, downResult);
                        msg.setData(data);
                        mHandle.sendMessage(msg);
                    }

                    @Override
                    public void onUploadSpeeding(long upSpeed) {
//                        Log.i("SheepYang", "正在上传，上传速度:" + upSpeed);
                    }

                    @Override
                    public void onUploadSpeedFinish(long finalUpSpeed) {
//                        Log.i("SheepYang", "上传结束，上传速度:" + finalUpSpeed);
                    }
                })
                .setPindCmd("59.61.92.196")
                .setSpeedCount(6)
                .setMode(SpeedManager.MODE_REAL_UPLOAD)
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
