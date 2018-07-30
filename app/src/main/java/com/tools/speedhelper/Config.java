package com.tools.speedhelper;

import android.os.Environment;

import java.io.File;

/**
 * TODO 备注
 *
 * @author SheepYang
 * @since 2018/7/25 11:02
 */

public class Config {


    public static File getDirFile(String subFileName) {
        boolean isSDMounted = isSDMounted();
        File file = null;
        if (isSDMounted) {
            file = mkdirsFolder(getSDirAbsolutePath() + "/SpeedLib/" + subFileName);
            return file;
        } else {
            return file;
        }
    }

    public static boolean isSDMounted() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static File mkdirsFolder(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return file.mkdirs() ? file : null;
        } else {
            return file;
        }
    }

    public static String getSDirAbsolutePath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

}