package six.eared.macaque.agent.test;

public class TestAddMethodClass {

    public Object field1 = "1234";
    public static Object field2 = "1234";
    private static String field3 = "1234";

    public String test1() {
        return "test1";
    }

    public String test2() {
        return "test2";
    }

    private static String test3() {
        return "test3";
    }

    private static String testStaic(String a, String b, String c) {
        return a+b+c;
    }
}