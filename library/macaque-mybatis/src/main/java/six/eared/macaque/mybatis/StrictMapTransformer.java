package six.eared.macaque.mybatis;

import io.github.hhy50.linker.LinkerFactory;
import io.github.hhy50.linker.asm.MethodBuilder;
import io.github.hhy50.linker.exceptions.LinkerException;
import io.github.hhy50.linker.generate.bytecode.MethodDescriptor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import six.eared.macaque.agent.asm2.AsmUtil;
import six.eared.macaque.agent.asm2.EnhancedAsmClassBuilder;
import six.eared.macaque.agent.asm2.classes.ClassVisitorDelegation;
import six.eared.macaque.agent.env.Environment;
import six.eared.macaque.common.util.ClassUtil;
import six.eared.macaque.common.util.FileUtil;
import six.eared.macaque.library.patch.MethodPatchWriter;
import six.eared.macaque.mybatis.mapping.MybatisStrictMap;
import six.eared.macaque.preload.PatchedInvocation;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class StrictMapTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader, String classPath, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (!classPath.equals("org/apache/ibatis/session/Configuration$StrictMap")) {
            return new byte[0];
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
                if (name.equals("put") && (access & Opcodes.ACC_SYNTHETIC) == 0) {
                    try {
                        return MethodPatchWriter.patchMethod(loader, classBuilder, methodBuilder, MethodDescriptor.of(StrictMapTransformer.class.getDeclaredMethod("put", PatchedInvocation.class)));
                    } catch (Exception e) {
                        if (Environment.isDebug()) {
                            e.printStackTrace();
                        }
                    }
                }
                return methodBuilder.getMethodBody().getWriter();
            }
        });
        byte[] bytecode = classBuilder.toBytecode();
        FileUtil.writeBytes(new File(FileUtil.getProcessTmpPath()+"/patched/"+
                        "Configuration$StrictMap$Enhanced.class"),
                bytecode);
        return bytecode;
    }

    public static Object put(PatchedInvocation invocation) throws LinkerException {
        try {
            return invocation.invoke();
        } catch (Exception e) {
            Object strictMap = invocation.getOrigin();
            Object[] args = invocation.getArgs();
            Object key = args[0];
            Object value = args[1];
            return LinkerFactory.createLinker(MybatisStrictMap.class, strictMap).put(key, value);
        }
    }
}
