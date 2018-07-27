package com.tools.speedlib.utils;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * TODO 备注
 *
 * @author SheepYang
 * @since 2018/7/27 19:46
 */
public class FileUtil {
    private static final String FILE_BASE = "SpeedLib";
    private static final String FILE_NAME = "download.apk";

    public static File getDownFile() {
        String filename = FILE_NAME;
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
}
