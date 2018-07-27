package com.tools.speedhelper;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.tools.speedlib.SpeedManager;
import com.tools.speedlib.listener.NetDelayListener;
import com.tools.speedlib.listener.SpeedListener;
import com.tools.speedlib.utils.ConverUtil;
import com.tools.speedlib.views.AwesomeSpeedView;
import com.tools.speedlib.views.NiceSpeedView;
import com.tools.speedlib.views.PointerSpeedView;

import java.io.File;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {
    private static final String FILE_BASE = "SpeedLib";
    private PointerSpeedView speedometer;
    private TextView tx_delay;
    private TextView tx_down;
    private TextView tx_up;
    SpeedManager speedManager;

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
                        tx_down.setText(downResult[0] + downResult[1]);
                        setSpeedView(downSpeed, downResult);

                        String[] upResult = ConverUtil.fomartSpeed(upSpeed);
                        tx_up.setText(upResult[0] + upResult[1]);
                    }

                    @Override
                    public void finishSpeed(long finalDownSpeed, long finalUpSpeed) {
                        String[] downResult = ConverUtil.fomartSpeed(finalDownSpeed);
                        tx_down.setText(downResult[0] + downResult[1]);
                        setSpeedView(finalDownSpeed, downResult);

                        String[] upResult = ConverUtil.fomartSpeed(finalUpSpeed);
                        tx_up.setText(upResult[0] + upResult[1]);
                    }
                })
                .setPindCmd("59.61.92.196")
                .setSpeedCount(6)
                .setDownFile(getDownFile("download.apk"))
                .setSpeedTimeOut(15000)
                .builder();
        speedManager.startSpeed();
    }

    private File getDownFile(String filename) {
        File rootFile = Environment.getExternalStorageDirectory();
        File downFile = new File(rootFile, FILE_BASE + File.separator + filename);
        if (createOrExistsFile(downFile)) {
            return downFile;
        } else {
            return null;
        }
    }


    /**
     * 判断文件是否存在，不存在则判断是否创建成功
     *
     * @param file 文件
     * @return {@code true}: 存在或创建成功<br>{@code false}: 不存在或创建失败
     */
    private static boolean createOrExistsFile(File file) {
        if (file == null) {
            return false;
        }
        // 如果存在，是文件则返回true，是目录则返回false
        if (file.exists()) {
            return file.isFile();
        }
        if (!createOrExistsDir(file.getParentFile())) {
            return false;
        }
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断目录是否存在，不存在则判断是否创建成功
     *
     * @param file 文件
     * @return {@code true}: 存在或创建成功<br>{@code false}: 不存在或创建失败
     */
    private static boolean createOrExistsDir(File file) {
        // 如果存在，是目录则返回true，是文件则返回false，不存在则返回是否创建成功
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    private void setSpeedView(long speed, String[] result) {
        if (null != result && 2 == result.length) {
            speedometer.setCurrentSpeed(result[0]);
            speedometer.setUnit(result[1]);
            speedometer.speedPercentTo(ConverUtil.getSpeedPercent(speed));
        }
    }
}
