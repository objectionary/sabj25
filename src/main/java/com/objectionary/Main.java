// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package com.objectionary;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark of a Stream pipeline that filters even numbers, doubles them,
 * and sums the result.
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

    private final int[] numbers = IntStream.rangeClosed(1, 10_000_000).toArray();

    @Benchmark
    public long sum() {
        final long sum = Arrays.stream(this.numbers)
            .filter(number -> number % 2 == 0)
            .mapToLong(number -> number * 2L)
            .sum();
        if (sum != 50_000_010_000_000L) {
            throw new IllegalStateException(
                String.format("the sum %d does not match the expected 50000010000000", sum)
            );
        }
        return sum;
    }
}
