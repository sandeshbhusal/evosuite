package org.evosuite.strategy;

import org.evosuite.Properties;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.TestSuiteFitnessFunction;

import java.util.ArrayList;
import java.util.List;

public class MulticoverStrategy extends TestGenerationStrategy{
    @Override
    public TestSuiteChromosome generateTests() {
        PropertiesSuiteGAFactory algorithmFactory = new PropertiesSuiteGAFactory();
        GeneticAlgorithm<TestSuiteChromosome> algorithm = algorithmFactory.getSearchAlgorithm();

        List<TestSuiteFitnessFunction> fitnessFunctions = getFitnessFunctions();
        List<TestFitnessFunction> goals = new ArrayList<>();

        TestFitnessFunction ff;

        int required_coverage = Properties.MULTICOVER_TARGET;

        throw new IllegalArgumentException("GenerateTests not implemented for Multicover strategy");
    }
}
