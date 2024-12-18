package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionObserver;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.HashSet;

public class BoiseArchive<T extends Chromosome<T>> {
    ArrayList<T> items = new ArrayList<>();
    HashSet<FitnessFunction<T>> goals = new HashSet<>();

    public boolean contains(T individual) {
        return items.contains(individual);
    }

    public void registerSolution(T solution) {
        if (!contains(solution))
            this.items.add(solution);
    }

    public boolean isFull() {
        // Check to see if all goals have been fulfilled - multiple times.
        int requiredcount = Properties.MULTICOVER_TARGET;

        int minCount = requiredcount + 1;
        for (FitnessFunction<T> goal: goals) {
            int coverageCountsForThisGoal = 0;

            for (T item: items) {
                if (item.getCoverage(goal) == 0) {
                    // Is covered.
                    coverageCountsForThisGoal += 1;
                }
            }

            minCount = Math.min(minCount, coverageCountsForThisGoal);
        }

        return minCount >= requiredcount;
    }

    public int getAppearanceCount(T item) {
        int count = 0;
        for (T containedItem: items) {
            if (containedItem == item) {
                count += 1;
            }
        }

        return count;
    }

    public void registerGoal(FitnessFunction<T> goal) {
        this.goals.add(goal);
    }

    public TestSuiteChromosome generateTestSuite() {
        TestSuiteChromosome rval = new TestSuiteChromosome();

        for (T solution: items) {
            // TODO: Find a better way to do this too! Generics, generics everywhere.

            if (solution instanceof  TestChromosome) {
                // Add directly to resulting testsuite.
                rval.addTestChromosome((TestChromosome) solution);
            } else if (solution instanceof TestSuiteChromosome) {
                rval.addTestChromosomes(((TestSuiteChromosome) solution).getTestChromosomes());
            }
        }

        LoggingUtils.getEvoLogger().info("Sending this testsuite: \n");
        LoggingUtils.getEvoLogger().info(rval.toString());
        return rval;
    }
}
