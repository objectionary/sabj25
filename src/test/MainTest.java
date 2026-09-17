// SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
// SPDX-License-Identifier: MIT
package sabj25;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;

/**
 * Test case for {@link Main}.
 *
 * @since 0.0.1
 */
final class MainTest {

    @Test
    @Timeout(value = 20, unit = TimeUnit.MINUTES)
    void producesBenchmarkResults() throws Exception {
        final Options options = new OptionsBuilder()
            .include(Main.class.getSimpleName())
            .resultFormat(ResultFormatType.CSV)
            .result("target/jmh-result.csv")
            .shouldFailOnError(true)
            .build();
        final Collection<RunResult> results = new Runner(options).run();
        assertThat(
            "JMH must produce at least one benchmark result, but it produced none",
            results,
            not(empty())
        );
    }

    @Test
    void scalesArraysByMillions() {
        final int millions = new Random().nextInt(2, 4);
        final Blackhole hole = new Blackhole(
            "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."
        );
        assertThat(
            String.format(
                "the stateless sum over %d million numbers must exceed the sum over one million, but it doesnt",
                millions
            ),
            new Main(millions).stateless(hole),
            greaterThan(new Main(1).stateless(hole))
        );
    }

    @Test
    void scalesTextByMillions() throws Exception {
        final int millions = new Random().nextInt(2, 4);
        final Blackhole hole = new Blackhole(
            "Today's password is swordfish. I understand instantiating Blackholes directly is dangerous."
        );
        assertThat(
            String.format(
                "the text sum over %d times the prose must exceed the sum over the default prose, but it doesnt",
                millions
            ),
            new Main(millions).text(hole),
            greaterThan(new Main(1).text(hole))
        );
    }

    @Test
    void keepsOverheadAtEightElements() {
        assertThat(
            "the overhead pipeline must fold the same eight elements at any scale, but it doesnt",
            new Main(new Random().nextInt(2, 4)).overhead(),
            equalTo(new Main(1).overhead())
        );
    }
}
