package six.eared.macaque.agent.spi;

import six.eared.macaque.agent.hotswap.handler.FileHookHandler;
import six.eared.macaque.common.util.FileUtil;
import six.eared.macaque.common.util.ReflectUtil;
import six.eared.macaque.library.annotation.Library;
import six.eared.macaque.library.hook.HotswapHook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class LibrarySpiLoader {

    private static final String PATH = "META-INF/Library/";

    public static <T> Iterator<T> loadService(Class<T> clazz) {
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        return loader.iterator();
    }

    public static void loadLibraries() throws Exception {
        for (LibraryDefinition library : findLibrary()) {
            Library libraryAnnotation = library.getClazz().getAnnotation(Library.class);
            if (libraryAnnotation != null) {
                for (Class<? extends HotswapHook> hook : libraryAnnotation.hooks()) {
                    FileHookHandler.registerHook(ReflectUtil.newInstance(hook));
                }
            }
        }
    }

    public static List<LibraryDefinition> findLibrary() {
        try {
            List<LibraryDefinition> libraries = new ArrayList<>();
            Enumeration<URL> libraryUrls = LibrarySpiLoader.class.getClassLoader().getResources(PATH);
            while (libraryUrls.hasMoreElements()) {
                URL url = libraryUrls.nextElement();
                if (inJar(url)) {
                    findLibraryFromJarFile(url, libraries);
                    continue;
                }
                File libraryDirectory = new File(url.getPath());
                if (!libraryDirectory.exists() || !libraryDirectory.isDirectory()) {
                    continue;
                }
                File[] files = libraryDirectory.listFiles();
                if (files != null) {
                    for (File library : files) {
                        LibraryDefinition definition = null;
                        try (FileInputStream fis = new FileInputStream(library.getPath())) {
                            definition = createDefinition(library.getName(), fis);
                        }
                        if (definition != null) libraries.add(definition);
                    }
                }
            }
            return libraries;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void findLibraryFromJarFile(URL url, List<LibraryDefinition> libraries) throws IOException {
        URLConnection urlConnection = url.openConnection();
        if (urlConnection instanceof JarURLConnection) {
            JarURLConnection connection = (JarURLConnection) urlConnection;
            JarFile jarFile = connection.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String relativePath = entry.getName();
                if (relativePath.startsWith(PATH) && !entry.isDirectory()) {
                    LibraryDefinition definition = null;
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        String[] split = relativePath.split("/");
                        definition = createDefinition(split[split.length - 1], is);
                    }
                    if (definition != null) libraries.add(definition);
                }
            }
        }
    }

    private static LibraryDefinition createDefinition(String name, InputStream is) {
        try {
            String className = new String(FileUtil.is2bytes(is));
            Class<?> clazz = Class.forName(className);

            LibraryDefinition libraryDefinition = new LibraryDefinition();
            libraryDefinition.setName(name);
            libraryDefinition.setClazz(clazz);
            return libraryDefinition;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean inJar(URL url) {
        return url.getPath().contains(".jar!");
    }
}
