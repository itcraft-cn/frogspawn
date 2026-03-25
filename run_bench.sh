#!/bin/bash

# Run JMH benchmark (multi-module version)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Build if needed
if [ ! -f "frogspawn/target/classes/cn/itcraft/frogspawn/ObjectsMemoryPool.class" ] || \
   [ ! -f "frogspawn-test/target/test-classes/cn/itcraft/frogspawn/ObjectsMemoryPoolBenchmark.class" ]; then
    echo "Building..."
    mvn clean compile test-compile -DskipTests -q
fi

# Run benchmark
cd frogspawn-test
FROGSPAWN_CP="../frogspawn/target/classes"
TEST_CP="target/test-classes"
DEPS_CP=$(mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && cat /tmp/cp.txt)

java -cp "$FROGSPAWN_CP:$TEST_CP:$DEPS_CP" \
     org.openjdk.jmh.Main ObjectsMemoryPoolBenchmark