package org.evosuite.strategy;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.boisega.BoiseGA;
import org.evosuite.ga.metaheuristics.mosa.structural.MultiCriteriaManager;
import org.evosuite.graphs.cdg.ControlDependenceGraph;
import org.evosuite.graphs.cfg.ActualControlFlowGraph;
import org.evosuite.graphs.cfg.RawControlFlowGraph;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testcase.factories.RandomLengthTestFactory;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        ArrayList<TestFitnessFunction> fitnessFunctions = new ArrayList<>();

        BoiseGA<TestChromosome> geneticAlgorithm = new BoiseGA<>(new RandomLengthTestFactory());
        for (TestFitnessFactory fitnessFactory : getFitnessFactories()) {
            for (TestFitnessFunction fitnessFunction : (List<TestFitnessFunction>) fitnessFactory.getCoverageGoals()) {
                if (fitnessFunction.toString().contains("root-Branch")) {
                    LoggingUtils.getEvoLogger().info("* Skipping fitness function: " + fitnessFunction);
                    continue;
                }

                fitnessFunctions.add(fitnessFunction);
                goalsCount += Properties.MULTICOVER_TARGET;
            }
        }

        // We use the same MultiCriteriaManager as DynaMOSA to handle control dependencies in the code.
        MultiCriteriaManager goalsManager = new MultiCriteriaManager(fitnessFunctions);
        Set<TestFitnessFunction> uncoveredGoals = goalsManager.getUncoveredGoals();
        geneticAlgorithm.addFitnessFunctions(uncoveredGoals);

        LoggingUtils.getEvoLogger().info("* Original goals count was {} and with control dependencies removed, we have {} goals.", fitnessFunctions.size(), uncoveredGoals.size());
        LoggingUtils.getEvoLogger().error("* Starting to generate tests for the SUT with " + goalsCount + " goals.");

        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals, goalsCount);
        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.MinCoverageCount, 0);

        // Start the genetic algorithm
        geneticAlgorithm.generateSolution();

        // For our peace of mind, we will actually run the test suite at the end.
        LoggingUtils.getEvoLogger().info("* Running the test suite.");
        TestSuiteChromosome ts = geneticAlgorithm.generateTestSuite();
        for (TestChromosome tc : ts.getTestChromosomes()) {
            LoggingUtils.getEvoLogger().info("* Running test case: " + tc);
            TestCaseExecutor.getInstance().execute(tc.getTestCase());
        }

        return geneticAlgorithm.generateTestSuite();
    }
}