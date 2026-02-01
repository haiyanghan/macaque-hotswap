package six.eared.macaque.agent.hotswap.handler;

import six.eared.macaque.agent.annotation.HotSwapFileType;
import six.eared.macaque.agent.asm2.AsmUtil;
import six.eared.macaque.agent.asm2.classes.ClazzDefinition;
import six.eared.macaque.agent.asm2.classes.CorrelationClazzDefinition;
import six.eared.macaque.agent.enhance.ClassIncrementUpdate;
import six.eared.macaque.agent.enhance.ClazzDataDefinition;
import six.eared.macaque.agent.enhance.CompatibilityModeByteCodeEnhancer;
import six.eared.macaque.agent.env.Environment;
import six.eared.macaque.agent.hotswap.ClassHotSwapper;
import six.eared.macaque.agent.vcs.VersionChainTool;
import six.eared.macaque.agent.vcs.VersionView;
import six.eared.macaque.common.ExtPropertyName;
import six.eared.macaque.common.type.FileType;
import six.eared.macaque.common.util.ClassUtil;
import six.eared.macaque.common.util.FileUtil;
import six.eared.macaque.mbean.rmi.HotSwapRmiData;
import six.eared.macaque.mbean.rmi.RmiResult;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@HotSwapFileType(fileType = FileType.Class)
public class ClassHotSwapHandler extends FileHookHandler {

    private static final List<String> SUPPORT_FILE = Arrays.asList("java", "class");

    @Override
    public RmiResult doHandler(HotSwapRmiData rmiData) throws Exception {
        if (!SUPPORT_FILE.contains(rmiData.getFileType())) {
            return RmiResult.error("unsupported file type");
        }
        return handler(rmiData.getFileData(), rmiData.getExtProperties());
    }

    @SuppressWarnings("unchecked")
    public RmiResult handler(byte[] bytes, Map<String, String> extProperties) throws Exception {
        List<ClazzDataDefinition> definitions = AsmUtil.readMultiClass(bytes);

        boolean compatibilityMode = Boolean.TRUE.toString()
                .equalsIgnoreCase(extProperties.get(ExtPropertyName.COMPATIBILITY_MODE));

        if (compatibilityMode) {
            List<ClassIncrementUpdate> enhanced = CompatibilityModeByteCodeEnhancer.enhance(definitions);
            return RmiResult.success().data(ClassHotSwapper.redefines(flatClassDefinition2(enhanced)));
        } else {
            return RmiResult.success().data(ClassHotSwapper
                    .redefines(flatClassDefinition(definitions)));
        }
    }

    private Map<String, byte[]> flatClassDefinition2(List<ClassIncrementUpdate> enhanced) {
        VersionView versionView = VersionChainTool.getActiveVersionView();

        Map<String, byte[]> flatMap = new HashMap<>();
        for (ClassIncrementUpdate item : enhanced) {
            flatMap.put(item.getClassName(), item.getEnhancedByteCode());
            if (Environment.isDebug()) {
                FileUtil.writeBytes(new File(FileUtil.getProcessTmpPath()+"/compatibility/"+
                                ClassUtil.toSimpleName(item.getClassName())+".class"),
                        item.getEnhancedByteCode());
            }

            if (item.getCorrelationClasses() == null) continue;
            for (CorrelationClazzDefinition itemCorrelation : item.getCorrelationClasses()) {
                ClazzDefinition clazzDefinition = itemCorrelation.getClazzDefinition();
                if (clazzDefinition instanceof ClazzDataDefinition) {
                    flatMap.put(clazzDefinition.getClassName(), ((ClazzDataDefinition) clazzDefinition).getBytecode());
                    if (Environment.isDebug()) {
                        FileUtil.writeBytes(new File(FileUtil.getProcessTmpPath()+"/compatibility/"+
                                        ClassUtil.toSimpleName(clazzDefinition.getClassName())+".class"),
                                ((ClazzDataDefinition) clazzDefinition).getBytecode());
                    }
                }
            }
//            versionView.addDefinition(item);
        }
        return flatMap;
    }

    private Map<String, byte[]> flatClassDefinition(List<ClazzDataDefinition> definitions) {
        VersionView versionView = VersionChainTool.getActiveVersionView();

        Map<String, byte[]> flatMap = new HashMap<>();
        for (ClazzDataDefinition item : definitions) {
            flatMap.put(item.getClassName(), item.getBytecode());
//            versionView.addDefinition(item);
        }
        return flatMap;
    }
}
