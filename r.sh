#!/bin/bash
set -e
set -x
./gradlew :theta-xcfa-cli:build
./gradlew :theta-xcfa-cli:test --tests YamlWitnessToXcfaTest
