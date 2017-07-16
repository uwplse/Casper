import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashSet;
import java.lang.Integer;

public class IteratorSum {

    public static void main(String[] args) {
        Set<Integer> set = new LinkedHashSet<Integer>();
        set.add(2);
        set.add(-1);
        set.add(3);
        sum(set);
    }

    public static int sum(Set<Integer> data) {
        Iterator<Integer> iterator = data.iterator();
        int sum = 0;

        while (iterator.hasNext()) {
            sum += iterator.next();
        }
        return sum;
    }
}
