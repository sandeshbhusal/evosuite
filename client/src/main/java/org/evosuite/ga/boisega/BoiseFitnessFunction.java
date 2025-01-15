package org.evosuite.ga.boisega;

import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;

import java.util.Set;

public class BoiseFitnessFunction extends TestFitnessFunction {
    private static final long serialVersionUID = 5222436175279169399L;

    public String id;
    public BytecodeInstruction node;

    public BoiseFitnessFunction(BytecodeInstruction node, String instrumentationId) {
        this.node = node;
        this.id = instrumentationId;
    }

    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        ExecutionTrace trace = result.getTrace();
        boolean wasReached = trace.getCoveredLines().contains(node.getLineNumber());

        if (wasReached) {
            return 0.0;
        } else {
            double mostFitness = Double.MIN_VALUE;
            Set<ControlDependency> deps = node.getControlDependencies();

            for (ControlDependency dep : deps) {
                Branch branch = dep.getBranch();

                BranchCoverageGoal false_goal = new BranchCoverageGoal(branch, false, node.getClassName(), node.getMethodName());
                BranchCoverageGoal true_goal = new BranchCoverageGoal(branch, true, node.getClassName(), node.getMethodName());

                BranchCoverageTestFitness true_ff = new BranchCoverageTestFitness(true_goal);
                BranchCoverageTestFitness false_ff = new BranchCoverageTestFitness(false_goal);

                double true_fitness = true_ff.getFitness(individual, result);
                double false_fitness = false_ff.getFitness(individual, result);

                double branch_fitness = Math.min(true_fitness, false_fitness);
                mostFitness = Math.min(branch_fitness, mostFitness);
            }

            return mostFitness;
        }
    }

    public int compareTo(TestFitnessFunction other) {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BoiseFitnessFunction)) return false;
        BoiseFitnessFunction otherBoise = (BoiseFitnessFunction) other;

        return this.id.equals(otherBoise.id) && this.node.equals(otherBoise.node);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + node.getClassName().hashCode();
        result = prime * result + node.getMethodName().hashCode();
        result = prime * result + node.getLineNumber();
        return result;
    }

    @Override
    public String getTargetClass() {
        return node.getClassName();
    }

    @Override
    public String getTargetMethod() {
        return node.getMethodName();
    }
}
