import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.lang.Integer;
import java.util.Set;

public class OrOperator {

    public static void main(String[] args) {
        List<Integer> list = Arrays.asList(4, 6, 0, 5, 1);
        orOperator(list);
    }

    public static Set<Integer> orOperator(List<Integer> data) {
        Set<Integer> set = new HashSet<Integer>();

        for (int i = 0; i < data.size(); i += 1) {
            int num = data.get(i);
            if (num != 0 || num == 1) {
                set.add(num);
            }
        }
        return set;
    }
}
