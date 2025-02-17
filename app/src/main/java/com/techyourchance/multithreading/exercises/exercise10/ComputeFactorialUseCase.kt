package com.techyourchance.multithreading.exercises.exercise10

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

    sealed class Result {
        data class Success(val factorial: BigInteger) : Result()
        data object Timeout : Result()
    }

    suspend fun computeFactorial(argument: Int, timeout: Int): Result {
        return withContext(Dispatchers.IO) {
            val numberOfThreads = if (argument < 20)
                1
            else Runtime.getRuntime().availableProcessors();
            val computationRanges = getThreadsComputationRanges(numberOfThreads, argument)

            try {
                withTimeout(timeout.toLong()) {
                    val deferredComputationResults = startComputation(computationRanges)
                    processComputationResults(deferredComputationResults.awaitAll())
                }
            } catch (e: TimeoutCancellationException) {
                Result.Timeout
            }
        }
    }

    private fun getThreadsComputationRanges(threads: Int, factorialArgument: Int): List<ComputationRange> {
        val computationRanges = mutableListOf<ComputationRange>()
        val computationRangeSize = factorialArgument / threads

        var nextComputationRangeEnd = factorialArgument.toLong()
        for (i in threads - 1 downTo 0) {
            computationRanges.add(
                ComputationRange(
                    nextComputationRangeEnd - computationRangeSize + 1,
                    nextComputationRangeEnd
                )
            )
            nextComputationRangeEnd = computationRanges.last().start - 1
        }

        // add potentially "remaining" values to first thread's range
        computationRanges[0] = ComputationRange(1, computationRanges[0].end)
        return computationRanges
    }

    private fun CoroutineScope.startComputation(computationRanges: List<ComputationRange>): List<Deferred<BigInteger>> {
        return computationRanges.map { range ->
            async(Dispatchers.IO) {
                var product = BigInteger("1")
                for (num in range.start..range.end) {
                    product = product.multiply(BigInteger(num.toString()))
                }
                product
            }
        }
    }

    private fun processComputationResults(rangeResults: List<BigInteger>): Result {
        val result = rangeResults.fold(BigInteger.valueOf(1)) { acc, x ->
            acc.multiply(x)
        }
        return Result.Success(result)
    }

    private data class ComputationRange(val start: Long, val end: Long)
}
