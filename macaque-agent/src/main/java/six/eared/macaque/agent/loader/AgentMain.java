package six.eared.macaque.agent.loader;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.util.jar.JarFile;

/**
 * 探针主类
 */
public class AgentMain {

    private static boolean START_FLAG = false;

    public static void agentmain(String args, Instrumentation inst) {
        loadBootstrap(args, inst);
    }

    /**
     * 加载引导类
     *
     * @param args 参数
     * @param inst inst
     */
    private synchronized static void loadBootstrap(String args, Instrumentation inst) {
        if (!START_FLAG) {
            Class<?> bootstrapClass = null;
            try {
                File agentJar = getTempJar("lib/agent.jar");
                File preloadJar = getTempJar("lib/preload.jar");

                // 加载引导类
                ClassLoader classLoader = getClassLoader(agentJar.toURI().toURL());
                bootstrapClass = classLoader.loadClass("six.eared.macaque.agent.AgentBootstrap");

                inst.appendToBootstrapClassLoaderSearch(new JarFile(preloadJar));
            } catch (Exception e) {
                System.out.println("load AgentBootstrap.class error");
                e.printStackTrace();
            }

            if (bootstrapClass != null) {
                try {
                    // 调用引导类的start方法
                    Method init = bootstrapClass.getMethod("start", String.class, Instrumentation.class);
                    START_FLAG = (boolean) init.invoke(null, args, inst);
                } catch (Exception e) {
                    System.out.println("AgentBootstrap.start error");
                    e.printStackTrace();
                }
            }
        }
    }

    private static File getTempJar(String innerJarName) throws IOException {
        File file = new File(System.getProperty("java.io.tmpdir"), "macaque"+File.separator+innerJarName);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (InputStream in = AgentMain.class.getClassLoader().getResourceAsStream(innerJarName);
             OutputStream out = Files.newOutputStream(file.toPath())) {
            byte[] buffer = new byte[4*1024];
            int bytesRead = -1;
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
        return file;
    }

    /**
     * 获取系统类加载器
     *
     * @return 类加载器
     */

    private static ClassLoader getClassLoader(URL agentJar) {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        while (classLoader.getParent() != null) {
            classLoader = classLoader.getParent();
        }
        return new MacaqueClassLoader(agentJar, classLoader);
    }
}
