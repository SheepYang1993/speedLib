package com.tools.speedlib;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.tools.speedlib.helper.ProgressHelper;
import com.tools.speedlib.listener.NetDelayListener;
import com.tools.speedlib.listener.SpeedListener;
import com.tools.speedlib.listener.impl.UIProgressListener;
import com.tools.speedlib.runnable.NetworkDelayRunnable;
import com.tools.speedlib.utils.FileUtil;
import com.tools.speedlib.utils.TimerTaskUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 测速
 * Created by wong on 17-3-27.
 */
public class SpeedManager {
    private static final int MSG_TIMEOUT = 1000;
    private OkHttpClient client;
    private Call downloadCall;
    private String pingCmd; //网络延时的指令
    private String downloadUrl; //下载网络测速的地址
    private String uploadUrl; //上传网络测速的地址
    private int maxCount; //测速的时间总数
    private long timeOut; //超时时间
    private File downFile;//下载文件地址
    private NetDelayListener delayListener; //网络延时回调
    private SpeedListener speedListener; //测速回调

    private SparseArray<Long> mTotalSpeeds = new SparseArray<>(); //保存每秒的速度
    private long mTempSpeed = 0L; //每秒的速度
    private int mSpeedCount = 0; //文件下载进度的回调次数
    private boolean mIsStopSpeed = false; //是否结束测速

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_TIMEOUT:
                    if (!mIsStopSpeed) {
//                        Log.i("SheepYang", "handleResultSpeed MSG_TIMEOUT");
                        mIsUploadDone = true;
                        handleResultSpeed(0L, true);
                    }
                    break;
                default:
                    break;
            }
        }
    };
    private Call uploadCall;
    private boolean mIsUploadDone = true;
    private long mCurrentBytes;
    private long mUploadTime;

    private SpeedManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS) //设置超时，不设置可能会报异常
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(2, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 开始测速
     */
    public void startSpeed() {
        mUploadTime = -1;
        mSpeedCount = 0;
        mTempSpeed = 0;
        mIsStopSpeed = false;
        mTotalSpeeds = new SparseArray<>();
        boolean isPingSucc = pingDelay(this.pingCmd);
        if (isPingSucc && null != speedListener) {
            speed();
        }
    }

    /**
     * 测速结束
     */
    public void finishSpeed() {
        finishDownloadSpeed();
        if (uploadCall != null) {
            uploadCall.cancel();
        }
        mIsUploadDone = true;
        mIsStopSpeed = true;
        TimerTaskUtil.cacleTimer(mHandler, MSG_TIMEOUT);
    }

    private void finishDownloadSpeed() {
        if (downloadCall != null) {
            downloadCall.cancel();
        }
    }

    /**
     * 进行测速
     * 下载速度和上传速度
     */
    private void speed() {
        finishSpeed();
        TimerTaskUtil.setTimer(mHandler, MSG_TIMEOUT, timeOut);
        UIProgressListener uiProgressListener = new UIProgressListener() {
            @Override
            public void onUIProgress(int taskId, long currentBytes, long contentLength, boolean done) {
                handleSpeed(currentBytes, done);
            }

            @Override
            public void onUIStart(int taskId, long currentBytes, long contentLength, boolean done) {
                super.onUIStart(taskId, currentBytes, contentLength, done);
            }

            @Override
            public void onUIFinish(int taskId, long currentBytes, long contentLength, boolean done) {
                super.onUIFinish(taskId, currentBytes, contentLength, done);
//                Log.i("SheepYang", "handleResultSpeed onUIFinish");
                handleResultSpeed(currentBytes, done);
            }
        };
        Request request = new Request.Builder()
                .url(this.downloadUrl)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build();
        downloadCall = ProgressHelper.addProgressResponseListener(client, uiProgressListener).newCall(request);
        downloadCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Log.i("SheepYang", "onFailure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
//                Log.i("SheepYang", "onResponse");
//                readBytesFromStream(response.body().byteStream());
                download(call, response);
            }
        });
    }

    private void readBytesFromStream(InputStream is) throws IOException {
        int len;
        int size = 1024;
        byte[] buf = new byte[size];
        while (!mIsStopSpeed && (len = is.read(buf, 0, size)) != -1) {
            Log.d("TAG", "byte length : " + len);
        }
    }

    /**
     * 获取下载数据并写入文件
     *
     * @param response
     */
    private void download(Call call, Response response) throws IOException {
        InputStream is = response.body().byteStream();
        if (downFile == null) {
//            Log.i("SheepYang", "downFile == null");
            readBytesFromStream(is);
            return;
//            throw new NullPointerException("downFile == null");
        }
        downFile.delete();
        downFile = FileUtil.getDownFile();
        byte[] buffer = new byte[1024];
        int len;
        long totalSize = 0;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(downFile);
            while ((len = is.read(buffer)) != -1) {
                totalSize += len;
                if (totalSize < 2 * 1024 * 1024) {
                    //大于2MB不再下载，因为上传接口限制最大只能2MB
//                    Log.i("SheepYang", "totalSize:" + totalSize);
                    fos.write(buffer, 0, len);
                }
//                Log.i("SheepYang", "fos.write:" + len);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                is.close();
//                Log.i("SheepYang", "is.close");
            }
            if (fos != null) {
                fos.close();
//                Log.i("SheepYang", "fos.close");
            }
        }
//        postSuccess(downloadCall,null);
    }

    /**
     * 网络延时
     *
     * @return
     */
    private boolean pingDelay(String cmd) {
//        if (null == this.delayListener) {
//            return true;
//        }
//        try {
//            Process p = Runtime.getRuntime().exec(cmd);// -c ping次数
//            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
//            String content;
//            while ((content = buf.readLine()) != null) {
//                // rtt min/avg/max/mdev = 32.745/78.359/112.030/33.451 ms
//                if (content.contains("avg")) {
//                    String[] delays = content.split("/");
//                    delayListener.result(delays[4] + "ms");
//                    break;
//                }
//            }
//            // PING的状态
//            int status = p.waitFor();
//            if (status == 0) {
//                return true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return false;

        if (null == this.delayListener) {
            return true;
        }
        try {
            NetworkDelayRunnable delayRunnable = new NetworkDelayRunnable(cmd);
            Thread thread = new Thread(delayRunnable);
            thread.start();
            thread.join(5000L);
            delayListener.result(delayRunnable.getDelayTime());
            return delayRunnable.isPingSucc();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 处理下载文件的回调
     *
     * @param currentBytes
     * @param done
     */
    private void handleSpeed(long currentBytes, boolean done) {
//        Log.i("SheepYang", String.format("handleSpeed currentBytes:%d, done:%b", currentBytes, done));
        if (mSpeedCount < maxCount) {
            mTempSpeed = currentBytes / (mSpeedCount + 1);
            mTotalSpeeds.put(mSpeedCount, mTempSpeed);
            mSpeedCount++;
            //回调每秒的速度
            if (null != speedListener) {
                long uploadSpeed;
                if (isNeedUpload()) {
                    if (mUploadTime != -1) {
                        uploadSpeed = mUploadTime;
                    } else {
                        uploadSpeed = 0;
                    }
                } else {
                    uploadSpeed = mTempSpeed / 4;
                }
                speedListener.speeding(mTempSpeed, uploadSpeed);
            }
        }
//        Log.i("SheepYang", String.format("handleResultSpeed handleSpeed ,mSpeedCount >= maxCount:%b, done:%b", mSpeedCount >= maxCount, done));
        handleResultSpeed(currentBytes, mSpeedCount >= maxCount || done);
    }

    /**
     * 结果的处理
     *
     * @param isDownloadDone
     * @param currentBytes
     */
    private void handleResultSpeed(long currentBytes, boolean isDownloadDone) {
        if (isDownloadDone) {
            mCurrentBytes = currentBytes;
            if (!isNeedUpload()) {
                finishSpeed();
                //回调最终的速度
                long finalSpeedTotal = 0L;
                for (int i = 0; i < mTotalSpeeds.size(); i++) {
                    finalSpeedTotal += mTotalSpeeds.get(i);
                }
                if (null != speedListener) {
                    if (mTotalSpeeds.size() > 0) {
                        speedListener.finishSpeed(finalSpeedTotal / mTotalSpeeds.size(), finalSpeedTotal / mTotalSpeeds.size() / 4);
                    } else if (0 != currentBytes) {
                        //文件较小时可能出现
                        speedListener.finishSpeed(currentBytes, currentBytes / 4);
                    } else {
                        //超时
                        speedListener.finishSpeed(0L, 0L);
                    }
                    speedListener = null;
                    TimerTaskUtil.cacleTimer(mHandler, MSG_TIMEOUT);
                }
            } else {
                finishDownloadSpeed();
                if (mUploadTime == -1) {
                    uploadFile();
                } else {
                    finishSpeed();
                    //回调最终的速度
                    long finalSpeedTotal = 0L;
                    for (int i = 0; i < mTotalSpeeds.size(); i++) {
                        finalSpeedTotal += mTotalSpeeds.get(i);
                    }
                    if (null != speedListener) {
                        long length = downFile.length();
                        long time = (mUploadTime / 1000000000) <= 0 ? 1 : (mUploadTime / 1000000000);
                        long uploadSpeed = mUploadTime > 0 ? length / time : 0L;
                        if (mTotalSpeeds.size() > 0) {
                            speedListener.finishSpeed(finalSpeedTotal / mTotalSpeeds.size(), uploadSpeed);
                        } else if (0 != currentBytes) {
                            //文件较小时可能出现
                            speedListener.finishSpeed(currentBytes, uploadSpeed);
                        } else {
                            //超时
                            speedListener.finishSpeed(0L, 0L);
                        }
                        speedListener = null;
                        TimerTaskUtil.cacleTimer(mHandler, MSG_TIMEOUT);
                    }
                }
            }
        }
    }

    private boolean isNeedUpload() {
        if (TextUtils.isEmpty(this.uploadUrl)) {
            return false;
        } else {
            return true;
        }
    }

    private void uploadFile() {
        if (!mIsUploadDone) {
            return;
        }
        mIsUploadDone = false;
        if (downFile == null) {
            mIsUploadDone = true;
            mUploadTime = 0;
            handleResultSpeed(mCurrentBytes, true);
            return;
        }
//        Log.i("SheepYang", "uploadFile");
        OkHttpClient client = new OkHttpClient();
        // form 表单形式上传
        MultipartBody.Builder requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (downFile != null) {
            // MediaType.parse() 里面是上传的文件类型。
            RequestBody body = RequestBody.create(MediaType.parse("image/*;charset=utf-8"), downFile);
            String filename = downFile.getName();
            // 参数分别为， 请求key ，文件名称 ， RequestBody
            requestBody.addFormDataPart("headImage", downFile.getName(), body);
        }
//        if (map != null) {
//            // map 里面是请求中所需要的 key 和 value
//            for (Map.Entry entry : map.entrySet()) {
//                requestBody.addFormDataPart(valueOf(entry.getKey()), valueOf(entry.getValue()));
//            }
//        }
        Request request = new Request.Builder().url(this.uploadUrl).post(requestBody.build()).build();
        // readTimeout("请求超时时间" , 时间单位);
        uploadCall = client.newBuilder().readTimeout(5000, TimeUnit.MILLISECONDS).build().newCall(request);
        final long tempTime = System.nanoTime();
        uploadCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mIsUploadDone = true;
                mUploadTime = 0;
//                Log.i("SheepYang", "upload onFailure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                mIsUploadDone = true;
                if (response.isSuccessful()) {
                    String str = response.body().string();
                    mUploadTime = System.nanoTime() - tempTime;
                    handleResultSpeed(mCurrentBytes, true);
//                    Log.i("SheepYang", "upload onResponse message:" + response.message() + " , body " + str);
                } else {
//                    Log.i("SheepYang", response.message() + "upload onResponse error : body " + response.body().string());
                }
            }
        });
    }

    /**
     * 建造者模式
     * 构建测速管理类
     */
    public static final class Builder {
        private static final String DEFAULE_CMD = "www.baidu.com";
        private static final String DEFAULT_URL = "http://dldir1.qq.com/qqfile/QQIntl/QQi_wireless/Android/qqi_4.6.13.6034_office.apk";
        private static final int MAX_COUNT = 6; //最多回调的次数（每秒回调一次）
        private String pingCmd;
        private String downloadUrl;
        private String uploadUrl;
        private int maxCount;
        private long timeOut;
        private NetDelayListener delayListener;
        private SpeedListener speedListener;
        private File downFile;

        public Builder() {
            pingCmd = DEFAULE_CMD;
            downloadUrl = DEFAULT_URL;
            maxCount = MAX_COUNT;
            timeOut = MAX_COUNT * 1000 + 5000;
        }

        public Builder setPindCmd(String cmd) {
            this.pingCmd = cmd;
            return this;
        }

        public Builder setDownloadSpeedUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Builder setUploadSpeedUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
            return this;
        }

        public Builder setSpeedCount(int maxCount) {
            this.maxCount = maxCount;
            return this;
        }

        public Builder setSpeedTimeOut(long timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        /**
         * 设置保存file
         *
         * @param file
         */
        public Builder setDownFile(File file) {
            this.downFile = file;
            return this;
        }

        public Builder setNetDelayListener(NetDelayListener delayListener) {
            this.delayListener = delayListener;
            return this;
        }

        public Builder setSpeedListener(SpeedListener speedListener) {
            this.speedListener = speedListener;
            return this;
        }

        private void applayConfig(SpeedManager manager) {
            if (!TextUtils.isEmpty(this.pingCmd)) {
                manager.pingCmd = "ping -c 3 " + this.pingCmd;
            }
            if (!TextUtils.isEmpty(this.downloadUrl)) {
                manager.downloadUrl = this.downloadUrl;
            }
            if (!TextUtils.isEmpty(this.uploadUrl)) {
                manager.uploadUrl = this.uploadUrl;
            }
            if (0 != this.maxCount) {
                manager.maxCount = this.maxCount;
            }
            if (0L != this.timeOut) {
                manager.timeOut = this.timeOut;
            }
            if (null != this.delayListener) {
                manager.delayListener = this.delayListener;
            }
            if (null != this.downFile) {
                manager.downFile = this.downFile;
            }
            if (null != this.speedListener) {
                manager.speedListener = this.speedListener;
            }
        }

        public SpeedManager builder() {
            SpeedManager manager = new SpeedManager();
            applayConfig(manager);
            return manager;
        }
    }
}
