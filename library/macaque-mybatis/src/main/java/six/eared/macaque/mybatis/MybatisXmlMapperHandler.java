package six.eared.macaque.mybatis;


import io.github.hhy50.linker.LinkerFactory;
import io.github.hhy50.linker.annotations.Method;
import io.github.hhy50.linker.annotations.Runtime;
import six.eared.macaque.agent.enhance.ClassEnhancer;
import six.eared.macaque.agent.env.Environment;
import six.eared.macaque.agent.tool.VmToolExt;
import six.eared.macaque.agent.vcs.VersionChainTool;
import six.eared.macaque.library.hook.HotswapHook;
import six.eared.macaque.mbean.rmi.HotSwapRmiData;
import six.eared.macaque.mbean.rmi.RmiResult;
import six.eared.macaque.mybatis.mapping.MybatisConfigure;
import six.eared.macaque.mybatis.mapping.XMLMapperBuilder;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.Set;


public class MybatisXmlMapperHandler implements HotswapHook {
    private Set<Class<?>> patchedStrictMaps = new HashSet<>();

    @Override
    public RmiResult executeBefore(HotSwapRmiData rmiData) {
        if (!rmiData.getFileType().equals("xml")) {
            return null;
        }
        String namespace = null;
        if ((namespace = getNamespace(rmiData.getFileData())) == null) {
            return null;
        }

        Object[] configureObjs = VmToolExt.getInstanceByName("org.apache.ibatis.session.Configuration");
        for (Object configureObj : configureObjs) {
            try {
                makeSurePatched(configureObj.getClass().getClassLoader());
                replaceXml(LinkerFactory.createLinker(MybatisConfigure.class, configureObj), namespace, rmiData.getFileName(), rmiData.getFileData());
            } catch (Exception e) {
                return RmiResult.error(e.getMessage());
            }
        }
        return RmiResult.success();
    }

    private void makeSurePatched(ClassLoader cl) throws ClassNotFoundException, IOException, UnmodifiableClassException {
        ClassEnhancer.enhance(cl.getClass());

        String cn = "org.apache.ibatis.session.Configuration$StrictMap";
        Class<?> strictMapClass = cl.loadClass(cn);
        if (patchedStrictMaps.contains(strictMapClass)) {
            return;
        }
        Environment.getInst().retransformClasses(strictMapClass);
        patchedStrictMaps.add(strictMapClass);
    }

    private String getNamespace(byte[] xmlData) {
        try (InputStream in = new ByteArrayInputStream(xmlData)) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            // 禁用外部实体解析
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

            XMLEventReader reader =  factory.createXMLEventReader(in);
            while (reader.hasNext()) {
                XMLEvent next = reader.nextEvent();
                if (next.getClass().getSimpleName().equals("StartElementEvent")) {
                    StartElementEvent mapperEvent = LinkerFactory.createLinker(StartElementEvent.class, next);
                    if (mapperEvent.getName().equals(QName.valueOf("mapper"))) {
                        Attribute attr = mapperEvent.getAttributeByName(QName.valueOf("namespace"));
                        if (attr != null) {
                            return attr.getValue();
                        }
                    }
                }
            }
        } catch (XMLStreamException ignored) {

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void replaceXml(MybatisConfigure configure, String namespace, String fileName, byte[] xmlData) throws MapperReplaceException {
        try (InputStream in = new ByteArrayInputStream(xmlData)) {
            XMLMapperBuilder xmlMapperBuilder = LinkerFactory.createStaticLinker(XMLMapperBuilder.class, configure.getClass().getClassLoader())
                    .newInstance(in,
                            configure,
                            String.format("/macaque/%s/%s", VersionChainTool.getActiveVersionView().getVersion().getNumber(), fileName),
                            configure.getSqlFragments(),
                            namespace);
            xmlMapperBuilder.parse();
        } catch (Exception e) {
            throw new MapperReplaceException(e.getMessage());
        }
    }

    @Override
    public RmiResult executeAfter(HotSwapRmiData rmiData, RmiResult result, Throwable error) {
        return null;
    }

    @Runtime
    static interface StartElementEvent {
        public QName getName();

        @Method.Expr("getAttributeByName(..)")
        Attribute getAttributeByName(QName name);
    }
}
