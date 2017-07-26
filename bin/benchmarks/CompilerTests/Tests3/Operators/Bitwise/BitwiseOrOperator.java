import java.util.Arrays;
import java.util.List;
import java.lang.Integer;

public class BitwiseOrOperator {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(5, 7, 2, 10);
        orOperator(list);
    }

    private static int orOperator(List<Integer> data) {
        Integer num = 0;
        for (int i : data) {
            num = num | i;
        }
        return num;
    }
}
