package org.evosuite.strategy;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.boisega.BoiseArchive;
import org.evosuite.ga.boisega.BoiseGA;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.ranking.RankBasedPreferenceSorting;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.factories.TestSuiteChromosomeFactory;
import org.evosuite.utils.LoggingUtils;

import java.util.List;

public class BoiseStrategy extends TestGenerationStrategy {
    @Override
    public TestSuiteChromosome generateTests() {
        // The BoiseGA will maintain its own archive.
        Properties.TEST_ARCHIVE = false;
        Properties.MINIMIZE = false;

        if (!canGenerateTestsForSUT()) {
            LoggingUtils.getEvoLogger().error("* Client cannot generate tests for SUT");
            return new TestSuiteChromosome();
        }

        int goalsCount = 0;

        BoiseGA geneticAlgorithm = new BoiseGA();
        for (TestFitnessFactory fitnessFactory : getFitnessFactories()) {
            for (Object fitnessFunction : fitnessFactory.getCoverageGoals()) {
                // SAFETY: Can cast, because fitnessFactories always generate TestFitnessFunctions.
                // TODO: Not sure _why_ it's taking coverageGoals as List<Object>. Investigate later.
                geneticAlgorithm.registerGoal((TestFitnessFunction) fitnessFunction);
                goalsCount += 1;
            }
        }

        LoggingUtils.getEvoLogger().error("* Starting to generate tests for the thing.");

        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals, goalsCount * Properties.MULTICOVER_TARGET);
        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.MinCoverageCount, 0);

        geneticAlgorithm.run();

        TestSuiteChromosome suite = geneticAlgorithm.generateTestSuite();
        return new TestSuiteChromosome();
    }
}