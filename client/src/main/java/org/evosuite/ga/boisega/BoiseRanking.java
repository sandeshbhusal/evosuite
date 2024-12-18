package org.evosuite.ga.boisega;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.operators.ranking.RankingFunction;
import org.evosuite.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class BoiseRanking<T extends Chromosome<T>> implements RankingFunction<T> {
    private static BoiseArchive archiveRef;
    private static RankingFunction wrappedRankingFunction;
    HashMap<Integer, ArrayList<T>> subFronts = new HashMap<>();

    public BoiseRanking (BoiseArchive<T> archive, RankingFunction<T> rankingFunction) {
        wrappedRankingFunction = rankingFunction;
        archiveRef = archive;
    }

    @Override
    public void computeRankingAssignment(List<T> solutions, Set<? extends FitnessFunction<T>> uncovered_goals) {
        // First, rank the chromosomes normally as we would.
        wrappedRankingFunction.computeRankingAssignment(solutions, uncovered_goals);

        int subFront = 0;

        while (subFront < wrappedRankingFunction.getNumberOfSubfronts()) {
            subFronts.put(subFront, new ArrayList<T>(wrappedRankingFunction.getSubfront(subFront)));
            subFront += 1;
        }

        // Create a new SubFronts.
        HashMap<Integer, ArrayList<T>> newSubFronts = new HashMap<>();

        for (int index: subFronts.keySet()) {
            for (T item: subFronts.get(index)) {
                int puntToLevel = archiveRef.getAppearanceCount(item);
                puntToLevel += index; // We push this chromosome further back into the subfronts, so that
                                        // GA selection methods will not contain this chromosome
                                        // while generating a new population.

                if (puntToLevel > index) {
                    LoggingUtils.getEvoLogger().info("Chromosome demoted to level " + puntToLevel);
                }

                ArrayList<T> newSubFront = newSubFronts.getOrDefault(puntToLevel, new ArrayList<>());
                newSubFront.add(item);

                newSubFronts.put(index, newSubFront);
            }
        }

        subFronts = newSubFronts;
    }

    @Override
    public List<T> getSubfront(int rank) {
        return subFronts.get(rank);
    }

    @Override
    public int getNumberOfSubfronts() {
        return subFronts.size();
    }
}
