package com.techyourchance.multithreading.exercises.exercise9;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.WorkerThread;

import com.techyourchance.multithreading.common.BaseObservable;

import java.math.BigInteger;

import io.reactivex.Observable;

public class ComputeFactorialUseCase {

    public record Result(String result) { }

    private final Object LOCK = new Object();

    private int mNumberOfThreads;
    private ComputationRange[] mThreadsComputationRanges;
    private volatile BigInteger[] mThreadsComputationResults;
    private int mNumOfFinishedThreads = 0;

    private long mComputationTimeoutTime;

    private boolean mAbortComputation;

    public Observable<Result> computeFactorialAndNotify(final int argument, final int timeout) {
        return Observable.fromCallable(
                () -> {
                    initComputationParams(argument, timeout);
                    startComputation();
                    waitForThreadsResultsOrTimeoutOrAbort();
                    return processComputationResults();
                }
        );
    }

    private void initComputationParams(int factorialArgument, int timeout) {
        mNumberOfThreads = factorialArgument < 20
                ? 1 : Runtime.getRuntime().availableProcessors();

        synchronized (LOCK) {
            mNumOfFinishedThreads = 0;
            mAbortComputation = false;
        }

        mThreadsComputationResults = new BigInteger[mNumberOfThreads];

        mThreadsComputationRanges = new ComputationRange[mNumberOfThreads];

        initThreadsComputationRanges(factorialArgument);

        mComputationTimeoutTime = System.currentTimeMillis() + timeout;
    }

    private void initThreadsComputationRanges(int factorialArgument) {
        int computationRangeSize = factorialArgument / mNumberOfThreads;

        long nextComputationRangeEnd = factorialArgument;
        for (int i = mNumberOfThreads - 1; i >= 0; i--) {
            mThreadsComputationRanges[i] = new ComputationRange(
                    nextComputationRangeEnd - computationRangeSize + 1,
                    nextComputationRangeEnd
            );
            nextComputationRangeEnd = mThreadsComputationRanges[i].start - 1;
        }

        // add potentially "remaining" values to first thread's range
        mThreadsComputationRanges[0].start = 1;
    }

    @WorkerThread
    private void startComputation() {
        for (int i = 0; i < mNumberOfThreads; i++) {

            final int threadIndex = i;

            new Thread(() -> {
                long rangeStart = mThreadsComputationRanges[threadIndex].start;
                long rangeEnd = mThreadsComputationRanges[threadIndex].end;
                BigInteger product = new BigInteger("1");
                for (long num = rangeStart; num <= rangeEnd; num++) {
                    if (isTimedOut()) {
                        break;
                    }
                    product = product.multiply(new BigInteger(String.valueOf(num)));
                }
                mThreadsComputationResults[threadIndex] = product;

                synchronized (LOCK) {
                    mNumOfFinishedThreads++;
                    LOCK.notifyAll();
                }

            }).start();
        }
    }

    @WorkerThread
    private void waitForThreadsResultsOrTimeoutOrAbort() {
        synchronized (LOCK) {
            while (mNumOfFinishedThreads != mNumberOfThreads && !mAbortComputation && !isTimedOut()) {
                try {
                    LOCK.wait(getRemainingMillisToTimeout());
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }

    @WorkerThread
    private Result processComputationResults() {
        if (mAbortComputation) {
            return new Result("Computation aborted");
        }

        BigInteger value = computeFinalResult();

        // need to check for timeout after computation of the final result
        if (isTimedOut()) {
            return new Result("Computation timed out");
        }

        return new Result(value.toString());
    }

    @WorkerThread
    private BigInteger computeFinalResult() {
        BigInteger value = new BigInteger("1");
        for (int i = 0; i < mNumberOfThreads; i++) {
            if (isTimedOut()) {
                break;
            }
            value = value.multiply(mThreadsComputationResults[i]);
        }
        return value;
    }

    private long getRemainingMillisToTimeout() {
        return mComputationTimeoutTime - System.currentTimeMillis();
    }

    private boolean isTimedOut() {
        return System.currentTimeMillis() >= mComputationTimeoutTime;
    }


    private static class ComputationRange {
        private long start;
        private final long end;

        public ComputationRange(long start, long end) {
            this.start = start;
            this.end = end;
        }
    }
}
