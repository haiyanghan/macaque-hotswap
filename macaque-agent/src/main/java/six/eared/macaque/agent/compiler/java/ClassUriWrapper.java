package six.eared.macaque.agent.compiler.java;

import java.net.URI;

public class ClassUriWrapper {
    private final URI uri;

    private final String className;

    public ClassUriWrapper(String className, URI uri) {
        this.className = className;
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    public String getClassName() {
        return className;
    }
}
