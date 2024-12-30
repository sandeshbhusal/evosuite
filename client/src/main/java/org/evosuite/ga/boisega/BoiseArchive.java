package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.testcase.TestCaseMinimizer;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.LoggingUtils;

import java.util.*;

public class BoiseArchive {
    // Coverage map tracks what testcases cover a particular goal.
    HashMap<TestFitnessFunction, HashSet<TestChromosome>> coverageMap;

    // One testcase can cover multiple goals. We need to track that so that we can
    // minimize testcases effectively per-goal.
    HashMap<TestChromosome, HashSet<TestFitnessFunction>> invertedCoverageMap;

    public BoiseArchive() {
        coverageMap = new HashMap<>();
        invertedCoverageMap = new HashMap<>();
    }

    // Register once a solution is found.
    public void registerSolutionForGoal(TestFitnessFunction goal, TestChromosome solution) {
        HashSet<TestChromosome> solutions = coverageMap.getOrDefault(goal, new HashSet<>());
        solutions.add(solution);
        coverageMap.put(goal, solutions);

        HashSet<TestFitnessFunction> goals = invertedCoverageMap.getOrDefault(solution, new HashSet<>());
        goals.add(goal);
        invertedCoverageMap.put(solution, goals);
    }

    // Register a goal.
    // Avoid overwriting existing solutions if called again.
    public void registerGoal(TestFitnessFunction goal) {
        coverageMap.put(goal, coverageMap.getOrDefault(goal, new HashSet<>()));
    }

    // Check if multicover is reached, i.e. each goal is covered at least
    // MULTICOVER_TARGET times.
    public boolean isFull() {
        int requiredCoverageCount = Properties.MULTICOVER_TARGET;

        int achievedCoverageCount = Integer.MAX_VALUE;
        for (HashSet<TestChromosome> coverageCount: coverageMap.values()) {
            achievedCoverageCount = Math.min(achievedCoverageCount, coverageCount.size());
        }

        return achievedCoverageCount >= requiredCoverageCount;
    }

    public List<TestChromosome> getAllTestChromosomes() {
        ArrayList<TestChromosome> allSolutions = new ArrayList<>();

        for (TestFitnessFunction goal: coverageMap.keySet()) {
            LoggingUtils.getEvoLogger().info("* For goal: " + goal + " have " + coverageMap.get(goal).size() + " solution");
            for (TestChromosome solution: coverageMap.get(goal)) {
                TestCaseMinimizer minimizer = new TestCaseMinimizer(goal);
                TestChromosome copy = solution.clone();
                minimizer.minimize(copy);
                allSolutions.add(copy);
            }
        }

        return allSolutions;
    }
}
