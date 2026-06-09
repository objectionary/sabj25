// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package sabj25;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LongSummaryStatistics;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;
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
 * of the stateful operations that must remember state, one of repeated map and
 * filter operations whose many lambdas turn the call sites megamorphic, one
 * that drives the primitive chain in parallel across the fork-join pool, one of
 * the Java 25 gatherers, one of the numeric reduction terminals gathered into a
 * single summary, one of the short-circuiting terminals, and one that collects
 * an object stream through composed collectors.
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

    @Benchmark
    public long parallel(final Blackhole blackhole) {
        return this.verified(
            Arrays.stream(this.numbers)
                .parallel()
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
    public long gatherer(final Blackhole blackhole) {
        return this.verified(
            Arrays.stream(this.numbers)
                .boxed()
                .gather(Gatherers.windowFixed(100))
                .gather(Gatherers.mapConcurrent(8, window -> window.stream().mapToLong(Long::longValue).sum()))
                .peek(blackhole::consume)
                .gather(Gatherers.scan(() -> 0L, Long::sum))
                .gather(Gatherers.windowSliding(2))
                .map(window -> window.getLast() - window.getFirst())
                .peek(blackhole::consume)
                .gather(Gatherers.fold(() -> 0L, Long::sum))
                .mapToLong(Long::longValue)
                .sum(),
            500_000_494_950L
        );
    }

    @Benchmark
    public long reduction() {
        final LongSummaryStatistics stats = Arrays.stream(this.numbers)
            .filter(number -> number % 3L != 0L)
            .map(number -> number * 2L - 1L)
            .summaryStatistics();
        return this.verified(
            stats.getSum() + stats.getMin() + stats.getMax() + stats.getCount() + (long) stats.getAverage(),
            666_670_333_333L
        );
    }

    @Benchmark
    public long shortcircuit() {
        final boolean any = Arrays.stream(this.numbers).anyMatch(number -> number > 999_990L);
        final boolean all = Arrays.stream(this.numbers).allMatch(number -> number < 2_000_000L);
        final boolean none = Arrays.stream(this.numbers).noneMatch(number -> number > 2_000_000L);
        final long first = Arrays.stream(this.numbers)
            .filter(number -> number > 999_000L)
            .map(number -> number + 7L)
            .findFirst()
            .getAsLong();
        final long single = Arrays.stream(this.numbers)
            .filter(number -> number == 777_777L)
            .map(number -> number * 2L)
            .findAny()
            .getAsLong();
        return this.verified(
            (any ? 1L : 0L) + (all ? 1L : 0L) + (none ? 1L : 0L) + first + single,
            2_554_565L
        );
    }

    @Benchmark
    public long collectors() {
        return this.verified(
            Arrays.stream(this.numbers)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .collect(
                    Collectors.teeing(
                        Collectors.groupingBy(
                            number -> number % 4L,
                            Collectors.filtering(
                                number -> number > 10L,
                                Collectors.mapping(number -> number * 2L, Collectors.counting())
                            )
                        ),
                        Collectors.partitioningBy(
                            number -> number % 2L == 0L,
                            Collectors.summingLong(Long::longValue)
                        ),
                        (grouped, partitioned) ->
                            grouped.values().stream().mapToLong(Long::longValue).sum()
                                + partitioned.values().stream().mapToLong(Long::longValue).sum()
                    )
                ),
            500_001_499_990L
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
