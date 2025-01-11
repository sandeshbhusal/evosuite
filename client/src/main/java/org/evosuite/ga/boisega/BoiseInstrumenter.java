package org.evosuite.ga.boisega;

import org.evosuite.PackageInfo;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.utils.LoggingUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ILOAD;

public class BoiseInstrumenter extends ClassVisitor {
    static class SpanInstrumentingMethodVisitor extends MethodVisitor {
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
                    LoggingUtils.getEvoLogger().info("Captured variables' usages: {} and execution tracer is {}", capturedVariableIndices, ExecutionTracer.isEnabled());

                    // Here, we would continue to visit some custom instructions instead of the current one,
                    // that will redirect the captured variable values to evosuite's internal listener instead.
                    withinSpan = false;
                    capturedVariableIndices.clear();

                    String instrumentationCacheClassName = PackageInfo.getNameWithSlash(BoiseInstrumentationCache.class);
                    LoggingUtils.getEvoLogger().info("{}.{}", descriptor, opcode);
                    mv.visitMethodInsn(opcode, instrumentationCacheClassName, "captureDataPoint", descriptor, isInterface);

                } else {
                    throw new IllegalArgumentException(String.format("Invalid function {} called with Vtrace", name));
                }
            } else {
                // Continue to visit this normally.
                mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
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

        public SpanInstrumentingMethodVisitor(MethodVisitor mv) {
            super(ASM9, mv);
            LoggingUtils.getEvoLogger().info("Called instrumenting method visitor");
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
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return new SpanInstrumentingMethodVisitor(mv);
    }
}
