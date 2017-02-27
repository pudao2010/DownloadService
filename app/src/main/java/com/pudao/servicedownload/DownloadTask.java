package com.pudao.servicedownload;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by pucheng on 2017/2/24.
 * 用于下载的异步任务
 */

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    private static final int TYPE_SUCCESS = 0;
    private static final int TYPE_FAILED = 1;
    private static final int TYPE_PAUSED = 2;
    private static final int TYPE_CANCELED = 3;

    private boolean isCanceled = false;

    private boolean isPaused = false;

    private int lastProgress; // 记录上次下载的进度

    private DownloadListener mListener;

    public DownloadTask(DownloadListener listener) {
        this.mListener = listener;
    }


    // 后台中执行
    @Override
    protected Integer doInBackground(String... params) {
        // 执行具体的下载逻辑
        InputStream is = null;
        // 随机访问流,可以实现断点下载功能
        RandomAccessFile saveFile = null;
        // 下载保存的文件
        File file = null;
        // 记录已经下载的文件长度
        try {
            long downloadedLength = 0;
            String downloadUrl = params[0];
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            // SD卡的Download目录
            String directory = Environment.getExternalStoragePublicDirectory
                    (Environment.DIRECTORY_DOWNLOADS).getPath();
            file = new File(directory + fileName);
            if (file.exists()) {
                downloadedLength = file.length();
            }
            long contentLength = getContentLength(downloadUrl);
            if (contentLength == 0) {
                return TYPE_FAILED;
            } else if (contentLength == downloadedLength) {
                // 已下载的字节和文件总字节相等,说明已经下载完成
                return TYPE_SUCCESS;
            }
            // 其他情况继续下载
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    // 断点下载,指定从哪个字节开始下载
                    .addHeader("RANGE", "bytes=" + downloadedLength + "-")
                    .url(downloadUrl)
                    .build();
            Response response = client.newCall(request).execute();
            if (response != null) {
                is = response.body().byteStream();
                saveFile = new RandomAccessFile(file, "rw");
                // 跳过已下载的字节
                saveFile.seek(downloadedLength);
                byte[] buffer = new byte[1024];
                int total = 0;
                int len;
                while ((len = is.read(buffer)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;
                    } else if (isPaused) {
                        return TYPE_SUCCESS;
                    } else {
                        total += len;
                        saveFile.write(buffer, 0, len);
                        // 计算已经下载的百分比
                        int progress = (int) ((total + downloadedLength) * 100 / contentLength);
                        // 对外发布进度
                        publishProgress(progress);
                    }
                }
                // 下载完
                response.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (saveFile != null) {
                    saveFile.close();
                }
                // 如果取消下载就删除文件
                if (isCanceled && file != null){
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    // 根据下载的URL地址获取 下载文件的长度
    private long getContentLength(String downloadUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();
        Response response = client.newCall(request).execute();
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.body().close();
            return contentLength;
        }
        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        Integer progress = values[0];
        if (progress > lastProgress){
            mListener.onProgress(progress);
            // 更新上次的进度
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer integer) {
        switch (integer){
            case TYPE_SUCCESS:
                mListener.onSuccess();
                break;
            case TYPE_FAILED:
                mListener.onFailed();
                break;
            case TYPE_PAUSED:
                mListener.onPaused();
                break;
            case TYPE_CANCELED:
                mListener.onCanceled();
                break;
            default:
                break;
        }
    }

    // 对外提供暂停下载的方法
    public void pauseDownload(){
        isPaused = true;
    }

    // 对外提供取消下载的方法
    public void cancelDownload(){
        isCanceled = true;
    }
}
