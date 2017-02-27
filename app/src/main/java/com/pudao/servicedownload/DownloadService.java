package com.pudao.servicedownload;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask mDownloadTask;

    private String downloadUrl;

    private DownloadBinder mBinder = new DownloadBinder();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    private DownloadListener mListener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1, getNotification("下载中", progress));
        }

        @Override
        public void onSuccess() {
            mDownloadTask = null;
            // 下载成功将前台服务通知关闭, 并创建一个下载成功的通知
            stopForeground(true);
            // 下载完成用-1表示
            getNotificationManager().notify(1, getNotification("下载完成", -1));
            System.out.println("下载完成");
        }

        @Override
        public void onFailed() {
            mDownloadTask = null;
            // 下载失败时将前台服务通知关闭,并创建一个下载失败的通知
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("下载失败", -1));
            System.out.println("下载失败");
        }

        @Override
        public void onPaused() {
            mDownloadTask = null;
            System.out.println("下载暂停");
        }

        @Override
        public void onCanceled() {
            mDownloadTask = null;
            stopForeground(true);
            System.out.println("下载取消");
        }
    };

    class DownloadBinder extends Binder{

        public void startDownload(String url){
            if (mDownloadTask == null) {
                downloadUrl = url;
                mDownloadTask = new DownloadTask(mListener);
                mDownloadTask.execute(downloadUrl);
                // 开启前台服务
                startForeground(1, getNotification("下载中...", 0));
                System.out.println("开始下载了");
            }
        }

        public void pauseDownload(){
            if (mDownloadTask != null) {
                mDownloadTask.pauseDownload();
            }
        }

        public void cancelDownload(){
            if (mDownloadTask != null) {
                mDownloadTask.cancelDownload();
            } else {
                // 取消下载时将文件删除, 并关闭通知
                if (downloadUrl != null) {
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.
                            getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()){
                        file.delete();
                    }
                    getNotificationManager().cancel(1);
                    stopForeground(true);
                    System.out.println("下载取消");
                }
            }
        }
    }

    private NotificationManager getNotificationManager(){
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(String title, int progress){
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentIntent(pi);
        builder.setContentTitle(title);
        if (progress > 0){
            // 当progress大于0时才需要显示进度
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }
}
