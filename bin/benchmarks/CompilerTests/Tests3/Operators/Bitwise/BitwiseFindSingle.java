import java.util.Arrays;
import java.util.List;
import java.lang.Integer;

public class BitwiseFindSingle {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(2, -10, 1, 2, 9, 1, 3, 9, -10);
        findUnpaired(list);
    }

    public static int findUnpaired(List<Integer> data) {
        int single = 0;
        for (int num : data) {
            single = single ^ num;
        }
        return single;
    }
}
