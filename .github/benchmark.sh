#!/usr/bin/env bash
# SPDX-FileCopyrightText: Copyright (c) 2026 Objectionary.com
# SPDX-License-Identifier: MIT

set -e -o pipefail

shopt -s nullglob
dirs=(artifacts/jmh-*/)
if [ "${#dirs[@]}" -eq 0 ]; then
    echo 'no benchmark artifacts under artifacts/, did the matrix jobs run' >&2
    exit 1
fi
declare -a jvms
declare -A score
declare -A seen
declare -a benches
col=0
for dir in "${dirs[@]}"; do
    csv="${dir}result.csv"
    label="${dir}name.txt"
    if [ ! -s "${csv}" ] || [ ! -s "${label}" ]; then
        echo "skipping ${dir}, result.csv or name.txt is missing" >&2
        continue
    fi
    jvms[col]=$(cat "${label}")
    while IFS=',' read -r name _mode _threads _samples value _; do
        bench="${name##*.}"
        score["${bench}:${col}"]="${value}"
        if [ -z "${seen[${bench}]:-}" ]; then
            seen[${bench}]=1
            benches+=("${bench}")
        fi
    done < <(tail -n +2 "${csv}" | tr -d '"')
    col=$((col + 1))
done
if [ "${col}" -eq 0 ]; then
    echo 'every benchmark artifact was empty, nothing to render' >&2
    exit 1
fi
header='| Benchmark |'
divider='| --- |'
for ((i = 0; i < col; i++)); do
    header+=" ${jvms[i]} |"
    divider+=' ---: |'
done
table=$(
    echo "${header}"
    echo "${divider}"
    while read -r bench; do
        row="| ${bench} |"
        for ((i = 0; i < col; i++)); do
            value="${score[${bench}:${i}]:-}"
            if [ -n "${value}" ]; then
                row+=$(printf ' %.3f |' "${value}")
            else
                row+=' — |'
            fi
        done
        echo "${row}"
    done < <(printf '%s\n' "${benches[@]}" | sort)
)
sum=$(
    printf '%s\n\n' "${table}"
    echo 'All scores are in milliseconds per operation (ms/op); lower is better.'
    echo 'The results were calculated in [this GHA job][benchmark-gha]'
    echo "on $(date +'%Y-%m-%d') at $(date +'%H:%M')."
    echo 'Each JVM ran on its own GitHub-hosted Linux runner,'
    echo 'so the scores across columns are indicative, not strictly comparable.'
)
export sum
perl -i -0777 -pe 's/(?<=<!-- benchmark_begin -->).*'\
'(?=<!-- benchmark_end -->)/\n\n$ENV{sum}\n/gs;' README.md
url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
export url
perl -i -0777 -pe 's/(?<=\[benchmark-gha\]: )[^\n]+'\
'(?=\n)/$ENV{url}/gs;' README.md
