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

One class `Main`, twenty-eight `@Benchmark` methods.
Most run a `long` pipeline over 1,000,000 numbers.
`scalar`: only one-to-one scalar conversions.
`stateless`: one of every stateless operation.
`stateful`: operations that must remember state.
`megamorphic`: many lambdas, megamorphic call sites.
`fold`: `reduce` and the mutable three-arg `collect`.
`combine`: those same three-arg combiners run in parallel.
`generated`: `iterate`, `generate`, and `concat` sources.
`objects`: a reference stream of `Pair` records.
`fanout`: `flatMap` that expands one element into many.
`materialize`: `toArray`, `toList`, `count`, `min`, `max`, `forEach`.
`concurrent`: parallel stateful ops and concurrent collectors.
`collection`: a `List` and a `Set` source instead of an array.
`unordered`: `unordered` and `sequential` toggles in parallel.
`harvest`: the remaining collectors (`flatMapping`, `minBy`, etc).
`spread`: the primitive `mapMultiToInt`/`Long`/`Double` variants.
`overhead`: fixed pipeline cost over only eight elements.
`forge`: hand-built gatherers, composed and driven in parallel.
`craft`: a custom `Collector.of` with explicit characteristics.
`sources`: stream builders, infinite `iterate`, and spliterators.
`text`: `chars`, `codePoints`, `splitAsStream`, matches, and lines.
`traverse`: manual `iterator`, `spliterator`, and one-arg `reduce`.
`random`: seeded pseudorandom `ints`, `longs`, and `doubles`.
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
