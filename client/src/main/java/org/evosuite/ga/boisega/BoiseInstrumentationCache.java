package org.evosuite.ga.boisega;

import org.evosuite.utils.LoggingUtils;

import java.util.HashMap;

public class BoiseInstrumentationCache {
    private static BoiseInstrumentationCache instance = null;

    private class DataPoint {
        String className;
        String methodName;
        int localIndex;

        public DataPoint(String className, String methodName, int localIndex) {
            this.className = className;
            this.methodName = methodName;
            this.localIndex = localIndex;
        }
    }

    public static BoiseInstrumentationCache getInstance() {
        if (instance == null) {
            instance = new BoiseInstrumentationCache();
        }

        return instance;
    }

    public static void captureDataPoint(String instrumentationID, int instruumentationIDInt, int ... values) {
        LoggingUtils.getEvoLogger().info("{}: {}", instrumentationID, instruumentationIDInt);
    }
}
