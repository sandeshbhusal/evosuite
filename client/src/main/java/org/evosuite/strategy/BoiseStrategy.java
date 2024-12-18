package org.evosuite.strategy;

import org.evosuite.Properties;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.ga.boisega.BoiseArchive;
import org.evosuite.ga.boisega.BoiseGA;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.boisega.BoiseRanking;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.ranking.RankBasedPreferenceSorting;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.testsuite.factories.TestSuiteChromosomeFactory;
import org.evosuite.utils.LoggingUtils;

import java.util.List;

public class BoiseStrategy extends TestGenerationStrategy {
    @Override
    public TestSuiteChromosome generateTests() {
        // A strategy must do the following things (from what I've gathered from other things):
        // Initialize an algorithm
        // Ask the algorithm to generate tests.
        // Return "best" individual from the algorithm.
        // ???
        // Profit.

        // The BoiseGA will maintain it's own archive.
        Properties.TEST_ARCHIVE = false;
        BoiseArchive ar = new BoiseArchive();
        if (!canGenerateTestsForSUT()) {
            LoggingUtils.getEvoLogger().error("* Client cannot generate tests for SUT");
            return new TestSuiteChromosome();
        }

        TestSuiteChromosomeFactory chromosomeFactory = new TestSuiteChromosomeFactory();
        GeneticAlgorithm<TestSuiteChromosome> ouralgorithm = new BoiseGA<>(chromosomeFactory, ar);
        ouralgorithm.setRankingFunction(new BoiseRanking(ar, new RankBasedPreferenceSorting()));

        LoggingUtils.getEvoLogger().error("* Starting to generate tests for the thing.");

        List<TestFitnessFactory<?>> fitnessFactories = getFitnessFactories();
        getStoppingCondition();
        int goalsCount = 0;


        for (TestFitnessFactory<?> fitnessFactory : fitnessFactories) {
            for (FitnessFunction fitnessFunction : fitnessFactory.getCoverageGoals()) {
                // instead of using the wrapped version of fitness function, we will instead
                // compute the ranking assignment based on "repeat" of the chromosomes to demote
                // them to other fronts.
                ouralgorithm.addFitnessFunction(fitnessFunction);
                ar.registerGoal(fitnessFunction);
//                ouralgorithm.addFitnessFunction(new WrappingFitnessFunction<>(fitnessFunction, ar)); // TODO: What do about unchecked assignment?
                goalsCount += 1;
            }
        }

        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals, goalsCount * Properties.MULTICOVER_TARGET);
        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.MinCoverageCount, 0);
        ouralgorithm.generateSolution();

        TestSuiteChromosome suite = ouralgorithm.getBestIndividual();
        return suite;
    }
}

//
//class WrappingFitnessFunction<T extends Chromosome<T>> extends FitnessFunction<T> {
//    private final FitnessFunction<T> wrappedFitnessFunction;
//    BoiseArchive<T> archive;
//
//    public WrappingFitnessFunction(FitnessFunction<T> wrappedFunction, BoiseArchive<T> ar) {
//        wrappedFitnessFunction = wrappedFunction;
//        archive = ar;
//    }
//
//    @Override
//    public double getFitness(T individual) {
//        double computedFitness = wrappedFitnessFunction.getFitness(individual);
//        if (this.archive.contains(individual)) {
//            return 1 + computedFitness;
//        } else {
//            return computedFitness;
//        }
//    }
//
//    @Override
//    public boolean isMaximizationFunction() {
//        return this.wrappedFitnessFunction.isMaximizationFunction();
//    }
//}