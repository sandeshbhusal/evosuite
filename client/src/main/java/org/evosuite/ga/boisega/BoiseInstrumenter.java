package org.evosuite.ga.boisega;

import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ILOAD;

public class BoiseInstrumenter extends ClassVisitor {
    class SpanInstrumentingMethodVisitor extends MethodVisitor {
        boolean withinSpan = false;

        List<Integer> capturedVariableIndices = new ArrayList<>();

        @Override
        public void visitMethodInsn(
                final int opcode,
                final String owner,
                final String name,
                final String descriptor,
                final boolean isInterface) {
            // Check if the method call is a static call to Vtrace.start();
            if (owner.equals("Vtrace")) {
                if (name.equals("start")) {
                    withinSpan = true;
                } else if (name.equals("capture")) {
                    // Dump stuff here.
                    LoggingUtils.getEvoLogger().info("Captured variables' usages: {}", capturedVariableIndices);
                    // Here, we would continue to visit some custom instructions instead of the current one,
                    // that will redirect the captured variable values to evosuite's internal listener instead.
                    withinSpan = false;

                    String instrumentationCacheClassName = BoiseInstrumentationCache.class.getName().replace('.', '/');
                    super.visitMethodInsn(opcode, instrumentationCacheClassName, "captureDataPoint", descriptor, isInterface);

                } else {
                    throw new IllegalArgumentException(String.format("Invalid function {} called with Vtrace", name));
                }
            } else {
                // Continue to visit this normally.
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }

        @Override
        public void visitVarInsn(final int opcode, final int var) {
            if (withinSpan) {
                if (opcode == ILOAD) {
                    capturedVariableIndices.add(var);
                }
            }

            // We still need this var on the stack. We are just going to modify the "Vtrace.capture()" call,
            // and change it to something else we define inside evosuite.
            super.visitVarInsn(opcode, var);
        }

        public SpanInstrumentingMethodVisitor(int api) {
            super(api);
        }

        public SpanInstrumentingMethodVisitor(MethodVisitor mv) {
            super(ASM9, mv);
            LoggingUtils.getEvoLogger().info("Called instrumenting method visitor");
        }
    }

    private String className;
    private int api;
    private List<Span> capturedSpans = new ArrayList<>();

    public BoiseInstrumenter(int api) {
        super(api);
        this.api = api;
    }

    public BoiseInstrumenter(ClassVisitor cv, String cn) {
        super(ASM9, cv);
        this.className = cn;
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new SpanInstrumentingMethodVisitor(mv);
    }


    private static class Span {
        int start;
        int end;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

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

        for (Span span : instrumentationSpans) {
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
}
