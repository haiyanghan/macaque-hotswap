package six.eared.macaque.agent.test;

public class TestAddMethodClass {

    public String test1() {
        System.out.println("test1");
        return "test1";
    }

    public String test2() {
        System.out.println("test2");
        return _newStaticMethod();
    }

    public static String test3() {
        return "test3";
    }

    public static String _newStaticMethod() {
        return "_newStaticMethod";
    }
}