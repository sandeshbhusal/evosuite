package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.FitnessFunction;
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
//public class BoiseGA<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
//    private BoiseArchive archive = new BoiseArchive();
//    private HashSet<FitnessFunction<T>> remainingGoals = new HashSet<>();
//
//    /**
//     * Constructor
//     *
//     * @param factory a {@link ChromosomeFactory} object.
//     */
//    public BoiseGA(ChromosomeFactory<T> factory) {
//        super(factory);
//    }
//
//    @Override
//    public void addFitnessFunction(FitnessFunction<T> ff) {
//        super.addFitnessFunction(ff);
//        this.archive.registerGoal((TestFitnessFunction) ff);
//        this.remainingGoals.add(ff);
//    }
//
//    private List<T> getNextGeneration() {
//        // This is the same as what NSGA-II does
//        List<T> offspringPopulation = new ArrayList<>(population.size());
//
//        // execute binary tournment selection, crossover, and mutation to
//        // create a offspring population Qt of size N
//        for (int i = 0; i < (population.size() / 2); i++) {
//            // Selection
//            T parent1 = selectionFunction.select(population);
//            T parent2 = selectionFunction.select(population);
//
//            // Crossover
//            T offspring1 = parent1.clone();
//            T offspring2 = parent2.clone();
//
//            try {
//                if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE)
//                    crossoverFunction.crossOver(offspring1, offspring2);
//            } catch (Exception e) {
//                // Do nothing.
//            }
//
//            // Mutation
//            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
//                notifyMutation(offspring1);
//                offspring1.mutate();
//                notifyMutation(offspring2);
//                offspring2.mutate();
//            }
//
//            // Evaluate
//            for (final FitnessFunction<T> ff : this.getFitnessFunctions()) {
//                ff.getFitness(offspring1);
//                notifyEvaluation(offspring1);
//                ff.getFitness(offspring2);
//                notifyEvaluation(offspring2);
//            }
//
//            offspringPopulation.add(offspring1);
//            offspringPopulation.add(offspring2);
//        }
//
//        return offspringPopulation;
//    }
//
//    @Override
//    protected void evolve() {
//        // Generate a union of parents and offsprings with mutation.
//        this.population = this.getNextGeneration();
//
//        this.subFrontSelection();
//        this.removeCoveredGoals();
//    }
//
//    private Set<TestFitnessFunction> getRemainingGoals() {
//        Set<TestFitnessFunction> goals = archive.coverageMap.keySet();
//        goals.removeAll(archive.getCoveredGoals());
//
//        return goals;
//    }
//
//    // subFrontSelection selects the first subfront to
//    private void subFrontSelection() {
//        ArrayList<T> nextGeneration = new ArrayList<>();
//        this.rankingFunction.computeRankingAssignment(this.population, remainingGoals);
//
//        // From the first subfront, we find the chromosomes, and send out the solutions to the archive.
//        List<T> firstSubFront = this.rankingFunction.getSubfront(0);
//        for (T solution: firstSubFront) {
//            for (FitnessFunction<T> goal: this.remainingGoals) {
//                if (solution.getCoverage(goal) == 0.0) {
//                    TestChromosome copied = ((TestChromosome)  solution).clone();
//                    TestCaseMinimizer minimizer = new TestCaseMinimizer((TestFitnessFunction) goal);
//                    Properties.MINIMIZE = true;
//                    minimizer.minimize(copied);
//                    Properties.MINIMIZE = false;
//
//                    LoggingUtils.getEvoLogger().info("Registering one solution because " + goal + " is covered by " + copied.toString());
//                    archive.registerSolutionForGoal((TestFitnessFunction) goal, (TestChromosome) solution);
//                } else {
//                    nextGeneration.add(solution);
//                }
//            }
//        }
//
//        this.population.clear();
//        this.population.addAll(nextGeneration);
//    }
//
//    // Check if population is zero; initialize if required.
//    public void initializePopulation() {
//        if (this.population.isEmpty()) {
//            while (this.population.size() < Properties.POPULATION) {
//                this.population.add(chromosomeFactory.getChromosome());
//            }
//        }
//    }
//
//    private void removeCoveredGoals() {
//        LinkedHashSet<FitnessFunction<T>> remainingGoals = new LinkedHashSet<>();
//        Set<TestFitnessFunction> coveredGoals =archive.getCoveredGoals();
//        remainingGoals.addAll(this.getFitnessFunctions());
//
//        for (TestFitnessFunction ff: coveredGoals) {
//            remainingGoals.remove(ff);
//        }
//    }
//
//    @Override
//    public void generateSolution() {
//        this.initializePopulation();
//
//        while (!this.archive.isFull() && this.currentIteration < 20) {
//            LoggingUtils.getEvoLogger().info("--------------------------------------------------------------------");
//            LoggingUtils.getEvoLogger().info("Population size: " + this.population.size());
//            this.evolve();
//            this.currentIteration += 1;
//            LoggingUtils.getEvoLogger().info("Covered goals: " + archive.getCoveredGoals().size());
//            this.notifyIteration();
//            LoggingUtils.getEvoLogger().info("--------------------------------------------------------------------");
//        }
//    }
//
//    public TestSuiteChromosome generateTestSuite() {
//        return archive.getSuite();
//    }
//}
public class BoiseGA<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
    private final BoiseArchive archive = new BoiseArchive();
    private final HashSet<FitnessFunction<T>> remainingGoals = new HashSet<>();

    public BoiseGA(ChromosomeFactory<T> factory) {
        super(factory);
    }

    @Override
    public void addFitnessFunction(FitnessFunction<T> ff) {
        super.addFitnessFunction(ff);
        this.archive.registerGoal((TestFitnessFunction) ff);
        this.remainingGoals.add(ff);
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

        // Remove goals that have been sufficiently covered.
        removeCoveredGoals();
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
                notifyMutation(offspring2);
                offspring2.mutate();
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
        this.rankingFunction.computeRankingAssignment(combinedPopulation, remainingGoals);

        // Retain solutions from multiple subfronts.
        for (int i = 0; i < this.rankingFunction.getNumberOfSubfronts(); i++) {
            List<T> subfront = this.rankingFunction.getSubfront(i);
            for (T solution : subfront) {
                for (FitnessFunction<T> goal : this.remainingGoals) {
                    if (solution.getCoverage(goal) == 0.0) {
                        TestChromosome copied = ((TestChromosome) solution).clone();
                        TestCaseMinimizer minimizer = new TestCaseMinimizer((TestFitnessFunction) goal);

                        Properties.MINIMIZE = true;
                        minimizer.minimize(copied);
                        Properties.MINIMIZE = false;

                        LoggingUtils.getEvoLogger().info("Registering solution for goal " + goal + ": " + copied.toString());
                        archive.registerSolutionForGoal((TestFitnessFunction) goal, (TestChromosome) copied);
                    } else {
                        nextGeneration.add(solution);
                    }
                }
            }
        }

        return nextGeneration;
    }

    private void removeCoveredGoals() {
        Set<TestFitnessFunction> coveredGoals = archive.getCoveredGoals();
        remainingGoals.removeIf(coveredGoals::contains);
    }

    @Override
    public void generateSolution() {
        initializePopulation();

        while (!archive.isFull() || this.currentIteration < 50) {
            LoggingUtils.getEvoLogger().info("Iteration: " + currentIteration);
            LoggingUtils.getEvoLogger().info("Population size: " + population.size());
            this.evolve();
            this.currentIteration++;
            LoggingUtils.getEvoLogger().info("Covered goals: " + archive.getCoveredGoals().size());
            this.notifyIteration();
        }
    }

    public TestSuiteChromosome generateTestSuite() {
        return archive.getSuite();
    }
}
