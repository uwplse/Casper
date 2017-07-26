import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.lang.Integer;

public class OmitBracketsSum {

    public static void main(String[] args) {
        Set<Integer> set = new TreeSet<Integer>(Arrays.asList(2, -10, 1, 3, 9));
        sum(set);
    }

    public static int sum(Set<Integer> data) {
        if (data.size() == 0) return 0;

        int sum = 0;
        for (int num : data)
            if (true)
                sum += num;

        return sum;
    }
}
