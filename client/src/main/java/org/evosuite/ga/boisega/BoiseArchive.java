package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.comparators.RankAndCrowdingDistanceComparator;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

import java.util.*;

public class BoiseArchive {
    // Coverage map tracks what test cases cover a particular goal.
    private HashMap<TestFitnessFunction, HashSet<TestChromosome>> coverageMap;

    // Keep track of what we have to cover
    private HashSet<TestFitnessFunction> remainingGoals = new HashSet<>();

    public BoiseArchive() {
        coverageMap = new HashMap<>();
    }

    // Register a solution for a goal if it's not already in the archive.
    public void registerSolutionForGoal(TestFitnessFunction goal, TestChromosome solution) {
        HashSet<TestChromosome> solutions = coverageMap.getOrDefault(goal, new HashSet<>());

        // If this solution covers other goals, then remove them from the cached results.
        solution.clearCachedResults();
        solution.clearMutationHistory();
        goal.getFitness(solution);

        // Check if the solution is already in the archive.
        if (!isSolutionAlreadyInArchive(goal, solution)) {
            solutions.add(solution);
            coverageMap.put(goal, solutions);

            updateGoalCoverage(goal);
        }
    }

    // Check if a solution is already in the archive for a given goal.
    private boolean isSolutionAlreadyInArchive(TestFitnessFunction goal, TestChromosome solution) {
        HashSet<TestChromosome> solutions = coverageMap.getOrDefault(goal, new HashSet<>());

        // A simpler way is to only check the text. This is not a proper way. But it is a quick fix.
        for (TestChromosome existingSolution : solutions) {
            if (existingSolution.getTestCase().toCode().equals(solution.getTestCase().toCode())) return true;
        }
        return false;
    }

    // Register a goal.
    public void registerGoal(TestFitnessFunction goal) {
        coverageMap.putIfAbsent(goal, new HashSet<>());
        remainingGoals.add(goal);
    }

    // Check if a specific goal is covered.
    public boolean isCovered(TestFitnessFunction ff) {
        return coverageMap.getOrDefault(ff, new HashSet<>()).size() >= Properties.MULTICOVER_TARGET;
    }

    public void updateGoalCoverage(TestFitnessFunction goal) {
        LoggingUtils.getEvoLogger().info("Have a new solution for goal {}, with total = {}", goal, coverageMap.get(goal).size());
        if (isCovered(goal)) {
            LoggingUtils.getEvoLogger().info("Goal {} is covered", goal);
            remainingGoals.remove(goal);
        } else {
            LoggingUtils.getEvoLogger().info("Goal {} is not covered, has {} solutions in archive", goal, coverageMap.get(goal).size());
        }
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
        for (TestFitnessFunction goal : coverageMap.keySet()) {
            Set<TestChromosome> solutions = coverageMap.get(goal);

            int count = 0;
            for (TestChromosome solution : solutions) {
                if (suite.getTestChromosomes().contains(solution)) {
                    LoggingUtils.getEvoLogger().warn("Skipping duplicate chromosome being added in the suite.");
                    continue;
                } else {
                    suite.addTestChromosome(solution.clone());

                    count += 1;
                    if (count >= Properties.MULTICOVER_TARGET) break;
                }

            }
        }
        return suite;
    }
}