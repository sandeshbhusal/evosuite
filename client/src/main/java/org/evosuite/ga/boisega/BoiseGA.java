package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.TournamentChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.ranking.RankBasedPreferenceSorting;
import org.evosuite.ga.operators.ranking.RankingFunction;
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

            offspringPopulation.add(offspring1);
            offspringPopulation.add(offspring2);
        }

        return offspringPopulation;
    }

    private List<T> subFrontSelection(List<T> combinedPopulation) {
        ArrayList<T> nextGeneration = new ArrayList<>();

        // Get the remaining goals from the archive.
        Set<FitnessFunction> remainingGoals = archive.getRemainingGoals();
        HashMap<FitnessFunction, List<T>> coveringSolutions = new HashMap<>();
        ArrayList<T> nonCoveringSolutions = new ArrayList<>();

        // See what chromosomes cover the goals; rank them by their fitness scores first.
        // if there is a chromosome that covers a goal entirely, put it in coveringSolutions,
        // else, put it in nonCoveringSolutions to make it a part of the next generation.

        for (T solution: combinedPopulation) {
            boolean coversSomeGoal = false;

            for (FitnessFunction goal: remainingGoals) {
                double fitness = goal.getFitness(solution);
                assert (!goal.isMaximizationFunction()) : "Boise GA works with minimization functions only for now.";

                if (fitness == 0.0) {
                    // This covers some goal G.
                    coversSomeGoal = true;

                    // One solution may cover multiple goals. We allow it to.
                    // However, in order to make our calculations easier, we will clamp
                    // a "copy" of the solution to one goal, minimizing it so that we
                    // see less side-effects.
                    TestCaseMinimizer minimizer = new TestCaseMinimizer((TestFitnessFunction) goal);
                    T clonedSolution = solution.clone();
                    minimizer.minimize((TestChromosome) clonedSolution);

                    List<T> foundSolutions = coveringSolutions.getOrDefault(goal, new ArrayList<>());
                    foundSolutions.add(clonedSolution);
                    coveringSolutions.put(goal, foundSolutions);
                }
            }

            if (!coversSomeGoal) {
                nonCoveringSolutions.add(solution);
            }
        }

        // We only take the "best" from each goal, so that in each iteration, we have only one solution.
        // This is _very_ suboptimal, but I want to check if this produces diverse data.
        for (FitnessFunction goal: coveringSolutions.keySet()) {
            List<T> solutions = coveringSolutions.getOrDefault(goal, new ArrayList<>());
            if (solutions.isEmpty()) {
                // nothing covers this goal yet.
                continue;
            }

            BoiseSubFrontSelection subFrontSelection = new BoiseSubFrontSelection((BoiseFitnessFunction) goal, (List<TestChromosome>) solutions);
            T bestSolution = (T) subFrontSelection.getBestChromosome();

            if (bestSolution == null) {
                // No best solution found.
                continue;
            }

            // Register the best solution to the archive, move the rest to the next generation.
            boolean registered = archive.registerSolutionForGoal((BoiseFitnessFunction) goal, (TestChromosome) bestSolution);

            // Check if this is a new solution. If so, steal it, otherwise, keep it in the population.
            if (registered) {
                // Solution was not already in archive.
                solutions.remove(bestSolution);
            } else {
                // Mutate and add this solution to the next generation
                bestSolution.mutate();
                nonCoveringSolutions.add(bestSolution);
            }

            // TODO: Think about this.
            nonCoveringSolutions.addAll(solutions);
        }

        // Now that we have the remaining solutions, we rank them, according to the internal and
        // cluster diversity (see BoiseSubFrontSelection).
        if (nonCoveringSolutions.size() > Properties.POPULATION) {
            // We have a lot of chromosomes that do not cover any goal.
            // We mutate _all_ of them, and add them to the next generation,
            // after doing a subfront selection (normal; dynamosa).

            RankingFunction rankingFunction = new RankBasedPreferenceSorting();
            rankingFunction.computeRankingAssignment(nonCoveringSolutions, remainingGoals);

            int remaining = Properties.POPULATION;
            int currentSubFront = 0;
            while (remaining > 0) {
                List<T> subfront = rankingFunction.getSubfront(currentSubFront);
                for (T solution: subfront) {
                    if (remaining == 0) {
                        break;
                    }

                    nextGeneration.add(solution);
                    remaining--;
                }
            }
        } else {
            int requiredRemaining = Properties.POPULATION - nonCoveringSolutions.size();

            // Add all non-covering solutions to the next generation.
            nextGeneration.addAll(nonCoveringSolutions);

            // Next, generate some new chromosomes to fill the remaining slots.
            for (FitnessFunction goal: remainingGoals) {
                // Randomly generate a new chromosome with tournament selection and add it to the next generation.
                TournamentChromosomeFactory<T> factory = new TournamentChromosomeFactory<T>(goal, chromosomeFactory);
                T newChromosome = factory.getChromosome();

                nextGeneration.add(newChromosome);
                requiredRemaining -= 1;

                if (requiredRemaining == 0) {
                    break;
                }
            }
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
