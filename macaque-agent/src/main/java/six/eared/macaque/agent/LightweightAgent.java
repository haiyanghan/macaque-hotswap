package six.eared.macaque.agent;

import six.eared.macaque.agent.env.Environment;
import six.eared.macaque.agent.spi.LibrarySpiLoader;

import java.lang.instrument.Instrumentation;

public class LightweightAgent {

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        Environment.initEnv(true, inst);
        LibrarySpiLoader.loadLibraries();
        System.out.println("attach success, initEnv()");
        System.out.println("linker output path: " + System.getProperty("linker.output.path"));
    }
}
