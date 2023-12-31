package six.eared.macaque.common.util;

public class HexStringUtil {

    private static final byte F = (byte) 0xF;



    public static String bytes2hexStr(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            chars[i*2 + 0] = byteToChar((byte) (b >> 4));
            chars[i*2 + 1] = byteToChar((byte) (b & F));
        }
        return new String(chars);
    }

    private static char byteToChar(byte b) {
        return new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'} [b];
    }

    public static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}
