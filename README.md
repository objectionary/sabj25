# Stream API Benchmarks for Java 25

[![benchmark](https://github.com/objectionary/sabj25/actions/workflows/benchmark.yml/badge.svg)](https://github.com/objectionary/sabj25/actions/workflows/benchmark.yml)
[![mvn](https://github.com/objectionary/sabj25/actions/workflows/mvn.yml/badge.svg)](https://github.com/objectionary/sabj25/actions/workflows/mvn.yml)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/objectionary/sabj25/blob/master/LICENSE.txt)

This repository contains a set of benchmarks for [Stream API] in Java 25.

The motivation of this repository is the lack of benchmarks that would
  cover the entire set of terminal and non-terminal methods of
  Java Stream API, in their different combinations.
The benchmarks of [Biboudis et al.][biboudis2014] ([sources][cotl])
  and [Møller et al.][moller2020] ([sources][streamliner])
  only cover `map()` and `filter()` methods.
[Rosales et al.][rosales2023] go further with `StreamProf`, the first
  dedicated stream profiler for the JVM, yet they target the runtime
  overhead of `map()`, `filter()`, and `reduce()` rather than the
  breadth of the API.

A few open-source projects on GitHub benchmark streams as well,
  yet each of them stays narrow:
  [keaz/java-stream-benchmark][keaz] measures only `sort()` and
  `toString()` to tell when a parallel stream pays off,
  [Pask423/stream-benchmarks][pask423] studies the internals of
  parallel streams across a range of thread counts,
  [phonty29/stream-benchmarks][phonty29] compares streams against
  ordinary `for` loops,
  and [Nikolas-Charalambidis/java-16-mapmulti-benchmark][mapmulti]
  contrasts the `mapMulti()` method with `flatMap()`.
The [SoftwareMill][softwaremill] blog benchmarks a single
  log-processing pipeline of `map()`, `filter()`, and
  `collect(groupingBy())`, again touching only a handful of methods.
None of them exercise the full set of terminal and intermediate
  methods, let alone their combinations.

## Principles

The pipelines are built to measure the Stream API itself, not the
  cleverness of a particular JIT compiler. They follow a few rules:

- **No incidental repetition.** Where a pipeline sets out to cover the
  API, each method appears exactly once, so it measures the operation in
  combination with others rather than a loop of the same call.
  Repetition shows up only when it is the subject itself, as in the
  chain inside `stateless` that repeats `map()` and `filter()` on
  purpose to turn the call sites megamorphic.
- **No easy optimization hotspots.** A `Blackhole` observation sits
  after each boxing stage, because otherwise GraalVM's partial escape
  analysis scalar-replaces the boxed `Long` and `Double` values and the
  whole pipeline collapses to almost nothing, measuring elision instead
  of work.
- **Lambdas do real arithmetic.** No lambda is an identity function;
  every element flows through genuine computation at each step.
- **Every result is verified.** Each pipeline checks its result against a
  precomputed constant, so a run that silently skipped work fails loudly
  rather than reporting a fast but wrong number.
  Order-insensitive pipelines fold into a plain sum, while pipelines
  whose contract includes encounter order, such as `sorted()`,
  `limit()`, and `forEachOrdered()`, fold through a rolling `31`-based
  mix, so a result that holds the right elements in the wrong order
  fails too.
- **The workload scales.** The arrays hold one million numbers and the
  text fixtures ten thousand sentences and lines, while every count,
  slice, and threshold woven through the pipelines is a share of those
  sizes.
  The `NUMBERS` environment variable multiplies all of them, one by
  default, so `NUMBERS=10` runs every pipeline over ten million
  numbers.
  Fixed are the arguments that shape a pipeline rather than size it:
  the window widths, the small moduli that fan elements into a set
  number of buckets, the random seeds, and the handful of elements
  behind `overhead`.
  The verified constants are precomputed for the default workload, so
  `verified()` compares against them only when `NUMBERS` is 1.
- **The full API, in combination.** The pipelines span all four stream
  types, `long`, `int`, `double`, and object, and weave terminal and
  intermediate methods together, including `flatMap()` and `mapMulti()`.
- **One facet of the API per benchmark.** `stateless` covers the
  stateless operations including the one-to-one scalar conversions,
  `stateful` the operations that must remember state, `fusable` those
  same operations over boxed numbers with no barrier, `fold` the
  reduction terminals, `ordered` the short-circuiting ones, and so on
  through gatherers, collectors, sources, and parallel execution.

## Results

The numbers come from [JMH][jmh] 1.37, the Java Microbenchmark Harness,
  driven from a JUnit test that builds the `Options` and runs the `Runner`
  programmatically rather than from the command line.
Every method is measured in `AverageTime` mode and reported in
  milliseconds per operation (`ms/op`), so each score is the mean wall-clock
  time of a single invocation of one `@Benchmark` method.
Because each pipeline processes a different element count and operation mix,
  the rows measure distinct workloads: compare JVMs down a single column,
  not one benchmark against another across rows.
Each benchmark runs in two forks (each a freshly started JVM), with three
  warmup iterations of one second each to let the JIT compiler settle,
  followed by five measurement iterations of two seconds each; the reported
  score is the arithmetic mean of all ten measurement iterations across the
  two forks.
The state is thread-scoped and JMH drives each benchmark from a single
  measurement thread, though the pipelines that call `parallel()` still fan
  their work across the shared fork-join pool; a `Blackhole` consumes
  intermediate boxed values to stop the JIT from eliminating the pipeline
  as dead code.
JMH writes the raw results to `target/jmh-result.csv`, which the CI
  pipeline parses into the table below.
CI leaves `NUMBERS` at its default, so the table reflects one million
  numbers per array.
The benchmarks run on every push to `master`, once per JVM, and the
  table is regenerated automatically:

<!-- benchmark_begin -->

| Benchmark | Temurin 25 | Zulu 25 | Corretto 25 | GraalVM 25 | Oracle 25 | Semeru 25 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `collectors` | 859.041 | 836.939 | 1397.961 | 799.920 | 1148.943 | 1170.086 |
| `concurrent` | 257.273 | 138.833 | 224.380 | 166.232 | 276.845 | 287.713 |
| `fanout` | 44.211 | 44.966 | 81.668 | 27.624 | 61.157 | 146.897 |
| `fold` | 98.946 | 85.204 | 178.064 | 106.265 | 129.213 | 440.060 |
| `fusable` | 78.673 | 111.503 | 137.509 | 88.052 | 109.525 | 75.191 |
| `gatherers` | 52.880 | 46.794 | 90.744 | 53.783 | 73.779 | 193.780 |
| `materialize` | 85.193 | 78.360 | 146.778 | 69.324 | 126.225 | 205.692 |
| `ordered` | 57.265 | 48.759 | 98.304 | 24.091 | 74.576 | 91.093 |
| `overhead` | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 |
| `sources` | 109.789 | 95.269 | 170.520 | 116.640 | 144.793 | 225.768 |
| `stateful` | 17.444 | 15.679 | 77.299 | 55.509 | 22.157 | 79.899 |
| `stateless` | 47.676 | 40.981 | 86.730 | 67.290 | 61.965 | 143.298 |
| `text` | 5.770 | 5.113 | 10.443 | 12.193 | 7.300 | 17.344 |

All scores are in milliseconds per operation (ms/op); lower is better.
The results were calculated in [this GHA job][benchmark-gha]
on 2026-11-18 at 10:58.
Each JVM ran on its own GitHub-hosted Linux runner,
so the scores across columns are indicative, not strictly comparable.
<!-- benchmark_end -->

[biboudis2014]: https://arxiv.org/abs/1406.6631
[cotl]: https://github.com/biboudis/clashofthelambdas
[moller2020]: https://dl.acm.org/doi/abs/10.1145/3428236
[streamliner]: https://github.com/cs-au-dk/streamliner
[keaz]: https://github.com/keaz/java-stream-benchmark
[pask423]: https://github.com/Pask423/stream-benchmarks
[phonty29]: https://github.com/phonty29/stream-benchmarks
[mapmulti]: https://github.com/Nikolas-Charalambidis/java-16-mapmulti-benchmark
[rosales2023]: https://arxiv.org/abs/2302.10006
[softwaremill]: https://softwaremill.com/benchmarking-java-streams/
[benchmark-gha]: https://github.com/objectionary/sabj25/actions/runs/44285948191
[jmh]: https://github.com/openjdk/jmh
[Stream API]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html
