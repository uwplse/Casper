import java.util.Arrays;
import java.util.HashSet;
import java.lang.Integer;
import java.util.Set;

public class SwitchSum {

    public static void main(String[] args) {
        Set<Integer> set = new HashSet<Integer>(Arrays.asList(2, 0, 1, 3, 9));
        sum(set);
    }

    public static Integer sum(Set<Integer> data) {
        int sum = 0;
        for (int num : data) {
            switch (num) {
                case 0: sum += 0;
                    break;
                case 1: sum += 1;
                    break;
                case 2: sum += 2;
                    break;
                default: sum += num;
                    break;
            }
        }
        return sum;
    }
}
