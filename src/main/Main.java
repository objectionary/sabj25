// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package sabj25;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.NavigableMap;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
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
 * one per facet of the Stream API: one of every stateless operation,
 * including all the scalar one-to-one conversions, that also drives a
 * primitive long chain sequentially and across the fork-join pool and
 * repeats map and filter through enough distinct lambdas to turn the call
 * sites megamorphic, one of the stateful operations that must remember
 * state, one of the Java 25 gatherers together with gatherers built and
 * composed by hand and a hand-rolled collector of supplier, accumulator,
 * combiner, and finisher, one that collects through composed teeing,
 * grouping, and partitioning collectors, nests collectors inside collectors
 * through tree and linked maps, harvests the collectors the others leave
 * untouched, and sorts a reference stream of records through composed
 * comparators into joined text, one of the general reduction terminals
 * reduce and mutable collect run both sequentially and across the fork-join
 * pool together with the primitive int, double, and long streams gathered
 * into summary statistics with boxed round-trips and averaging, one that
 * expands every element through fan-out flat-maps and the primitive
 * map-multi variants, one of the materializing terminals that also drains a
 * pipeline through its own iterator and spliterator, one that runs stateful
 * operations and concurrent collectors across the fork-join pool, one that
 * measures the fixed overhead of a pipeline over only a handful of
 * elements, one sourced from stream builders, generators, concatenation, an
 * infinite iterate, hand-made spliterators, seeded pseudorandom int, long,
 * and double generators, and a list and a set rather than an array that
 * also relaxes encounter order so the parallel pipeline may skip ordering,
 * one that walks the characters, code points, regular-expression matches,
 * and buffered lines of a block of text, and one that drives the
 * order-sensitive slicing and short-circuiting matching and finding
 * terminals across the fork-join pool. The sequential pipelines that sort
 * draw from a deterministically shuffled copy of the source rather than the
 * ascending range, so {@code sorted} pays its full comparison cost instead
 * of the near-linear best case the adaptive sort takes on already-ordered
 * input.
 *
 * @since 0.0.1
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(2)
public class Main {

    private final long[] numbers = LongStream.rangeClosed(1, 1_000_000).toArray();

    private final long[] scrambled = Main.scrambled(this.numbers);

    private final int[] integers = IntStream.rangeClosed(1, 1_000_000).toArray();

    private final double[] decimals =
        LongStream.rangeClosed(1L, 1_000_000L).asDoubleStream().toArray();

    private final List<Long> list = Arrays.stream(this.numbers).boxed().toList();

    private final Set<Long> set = Set.copyOf(this.list);

    private final String prose =
        "the quick brown fox jumps over the lazy dog ".repeat(10_000);

    private final String document = LongStream.rangeClosed(1L, 10_000L)
        .mapToObj(Long::toString)
        .collect(Collectors.joining("\n"));

    @Benchmark
    public long stateless(final Blackhole blackhole) {
        final long sequential = Arrays.stream(this.numbers)
            .filter(number -> number % 2L == 0L)
            .map(number -> number + 7L)
            .peek(blackhole::consume)
            .map(number -> number * 2L)
            .map(number -> number - 1L)
            .peek(blackhole::consume)
            .map(number -> number + 4L)
            .sum();
        final long parallel = Arrays.stream(this.numbers)
            .parallel()
            .filter(number -> number % 2L == 0L)
            .map(number -> number + 7L)
            .peek(blackhole::consume)
            .map(number -> number * 2L)
            .map(number -> number - 1L)
            .peek(blackhole::consume)
            .map(number -> number + 4L)
            .sum();
        final long scalars = Arrays.stream(this.numbers)
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
            .sum();
        final long lambdas = Arrays.stream(this.numbers)
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
            .sum();
        return this.verified(
            sequential + parallel + scalars + lambdas,
            1_353_165_329_694L
        );
    }

    @Benchmark
    public long stateful() {
        return this.verified(
            this.mixed(
                Arrays.stream(this.scrambled)
                    .skip(1_000)
                    .limit(800_000)
                    .sorted()
                    .distinct()
                    .dropWhile(number -> number < 50_000L)
                    .takeWhile(number -> number < 700_000L)
            ),
            1_188_043_783_387_903_344L
        );
    }

    @Benchmark
    public long gatherers(final Blackhole blackhole) {
        final long library = Arrays.stream(this.numbers)
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
            .sum();
        final Gatherer<Long, long[], Long> windows = Gatherer.ofSequential(
            () -> new long[2],
            Gatherer.Integrator.ofGreedy(
                (state, number, downstream) -> {
                    state[0] = state[0] + 1L;
                    state[1] = state[1] + number;
                    if (state[0] < 1_000L) {
                        return true;
                    }
                    final long window = state[1];
                    state[0] = 0L;
                    state[1] = 0L;
                    return downstream.push(window);
                }
            ),
            (state, downstream) -> {
                if (state[0] > 0L) {
                    downstream.push(state[1]);
                }
            }
        );
        final Gatherer<Long, Void, Long> growth = Gatherer.of(
            Gatherer.Integrator.of(
                (state, number, downstream) -> downstream.push(number * 2L)
            )
        );
        final Gatherer<Long, long[], Long> total = Gatherer.of(
            () -> new long[1],
            Gatherer.Integrator.ofGreedy(
                (state, number, downstream) -> {
                    state[0] = state[0] + number;
                    return true;
                }
            ),
            (left, right) -> {
                left[0] = left[0] + right[0];
                return left;
            },
            (state, downstream) -> downstream.push(state[0])
        );
        final long composed = Arrays.stream(this.numbers)
            .boxed()
            .peek(blackhole::consume)
            .gather(windows.andThen(growth))
            .mapToLong(Long::longValue)
            .sum();
        final long summed = Arrays.stream(this.numbers)
            .parallel()
            .boxed()
            .peek(blackhole::consume)
            .gather(total)
            .mapToLong(Long::longValue)
            .sum();
        final long accrued = Arrays.stream(this.numbers)
            .boxed()
            .peek(blackhole::consume)
            .collect(
                Collector.of(
                    () -> new long[1],
                    (box, number) -> box[0] = box[0] + number + 1L,
                    (left, right) -> {
                        left[0] = left[0] + right[0];
                        return left;
                    },
                    box -> box[0]
                )
            );
        final long concurrent = Arrays.stream(this.numbers)
            .parallel()
            .boxed()
            .peek(blackhole::consume)
            .collect(
                Collector.of(
                    LongAdder::new,
                    (adder, number) -> adder.add(number * 2L),
                    (left, right) -> {
                        left.add(right.sum());
                        return left;
                    },
                    LongAdder::sum,
                    Collector.Characteristics.CONCURRENT,
                    Collector.Characteristics.UNORDERED
                )
            );
        final int identity = Arrays.stream(this.numbers)
            .limit(500L)
            .boxed()
            .collect(
                Collector.<Long, List<Long>>of(
                    ArrayList::new,
                    (kept, number) -> kept.add(number + 1L),
                    (left, right) -> {
                        left.addAll(right);
                        return left;
                    }
                )
            )
            .size();
        return this.verified(
            library + composed + summed + accrued + concurrent + (long) identity,
            3_500_004_495_450L
        );
    }

    @Benchmark
    public long collectors(final Blackhole blackhole) {
        final long teed = Arrays.stream(this.scrambled)
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
            );
        final Map<Long, Long> mapped = Arrays.stream(this.numbers)
            .boxed()
            .collect(
                Collectors.groupingBy(
                    number -> number % 4L,
                    Collectors.flatMapping(
                        number -> Stream.of(number, number + 1L),
                        Collectors.summingLong(Long::longValue)
                    )
                )
            );
        final LongSummaryStatistics stats = Arrays.stream(this.numbers)
            .boxed()
            .collect(Collectors.summarizingLong(Long::longValue));
        final double average = Arrays.stream(this.numbers)
            .boxed()
            .collect(Collectors.averagingLong(number -> number * 2L));
        final long smallest = Arrays.stream(this.numbers)
            .boxed()
            .collect(Collectors.minBy(Comparator.naturalOrder()))
            .get();
        final long largest = Arrays.stream(this.numbers)
            .boxed()
            .collect(Collectors.maxBy(Comparator.naturalOrder()))
            .get();
        final Set<Long> residues = Arrays.stream(this.numbers)
            .boxed()
            .map(number -> number % 100L)
            .collect(Collectors.toUnmodifiableSet());
        final List<Long> listed = Arrays.stream(this.numbers)
            .limit(500L)
            .boxed()
            .collect(Collectors.toCollection(ArrayList::new));
        final Map<Long, Map<Long, Long>> grouped = Arrays.stream(this.numbers)
            .boxed()
            .collect(
                Collectors.groupingBy(
                    number -> number % 4L,
                    Collectors.groupingBy(
                        number -> number % 3L,
                        Collectors.summingLong(Long::longValue)
                    )
                )
            );
        final NavigableMap<Long, Long> counted = Arrays.stream(this.numbers)
            .boxed()
            .collect(
                Collectors.groupingBy(
                    number -> number % 8L,
                    TreeMap::new,
                    Collectors.counting()
                )
            );
        final Map<Long, Long> merged = Arrays.stream(this.numbers)
            .limit(10_000L)
            .boxed()
            .collect(
                Collectors.toMap(
                    number -> number % 1_000L,
                    number -> number,
                    Long::sum,
                    LinkedHashMap::new
                )
            );
        final Map<Long, Long> immutable = Arrays.stream(this.numbers)
            .limit(10_000L)
            .boxed()
            .collect(Collectors.toUnmodifiableMap(number -> number, number -> number * 2L));
        final Map<Long, Long> paired = Arrays.stream(this.scrambled)
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
        final long ranked = Arrays.stream(this.numbers)
            .limit(100_000L)
            .mapToObj(number -> new Pair(number % 50L, number))
            .peek(blackhole::consume)
            .sorted(
                Comparator.comparingLong(Pair::head)
                    .thenComparing(Comparator.comparingLong(Pair::tail).reversed())
            )
            .limit(1_000L)
            .mapToLong(Pair::tail)
            .sum();
        final String wrapped = Arrays.stream(this.scrambled)
            .limit(1_000L)
            .boxed()
            .sorted(Comparator.<Long>naturalOrder().reversed())
            .map(number -> number.toString())
            .collect(Collectors.joining(",", "[", "]"));
        final List<Long> reversed = Arrays.stream(this.scrambled)
            .limit(500L)
            .boxed()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toUnmodifiableList());
        return this.verified(
            teed
                + mapped.values().stream().mapToLong(Long::longValue).sum()
                + stats.getSum()
                + (long) average
                + smallest + largest
                + residues.stream().mapToLong(Long::longValue).sum()
                + (long) listed.size()
                + grouped.values().stream()
                    .flatMap(inner -> inner.values().stream())
                    .mapToLong(Long::longValue).sum()
                + counted.values().stream().mapToLong(Long::longValue).sum()
                + merged.values().stream().mapToLong(Long::longValue).sum()
                + immutable.values().stream().mapToLong(Long::longValue).sum()
                + paired.values().stream().mapToLong(Long::longValue).sum()
                + reduced + joined + ranked
                + (long) wrapped.length() + (long) reversed.size(),
            3_500_234_556_718L
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
        final long pooled = Arrays.stream(this.numbers)
            .parallel()
            .boxed()
            .peek(blackhole::consume)
            .reduce(0L, (total, number) -> total + number - 1L, Long::sum);
        final long gathered = Arrays.stream(this.numbers)
            .parallel()
            .collect(() -> new long[1], (box, number) -> box[0] += number + 3L, (left, right) -> left[0] += right[0])[0];
        final IntSummaryStatistics whole = Arrays.stream(this.integers)
            .filter(number -> number % 2 == 0)
            .map(number -> number + 3)
            .summaryStatistics();
        final long wholeBoxed = Arrays.stream(this.integers)
            .limit(200_000)
            .boxed()
            .peek(blackhole::consume)
            .mapToInt(Integer::intValue)
            .asLongStream()
            .map(number -> number * 2L)
            .sum();
        final double mean = Arrays.stream(this.integers)
            .map(number -> number % 1_000)
            .average()
            .getAsDouble();
        final DoubleSummaryStatistics real = Arrays.stream(this.decimals)
            .filter(number -> number % 2.0 == 0.0)
            .map(number -> number + 1.0)
            .summaryStatistics();
        final double realBoxed = Arrays.stream(this.decimals)
            .limit(200_000)
            .boxed()
            .peek(blackhole::consume)
            .mapToDouble(Double::doubleValue)
            .map(number -> number * 2.0)
            .sum();
        final double averaged = Arrays.stream(this.decimals)
            .boxed()
            .peek(blackhole::consume)
            .collect(Collectors.averagingDouble(number -> number));
        final double summed = Arrays.stream(this.decimals)
            .boxed()
            .peek(blackhole::consume)
            .collect(Collectors.summingDouble(number -> number));
        final LongSummaryStatistics counted = Arrays.stream(this.numbers)
            .filter(number -> number % 3L != 0L)
            .map(number -> number * 2L - 1L)
            .summaryStatistics();
        return this.verified(
            reduced + combined + collected + pooled + gathered
                + whole.getSum() + (long) whole.getMax() + (long) whole.getMin()
                + whole.getCount() + (long) whole.getAverage()
                + wholeBoxed + (long) mean
                + (long) real.getSum() + (long) real.getMax() + (long) real.getMin()
                + real.getCount() + (long) real.getAverage()
                + (long) realBoxed + (long) averaged + (long) summed
                + counted.getSum() + counted.getMin() + counted.getMax()
                + counted.getCount() + (long) counted.getAverage(),
            4_496_685_733_850L
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
        final double ints = Arrays.stream(this.numbers)
            .limit(200_000L)
            .mapToObj(Long::valueOf)
            .peek(blackhole::consume)
            .mapMultiToInt((number, sink) -> {
                sink.accept((int) (number % 100L));
                sink.accept((int) (number % 7L));
            })
            .average()
            .getAsDouble();
        final long longs = Arrays.stream(this.numbers)
            .limit(200_000L)
            .mapToObj(Long::valueOf)
            .peek(blackhole::consume)
            .mapMultiToLong((number, sink) -> {
                sink.accept(number + 1L);
                sink.accept(number - 1L);
            })
            .sum();
        final double doubles = Arrays.stream(this.numbers)
            .limit(200_000L)
            .mapToObj(Double::valueOf)
            .peek(blackhole::consume)
            .mapMultiToDouble((number, sink) -> {
                sink.accept(number + 0.5);
                sink.accept(number - 0.5);
            })
            .sum();
        final Long[] array = Arrays.stream(this.numbers)
            .limit(1_000L)
            .boxed()
            .toArray(Long[]::new);
        return this.verified(
            expanded + widened + (long) real
                + (long) ints + longs + (long) doubles + (long) array.length,
            300_013_801_023L
        );
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
            .forEachOrdered(number -> ordered[0] = ordered[0] * 31L + number);
        final long[] pulled = new long[1];
        final PrimitiveIterator.OfLong iterator = Arrays.stream(this.numbers)
            .filter(number -> number % 2L == 0L)
            .map(number -> number + 1L)
            .iterator();
        while (iterator.hasNext()) {
            pulled[0] = pulled[0] * 31L + iterator.nextLong();
        }
        final long[] advanced = new long[1];
        final Spliterator.OfLong cursor = Arrays.stream(this.numbers)
            .map(number -> number * 2L)
            .spliterator();
        cursor.tryAdvance((long number) -> advanced[0] = advanced[0] * 31L + number);
        cursor.forEachRemaining((long number) -> advanced[0] = advanced[0] * 31L + number);
        final long[] split = new long[1];
        final Spliterator.OfLong source = Arrays.stream(this.numbers).spliterator();
        final Spliterator.OfLong prefix = source.trySplit();
        if (prefix != null) {
            prefix.forEachRemaining((long number) -> split[0] = split[0] * 31L + number);
        }
        source.forEachRemaining((long number) -> split[0] = split[0] * 31L + number);
        final long reduced = Arrays.stream(this.numbers)
            .filter(number -> number % 7L == 0L)
            .map(number -> number + 2L)
            .reduce(Long::sum)
            .getAsLong();
        return this.verified(
            Arrays.stream(array).sum() + (long) list.size() + count + min + max + total[0] + ordered[0]
                + pulled[0] + advanced[0] + split[0] + reduced,
            -5_842_838_572_838_927_359L
        );
    }

    @Benchmark
    public long concurrent(final Blackhole blackhole) {
        final long stateful = this.mixed(
            Arrays.stream(this.numbers)
                .parallel()
                .mapToObj(number -> new Pair(number % 100_000L, number))
                .distinct()
                .sorted(Comparator.comparingLong(Pair::head))
                .mapToLong(Pair::tail)
        );
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
            4_654_815_638_209_436_400L
        );
    }

    @Benchmark
    public long overhead() {
        return this.verified(
            this.mixed(
                Arrays.stream(this.numbers)
                    .limit(8L)
                    .filter(number -> number % 2L == 1L)
                    .map(number -> number * 2L)
                    .sorted()
                    .distinct()
            ),
            65_672L
        );
    }

    @Benchmark
    public long sources(final Blackhole blackhole) {
        final LongStream.Builder digits = LongStream.builder();
        LongStream.rangeClosed(1L, 1_000L).forEach(digits::add);
        final long built = digits.build().map(number -> number + 1L).sum();
        final Stream.Builder<Long> stock = Stream.builder();
        LongStream.rangeClosed(1L, 1_000L).boxed().forEach(stock::add);
        final long boxed = stock.build()
            .peek(blackhole::consume)
            .mapToLong(Long::longValue)
            .sum();
        final long infinite = Stream.iterate(1L, number -> number + 1L)
            .limit(1_000L)
            .peek(blackhole::consume)
            .mapToLong(Long::longValue)
            .sum();
        final long nullable = LongStream.rangeClosed(1L, 1_000L)
            .boxed()
            .flatMap(number -> Stream.ofNullable(number % 2L == 0L ? number : null))
            .peek(blackhole::consume)
            .mapToLong(Long::longValue)
            .sum();
        final long empty = Stream.<Long>empty().mapToLong(Long::longValue).sum();
        final long supported = StreamSupport.longStream(
                Spliterators.spliterator(this.numbers, 0, 1_000, Spliterator.ORDERED),
                false
            )
            .map(number -> number + 1L)
            .sum();
        final long parallel = StreamSupport.stream(
                Spliterators.spliterator(this.list, Spliterator.ORDERED),
                true
            )
            .peek(blackhole::consume)
            .mapToLong(Long::longValue)
            .sum();
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
        final long ints = new Random(42L)
            .ints(500_000L)
            .asLongStream()
            .map(number -> number & 0xFFFFL)
            .sum();
        final long longs = new Random(1_234L)
            .longs(500_000L)
            .map(number -> number & 0xFFFFL)
            .sum();
        final long reals = (long) new Random(9_999L)
            .doubles(500_000L)
            .map(number -> number * 100.0)
            .sum();
        final long bounded = new Random(7L)
            .ints(500_000L, 0, 1_000)
            .asLongStream()
            .sum();
        final long sequential = this.list.stream()
            .mapToLong(Long::longValue)
            .filter(number -> number % 2L == 0L)
            .map(number -> number + 1L)
            .sum();
        final long pooled = this.list.parallelStream()
            .mapToLong(Long::longValue)
            .map(number -> number * 2L)
            .sum();
        final long hashed = this.set.stream()
            .mapToLong(Long::longValue)
            .filter(number -> number % 3L == 0L)
            .sum();
        final long distinct = this.list.parallelStream()
            .unordered()
            .map(number -> number % 200_000L)
            .distinct()
            .mapToLong(Long::longValue)
            .sum();
        final long ordered = this.list.parallelStream()
            .sequential()
            .mapToLong(Long::longValue)
            .filter(number -> number % 4L == 0L)
            .map(number -> number - 1L)
            .sum();
        return this.verified(
            built + boxed + infinite + nullable + empty + supported + parallel
                + iterated + made + concatenated
                + ints + longs + reals + bounded
                + sequential + pooled + hashed + distinct + ordered,
            2_344_733_191_776L
        );
    }

    @Benchmark
    public long text(final Blackhole blackhole) throws IOException {
        final long letters = this.prose.chars()
            .peek(blackhole::consume)
            .filter(code -> code != (int) ' ')
            .count();
        final long points = this.prose.codePoints()
            .peek(blackhole::consume)
            .mapToLong(code -> (long) (code & 0x1F))
            .sum();
        final long words = Pattern.compile(" ")
            .splitAsStream(this.prose)
            .peek(blackhole::consume)
            .filter(word -> word.length() > 3)
            .count();
        final long spans = Pattern.compile("\\w+")
            .matcher(this.prose)
            .results()
            .peek(blackhole::consume)
            .mapToLong(match -> (long) (match.end() - match.start()))
            .sum();
        final long lines;
        try (BufferedReader reader = new BufferedReader(new StringReader(this.document))) {
            lines = reader.lines()
                .peek(blackhole::consume)
                .mapToLong(Long::parseLong)
                .filter(number -> number % 2L == 0L)
                .sum();
        }
        return this.verified(letters + points + words + spans + lines, 30_485_000L);
    }

    @Benchmark
    public long ordered() {
        final long first = Arrays.stream(this.numbers)
            .parallel()
            .filter(number -> number > 500_000L)
            .map(number -> number + 1L)
            .findFirst()
            .getAsLong();
        final long limited = this.mixed(
            Arrays.stream(this.numbers)
                .parallel()
                .map(number -> number * 2L)
                .limit(100_000L)
        );
        final long skipped = this.mixed(
            Arrays.stream(this.numbers)
                .parallel()
                .skip(900_000L)
                .map(number -> number + 1L)
        );
        final long taken = this.mixed(
            Arrays.stream(this.numbers)
                .parallel()
                .takeWhile(number -> number < 300_000L)
        );
        final long dropped = this.mixed(
            Arrays.stream(this.numbers)
                .parallel()
                .dropWhile(number -> number < 999_000L)
        );
        final boolean any = Arrays.stream(this.numbers).anyMatch(number -> number > 999_990L);
        final boolean all = Arrays.stream(this.numbers).allMatch(number -> number < 2_000_000L);
        final boolean none = Arrays.stream(this.numbers).noneMatch(number -> number > 2_000_000L);
        final long found = Arrays.stream(this.numbers)
            .filter(number -> number > 999_000L)
            .map(number -> number + 7L)
            .findFirst()
            .getAsLong();
        final long only = Arrays.stream(this.numbers)
            .filter(number -> number == 777_777L)
            .map(number -> number * 2L)
            .findAny()
            .getAsLong();
        return this.verified(
            first + limited + skipped + taken + dropped
                + (any ? 1L : 0L) + (all ? 1L : 0L) + (none ? 1L : 0L) + found + only,
            -2_200_368_215_367_540_077L
        );
    }

    /**
     * A deterministically shuffled copy of the source, so the order-sensitive
     * operations under test, above all {@code sorted}, face randomly arranged
     * input rather than the ascending range that lets the adaptive sort
     * collapse into its near-linear best case. The seed is fixed so the
     * pipelines stay reproducible and their folded results remain verifiable.
     *
     * @param source The ascending source to copy and shuffle
     * @return A shuffled copy of the source
     */
    private static long[] scrambled(final long[] source) {
        final long[] copy = source.clone();
        final Random random = new Random(42L);
        for (int index = copy.length - 1; index > 0; index = index - 1) {
            final int swap = random.nextInt(index + 1);
            final long value = copy[index];
            copy[index] = copy[swap];
            copy[swap] = value;
        }
        return copy;
    }

    private long mixed(final LongStream stream) {
        final long[] mix = new long[1];
        stream.forEachOrdered(number -> mix[0] = mix[0] * 31L + number);
        return mix[0];
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
     * a boxed primitive. Equality and hashing collapse on the head alone, so
     * two pairs sharing a head but carrying different tails are equal yet
     * distinguishable; this lets a {@code distinct()} over pairs expose whether
     * the surviving element honours encounter order, which folding the tails
     * through the order-sensitive hash then certifies.
     *
     * @param head The original number
     * @param tail A number derived from the original
     * @since 0.0.1
     */
    private record Pair(long head, long tail) {
        @Override
        public boolean equals(final Object other) {
            return other instanceof Pair pair && pair.head == this.head;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.head);
        }
    }
}
