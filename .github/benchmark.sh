#!/usr/bin/env bash
# SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
# SPDX-License-Identifier: MIT

set -e -o pipefail

csv=target/jmh-result.csv
if [ ! -s "${csv}" ]; then
    echo "${csv} is missing or empty, run 'mvn test' first" >&2
    exit 1
fi
table=$(
    echo '| Benchmark | Mode | Threads | Samples | Score | Error | Unit |'
    echo '| --- | :---: | ---: | ---: | ---: | ---: | :---: |'
    tail -n +2 "${csv}" | tr -d '"' | while IFS=',' \
        read -r name mode threads samples score error unit; do
        printf '| %s | %s | %s | %s | %.3f | ± %.3f | %s |\n' \
            "${name}" "${mode}" "${threads}" "${samples}" \
            "${score}" "${error}" "${unit}"
    done
)
sum=$(
    printf '%s\n\n' "${table}"
    echo 'The results were calculated in [this GHA job][benchmark-gha]'
    echo "on $(date +'%Y-%m-%d') at $(date +'%H:%M'),"
    echo "on $(uname) with $(getconf _NPROCESSORS_ONLN) CPUs."
)
export sum
perl -i -0777 -pe 's/(?<=<!-- benchmark_begin -->).*'\
'(?=<!-- benchmark_end -->)/\n\n$ENV{sum}\n/gs;' README.md
url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
export url
perl -i -0777 -pe 's/(?<=\[benchmark-gha\]: )[^\n]+'\
'(?=\n)/$ENV{url}/gs;' README.md
