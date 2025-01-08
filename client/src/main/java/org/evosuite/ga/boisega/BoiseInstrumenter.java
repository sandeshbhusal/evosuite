package org.evosuite.ga.boisega;

import org.evosuite.instrumentation.coverage.MethodInstrumentation;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;

public class BoiseInstrumenter implements MethodInstrumentation {
    private static class Span {
        int start;
        int end;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    @Override
    public void analyze(ClassLoader classLoader, MethodNode mn, String className, String methodName, int access) {
        // Gather all "static" invocation points, and check what methods they invoke.
        // If the invocation looks like "Vtrace.start()", then store the starting point,
        // until "Vtrace.end(...)" is called.

        // We will store the spans in a list.
        ArrayList<Span> instrumentationSpans = new ArrayList<>();

        // Iterate through all instructions in the methodnode, until we hit a static invocation.
        // If we hit a static invocation, we will check if it is a "Vtrace.start()" invocation.
        InsnList instructions = mn.instructions;

        int currentSpanStart = -1;
        for (int i = 0; i <= instructions.size(); i++) {
            AbstractInsnNode node = instructions.get(i);
            if (node.getOpcode() == AbstractInsnNode.METHOD_INSN) {
                // Method invocation node.
                MethodInsnNode methodNode = (MethodInsnNode) node;
                if (methodNode.owner.equals("Vtrace")) {
                    if (methodNode.name.equals("start")) {
                        // Start of a span.
                        currentSpanStart = i;
                    } else if (methodNode.name.equals("capture")) {
                        // End of a span.
                        if (currentSpanStart != -1) {
                            instrumentationSpans.add(new Span(currentSpanStart, i));
                            currentSpanStart = -1;
                        }
                    }
                }
            }
        }

        HashMap<Span, ArrayList<Integer>> iloads = new HashMap<>();

        for (Span span: instrumentationSpans) {
            ArrayList<Integer> iloadLocations = new ArrayList<>();

            for (int i = 0; i < span.end - span.start; i++) {
                AbstractInsnNode node = instructions.get(i + span.start);
                if (node.getOpcode() == AbstractInsnNode.VAR_INSN) {
                    VarInsnNode varNode = (VarInsnNode) node;

                    // Get opcode.
                    int opcode = varNode.getOpcode();
                    if (opcode == Opcodes.ILOAD) {
                        int iloadConstantPoolIndex = varNode.var;

                        // Add the local variable index from the internal constant pool.
                        // If we add 'i' here, it does not make any sense.
                        iloadLocations.add(iloadConstantPoolIndex);
                    }
                }
            }

            iloads.put(span, iloadLocations);
        }

        // At this point, we have a list of spans where Vtrace.start() and Vtrace.end() are called.
        // and the variables that have been loaded onto the stack between those two calls, which
        // happen to be the input parameters to the Vtrace.capture() method.

    }

    @Override
    public boolean executeOnMainMethod() {
        return false;
    }

    @Override
    public boolean executeOnExcludedMethods() {
        return false;
    }
}
