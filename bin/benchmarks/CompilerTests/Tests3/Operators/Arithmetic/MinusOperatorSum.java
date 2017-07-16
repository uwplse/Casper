import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.lang.Float;
import java.lang.Math;

public class MinusOperatorSum {
    public static void main(String[] args) {
        Set<Float> set = new HashSet<Float>(Arrays.asList(1.5f, 2.2f, 8.7f, -1f));
        sum(set);
    }

    public static Float sum(Set<Float> set) {
        Iterator<Float> iterator = set.iterator();
        float sum = 0f;

        do {
            sum = sum - iterator.next();
        } while (iterator.hasNext());

        return Math.abs(sum);
    }
}
