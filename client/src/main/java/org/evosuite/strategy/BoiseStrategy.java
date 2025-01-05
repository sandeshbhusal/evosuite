package org.evosuite.strategy;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.coverage.branch.OnlyBranchCoverageFactory;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.boisega.BoiseArchive;
import org.evosuite.ga.boisega.BoiseGA;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.ranking.RankBasedPreferenceSorting;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.factories.RandomLengthTestFactory;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.factories.TestSuiteChromosomeFactory;
import org.evosuite.utils.LoggingUtils;

import java.util.List;

public class BoiseStrategy extends TestGenerationStrategy {
    @Override
    public TestSuiteChromosome generateTests() {
        // The BoiseGA will maintain its own archive.
        Properties.TEST_ARCHIVE = false;
        // BoiseGA does its own minimization.
        Properties.MINIMIZE = false;
        // BoiseGA does not need assertions.
        Properties.ASSERTIONS = false;

        if (!canGenerateTestsForSUT()) {
            LoggingUtils.getEvoLogger().error("* Client cannot generate tests for SUT");
            return new TestSuiteChromosome();
        }

        int goalsCount = 0;

        BoiseGA<TestChromosome> geneticAlgorithm = new BoiseGA<>(new RandomLengthTestFactory());
        for (TestFitnessFactory fitnessFactory : getFitnessFactories()) {
            for (TestFitnessFunction fitnessFunction: (List<TestFitnessFunction>) fitnessFactory.getCoverageGoals()) {
                geneticAlgorithm.addFitnessFunction(fitnessFunction);
                goalsCount += Properties.MULTICOVER_TARGET;
            }
        }

        LoggingUtils.getEvoLogger().error("* Starting to generate tests for the SUT with " + goalsCount + " goals.");

        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals, goalsCount * Properties.MULTICOVER_TARGET);
        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.MinCoverageCount, 0);

        // Start the genetic algorithm
        geneticAlgorithm.generateSolution();

        return geneticAlgorithm.generateTestSuite();
    }
}