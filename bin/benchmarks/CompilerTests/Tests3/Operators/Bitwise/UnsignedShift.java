import java.lang.Integer;
import java.util.Arrays;

public class UnsignedShift {

    public static void main(String[] args) {
        int[] list = {17, 8, -12, -5};
        shift(list);
        System.out.println(Arrays.toString(list));
    }

    public static void shift(int[] data) {
        for (Integer i = 0; i < data.length; i++) {
            int num = data[i] >>> 1;
            data[i] = num;
        }
    }
}
