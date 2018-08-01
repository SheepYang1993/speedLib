package com.tools.speedlib.listener;

/**
 * 上传和下载速度
 * Created by wong on 17-3-27.
 */
public interface SpeedListener {
    void onDownloadSpeeding(long downSpeed, long upSpeed);

    void onDownloadSpeedFinish(long finalDownSpeed, long finalUpSpeed);

    void onUploadSpeeding(long upSpeed);

    void onUploadSpeedFinish(long finalUpSpeed);
}
