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
- **The full API, in combination.** The pipelines span all four stream
  types, `long`, `int`, `double`, and object, and weave terminal and
  intermediate methods together, including `flatMap()` and `mapMulti()`.
- **One facet of the API per benchmark.** `stateless` covers the
  stateless operations including the one-to-one scalar conversions,
  `stateful` the operations that must remember state, `fold` the
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
The benchmarks run on every push to `master`, once per JVM, and the
  table is regenerated automatically:

<!-- benchmark_begin -->

| Benchmark | Temurin 25 | Zulu 25 | Corretto 25 | GraalVM 25 | Oracle 25 | Semeru 25 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `collectors` | 1163.873 | 1020.984 | 1299.877 | 988.181 | 1143.993 | 1162.453 |
| `concurrent` | 208.685 | 197.494 | 201.980 | 184.295 | 193.867 | 293.861 |
| `fanout` | 83.244 | 81.083 | 87.590 | 42.214 | 91.420 | 156.651 |
| `fold` | 99.087 | 92.154 | 95.466 | 83.151 | 96.319 | 347.349 |
| `gatherers` | 92.131 | 83.829 | 93.842 | 70.171 | 86.141 | 188.773 |
| `materialize` | 154.692 | 142.544 | 155.673 | 81.755 | 143.578 | 156.609 |
| `ordered` | 91.714 | 83.918 | 101.541 | 25.095 | 88.683 | 101.955 |
| `overhead` | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 | 0.000 |
| `sources` | 104.367 | 97.231 | 105.494 | 85.646 | 97.956 | 174.994 |
| `stateful` | 75.129 | 68.488 | 76.882 | 62.902 | 71.089 | 76.656 |
| `stateless` | 87.049 | 79.636 | 87.197 | 80.037 | 81.211 | 144.867 |
| `text` | 10.397 | 9.715 | 10.259 | 14.292 | 9.855 | 16.378 |

All scores are in milliseconds per operation (ms/op); lower is better.
The results were calculated in [this GHA job][benchmark-gha]
on 2026-06-11 at 22:53.
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
[benchmark-gha]: https://github.com/objectionary/sabj25/actions/runs/27382217103
[jmh]: https://github.com/openjdk/jmh
[Stream API]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/stream/package-summary.html
