#!/bin/bash
set -e
set -x
./gradlew build -x shadowJar -x test -x distTar -x distZip
# ./gradlew :theta-xcfa-cli:build -x test
./gradlew :theta-xcfa-cli:cleanTest :theta-xcfa-cli:test \
  --tests "hu.bme.mit.theta.xcfa.cli.ReusePartialResultsTest.*" \
  "$@"
