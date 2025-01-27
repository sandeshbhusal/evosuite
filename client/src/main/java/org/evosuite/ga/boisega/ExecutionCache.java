package org.evosuite.ga.boisega;

import java.util.HashMap;

import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.execution.ExecutionResult;

/* Stores all execution result */
public class ExecutionCache {
    private static final HashMap<CacheKey, ExecutionResult> cache = new HashMap<>();

    public static class CacheKey {
        String testcaseCode;
        String instrumentationID;

        public CacheKey(TestCase code, String instrumentationId) {
            this.testcaseCode = code.toCode();
            this.instrumentationID = instrumentationId;
        }
    }

    public static void insert(CacheKey key, ExecutionResult result) {
        cache.put(key, result);
    }

    public static int getNumEntries() {
        return cache.keySet().size();
    }
}
