#!/bin/bash

echo "Running JMH benchmark for frogspawn..."
echo "Mode: Throughput, Units: ops/ms"
echo "=================================="

# Run JMH benchmark with throughput mode
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && cat /tmp/cp.txt)" \
     org.openjdk.jmh.Main ObjectsMemoryPoolBenchmark \
     -wi 3 -i 5 -f 1 -bm thrpt -tu ms

echo "=================================="
echo "Benchmark completed."