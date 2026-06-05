// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package com.objectionary;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

/**
 * Test case for {@link Main}.
 *
 * @since 0.0.1
 */
final class MainTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void producesBenchmarkResults() throws Exception {
        final Options options = new OptionsBuilder()
            .include(Main.class.getSimpleName())
            .resultFormat(ResultFormatType.CSV)
            .result("target/jmh-result.csv")
            .build();
        final Collection<RunResult> results = new Runner(options).run();
        assertThat(
            "JMH must produce at least one benchmark result, but it produced none",
            results,
            not(empty())
        );
    }
}
