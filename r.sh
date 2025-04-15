#!/bin/bash
set -e
set -x
./gradlew :theta-xcfa-cli:build -x test
./gradlew :theta-xcfa-cli:cleanTest :theta-xcfa-cli:test \
  --tests "hu.bme.mit.theta.xcfa.cli.YamlWitnessToXcfaTest.testWitnessConversion*" \
  $1
