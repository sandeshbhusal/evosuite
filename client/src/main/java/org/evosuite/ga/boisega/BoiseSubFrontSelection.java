package org.evosuite.ga.boisega;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

import java.util.List;

public class BoiseSubFrontSelection {
    List<TestChromosome> solutions;
    BoiseFitnessFunction goal;

    public BoiseSubFrontSelection(BoiseFitnessFunction goal, List<TestChromosome> solutions) {
        this.solutions = solutions;
        this.goal = goal;
    }

    public TestChromosome getBestChromosome() {
        // Capture the chromosome with the best internal diversity
        double bestDiversity = Double.MIN_VALUE;
        TestChromosome bestSolution = null;

        for (TestChromosome solution: solutions) {
            double diversity = getInternalDiversity(solution);
            if (diversity > bestDiversity) {
                bestDiversity = diversity;
                bestSolution = solution;
            }
        }

        return bestSolution;
    }

    public BoiseArchive.Vector getCentroid() {
        String instrumentationId = goal.getId();

        return null;
    }

    // Grab the "internal diversity" of chromosome, i.e.
    // for the goal's id, and the vector, we grab the average
    // distance between vector values within a vector.
    public double getInternalDiversity(TestChromosome solution) {
        double internalDiversity = 0.0;
        ExecutionResult result = solution.getLastExecutionResult();
        List<List<Integer>> vectors = result.getTrace().getHitInstrumentationData(goal.getId());
        for (List<Integer> vector: vectors) {
            double sum = 0.0;

            for (int i = 0; i < vector.size(); i++) {
                for (int j = i + 1; j < vector.size(); j++) {
                    sum += Math.abs(vector.get(i) - vector.get(j));
                }
            }

            internalDiversity += sum / ((double) (vector.size() * (vector.size() - 1)) / 2);
        }
        return internalDiversity;
    }
}
