# sabj25

Stream API benchmarks for Java 25, built on JMH.
Goal: cover the full Stream API, not just map/filter.
Repo objectionary/sabj25, license MIT.

## Layout

Non-standard Maven dirs: `src/main` and `src/test`.
`src/main/Main.java` holds the JMH benchmarks.
`src/test/MainTest.java` runs JMH from a JUnit test.
`.github/benchmark.sh` renders the results table.
`.github/workflows/benchmark.yml` is the CI pipeline.
`target/` is build output, git-ignored.

## Benchmarks

One class `Main`, twelve `@Benchmark` methods.
Most run a `long` pipeline over 1,000,000 numbers.
Each method covers one facet of the Stream API.
`stateless`: every stateless operation with scalar conversions, a primitive long chain sequential then parallel, and a megamorphic map/filter chain of many lambdas.
`stateful`: operations that must remember state.
`gatherers`: the Java 25 `Gatherers` (window, scan, fold, `mapConcurrent`), hand-built gatherers, and a custom `Collector.of`, composed and driven in parallel.
`collectors`: composed teeing/grouping/partitioning, nested collectors through tree and linked maps, the remaining collectors, plus a reference stream of `Pair` records, composed comparators, `thenComparing`, and `joining`.
`fold`: `reduce` and the mutable three-arg `collect`, sequential and parallel, plus `IntStream`, `DoubleStream`, and `LongStream` summary statistics.
`fanout`: `flatMap` and the primitive `mapMulti` variants that expand one into many.
`materialize`: `toArray`/`toList`/`count`/`min`/`max`/`forEach` plus manual iterator and spliterator.
`concurrent`: parallel stateful ops and concurrent collectors.
`overhead`: fixed pipeline cost over only eight elements.
`sources`: stream builders, `iterate`, `generate`, `concat`, spliterators, seeded pseudorandom `ints`/`longs`/`doubles`, and `List`/`Set` sources with `unordered` and `sequential` toggles in parallel.
`text`: `chars`, `codePoints`, `splitAsStream`, matches, and lines.
`ordered`: parallel order-sensitive slicing and short-circuiting match/find terminals.
Every method ends with `verified(sum, expected)`.
`verified` throws if the sum drifts from its constant.
Those constants guard against silent pipeline bugs.

## Build and run

Java 25 required (`maven.compiler.release=25`).
JMH pinned at 1.37 via an annotation processor.
Run all: `mvn clean test --errors --batch-mode`.
The test runs JMH and writes `target/jmh-result.csv`.
Test timeout is 20 minutes; JMH alone is slow.

## CI

`benchmark.yml` fires on push to `master`.
Runs touching only README.md or .github are skipped.
Matrix: Temurin, Zulu, Corretto, GraalVM, all JDK 25.
Each JVM runs on its own runner, uploads a CSV.
The `merge` job downloads all CSV artifacts.
`benchmark.sh` parses them into a markdown table.
The table lands between the README benchmark markers.
A PR titled "New benchmarking results" is opened.
Cross-column scores are indicative, not comparable.

## benchmark.sh

Pure bash; reads `artifacts/jmh-*/result.csv`.
Bench name is the text after the last dot in column 1.
Builds a header, a divider, one row per benchmark.
Missing scores render as an em dash.
`perl -0777` swaps text between the comment markers.
It also rewrites `[benchmark-gha]` to the current run.

## Conventions

Every file carries SPDX/REUSE copyright headers.
Code style lives in the global CLAUDE.md.
Classes are final and immutable.
Names are single nouns, no -er suffixes.
No inline comments; docblocks explain purpose.
Tests use JUnit 5 with Hamcrest matchers.
