package org.evosuite.ga.boisega;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;

// Although we have a generic class, BoiseGA should never be called with anything other than
// a TestChromosome.
public class BoiseGA<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
    private final HashSet<FitnessFunction<T>> remainingGoals = new HashSet<>();
    private final HashMap<String, Set<TestChromosome>> candidatesForSuite = new HashMap<>();

    public BoiseGA(ChromosomeFactory<T> factory) {
        super(factory);
    }

    @Override
    public void addFitnessFunction(FitnessFunction<T> goal) {
        this.remainingGoals.add(goal);
    }

    public List<T> initializeDistributedPopulation(int count) {
        List<T> distributedPopulation = new ArrayList<>();
        int approxPopulationCount = (int) Math.ceil(count / this.remainingGoals.size());
        while (approxPopulationCount > 0) {
            for (FitnessFunction<T> goal : remainingGoals) {
                TournamentChromosomeFactory<T> tcf = new TournamentChromosomeFactory<>(goal, this.chromosomeFactory);
                distributedPopulation.add(tcf.getChromosome());
            }
            approxPopulationCount -= 1;
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
        // Copy the population
        List<T> chromosomes = new ArrayList<>(this.population);

        // Clear the population. We will re-populate it with the best solutions.
        this.population.clear();

        List<String> remainingIDs = new ArrayList<>();
        for (FitnessFunction<T> goal : this.remainingGoals) {
            remainingIDs.add(((BoiseFitnessFunction) goal).getId());
        }

        HashMap<FitnessFunction<T>, HashSet<T>> coverageMap = new HashMap<>();
        HashSet<T> nonCoveringChromosomes = new HashSet<>();

        for (T chromosome : chromosomes) {
            boolean coversSomeGoal = false;
            for (FitnessFunction<T> goal : this.remainingGoals) {
                if (!coverageMap.containsKey(goal)) {
                    coverageMap.put(goal, new HashSet<>());
                }

                // We clone, because "last execution result" is wiped when we re-run
                // this chromosome on a different goal.
                T cloned_chromosome = chromosome.clone();

                // This runs this cloned chromosome on this goal.
                double goalFitness = goal.getFitness(cloned_chromosome);

                if (goalFitness == 0.0) {
                    // Goal is covered. Yayy!
                    // Minimization comes later :)
                    coverageMap.get(goal).add(cloned_chromosome);
                }

                // Regardless of the fitness, we need to store all execution results in
                // Execution cache, so that we do _not_ re-run the same test case again on the
                // same goal.
                // This is important for performance reasons, when we do a ranking of the
                // solutions.
                TestChromosome chr = (TestChromosome) cloned_chromosome;
                ExecutionCache.CacheKey cKey = new ExecutionCache.CacheKey(chr.getTestCase(),
                        ((BoiseFitnessFunction) goal).getId());
                ExecutionCache.insert(cKey, chr.getLastExecutionResult());
            }

            if (!coversSomeGoal) {
                nonCoveringChromosomes.add(chromosome);
            }
        }

        // Gather the candidate solutions, and rank them.
        for (FitnessFunction<T> goal : this.remainingGoals) {
            List<T> coveringChromosomes = new ArrayList<>(coverageMap.get(goal));
            List<Integer> rankedChromosomes = BoiseSelectionFunction.diversityRanking(coveringChromosomes);

            // TODO: Change to user-configurable heuristic. For now, we select 50% of the
            // ranked
            // chromosomes to send to the final archive. The remaining 50% are sent to next
            // generation.
            double cutoff = 0.5;
            for (int i = 0; i < rankedChromosomes.size(); i++) {
                TestChromosome chr = (TestChromosome) coveringChromosomes.get(rankedChromosomes.get(i));

                if (i < rankedChromosomes.size() * cutoff) {
                    // This goes to the archive directly.
                    String key = ((BoiseFitnessFunction) goal).getId();
                    if (!candidatesForSuite.containsKey(key)) {
                        candidatesForSuite.put(key, new HashSet<>());
                    }
                    candidatesForSuite.get(key).add(chr);
                } else {
                    // This goes to the next generation.
                    this.population.add(coveringChromosomes.get(rankedChromosomes.get(i)));
                }
            }
        }

        // For the rest of the non-covering chromosomes, we follow a similar suite as
        // MOSA, i.e. RankBasedPreferenceSorting.
        RankingFunction<T> rankingFunction = new RankBasedPreferenceSorting<T>();
        rankingFunction.computeRankingAssignment(chromosomes, remainingGoals);

        int currentSubFrontIndex = 0;
        while (currentSubFrontIndex < rankingFunction.getNumberOfSubfronts()) {
            List<T> currentSubFront = rankingFunction.getSubfront(currentSubFrontIndex);
            if (this.population.size() + currentSubFront.size() <= Properties.POPULATION) {
                this.population.addAll(currentSubFront);
            } else {
                break;
            }
        }

        // Is the population full? If not, we need to add some non-covering chromosomes.
        int remaining = Properties.POPULATION - this.population.size();
        if (remaining > 0) {
            this.population.addAll(initializeDistributedPopulation(remaining));
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

        for (FitnessFunction<T> goal: this.remainingGoals) {
            BoiseFitnessFunction boiseGoal = (BoiseFitnessFunction) goal;
            Set<Vector> data = ExecutionCache.getInstrumentedData(boiseGoal.getId());
            if (data.size() < Properties.MULTICOVER_TARGET) {
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

        while (this.remainingGoals.size() > 0 && this.currentIteration < 10) {
            LoggingUtils.getEvoLogger().info("Iteration {}", this.currentIteration);
            this.evolve();
        }
    }

    public TestSuiteChromosome generateTestSuite() {
        LoggingUtils.getEvoLogger().info("{} entries in cache", ExecutionCache.getNumEntries());
        return new TestSuiteChromosome();
    }
}
