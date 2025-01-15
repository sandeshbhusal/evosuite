package org.evosuite.ga.boisega;

import org.evosuite.PackageInfo;
import org.evosuite.TestGenerationContext;
import org.evosuite.graphs.cfg.BytecodeInstruction;
import org.evosuite.graphs.cfg.BytecodeInstructionPool;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.List;

public class BoiseTestFitnessFactory {
    public static List<TestFitnessFunction> getGoals() {
        ArrayList<TestFitnessFunction> goals = new ArrayList<>();
        BytecodeInstructionPool pool = BytecodeInstructionPool
                .getInstance(
                        TestGenerationContext
                                .getInstance()
                                .getClassLoaderForSUT());

        String currentLDCCode = "";
        boolean withinScope = false;

        for (BytecodeInstruction instr : pool.getAllInstructions()) {
            AbstractInsnNode asmNode = instr.getASMNode();

            if (asmNode instanceof LdcInsnNode) {
                LdcInsnNode node = (LdcInsnNode) asmNode;

                if (withinScope) {
                    currentLDCCode = node.cst.toString();
                }
            } else if (asmNode instanceof MethodInsnNode) {
                MethodInsnNode node = (MethodInsnNode) asmNode;

                String instrumentationCacheClassName = PackageInfo.getNameWithSlash(ExecutionTracer.class);
                String owner = node.owner;
                String name = node.name;

                if (owner.equals("Vtrace") && name.equals("capture")) {
                    withinScope = true;
                    continue;
                }

                if (withinScope && owner.equals(instrumentationCacheClassName) && name.equals("captureDataPoint")) {
                    withinScope = false;

                    BoiseFitnessFunction goal = new BoiseFitnessFunction(instr, currentLDCCode);
                    goals.add(goal);
                }
            }
        }

        return goals;
    }
}
