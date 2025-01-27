package org.evosuite.ga.boisega;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.utils.LoggingUtils;

import java.util.List;
import java.util.Set;

public class BoiseSubFrontSelection {
    List<TestChromosome> solutions;
    BoiseFitnessFunction goal;

    public BoiseSubFrontSelection(BoiseFitnessFunction goal, List<TestChromosome> solutions) {
        this.solutions = solutions;
        this.goal = goal;
    }

    public TestChromosome getBestChromosome() {
        // Capture the chromosome with the best internal diversity
        double bestDiversity = -1.0;
        TestChromosome bestSolution = null;

        for (TestChromosome solution : solutions) {
            solution.clearCachedResults();
            solution.clearMutationHistory();
            goal.getFitness(solution);

            double diversity = getInternalDiversity(solution);
            if (diversity >= bestDiversity) {
                bestDiversity = diversity;
                bestSolution = solution;
            }
        }

        // If internal diversity is not 0, then return immediately.
        // This is to save on computation.
        if (bestDiversity != 0.0) {
            return bestSolution;
        }

        // If the bestDiversity is 0.0 (i.e. all values in the vector are the same),
        // then we look for diversity within the cluster (list of solutions).
        LoggingUtils.getEvoLogger().info(
                "All vector values are the same for {}: {}. Looking for diversity within the cluster.",
                goal.getId(), getVectorsForSolution(bestSolution));

        BoiseArchive.Vector centroid = getCentroid();
        double bestDistance = -1.0;

        for (TestChromosome solution : solutions) {
            ExecutionTrace trace = solution.getLastExecutionResult().getTrace();
            Set<List<Integer>> vectors = trace.getHitInstrumentationData(goal.getId());

            for (List<Integer> vector : vectors) {
                BoiseArchive.Vector currentVector = new BoiseArchive.Vector(
                        vector.stream().mapToInt(i -> i).toArray());
                double distance = centroid.distance(currentVector);
                if (distance > bestDistance) {
                    bestDistance = distance;
                    bestSolution = solution;
                }
            }
        }

        return bestSolution;
    }

    public BoiseArchive.Vector getCentroid() {
        if (solutions.isEmpty()) {
            return new BoiseArchive.Vector(new int[0]);
        }

        // Grab the first vector, to find the length of the resultant vector.
        // Ehh. This is a super bad way to do this, but it's 4 am :)
        List<Integer> firstVector = null;
        try {
            firstVector = solutions.get(0).getLastExecutionResult().getTrace().getHitInstrumentationData(goal.getId())
                    .iterator().next();
        } catch (Exception e) {
            return new BoiseArchive.Vector(new int[0]);
        }

        int[] centroid = new int[firstVector.size()];

        for (TestChromosome solution : solutions) {
            ExecutionResult result = solution.getLastExecutionResult();
            Set<List<Integer>> vectors = result.getTrace().getHitInstrumentationData(goal.getId());
            for (List<Integer> vector : vectors) {
                for (int i = 0; i < vector.size(); i++) {
                    centroid[i] += vector.get(i);
                }
            }
        }

        for (int i = 0; i < centroid.length; i++) {
            centroid[i] /= solutions.size();
        }

        return new BoiseArchive.Vector(centroid);
    }

    public Set<List<Integer>> getVectorsForSolution(TestChromosome solution) {
        return solution.getLastExecutionResult().getTrace().getHitInstrumentationData(goal.getId());
    }

    // Grab the "internal diversity" of chromosome, i.e.
    // for the goal's id, and the vector, we grab the average
    // distance between vector values within a vector.
    public double getInternalDiversity(TestChromosome solution) {
        double internalDiversity = 0.0;
        ExecutionResult result = solution.getLastExecutionResult();
        Set<List<Integer>> vectors = result.getTrace().getHitInstrumentationData(goal.getId());
        for (List<Integer> vector : vectors) {
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
