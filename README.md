# Stream API Benchmarks for Java 25

This repository contains a set of benchmarks for Stream API in Java 25.

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

## Results

The benchmarks run on every push to `master`, and the table below
  is regenerated automatically by the CI pipeline:

<!-- benchmark_begin -->

| Benchmark | Mode | Threads | Samples | Score | Error | Unit |
| --- | :---: | ---: | ---: | ---: | ---: | :---: |
| com.objectionary.Main.asDoubleStream | avgt | 1 | 5 | 4.462 | ± 0.040 | ms/op |
| com.objectionary.Main.boxed | avgt | 1 | 5 | 2.145 | ± 0.022 | ms/op |
| com.objectionary.Main.distinct | avgt | 1 | 5 | 58.048 | ± 25.257 | ms/op |
| com.objectionary.Main.dropWhile | avgt | 1 | 5 | 0.319 | ± 0.003 | ms/op |
| com.objectionary.Main.filter | avgt | 1 | 5 | 0.518 | ± 0.013 | ms/op |
| com.objectionary.Main.flatMap | avgt | 1 | 5 | 7.217 | ± 0.071 | ms/op |
| com.objectionary.Main.limit | avgt | 1 | 5 | 0.189 | ± 0.001 | ms/op |
| com.objectionary.Main.map | avgt | 1 | 5 | 0.101 | ± 0.000 | ms/op |
| com.objectionary.Main.mapMulti | avgt | 1 | 5 | 0.314 | ± 0.000 | ms/op |
| com.objectionary.Main.mapToDouble | avgt | 1 | 5 | 4.462 | ± 0.054 | ms/op |
| com.objectionary.Main.mapToInt | avgt | 1 | 5 | 0.133 | ± 0.022 | ms/op |
| com.objectionary.Main.mapToObj | avgt | 1 | 5 | 2.147 | ± 0.024 | ms/op |
| com.objectionary.Main.parallel | avgt | 1 | 5 | 0.129 | ± 0.003 | ms/op |
| com.objectionary.Main.peek | avgt | 1 | 5 | 0.315 | ± 0.001 | ms/op |
| com.objectionary.Main.skip | avgt | 1 | 5 | 0.658 | ± 0.016 | ms/op |
| com.objectionary.Main.sorted | avgt | 1 | 5 | 1.635 | ± 0.042 | ms/op |
| com.objectionary.Main.takeWhile | avgt | 1 | 5 | 0.190 | ± 0.004 | ms/op |

The results were calculated in [this GHA job][benchmark-gha]
on 2026-06-05 at 14:00,
on Linux with 4 CPUs.
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
[benchmark-gha]: https://github.com/objectionary/sabj-25/actions/runs/27019209233
