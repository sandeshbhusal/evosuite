package org.evosuite.ga.boisega;

import org.evosuite.coverage.branch.BranchCoverageGoal;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTrace;

import java.io.Serializable;
import java.util.Set;

public class BoiseFitnessFunction extends TestFitnessFunction {
    private static final long serialVersionUID = 631096774725721L;

    private final transient String id;
    private final transient BytecodeInstruction node;

    private final String targetClassName;
    private final String targetMethodName;
    private final int targetLineNumber;

    public BoiseFitnessFunction(BytecodeInstruction node, String instrumentationId) {
        this.node = node;
        this.id = instrumentationId;

        this.targetClassName = node.getClassName();
        this.targetMethodName = node.getMethodName();
        this.targetLineNumber = node.getLineNumber();
    }

    public String getId() {
        return this.id;
    }

    public BytecodeInstruction getNode() {
        return this.node;
    }

    @Override
    public double getFitness(TestChromosome individual, ExecutionResult result) {
        ExecutionTrace trace = result.getTrace();
        boolean wasReached = trace.getCoveredLines().contains(node.getLineNumber());

        if (wasReached) {
            return 0.0;
        }

        Set<ControlDependency> dependencies = node.getControlDependencies();

        double minCdFitness = 1.0;

        for (ControlDependency dep: dependencies) {
            BranchCoverageGoal goal = new BranchCoverageGoal(dep, node.getClassName(), node.getMethodName());
            BranchCoverageTestFitness cdGoal = new BranchCoverageTestFitness(goal);

            minCdFitness = Math.min(minCdFitness, cdGoal.getFitness(individual, result));
        }

        return minCdFitness;
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
        result = prime * result + targetClassName.hashCode();
        result = prime * result + targetMethodName.hashCode();
        result = prime * result + targetLineNumber;
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

    @Override
    public String toString() {
        return "Goal [" + node.getClassName() + "::" + node.getMethodName() + " @line: " + node.getLineNumber() + "]";
    }
}
