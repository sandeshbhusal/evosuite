package org.evosuite.ga.boisega;

import java.util.*;

import org.evosuite.ga.Chromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionTrace;

public class BoiseDiversitySelectionFunction<T extends Chromosome<T>> {
    private List<Integer> rankedIndexes = new ArrayList<>();

    BoiseDiversitySelectionFunction(List<T> population) {
        HashMap<Integer, Double> diversityMap = new HashMap<>();

        List<Integer> indexMap = new ArrayList<>();
        for (int i =0; i< population.size(); i++) {
            indexMap.add(i);
            T solution = population.get(i);

            double sumDiversity = 0;
            int totalVectorCount = 0;

            // Cast to chromosome
            TestChromosome chromosome = (TestChromosome) solution;
            ExecutionTrace trace = chromosome.getLastExecutionResult().getTrace();
            Set<String> hitGoals = trace.getHitInstrumentationPoints();
            for (String goal: hitGoals) {
                for (List<Integer> v: trace.getHitInstrumentationData(goal)) {
                    Vector vec = new Vector(v);
                    sumDiversity += vec.internalDiversity();
                    totalVectorCount++;
                }
            }

            double diversity = sumDiversity / totalVectorCount;
            diversityMap.put(i, diversity);
        }

        // Sort the map by diversity (double).
        // according to the hashmap's values.
        Collections.sort(indexMap, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Double.compare(diversityMap.get(o1), diversityMap.get(o2));
            }
        });

        this.rankedIndexes = indexMap;
    }

    public int getNextIndex() {
        return rankedIndexes.remove(0);
    }
}
