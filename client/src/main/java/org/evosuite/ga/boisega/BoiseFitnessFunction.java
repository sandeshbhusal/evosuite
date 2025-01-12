package org.evosuite.ga.boisega;

import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

public class BoiseFitnessFunction extends TestFitnessFunction {
    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        return 0;
    }

    @Override
    public int compareTo(TestFitnessFunction other) {
        return 0;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        return false;
    }

    @Override
    public String getTargetClass() {
        return "";
    }

    @Override
    public String getTargetMethod() {
        return "";
    }
}
