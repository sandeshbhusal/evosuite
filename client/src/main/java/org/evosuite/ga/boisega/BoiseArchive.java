package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

import java.util.*;

public class BoiseArchive {
    // Coverage map tracks what test cases cover a particular goal.
    private final HashMap<TestFitnessFunction, HashSet<TestChromosome>> coverageMap;

    public BoiseArchive() {
        coverageMap = new HashMap<>();
    }

    // Register a solution for a goal if it's not already in the archive.
    public void registerSolutionForGoal(TestFitnessFunction goal, TestChromosome solution) {
        HashSet<TestChromosome> solutions = coverageMap.getOrDefault(goal, new HashSet<>());

        // Check if the solution is already in the archive.
        if (!isSolutionAlreadyInArchive(goal, solution)) {
            solutions.add(solution);
            coverageMap.put(goal, solutions);
        }
    }

    // Check if a solution is already in the archive for a given goal.
    private boolean isSolutionAlreadyInArchive(TestFitnessFunction goal, TestChromosome solution) {
        HashSet<TestChromosome> solutions = coverageMap.getOrDefault(goal, new HashSet<>());
        for (TestChromosome existingSolution : solutions) {
            if (existingSolution.equals(solution)) return true;
        }
        return false;
    }

    // Register a goal.
    public void registerGoal(TestFitnessFunction goal) {
        coverageMap.putIfAbsent(goal, new HashSet<>());
    }

    // Check if multicover is reached, i.e., each goal is covered at least MULTICOVER_TARGET times.
    public boolean isFull() {
        if (coverageMap.isEmpty()) {
            LoggingUtils.getEvoLogger().info("Coverage map is empty");
            return false;
        }

        int requiredCoverageCount = Properties.MULTICOVER_TARGET;
        int achievedCoverageCount = Integer.MAX_VALUE;

        for (HashSet<TestChromosome> coverageCount : coverageMap.values()) {
            achievedCoverageCount = Math.min(achievedCoverageCount, coverageCount.size());
        }

        LoggingUtils.getEvoLogger().info("Achieved min coverage: " + achievedCoverageCount + ", required: " + requiredCoverageCount);
        return achievedCoverageCount >= requiredCoverageCount;
    }

    // Check if a specific goal is covered.
    public boolean isCovered(TestFitnessFunction ff) {
        return coverageMap.getOrDefault(ff, new HashSet<>()).size() >= Properties.MULTICOVER_TARGET;
    }

    // Get all covered goals.
    public Set<TestFitnessFunction> getCoveredGoals() {
        HashSet<TestFitnessFunction> coveredGoals = new HashSet<>();
        for (TestFitnessFunction goal : coverageMap.keySet()) {
            if (isCovered(goal)) {
                coveredGoals.add(goal);
            }
        }
        return coveredGoals;
    }

    // Return all solutions as a test suite.
    public TestSuiteChromosome getSuite() {
        TestSuiteChromosome suite = new TestSuiteChromosome();
        for (TestFitnessFunction goal : coverageMap.keySet()) {
            for (TestChromosome solution : coverageMap.get(goal)) {
                LoggingUtils.getEvoLogger().info("Solution for goal " + goal + ": " + solution.toString());
                suite.addTestChromosome(solution.clone());
            }
        }
        return suite;
    }
}