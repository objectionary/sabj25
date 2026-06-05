// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package com.objectionary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks of long stream pipelines over an array of one million numbers,
 * starting with a pipeline of only lightweight, stateless operations.
 *
 * @since 0.0.1
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class Main {

    private final long[] numbers = LongStream.rangeClosed(1, 1_000_000).toArray();

    @Benchmark
    public long lightweight(final Blackhole blackhole) {
        return this.verified(
            Arrays.stream(this.numbers)
                .filter(number -> number % 2L == 0L)
                .map(number -> number + 1L)
                .peek(blackhole::consume)
                .mapToDouble(number -> (double) number)
                .mapToLong(value -> (long) value)
                .asDoubleStream()
                .mapToInt(value -> (int) value)
                .asLongStream()
                .boxed()
                .mapToLong(Long::longValue)
                .sum(),
            250_001_000_000L
        );
    }

    private long verified(final long sum, final long expected) {
        if (sum != expected) {
            throw new IllegalStateException(
                String.format("the sum %d does not match the expected %d", sum, expected)
            );
        }
        return sum;
    }
}
