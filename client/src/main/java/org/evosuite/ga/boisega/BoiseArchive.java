package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.comparators.RankAndCrowdingDistanceComparator;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class BoiseArchive {
    public static class Vector {
        public double[] values;

        public Vector(double[] values) {
            this.values = values;
        }

        public double distance(Vector other) {
            double sum = 0;
            for (int i = 0; i < values.length; i++) {
                sum += Math.pow(values[i] - other.values[i], 2);
            }
            return Math.sqrt(sum);
        }

        public boolean equals(Vector other) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] != other.values[i]) {
                    return false;
                }
            }
            return true;
        }

        public String toString() {
            return Arrays.toString(values);
        }
    }

    private final HashMap<String, List<Vector>> instrumentationCache = new HashMap<>();

    // Coverage map tracks what test cases cover a particular goal.
    private HashMap<BoiseFitnessFunction, HashSet<TestChromosome>> coverageMap;

    // Keep track of what we have to cover
    private HashSet<BoiseFitnessFunction> remainingGoals = new HashSet<>();

    public BoiseArchive() {
        coverageMap = new HashMap<>();
    }

    // Register a solution for a goal if it's not already in the archive.
    public boolean registerSolutionForGoal(BoiseFitnessFunction goal, TestChromosome solution) {
        // Sometimes, if no solutions have been generated, this can return null.
        // TODO: Fix this in the future.
        if (solution == null) {
            return false;
        }

        HashSet<TestChromosome> solutions = coverageMap.getOrDefault(goal, new HashSet<>());

        ExecutionResult result = solution.getLastExecutionResult();
        ExecutionTrace trace = result.getTrace();

        List<List<Integer>> dataCapturedInThisTrace = trace.getHitInstrumentationData(goal.getId());

        // Check if the solution is already in the archive.
        if (!isSolutionAlreadyInArchive(goal, solution, dataCapturedInThisTrace)) {
            solutions.add(solution);
            coverageMap.put(goal, solutions);
            List<Vector> capturedData = instrumentationCache.getOrDefault(goal.getId(), new ArrayList<>());
            for (List<Integer> data : dataCapturedInThisTrace) {
                capturedData.add(new Vector(data.stream().mapToDouble(i -> i).toArray()));
            }
            instrumentationCache.put(goal.getId(), capturedData);

            updateGoalCoverage(goal);
            return true;
        } else {
            return false;
        }
    }

    // Check if a solution is already in the archive for a given goal.
    private boolean isSolutionAlreadyInArchive(
            BoiseFitnessFunction goal,
            TestChromosome solution,
            List<List<Integer>> capturedData) {

        // We do not care a whole lot about the tests in the archive; just the data.
        // Just compute the distance between the captured data and the data in the archive.
        List<Vector> availableData = instrumentationCache.getOrDefault(goal.getId(), new ArrayList<>());
        for (Vector data : availableData) {
            int equalCount = 0;
            for (List<Integer> captured : capturedData) {
                Vector capturedVector = new Vector(captured.stream().mapToDouble(i -> i).toArray());
                if (data.equals(capturedVector)) {
                    equalCount += 1;
                }
            }

            // ALL data points are equal.
            if (equalCount == capturedData.size()) {
                return true;
            }
        }

        return false;
    }

    // Register a goal.
    public void registerGoal(BoiseFitnessFunction goal) {
        coverageMap.putIfAbsent(goal, new HashSet<>());
        remainingGoals.add(goal);
    }

    // Check if a specific goal is covered.
    public boolean isCovered(BoiseFitnessFunction ff) {
        return instrumentationCache.getOrDefault(ff.getId(), new ArrayList<>()).size() >= Properties.MULTICOVER_TARGET;
//        return coverageMap.getOrDefault(ff, new HashSet<>()).size() >= Properties.MULTICOVER_TARGET;
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
                    if (count >= Properties.MULTICOVER_TARGET) break;
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
                LoggingUtils.getEvoLogger().error("Could not write data to file: {}", filename + "because of: " + e.getMessage());
            }

            LoggingUtils.getEvoLogger().info("Goal: {}", goal);
            for (Vector data : instrumentationCache.get(goal)) {
                LoggingUtils.getEvoLogger().info("Data: {}", data);
            }
            LoggingUtils.getEvoLogger().info("--------------------");
        }

        return suite;
    }
}