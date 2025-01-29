package org.evosuite.ga.boisega;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.evosuite.Properties;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.TournamentChromosomeFactory;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.operators.ranking.RankBasedPreferenceSorting;
import org.evosuite.ga.operators.ranking.RankingFunction;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;

// Although we have a generic class, BoiseGA should never be called with anything other than
// a TestChromosome.
public class BoiseGA<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
    private final HashSet<FitnessFunction<T>> remainingGoals = new HashSet<>();
    private final HashMap<String, Set<TestChromosome>> candidatesForSuite = new HashMap<>();
    private final HashMap<String, Set<Vector>> instrumentedVectors = new HashMap<>();

    public BoiseGA(ChromosomeFactory<T> factory) {
        super(factory);
    }

    @Override
    public void addFitnessFunction(FitnessFunction<T> goal) {
        this.remainingGoals.add(goal);
        if (!candidatesForSuite.containsKey(((BoiseFitnessFunction) goal).getId())) {
            this.candidatesForSuite.put(((BoiseFitnessFunction) goal).getId(), new HashSet<>());
        }
        if (!instrumentedVectors.containsKey(((BoiseFitnessFunction) goal).getId())) {
            this.instrumentedVectors.put(((BoiseFitnessFunction) goal).getId(), new HashSet<>());
        }
    }

    public List<T> initializeDistributedPopulation(int count) {
        List<T> distributedPopulation = new ArrayList<>();
        int numGoals = this.remainingGoals.size();

        if (numGoals == 0) {
            throw new IllegalStateException("No remaining goals to distribute population.");
        }

        int approxPopulationCount = (int) Math.ceil((double) count / numGoals);

        for (int i = 0; i < approxPopulationCount; i++) {
            for (FitnessFunction<T> goal : remainingGoals) {
                TournamentChromosomeFactory<T> tcf = new TournamentChromosomeFactory<>(goal, this.chromosomeFactory);
                distributedPopulation.add(tcf.getChromosome());
            }
        }

        return distributedPopulation;
    }

    @Override
    public void initializePopulation() {
        // Initialize the population with a distributed population.
        this.population = initializeDistributedPopulation(Properties.POPULATION);

        // Evaluate our first population.
        this.evaluatePopulation();
    }

    public void breedNextGeneration() {
        ArrayList<T> nextGeneration = new ArrayList<>();

        List<Integer> fitnessRankingForRemainingGoals = BoiseSelectionFunction.averageFitnessRanking(
                (List<?>) this.population,
                (HashSet<FitnessFunction<?>>) (HashSet<?>) this.remainingGoals);

        for (int i = 0; i < this.population.size() / 2; i++) {
            T first = this.population.get(fitnessRankingForRemainingGoals.get(i * 2)).clone();
            T second = this.population.get(fitnessRankingForRemainingGoals.get(i * 2 + 1)).clone();

            if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
                try {
                    this.crossoverFunction.crossOver(first, second);
                } catch (ConstructionFailedException e) {
                    e.printStackTrace();
                }
            }

            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                first.mutate();
                this.notifyMutation(first);
            }

            if (Randomness.nextDouble() <= Properties.MUTATION_RATE) {
                second.mutate();
                this.notifyMutation(second);
            }

            nextGeneration.add(first);
            nextGeneration.add(second);
        }

        this.population.addAll(nextGeneration);
    }

    // Evaluate the given chromosomes on the remaining goals (only).
    public void evaluatePopulation() {
        assert (!this.population.isEmpty()) : "Population is empty. Cannot evaluate fitness.";

        // Copy the population
        List<T> chromosomes = new ArrayList<>(this.population);

        // Clear the population. We will re-populate it with the best solutions.
        this.population.clear();

        // Step 1: Generate a cartesian product of Goal x Chromosomes.
        // TODO: There's probably better ways to do this, since each chromosome gets
        // evaluated multiple times. Instead, we can make this better by executing each
        // chromosome exactly once and finding out all goals covered by it.
        HashSet<Pair<FitnessFunction<T>, T>> evaluationTargets = new HashSet<>();
        for (FitnessFunction<T> goal : this.remainingGoals) {
            for (T chromosome : chromosomes) {
                evaluationTargets.add(Pair.of(goal, chromosome.clone()));
            }
        }

        // Step 2: Evaluate the chromosomes on the goals.
        HashMap<FitnessFunction<T>, HashSet<T>> coveringSolutions = new HashMap<>();
        HashSet<T> nonCoveringSolutions = new HashSet<>();

        for (Pair<FitnessFunction<T>, T> target : evaluationTargets) {
            FitnessFunction<T> goal = target.getLeft();
            T chromosome = target.getRight();

            if (!coveringSolutions.containsKey(goal)) {
                coveringSolutions.put(goal, new HashSet<>());
            }

            // Clear out the chromosome's cached results, so that we can get a fair
            // evaluation. This is safe to cast as long as we don't work with testsuites.
            ((TestChromosome) chromosome).clearCachedMutationResults();
            ((TestChromosome) chromosome).clearCachedResults();
            ((TestChromosome) chromosome).clearMutationHistory();

            double fitness = goal.getFitness(chromosome);
            if (fitness == 0.0) {
                coveringSolutions.get(goal).add(chromosome);
                // Get all vectors that were found on this goal during instrumentation.
                BoiseFitnessFunction boiseGoal = (BoiseFitnessFunction) goal;
                String goalName = boiseGoal.getId();

                // Get the vectors that were found on this goal.
                ExecutionTrace trace = ((TestChromosome) chromosome).getLastExecutionResult().getTrace();
                trace.getHitInstrumentationData(goalName).forEach(vector -> {
                    this.instrumentedVectors.get(goalName).add(new Vector(vector));
                });

            } else {
                nonCoveringSolutions.add(chromosome);
            }
        }

        // Step 3: For each covering solution, we retain a particular number of
        // "diverse" solutions
        // and send the remaining back to the population.
        // TODO: Make this a heuristic.
        double cutoff = 0.1; // Select 10% of the best solutions.
        for (FitnessFunction<T> goal : coveringSolutions.keySet()) {
            // We have to use a list here to avoid duplications while computing ranking.
            ArrayList<T> availableBestSolutions = new ArrayList<>(coveringSolutions.get(goal));
            List<Integer> mostDiverseSolutionsIndexes = BoiseSelectionFunction.diversityRanking(availableBestSolutions);

            // Pop the indexes and store them in the archive.
            int cutoffCount = (int) Math.ceil(cutoff * availableBestSolutions.size());
            List<T> solutionsToRemove = new ArrayList<>();
            for (int i = 0; i < cutoffCount; i++) {
                int index = mostDiverseSolutionsIndexes.get(i);
                T solution = availableBestSolutions.get(index);

                // Send this solution to the ExecutionCache. "Steal"ing phase.
                BoiseFitnessFunction boiseGoal = (BoiseFitnessFunction) goal;
                this.candidatesForSuite.get(boiseGoal.getId()).add((TestChromosome) solution);
            }
            // At this point, we only retain the solutions that were not "diverse" enough.
            availableBestSolutions.removeAll(solutionsToRemove);

            // Put the remaining solutions back in the coveringSolutions map.
            coveringSolutions.put(goal, new HashSet<>(availableBestSolutions));
        }

        // Step 4: For each best solution we have remaining in the
        // availbaleBestSolutions, we
        // compute another ranking assignment - RankBasedPreferenceSorting, which
        // computers the closeness
        // of the solution to the goal.
        RankingFunction<T> rankingFunction = new RankBasedPreferenceSorting<>();
        List<T> flattenedList = coveringSolutions.values().stream()
                .flatMap(HashSet::stream)
                .collect(Collectors.toList());

        rankingFunction.computeRankingAssignment(flattenedList, this.remainingGoals);

        // With this, we find the subfronts, and add chromosomes from the subfronts to
        // the population until
        // population size is met, or we run out of solutions.
        int subfrontIndex = 0;
        while (subfrontIndex < rankingFunction.getNumberOfSubfronts()) {
            List<T> subfront = rankingFunction.getSubfront(subfrontIndex);
            for (T solution : subfront) {
                if (this.population.size() < Properties.POPULATION) {
                    this.population.add(solution);
                } else {
                    break;
                }
            }
            subfrontIndex += 1;
        }

        // Step 5: If the population is still underfull, we add more chromosomes
        // distributed uniformly
        // across the remaining goals.
        if (this.population.size() < Properties.POPULATION) {
            List<T> distributedPopulation = initializeDistributedPopulation(
                    Properties.POPULATION - this.population.size());
            this.population.addAll(distributedPopulation);
        }
    }

    @Override
    protected void evolve() {
        // Extend current population along with children.
        this.breedNextGeneration();

        // Evaluate the new population.
        this.evaluatePopulation();

        // Update the coverage map.
        this.updateCoverageMap();
    }

    private void updateCoverageMap() {
        // For each chromosome in the candidate solution set,
        // we check the execution trace, and see if it was already seen. If so,
        // we remove that solution from the candidate set. If not,
        // we add the seen vector to the vectorCache.
        HashSet<FitnessFunction<T>> nextGoals = new HashSet<>();

        for (FitnessFunction<T> goal : this.remainingGoals) {
            BoiseFitnessFunction boiseGoal = (BoiseFitnessFunction) goal;
            String goalName = boiseGoal.getId();
            int count = this.instrumentedVectors.get(goalName).size();

            LoggingUtils.getEvoLogger().info("Goal {} has {} entries", goal, count);
            if (count < Properties.MULTICOVER_TARGET) {
                nextGoals.add(goal);
            }
        }

        this.remainingGoals.clear();
        this.remainingGoals.addAll(nextGoals);
    }

    @Override
    public void generateSolution() {
        if (this.population.isEmpty()) {
            this.initializePopulation();
        }

        while (this.remainingGoals.size() > 0 && this.currentIteration < 30) {
            LoggingUtils.getEvoLogger().info("Iteration {}", this.currentIteration);
            LoggingUtils.getEvoLogger().info("Population size: {}", this.population.size());
            this.evolve();

            this.currentIteration += 1;
        }
    }

    public TestSuiteChromosome generateTestSuite() {
        return new TestSuiteChromosome();
    }
}
