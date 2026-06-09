// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package sabj25;

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
 * Benchmarks of long stream pipelines over an array of one million numbers:
 * one of only scalar, one-to-one conversions, one like it that stays with
 * primitive longs to avoid all boxing, one of every stateless operation, one
 * of the stateful operations that must remember state, and one of repeated
 * map and filter operations whose many lambdas turn the call sites
 * megamorphic.
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
    public long scalar(final Blackhole blackhole) {
        return this.verified(
            Arrays.stream(this.numbers)
                .filter(number -> number % 2L == 0L)
                .map(number -> number * 3L - 1L)
                .asDoubleStream()
                .mapToObj(value -> Double.valueOf(value + 2.0))
                .peek(blackhole::consume)
                .mapToInt(value -> (int) (value - 2.0))
                .asLongStream()
                .boxed()
                .peek(blackhole::consume)
                .mapToDouble(value -> value + 0.5)
                .mapToLong(value -> (long) value + 7L)
                .sum(),
            750_004_500_000L
        );
    }

    @Benchmark
    public long longlar(final Blackhole blackhole) {
        return this.verified(
            Arrays.stream(this.numbers)
                .filter(number -> number % 2L == 0L)
                .map(number -> number + 7L)
                .peek(blackhole::consume)
                .map(number -> number * 2L)
                .map(number -> number - 1L)
                .peek(blackhole::consume)
                .map(number -> number + 4L)
                .sum(),
            500_009_500_000L
        );
    }

    @Benchmark
    public long stateless(final Blackhole blackhole) {
        return this.verified(
            Arrays.stream(this.numbers)
                .filter(number -> number % 2L == 0L)
                .map(number -> number + 5L)
                .flatMap(number -> LongStream.of(number - 3L))
                .mapMulti((number, sink) -> sink.accept(number + 2L))
                .asDoubleStream()
                .mapToObj(value -> Double.valueOf(value * 2.0))
                .peek(blackhole::consume)
                .mapToInt(value -> (int) (value / 2.0))
                .asLongStream()
                .boxed()
                .peek(blackhole::consume)
                .mapToDouble(value -> value - 1.5)
                .mapToLong(value -> (long) value + 4L)
                .sum(),
            250_003_500_000L
        );
    }

    @Benchmark
    public long stateful() {
        return this.verified(
            Arrays.stream(this.numbers)
                .skip(1_000)
                .limit(800_000)
                .sorted()
                .distinct()
                .dropWhile(number -> number < 50_000L)
                .takeWhile(number -> number < 700_000L)
                .sum(),
            243_749_675_000L
        );
    }

    @Benchmark
    public long megamorphic() {
        return this.verified(
            Arrays.stream(this.numbers)
                .filter(number -> number % 2L == 0L)
                .map(number -> number + 3L)
                .filter(number -> number % 5L != 0L)
                .map(number -> number * 2L)
                .filter(number -> number > 100L)
                .map(number -> number - 7L)
                .filter(number -> number % 3L != 0L)
                .map(number -> number + 11L)
                .filter(number -> number < 1_900_000L)
                .map(number -> number / 2L)
                .filter(number -> number % 7L != 0L)
                .map(number -> number + 1L)
                .sum(),
            103_142_829_694L
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
