import java.util.Arrays;
import java.util.List;
import java.lang.Integer;

public class TimesOperator {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(4, 6, 2);
        times(list);
    }

    public static Integer times(List<Integer> data) {
        Integer total = 1;
        for (int num : data) {
            total = total * num;
        }
        return total;
    }
}
