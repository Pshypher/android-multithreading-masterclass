package com.techyourchance.multithreading.common.dependencyinjection;

import android.util.Log;

import com.techyourchance.threadposter.BackgroundThreadPoster;
import com.techyourchance.threadposter.UiThreadPoster;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ApplicationCompositionRoot {

    private UiThreadPoster mUiThreadPoster;
    private BackgroundThreadPoster mBackgroundThreadPoster;
    private ThreadPoolExecutor mThreadPoolExecutor;

    public UiThreadPoster getUiThreadPoster() {
        if (mUiThreadPoster == null) {
            mUiThreadPoster = new UiThreadPoster();
        }
        return mUiThreadPoster;
    }

    public BackgroundThreadPoster getBackgroundThreadPoster() {
        if (mBackgroundThreadPoster == null) {
            mBackgroundThreadPoster = new BackgroundThreadPoster();
        }
        return mBackgroundThreadPoster;
    }

    public ThreadPoolExecutor getThreadPool() {
        if (mThreadPoolExecutor == null) {
            mThreadPoolExecutor = new ThreadPoolExecutor(
                    10,
                    Integer.MAX_VALUE,
                    10,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<>(),
                    new ThreadFactory() {
                        @Override
                        public Thread newThread(Runnable r) {
                            Log.d("ThreadFactory",
                                  String.format("size %d, active count %d, queue remaining %d",
                                                mThreadPoolExecutor.getPoolSize(),
                                                mThreadPoolExecutor.getActiveCount(),
                                                mThreadPoolExecutor.getQueue().remainingCapacity()
                                  )
                            );
                            return new Thread(r);
                        }
                    }
            );
        }
        return mThreadPoolExecutor;
    }
}
