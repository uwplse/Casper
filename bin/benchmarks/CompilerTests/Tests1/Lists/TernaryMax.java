import java.util.Arrays;
import java.util.List;
import java.lang.Integer;

public class TernaryMax {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(2, 0, 1, 3, 9);
        max(list);
    }

    public static int max(List<Integer> data) {
        int max = Integer.MIN_VALUE;
        for (int i = data.size() - 1; i >= 0; i--) {
            int num = data.get(i);
            max = (max > num) ? max : num;
        }
        return max;
    }
}
