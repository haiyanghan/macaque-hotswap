package six.eared.macaque.library.patch;

import io.github.hhy50.linker.asm.MethodBuilder;
import io.github.hhy50.linker.generate.bytecode.MethodDescriptor;
import lombok.SneakyThrows;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import six.eared.macaque.agent.asm2.AsmUtil;
import six.eared.macaque.agent.asm2.EnhancedAsmClassBuilder;
import six.eared.macaque.agent.asm2.classes.ClassVisitorDelegation;
import six.eared.macaque.agent.env.Environment;
import six.eared.macaque.common.util.ClassUtil;
import six.eared.macaque.preload.PatchedInvocation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import static six.eared.macaque.library.patch.MethodPatchWriter.patchMethod;

public class ClassloaderEnhancer implements ClassFileTransformer {

    private static Set<Class<?>> CLASSES = new HashSet<>();
    private final String className0;

    @SneakyThrows
    public ClassloaderEnhancer(ClassLoader cl) {
        this.className0 = cl.getClass().getName();
    }

    @SneakyThrows
    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!className.replace('/', '.').equals(this.className0)) {
            return null;
        }
        final EnhancedAsmClassBuilder classBuilder = new EnhancedAsmClassBuilder();
        AsmUtil.visitClass(classfileBuffer, new ClassVisitorDelegation(classBuilder.getClassWriter()) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                classBuilder.visit(access, ClassUtil.classpath2name(name), superName, interfaces, signature);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodBuilder methodBuilder = classBuilder.defineMethod(access, name, Type.getMethodType(descriptor), exceptions);
                if (name.equals("loadClass") && descriptor.equals("(Ljava/lang/String;)Ljava/lang/Class;") && (access & Opcodes.ACC_SYNTHETIC) == 0) {
                    try {
                        return patchMethod(loader, classBuilder, methodBuilder,
                                MethodDescriptor.of(ClassloaderEnhancer.class.getDeclaredMethod("loadClass", PatchedInvocation.class)));
                    } catch (Exception e) {
                        if (Environment.isDebug()) {
                            e.printStackTrace();
                        }
                    }
                }
                return methodBuilder.getMethodBody().getWriter();
            }
        });
        return classBuilder.toBytecode();
    }

    @SneakyThrows
    public synchronized static void enhance(ClassLoader cl) {
        Class<?> clClass = cl.getClass();
        if (CLASSES.contains(clClass)) {
            return;
        }
//        Environment.getInst().addTransformer(new ClassloaderEnhancer(cl), true);
//        Environment.getInst().retransformClasses(clClass);
        CLASSES.add(clClass);
    }

    public static Class<?> loadClass(PatchedInvocation invocation) {
        Object[] args = invocation.getArgs();
        String className = (String) args[0];
        try {
            Class<?> clazz = (Class<?>) invocation.invoke();
            if (clazz == null && (
                    className.startsWith("six.eared.macaque") || className.startsWith("io.github.hhy50.linker")
            )) {
                System.out.println("加载。。。。");
            }
            return clazz;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
