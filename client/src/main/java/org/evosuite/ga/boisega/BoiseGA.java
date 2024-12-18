package org.evosuite.ga.boisega;

import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.ranking.CrowdingDistance;
import org.evosuite.ga.operators.selection.SelectionFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.ValueMinimizer;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;

import java.util.*;

public class BoiseGA<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
    private final ChromosomeFactory<T> chromosomeFactory;
    private final BoiseArchive<T> archive;
    private int iteration = 0;

    /**
     * Constructor
     *
     * @param factory a {@link ChromosomeFactory} object.
     */
    public BoiseGA(ChromosomeFactory<T> factory, BoiseArchive ar) {
        super(factory);
        chromosomeFactory = factory;
        archive = ar;
    }

    @Override
    protected void evolve() {
        // Do nothing.
        SelectionFunction selectionFunction = getSelectionFunction();
        ArrayList<T> newPopulation = new ArrayList<>();

        for (int i = 0; i < this.population.size(); ++i) {
            // we follow a straight-forward way like NSGAII does. Not sure if this
            // is going to be enough (or efficient), but this can be swapped out for other parts.

            T parent1 = this.selectionFunction.select(this.population);
            T parent2 = this.selectionFunction.select(this.population);

            T offspring1 = parent1.clone();
            T offspring2 = parent2.clone();

            // CROSSOVER
            try {
                if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                    crossoverFunction.crossOver(offspring1, offspring2);
                }
            } catch (Exception e) {
                // ?? WAT?
            }

            // MUTATION
            // We only selectively mutate offsprings, not both at the same time.
            // WHY is NSGAII doing simultaneous mutations ? :shrug: TODO: Check paper.
            try {
                if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                    offspring1.mutate();
                    this.notifyMutation(offspring1);
                }

                if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                    offspring2.mutate();
                    this.notifyMutation(offspring2);
                }
            } catch (Exception e) {

            }
        }

        // Add everything.
        newPopulation.addAll(this.population);

        this.rankingFunction.computeRankingAssignment(newPopulation, new LinkedHashSet<>(this.getFitnessFunctions()));
        int remain = population.size();
        int index = 0;
        population.clear();

        ArrayList<T> nextPopulation = new ArrayList();

//        LoggingUtils.getEvoLogger().info(this.getRankingFunction().getSubfront(0).toString());
        List<T> front = this.getRankingFunction().getSubfront(index);

        // Getting a NullPointerException in this part.
        while (front != null && (remain > 0) && (remain >= front.size())) {
            nextPopulation.addAll(front);
            remain = remain - front.size();
            index += 1;

            front = this.rankingFunction.getSubfront(index);
        }

        if (remain > 0) {
            LoggingUtils.getEvoLogger().warn("Population is not full; the number of fronts was found to be " + this.getRankingFunction().getNumberOfSubfronts());
        }

        // TODO: What if the population is still smaller than we would've liked it to be ??

        // Get the first subFront, and add all solutions to our archive.
        List<T> bestSubFront = this.rankingFunction.getSubfront(0);
        for (T solution : bestSubFront) {
            archive.registerSolution(solution);
        }

        this.population.addAll(nextPopulation);

        notifyIteration();
    }

    @Override
    public void initializePopulation() {
        LoggingUtils.getEvoLogger().info("Started to initialize new population within BoiseGA.");
        this.generateInitialPopulation(Properties.POPULATION);
        this.notifySearchStarted();
        this.notifyIteration();
    }

    @Override
    public void generateSolution() {
        if (this.population.isEmpty()) {
            this.initializePopulation();
        }

        // TODO: Get search budget, etc.
        while (!this.archive.isFull()) {
            this.evolve();
            LoggingUtils.getEvoLogger().info("Iteration: " + iteration);
            iteration += 1;
        }

        // Gather the fitness criteria.
        this.notifySearchFinished();
    }

    // TODO: It seems like getBestIndividual always returns a TestSuiteChromosome,
    // or is invoked in such contexts.


    // It looks like even if we send the full suite with multiple-covering things, they get
    // killed during minimization.
    @Override
    public T getBestIndividual() {
        // We disable minimization directly.
//        Properties.MINIMIZE = false;

        // TODO: not sure how I was supposed to do this instead of this.
        // Since getBestIndividual is only called on Testsuite, mayyyyyybe safe to cast this?
        return (T) archive.generateTestSuite();
    }
}