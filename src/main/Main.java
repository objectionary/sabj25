// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package sabj25;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
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
 * single summary, one of the short-circuiting terminals, one that collects an
 * object stream through composed collectors, one of the general reduction
 * terminals reduce and mutable collect, one sourced from generators and
 * concatenation rather than an array, one over a reference stream of records
 * sorted by comparator and folded through map and joining collectors, one that
 * expands every element through fan-out flat-maps, one of the materializing and
 * traversing terminals, and one that runs stateful operations and concurrent
 * collectors across the fork-join pool.
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

    @Benchmark
    public long fold(final Blackhole blackhole) {
        final long reduced = Arrays.stream(this.numbers)
            .filter(number -> number % 2L == 0L)
            .map(number -> number * 3L - 1L)
            .reduce(0L, Long::sum);
        final long combined = Arrays.stream(this.numbers)
            .boxed()
            .peek(blackhole::consume)
            .reduce(0L, (total, number) -> total + number - 1L, Long::sum);
        final long collected = Arrays.stream(this.numbers)
            .collect(() -> new long[1], (box, number) -> box[0] += number + 3L, (left, right) -> left[0] += right[0])[0];
        return this.verified(reduced + combined + collected, 1_750_004_000_000L);
    }

    @Benchmark
    public long generated(final Blackhole blackhole) {
        final long iterated = LongStream.iterate(1L, number -> number <= 500_000L, number -> number + 1L)
            .map(number -> number + 1L)
            .sum();
        final long made = Stream.generate(() -> 2L)
            .limit(500_000L)
            .peek(blackhole::consume)
            .mapToLong(Long::longValue)
            .map(number -> number * 3L)
            .sum();
        final long concatenated = LongStream.concat(
                LongStream.range(0L, 250_000L),
                LongStream.range(250_000L, 500_000L)
            )
            .map(number -> number + 5L)
            .sum();
        return this.verified(iterated + made + concatenated, 250_006_000_000L);
    }

    @Benchmark
    public long objects(final Blackhole blackhole) {
        final Map<Long, Long> grouped = Arrays.stream(this.numbers)
            .mapToObj(number -> new Pair(number, number % 8L))
            .peek(blackhole::consume)
            .sorted(Comparator.comparingLong(Pair::head).reversed())
            .collect(Collectors.toMap(Pair::tail, Pair::head, Long::sum));
        final long reduced = Arrays.stream(this.numbers)
            .mapToObj(number -> new Pair(number, number + 1L))
            .peek(blackhole::consume)
            .collect(
                Collectors.collectingAndThen(
                    Collectors.reducing(0L, Pair::tail, Long::sum),
                    total -> total - 1L
                )
            );
        final long joined = Arrays.stream(this.numbers)
            .limit(1_000L)
            .mapToObj(Long::toString)
            .collect(Collectors.joining(","))
            .length();
        return this.verified(
            grouped.values().stream().mapToLong(Long::longValue).sum() + reduced + joined,
            1_000_002_003_891L
        );
    }

    @Benchmark
    public long fanout(final Blackhole blackhole) {
        final long expanded = Arrays.stream(this.numbers)
            .limit(200_000L)
            .boxed()
            .peek(blackhole::consume)
            .flatMap(number -> Stream.of(number, number + 1L, number + 2L))
            .flatMapToLong(number -> LongStream.of(number, number * 2L))
            .sum();
        final long widened = Arrays.stream(this.numbers)
            .limit(200_000L)
            .mapToObj(Long::valueOf)
            .peek(blackhole::consume)
            .flatMapToInt(number -> IntStream.of((int) (number % 100L), (int) (number % 7L)))
            .asLongStream()
            .sum();
        final double real = Arrays.stream(this.numbers)
            .limit(200_000L)
            .mapToObj(Double::valueOf)
            .peek(blackhole::consume)
            .flatMapToDouble(number -> DoubleStream.of(number + 0.5, number - 0.5))
            .sum();
        return this.verified(expanded + widened + (long) real, 220_013_399_997L);
    }

    @Benchmark
    public long materialize(final Blackhole blackhole) {
        final long[] array = Arrays.stream(this.numbers)
            .filter(number -> number % 2L == 0L)
            .map(number -> number + 1L)
            .toArray();
        final List<Long> list = Arrays.stream(this.numbers)
            .filter(number -> number % 3L == 0L)
            .boxed()
            .peek(blackhole::consume)
            .toList();
        final long count = Arrays.stream(this.numbers)
            .filter(number -> number % 5L == 0L)
            .count();
        final long min = Arrays.stream(this.numbers)
            .map(number -> number * 2L)
            .min()
            .getAsLong();
        final long max = Arrays.stream(this.numbers)
            .map(number -> number - 3L)
            .max()
            .getAsLong();
        final long[] total = new long[1];
        Arrays.stream(this.numbers)
            .filter(number -> number % 7L == 0L)
            .forEach(number -> total[0] += number);
        final long[] ordered = new long[1];
        Arrays.stream(this.numbers)
            .filter(number -> number % 11L == 0L)
            .forEachOrdered(number -> ordered[0] += number);
        return this.verified(
            Arrays.stream(array).sum() + (long) list.size() + count + min + max + total[0] + ordered[0],
            366_886_416_448L
        );
    }

    @Benchmark
    public long concurrent(final Blackhole blackhole) {
        final long stateful = Arrays.stream(this.numbers)
            .parallel()
            .map(number -> number % 100_000L)
            .distinct()
            .sorted()
            .map(number -> number + 1L)
            .sum();
        final ConcurrentMap<Long, Long> grouped = Arrays.stream(this.numbers)
            .parallel()
            .boxed()
            .peek(blackhole::consume)
            .collect(
                Collectors.groupingByConcurrent(
                    number -> number % 16L,
                    Collectors.summingLong(Long::longValue)
                )
            );
        final ConcurrentMap<Boolean, Long> partitioned = Arrays.stream(this.numbers)
            .parallel()
            .boxed()
            .collect(
                Collectors.toConcurrentMap(
                    number -> number % 2L == 0L,
                    Long::longValue,
                    Long::sum
                )
            );
        return this.verified(
            stateful
                + grouped.values().stream().mapToLong(Long::longValue).sum()
                + partitioned.values().stream().mapToLong(Long::longValue).sum(),
            1_005_001_050_000L
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

    /**
     * A pairing of a number with a value derived from it, giving the object
     * pipelines a genuine reference type to carry, sort, and key on rather than
     * a boxed primitive.
     *
     * @param head The original number
     * @param tail A number derived from the original
     * @since 0.0.1
     */
    private record Pair(long head, long tail) {
    }
}
