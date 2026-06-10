# Stream API Benchmarks for Java 25

[![benchmark](https://github.com/objectionary/sabj25/actions/workflows/benchmark.yml/badge.svg)](https://github.com/objectionary/sabj25/actions/workflows/benchmark.yml)
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
  Repetition shows up only when it is the subject itself, as in
  `megamorphic`, which repeats `map()` and `filter()` on purpose to turn
  the call sites megamorphic.
- **No easy optimization hotspots.** A `Blackhole` observation sits
  after each boxing stage, because otherwise GraalVM's partial escape
  analysis scalar-replaces the boxed `Long` and `Double` values and the
  whole pipeline collapses to almost nothing, measuring elision instead
  of work.
- **Lambdas do real arithmetic.** No lambda is an identity function;
  every element flows through genuine computation at each step.
- **Every result is verified.** Each pipeline checks its sum against a
  precomputed constant, so a run that silently skipped work fails loudly
  rather than reporting a fast but wrong number.
- **The full API, in combination.** The pipelines span all four stream
  types, `long`, `int`, `double`, and object, and weave terminal and
  intermediate methods together, including `flatMap()` and `mapMulti()`.
- **One concern per pipeline.** `scalar` isolates one-to-one
  conversions, `stateless` every stateless operation, `stateful` the
  operations that must remember state, and `megamorphic` the megamorphic
  call sites.

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
The benchmarks run on every push to `master`, once per JVM, and the
  table is regenerated automatically:

<!-- benchmark_begin -->

| Benchmark | Temurin 25 | Zulu 25 | Corretto 25 | GraalVM 25 | Oracle 25 | Semeru 25 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `collection` | 15.863 | 11.762 | 15.793 | 7.626 | 16.232 | 23.195 |
| `collectors` | 170.269 | 132.419 | 170.478 | 124.695 | 167.776 | 238.377 |
| `comparators` | 5.150 | 3.958 | 5.179 | 5.741 | 5.219 | 7.952 |
| `concurrent` | 207.712 | 169.605 | 205.420 | 208.381 | 208.284 | 297.710 |
| `craft` | 11.734 | 9.488 | 11.383 | 22.228 | 12.246 | 68.365 |
| `fanout` | 84.761 | 60.899 | 84.032 | 37.174 | 87.785 | 157.002 |
| `fold` | 31.838 | 24.051 | 33.241 | 24.281 | 32.333 | 154.170 |
| `forge` | 16.863 | 12.781 | 16.448 | 18.217 | 16.332 | 63.100 |
| `gatherer` | 22.107 | 17.318 | 22.442 | 22.678 | 22.389 | 42.646 |
| `generated` | 9.184 | 7.044 | 9.153 | 9.173 | 9.159 | 15.001 |
| `longlar` | 29.327 | 22.637 | 28.639 | 33.072 | 29.653 | 25.060 |
| `materialize` | 155.976 | 118.425 | 142.117 | 88.620 | 157.449 | 171.187 |
| `megamorphic` | 17.022 | 13.651 | 17.008 | 20.874 | 17.293 | 23.667 |
| `nested` | 56.968 | 45.560 | 57.446 | 41.440 | 56.536 | 80.636 |
| `numerics` | 44.196 | 34.400 | 44.384 | 52.466 | 44.874 | 193.574 |
| `objects` | 68.275 | 53.771 | 66.920 | 66.970 | 68.438 | 84.042 |
| `ordered` | 98.572 | 73.759 | 101.658 | 20.304 | 97.040 | 96.457 |
| `overhead` | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 | 0.001 |
| `random` | 15.555 | 12.047 | 15.540 | 15.517 | 15.524 | 19.386 |
| `scalar` | 11.055 | 8.255 | 11.087 | 12.729 | 11.030 | 22.736 |
| `sources` | 5.554 | 4.328 | 5.536 | 7.517 | 5.573 | 36.122 |
| `stateful` | 14.290 | 10.679 | 14.223 | 16.298 | 14.267 | 28.496 |
| `stateless` | 16.290 | 12.843 | 16.270 | 12.982 | 16.541 | 28.777 |
| `text` | 10.187 | 7.457 | 10.336 | 14.633 | 10.408 | 15.733 |
| `unordered` | 20.481 | 14.975 | 19.462 | 18.560 | 20.007 | 28.110 |

All scores are in milliseconds per operation (ms/op); lower is better.
The results were calculated in [this GHA job][benchmark-gha]
on 2026-06-10 at 04:52.
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
[benchmark-gha]: https://github.com/objectionary/sabj25/actions/runs/27253618638
[jmh]: https://github.com/openjdk/jmh
[Stream API]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html
