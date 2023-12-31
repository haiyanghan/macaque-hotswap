package six.eared.macaque.common.util;

public class StringUtil {
    public static final String EMPTY_STR = "";
    public static final String ARG_PREFIX = "--";
    public static final String[] SPACE_STR = {
            " ", "  ", "   ", "    "
    };

    public static boolean isNotEmpty(String str) {
        return str != null && !"".equals(str);
    }

    public static boolean isEmpty(String str) {
        return str == null || "".equals(str);
    }
}
