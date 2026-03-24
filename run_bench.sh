#!/bin/bash

# Run JMH benchmark
java -cp "target/classes:target/test-classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -q && cat /tmp/cp.txt)" \
     org.openjdk.jmh.Main ObjectsMemoryPoolBenchmark

