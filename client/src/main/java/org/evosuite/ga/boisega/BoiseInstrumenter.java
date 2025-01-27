package org.evosuite.ga.boisega;

import org.evosuite.PackageInfo;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.*;

public class BoiseInstrumenter extends ClassVisitor {
    static class SpanInstrumentingMethodVisitor extends MethodVisitor {
        boolean withinSpan = false;
        List<Integer> capturedVariableIndices = new ArrayList<>();

        private static String currentInstrumentationIdName;

        @Override
        public void visitMethodInsn(
                final int opcode,
                final String owner,
                final String name,
                final String descriptor,
                final boolean isInterface) {

            // Default visit this.
            mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

            // Check if instrumentation directives.
            if (owner.equals("Vtrace")) {

                // Check if the method call is a static call to Vtrace.start();
                if (name.equals("start")) {
                    withinSpan = true;
                    return;
                }

                // Check if the method call is a static call to Vtrace.capture();
                if (name.equals("capture")) {
                    // Dump stuff here.
                    LoggingUtils.getEvoLogger().info("Captured variables' usages: {} and execution tracer is {}", capturedVariableIndices, ExecutionTracer.isEnabled());

                    // Push the instumentation tag
                    mv.visitLdcInsn(currentInstrumentationIdName);

                    if (capturedVariableIndices.isEmpty()) {
                        throw new IllegalArgumentException("Vtrace called with empty capture options");
                    }

                    // Generate an array to push our values.
                    mv.visitIntInsn(BIPUSH, capturedVariableIndices.size());
                    mv.visitIntInsn(NEWARRAY, T_INT);

                    // Push all values to an array (last arg to our captureDataPoint call)
                    for (int idx = 0; idx < capturedVariableIndices.size() ; idx++) {
                        mv.visitInsn(DUP);
                        mv.visitIntInsn(BIPUSH, idx);
                        // Get the variable from the stack.
                        mv.visitVarInsn(ILOAD, capturedVariableIndices.get(idx));
                        mv.visitInsn(IASTORE);
                    }

                    String instrumentationCacheClassName = PackageInfo.getNameWithSlash(ExecutionTracer.class);
                    mv.visitMethodInsn(opcode, instrumentationCacheClassName, "captureDataPoint", "(Ljava/lang/String;[I)V", false);

                    // Reset everything.
                    withinSpan = false;
                    capturedVariableIndices.clear();

                    return;
                }

                // Nothing matched for Vtrace library.
                throw new IllegalArgumentException(String.format("Invalid function {} called with Vtrace", name));
            }
        }

        @Override
        public void visitLdcInsn(final Object value) {
            mv.visitLdcInsn(value);

            if (value instanceof String && withinSpan) {
                currentInstrumentationIdName = value.toString();
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
            mv.visitVarInsn(opcode, var);
        }

        public SpanInstrumentingMethodVisitor(MethodVisitor mv) {
            super(ASM9, mv);
        }
    }


    public BoiseInstrumenter(ClassVisitor cv, String cn) {
        super(ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new SpanInstrumentingMethodVisitor(mv);
    }
}
