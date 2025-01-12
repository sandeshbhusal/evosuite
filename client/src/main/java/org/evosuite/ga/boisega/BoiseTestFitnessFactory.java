package org.evosuite.ga.boisega;

import org.evosuite.TestGenerationContext;
import org.evosuite.coverage.branch.Branch;
import org.evosuite.coverage.branch.BranchCoverageFactory;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.graphs.cfg.ControlDependency;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoiseTestFitnessFactory {
    public static List<TestFitnessFunction> getGoals() {
        BytecodeInstructionPool pool = BytecodeInstructionPool.getInstance(TestGenerationContext.getInstance().getClassLoaderForSUT());
        HashSet<Branch> goalBranches = new HashSet<>();

        int totalBranchesNeededForInstrumentation = 0;

        for (BytecodeInstruction instr: pool.getAllInstructions()) {
            instr.getInstructionType();
            instr.getLineNumber();
            String methodName = instr.getMethodName();
            String className = instr.getClassName();
            if (instr.getASMNode().getOpcode() == Opcodes.INVOKESTATIC) {
                MethodInsnNode node = (MethodInsnNode) instr.getASMNode();
                boolean ourinstrumentationnode = node.name.equals("capture");
                if (ourinstrumentationnode) {
                    // Now we have some fun with this node, generate a test fitness target for it, etc.
                    LoggingUtils.getEvoLogger().info("*** Found a node to instrument! {} has cdg as {}", instr, instr.getCDG());

                    // let's gather all branches this instruction is control-dependent on.
                    Set<ControlDependency> dependentBranches = instr.getControlDependencies();
                    LoggingUtils.getEvoLogger().info("Dependent branches: {}", dependentBranches.size());

                    for (ControlDependency cd: dependentBranches) {
                        LoggingUtils.getEvoLogger().info("Branch: {}", cd.getBranch().getInstruction().getASMNode());
                        goalBranches.add(cd.getBranch());
                    }

                    LoggingUtils.getEvoLogger().info("=== End of instrumentation node ===");
                }
            }
        }

        // Get all branches from branch factory, and retain if only in goalBranches.
        BranchCoverageFactory factory = new BranchCoverageFactory();
        List<BranchCoverageTestFitness> allBranches = factory.getCoverageGoals();
        LoggingUtils.getEvoLogger().info("Total branches: {}", allBranches.size());

        List<TestFitnessFunction> retained = new ArrayList<>();

        for (BranchCoverageTestFitness branch: allBranches) {
            if (goalBranches.contains(branch.getBranch())) {
                LoggingUtils.getEvoLogger().info("Goal branch: {}", branch.getBranch().getInstruction().getASMNode());
                retained.add(branch);
                totalBranchesNeededForInstrumentation += 1;
            } else {
                LoggingUtils.getEvoLogger().info("Skipping non-required branch fitness: {}", branch.getBranch());
            }
        }


        LoggingUtils.getEvoLogger().info("Total branches needed for instrumentation: {}", totalBranchesNeededForInstrumentation);
        return retained;
    }
}
