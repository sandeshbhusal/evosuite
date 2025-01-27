package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoiseArchive {
    public static class Vector {
        public int[] values;

        public Vector(int[] values) {
            this.values = values;
        }

        public Vector(List<Integer> values) {
            this.values = values.stream().mapToInt(i -> i).toArray();
        }

        public int distance(Vector other) {
            int sum = 0;
            for (int i = 0; i < values.length; i++) {
                sum += Math.pow(values[i] - other.values[i], 2);
            }

            return sum;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Vector other = (Vector) obj;
            return Arrays.equals(values, other.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }

        public String toString() {
            return Arrays.toString(values);
        }
    }

    // Holds the captured data for each goal.
    private final HashMap<String, Set<Vector>> instrumentationCache = new HashMap<>();

    // Coverage map tracks what test cases cover a particular goal.
    private final HashMap<BoiseFitnessFunction, HashSet<TestChromosome>> coverageMap = new HashMap<>();

    // Keep track of what we have to cover
    private HashSet<BoiseFitnessFunction> remainingGoals = new HashSet<>();

    public boolean registerSolutionForGoal(BoiseFitnessFunction goal, TestChromosome solution) {
        if (solution == null) {
            return false;
        }

        HashSet<TestChromosome> solutions = coverageMap.getOrDefault(goal, new HashSet<>());

        ExecutionResult result = solution.getLastExecutionResult();
        ExecutionTrace trace = result.getTrace();

        Set<List<Integer>> dataCapturedInThisTrace = trace.getHitInstrumentationData(goal.getId());

        // Remove duplicates from captured data
        Set<Vector> uniqueCapturedData = new HashSet<>();
        for (List<Integer> data : dataCapturedInThisTrace) {
            uniqueCapturedData.add(new Vector(data));
        }

        if (!isSolutionAlreadyInArchive(goal, uniqueCapturedData)) {
            solutions.add(solution);
            coverageMap.put(goal, solutions);
            Set<Vector> capturedData = instrumentationCache.getOrDefault(goal.getId(), new HashSet<>());
            capturedData.addAll(uniqueCapturedData);
            instrumentationCache.put(goal.getId(), capturedData);

            updateGoalCoverage(goal);
            return true;
        } else {
            return false;
        }
    }

    private boolean isSolutionAlreadyInArchive(
            BoiseFitnessFunction goal,
            Set<Vector> capturedData) {

        Set<Vector> availableData = instrumentationCache.getOrDefault(goal.getId(), new HashSet<>());
        availableData.retainAll(capturedData);
        return !availableData.isEmpty();
    }

    // Register a goal.
    public void registerGoal(BoiseFitnessFunction goal) {
        coverageMap.putIfAbsent(goal, new HashSet<>());
        remainingGoals.add(goal);
    }

    // Check if a specific goal is covered.
    public boolean isCovered(BoiseFitnessFunction ff) {
        return instrumentationCache.getOrDefault(ff.getId(), new HashSet<>()).size() >= Properties.MULTICOVER_TARGET;
        // return coverageMap.getOrDefault(ff, new HashSet<>()).size() >=
        // Properties.MULTICOVER_TARGET;
    }

    public void updateGoalCoverage(BoiseFitnessFunction goal) {
        if (isCovered(goal)) {
            LoggingUtils.getEvoLogger().info("Goal {} is covered", goal);
            remainingGoals.remove(goal);
        }
    }

    // Get all covered goals.
    public Set<BoiseFitnessFunction> getCoveredGoals() {
        HashSet<BoiseFitnessFunction> coveredGoals = new HashSet<>();
        for (BoiseFitnessFunction goal : coverageMap.keySet()) {
            if (isCovered(goal)) {
                coveredGoals.add(goal);
            }
        }
        return coveredGoals;
    }

    public Set<FitnessFunction> getRemainingGoals() {
        return new HashSet<>(remainingGoals);
    }

    // Return all solutions as a test suite.
    public TestSuiteChromosome getSuite() {
        int totalTests = 0;
        for (TestFitnessFunction goal : coverageMap.keySet()) {
            LoggingUtils.getEvoLogger().info("Solution count for {} is {}", goal, coverageMap.get(goal).size());
            totalTests += coverageMap.get(goal).size();
        }

        LoggingUtils.getEvoLogger().info("Have a total of {} solutions in the archive", totalTests);
        TestSuiteChromosome suite = new TestSuiteChromosome();
        int skipped = 0;
        for (TestFitnessFunction goal : coverageMap.keySet()) {
            Set<TestChromosome> solutions = coverageMap.get(goal);

            int count = 0;
            for (TestChromosome solution : solutions) {
                if (suite.getTestChromosomes().contains(solution)) {
                    skipped += 1;
                    continue;
                } else {
                    suite.addTestChromosome(solution.clone());

                    count += 1;
                    if (count >= Properties.MULTICOVER_TARGET)
                        break;
                }

            }
        }

        if (skipped > 0) {
            LoggingUtils.getEvoLogger().warn("{} chromosomes were skipped because of duplicates", skipped);
        }

        // Print out the solutions too for each goal + instrumentation point.
        LoggingUtils.getEvoLogger().info("--------------------");
        for (String goal : instrumentationCache.keySet()) {
            String filename = goal + ".csv";
            File dataFile = new File(filename);
            try {
                FileWriter writer = new FileWriter(dataFile);
                for (Vector data : instrumentationCache.get(goal)) {
                    writer.write(data.toString() + "\n");
                }
                writer.close();
            } catch (Exception e) {
                LoggingUtils.getEvoLogger().error("Could not write data to file: {}",
                        filename + "because of: " + e.getMessage());
            }

            LoggingUtils.getEvoLogger().info("Goal: {}", goal);
            for (Vector data : instrumentationCache.get(goal)) {
                LoggingUtils.getEvoLogger().info("Data: {}", data);
            }
            LoggingUtils.getEvoLogger().info("--------------------");
        }

        return suite;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Archive summary:\n");
        for (BoiseFitnessFunction goal : coverageMap.keySet()) {
            sb.append(goal).append(": datapoints = ")
                    .append(instrumentationCache.getOrDefault(goal.getId(), new HashSet<>()).size()).append(" in ");
            sb.append(coverageMap.get(goal).size()).append(" tests.\n");
        }
        sb.append("Remaining goals: ").append(remainingGoals.size()).append("\n");
        sb.append("Covered goals: ").append(coverageMap.size() - remainingGoals.size()).append("\n");
        return sb.toString();
    }
}