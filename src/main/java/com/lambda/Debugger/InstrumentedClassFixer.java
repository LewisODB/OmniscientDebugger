package com.lambda.Debugger;

import org.apache.bcel.classfile.JavaClass;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public final class InstrumentedClassFixer {

    private InstrumentedClassFixer() {
    }

    public static byte[] toVerifiedClassBytes(JavaClass javaClass) {
        byte[] classFile = javaClass.getBytes();
        try {
            ClassReader reader = new ClassReader(classFile);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    try {
                        return super.getCommonSuperClass(type1, type2);
                    } catch (RuntimeException e) {
                        return "java/lang/Object";
                    }
                }
            };
            reader.accept(writer, ClassReader.SKIP_FRAMES);
            return writer.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to recompute stack map frames for "
                    + javaClass.getClassName(), e);
        }
    }
}
