package com.pudao.servicedownload;

/**
 * Created by pucheng on 2017/2/24.
 * 用于下载监听的回调
 */

public interface DownloadListener {

    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCanceled();

}
