package org.evosuite.ga.boisega;

import java.util.ArrayList;
import java.util.List;

import org.evosuite.ga.Chromosome;
import org.evosuite.ga.operators.selection.SelectionFunction;

public class BoiseSelectionFunction<T extends Chromosome<T>> extends SelectionFunction<T> {

    @Override
    public int getIndex(List<T> population) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getIndex'");
    }

    public static List<Integer> averageFitnessRanking(List<?> population) {
        return null;
    }

    public static List<Integer> diversityRanking(List<?> population) {
        // For now, return a list.
        ArrayList<Integer> output = new ArrayList<>();
        for (int i = 0; i < population.size(); i++) {
            output.add(i);
        }
        return output;
    }
}
