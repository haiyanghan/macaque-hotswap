package six.eared.macaque.agent.test;

public class TestAddMethodClass {
    public String test1() {
        return "test1";
    }

    public String test2() {
        return test3();
    }

    /**
     * new method
     *
     * @return
     */
    public String test3() {
        return "test3";
    }
}