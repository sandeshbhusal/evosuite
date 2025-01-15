package org.evosuite.strategy;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.boisega.BoiseGA;
import org.evosuite.ga.boisega.BoiseTestFitnessFactory;
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

        BoiseGA<TestChromosome> geneticAlgorithm = new BoiseGA<>(new RandomLengthTestFactory());
        List<TestFitnessFunction> goals = BoiseTestFitnessFactory.getGoals();
        geneticAlgorithm.addFitnessFunctions(goals);

        int goalsCount = goals.size() * Properties.MULTICOVER_TARGET;

        LoggingUtils.getEvoLogger().error("* Starting to generate tests for the SUT with " + goalsCount + " goals.");

        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals, goalsCount);
        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.MinCoverageCount, 0);

        // Start the genetic algorithm
        geneticAlgorithm.generateSolution();

        return geneticAlgorithm.generateTestSuite();
    }
}