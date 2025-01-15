package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.TournamentChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.testcase.TestCaseMinimizer;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;

import java.util.*;

// Although we have a generic class, BoiseGA should never be called with anything other than
// a TestChromosome.
public class BoiseGA<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
    private final BoiseArchive archive = new BoiseArchive();

    public BoiseGA(ChromosomeFactory<T> factory) {
        super(factory);
    }

    @Override
    public void addFitnessFunction(FitnessFunction<T> ff) {
        super.addFitnessFunction(ff);
        this.archive.registerGoal((BoiseFitnessFunction) ff);
    }

    @Override
    protected void evolve() {
        // Generate the next generation.
        List<T> offspringPopulation = getNextGeneration();

        // Combine parents and offspring.
        List<T> combinedPopulation = new ArrayList<>(population);
        combinedPopulation.addAll(offspringPopulation);

        // Select the next generation.
        this.population = subFrontSelection(combinedPopulation);
    }

    // Check if population is zero; initialize if required.
    public void initializePopulation() {
        if (this.population.isEmpty()) {
            while (this.population.size() < Properties.POPULATION) {
                this.population.add(chromosomeFactory.getChromosome());
            }
        }
    }

    private List<T> getNextGeneration() {
        List<T> offspringPopulation = new ArrayList<>(population.size());

        for (int i = 0; i < (population.size() / 2); i++) {
            // Selection
            T parent1 = selectionFunction.select(population);
            T parent2 = selectionFunction.select(population);

            // Crossover
            T offspring1 = parent1.clone();
            T offspring2 = parent2.clone();

            try {
                if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                    crossoverFunction.crossOver(offspring1, offspring2);
                }
            } catch (Exception e) {
                // Ignore exceptions during crossover.
            }

            // Mutation
            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                notifyMutation(offspring1);
                offspring1.mutate();
            }

            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                offspring2.mutate();
                notifyMutation(offspring2);
            }

            // Evaluate
            for (FitnessFunction<T> ff : this.getFitnessFunctions()) {
                ff.getFitness(offspring1);
                notifyEvaluation(offspring1);
                ff.getFitness(offspring2);
                notifyEvaluation(offspring2);
            }

            offspringPopulation.add(offspring1);
            offspringPopulation.add(offspring2);
        }

        return offspringPopulation;
    }

    private List<T> subFrontSelection(List<T> combinedPopulation) {
        ArrayList<T> nextGeneration = new ArrayList<>();

        int coveringChromsomesCount = 0;

        // I really need to think about this:
        // For now, skip all chromosomes that do not cover anything. Then,
        // we add chromosomes that _might_ cover something.
        for (T solution: combinedPopulation) {
            boolean coversSomeGoal = false;

            for (FitnessFunction goal: archive.getRemainingGoals()) {
                double fitness = solution.getFitness(goal);
                if (fitness == 0.0) {
                    // goal is covered by solution
                    coversSomeGoal = true;
                    archive.registerGoal((BoiseFitnessFunction) goal);

                    Properties.MINIMIZE = true;
                    TestCaseMinimizer minimizer = new TestCaseMinimizer((TestFitnessFunction) goal);
                    minimizer.minimize((TestChromosome) solution);
                    Properties.MINIMIZE = false;

                    archive.registerSolutionForGoal((BoiseFitnessFunction) goal, (TestChromosome) solution);
                    // one solution may cover multiple goals, but minimization should make it more specific.
                    // Let one chromosome only cover one goal.
                    coveringChromsomesCount += 1;
                    break;
                }
            }
        }

        LoggingUtils.getEvoLogger().info("Covering chromosomes count: {}", coveringChromsomesCount);

        // Add some random chromosomes to fill up the population.
        // But we do a tournament selection with remaining goals, otherwise, our population
        // will be too random to do anything.
        // TODO: For now, we will only add one chromosome per goal.
        for (FitnessFunction<T> goal: archive.getRemainingGoals()) {
            LoggingUtils.getEvoLogger().info("Added one chromosome for goal {} in the population", goal);
            TournamentChromosomeFactory<T> tournamentFactory = new TournamentChromosomeFactory<>(goal, this.chromosomeFactory);
            // Get the best chromosome from the tournament.
            T bestChromosome = tournamentFactory.getChromosome();
            nextGeneration.add(bestChromosome);
        }

        if (nextGeneration.size() < Properties.POPULATION) {
            int required = Properties.POPULATION - nextGeneration.size();
            LoggingUtils.getEvoLogger().info("Filling up population with {} random chromosomes because population size is {}",
                    required, nextGeneration.size());
            while (required > 0) {
                nextGeneration.add(chromosomeFactory.getChromosome());
                required--;
            }
        }

        // Truncate the generation if it exceeds population size.
        if (nextGeneration.size() > Properties.POPULATION) {
            nextGeneration = new ArrayList<>(nextGeneration.subList(0, Properties.POPULATION));
        }

        return nextGeneration;
    }

    @Override
    public void generateSolution() {
        initializePopulation();
        LoggingUtils.getEvoLogger().info("Total goals count {}", this.fitnessFunctions.size());
        while (!archive.getRemainingGoals().isEmpty()) {
            LoggingUtils.getEvoLogger().info("-------------------------------------------------------------------");
            LoggingUtils.getEvoLogger().info("Iteration: {}", currentIteration);
            LoggingUtils.getEvoLogger().info("Population size: {}", population.size());
            LoggingUtils.getEvoLogger().info("Remaining goals count: {}", archive.getRemainingGoals().size());
            this.evolve();
            this.currentIteration++;
            LoggingUtils.getEvoLogger().info("Covered goals: {}", archive.getCoveredGoals().size());
            this.notifyIteration();
            LoggingUtils.getEvoLogger().info("-------------------------------------------------------------------");
        }

        LoggingUtils.getEvoLogger().info("* GA finished in {} iterations", currentIteration);
    }

    public TestSuiteChromosome generateTestSuite() {
        return archive.getSuite();
    }
}
