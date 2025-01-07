package org.evosuite.ga.boisega;

import org.evosuite.ga.Chromosome;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.statements.MethodStatement;
import org.evosuite.testcase.statements.Statement;
import org.evosuite.testcase.variable.VariableReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class BoiseComparator<T extends Chromosome<T>> implements Comparator<T>,
        Serializable {

    private class VariableReferencesMap {
        @Override
        public boolean equals(Object other) {
            return false;
        }
    }

    @Override
    public int compare(T o1, T o2) {
        if (!(o1 instanceof TestChromosome && o2 instanceof TestChromosome)) {
            // Ignore.
            return 0;
        }

        // SAFETY: Safe to cast, as we checked the type.
        TestChromosome tc1 = (TestChromosome) o1;
        TestChromosome tc2 = (TestChromosome) o2;


        // Gather the input parameters for both test cases.
        List<Statement> invocations1 = new ArrayList<>();
        List<Statement> invocations2 = new ArrayList<>();

        for (int i = 0; i < tc1.size(); i++) {
            Statement s = tc1.getTestCase().getStatement(i);
            if (s instanceof MethodStatement) {
                MethodStatement ms = (MethodStatement) s;
                invocations1.add(ms);
            }
        }

        for (int i = 0; i < tc2.size(); i++) {
            Statement s = tc2.getTestCase().getStatement(i);
            if (s instanceof MethodStatement) {
                MethodStatement ms = (MethodStatement) s;
                invocations2.add(ms);
            }
        }

        // Generate a hashmap from the class.method -> List<params>
        // This will allow us to compare the two test cases.

        HashMap<String, List<List<VariableReference>>> paramsMap1 = new HashMap<>();

        // TODO: Complete this implementation.
        return 0;
    }
}

