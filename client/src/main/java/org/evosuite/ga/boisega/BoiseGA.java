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
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;

// Although we have a generic class, BoiseGA should never be called with anything other than
// a TestChromosome.
public class BoiseGA<T extends Chromosome<T>> extends GeneticAlgorithm<T> {
    private final HashSet<FitnessFunction<T>> remainingGoals = new HashSet<>();
    private final HashMap<String, FitnessFunction<T>> idToFitness = new HashMap<>();

    public BoiseGA(ChromosomeFactory<T> factory) {
        super(factory);
    }

    @Override
    public void addFitnessFunction(FitnessFunction<T> goal) {
        this.remainingGoals.add(goal);
        this.idToFitness.put(((BoiseFitnessFunction) goal).getId(), goal);
    }

    @Override
    public void initializePopulation() {
        int approxPopulationCount = (int) Math.ceil(Properties.POPULATION / this.remainingGoals.size());
        if (this.population.isEmpty()) {
            while (approxPopulationCount > 0) {
                for (FitnessFunction<T> goal : remainingGoals) {
                    TournamentChromosomeFactory<T> tcf = new TournamentChromosomeFactory<>(goal,
                            this.chromosomeFactory);
                    this.population.add(tcf.getChromosome());
                }
                approxPopulationCount -= 1;
            }
        }

        // Evaluate our first population.
        this.evaluateChromosomes(this.population);
    }

    public List<T> breedNextGeneration() {
        ArrayList<T> nextGeneration = new ArrayList<>();

        List<Integer> quickRanking = BoiseSelectionFunction.diversityRanking(this.population);
        for (int i = 0; i < this.population.size() / 2; i++) {
            T first = this.population.get(quickRanking.get(i * 2)).clone();
            T second = this.population.get(quickRanking.get(i * 2 + 1)).clone();

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

        // Evaluate them immediately.
        // This will run all the chromosomes, and their results are populated
        // in our cache, so that we can analyze the diversity and other metrics down
        // the line.
        evaluateChromosomes(nextGeneration);

        return nextGeneration;
    }

    // Evaluate the given chromosomes on the remaining goals (only).
    public void evaluateChromosomes(List<T> chromosomes) {
        List<String> remainingIDs = new ArrayList<>();
        for (FitnessFunction<T> goal : this.remainingGoals) {
            remainingIDs.add(((BoiseFitnessFunction) goal).getId());
        }

        for (T chromosome : chromosomes) {
            // WARN: Here we _know_ the types, so casting is safe.
            // This will NOT work with other GAs without some modification.
            TestChromosome chr = (TestChromosome) chromosome;

            // Indiscriminately clear out the chromosome's cached results - we don't need
            // it anymore.
            chr.clearCachedMutationResults();
            chr.clearCachedResults();
            chr.clearMutationHistory();
            
            TestCase testCase = chr.getTestCase();
            ExecutionResult result = TestCaseExecutor.runTest(testCase);
            ExecutionTrace trace = result.getTrace().lazyClone();

            // Do an intersection between what was hit and what was covered.
            Set<String> hitInstrumentationIDs = trace.getHitInstrumentationPoints();
            hitInstrumentationIDs.retainAll(remainingIDs);

            for (String pointOfInterest: hitInstrumentationIDs) {
                ExecutionCache.CacheKey kCacheKey = new ExecutionCache.CacheKey(testCase, pointOfInterest);
                ExecutionCache.insert(kCacheKey, result);
            }
        }
    }

    @Override
    protected void evolve() {
        // Extend current population along with children.
        List<T> nextGeneration = this.breedNextGeneration();
        this.population.addAll(nextGeneration);
    }

    @Override
    public void generateSolution() {
        if (this.population.isEmpty()) {
            this.initializePopulation();
        }

        this.evolve();
    }

    public TestSuiteChromosome generateTestSuite() {
        LoggingUtils.getEvoLogger().info("{} entries in cache", ExecutionCache.getNumEntries());
        return new TestSuiteChromosome();
    }
}
