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
 * one of only scalar, one-to-one conversions, one of every stateless
 * operation, one of the stateful operations that must remember state, and
 * one of repeated map and filter operations whose many lambdas turn the call
 * sites megamorphic.
 *
 * <p>Each {@code @Benchmark} method is a thin instance wrapper that delegates
 * to a {@code private static} method holding the actual stream pipeline. Hone's
 * entry-point rule {@code 111-invokedynamic-to-lambda} only matches a method
 * whose JEO modifiers contain {@code static ↦ Φ.true}; keeping the pipelines in
 * static methods lets the lambda lift start, so the {@code streams/*} fusion
 * actually touches them.</p>
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
        return Main.scalar(this.numbers, blackhole);
    }

    @Benchmark
    public long stateless(final Blackhole blackhole) {
        return Main.stateless(this.numbers, blackhole);
    }

    @Benchmark
    public long stateful() {
        return Main.stateful(this.numbers);
    }

    @Benchmark
    public long megamorphic() {
        return Main.megamorphic(this.numbers);
    }

    private static long scalar(final long[] numbers, final Blackhole blackhole) {
        return Main.verified(
            Arrays.stream(numbers)
                .filter(number -> number % 2L == 0L)
                .map(number -> number + 1L)
                .peek(blackhole::consume)
                .mapToDouble(number -> (double) number)
                .mapToObj(Double::valueOf)
                .mapToLong(Double::longValue)
                .asDoubleStream()
                .mapToInt(value -> (int) value)
                .asLongStream()
                .boxed()
                .mapToLong(Long::longValue)
                .map(number -> number * 2L)
                .filter(number -> number > 4L)
                .peek(blackhole::consume)
                .mapToDouble(number -> (double) number)
                .map(value -> value + 0.5)
                .mapToObj(Double::valueOf)
                .mapToLong(Double::longValue)
                .asDoubleStream()
                .mapToInt(value -> (int) value)
                .asLongStream()
                .boxed()
                .mapToLong(Long::longValue)
                .sum(),
            500_002_000_000L
        );
    }

    private static long stateless(final long[] numbers, final Blackhole blackhole) {
        return Main.verified(
            Arrays.stream(numbers)
                .filter(number -> number % 2L == 0L)
                .map(number -> number + 1L)
                .peek(blackhole::consume)
                .mapToDouble(number -> (double) number)
                .map(value -> value * 2.0)
                .mapToLong(value -> (long) value)
                .flatMap(LongStream::of)
                .mapMulti((number, sink) -> sink.accept(number))
                .boxed()
                .mapToLong(Long::longValue)
                .asDoubleStream()
                .mapToObj(Double::valueOf)
                .mapToInt(Double::intValue)
                .asLongStream()
                .sum(),
            500_002_000_000L
        );
    }

    private static long stateful(final long[] numbers) {
        return Main.verified(
            Arrays.stream(numbers)
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

    private static long megamorphic(final long[] numbers) {
        return Main.verified(
            Arrays.stream(numbers)
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

    private static long verified(final long sum, final long expected) {
        if (sum != expected) {
            throw new IllegalStateException(
                String.format("the sum %d does not match the expected %d", sum, expected)
            );
        }
        return sum;
    }
}
