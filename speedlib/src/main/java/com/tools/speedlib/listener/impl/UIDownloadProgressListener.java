/**
 * Copyright 2015 ZhangQu Li
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tools.speedlib.listener.impl;

import android.os.Handler;
import android.os.Message;

import com.tools.speedlib.listener.DownloadProgressListener;
import com.tools.speedlib.listener.impl.handler.ProgressHandler;
import com.tools.speedlib.listener.impl.model.ProgressModel;

public abstract class UIDownloadProgressListener implements DownloadProgressListener {
    private int id;
    private boolean isFirst = false;
    private long updateTime = System.currentTimeMillis();
    private boolean isCancled = false;

    //主线程Handler
    private Handler mHandler;

    public UIDownloadProgressListener() {
        this.id = id;
        mHandler = new UIHandler(this, id);
    }

    //处理UI层的Handler子类
    private static class UIHandler extends ProgressHandler {
        private int id;

        public UIHandler(UIDownloadProgressListener uiDownloadProgressListener, int id) {
            super(uiDownloadProgressListener);
            this.id = id;
        }

        @Override
        public void downloadStart(UIDownloadProgressListener uiDownloadProgressListener, long currentBytes, long contentLength, boolean done) {
            if (uiDownloadProgressListener != null) {
                uiDownloadProgressListener.onUIDownloadStart(id, currentBytes, contentLength, done);
            }
        }

        @Override
        public void downloadProgress(UIDownloadProgressListener uiDownloadProgressListener, long currentBytes, long contentLength, boolean done) {
            if (uiDownloadProgressListener != null) {
                uiDownloadProgressListener.onUIDownloadProgress(id, currentBytes, contentLength, done);
            }
        }

        @Override
        public void downloadFinish(UIDownloadProgressListener uiDownloadProgressListener, long currentBytes, long contentLength, boolean done) {
            if (uiDownloadProgressListener != null) {
                uiDownloadProgressListener.onUIDownloadFinish(id, currentBytes, contentLength, done);
            }
        }
    }

    @Override
    public void onDownloadProgress(long bytesWrite, long contentLength, boolean done) {
        if (isDownloadCancled()) {
            return;
        }
        //如果是第一次，发送消息
        if (!isFirst) {
            isFirst = true;
            updateTime = System.currentTimeMillis();
            Message start = Message.obtain();
            start.obj = new ProgressModel(bytesWrite, contentLength, done);
            start.what = ProgressHandler.START;
//            Log.i("SheepYang", "Progress START");
            mHandler.sendMessage(start);
        }

        //通过Handler发送进度消息
        if (System.currentTimeMillis() - updateTime >= 1000) {
            updateTime = System.currentTimeMillis();
            Message message = Message.obtain();
            message.obj = new ProgressModel(bytesWrite, contentLength, done);
            message.what = ProgressHandler.UPDATE;
//            Log.i("SheepYang", "Progress UPDATE");
            mHandler.sendMessage(message);
        }

        if (done) {
            Message finish = Message.obtain();
            finish.obj = new ProgressModel(bytesWrite, contentLength, done);
            finish.what = ProgressHandler.FINISH;
//            Log.i("SheepYang", "Progress FINISH");
            mHandler.sendMessage(finish);
        }
    }

    @Override
    public boolean isDownloadCancled() {
        return isCancled;
    }

    @Override
    public void setDownloadCancled() {
        isCancled = true;
    }

    /**
     * UI层回调抽象方法
     *
     * @param currentBytes  当前的字节长度
     * @param contentLength 总字节长度
     * @param done          是否写入完成
     */
    public abstract void onUIDownloadProgress(int taskId, long currentBytes, long contentLength, boolean done);

    /**
     * UI层开始请求回调方法
     *
     * @param currentBytes  当前的字节长度
     * @param contentLength 总字节长度
     * @param done          是否写入完成
     */
    public void onUIDownloadStart(int taskId, long currentBytes, long contentLength, boolean done) {

    }

    /**
     * UI层结束请求回调方法
     *
     * @param currentBytes  当前的字节长度
     * @param contentLength 总字节长度
     * @param done          是否写入完成
     */
    public void onUIDownloadFinish(int taskId, long currentBytes, long contentLength, boolean done) {

    }
}
