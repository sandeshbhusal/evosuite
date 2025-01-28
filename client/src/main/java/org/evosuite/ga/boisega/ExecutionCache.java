package org.evosuite.ga.boisega;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;

/* Stores all execution result */
public class ExecutionCache {
    private static final HashMap<CacheKey, ExecutionResult> cache = new HashMap<>();
    private static final HashMap<String, Set<Vector>> instrumentedDataCache = new HashMap<>();
    private static final HashMap<String, Set<TestChromosome>> coveringTestCache = new HashMap<>();

    public static class CacheKey {
        String testcaseCode;
        String instrumentationID;

        public CacheKey(TestCase code, String instrumentationId) {
            this.testcaseCode = code.toCode();
            this.instrumentationID = instrumentationId;
        }
    }

    public static void insert(CacheKey key, ExecutionResult result) {
        cache.put(key, result.clone());
        ExecutionTrace trace = result.getTrace();
        for (String hitInstrumentationPoint: trace.getHitInstrumentationPoints()) {
            if (!instrumentedDataCache.containsKey(hitInstrumentationPoint)) {
                instrumentedDataCache.put(hitInstrumentationPoint, trace.getHitInstrumentationData(hitInstrumentationPoint));
            }
            instrumentedDataCache.get(hitInstrumentationPoint).addAll(trace.getHitInstrumentationData(hitInstrumentationPoint));
        }
    }

    public static Set<Vector> getInstrumentedData(String instrumentationID) {
        return instrumentedDataCache.getOrDefault(instrumentationID, new HashSet<>());
    }

    public static int getNumEntries() {
        return cache.keySet().size();
    }
}
