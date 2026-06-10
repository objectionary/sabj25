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
- **One concern per pipeline.** `stateless` covers every stateless
  operation including the one-to-one scalar conversions, `stateful` the
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
| `collection` | 58.645 | 61.339 | 47.530 | 33.846 | 48.512 | 62.586 |
| `collectors` | 261.320 | 263.326 | 208.217 | 205.265 | 208.035 | 341.141 |
| `concurrent` | 192.124 | 211.101 | 157.141 | 197.214 | 160.368 | 301.954 |
| `fanout` | 84.482 | 78.208 | 61.718 | 37.357 | 67.944 | 153.254 |
| `fold` | 31.473 | 31.258 | 24.709 | 25.292 | 25.232 | 150.247 |
| `forge` | 43.283 | 44.351 | 36.906 | 38.953 | 35.005 | 150.965 |
| `gatherer` | 20.266 | 21.858 | 17.393 | 20.846 | 17.324 | 42.912 |
| `longlar` | 26.144 | 29.192 | 22.488 | 22.560 | 23.121 | 23.348 |
| `materialize` | 143.173 | 154.848 | 118.148 | 86.677 | 120.747 | 153.090 |
| `megamorphic` | 15.981 | 17.297 | 13.652 | 15.868 | 13.954 | 21.985 |
| `numerics` | 41.713 | 42.062 | 34.576 | 52.980 | 33.042 | 194.859 |
| `objects` | 85.269 | 89.492 | 69.396 | 77.912 | 68.730 | 106.998 |
| `ordered` | 85.902 | 88.728 | 72.004 | 18.050 | 69.282 | 104.497 |
| `overhead` | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 | 0.001 |
| `random` | 14.077 | 15.541 | 12.052 | 15.872 | 12.043 | 18.718 |
| `sources` | 19.305 | 19.760 | 15.733 | 22.628 | 15.612 | 69.530 |
| `stateful` | 14.533 | 13.826 | 11.050 | 14.809 | 10.576 | 26.047 |
| `stateless` | 15.095 | 17.004 | 12.732 | 12.493 | 12.779 | 27.890 |
| `text` | 9.633 | 10.209 | 7.499 | 14.978 | 7.751 | 16.925 |

All scores are in milliseconds per operation (ms/op); lower is better.
The results were calculated in [this GHA job][benchmark-gha]
on 2026-06-10 at 09:16.
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
[benchmark-gha]: https://github.com/objectionary/sabj25/actions/runs/27265661261
[jmh]: https://github.com/openjdk/jmh
[Stream API]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html
