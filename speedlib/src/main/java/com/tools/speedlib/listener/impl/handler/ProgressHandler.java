package com.tools.speedlib.listener.impl.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.tools.speedlib.listener.impl.UIDownloadProgressListener;
import com.tools.speedlib.listener.impl.model.ProgressModel;

import java.lang.ref.WeakReference;

public abstract class ProgressHandler extends Handler {
    public static final int UPDATE = 0x01;
    public static final int START = 0x02;
    public static final int FINISH = 0x03;
    //弱引用
    private final WeakReference<UIDownloadProgressListener> mUIProgressListenerWeakReference;

    public ProgressHandler(UIDownloadProgressListener uiDownloadProgressListener) {
        super(Looper.getMainLooper());
        mUIProgressListenerWeakReference = new WeakReference<UIDownloadProgressListener>(uiDownloadProgressListener);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE: {
                UIDownloadProgressListener uiProgessListener = mUIProgressListenerWeakReference.get();
                if (uiProgessListener != null) {
                    //获得进度实体类
                    ProgressModel progressModel = (ProgressModel) msg.obj;
                    //回调抽象方法
                    downloadProgress(uiProgessListener, progressModel.getCurrentBytes(), progressModel.getContentLength(), progressModel.isDone());
                }
                break;
            }
            case START: {
                UIDownloadProgressListener uiDownloadProgressListener = mUIProgressListenerWeakReference.get();
                if (uiDownloadProgressListener != null) {
                    //获得进度实体类
                    ProgressModel progressModel = (ProgressModel) msg.obj;
                    //回调抽象方法
                    downloadStart(uiDownloadProgressListener, progressModel.getCurrentBytes(), progressModel.getContentLength(), progressModel.isDone());

                }
                break;
            }
            case FINISH: {
                UIDownloadProgressListener uiDownloadProgressListener = mUIProgressListenerWeakReference.get();
                if (uiDownloadProgressListener != null) {
                    //获得进度实体类
                    ProgressModel progressModel = (ProgressModel) msg.obj;
                    //回调抽象方法
                    downloadFinish(uiDownloadProgressListener, progressModel.getCurrentBytes(), progressModel.getContentLength(), progressModel.isDone());
                }
                break;
            }
            default:
                super.handleMessage(msg);
                break;
        }
    }

    public abstract void downloadStart(UIDownloadProgressListener uiDownloadProgressListener, long currentBytes, long contentLength, boolean done);

    public abstract void downloadProgress(UIDownloadProgressListener uiDownloadProgressListener, long currentBytes, long contentLength, boolean done);

    public abstract void downloadFinish(UIDownloadProgressListener uiDownloadProgressListener, long currentBytes, long contentLength, boolean done);
}
