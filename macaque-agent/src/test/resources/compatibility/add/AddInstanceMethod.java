package six.eared.macaque.agent.test;

public class TestAddMethodClass {
    public Object field1 = "1234";
    public static Object field2 = "1234";
    private static String field3 = "1234";

    public String test1() {
        return "test1";
    }

    public String test2() {
        System.out.println(test4());
        return test4();
    }

    private String test3(String a) {
        return "test3";
    }

    private String test4(String a) {
        return "";
    }

    private static String test3() {
        return "test3";
    }

    private static String testStaic(String a, String b, String c) {
        return a+b+c;
    }

    /**
     * new method
     *
     * @return
     */
    public String test4() {
        field1 = "1234";
        field2 = "5678";
        field3 = "90";
        return (String) field1+field2+test1()+test3("")+test4("")+field3;
    }
}