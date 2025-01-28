package org.evosuite.ga.boisega;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;

/* Stores all execution result */
public class ExecutionCache {
    private static final HashMap<String, Set<Vector>> instrumentedDataCache = new HashMap<>();

    public static void insert(String instrumentationPoint, TestChromosome tc) {
        ExecutionResult result = tc.getLastExecutionResult();
        ExecutionTrace trace = result.getTrace();

        if (!trace.getHitInstrumentationPoints().contains(instrumentationPoint)) return;

        Set<List<Integer>> data = trace.getHitInstrumentationData(instrumentationPoint);

        if (data.isEmpty()) {
            throw new IllegalArgumentException("No data for instrumentation point: " + instrumentationPoint + " but it was hit?");
        }

        for (List<Integer> d : data) {
            Vector v = new Vector(d);
            instrumentedDataCache.computeIfAbsent(instrumentationPoint, k -> new HashSet<>()).add(v);
        }
    }

    public static int count(String ip) {
        return instrumentedDataCache.getOrDefault(ip, new HashSet<>()).size();
    }

    public static Set<Vector> getInstrumentedData(String instrumentationID) {
        return instrumentedDataCache.getOrDefault(instrumentationID, new HashSet<>());
    }
}
