package com.techyourchance.multithreading.exercises.exercise10

import androidx.annotation.WorkerThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.math.BigInteger

class ComputeFactorialUseCase {

    private var numberOfThreads: Int = 0
    private var threadsComputationRanges: Array<ComputationRange?> = arrayOf()

    private var computationTimeoutTime: Long = 0

    suspend fun computeFactorial(argument: Int, timeout: Int): String {
        return withContext(Dispatchers.IO) {
            initComputationParams(argument, timeout)
            try {
                withTimeout(remainingMillisToTimeout()) {
                    val deferredComputationResults = startComputation()
                    processComputationResults(deferredComputationResults.awaitAll())
                }
            } catch (e: TimeoutCancellationException) {
                "Computation timed out"

            }
        }
    }

    private fun initComputationParams(factorialArgument: Int, timeout: Int) {
        numberOfThreads = if (factorialArgument < 20)
            1
        else
            Runtime.getRuntime().availableProcessors()

        threadsComputationRanges = arrayOfNulls(numberOfThreads)

        initThreadsComputationRanges(factorialArgument)

        computationTimeoutTime = System.currentTimeMillis() + timeout
    }

    private fun initThreadsComputationRanges(factorialArgument: Int) {
        val computationRangeSize = factorialArgument / numberOfThreads

        var nextComputationRangeEnd = factorialArgument.toLong()
        for (i in numberOfThreads - 1 downTo 0) {
            threadsComputationRanges[i] = ComputationRange(
                nextComputationRangeEnd - computationRangeSize + 1,
                nextComputationRangeEnd
            )
            nextComputationRangeEnd = threadsComputationRanges[i]!!.start - 1
        }

        // add potentially "remaining" values to first thread's range
        threadsComputationRanges[0] = ComputationRange(1, threadsComputationRanges[0]!!.end)
    }

    private fun CoroutineScope.startComputation(): List<Deferred<BigInteger>> {
        val deferredComputationResults = mutableListOf<Deferred<BigInteger>>()
        for (i in 0 until numberOfThreads) {
            deferredComputationResults.add(async(Dispatchers.IO) {
                val rangeStart = threadsComputationRanges[i]!!.start
                val rangeEnd = threadsComputationRanges[i]!!.end
                var product = BigInteger("1")
                for (num in rangeStart..rangeEnd) {
                    if (isTimedOut()) {
                        break
                    }
                    product = product.multiply(BigInteger(num.toString()))
                }
                product
            })
        }

        return deferredComputationResults
    }

    @WorkerThread
    private fun processComputationResults(rangeResults: List<BigInteger>): String {
        if (isTimedOut()) {
            return "Computation timed out"
        }

        return computeFinalResult(rangeResults)
    }

    @WorkerThread
    private fun computeFinalResult(rangeResults: List<BigInteger>): String {
        val result = rangeResults.fold(BigInteger.valueOf(1)) { acc, x ->
            if (isTimedOut()) {
                return "Computation timed out"
            }
            acc.multiply(x)
        }

        return result.toString()
    }

    private fun remainingMillisToTimeout(): Long {
        return computationTimeoutTime - System.currentTimeMillis()
    }

    private fun isTimedOut(): Boolean {
        return System.currentTimeMillis() >= computationTimeoutTime
    }


    private data class ComputationRange(val start: Long, val end: Long)
}
