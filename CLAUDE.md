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

One class `Main`, twenty-five `@Benchmark` methods.
Most run a `long` pipeline over 1,000,000 numbers.
`scalar`: only one-to-one scalar conversions.
`longlar`: the same chain in primitive longs, sequential then parallel.
`stateless`: one of every stateless operation.
`stateful`: operations that must remember state.
`megamorphic`: many lambdas, megamorphic call sites.
`gatherer`: the Java 25 `Gatherers` (window, scan, fold, `mapConcurrent`).
`collectors`: composed teeing/grouping/partitioning plus the remaining collectors.
`fold`: `reduce` and the mutable three-arg `collect`, sequential and parallel.
`generated`: `iterate`, `generate`, and `concat` sources.
`objects`: a reference stream of `Pair` records.
`fanout`: `flatMap` and the primitive `mapMulti` variants that expand one into many.
`materialize`: `toArray`/`toList`/`count`/`min`/`max`/`forEach` plus manual iterator and spliterator.
`concurrent`: parallel stateful ops and concurrent collectors.
`collection`: a `List` and a `Set` source instead of an array.
`unordered`: `unordered` and `sequential` toggles in parallel.
`overhead`: fixed pipeline cost over only eight elements.
`forge`: hand-built gatherers, composed and driven in parallel.
`craft`: a custom `Collector.of` with explicit characteristics.
`sources`: stream builders, infinite `iterate`, and spliterators.
`text`: `chars`, `codePoints`, `splitAsStream`, matches, and lines.
`random`: seeded pseudorandom `ints`, `longs`, and `doubles`.
`numerics`: `IntStream`, `DoubleStream`, and `LongStream` summary statistics.
`ordered`: parallel order-sensitive slicing and short-circuiting match/find terminals.
`nested`: collectors nested in collectors, through tree and linked maps.
`comparators`: composed comparators, `thenComparing`, and `joining`.
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
