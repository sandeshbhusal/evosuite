package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.operators.crossover.CrossOverFunction;
import org.evosuite.ga.operators.crossover.SinglePointCrossOver;
import org.evosuite.ga.operators.crossover.UniformCrossOver;
import org.evosuite.ga.operators.ranking.FastNonDominatedSorting;
import org.evosuite.ga.operators.ranking.RankingFunction;
import org.evosuite.ga.operators.selection.SelectionFunction;
import org.evosuite.ga.operators.selection.TournamentSelectionRankAndCrowdingDistanceComparator;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.factories.RandomLengthTestFactory;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoiseGA {
    private SelectionFunction<TestChromosome> selectionFunction;
    private ChromosomeFactory<TestChromosome> chromosomeFactory;
    private RankingFunction<TestChromosome> rankingFunction;
    private BoiseArchive archive;
    private ArrayList<TestFitnessFunction> goals;
    private ArrayList<TestChromosome> population;

    public BoiseGA() {
        this.selectionFunction = new TournamentSelectionRankAndCrowdingDistanceComparator<>();
        this.rankingFunction = new FastNonDominatedSorting<>();
        this.chromosomeFactory = new RandomLengthTestFactory();

        this.archive = new BoiseArchive();
        this.goals = new ArrayList<>();
        this.population = new ArrayList<>();
    }

    // Check if population is zero; initialize if required.
    public void initializePopulation() {
        if (this.population.isEmpty()) {
            while (this.population.size() < Properties.POPULATION) {
                this.population.add(chromosomeFactory.getChromosome());
            }
        }
    }

    // Breed next generation from current population.
    public void breedNextGeneration() {
        ArrayList<TestChromosome> nextGeneration = new ArrayList<>();

        // From the current population, select the top 20% of parents, do
        // crossover, and optionally, mutation, and add the resulting next Generation
        // to the current population.
        if (this.population.size() < 2) {
            throw new IllegalArgumentException("Population has eroded. Bug in GA");
        }

        int topTwentyPercentCount = (int) Math.floor(this.population.size() * 0.2);
        List<TestChromosome> parents = this.selectionFunction.select(this.population, topTwentyPercentCount);
        while (nextGeneration.size() < (Properties.POPULATION - topTwentyPercentCount)) {
            // Fill out the population with crossover/mutation.
            TestChromosome parent1 = this.population.get(Randomness.nextInt(0, parents.size() - 1));
            TestChromosome parent2 = this.population.get(Randomness.nextInt(0, parents.size() - 1));

            // By default, fixed single-point crossover function is used.
            try {
                CrossOverFunction<TestChromosome> crossoverFunction = new UniformCrossOver<>();
                crossoverFunction.crossOver(parent1, parent2);

            } catch (ConstructionFailedException e) {
                LoggingUtils.getEvoLogger().error("Could not perform crossover on parents.");
                throw new RuntimeException(e);
            }

            // Optionally, mutate with a certain probability.
            if (Randomness.nextDouble() < Properties.MUTATION_RATE) {
                parent1.mutate();
            }

            if (Randomness.nextDouble() < Properties.MUTATION_RATE) {
                parent2.mutate();
            }

            // Add both to the population.
            nextGeneration.add(parent1);
            nextGeneration.add(parent2);
        }

        this.population.clear();
        this.population.addAll(parents);
        this.population.addAll(nextGeneration);
    }

    public void evaluateCoverageGoals() {
        for (TestFitnessFunction goal : goals) {
            for (TestChromosome solution : population) {
                if (goal.isCovered(solution)) {
                    // TODO: This is a design question; not sure where to put this,
                    // but I'll put this here regardless.
                    this.archive.registerGoal(goal);
                    this.archive.registerSolutionForGoal(goal, solution);
                }
            }
        }
    }

    public void registerGoal(TestFitnessFunction goal) {
        this.archive.registerGoal(goal);
        this.goals.add(goal);
    }

    public void run() {
        int iterationCount = 0;

        this.initializePopulation();
        // TODO: Do some stuff to notify iteration, initialization, etc.
        while (!this.archive.isFull()) {
            this.breedNextGeneration();
            this.evaluateCoverageGoals();
            iterationCount += 1;
        }
    }

    // Generate a Test suite from the gathered tests.
    public TestSuiteChromosome generateTestSuite() {
        TestSuiteChromosome testsuite = new TestSuiteChromosome();
        for (TestChromosome chromosome: archive.getAllTestChromosomes()) {
            testsuite.addTestChromosome(chromosome);
        }
        return testsuite;
    }
}
