package six.eared.macaque.library.patch;


import io.github.hhy50.linker.asm.AsmClassBuilder;
import io.github.hhy50.linker.asm.MethodBuilder;
import io.github.hhy50.linker.generate.bytecode.MethodDescriptor;
import io.github.hhy50.linker.generate.bytecode.action.*;
import io.github.hhy50.linker.generate.bytecode.utils.Methods;
import io.github.hhy50.linker.generate.bytecode.vars.ObjectVar;
import io.github.hhy50.linker.util.TypeUtil;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import six.eared.macaque.agent.accessor.Accessor;
import six.eared.macaque.agent.accessor.AccessorUtil;
import six.eared.macaque.agent.asm2.AsmMethod;
import six.eared.macaque.agent.enhance.*;
import six.eared.macaque.agent.env.Environment;
import six.eared.macaque.common.util.ClassUtil;
import six.eared.macaque.common.util.FileUtil;
import six.eared.macaque.preload.PatchMethodInvoker;
import six.eared.macaque.preload.PatchedInvocation;

import java.io.File;

public class MethodPatchWriter {

    public static MethodVisitor patchMethod(ClassLoader classLoader, AsmClassBuilder classBuilder, MethodBuilder methodBuilder,
                                            MethodDescriptor delegationMd) {
        String className = classBuilder.getClassName();
        MethodDescriptor methodDescriptor = methodBuilder.getDescriptor();
        Accessor accessor = AccessorUtil.createAccessor(className, new AccessorClassNameGenerator(), 1);
        MethodBindInfo bindInfo = MethodBindManager.createPatchedBindInfo(
                className,
                AsmMethod.AsmMethodBuilder.builder()
                        .modifier(methodBuilder.getAccess())
                        .methodName(methodDescriptor.getMethodName())
                        .desc(methodDescriptor.getDesc())
                        .build(),
                accessor.getClassName()
        );
        if (bindInfo.isLoaded()) {
            return null;
        }
        accessor.load(classLoader);

        // 生成调用“委托方法”的字节码
        invokeDelegationMethod(methodBuilder, bindInfo.getBindClass(), accessor.getClassName(), delegationMd);
        return new PatchedMethodUpdater(classLoader, bindInfo, accessor);
    }

    private static void invokeDelegationMethod(MethodBuilder methodBuilder, String patchedClass, String accessor,
                                               MethodDescriptor delegationMd) {
        MethodInvokeAction invoker = new MethodInvokeAction(MethodDescriptor.LINKER_FACTORY_CREATE_STATIC_LINKER).setArgs(
                LdcLoadAction.of(Type.getType(PatchMethodInvoker.class)),
                LdcLoadAction.of(TypeUtil.getType(patchedClass)));

        methodBuilder.intercept(new MethodInvokeAction(delegationMd)
                .setArgs(new NewObjectAction(Type.getType(PatchedInvocation.class), ObjectVar.TYPE, Type.getType(PatchMethodInvoker.class), Type.getType(Class.class), Type.getType(Object[].class))
                        .setArgs(methodBuilder.isStatic() ? Actions.loadNull() : LoadAction.LOAD0, invoker, LdcLoadAction.of(TypeUtil.getType(accessor)),
                                Actions.asArray(ObjectVar.TYPE, methodBuilder.getMethodBody().getArgs())))
                .thenReturn());
    }
}

class PatchedMethodUpdater extends BindMethodWriter {
    private final ClassLoader loader;
    private final MethodBindInfo bindInfo;
    private final AsmClassBuilder bindClassBuilder;

    protected PatchedMethodUpdater(ClassLoader loader, MethodBindInfo bindInfo, Accessor accessor) {
        super(bindInfo, accessor);
        this.loader = loader;
        this.bindClassBuilder = new AsmClassBuilder(Opcodes.ACC_PUBLIC, bindInfo.getBindClass(), Object.class.getName(), null, null)
                .defineConstruct(Opcodes.ACC_PUBLIC)
                .intercept(Methods.invokeSuper().thenReturn());
        this.bindInfo = bindInfo;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        // 这里不需要调用MethodBuilder的end()，不然回导致visitMax调用两次
        this.bindClassBuilder.defineMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                        bindInfo.getBindMethod(), bindInfo.getBindMethodType(), null)
                .accept(body -> PatchedMethodUpdater.this.accept(body.getWriter()));
        this.bindClassBuilder.end();
        this.bindInfo.setLoaded(true);

        try {
            byte[] bytecode = this.bindClassBuilder.toBytecode();
            if (Environment.isDebug()) {
                FileUtil.writeBytes(new File(FileUtil.getProcessTmpPath()+"/patched/"+ClassUtil.toSimpleName(bindClassBuilder.getClassName())+".class"),
                        bytecode);
            }
            EnhanceBytecodeClassLoader.loadClass(this.loader, this.bindInfo.getBindClass(), bytecode);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}