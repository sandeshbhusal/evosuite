package org.evosuite.testsuite.multicover;

import org.evosuite.Properties;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.coverage.statement.StatementCoverageFactory;
import org.evosuite.coverage.statement.StatementCoverageTestFitness;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.metaheuristics.SearchListener;
import org.evosuite.rmi.ClientServices;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * Track the coverage of the testsuite across all test goals, and check to see
 * if they have been covered at least "n" number of times.
 *
 * TODO
 * */
public class MulticoverObserver<T extends Chromosome<T>> implements SearchListener<T> {
    private static final int requiredCoverage = Properties.MULTICOVER_TARGET;
    private static final boolean isDisabled = requiredCoverage < 2;

    @Override
    public void searchStarted(GeneticAlgorithm<T> algorithm) {
        if (isDisabled) return;
    }

    @Override
    public void iteration(GeneticAlgorithm<T> algorithm) {
        if (isDisabled) return;

        // From the algorithm, get the best suite,
        // and check coverage counts.
        T chromosome = algorithm.getBestIndividual();

        if (chromosome instanceof TestSuiteChromosome) {
            TestSuiteChromosome suite = (TestSuiteChromosome) chromosome;

            // TODO: Here, we can select what kind of coverage we want.
            // Need to talk to Dr. Sherman for this.
//            List<StatementCoverageTestFitness> statementGoals = new StatementCoverageFactory().getCoverageGoals();
            List<BranchCoverageTestFitness> branchGoals = new BranchCoverageFactory().getCoverageGoals();

            ArrayList<TestFitnessFunction> goals =  new ArrayList<>();
//            ArrayList<TestFitnessFunction> goals = new ArrayList<>(statementGoals);
            goals.addAll(branchGoals);

            HashMap<TestFitnessFunction, Integer> coverage = new HashMap<>();
            int mincoverage = 100;

            // TODO: Check how they have implemented this.
            for (TestCase tc: suite.getTests()) {
                for (TestFitnessFunction ff: goals) {
                    if (ff.isCovered(tc)) {
                        int newcoverage = coverage.getOrDefault(ff, 0) + 1;
                        mincoverage = Math.min(mincoverage, newcoverage);
                        coverage.put(ff, newcoverage);
                    }
                }
            }

            ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.MinCoverageCount, mincoverage);
        } else {
            LoggingUtils.getEvoLogger().info("GA iteration does not give a TestSuiteChromosome. Change this.");
        }
    }

    @Override
    public void searchFinished(GeneticAlgorithm<T> algorithm) {
        if (isDisabled) return;
    }

    @Override
    public void fitnessEvaluation(T individual) {
        if (isDisabled) return;
    }

    @Override
    public void modification(T individual) {
        if (isDisabled) return;
    }
}
