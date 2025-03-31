#!/bin/bash
set -e
set -x
./gradlew :theta-xcfa-cli:build -x test
./gradlew :theta-xcfa-cli:test --tests YamlWitnessToXcfaTest $1
