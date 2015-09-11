package ua.video.opensvit.utils;

import android.support.v4.content.Loader;
import android.widget.ProgressBar;

import ua.video.opensvit.VideoStreamApp;
import ua.video.opensvit.http.IOkHttpLoadInfo;
import ua.video.opensvit.http.OkHttpAsyncTask;
import ua.video.opensvit.http.OkHttpClientRunnable;
import ua.video.opensvit.http.OkHttpLoader;

public class HttpRequestsCreator {
    private HttpRequestsCreator() {
    }

    public static Loader<String> createLoader(ProgressBar progressBar, String url, IOkHttpLoadInfo
            .GetLoaderCreateInfo loadInfo, OkHttpAsyncTask.OnLoadFinishedListener
                                                      mLoadFinishedListener) {
        return new OkHttpLoader(VideoStreamApp.getInstance()
                .getApplicationContext(), createTask(progressBar, url, loadInfo,
                mLoadFinishedListener));
    }

    public static OkHttpAsyncTask createTask(ProgressBar progressBar, String url, IOkHttpLoadInfo
            .GetLoaderCreateInfo loadInfo, OkHttpAsyncTask.OnLoadFinishedListener
                                                     mLoadFinishedListener) {
        OkHttpClientRunnable mRunnable = createRunnable(url, loadInfo);
        OkHttpAsyncTask task = new OkHttpAsyncTask(progressBar, mRunnable);
        task.setOnLoadFinishedListener(mLoadFinishedListener);
        return task;
    }

    public static OkHttpClientRunnable createRunnable(String url, IOkHttpLoadInfo
            .GetLoaderCreateInfo loadInfo) {
        OkHttpClientRunnable mRunnable = new OkHttpClientRunnable(url, loadInfo);
        return mRunnable;
    }

    public static OkHttpClientRunnable populateWithResultListener(OkHttpClientRunnable runnable,
                                                                  OkHttpClientRunnable
                                                                          .OnLoadResultListener
                                                                          mListener) {
        runnable.setOnLoadResultListener(mListener);
        return runnable;
    }
}
